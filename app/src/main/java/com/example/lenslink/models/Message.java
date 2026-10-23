package com.example.lenslink.models;
public class Message {
    private String messageId, chatId, senderId, senderRole, text;
    private boolean isBot;
    private long timestamp;
    public Message() {}
    public Message(String messageId, String chatId, String senderId, String senderRole, String text, boolean isBot) {
        this.messageId = messageId; this.chatId = chatId; this.senderId = senderId;
        this.senderRole = senderRole; this.text = text; this.isBot = isBot;
        this.timestamp = System.currentTimeMillis();
    }
    public String getMessageId() { return messageId; } public void setMessageId(String v) { messageId = v; }
    public String getChatId() { return chatId; } public void setChatId(String v) { chatId = v; }
    public String getSenderId() { return senderId; } public void setSenderId(String v) { senderId = v; }
    public String getSenderRole() { return senderRole; } public void setSenderRole(String v) { senderRole = v; }
    public String getText() { return text; } public void setText(String v) { text = v; }
    public boolean isBot() { return isBot; } public void setBot(boolean v) { isBot = v; }
    public long getTimestamp() { return timestamp; } public void setTimestamp(long v) { timestamp = v; }
}
