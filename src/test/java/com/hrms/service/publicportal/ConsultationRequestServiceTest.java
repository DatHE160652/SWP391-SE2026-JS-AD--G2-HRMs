package com.hrms.service.publicportal;

import com.hrms.dao.RegistrationRequestDAO;
import com.hrms.model.RegistrationRequest;
import com.hrms.model.publicportal.ConsultationSubmission;
import com.hrms.model.publicportal.ConsultationValidation;
import com.hrms.service.email.EmailDeliveryException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConsultationRequestServiceTest {
    private final ConsultationRequestService service = new ConsultationRequestService(null, request -> { });

    @Test
    public void normalizesFiveFieldsBeforePersistence() {
        ConsultationValidation validation = service.validate(
                "  Nguyễn   Minh Anh  ", "  USER@Example.COM ", " 0901234567 ",
                "  Công ty   Sao   Bắc ", "NORTH");

        assertTrue(validation.isValid());
        assertEquals("Nguyễn   Minh Anh", validation.getRequest().getFullName());
        assertEquals("user@example.com", validation.getRequest().getEmail());
        assertEquals("0901234567", validation.getRequest().getPhone());
        assertEquals("Công ty Sao Bắc", validation.getRequest().getCompanyName());
        assertEquals("NORTH", validation.getRequest().getRegion());
    }

    @Test
    public void rejectsInvalidPhoneAndRegion() {
        ConsultationValidation validation = service.validate(
                "Nguyễn Minh Anh", "user@example.com", "091234567", "Công ty Sao Bắc", "WEST");

        assertFalse(validation.isValid());
        assertTrue(validation.getErrors().containsKey("phone"));
        assertTrue(validation.getErrors().containsKey("region"));
    }

    @Test
    public void rejectsBlankNamesAndMalformedEmail() {
        ConsultationValidation validation = service.validate("  ", "not-an-email", "0901234567", " ", "SOUTH");

        assertFalse(validation.isValid());
        assertTrue(validation.getErrors().containsKey("fullName"));
        assertTrue(validation.getErrors().containsKey("email"));
        assertTrue(validation.getErrors().containsKey("companyName"));
    }

    @Test
    public void acceptsAllThreeRegionCodes() {
        for (String region : new String[]{"NORTH", "CENTRAL", "SOUTH"}) {
            assertTrue(service.validate("Nguyễn Minh Anh", "user@example.com", "0901234567",
                    "Công ty Sao Bắc", region).isValid());
        }
    }

    @Test
    public void emailFailureDoesNotUndoCommittedRequest() throws Exception {
        RecordingDAO dao = new RecordingDAO();
        ConsultationRequestService failingService = new ConsultationRequestService(dao,
                request -> { throw new EmailDeliveryException("MAIL_PROVIDER_REJECTED", "rejected"); });

        ConsultationSubmission result = failingService.submit(
                "Nguyễn Minh Anh", "user@example.com", "0901234567", "Công ty Sao Bắc", "NORTH");

        assertNotNull(result);
        assertFalse(result.isEmailSent());
        assertEquals("NEW", result.getRequest().getStatus());
        assertEquals("FAILED", dao.lastStatus);
        assertEquals("MAIL_PROVIDER_REJECTED", dao.lastErrorCode);
    }

    @Test
    public void unavailableEmailIsRecordedUnknownAndStillSucceeds() throws Exception {
        RecordingDAO dao = new RecordingDAO();
        ConsultationRequestService failingService = new ConsultationRequestService(dao,
                request -> { throw new EmailDeliveryException("MAIL_PROVIDER_UNAVAILABLE", "unavailable"); });

        ConsultationSubmission result = failingService.submit(
                "Nguyễn Minh Anh", "user@example.com", "0901234567", "Công ty Sao Bắc", "CENTRAL");

        assertNotNull(result);
        assertFalse(result.isEmailSent());
        assertEquals("UNKNOWN", dao.lastStatus);
        assertEquals("MAIL_PROVIDER_UNAVAILABLE", dao.lastErrorCode);
    }

    private static final class RecordingDAO extends RegistrationRequestDAO {
        private String lastStatus;
        private String lastErrorCode;

        @Override
        public InsertResult insertNew(RegistrationRequest request) {
            request.setRegistrationRequestId(42);
            request.setStatus("NEW");
            return new InsertResult(request, 99L);
        }

        @Override
        public void markEmailAttempt(long attemptId, String status, String errorCode) {
            lastStatus = status;
            lastErrorCode = errorCode;
        }
    }
}
