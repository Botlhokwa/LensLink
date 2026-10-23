package com.example.lenslink.models;

public class Photo {
    private String photoId, bookingId, galleryId, imageUrl, caption;
    private String fileName, uploadedBy, storagePath;
    private long uploadedAt;

    public Photo() {}
    public Photo(String photoId, String bookingId, String galleryId, String imageUrl, String caption) {
        this.photoId = photoId; this.bookingId = bookingId; this.galleryId = galleryId; this.imageUrl = imageUrl; this.caption = caption;
    }
    public String getPhotoId() { return photoId; } public void setPhotoId(String v) { photoId = v; }
    public String getBookingId() { return bookingId; } public void setBookingId(String v) { bookingId = v; }
    public String getGalleryId() { return galleryId; } public void setGalleryId(String v) { galleryId = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
    public String getCaption() { return caption; } public void setCaption(String v) { caption = v; }
    public String getFileName() { return fileName; } public void setFileName(String v) { fileName = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { uploadedBy = v; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String v) { storagePath = v; }
    public long getUploadedAt() { return uploadedAt; } public void setUploadedAt(long v) { uploadedAt = v; }
}
