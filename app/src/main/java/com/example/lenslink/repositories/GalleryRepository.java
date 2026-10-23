package com.example.lenslink.repositories;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import com.example.lenslink.models.Photo;
import com.example.lenslink.utils.FirestoreConstants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Photo delivery.
 *  - Photographer: picks images from the device, originals go to Firebase Storage
 *    (galleries/{bookingId}/{photoId}.ext) and a Photo document is written to Firestore.
 *  - Client: fetches every photo for a booking and saves them all to Pictures/LensLink/{shoot}.
 */
public class GalleryRepository {

    public interface UploadProgress { void onProgress(int done, int total); }

    public static class UploadResult {
        public final int uploaded, failed;
        UploadResult(int uploaded, int failed) { this.uploaded = uploaded; this.failed = failed; }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    // Sample photos for the two demo bookings that exist in the mocked booking data.
    private static final List<Photo> SEED = new ArrayList<>();
    static {
        SEED.add(seed("ph1", "b1", "https://images.unsplash.com/photo-1519741497674-611481863552?w=1200", "Golden Hour Portrait #1"));
        SEED.add(seed("ph2", "b1", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1200", "Golden Hour Portrait #2"));
        SEED.add(seed("ph3", "b3", "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=1200", "Wedding Ceremony Highlight"));
    }

    private static Photo seed(String id, String bookingId, String url, String caption) {
        Photo p = new Photo(id, bookingId, "gal_" + bookingId, url, caption);
        p.setFileName(caption.replace('#', ' ').replace("  ", " ").trim() + ".jpg");
        return p;
    }

    // ------------------------------------------------------------------ upload (photographer)

    /** Uploads the picked images one after another so progress is easy to report and memory stays low. */
    public void uploadPhotos(Context context, String bookingId, String uploadedBy, List<Uri> uris,
                             UploadProgress progress, RepositoryCallback<UploadResult> cb) {
        uploadNext(context.getApplicationContext().getContentResolver(), bookingId, uploadedBy, uris, 0, 0, progress, cb);
    }

    private void uploadNext(ContentResolver cr, String bookingId, String uploadedBy, List<Uri> uris,
                            int index, int ok, UploadProgress progress, RepositoryCallback<UploadResult> cb) {
        if (index >= uris.size()) {
            cb.onSuccess(new UploadResult(ok, uris.size() - ok));
            return;
        }
        if (progress != null) progress.onProgress(index, uris.size());

        final Uri uri = uris.get(index);
        String mime = cr.getType(uri);
        if (mime == null || !mime.startsWith("image/")) mime = "image/jpeg";
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (ext == null) ext = "jpg";

        final DocumentReference doc = db.collection(FirestoreConstants.PHOTOS).document();
        final String photoId = doc.getId();
        final String path = "galleries/" + bookingId + "/" + photoId + "." + ext;
        final String fileName = displayName(cr, uri, photoId + "." + ext);
        final StorageReference ref = storage.getReference(path);
        StorageMetadata meta = new StorageMetadata.Builder().setContentType(mime).build();

        ref.putFile(uri, meta)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            })
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                Photo p = new Photo(photoId, bookingId, "gal_" + bookingId, task.getResult().toString(), fileName);
                p.setFileName(fileName);
                p.setUploadedBy(uploadedBy);
                p.setStoragePath(path);
                p.setUploadedAt(System.currentTimeMillis());
                return doc.set(p);
            })
            .addOnCompleteListener(task ->
                uploadNext(cr, bookingId, uploadedBy, uris, index + 1, ok + (task.isSuccessful() ? 1 : 0), progress, cb));
    }

    private static String displayName(ContentResolver cr, Uri uri, String fallback) {
        try (Cursor c = cr.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null && !n.trim().isEmpty()) return n;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    // ------------------------------------------------------------------ read

    public void getPhotosForBooking(String bookingId, RepositoryCallback<List<Photo>> cb) {
        final List<Photo> result = new ArrayList<>();
        for (Photo p : SEED) if (p.getBookingId().equals(bookingId)) result.add(p);

        db.collection(FirestoreConstants.PHOTOS).whereEqualTo("bookingId", bookingId).get()
            .addOnSuccessListener(snap -> {
                List<Photo> remote = snap.toObjects(Photo.class);
                Collections.sort(remote, (a, b) -> Long.compare(a.getUploadedAt(), b.getUploadedAt()));
                result.addAll(remote);
                cb.onSuccess(result);
            })
            .addOnFailureListener(cb::onError);
    }

    // ------------------------------------------------------------------ download all (client)

    /** Queues every photo with the system DownloadManager. Returns how many were queued. */
    public int downloadAll(Context context, List<Photo> photos, String shootName) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return 0;
        String folder = "LensLink/" + sanitize(shootName);
        Set<String> used = new HashSet<>();
        int queued = 0;
        for (Photo p : photos) {
            if (p.getImageUrl() == null) continue;
            String name = uniqueName(p, used);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(p.getImageUrl()));
            req.setTitle(name);
            req.setDescription("LensLink photo");
            // One grouped progress notification for a big batch, a completion notice for a single photo.
            req.setNotificationVisibility(photos.size() > 1
                ? DownloadManager.Request.VISIBILITY_VISIBLE
                : DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, folder + "/" + name);
            req.allowScanningByMediaScanner();
            dm.enqueue(req);
            queued++;
        }
        return queued;
    }

    private static String uniqueName(Photo p, Set<String> used) {
        String base = p.getFileName() != null && !p.getFileName().trim().isEmpty()
            ? sanitize(p.getFileName()) : "photo_" + p.getPhotoId() + ".jpg";
        if (!base.contains(".")) base = base + ".jpg";
        String name = base;
        int n = 1;
        while (!used.add(name.toLowerCase(Locale.ROOT))) {
            int dot = base.lastIndexOf('.');
            name = base.substring(0, dot) + "_" + (n++) + base.substring(dot);
        }
        return name;
    }

    private static String sanitize(String s) {
        String clean = s == null ? "" : s.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return clean.isEmpty() ? "Photos" : clean;
    }
}
