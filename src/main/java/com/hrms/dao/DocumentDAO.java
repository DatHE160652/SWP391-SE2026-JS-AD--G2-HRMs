package com.hrms.dao;

import com.hrms.model.EmployeeDocument;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {
    public List<EmployeeDocument> getDocumentsByEmployee(int employeeId) throws SQLException {
        ensureTableExists();
        String sql = """
                SELECT DocumentId, EmployeeId, DocumentName, DocumentType, FileName, FilePath,
                       UploadedBy, UploadedDate, Status, MimeType, FileSizeBytes
                FROM EmployeeDocuments
                WHERE EmployeeId = ?
                ORDER BY UploadedDate DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EmployeeDocument> documents = new ArrayList<>();
                while (resultSet.next()) {
                    documents.add(mapDocument(resultSet));
                }
                return documents;
            }
        }
    }

    public EmployeeDocument getDocumentById(int documentId) throws SQLException {
        ensureTableExists();
        String sql = """
                SELECT DocumentId, EmployeeId, DocumentName, DocumentType, FileName, FilePath,
                       UploadedBy, UploadedDate, Status, MimeType, FileSizeBytes
                FROM EmployeeDocuments
                WHERE DocumentId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapDocument(resultSet) : null;
            }
        }
    }

    public boolean insertDocument(EmployeeDocument document) throws SQLException {
        return insertDocument(document, null, null);
    }

    public boolean insertDocument(EmployeeDocument document, String mimeType, Long size) throws SQLException {
        ensureTableExists();
        try (Connection connection=DBConnection.getConnection()) {
            return insertDocument(connection,document,mimeType,size);
        }
    }

    public boolean insertDocument(Connection connection,EmployeeDocument document,String mimeType,Long size) throws SQLException {
        String sql = """
                INSERT INTO EmployeeDocuments (
                    EmployeeId, DocumentName, DocumentType, FileName, FilePath, UploadedBy, Status, MimeType, FileSizeBytes, DocumentPeriodId
                )
                VALUES (?, ?, ?, ?, ?, ?, 'Active', ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql,java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, document.getEmployeeId());
            statement.setString(2, document.getDocumentName());
            statement.setString(3, document.getDocumentType());
            statement.setString(4, document.getFileName());
            statement.setString(5, document.getFilePath());
            statement.setString(7, mimeType);
            statement.setObject(8, size, java.sql.Types.BIGINT);
            statement.setObject(9, document.getDocumentPeriodId(), java.sql.Types.BIGINT);
            if (document.getUploadedBy() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, document.getUploadedBy());
            }
            if(statement.executeUpdate()!=1) return false;
            try(ResultSet keys=statement.getGeneratedKeys()) {
                if(!keys.next()) throw new SQLException("Chưa nhận được mã tài liệu.");
                document.setDocumentId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean setDocumentActive(int documentId, boolean active) throws SQLException {
        ensureTableExists();
        String sql = "UPDATE EmployeeDocuments SET Status = ? WHERE DocumentId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, active ? "Active" : "Inactive");
            statement.setInt(2, documentId);
            return statement.executeUpdate() > 0;
        }
    }

    private EmployeeDocument mapDocument(ResultSet resultSet) throws SQLException {
        EmployeeDocument document = new EmployeeDocument();
        document.setDocumentId(resultSet.getInt("DocumentId"));
        document.setEmployeeId(resultSet.getInt("EmployeeId"));
        document.setDocumentName(resultSet.getString("DocumentName"));
        document.setDocumentType(resultSet.getString("DocumentType"));
        document.setFileName(resultSet.getString("FileName"));
        document.setFilePath(resultSet.getString("FilePath"));
        int uploadedBy = resultSet.getInt("UploadedBy");
        document.setUploadedBy(resultSet.wasNull() ? null : uploadedBy);
        document.setUploadedDate(resultSet.getTimestamp("UploadedDate"));
        document.setStatus(resultSet.getString("Status"));
        document.setMimeType(resultSet.getString("MimeType"));
        document.setFileSizeBytes((Long)resultSet.getObject("FileSizeBytes"));
        return document;
    }

    private void ensureTableExists() throws SQLException {
        String sql = """
                IF OBJECT_ID('dbo.EmployeeDocuments', 'U') IS NULL
                    THROW 51000, 'EmployeeDocuments is missing. Apply the document migration.', 1;
                IF COL_LENGTH('dbo.EmployeeDocuments', 'DocumentPeriodId') IS NULL
                    ALTER TABLE dbo.EmployeeDocuments ADD DocumentPeriodId BIGINT NULL;
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
