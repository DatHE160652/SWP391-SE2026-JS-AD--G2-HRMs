package com.hrms.dao.publicportal;

import com.hrms.model.publicportal.FaqComment;
import com.hrms.model.publicportal.FaqTopic;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FaqDAO {
    public List<FaqTopic> findTopicsWithComments() throws SQLException {
        String sql = """
                SELECT t.TopicId, t.Title, t.Content, t.AuthorUserId, t.CreatedAt,
                       COALESCE(u.DisplayName, u.Username, u.Email) AS AuthorName,
                       c.CommentId, c.TopicId AS CommentTopicId, c.Content AS CommentContent,
                       c.AuthorUserId AS CommentAuthorUserId, c.CreatedAt AS CommentCreatedAt,
                       COALESCE(cu.DisplayName, cu.Username, cu.Email) AS CommentAuthorName
                FROM FaqTopics t
                INNER JOIN Users u ON u.UserId = t.AuthorUserId
                LEFT JOIN FaqComments c ON c.TopicId = t.TopicId
                LEFT JOIN Users cu ON cu.UserId = c.AuthorUserId
                ORDER BY t.CreatedAt DESC, t.TopicId DESC, c.CreatedAt ASC, c.CommentId ASC
                """;
        List<FaqTopic> topics = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            FaqTopic current = null;
            while (result.next()) {
                int topicId = result.getInt("TopicId");
                if (current == null || current.getTopicId() != topicId) {
                    current = new FaqTopic();
                    current.setTopicId(topicId);
                    current.setTitle(result.getString("Title"));
                    current.setContent(result.getString("Content"));
                    current.setAuthorUserId(result.getInt("AuthorUserId"));
                    current.setAuthorName(result.getString("AuthorName"));
                    current.setCreatedAt(toLocalDateTime(result.getTimestamp("CreatedAt")));
                    topics.add(current);
                }
                long commentId = result.getLong("CommentId");
                if (!result.wasNull()) {
                    FaqComment comment = new FaqComment();
                    comment.setCommentId(commentId);
                    comment.setTopicId(result.getInt("CommentTopicId"));
                    comment.setContent(result.getString("CommentContent"));
                    comment.setAuthorUserId(result.getInt("CommentAuthorUserId"));
                    comment.setAuthorName(result.getString("CommentAuthorName"));
                    comment.setCreatedAt(toLocalDateTime(result.getTimestamp("CommentCreatedAt")));
                    current.addComment(comment);
                }
            }
        }
        return topics;
    }

    public boolean isTenantUserActive(int userId) throws SQLException {
        String sql = """
                SELECT 1 FROM Users u
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.CompanyId IS NOT NULL
                  AND u.IsActive = 1 AND u.IsLocked = 0
                  AND r.RoleCode IN ('COMPANY_ADMIN', 'MANAGER', 'EMPLOYEE')
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean topicExists(Connection connection, int topicId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM FaqTopics WHERE TopicId = ?")) {
            statement.setInt(1, topicId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public int createTopic(int authorUserId, String title, String content) throws SQLException {
        String sql = "INSERT INTO FaqTopics (Title, Content, AuthorUserId) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setNString(1, title);
            statement.setNString(2, content);
            statement.setInt(3, authorUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("FAQ topic was not created.");
    }

    public long createComment(int authorUserId, int topicId, String content) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            if (!topicExists(connection, topicId)) return -1;
            String sql = "INSERT INTO FaqComments (TopicId, Content, AuthorUserId) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, topicId);
                statement.setNString(2, content);
                statement.setInt(3, authorUserId);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) return keys.getLong(1);
                }
            }
        }
        throw new SQLException("FAQ comment was not created.");
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
