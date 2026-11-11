<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="csrf-token" content="<c:out value='${payrollCsrf}'/>">
    <title>Thiết lập đợt thưởng · HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payroll.css?v=<%= application.getResource("/css/payroll.css").openConnection().getLastModified() %>">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
    <style>
        .bonus-table-input {
            width: 140px;
            padding: 8px 10px;
            text-align: right;
            font-weight: 600;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-size: 13px;
        }
        .bonus-table-input:focus {
            border-color: var(--blue);
            outline: 2px solid #bfdbfe;
            background: #f8fafc;
        }
        .bonus-note-input {
            width: 100%;
            min-width: 200px;
            padding: 8px 10px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-size: 13px;
        }
        .bonus-note-input:focus {
            border-color: var(--blue);
            outline: 2px solid #bfdbfe;
        }
        .sticky-action-bar {
            position: sticky;
            bottom: 12px;
            background: #ffffff;
            border: 1px solid #cbd5e1;
            border-radius: 12px;
            padding: 16px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
            z-index: 50;
            margin-top: 24px;
        }
        .preset-badge-btn {
            padding: 5px 10px;
            font-size: 12px;
            border-radius: 6px;
            background: #f1f5f9;
            border: 1px solid #cbd5e1;
            color: #334155;
            cursor: pointer;
            font-weight: 500;
            transition: all 0.15s ease;
        }
        .preset-badge-btn:hover, .preset-badge-btn.active {
            background: var(--blue);
            color: #ffffff;
            border-color: var(--blue);
        }
        .tenure-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
            background: #e0f2fe;
            color: #0369a1;
        }
        .tenure-pill.full {
            background: #dcfce7;
            color: #15803d;
        }
    </style>
</head>
<body class="lc workspace-payroll" data-page="bonus-campaign" data-context="${pageContext.request.contextPath}">
<div class="pay-shell">
    <%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>
    <main class="lc-main pay-main">
        <div class="pay-content">
            <div class="page-heading">
                <div>
                    <div class="eyebrow">CHÍNH SÁCH ĐÃI NGỘ & THƯỞNG</div>
                    <h1>🎁 Thiết lập đợt thưởng (Tháng 13 / Lễ Tết)</h1>
                    <p class="subtle">
                        Kỳ lương: <strong><c:out value="${period.periodName}"/></strong> (Tháng <c:out value="${period.month}/${period.year}"/>)
                        &nbsp;·&nbsp; Trạng thái: <span class="badge <c:out value="${period.status}"/>"><c:out value="${period.status}"/></span>
                    </p>
                </div>
                <div class="toolbar-fields">
                    <a href="${pageContext.request.contextPath}/payroll?periodId=${period.payrollPeriodId}" class="btn secondary">
                        ← Quay lại kỳ lương
                    </a>
                </div>
            </div>

            <div id="feedback" class="feedback" role="status" aria-live="polite" hidden></div>

            <form id="bonusCampaignForm" autocomplete="off">
                <input type="hidden" name="periodId" id="periodId" value="<c:out value="${period.payrollPeriodId}"/>">

                <!-- CARD 1: CẤU HÌNH ĐỢT THƯỞNG -->
                <section class="pay-card" style="margin-bottom: 24px;">
                    <div class="card-toolbar" style="border-bottom: 1px solid var(--line);">
                        <div>
                            <h2>1. Thiết lập chính sách & công thức tính thưởng</h2>
                            <p class="subtle">Hệ thống tự động áp dụng công thức cho toàn bộ nhân viên. Bạn có thể xem trước và tinh chỉnh số tiền bên dưới.</p>
                        </div>
                    </div>
                    <div style="padding: 24px;">
                        <div class="form-pair" style="margin-bottom: 16px;">
                            <label style="font-weight: 600;">Tên đợt thưởng / Lý do
                                <input type="text" name="campaignName" id="campaignName" maxlength="100" required
                                       value="Lương tháng 13 năm <c:out value="${period.year}"/>"
                                       placeholder="VD: Lương tháng 13 năm 2026, Thưởng Tết Nguyên Đán, Thưởng hoàn thành dự án">
                            </label>
                            <label style="font-weight: 600;">Cách áp dụng vào kỳ lương
                                <select name="applyMode" id="applyMode">
                                    <option value="REPLACE" selected>Ghi đè mức thưởng của kỳ (Khuyên dùng khi bắt đầu đợt thưởng mới)</option>
                                    <option value="ADD">Cộng dồn vào mức thưởng hiện có của nhân viên</option>
                                </select>
                            </label>
                        </div>

                        <div class="form-pair" style="margin-bottom: 16px;">
                            <label style="font-weight: 600;">Hình thức tính thưởng
                                <select name="calcType" id="calcType">
                                    <option value="CONTRACT_SALARY" selected>Theo tháng lương hợp đồng (VD: 1.0 tháng lương cơ bản T13)</option>
                                    <option value="FIXED_AMOUNT">Số tiền thưởng cố định cho mỗi nhân viên (VND)</option>
                                </select>
                            </label>
                            <div id="monthsMultiplierContainer">
                                <label style="font-weight: 600;">Số tháng lương cơ bản được thưởng
                                    <div style="display: flex; align-items: center; gap: 8px; margin-top: 6px; flex-wrap: wrap;">
                                        <input type="number" name="monthsMultiplier" id="monthsMultiplier" min="0.1" max="12" step="0.1" value="1.0"
                                               style="max-width: 120px; font-weight: 700;">
                                        <span class="subtle">tháng lương · Chọn nhanh:</span>
                                        <button type="button" class="preset-badge-btn" data-preset="0.5">0.5 tháng</button>
                                        <button type="button" class="preset-badge-btn active" data-preset="1.0">1.0 tháng (Chuẩn T13)</button>
                                        <button type="button" class="preset-badge-btn" data-preset="1.5">1.5 tháng</button>
                                        <button type="button" class="preset-badge-btn" data-preset="2.0">2.0 tháng</button>
                                    </div>
                                </label>
                            </div>
                            <div id="fixedAmountContainer" style="display: none;">
                                <label style="font-weight: 600;">Số tiền thưởng cho mỗi nhân viên (VND)
                                    <input type="number" name="fixedAmount" id="fixedAmount" min="0" step="50000" value="1000000"
                                           style="max-width: 240px; font-weight: 700; margin-top: 6px;">
                                </label>
                            </div>
                        </div>

                        <div id="tenureProrateBox" style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 16px; margin-top: 12px;">
                            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-weight: 600; margin: 0;">
                                <input type="checkbox" name="prorateByTenure" id="prorateByTenure" value="true" checked style="width: 18px; height: 18px;">
                                <span>Tính theo <strong>thâm niên làm việc trong năm <c:out value="${period.year}"/></strong> (<code>Số tháng làm việc / 12</code>)</span>
                            </label>
                            <p class="subtle" style="margin: 6px 0 0 28px;">
                                Nhân viên làm đủ từ đầu năm nhận 100% mức thưởng. Nhân viên ký hợp đồng giữa năm (VD: làm 2 tháng từ tháng 11) sẽ được tính theo tỷ lệ 2/12 tháng thưởng.
                            </p>
                        </div>
                    </div>
                </section>

                <!-- METRICS TỔNG HỢP -->
                <section class="pay-metrics" style="margin-bottom: 24px;">
                    <div class="metric">
                        <small>Nhân viên đủ điều kiện</small>
                        <strong id="metricCandidateCount">0</strong>
                        <span>Hồ sơ có hợp đồng trong kỳ</span>
                    </div>
                    <div class="metric">
                        <small>Mức thưởng bình quân</small>
                        <strong id="metricAvgBonus">0 ₫</strong>
                        <span>Bình quân theo nhân viên</span>
                    </div>
                    <div class="metric highlight">
                        <small>Tổng ngân sách thưởng đợt này</small>
                        <strong id="metricTotalBonus">0 ₫</strong>
                        <span>Dự kiến cộng vào bảng lương kỳ</span>
                    </div>
                </section>

                <!-- CARD 2: BẢNG DANH SÁCH & TINH CHỈNH TAY -->
                <section class="pay-card">
                    <div class="card-toolbar" style="border-bottom: 1px solid var(--line); flex-wrap: wrap;">
                        <div>
                            <h2>2. Bảng xem trước & Tinh chỉnh mức thưởng theo nhân viên</h2>
                            <p class="subtle">Bạn có thể gõ sửa trực tiếp số tiền thưởng hoặc ghi chú riêng cho từng nhân viên trước khi áp dụng.</p>
                        </div>
                        <div class="toolbar-fields">
                            <label class="search-field">
                                <span>⌕</span>
                                <input type="search" id="candidateSearch" placeholder="Tìm tên, mã nhân viên hoặc phòng ban" aria-label="Tìm nhân viên">
                            </label>
                            <button type="button" class="btn secondary" id="resetToFormulaBtn">
                                ↻ Đặt lại theo công thức
                            </button>
                        </div>
                    </div>
                    <div class="table-scroll">
                        <table>
                            <thead>
                                <tr>
                                    <th style="width: 110px;">Mã NV</th>
                                    <th>Họ và tên</th>
                                    <th>Phòng ban</th>
                                    <th>Ngày vào làm</th>
                                    <th style="text-align: center;">Thâm niên năm <c:out value="${period.year}"/></th>
                                    <th class="number">Lương hợp đồng</th>
                                    <th class="number" style="width: 160px;">Mức thưởng (VND)</th>
                                    <th style="min-width: 220px;">Ghi chú đợt thưởng</th>
                                </tr>
                            </thead>
                            <tbody id="candidateRows">
                                <tr><td colspan="8" class="empty">Đang tải danh sách nhân viên…</td></tr>
                            </tbody>
                        </table>
                    </div>
                </section>

                <!-- STICKY ACTION BAR -->
                <div class="sticky-action-bar">
                    <div>
                        <a href="${pageContext.request.contextPath}/payroll?periodId=${period.payrollPeriodId}" class="btn secondary">
                            ← Hủy & Quay lại
                        </a>
                    </div>
                    <div style="display: flex; align-items: center; gap: 14px;">
                        <span class="subtle" id="saveHint">Đang sẵn sàng áp dụng đợt thưởng</span>
                        <button type="submit" class="btn primary" id="applyBonusBtn" style="padding: 12px 24px; font-size: 14px; font-weight: 700;">
                            💾 Áp dụng đợt thưởng vào kỳ lương
                        </button>
                    </div>
                </div>
            </form>

            <footer class="pay-footer" style="margin-top: 40px;">
                HRMs Cloud <span>•</span> Quản lý lương & đãi ngộ trong không gian công ty của bạn
            </footer>
        </div>
    </main>
</div>

<!-- Raw candidates data injected safely for fast initial rendering -->
<script id="candidatesData" type="application/json">
    ${empty candidatesJson ? '[]' : candidatesJson}
</script>

<script src="${pageContext.request.contextPath}/js/i18n-catalog.js"></script>
<script src="${pageContext.request.contextPath}/js/bonus-campaign.js?v=20261110-1"></script>
</body>
</html>

