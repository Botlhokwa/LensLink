package com.example.lenslink.repositories;
import com.example.lenslink.models.Message;
import com.example.lenslink.utils.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatRepository {
    private static final List<Message> directMessages = new ArrayList<>();
    static {
        directMessages.add(new Message("m1", "direct_chat", "photo1", "photographer",
            "Hello Bee! Looking forward to your upcoming Golden Hour shoot in Rosebank. Let me know if you have any questions!", false));
    }

    public void sendDirectMessage(String senderRole, String text) {
        if (!Permissions.can(senderRole, Permissions.Feature.DIRECT_MESSAGE)) return;
        Message m = new Message("msg_" + System.currentTimeMillis(), "direct_chat", senderRole, senderRole, text, false);
        directMessages.add(m);
    }

    public List<Message> getDirectMessages() {
        return new ArrayList<>(directMessages);
    }

    public String getTailoredAiReply(String role, String input) {
        if (!Permissions.can(role, Permissions.Feature.AI_CHAT)) {
            return "The LensLink assistant is only available on client accounts.";
        }
        String q = input.toLowerCase(Locale.ROOT);
        if (q.contains("graduation") || q.contains("how much")) {
            return "We have 3 Graduation Packages at Barred Production Studios: \n• Basic – R800 (1 hr, 10 photos) \n• Standard – R1,500 (2 hrs, 25 photos) \n• Premium – R2,500 (3 hrs, 50 photos + A4 print) \nWould you like to book a session?";
        } else if (q.contains("standard") || q.contains("package")) {
            return "The Standard Graduation Package (R1,500) includes a 90-minute studio & outdoor session, 25 high-resolution edited photos, and online gallery download access.";
        } else if (q.contains("download") || q.contains("photo") || q.contains("gallery")) {
            return "Once your photographer uploads your shoot photos, go to 'My Bookings', tap 'Download Shoot Photos', and select any photo to save it directly to your device's Pictures folder!";
        } else if (q.contains("cancel") || q.contains("slot") || q.contains("available")) {
            return "Time slots are securely locked when booked to prevent double booking. If a booking is cancelled, that time slot immediately frees up for any user to book again!";
        } else {
            return "I'm your professional LensLink Assistant! You can ask me about photographer packages, pricing, slot locking rules, or downloading your high-res gallery photos.";
        }
    }
}
