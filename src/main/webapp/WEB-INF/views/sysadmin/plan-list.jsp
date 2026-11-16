<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý Gói dịch vụ | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
</head>
<body class="admin-page company-page company-list-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="admin-main" id="main-content">
    <header class="company-page-title" style="display:flex; align-items:flex-end; justify-content:space-between; gap:16px; flex-wrap:wrap;">
        <div>
            <nav class="breadcrumb" aria-label="Đường dẫn">
                <a href="${pageContext.request.contextPath}/systemadmin/dashboard">Quản lý tenant</a>
                <svg class="icon" aria-hidden="true"><use href="${icons}#chevron"/></svg>
                <span>Gói dịch vụ</span>
            </nav>
            <h1>Quản lý Gói dịch vụ</h1>
        </div>
        <button class="button button-primary" type="button" data-dialog-open="create-plan-dialog">+ Thêm gói mới</button>
    </header>

    <c:set var="activePlanCount" value="0"/>
    <c:set var="inactivePlanCount" value="0"/>
    <c:set var="assignedCompanies" value="0"/>
    <c:forEach var="p" items="${plans}">
        <c:choose>
            <c:when test="${p.active}"><c:set var="activePlanCount" value="${activePlanCount + 1}"/></c:when>
            <c:otherwise><c:set var="inactivePlanCount" value="${inactivePlanCount + 1}"/></c:otherwise>
        </c:choose>
        <c:set var="assignedCompanies" value="${assignedCompanies + p.activeCompanyCount}"/>
    </c:forEach>

    <section class="company-stat-grid" aria-label="Tổng quan gói dịch vụ">
        <article class="company-stat-card">
            <div><h2>TỔNG SỐ GÓI<br><span>(PLANS)</span></h2><strong><c:out value="${plans.size()}"/></strong></div>
            <span class="company-stat-icon icon-blue"><svg class="icon" aria-hidden="true"><use href="${icons}#credit-card"/></svg></span>
            <p>Gói cấu hình trên nền tảng</p>
        </article>
        <article class="company-stat-card">
            <div><h2>ĐANG MỞ<br><span>(ACTIVE)</span></h2><strong class="text-success"><c:out value="${activePlanCount}"/></strong></div>
            <span class="company-stat-icon icon-success"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg></span>
            <p>Có thể chọn khi cấp tenant</p>
        </article>
        <article class="company-stat-card">
            <div><h2>TẠM ẨN<br><span>(INACTIVE)</span></h2><strong class="text-warning"><c:out value="${inactivePlanCount}"/></strong></div>
            <span class="company-stat-icon icon-warning"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg></span>
            <p>Không cấp mới cho doanh nghiệp</p>
        </article>
        <article class="company-stat-card">
            <div><h2>DOANH NGHIỆP<br><span>ĐANG DÙNG</span></h2><strong><c:out value="${assignedCompanies}"/></strong></div>
            <span class="company-stat-icon icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span>
            <p>Tổng thuê bao đang gắn gói</p>
        </article>
    </section>

    <c:if test="${not empty actionMessage}">
        <div class="alert alert-success" role="status"><c:out value="${actionMessage}"/></div>
    </c:if>
    <c:if test="${not empty actionError}">
        <div class="alert alert-error" role="alert"><c:out value="${actionError}"/></div>
    </c:if>

    <section class="company-list-panel" aria-label="Danh sách gói dịch vụ">
        <div class="table-wrap" tabindex="0">
            <table class="company-table">
                <thead>
                <tr>
                    <th scope="col">ID</th>
                    <th scope="col">Tên gói</th>
                    <th scope="col">Tài khoản tối đa</th>
                    <th scope="col">Đơn giá (VNĐ)</th>
                    <th scope="col">Chu kỳ</th>
                    <th scope="col">Doanh nghiệp dùng</th>
                    <th scope="col">Trạng thái</th>
                    <th scope="col" class="company-action-heading">Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="plan" items="${plans}">
                    <tr class="${!plan.active ? 'subscription-suspended-row' : ''}">
                        <td><strong class="workspace-code">#<c:out value="${plan.planId}"/></strong></td>
                        <td class="company-name"><c:out value="${plan.planName}"/></td>
                        <td><strong><c:out value="${plan.maxAccounts}"/></strong></td>
                        <td><strong><c:out value="${plan.formattedPrice}"/> đ</strong></td>
                        <td><span class="region-label"><c:out value="${plan.billingCycleLabel}"/> (<c:out value="${plan.billingCycle}"/>)</span></td>
                        <td><strong><c:out value="${plan.activeCompanyCount}"/></strong> doanh nghiệp</td>
                        <td>
                            <span class="subscription-status ${plan.active ? 'is-active' : 'is-suspended'}">
                                <i></i>
                                <c:out value="${plan.active ? 'Đang mở' : 'Tạm ẩn'}"/>
                            </span>
                        </td>
                        <td class="company-action" style="display:flex; justify-content:flex-end; gap:8px;">
                            <button class="button button-small" type="button" data-dialog-open="edit-plan-${plan.planId}">Sửa</button>
                            <form method="post" action="${pageContext.request.contextPath}/systemadmin/plans" style="display:inline;">
                                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
                                <input type="hidden" name="action" value="toggle">
                                <input type="hidden" name="planId" value="${plan.planId}">
                                <input type="hidden" name="active" value="${!plan.active}">
                                <button class="button button-small ${plan.active ? 'button-danger' : 'button-primary'}" type="submit">
                                    <c:out value="${plan.active ? 'Tạm ẩn' : 'Mở lại'}"/>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>

    <dialog class="action-dialog" id="create-plan-dialog" aria-labelledby="create-plan-title">
        <form method="post" action="${pageContext.request.contextPath}/systemadmin/plans">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
            <input type="hidden" name="action" value="create">
            <h2 id="create-plan-title">Thêm gói dịch vụ mới</h2>
            <p>Thiết lập tên gói, định mức tài khoản tối đa, đơn giá và chu kỳ áp dụng cho doanh nghiệp.</p>
            <label class="dialog-field" for="create-plan-name">Tên gói <span aria-hidden="true">*</span></label>
            <input id="create-plan-name" type="text" name="planName" required maxlength="100" placeholder="VD: GROWTH, ENTERPRISE"
                   style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; margin-bottom:14px;">
            <label class="dialog-field" for="create-plan-accounts">Số tài khoản tối đa <span aria-hidden="true">*</span></label>
            <input id="create-plan-accounts" type="number" name="maxAccounts" required min="1" max="10000" value="50"
                   style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; margin-bottom:14px;">
            <label class="dialog-field" for="create-plan-price">Giá gói (VNĐ) <span aria-hidden="true">*</span></label>
            <input id="create-plan-price" type="number" name="price" required min="0" step="1000" value="199000"
                   style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; margin-bottom:14px;">
            <label class="dialog-field" for="create-plan-cycle">Chu kỳ thanh toán</label>
            <select id="create-plan-cycle" name="billingCycle"
                    style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff;">
                <option value="MONTHLY">Tháng (MONTHLY)</option>
                <option value="YEARLY">Năm (YEARLY)</option>
            </select>
            <div class="dialog-actions">
                <button class="button" type="button" data-dialog-close>Hủy bỏ</button>
                <button class="button button-primary" type="submit">Lưu gói mới</button>
            </div>
        </form>
    </dialog>

    <c:forEach var="plan" items="${plans}">
        <dialog class="action-dialog" id="edit-plan-${plan.planId}" aria-labelledby="edit-plan-title-${plan.planId}">
            <form method="post" action="${pageContext.request.contextPath}/systemadmin/plans">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="planId" value="${plan.planId}">
                <h2 id="edit-plan-title-${plan.planId}">Chỉnh sửa gói #<c:out value="${plan.planId}"/></h2>
                <p>Cập nhật thông tin gói dịch vụ <strong><c:out value="${plan.planName}"/></strong>.</p>
                <label class="dialog-field" for="edit-plan-name-${plan.planId}">Tên gói <span aria-hidden="true">*</span></label>
                <input id="edit-plan-name-${plan.planId}" type="text" name="planName" required maxlength="100" value="<c:out value='${plan.planName}'/>"
                       style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; margin-bottom:14px;">
                <label class="dialog-field" for="edit-plan-accounts-${plan.planId}">Số tài khoản tối đa <span aria-hidden="true">*</span></label>
                <input id="edit-plan-accounts-${plan.planId}" type="number" name="maxAccounts" required min="1" max="10000" value="${plan.maxAccounts}"
                       style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; margin-bottom:14px;">
                <label class="dialog-field" for="edit-plan-price-${plan.planId}">Giá gói (VNĐ) <span aria-hidden="true">*</span></label>
                <input id="edit-plan-price-${plan.planId}" type="number" name="price" required min="0" step="1000" value="${plan.price.longValue()}"
                       style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff; margin-bottom:14px;">
                <label class="dialog-field" for="edit-plan-cycle-${plan.planId}">Chu kỳ thanh toán</label>
                <select id="edit-plan-cycle-${plan.planId}" name="billingCycle"
                        style="width:100%; height:42px; padding:0 12px; border:1px solid #cbd5e1; border-radius:8px; background:#fff;">
                    <option value="MONTHLY" ${plan.billingCycle == 'MONTHLY' ? 'selected' : ''}>Tháng (MONTHLY)</option>
                    <option value="YEARLY" ${plan.billingCycle == 'YEARLY' ? 'selected' : ''}>Năm (YEARLY)</option>
                </select>
                <div class="dialog-actions">
                    <button class="button" type="button" data-dialog-close>Hủy bỏ</button>
                    <button class="button button-primary" type="submit">Cập nhật gói</button>
                </div>
            </form>
        </dialog>
    </c:forEach>
</main>
</body>
</html>
