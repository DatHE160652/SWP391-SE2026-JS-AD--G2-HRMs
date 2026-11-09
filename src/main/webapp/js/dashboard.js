(function (root) {
    'use strict';
    const escape = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
    const colors = ['var(--lc-purple)', '#22b8a2', '#f6b94b', '#f07d97', '#8793db', '#aab3c7'];
    const labels = { ACTIVE: HrmI18n.t('Đang hiệu lực'), DRAFT: HrmI18n.t('Bản nháp'), EXPIRED: HrmI18n.t('Hết hạn'), TERMINATED: HrmI18n.t('Đã chấm dứt'), CANCELLED: HrmI18n.t('Đã hủy'), Male: HrmI18n.t('Nam'), Female: HrmI18n.t('Nữ'), Other: HrmI18n.t('Khác'), Pending: HrmI18n.t('Chờ duyệt'), Approved: HrmI18n.t('Đã duyệt'), Rejected: HrmI18n.t('Từ chối') };
    const count = value => Number.isFinite(Number(value)) ? Math.max(0, Number(value)) : 0;
    function donut(title, values, footer, context, systemLabels = false) {
        const entries = (values || []).filter(item => count(item.Total) > 0);
        const labelFor = item => item.LabelCode === 'UNASSIGNED' ? HrmI18n.t('Chưa phân công')
            : systemLabels ? labels[item.Label] || HrmI18n.t(item.Label) : item.Label;
        const total = entries.reduce((sum, item) => sum + count(item.Total), 0);
        let offset = 0;
        const slices = entries.map((item, index) => {
            const start = offset;
            offset += count(item.Total) / total * 100;
            return colors[index % colors.length] + ' ' + start + '% ' + offset + '%';
        });
        const description = entries.map(item => labelFor(item) + ': ' + count(item.Total)).join(', ');
        return '<section class="lc-card db-chart"><h3>' + escape(title) + '</h3><div class="db-donut-content"><div class="db-donut" role="img" aria-label="' + escape(title + ' — ' + (description || HrmI18n.t('Chưa có dữ liệu'))) + '" style="background:' + (total ? 'conic-gradient(' + slices.join(',') + ')' : 'var(--lc-line)') + '"><div><strong>' + total + HrmI18n.fragment('</strong><small>Tổng cộng</small></div></div><ul class="db-legend">') + (entries.length ? entries.map((item, index) => '<li><i style="background:' + colors[index % colors.length] + '"></i><span>' + escape(labelFor(item)) + '</span><b>' + count(item.Total) + '</b><small>' + Math.round(count(item.Total) / total * 100) + '%</small></li>').join('') : HrmI18n.fragment('<li>Chưa có dữ liệu</li>')) + '</ul></div>' + (footer ? '<a class="db-footer" href="' + escape(context + footer[0]) + '">' + footer[1] + ' <span>→</span></a>' : '') + '</section>';
    }
    function render(data, context) {
        const metric = (title, value, hint, tone, icon) => '<section class="lc-card db-metric"><div><span>' + title + '</span><strong>' + (value == null ? '—' : count(value)) + '</strong><small>' + escape(value == null ? HrmI18n.t('Chưa có tích hợp dữ liệu') : hint) + '</small></div><i class="db-metric-icon ' + tone + '" aria-hidden="true">' + icon + '</i></section>';
        const recent = data.recentLeave;
        return '<div class="db-metrics">' + metric(HrmI18n.t('Có mặt hôm nay'), data.attendance?.[0]?.Total, HrmI18n.t('Nhân viên đã chấm công'), 'green', '✓') + metric(HrmI18n.t('HĐ sắp hết hạn'), data.expiring?.[0]?.Total, HrmI18n.t('Trong vòng 90 ngày tới'), 'orange', '▤') + metric(HrmI18n.t('Đơn phép chờ duyệt'), data.pendingLeave?.[0]?.Total, HrmI18n.t('Đang chờ phê duyệt'), 'purple', '◷') + metric(HrmI18n.t('Tổng nhân viên'), data.employees?.[0]?.Total, count(data.employees?.[0]?.Active) + HrmI18n.t(' đang làm việc'), 'blue', '♙') + '</div>'
            + '<div class="db-two">' + donut(HrmI18n.t('Phân bổ nhân sự'), data.departments, ['/employee/employee-list', HrmI18n.t('Quản lý nhân viên')], context) + donut(HrmI18n.t('Trạng thái hợp đồng'), data.contracts, ['/lifecycle/contracts', HrmI18n.t('Quản lý hợp đồng')], context, true) + '</div>'
            + '<div class="db-three">' + donut(HrmI18n.t('Giới tính'), data.gender, null, context, true) + donut(HrmI18n.t('Độ tuổi'), data.age, null, context, true) + donut(HrmI18n.t('Thâm niên'), data.tenure, null, context, true) + '</div>'
            + HrmI18n.fragment('<section class="lc-card db-leave"><div class="db-card-heading"><h3>Đơn nghỉ gần đây</h3><a href="') + escape(context) + HrmI18n.fragment('/leave/all-requests">Xem tất cả →</a></div>') + (recent?.length ? HrmI18n.fragment('<div class="lc-table-wrap"><table><thead><tr><th>Nhân viên</th><th>Từ ngày</th><th>Đến ngày</th><th>Trạng thái</th></tr></thead><tbody>') + recent.map(item => '<tr><td>' + escape(item.FullName) + '</td><td>' + escape(item.StartDate) + '</td><td>' + escape(item.EndDate) + '</td><td><span class="lc-badge">' + escape(labels[item.Status] || item.Status) + '</span></td></tr>').join('') + '</tbody></table></div>' : '<div class="lc-empty">' + (recent ? HrmI18n.t('Chưa có đơn nghỉ phép nào') : HrmI18n.t('Phân hệ nghỉ phép chưa cung cấp dữ liệu')) + '</div>') + '</section>';
    }
    root.HrmDashboard = { render, donut };
    if (typeof module !== 'undefined') module.exports = root.HrmDashboard;
})(globalThis);
