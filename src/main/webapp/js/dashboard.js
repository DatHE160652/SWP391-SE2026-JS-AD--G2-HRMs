(function (root) {
    'use strict';
    const escape = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
    const colors = ['var(--lc-purple)', '#22b8a2', '#f6b94b', '#f07d97', '#8793db', '#aab3c7'];
    const labels = { ACTIVE: 'Đang hiệu lực', DRAFT: 'Bản nháp', EXPIRED: 'Hết hạn', TERMINATED: 'Đã chấm dứt', CANCELLED: 'Đã hủy', Male: 'Nam', Female: 'Nữ', Other: 'Khác', Pending: 'Chờ duyệt', Approved: 'Đã duyệt', Rejected: 'Từ chối' };
    const count = value => Number.isFinite(Number(value)) ? Math.max(0, Number(value)) : 0;
    function donut(title, values, footer, context) {
        const entries = (values || []).filter(item => count(item.Total) > 0);
        const total = entries.reduce((sum, item) => sum + count(item.Total), 0);
        let offset = 0;
        const slices = entries.map((item, index) => {
            const start = offset;
            offset += count(item.Total) / total * 100;
            return colors[index % colors.length] + ' ' + start + '% ' + offset + '%';
        });
        const description = entries.map(item => (labels[item.Label] || item.Label) + ': ' + count(item.Total)).join(', ');
        return '<section class="lc-card db-chart"><h3>' + escape(title) + '</h3><div class="db-donut-content"><div class="db-donut" role="img" aria-label="' + escape(title + ' — ' + (description || 'Chưa có dữ liệu')) + '" style="background:' + (total ? 'conic-gradient(' + slices.join(',') + ')' : 'var(--lc-line)') + '"><div><strong>' + total + '</strong><small>Tổng cộng</small></div></div><ul class="db-legend">' + (entries.length ? entries.map((item, index) => '<li><i style="background:' + colors[index % colors.length] + '"></i><span>' + escape(labels[item.Label] || item.Label) + '</span><b>' + count(item.Total) + '</b><small>' + Math.round(count(item.Total) / total * 100) + '%</small></li>').join('') : '<li>Chưa có dữ liệu</li>') + '</ul></div>' + (footer ? '<a class="db-footer" href="' + escape(context + footer[0]) + '">' + footer[1] + ' <span>→</span></a>' : '') + '</section>';
    }
    function render(data, context) {
        const metric = (title, value, hint, tone, icon) => '<section class="lc-card db-metric"><div><span>' + title + '</span><strong>' + (value == null ? '—' : count(value)) + '</strong><small>' + escape(value == null ? 'Chưa có tích hợp dữ liệu' : hint) + '</small></div><i class="db-metric-icon ' + tone + '" aria-hidden="true">' + icon + '</i></section>';
        const recent = data.recentLeave;
        return '<div class="db-metrics">' + metric('Có mặt hôm nay', data.attendance?.[0]?.Total, 'Nhân viên đã chấm công', 'green', '✓') + metric('HĐ sắp hết hạn', data.expiring?.[0]?.Total, 'Trong vòng 90 ngày tới', 'orange', '▤') + metric('Đơn phép chờ duyệt', data.pendingLeave?.[0]?.Total, 'Đang chờ phê duyệt', 'purple', '◷') + metric('Tổng nhân viên', data.employees?.[0]?.Total, count(data.employees?.[0]?.Active) + ' đang làm việc', 'blue', '♙') + '</div>'
            + '<section class="lc-card db-payroll"><div class="db-card-heading"><div><h3>Diễn biến quỹ lương</h3><small>Lương trước khấu trừ &amp; thực nhận · 6 tháng gần nhất</small></div><div class="db-payroll-legend"><span>● Thực nhận</span><span>● Trước khấu trừ</span></div></div><div class="db-payroll-empty"><span>Chưa có dữ liệu quỹ lương trước khấu trừ / thực nhận đã duyệt</span><small>Số liệu sẽ hiển thị khi phân hệ bảng lương cung cấp dữ liệu. Không tự suy tính lương thực nhận từ hợp đồng.</small></div></section>'
            + '<div class="db-two">' + donut('Phân bổ nhân sự', data.departments, ['/employee/employee-list', 'Quản lý nhân viên'], context) + donut('Trạng thái hợp đồng', data.contracts, ['/lifecycle/contracts', 'Quản lý hợp đồng'], context) + '</div>'
            + '<div class="db-three">' + donut('Giới tính', data.gender, null, context) + donut('Độ tuổi', data.age, null, context) + donut('Thâm niên', data.tenure, null, context) + '</div>'
            + '<section class="lc-card db-leave"><div class="db-card-heading"><h3>Đơn nghỉ gần đây</h3><a href="' + escape(context) + '/leave/all-requests">Xem tất cả →</a></div>' + (recent?.length ? '<div class="lc-table-wrap"><table><thead><tr><th>Nhân viên</th><th>Từ ngày</th><th>Đến ngày</th><th>Trạng thái</th></tr></thead><tbody>' + recent.map(item => '<tr><td>' + escape(item.FullName) + '</td><td>' + escape(item.StartDate) + '</td><td>' + escape(item.EndDate) + '</td><td><span class="lc-badge">' + escape(labels[item.Status] || item.Status) + '</span></td></tr>').join('') + '</tbody></table></div>' : '<div class="lc-empty">' + (recent ? 'Chưa có đơn nghỉ phép nào' : 'Phân hệ nghỉ phép chưa cung cấp dữ liệu') + '</div>') + '</section>';
    }
    root.HrmDashboard = { render, donut };
    if (typeof module !== 'undefined') module.exports = root.HrmDashboard;
})(globalThis);
