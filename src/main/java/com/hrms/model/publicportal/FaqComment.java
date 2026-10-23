package com.hrms.model.publicportal;

import java.time.LocalDateTime;

public class FaqComment {
    private long commentId;
    private int topicId;
    private String content;
    private int authorUserId;
    private String authorName;
    private LocalDateTime createdAt;

    public long getCommentId() { return commentId; }
    public void setCommentId(long commentId) { this.commentId = commentId; }
    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(int authorUserId) { this.authorUserId = authorUserId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : com.hrms.util.TimeSupport.formatUtc(createdAt);
    }
}
