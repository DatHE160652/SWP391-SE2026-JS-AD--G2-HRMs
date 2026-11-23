package com.hrms.service.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FromStringTerm;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Verifies whether a recipient email address is deliverable before tenant provisioning.
 * Combines DNS MX lookup, optional SMTP RCPT TO probe, and Gmail IMAP bounce detection.
 */
public class EmailRecipientVerifier {
    private static final int MX_SOCKET_TIMEOUT_MS = 3000;
    private static final int IMAP_TIMEOUT_MS = 5000;
    private static final int MAX_BOUNCE_MESSAGES_TO_SCAN = 25;

    public record VerifyResult(boolean deliverable, String errorCode, String message) {
        static VerifyResult ok() {
            return new VerifyResult(true, null, null);
        }

        static VerifyResult undeliverable(String errorCode, String message) {
            return new VerifyResult(false, errorCode, message);
        }
    }

    public VerifyResult verify(String recipientEmail, boolean checkBounceInbox) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return VerifyResult.undeliverable("MAIL_RECIPIENT_INVALID", "Địa chỉ email không hợp lệ.");
        }
        String email = recipientEmail.trim().toLowerCase(Locale.ROOT);
        int atIndex = email.lastIndexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return VerifyResult.undeliverable("MAIL_RECIPIENT_INVALID", "Địa chỉ email không hợp lệ.");
        }
        String domain = email.substring(atIndex + 1);

        List<String> mxHosts;
        try {
            mxHosts = lookupMxHosts(domain);
        } catch (NameNotFoundException exception) {
            return VerifyResult.undeliverable("MAIL_DOMAIN_NOT_FOUND",
                    "Tên miền email '" + domain + "' không tồn tại.");
        } catch (NamingException exception) {
            mxHosts = List.of();
        }

        if (mxHosts != null && mxHosts.isEmpty()) {
            // Only reject when DNS explicitly succeeded with no MX/A mail host
            return VerifyResult.undeliverable("MAIL_DOMAIN_NOT_FOUND",
                    "Tên miền email '" + domain + "' không có máy chủ nhận thư.");
        }

        String senderAddress = resolveSenderAddress();
        if (mxHosts != null) {
            for (String mxHost : mxHosts) {
                VerifyResult probe = probeSmtpRecipient(mxHost, senderAddress, email);
                if (probe != null) {
                    return probe;
                }
            }
        }

        if (checkBounceInbox) {
            VerifyResult bounce = checkGmailBounceInbox(email);
            if (bounce != null) {
                return bounce;
            }
        }

        return VerifyResult.ok();
    }

    private String resolveSenderAddress() {
        try {
            return SmtpConfiguration.load().from;
        } catch (EmailDeliveryException exception) {
            return "noreply@hrms.local";
        }
    }

    private List<String> lookupMxHosts(String domain) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2500");
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        InitialDirContext dirContext = new InitialDirContext(env);
        try {
            Attributes attrs = dirContext.getAttributes(domain, new String[]{"MX"});
            Attribute mxAttr = attrs.get("MX");
            if (mxAttr != null && mxAttr.size() > 0) {
                record MxEntry(int priority, String host) {}
                List<MxEntry> entries = new ArrayList<>();
                NamingEnumeration<?> all = mxAttr.getAll();
                while (all.hasMore()) {
                    String raw = String.valueOf(all.next()).trim();
                    String[] parts = raw.split("\\s+", 2);
                    if (parts.length == 2) {
                        int prio = Integer.parseInt(parts[0]);
                        String host = parts[1].endsWith(".") ? parts[1].substring(0, parts[1].length() - 1) : parts[1];
                        entries.add(new MxEntry(prio, host));
                    }
                }
                entries.sort(Comparator.comparingInt(MxEntry::priority));
                List<String> hosts = new ArrayList<>();
                for (MxEntry entry : entries) {
                    hosts.add(entry.host());
                }
                if (!hosts.isEmpty()) {
                    return hosts;
                }
            }
            Attributes aAttrs = dirContext.getAttributes(domain, new String[]{"A"});
            Attribute aAttr = aAttrs.get("A");
            if (aAttr != null && aAttr.size() > 0) {
                return List.of(domain);
            }
            return List.of();
        } finally {
            dirContext.close();
        }
    }

    /**
     * Probes MX port 25 with RCPT TO. Returns undeliverable result if explicitly rejected (550/551/553),
     * or null if port 25 is blocked/inconclusive.
     */
    private VerifyResult probeSmtpRecipient(String mxHost, String senderAddress, String recipientEmail) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(mxHost, 25), MX_SOCKET_TIMEOUT_MS);
            socket.setSoTimeout(MX_SOCKET_TIMEOUT_MS);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);

            String greeting = readSmtpResponse(reader);
            if (!greeting.startsWith("220")) return null;

            writer.print("EHLO hrms.local\r\n");
            writer.flush();
            String ehlo = readSmtpResponse(reader);
            if (!ehlo.startsWith("250")) return null;

            writer.print("MAIL FROM:<" + senderAddress + ">\r\n");
            writer.flush();
            String mailFrom = readSmtpResponse(reader);
            if (!mailFrom.startsWith("250")) return null;

            writer.print("RCPT TO:<" + recipientEmail + ">\r\n");
            writer.flush();
            String rcptTo = readSmtpResponse(reader);

            writer.print("QUIT\r\n");
            writer.flush();

            if (rcptTo.startsWith("550") || rcptTo.startsWith("551") || rcptTo.startsWith("553")) {
                return VerifyResult.undeliverable("MAIL_RECIPIENT_NOT_FOUND",
                        "Địa chỉ email '" + recipientEmail + "' không tồn tại trên máy chủ thư.");
            }
            if (rcptTo.startsWith("250") || rcptTo.startsWith("251")) {
                return VerifyResult.ok();
            }
            return null;
        } catch (IOException exception) {
            // Port 25 outbound is frequently blocked on residential/cloud networks; treat as inconclusive.
            return null;
        }
    }

    private String readSmtpResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append('\n');
            if (line.length() < 4 || line.charAt(3) != '-') {
                break;
            }
        }
        return response.toString();
    }

    private VerifyResult checkGmailBounceInbox(String recipientEmail) {
        SmtpConfiguration config;
        try {
            config = SmtpConfiguration.load();
        } catch (EmailDeliveryException exception) {
            return null;
        }
        if (!"smtp.gmail.com".equalsIgnoreCase(config.host) || config.username.isBlank() || config.password.isBlank()) {
            return null;
        }

        Properties props = new Properties();
        props.setProperty("mail.imaps.host", "imap.gmail.com");
        props.setProperty("mail.imaps.port", "993");
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.connectiontimeout", String.valueOf(IMAP_TIMEOUT_MS));
        props.setProperty("mail.imaps.timeout", String.valueOf(IMAP_TIMEOUT_MS));

        Store store = null;
        Folder inbox = null;
        try {
            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com", config.username, config.password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] bounces = inbox.search(new FromStringTerm("mailer-daemon"));
            int start = Math.max(0, bounces.length - MAX_BOUNCE_MESSAGES_TO_SCAN);
            for (int i = bounces.length - 1; i >= start; i--) {
                Message msg = bounces[i];
                String content = extractText(msg).toLowerCase(Locale.ROOT);
                if (content.contains(recipientEmail) && isBounceFailureText(content)) {
                    return VerifyResult.undeliverable("MAIL_RECIPIENT_NOT_FOUND",
                            "Địa chỉ email '" + recipientEmail + "' không tồn tại (đã bị hệ thống thư trả lại).");
                }
            }
            return null;
        } catch (MessagingException | IOException exception) {
            return null;
        } finally {
            if (inbox != null && inbox.isOpen()) {
                try { inbox.close(false); } catch (MessagingException ignored) { }
            }
            if (store != null) {
                try { store.close(); } catch (MessagingException ignored) { }
            }
        }
    }

    private static boolean isBounceFailureText(String lowerText) {
        return lowerText.contains("550 5.1.1")
                || lowerText.contains("550-5.1.1")
                || lowerText.contains("address not found")
                || lowerText.contains("does not exist")
                || lowerText.contains("user unknown")
                || lowerText.contains("no such user");
    }

    private static String extractText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/*")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        }
        if (part.isMimeType("multipart/*")) {
            Object content = part.getContent();
            if (content instanceof Multipart multipart) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    sb.append(extractText(bodyPart)).append('\n');
                }
                return sb.toString();
            }
        }
        return "";
    }
}
