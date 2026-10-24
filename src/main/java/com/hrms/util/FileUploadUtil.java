package com.hrms.util;

import com.hrms.service.FieldValidationException;

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
        java.nio.file.Path root = storageRoot();
        java.nio.file.Files.createDirectories(root);
        root = root.toRealPath();
        if(servletContext!=null && servletContext.getRealPath("/")!=null
                && root.startsWith(java.nio.file.Path.of(servletContext.getRealPath("/")).toRealPath()))
            throw new IOException("HRM_DOCUMENT_ROOT must be outside the public web root.");
        java.nio.file.Path directory = root.resolve(String.valueOf(employeeId));
        java.nio.file.Files.createDirectories(directory);
        directory = directory.toRealPath();
        if (!directory.startsWith(root)) throw new IOException("Thư mục lưu trữ không hợp lệ.");
        String savedFileName = java.util.UUID.randomUUID() + extension;
        java.nio.file.Path destination = directory.resolve(savedFileName);
        try (java.io.InputStream input = filePart.getInputStream()) {
            byte[] signature = input.readNBytes(8);
            if (!validSignature(extension, signature)) throw new FieldValidationException("file", "Nội dung tệp không phù hợp với phần mở rộng.");
            try (java.io.OutputStream output = java.nio.file.Files.newOutputStream(destination, java.nio.file.StandardOpenOption.CREATE_NEW)) {
                output.write(signature);
                byte[] buffer = new byte[8192];
                long total = signature.length;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_FILE_SIZE) throw new FieldValidationException("file", "Dung lượng tệp tối đa là 10 MB.");
                    output.write(buffer, 0, count);
                }
            }
            if (".docx".equals(extension)) {
                try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(destination.toFile())) {
                    if (archive.getEntry("[Content_Types].xml") == null || archive.getEntry("word/document.xml") == null)
                        throw new FieldValidationException("file", "Tệp tài liệu Word không hợp lệ.");
                }
            }
        } catch (IOException | RuntimeException exception) {
            java.nio.file.Files.deleteIfExists(destination);
            throw exception;
        }
        return new SavedFile(originalFileName, "private:" + employeeId + "/" + savedFileName, destination.toString());
    }

    public static File resolveStoredFile(String filePath, ServletContext servletContext) {
        if (filePath == null || filePath.isBlank()) return null;
        try {
            if (filePath.startsWith("private:")) {
                java.nio.file.Path root = storageRoot().toRealPath();
                java.nio.file.Path candidate = root.resolve(filePath.substring(8)).normalize();
                if (!candidate.startsWith(root)) throw new IllegalArgumentException("Đường dẫn tài liệu không hợp lệ.");
                candidate = candidate.toRealPath();
                if (!candidate.startsWith(root)) throw new IllegalArgumentException("Đường dẫn tài liệu không hợp lệ.");
                return candidate.toFile();
            }
            String legacy = filePath.replaceFirst("^/uploads/employees/", "/images/employees/");
            if (!legacy.startsWith("/images/employees/")) throw new IllegalArgumentException("Đường dẫn tài liệu cũ không được hỗ trợ.");
            String webRoot = servletContext.getRealPath("/images/employees");
            String resolved = servletContext.getRealPath(legacy);
            if (webRoot == null || resolved == null) return null;
            java.nio.file.Path root = java.nio.file.Path.of(webRoot).toRealPath();
            java.nio.file.Path candidate = java.nio.file.Path.of(resolved).toRealPath();
            if (!candidate.startsWith(root)) throw new IllegalArgumentException("Đường dẫn tài liệu không hợp lệ.");
            return candidate.toFile();
        } catch (IOException exception) { return null; }
    }

    private static java.nio.file.Path storageRoot() {
        String configured = System.getenv("HRM_DOCUMENT_ROOT");
        if (configured == null || configured.isBlank()) configured = System.getProperty("hrm.documents.root");
        if (configured == null || configured.isBlank())
            configured = java.nio.file.Path.of(System.getProperty("user.home"), ".hrms", "documents").toString();
        return java.nio.file.Path.of(configured).toAbsolutePath().normalize();
    }

    private static boolean validSignature(String extension, byte[] signature) {
        String hex = java.util.HexFormat.of().formatHex(signature);
        return switch (extension) {
            case ".pdf" -> hex.startsWith("255044462d");
            case ".png" -> hex.startsWith("89504e470d0a1a0a");
            case ".jpg" -> hex.startsWith("ffd8ff");
            case ".docx" -> hex.startsWith("504b0304");
            case ".doc" -> hex.startsWith("d0cf11e0a1b11ae1");
            default -> false;
        };
    }

    public static String contentType(String fileName) {
        if(fileName==null || fileName.isBlank()) return "application/octet-stream";
        return switch(getExtension(fileName)) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg" -> "image/jpeg";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private static void validate(Part filePart) {
        if (filePart == null || filePart.getSize() == 0) {
            throw new FieldValidationException("file", "Vui lòng chọn tệp tài liệu.");
        }
        if (filePart.getSize() > MAX_FILE_SIZE) {
            throw new FieldValidationException("file", "Dung lượng tệp không được vượt quá 10 MB.");
        }

        String fileName = getSubmittedFileName(filePart);
        String extension = getExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FieldValidationException("file", "Định dạng tệp không được hỗ trợ.");
        }
    }

    private static String getSubmittedFileName(Part filePart) {
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || fileName.isBlank()) {
            throw new FieldValidationException("file", "Vui lòng chọn tệp tài liệu.");
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
