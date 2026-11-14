<%@ include file="/WEB-INF/views/common/leave-header.jspf" %>
<div class="page-header">
    <div>
        <h1 class="page-title">Đơn nghỉ phép của tôi</h1>
        <p class="page-subtitle">Theo dõi trạng thái và chi tiết các đơn đã gửi.</p>
    </div>
    <div class="header-actions"><a class="button" href="submit">Tạo đơn mới</a></div>
</div>
<section class="panel">
    <div class="filter-bar" style="display:flex;flex-wrap:wrap;gap:12px;align-items:flex-end;margin-bottom:16px;">
        <div class="filter-group">
            <label for="statusFilter">Trạng thái</label>
            <select id="statusFilter">
                <option value="ALL">Tất cả</option>
                <option value="Pending">Chờ duyệt</option>
                <option value="Approved">Đã duyệt</option>
                <option value="Rejected">Từ chối</option>
                <option value="Cancelled">Đã hủy</option>
            </select>
        </div>
    </div>
    <p id="listMessage" role="alert"></p>
    <table>
        <thead>
        <tr>
            <th>Tên đơn</th>
            <th>Người tạo</th>
            <th style="width:220px;">Ngày nghỉ</th>
            <th style="width:90px;">Số ngày</th>
            <th style="width:130px;">Trạng thái</th>
        </tr>
        </thead>
        <tbody id="requestsTableBody"></tbody>
    </table>
    <div id="paginationBar" class="pagination-bar"></div>
</section>
<%@ include file="/WEB-INF/views/common/leave-footer.jspf" %>
