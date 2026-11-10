(function (root) {
    'use strict';
    const labels = {
        ASSIGN_ACCOUNT_ROLE:HrmI18n.t('Đồng bộ vai trò theo chức danh'), SUPERSEDE_POSITION_PROPOSAL:HrmI18n.t('Thay thế đề xuất bằng phân công trực tiếp'),
        ACTIVE:HrmI18n.t('Đang hoạt động'), Active:HrmI18n.t('Đang hoạt động'), INACTIVE:HrmI18n.t('Ngừng hoạt động'), Inactive:HrmI18n.t('Ngừng hoạt động'),
        DRAFT:HrmI18n.t('Bản nháp'), EXPIRED:HrmI18n.t('Hết hạn'), TERMINATED:HrmI18n.t('Đã chấm dứt'), CANCELLED:HrmI18n.t('Đã hủy'), Cancelled:HrmI18n.t('Đã hủy'),
        PENDING:HrmI18n.t('Chờ duyệt'), Pending:HrmI18n.t('Chờ duyệt'), APPROVED:HrmI18n.t('Đã duyệt'), Approved:HrmI18n.t('Đã duyệt'), REJECTED:HrmI18n.t('Đã từ chối'), Rejected:HrmI18n.t('Đã từ chối'),
        APPLIED:HrmI18n.t('Đã áp dụng'), ENDED:HrmI18n.t('Đã kết thúc'), SCHEDULED:HrmI18n.t('Chờ hiệu lực'), CORRECTED:HrmI18n.t('Đã hiệu chỉnh trong ngày'),
        LOCKED:HrmI18n.t('Đã khóa'), REVOKED:HrmI18n.t('Đã thu hồi'), NONE:HrmI18n.t('Chưa có tài khoản'),
        SYSTEM_ADMIN:HrmI18n.t('Quản trị viên hệ thống'), COMPANY_ADMIN:'Company Admin / HR', HR:'Company Admin / HR', MANAGER:HrmI18n.t('Quản lý'), EMPLOYEE:HrmI18n.t('Nhân viên'),
        PROBATION:HrmI18n.t('Thử việc'), FIXED_TERM:HrmI18n.t('Có thời hạn'), INDEFINITE:HrmI18n.t('Không xác định thời hạn'),
        Male:HrmI18n.t('Nam'), Female:HrmI18n.t('Nữ'), Other:HrmI18n.t('Khác'),
        ONBOARDING:HrmI18n.t('Tạo hồ sơ nhân viên'), HIRE:HrmI18n.t('Phân công ban đầu'), BASELINE:HrmI18n.t('Ghi nhận phân công ban đầu'),
        TRANSFER:HrmI18n.t('Chuyển chức danh / phòng ban'), PROMOTION:HrmI18n.t('Thăng chức'), DEMOTION:HrmI18n.t('Hạ chức'), CORRECTION:HrmI18n.t('Hiệu chỉnh phân công'),
        DEPARTMENT_ASSIGNED:HrmI18n.t('Phân công phòng ban'), DEPARTMENT_TRANSFERRED:HrmI18n.t('Chuyển phòng ban'),
        MANAGER_ASSIGNED:HrmI18n.t('Gán quản lý trực tiếp'), MANAGER_TRANSFERRED:HrmI18n.t('Chuyển quản lý trực tiếp'), MANAGER_REMOVED:HrmI18n.t('Gỡ quản lý trực tiếp'),
        MANAGER_CHANGED:HrmI18n.t('Thay đổi quản lý trực tiếp'), MANAGER_CHANGE:HrmI18n.t('Thay đổi quản lý trực tiếp'),
        TEAM_MEMBER_ASSIGNED:HrmI18n.t('Thêm thành viên nhóm'), TEAM_MEMBER_TRANSFERRED:HrmI18n.t('Chuyển thành viên nhóm'), TEAM_MEMBER_REMOVED:HrmI18n.t('Gỡ thành viên nhóm'),
        SALARY_ADJUSTMENT:HrmI18n.t('Ghi nhận / điều chỉnh lương'), TERMINATE_EMPLOYEE:HrmI18n.t('Ngừng làm việc'),
        CREATE_EMPLOYEE:HrmI18n.t('Tạo hồ sơ nhân viên'), UPDATE_EMPLOYEE:HrmI18n.t('Cập nhật hồ sơ nhân viên'),
        ACTIVATE_EMPLOYEE:HrmI18n.t('Kích hoạt hồ sơ nhân viên'), DEACTIVATE_EMPLOYEE:HrmI18n.t('Ngừng hoạt động hồ sơ nhân viên'),
        SAVE_CONTRACT:HrmI18n.t('Lưu hợp đồng nháp'), RECORD_SIGNED_CONTRACT:HrmI18n.t('Ghi nhận hợp đồng đã ký'), ACTIVATE_CONTRACT:HrmI18n.t('Kích hoạt hợp đồng'), TERMINATE_CONTRACT:HrmI18n.t('Chấm dứt hợp đồng'),
        CONTRACT_ACTIVATE:HrmI18n.t('Kích hoạt hợp đồng'), CONTRACT_CANCEL:HrmI18n.t('Hủy hợp đồng'), CONTRACT_TERMINATE:HrmI18n.t('Chấm dứt hợp đồng'), CONTRACT_COMPENSATION_SYNC:HrmI18n.t('Đồng bộ lương theo hợp đồng'), CONTRACT_COMPENSATION_REVERT:HrmI18n.t('Hoàn tác lương hợp đồng đã hủy'),
        PORTAL_CREATE:HrmI18n.t('Tạo tài khoản nhân viên'), PORTAL_ROLE:HrmI18n.t('Thay đổi vai trò tài khoản'), PORTAL_RESET:HrmI18n.t('Đặt lại mật khẩu'),
        PORTAL_LOCK:HrmI18n.t('Khóa tài khoản'), PORTAL_UNLOCK:HrmI18n.t('Mở khóa tài khoản'), PORTAL_REVOKE:HrmI18n.t('Thu hồi quyền truy cập'),
        UPLOAD_DOCUMENT:HrmI18n.t('Tải tài liệu lên'), ACTIVATE_DOCUMENT:HrmI18n.t('Kích hoạt tài liệu'), DEACTIVATE_DOCUMENT:HrmI18n.t('Lưu trữ tài liệu'),
        UPDATE_SELF_PROFILE:HrmI18n.t('Cập nhật thông tin cá nhân'), CHANGE_PASSWORD:HrmI18n.t('Đổi mật khẩu'), REQUEST_POSITION_CHANGE:HrmI18n.t('Tạo đề xuất thay đổi chức danh'),
        POSITION_APPROVE:HrmI18n.t('Duyệt đề xuất chức danh'), POSITION_REJECT:HrmI18n.t('Từ chối đề xuất chức danh'), POSITION_APPLY:HrmI18n.t('Áp dụng thay đổi chức danh'),
        REQUEST_MANAGER_CHANGE:HrmI18n.t('Tạo đề xuất đổi Manager'), MANAGER_CHANGE_APPROVE:HrmI18n.t('Duyệt đề xuất đổi Manager'),
        MANAGER_CHANGE_REJECT:HrmI18n.t('Từ chối đề xuất đổi Manager'), MANAGER_CHANGE_APPLY:HrmI18n.t('Áp dụng đề xuất đổi Manager'),
        NEW:HrmI18n.t('Mới tiếp nhận'), UNDER_REVIEW:HrmI18n.t('Đang xem xét'), CONVERTING:HrmI18n.t('Đang khởi tạo'), CONVERTED:HrmI18n.t('Đã khởi tạo'),
        Present:HrmI18n.t('Có mặt'), Late:HrmI18n.t('Đi muộn'), Absent:HrmI18n.t('Vắng mặt'), 'Early Leave':HrmI18n.t('Về sớm'), 'On Leave':HrmI18n.t('Nghỉ phép'),
        PENDING_PAYMENT:HrmI18n.t('Chờ thanh toán'), SUSPENDED:HrmI18n.t('Tạm ngưng'), MONTHLY:HrmI18n.t('Hàng tháng'), YEARLY:HrmI18n.t('Hàng năm'),
        SYSTEM:HrmI18n.t('Hệ thống'), System:HrmI18n.t('Hệ thống'), 'Không ghi nhận':HrmI18n.t('Không ghi nhận'),
        'Initial compensation':HrmI18n.t('Khởi tạo lương'),
        'Manager relationship changed':HrmI18n.t('Thay đổi quản lý trực tiếp'),
        'Active; portal access remains separately controlled':HrmI18n.t('Đang hoạt động; quyền truy cập tài khoản được quản lý riêng'),
        true:HrmI18n.t('Có'), false:HrmI18n.t('Không'),
        ContractId:HrmI18n.t('Mã bản ghi hợp đồng'), CompanyId:HrmI18n.t('Mã công ty'), EmployeeId:HrmI18n.t('Mã bản ghi nhân viên'),
        ContractCode:HrmI18n.t('Mã hợp đồng'), ContractType:HrmI18n.t('Loại hợp đồng'), StartDate:HrmI18n.t('Ngày bắt đầu'), EndDate:HrmI18n.t('Ngày kết thúc'),
        GrossSalary:HrmI18n.t('Lương trước khấu trừ'), Status:HrmI18n.t('Trạng thái'), SignedDate:HrmI18n.t('Ngày ghi nhận ký kết'),
        TerminatedDate:HrmI18n.t('Ngày chấm dứt'), TerminationReason:HrmI18n.t('Lý do chấm dứt'), Notes:HrmI18n.t('Ghi chú'),
        CreatedByUserId:HrmI18n.t('Mã tài khoản tạo'), CreatedAt:HrmI18n.t('Thời điểm tạo'), UpdatedAt:HrmI18n.t('Thời điểm cập nhật'),
        FullName:HrmI18n.t('Họ và tên'), EmployeeCode:HrmI18n.t('Mã nhân viên'), DisplayStatus:HrmI18n.t('Trạng thái hiển thị'), Expiring:HrmI18n.t('Sắp hết hạn'),
        Version:HrmI18n.t('Phiên bản dữ liệu')
    };
    const label = value => {
        if (value == null) return value;
        const activationPrefix = HrmI18n.t('Active; Lý do kích hoạt lại: ');
        if (HrmI18n.language === 'en' && String(value).startsWith(activationPrefix))
            return 'Active; Reactivation reason: ' + String(value).slice(activationPrefix.length);
        return Object.prototype.hasOwnProperty.call(labels,String(value)) ? labels[String(value)] : value;
    };
    root.HrmVietnamese = {label,labels};
    if (typeof module !== 'undefined') module.exports = {label,labels};
})(globalThis);
