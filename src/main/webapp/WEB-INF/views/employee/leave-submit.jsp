<%@ include file="/WEB-INF/views/common/leave-header.jspf" %>
<div class="page-header">
    <div>
        <h1 class="page-title">Tạo đơn nghỉ phép</h1>
        <p class="page-subtitle">Chọn các ngày làm việc từ ngày mai.</p>
    </div>
    <div class="header-actions"><a href="my-requests" class="button secondary">Đơn của tôi</a></div>
</div>

<section class="panel">
    <form id="leaveSubmitForm">
        <p id="submitMessage" role="alert"></p>
        <p id="policyNotice"></p>

        <div class="form-grid">
            <div class="form-group-full">
                <label class="form-label" for="leaveDate">Chọn ngày nghỉ (Thứ Hai–Thứ Sáu)</label>
                <div style="display:flex;gap:10px;align-items:center;">
                    <input type="date" id="leaveDate" class="form-control">
                    <button type="button" id="addLeaveDate" class="button secondary" disabled>Thêm ngày</button>
                </div>
            </div>

            <div class="form-group-full">
                <label class="form-label">Ngày nghỉ đã chọn</label>
                <ul id="selectedDates" class="selected-dates" aria-label="Ngày nghỉ đã chọn"></ul>
                <p id="selectedDayCount" class="form-hint">Đã chọn 0 ngày</p>
            </div>

            <div class="form-group-full">
                <label class="form-label" for="reason">Lý do nghỉ <span class="required">*</span></label>
                <textarea id="reason" name="reason" class="form-control" required maxlength="500" rows="4"></textarea>
            </div>

            <div class="form-group-full">
                <p class="form-hint">Số ngày nghỉ chưa dùng hết trong tháng sẽ hết hạn.</p>
            </div>
        </div>

        <div class="form-actions">
            <a href="my-requests" class="button secondary">Hủy</a>
            <button type="submit" id="btnSubmitLeave" class="button" disabled>Gửi đơn nghỉ</button>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/views/common/leave-footer.jspf" %>
