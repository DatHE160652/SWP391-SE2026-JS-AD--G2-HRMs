package com.hrms.service.publicportal;

import com.hrms.dao.publicportal.FaqDAO;
import com.hrms.model.publicportal.FaqTopic;

import com.hrms.service.FieldValidationException;

import java.sql.SQLException;
import java.util.List;

public class FaqService {
    private final FaqDAO faqDAO;

    public FaqService() { this(new FaqDAO()); }
    public FaqService(FaqDAO faqDAO) { this.faqDAO = faqDAO; }

    public List<FaqTopic> getTopics() throws SQLException {
        List<FaqTopic> topics = faqDAO.findTopicsWithComments();
        return topics.isEmpty() ? defaultTopics() : topics;
    }

    public boolean isTenantUserActive(int userId) throws SQLException {
        return faqDAO.isTenantUserActive(userId);
    }

    public void validateText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new FieldValidationException("Tiêu đề".equals(field) ? "title" : "content", field + " không được để trống.");
        }
        if (value.trim().length() > 10000) {
            throw new FieldValidationException("Tiêu đề".equals(field) ? "title" : "content", field + " không được vượt quá 10.000 ký tự.");
        }
    }

    public int createTopic(int authorUserId, String title, String content) throws SQLException {
        validateText(title, "Tiêu đề");
        validateText(content, "Nội dung");
        return faqDAO.createTopic(authorUserId, title.trim(), content.trim());
    }

    public long createComment(int authorUserId, int topicId, String content) throws SQLException {
        if (topicId <= 0) throw new IllegalArgumentException("Chủ đề không hợp lệ.");
        validateText(content, "Bình luận");
        long id = faqDAO.createComment(authorUserId, topicId, content.trim());
        if (id < 0) throw new IllegalArgumentException("Không tìm thấy chủ đề.");
        return id;
    }

    private List<FaqTopic> defaultTopics() {
        return List.of(
                defaultTopic("HRMs Cloud cung cấp những phân hệ nào?",
                        "Nền tảng gồm hồ sơ nhân sự số, chấm công, tính lương và cổng tự phục vụ nhân viên."),
                defaultTopic("Gói tham chiếu có giá bao nhiêu?",
                        "Cấu hình tham chiếu là 59.000 đồng cho mỗi công ty mỗi tháng, không yêu cầu chọn nhiều plan."),
                defaultTopic("Gửi yêu cầu tư vấn có tạo tài khoản ngay không?",
                        "Không. Form chỉ tạo Registration Request ở trạng thái NEW; Company, User và Subscription chỉ được tạo sau khi System Admin confirm."),
                defaultTopic("Khách chưa đăng nhập có thể làm gì trên FAQ?",
                        "Khách có thể xem chủ đề và bình luận. Muốn tạo chủ đề hoặc bình luận, người dùng cần đăng nhập Tenant Portal."),
                defaultTopic("Một Company có tối đa bao nhiêu tài khoản?",
                        "Cấu hình tham chiếu hỗ trợ tối đa 20 tài khoản, gồm một Company Admin và tối đa ba Manager theo chính sách nền tảng.")
        );
    }

    private FaqTopic defaultTopic(String title, String content) {
        FaqTopic topic = new FaqTopic();
        topic.setTitle(title);
        topic.setContent(content);
        topic.setAuthorName("HRMs Cloud");
        return topic;
    }
}
