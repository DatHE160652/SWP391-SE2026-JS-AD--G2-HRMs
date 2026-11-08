<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_30d1e2eec84b", "Tạo đơn xin nghỉ phép - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-submit">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span><%= com.hrms.i18n.Language.text(request, "ui_c42485146656", "Cổng nhân viên") %></span></div>
        <div class="nav-label"><%= com.hrms.i18n.Language.text(request, "ui_0a2ac8dcdfc1", "Chức năng cá nhân") %></div>
        <a class="nav-link" href="../employee/my-profile"><%= com.hrms.i18n.Language.text(request, "ui_b70d1e2772e9", "Hồ sơ cá nhân") %></a>
        <a class="nav-link" href="../attendance/employee-attendance"><%= com.hrms.i18n.Language.text(request, "ui_5d058034330c", "Chấm công cá nhân") %></a>

        <div class="nav-label" style="margin-top: 18px;"><%= com.hrms.i18n.Language.text(request, "ui_9d21ce1caf72", "Nghỉ phép") %></div>
        <a class="nav-link" href="my-balances"><%= com.hrms.i18n.Language.text(request, "ui_35400d7d055b", "Số dư phép") %></a>
        <a class="nav-link active" href="submit"><%= com.hrms.i18n.Language.text(request, "ui_970a42dcb266", "Tạo đơn xin nghỉ") %></a>
        <a class="nav-link" href="my-requests"><%= com.hrms.i18n.Language.text(request, "ui_697728531b43", "Lịch sử đơn nghỉ") %></a>
        <a class="nav-link" href="calendar"><%= com.hrms.i18n.Language.text(request, "ui_bcfc74d75019", "Lịch nghỉ cá nhân") %></a>

        <a class="nav-link logout-link" href="../logout"><%= com.hrms.i18n.Language.text(request, "ui_d513aac0be87", "Đăng xuất") %></a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_45d6c3bb586e", "Đơn xin nghỉ phép") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_987c71347ee6", "Điền thông tin khoảng thời gian và lý do xin nghỉ. Đơn sẽ được chuyển tới Quản lý trực tiếp để xét duyệt.") %></p>
            </div>
            <div class="header-actions">
                <a href="my-requests" class="button secondary" style="text-decoration: none;"><%= com.hrms.i18n.Language.text(request, "ui_c6d3c2e7f3c1", "Xem lịch sử đơn") %></a>
            </div>
        </div>

        <section class="panel" style="max-width: 760px; margin: 0 auto 30px;">
            <form id="leaveSubmitForm">
                <div id="submitMessage" role="alert" style="display:none"></div>
                <div class="form-grid">
                    <div class="form-group-full">
                        <label class="form-label" for="leaveTypeId"><%= com.hrms.i18n.Language.text(request, "ui_1389e04eccd7", "Loại nghỉ phép") %> <span style="color:#b42318;">*</span></label>
                        <select id="leaveTypeId" name="leaveTypeId" class="form-control" required>
                            <option value=""><%= com.hrms.i18n.Language.text(request, "ui_1f1d7008912f", "-- Đang tải danh sách loại nghỉ phép... --") %></option>
                        </select>
                        <div id="balanceNotice" style="margin-top: 6px; font-size: 12px;"></div>
                    </div>

                    <div>
                        <label class="form-label" for="startDate"><%= com.hrms.i18n.Language.text(request, "ui_abb3e1eea3cd", "Từ ngày (bắt đầu)") %> <span style="color:#b42318;">*</span></label>
                        <input type="date" id="startDate" name="startDate" class="form-control" required>
                    </div>

                    <div>
                        <label class="form-label" for="endDate"><%= com.hrms.i18n.Language.text(request, "ui_edaa40955ae2", "Đến ngày (kết thúc)") %> <span style="color:#b42318;">*</span></label>
                        <input type="date" id="endDate" name="endDate" class="form-control" required>
                    </div>

                    <!-- Live Calculation Result Callout -->
                    <div class="form-group-full">
                        <div id="calcBanner" class="calc-banner" style="display: none;">
                            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"></path>
                            </svg>
                            <div class="calc-text">
                                <%= com.hrms.i18n.Language.text(request, "ui_83c6f3c6b2cc", "Số ngày làm việc tính toán:") %> <strong><span id="calcWorkingDays">0</span> <%= com.hrms.i18n.Language.text(request, "ui_8ccdd0407810", "ngày") %></strong> <%= com.hrms.i18n.Language.text(request, "ui_80219d6017ae", "(đã trừ Thứ 7, Chủ Nhật)") %>
                            </div>
                        </div>

                        <!-- Cross-Year Proposal Banner -->
                        <div id="crossYearBanner" class="calc-banner info" style="display: none; margin-top: 10px; background: #eff8ff; border: 1px solid #b2ddff; color: #175cd3; padding: 14px 16px; border-radius: 8px;">
                            <svg width="22" height="22" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0; margin-top: 2px;">
                                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
                            </svg>
                            <div style="font-size: 13px; line-height: 1.5;">
                                <strong><%= com.hrms.i18n.Language.text(request, "ui_8cf0e2b2d238", "Đề xuất tự động tách đơn theo năm tài chính:") %></strong><br>
                                <span id="crossYearText"><%= com.hrms.i18n.Language.text(request, "ui_176b35e5c22d", "Khoảng thời gian xin nghỉ vắt qua 2 năm. Hệ thống đề xuất tách thành các đơn riêng theo từng năm tài chính:") %></span>
                                <div id="crossYearDetails" style="margin-top: 8px; font-weight: 500;"></div>
                            </div>
                        </div>
                    </div>

                    <div class="form-group-full">
                        <label class="form-label" for="reason"><%= com.hrms.i18n.Language.text(request, "ui_c60a93281be1", "Lý do xin nghỉ") %> <span style="color:#b42318;">*</span></label>
                        <textarea id="reason" name="reason" data-validation-label="lý do nghỉ phép" class="form-control" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_cce8034545e2", "Mô tả lý do xin nghỉ phép...") %>" required></textarea>
                    </div>
                </div>

                <div style="margin-top: 24px; display: flex; justify-content: flex-end; gap: 12px;">
                    <a href="my-requests" class="button secondary" style="text-decoration: none;"><%= com.hrms.i18n.Language.text(request, "ui_247d4b1efe9c", "Hủy bỏ") %></a>
                    <button type="submit" id="btnSubmitLeave" class="button"><%= com.hrms.i18n.Language.text(request, "ui_24bdcb0f760d", "Nộp đơn xin nghỉ") %></button>
                </div>
            </form>
        </section>
    </main>
</div>



<script src="../js/leave.js?v=<%= application.getResource("/js/leave.js").openConnection().getLastModified() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initSubmitForm();
    });
</script>
</body>
</html>
