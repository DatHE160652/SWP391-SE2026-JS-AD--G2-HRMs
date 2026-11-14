package com.hrms.i18n;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** UI preference only; never used to translate employee or tenant data. */
public final class Language {
    private static final Properties ENGLISH = loadEnglish();
    private Language() {}

    private static Properties loadEnglish() {
        try (InputStream input = Language.class.getResourceAsStream("/i18n/messages_en.xml")) {
            if (input == null) throw new IllegalStateException("Missing English UI catalogue: /i18n/messages_en.xml");
            Properties catalogue = new Properties();
            // Properties.loadFromXML on Java 17 rejects supplementary Unicode characters.
            // The standard XML parser supports them; external resources remain disabled.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var entries = factory.newDocumentBuilder().parse(input).getElementsByTagName("entry");
            for (int index = 0; index < entries.getLength(); index++) {
                var entry = (org.w3c.dom.Element) entries.item(index);
                catalogue.setProperty(entry.getAttribute("key"), entry.getTextContent());
            }
            return catalogue;
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            throw new IllegalStateException("Cannot load English UI catalogue", exception);
        }
    }

    public static String selected(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) {
            if ("hrms.language".equals(cookie.getName())) return "en".equals(cookie.getValue()) ? "en" : "vi";
        }
        return "vi";
    }

    public static String text(HttpServletRequest request, String key, String vietnamese) {
        String value = vietnamese;
        if ("en".equals(selected(request))) {
            value = ENGLISH.getProperty(key, value);
        }
        value = value.replace("&amp;", "&").replace("&nbsp;", "\u00a0")
                .replace("&larr;", "←").replace("&rarr;", "→");
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String message(HttpServletRequest request, Object source) {
        if (source == null) return null;
        String value = source.toString();
        if (!"en".equals(selected(request))) return value;
        try {
            String normalized = value.strip().replaceAll("\\s+", " ");
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8))).substring(0, 12);
            return ENGLISH.getProperty("ui_" + digest, value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String fieldMessage(HttpServletRequest request, Object errors, String field) {
        return message(request, errors instanceof Map<?, ?> map ? map.get(field) : null);
    }

    public static String label(HttpServletRequest request, Object code) {
        if (code == null) return null;
        String value = switch (code.toString()) {
            case "Draft" -> "Bản nháp";
            case "Calculated" -> "Đã tính";
            case "Reviewed" -> "Đã rà soát";
            case "Approved" -> "Đã phê duyệt";
            default -> code.toString();
        };
        return message(request, value);
    }
}
