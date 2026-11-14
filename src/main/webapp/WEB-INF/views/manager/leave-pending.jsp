<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="leavePendingMode" value="${true}" scope="request" />
<%@ include file="/WEB-INF/views/common/leave-header.jspf" %>
<%@ include file="/WEB-INF/views/common/leave-requests-panel.jspf" %>
<%@ include file="/WEB-INF/views/common/leave-footer.jspf" %>