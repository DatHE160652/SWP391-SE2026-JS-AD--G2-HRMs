package com.hrms.model.publicportal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FaqTopic {
    private int topicId;
    private String title;
    private String content;
    private int authorUserId;
    private String authorName;
    private LocalDateTime createdAt;
    private final List<FaqComment> comments = new ArrayList<>();

    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(int authorUserId) { this.authorUserId = authorUserId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<FaqComment> getComments() { return List.copyOf(comments); }
    public void addComment(FaqComment comment) { comments.add(comment); }
    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : com.hrms.util.TimeSupport.formatUtc(createdAt);
    }
}
