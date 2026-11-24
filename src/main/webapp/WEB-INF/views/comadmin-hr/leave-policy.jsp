<%@ include file="/WEB-INF/views/common/leave-header.jspf" %>
<div class="page-header">
    <div>
        <h1 class="page-title">Cấu hình quota nghỉ phép</h1>
        <p class="page-subtitle">Thiết lập hạn mức nghỉ phép chung cho nhân viên.</p>
    </div>
</div>
<div id="policyMessage" class="message" role="status"></div>
<section class="panel">
    <form id="leavePolicyForm">
        <div class="form-grid">
            <div class="form-group-full">
                <label class="form-label" for="quotaMode">Cấp quota theo</label>
                <select id="quotaMode" name="quotaMode" class="form-control">
                    <option value="MONTHLY">Tháng</option>
                    <option value="ANNUAL">Năm</option>
                </select>
            </div>
            <div class="form-group-full">
                <label class="form-label" for="quotaDays">Số ngày nghỉ</label>
                <input type="number" id="quotaDays" name="quotaDays" class="form-control" min="0" max="366" step="1" required>
            </div>
            <div class="form-group-full">
                <p id="allocationPreview"></p>
                <p>Áp dụng riêng cho từng nhân viên. Quota năm chia đều theo tháng; phần dư cộng lần lượt mỗi tháng 1 ngày từ tháng 1. Ngày chưa dùng trong tháng sẽ hết hạn.</p>
                <p>Thay đổi chính sách áp dụng cho các đơn mới và đơn chưa duyệt. Các ngày đã được duyệt vẫn được giữ và tính vào số ngày đã dùng.</p>
            </div>
        </div>
        <div style="margin-top:24px;display:flex;justify-content:flex-end;">
            <button class="button" type="submit">Lưu cấu hình</button>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/views/common/leave-footer.jspf" %>
