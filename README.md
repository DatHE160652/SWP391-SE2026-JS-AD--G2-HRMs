# HRMs - HR Management Platform for SMEs

## Project Title

**HRMs - HR Management Platform for Small and Medium-sized Enterprises**

## Description

HRMs is a web-based Human Resource Management System built for small and medium-sized enterprises (SMEs). It uses a B2B SaaS multi-tenant model so that each company can operate in its own isolated workspace through a dedicated domain such as `{companydomain}.hrms.com`.

The project addresses common HR administration challenges for growing businesses: employee records stored in multiple places, manual attendance tracking, slow leave approvals, payroll preparation errors, and repetitive data entry. HRMs centralizes these daily workflows in one role-based platform for company administrators, managers, and employees.

This project was developed to apply practical knowledge of Java web development, database design, role-based access control, multi-tenant system design, and team-based software delivery. It also provides hands-on experience with modelling real business workflows and translating requirements into a maintainable web application.

## Main Core Workflows

1. Public Portal and Company Registration
2. Platform Administration
3. Employee Management
4. Attendance Management
5. Leave Management
6. Payroll Management
7. Bulk Data Import Management

## Features

- Public landing page with product information, pricing, FAQs, and company registration.
- Multi-tenant company workspace provisioning with domain availability validation.
- Role-based access for System Admin, Company Admin/HR, Manager, and Employee.
- Employee profile, department, position, manager, and employment-status management.
- Employee creation requires manually entered company codes (up to 50 characters, unique within the company). Existing employee codes are preserved when editing. Profiles are created separately from portal accounts; account provisioning uses the contact email and the role assigned by the employee's position.
- Newly generated employee accounts use the employee's validated identity number as their initial password, stored as plaintext in `Users.Password`. Reset and changed passwords are also stored as plaintext, and authentication compares passwords directly. Resetting a password or changing an account role invalidates existing tenant sessions. Company Admin and HR share the lifecycle dashboard and management permissions.
- Employee profiles store a Vietnamese CCCD identity number as text (exactly 12 ASCII digits, required for new employees). The form shows validation errors under each input. For existing databases, back up `HRMsDB` and run `database/scripts/employee-identity-number.sql` before restarting the app. This migration removes `Employees.StartDate` and `Employees.BaseSalary`; existing employees have a null identity number until it is supplied. Subscription/leave dates and payslip salaries remain unchanged.
- Employee check-in/check-out, attendance history, and attendance reports.
- Leave request submission, approval or rejection, leave-balance updates, and leave history.
- Payroll period management, bonuses, deductions, net-salary calculation, and payslip generation.
- Bulk Excel import for employee data, attendance records, and payroll adjustments with validation and import history.
- Platform-level company, plan, subscription, and system-account management.

## Technologies

| Technology | Purpose |
| --- | --- |
| Java Servlet | Server-side request handling and business workflows. |
| JSP | Dynamic web pages and user interfaces. |
| Apache Maven | Build automation and dependency management. |
| Microsoft SQL Server | Persistent storage for tenant, employee, attendance, leave, and payroll data. |
| SQL Server Management Studio 2022 | Database design, querying, administration, and debugging. |
| JUnit | Unit testing for Java components. |
| Draw.io | System, workflow, and database diagram design. |
| Git | Version control and team collaboration. |

## Local Database Setup

Copy `src/main/resources/db.properties` to `src/main/resources/db.properties` and supply your local SQL Server settings. The credential file is ignored by Git. Environment variables such as `HRM_DB_URL`, `HRM_DB_USER` and `HRM_DB_PASSWORD` override the file.

For an existing lifecycle database, back it up, select that database in SQL Server Management Studio, and run `database/scripts/employee-lifecycle-compatibility.sql`. This updates older lifecycle snapshots with the credential column, contract travel allowance, profile/document period type, and manager-change fields and procedure required by the current application. Existing credential values are preserved when the legacy credential column is renamed. Accounts with previously hashed passwords require a password reset before logging in using direct password comparison. This migration requires the lifecycle tables; the base `database/db.sql` alone does not contain them.

Run `mvn test` after configuring a dedicated test database with the lifecycle schema. Integration tests require SQL Server; missing configuration is reported as an error, not a successful test run.

## Form Validation

All JSP pages include `WEB-INF/views/common/form-validation.jspf`. The shared validator displays red, italic field messages, disables native browser validation popups, and blocks invalid submissions before existing form handlers run. Server-side field validation uses `FieldValidationException` and `ValidationSupport.errorJson` to return `fieldErrors`; other service failures keep their existing general messages.

Run the DOM regression tests in PowerShell:

```powershell
npm.cmd install --prefix target/validation-test-runtime --no-save --package-lock=false --ignore-scripts jsdom
$env:NODE_PATH = Join-Path (Get-Location) 'target/validation-test-runtime/node_modules'
node --test src/test/js/form-validation.test.cjs
```

## Profile Update Periods

Run `database/scripts/profile-update-periods.sql` for an existing database. Company Admin opens the management page from the employee list; HR lands on `/employee/profile-update-periods` after login. They can create company-wide periods with start/end times or close them early. Overlapping periods that have not been closed are rejected. Times are stored in UTC and displayed in the browser's local timezone.

Employee and Manager self-service profiles are read-only unless their company has a currently open period. During that period they can update only their own phone number (10 digits beginning with 0); names, gender, birth dates, employee codes and identity numbers remain protected. The API validates the session, role, ownership, company, CSRF token and active period on every save. Phone changes and their audit entries share a transaction. An open period allows repeated phone corrections until it ends or is closed.

## Collaborators

- [Nguyen Hoang Bach](https://github.com/HoangBach0)
- [Dang Hoang Son](https://github.com/SONDHHE180524)
- [Pham Ngoc Anh](https://github.com/anhpn24012004)
- [Nguyen Huy Hoang](https://github.com/huyhoag)
- [Hoang Le Dat](https://github.com/DatHE160652)

## License

No open-source license has been selected for this repository. All rights are reserved by the project contributors unless a license file is added in the future.
