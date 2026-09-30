package com.hrms.util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

public final class FileUploadUtil {
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".jpg", ".png");

    private FileUploadUtil() {
    }

    public static SavedFile saveUploadedFile(Part filePart, int employeeId, ServletContext servletContext)
            throws IOException {
        validate(filePart);

        String originalFileName = sanitizeFileName(getSubmittedFileName(filePart));
        String extension = getExtension(originalFileName);
        String baseName = originalFileName.substring(0, originalFileName.length() - extension.length());
        String savedFileName = baseName + "-" + System.currentTimeMillis() + extension;
        String relativeDirectory = "/uploads/employees/" + employeeId;
        String absoluteDirectory = servletContext.getRealPath(relativeDirectory);

        if (absoluteDirectory == null) {
            absoluteDirectory = Paths.get(System.getProperty("user.dir"), "uploads", "employees",
                    String.valueOf(employeeId)).toString();
            relativeDirectory = absoluteDirectory;
        }

        File uploadDirectory = new File(absoluteDirectory);
        if (!uploadDirectory.exists() && !uploadDirectory.mkdirs()) {
            throw new IOException("Could not create upload directory.");
        }

        File savedFile = new File(uploadDirectory, savedFileName);
        filePart.write(savedFile.getAbsolutePath());

        return new SavedFile(originalFileName, relativeDirectory + "/" + savedFileName, savedFile.getAbsolutePath());
    }

    public static File resolveStoredFile(String filePath, ServletContext servletContext) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        if (filePath.startsWith("/")) {
            String absolutePath = servletContext.getRealPath(filePath);
            if (absolutePath != null) {
                return new File(absolutePath);
            }
        }

        return new File(filePath);
    }

    private static void validate(Part filePart) {
        if (filePart == null || filePart.getSize() == 0) {
            throw new IllegalArgumentException("Please select a document");
        }
        if (filePart.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }

        String fileName = getSubmittedFileName(filePart);
        String extension = getExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type is not supported");
        }
    }

    private static String getSubmittedFileName(Part filePart) {
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Please select a document");
        }
        return fileName;
    }

    private static String sanitizeFileName(String fileName) {
        String sanitized = Paths.get(fileName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "document" : sanitized;
    }

    private static String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    public static class SavedFile {
        private final String originalFileName;
        private final String relativePath;
        private final String absolutePath;

        public SavedFile(String originalFileName, String relativePath, String absolutePath) {
            this.originalFileName = originalFileName;
            this.relativePath = relativePath;
            this.absolutePath = absolutePath;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getAbsolutePath() {
            return absolutePath;
        }
    }
}
