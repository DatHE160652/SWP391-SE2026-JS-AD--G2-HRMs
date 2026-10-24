package com.hrms.service.publicportal;

import com.hrms.dao.RegistrationRequestDAO;
import com.hrms.model.RegistrationRequest;
import com.hrms.model.publicportal.ConsultationSubmission;
import com.hrms.model.publicportal.ConsultationValidation;
import com.hrms.service.email.EmailDeliveryException;
import com.hrms.service.email.ReceiptEmailSender;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ConsultationRequestService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^0[0-9]{9}$");
    private static final Map<String, String> REGIONS = Map.of(
            "NORTH", "NORTH",
            "CENTRAL", "CENTRAL",
            "SOUTH", "SOUTH"
    );

    private final RegistrationRequestDAO dao;
    private final ReceiptEmailSender emailSender;

    public ConsultationRequestService() {
        this(new RegistrationRequestDAO(), new com.hrms.service.email.ConfiguredReceiptEmailSender());
    }

    public ConsultationRequestService(RegistrationRequestDAO dao, ReceiptEmailSender emailSender) {
        this.dao = dao;
        this.emailSender = emailSender;
    }

    public ConsultationValidation validate(String fullName, String email, String phone,
                                           String companyName, String region) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFullName(clean(fullName));
        request.setEmail(clean(email).toLowerCase(Locale.ROOT));
        request.setPhone(clean(phone));
        request.setCompanyName(normalizeCompanyName(companyName));
        request.setRegion(REGIONS.getOrDefault(clean(region), ""));

        Map<String, String> errors = new LinkedHashMap<>();
        if (request.getFullName().isBlank()) errors.put("fullName", "Họ và tên không được để trống hoặc chỉ chứa khoảng trắng.");
        else if (request.getFullName().length() > 150) errors.put("fullName", "Họ và tên không được vượt quá 150 ký tự.");
        if (request.getEmail().isBlank() || !EMAIL.matcher(request.getEmail()).matches()) {
            errors.put("email", "Vui lòng nhập email đúng định dạng.");
        } else if (request.getEmail().length() > 150) {
            errors.put("email", "Email không được vượt quá 150 ký tự.");
        }
        if (!PHONE.matcher(request.getPhone()).matches()) errors.put("phone", "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        if (request.getCompanyName().isBlank()) errors.put("companyName", "Tên công ty không được để trống.");
        else if (request.getCompanyName().length() > 200) errors.put("companyName", "Tên công ty không được vượt quá 200 ký tự.");
        if (request.getRegion().isBlank()) errors.put("region", "Vui lòng chọn một trong ba khu vực hợp lệ.");
        return errors.isEmpty() ? ConsultationValidation.valid(request) : ConsultationValidation.invalid(request, errors);
    }

    public ConsultationSubmission submit(String fullName, String email, String phone,
                                         String companyName, String region)
            throws InvalidConsultationException, DuplicateConsultationException, SQLException {
        ConsultationValidation validation = validate(fullName, email, phone, companyName, region);
        if (!validation.isValid()) throw new InvalidConsultationException(validation);

        RegistrationRequestDAO.InsertResult insert = dao.insertNew(validation.getRequest());
        if (insert == null) throw new DuplicateConsultationException();

        boolean emailSent = false;
        try {
            emailSender.send(insert.request());
            emailSent = true;
            markAttemptSafely(insert.emailAttemptId(), "ACCEPTED", null);
        } catch (EmailDeliveryException exception) {
            String status = "MAIL_PROVIDER_UNAVAILABLE".equals(exception.getCode())
                    ? "UNKNOWN" : "FAILED";
            markAttemptSafely(insert.emailAttemptId(), status, exception.getCode());
        } catch (RuntimeException exception) {
            markAttemptSafely(insert.emailAttemptId(), "UNKNOWN", "MAIL_PROVIDER_RUNTIME_ERROR");
        }
        return new ConsultationSubmission(insert.request(), insert.emailAttemptId(), emailSent);
    }

    private void markAttemptSafely(long attemptId, String status, String errorCode) {
        try {
            dao.markEmailAttempt(attemptId, status, errorCode);
        } catch (SQLException ignored) {
            // The request is committed; metadata failure must not change the public outcome.
        }
    }

    public RegistrationRequest insertWithoutEmail(String fullName, String email, String phone,
                                                   String companyName, String region)
            throws InvalidConsultationException, DuplicateConsultationException, SQLException {
        ConsultationValidation validation = validate(fullName, email, phone, companyName, region);
        if (!validation.isValid()) throw new InvalidConsultationException(validation);
        RegistrationRequestDAO.InsertResult insert = dao.insertNew(validation.getRequest());
        if (insert == null) throw new DuplicateConsultationException();
        return insert.request();
    }

    public static String normalizeCompanyName(String value) {
        return clean(value).replaceAll("\\s+", " ");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class InvalidConsultationException extends Exception {
        private final ConsultationValidation validation;
        public InvalidConsultationException(ConsultationValidation validation) {
            super("Consultation request validation failed.");
            this.validation = validation;
        }
        public ConsultationValidation getValidation() { return validation; }
    }

    public static final class DuplicateConsultationException extends Exception {
        public DuplicateConsultationException() {
            super("Email hoặc công ty này đã có yêu cầu đang được xử lý.");
        }
    }
}
