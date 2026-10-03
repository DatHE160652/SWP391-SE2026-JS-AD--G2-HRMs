# HRMs – HR Management Platform for SMEs

## Team

**Group 2**

| Member | Student ID |
| --- | --- |
| Nguyen Hoang Bach | HE190498 |
| Dang Hoang Son | HE180524 |
| Pham Ngoc Anh | HE180850 |
| Nguyen Huy Hoang | HE181330 |
| Hoang Le Dat | HE160652 |

## 1. Project Overview

**HRMs** is a web-based Human Resource Management System designed for **Small and Medium-sized Enterprises (SMEs)** using a **B2B SaaS Multi-tenant Architecture**.

The system consists of two main portals:
1. **Public Marketing Portal (Landing Page):** Showcases features, pricing plans, solutions, company info, and handles lead generation (Request Demo / Contact Sales / Company Registration).
2. **Company Workspace Portal (`{companydomain}.hrms.com`):** Isolated workspace for company admins, managers, and employees to handle daily HR operations.

The system focuses on the **active working stage** of the employee lifecycle (excluding recruitment and resignation).

---

## 2. Business Model

- **Business Model:** B2B (Business-to-Business) SaaS
- **Tenant Architecture:** Multi-Tenancy via Company Domain / Subdomain.
- **Company Registration Flow:**
        `Public Landing Page` -> `Register / Request Demo` -> `Select Service Plan` -> `Provision Tenant Domain ({companydomain})` -> `Invite Employees & Start Operations`.

---

## 3. Project Scope

### In Scope
- Public Landing Page (Features, Pricing, Solution, About Us, Request Demo / Contact Form)
- Tenant / Company Registration & Domain Provisioning (`{companydomain}.hrms.com`)
- Ongoing Workspace Configuration & Customization (Shift policies, organization structure updates)
- Subscription & Plan Management (Upgrade, Renew)
- Platform Administration (Company management, service plans, subscriptions, system accounts, platform reports)
- Employee Management & Profile Portal
- Attendance Management (Check-in/out, GPS/Wifi/Manual)
- Leave Policy & Request Approval Workflow
- Payroll Calculation (Base salary, Bonuses, Deductions, Payslips)
- Bulk Excel Data Import (Employees, Attendance, Payroll)
- Multi-role Dashboards & Analytics

### Out of Scope
- Candidate Recruitment & Interview Management
- Pre-employment Onboarding & Offboarding workflows
- Contract Electronic Signing
- Performance Evaluation (KPI/OKR) & Task Management
- Complex Tax (PIT) and Social Insurance (BHXH) automated deductions

---

## 4. Main Actors

1. **Public Guest / Company Representative:** Visitors on the Landing Page looking for HR solutions, requesting demos, or self-registering a company and selecting a service plan.
2. **System Admin:** Platform owner responsible for platform-level administration — managing tenant companies, service plans, subscriptions, system accounts, and viewing platform-wide reports. Does **not** access tenant HR data.
3. **Company Admin / HR:** Manages tenant HR operations — employee data, attendance, leave, payroll, and ongoing workspace configuration.
4. **Manager:** Oversees team attendance, reviews, and approves/rejects team leave requests.
5. **Employee:** Accesses personal workspace (`{companydomain}.hrms.com`), checks in/out, views payslips, and requests leave.

---

# 5. Main Core Workflow

1. Public Portal & Company Registration

2. Employee Lifecycle Data Management

3. Attendance Tracking

4. Leave Approval Management

5. Payroll Calculation & Payslip Generation

6. Bulk Data Excel Import

---

## 6. System Workflows

### 6.1 Public Portal & Company Registration Flow

```text
Public Guest / Company Representative
        |
        v
Landing Page (Features / Pricing / Solutions / About)
        |                                  |
        v                                  v
Request Demo / Contact                Register Company
        |                                  |
        v                                  v
Sales Follow-up                  Enter Company Info / Subdomain
                                           |
                                           v
                                  Select Service Plan
                                           |
                                           v
                                  Create Company Admin Account
                                           |
                                           v
                                  Activate Subscription
                                           |
                                           v
                             [Platform] Provision Workspace
                                 ({companydomain}.hrms.com)
                                           |
                                           v
                         [Company Admin / HR] Invite Employees & Start Operations
```

Main Functions:
- View public solution details, features, and pricing tiers.
- Submit "Request Demo / Contact" form for direct sales consultation.
- Self-service company registration with real-time domain availability check.
- Select service plan, initiate subscription, and create primary Admin credentials.
- Automated provisioning of an isolated company workspace.
- Invite employees and begin day-to-day HR operations.

---

### 6.2 Tenant Domain Login Flow
```text
User Accesses App
        |
        v
Enter Company Domain / Code
        |
        v
System Identifies Tenant Workspace
        |
        v
Display Company-Branded Login Screen
        |
        v
Enter Username / Password
        |
        v
Access Workspace Dashboard
```

Main Functions:
- Tenant domain identification and verification.
- Dynamic interface customization (Company logo, name, custom theme).
- Role-Based Access Control (RBAC) authentication (Admin, Manager, Employee).
- Tenant-level data isolation enforcement.

---

## 6.3 Company Registration & Subscription Management

```text
[Public Guest / Company Representative]
        |
        v
Register Company
        |
        v
Create Company Admin Account
        |
        v
Select Service Plan
        |
        v
Activate Subscription
        |
        v
[Platform] Create Company Workspace
        |
        v
[Company Admin / HR] Use HRMs
        |
        +--------------------+
        |                    |
        v                    v
      Renew               Upgrade
```

Main functions:
- Register company (Company Representative)
- Create company admin account (Company Representative)
- Select service plan (Company Representative)
- Activate subscription (Company Representative)
- Create workspace (Platform / automated)
- Ongoing configuration updates (Company Admin / HR)
- Renew subscription (Company Admin)
- Upgrade service plan (Company Admin)
- View subscription status (Company Admin)

---

## 6.4 Employee Management

```text
Company Admin / HR
        |
        v
Add Employee
        |
        v
Enter Employee Information
        |
        v
Assign Department
        |
        v
Assign Position
        |
        v
Create Employee Account
        |
        v
Activate Employee
```

Main functions:
- Add employee
- Update employee information
- View employee profile
- Assign department
- Assign position
- Assign manager
- Set base salary
- Create employee account
- Change employee status

Employee status:
```text
Active
On Leave
Inactive
```

---

## 6.5 Attendance Management

Attendance Management handles employee working-time records independently from payroll calculation.

### Attendance Workflow

```text
Employee
   |
   v
Check-in
   |
   v
Working
   |
   v
Check-out
   |
   v
System Records Working Time
   |
   v
Attendance Report
```

### Attendance Functions

- Check-in
- Check-out
- View attendance history
- Record working time
- Record late arrival
- Record early leave
- Record absence
- HR attendance adjustment
- Attendance report

### Possible Attendance Status

```text
Present
Late
Absent
On Leave
Early Leave
```

### Relationship with Payroll

Attendance is **not used directly in the payroll calculation formula**.

Attendance data is used for:

- Working-time tracking
- Attendance reports
- HR monitoring
- Supporting leave and workforce records

Payroll deductions are entered or imported separately by HR.

---

## 6.6 Leave Management

Leave Management allows employees to submit leave requests and managers or HR staff to review and process them.

### Leave Workflow

```text
Employee
   |
   v
Create Leave Request
   |
   v
Select Leave Type and Date
   |
   v
Submit Request
   |
   v
Manager / HR Reviews
   |
   +-----------+
   |           |
   v           v
Approve      Reject
   |           |
   v           v
Update      Notify
Leave       Employee
Balance
```

### Leave Functions

- Submit leave request
- Select leave type
- Select leave date
- Check leave balance
- Approve leave request
- Reject leave request
- Update leave balance
- View leave history
- Configure leave types
- Configure leave policy

### Relationship with Attendance

Approved leave can update the employee's attendance status to **On Leave** for the approved period.

Leave data is not used directly in the payroll formula.

---

## 6.7 Payroll Management

### Salary Formula

```text
Net Salary = Base Salary + Bonuses - Deductions
```

Example:

```text
Base Salary:   15,000,000 VND
Bonuses:        2,000,000 VND
Deductions:       500,000 VND

Net Salary = 16,500,000 VND
```

### Payroll Workflow

```text
HR Selects Payroll Period
        |
        v
System Loads Employees
        |
        v
Load Base Salary
        |
        v
Enter / Import Bonuses
        |
        v
Enter / Import Deductions
        |
        v
Calculate Net Salary
        |
        v
HR Reviews Payroll
        |
        v
Approve Payroll
        |
        v
Generate Payslip
        |
        v
Employee Views Payslip
```

Payroll status:
```text
Draft
Calculated
Reviewed
Approved
```

Main functions:
- Configure base salary
- Create payroll period
- Enter bonus
- Enter deduction
- Import payroll adjustments
- Calculate net salary
- Review payroll
- Approve payroll
- Generate payslip
- View payroll history

Out of payroll scope:
- Overtime-based salary calculation
- Complex tax calculation
- Complex social insurance calculation
- Performance-based automatic salary calculation

---

## 6.8 Bulk Data Import Management

```text
HR
 |
 v
Download Excel Template
 |
 v
Fill Data
 |
 v
Upload Excel File
 |
 v
System Reads File
 |
 v
Validate Data
 |
 v
Show Preview
 |
 v
Display Valid / Invalid Rows
 |
 v
HR Confirms Import
 |
 v
Import Valid Data
 |
 v
Show Import Result
```

### Supported Import Types

#### Employee Import
```text
EmployeeCode
FullName
Email
Phone
Department
Position
StartDate
BaseSalary
```

#### Attendance Import
```text
EmployeeCode
Date
CheckIn
CheckOut
Status
```

#### Payroll Adjustment Import
```text
EmployeeCode
PayrollMonth
Bonus
Deduction
Note
```

### Validation Rules
- Duplicate employee code
- Invalid email format
- Missing required fields
- Invalid department
- Invalid position
- Invalid date format
- Invalid salary format
- Invalid bonus or deduction values

Example:
```text
Row 2: Valid
Row 3: Valid
Row 4: Invalid - EmployeeCode already exists
Row 5: Invalid - Department not found
```

---

## 6.9 Platform Administration & Reporting

> **Note:** This is a **platform-level** workflow performed exclusively by the **System Admin**. It is separate from tenant HR administration carried out by Company Admin / HR within an individual workspace.

### Platform Administration Flow

```text
System Admin
      |
      v
Platform Administration Dashboard
      |
        v
Select Administration Area
        |
        +--> Company Management
        +--> Service Plan Management
        +--> Subscription Management
        +--> System Account Management
        +--> Platform Reports
```

Main functions:
- Review and search registered companies, inspect company profiles, and activate or deactivate tenants.
- View service plans and create, update, or deactivate plans that are no longer offered.
- Browse and filter subscriptions, inspect their details, and activate, suspend, or cancel them.
- Create and update System Admin accounts, change their access status, and reset credentials.
- Review platform-wide company, subscription, and revenue reports; filter results and export report data.

---

# 7. Overall Business Process

```text
HRMs Provider / Platform
      |
      v
Company Registration & Plan Selection
      |
      v
Activate Subscription
      |
      v
[Platform] Create Company Workspace
      |
      v
+----------------------------------+
|       HR MANAGEMENT SYSTEM       |
+----------------------------------+
| Employee Management              |
| Attendance Management            |
| Leave Management                 |
| Payroll Management               |
| Bulk Data Import Management      |
+----------------------------------+
      |
      v
Reports & Analytics
      |
      v
Subscription Renewal / Upgrade
```

---

# 8. Relationships Between Modules

```text
Company
   |
   +--> Subscription
   |
   +--> Department
   |
   +--> Position
   |
   +--> Employee
           |
           +--> Attendance
           |
           +--> Leave
           |
           +--> Payroll

Bulk Data Import
   |
   +--> Employee Data
   |
   +--> Attendance Data
   |
   +--> Payroll Adjustment Data
```

Payroll does not depend directly on Attendance.

```text
Employee.BaseSalary
      +
Payroll.Bonus
      -
Payroll.Deduction
      =
NetSalary
```

---

# 9. Role-Based Access Control

| Function | System Admin | Company Admin / HR | Manager | Employee |
|---|:---:|:---:|:---:|:---:|
| **— Platform Administration —** | | | | |
| Manage tenant companies | ✓ | | | |
| Manage service plans | ✓ | | | |
| Manage subscriptions — platform-level | ✓ | | | |
| Manage system admin accounts | ✓ | | | |
| View platform reports | ✓ | | | |
| **— Tenant Workspace Administration —** | | | | |
| Configure company workspace | | ✓ | | |
| Manage own subscription (renew/upgrade) | | ✓ | | |
| Manage employees | | ✓ | Limited | |
| View attendance | | ✓ | Team | Own |
| Edit attendance | | ✓ | | |
| Submit leave request | | | ✓ | ✓ |
| Approve leave request | | ✓ | ✓ | |
| Manage payroll | | ✓ | | |
| View payslip | | ✓ | | Own |
| Import Excel data | | ✓ | | |
| View tenant HR reports | | ✓ | Limited | Limited |

---

# 10. Reports & Analytics

### System Admin Dashboard
- Total registered companies
- Active / Expired / Cancelled subscription counts
- New company registrations by time period
- Revenue summary by service plan
- Export platform report data

### Company Admin / HR Dashboard
- Total employees
- Employees by department
- Present employees
- Absent employees
- Employees on leave
- Pending leave requests
- Monthly payroll total
- Subscription status
- Import history

### Manager Dashboard
- Team members
- Team attendance
- Employees on leave
- Pending leave requests

### Employee Dashboard
- Personal information
- Attendance summary
- Leave balance
- Leave request status
- Latest payslip

---

# 11. Tools & Technologies

## Requirement & Design

| Tool | Purpose |
|---|---|
| Figma | UI/UX design, wireframes, mockups, prototypes |
| Draw.io | BPMN, Use Case, ERD, Activity, Sequence, and architecture diagrams |
| ChatGPT | Requirement analysis, documentation, brainstorming, development assistance |
| Gemini | Requirement analysis, research, documentation, AI-assisted development |

## Coding & Debugging

| Tool | Purpose |
|---|---|
| Visual Studio Code | Source-code editing and development |
| Visual Studio | Application development and debugging |

## Database

| Tool | Purpose |
|---|---|
| SQL Server Management Studio 2022 | Database design, querying, administration, and debugging |

## Testing

| Tool | Purpose |
|---|---|
| JUnit | Unit testing for Java-based components |
| NUnit | Unit testing for .NET-based components |
| Postman | API testing |
| Selenium | Automated web testing |
| AI-powered Test Case Generators | AI-assisted test case generation |

## Project Management

| Tool | Purpose |
|---|---|
| Jira | Task management, sprint planning, issue tracking |
| Git | Version control |
| GitHub / Gitea | Repository hosting, branch management, collaboration, and code review |

---

# 12. Proposed Repository Structure

```text
HRMs/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/hrms/
│       │       ├── controller/
│       │       │       ├── auth/
│       │       │       ├── admin/
│       │       │       ├── hr/
│       │       │       ├── manager/
│       │       │       └── employee/
│       │       ├── dao/
│       │       ├── model/
│       │       ├── filter/
│       │       ├── common/
│       │       └── util/
│       │
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── views/
│           │   │   ├── auth/
│           │   │   ├── admin/
│           │   │   ├── hr/
│           │   │   ├── manager/
│           │   │   └── employee/
│           │   │
│	    │   ├── asset
│           │   └── web.xml
│           │
│           └── index.jsp
│
├── database/
│   ├── scripts/
│   └── sample-data/
│
├── docs/
│   ├── Requirement/
│   ├── diagrams/
│   └── ui-ux/
│
├── .gitignore
├── pom.xml
└── README.md
```

---

# 13. Development Process

```text
Requirement Analysis
        |
        v
       SRS
        |
        v
Business Process Design
        |
        v
       SDS
        |
        v
Database & UI Design
        |
        v
Implementation
        |
        v
     Testing
        |
        v
    Deployment
```

The project is developed in **3 iterations over 10 weeks**.

Each iteration includes requirement refinement, design, implementation, testing, and documentation updates.

---

# 14. Project Goal

The goal of HRMs is to develop a B2B Human Resource Management System that supports SMEs in managing employees during their active working period.

The system focuses on six main workflows:

1. Public Portal & Company Registration
2. Employee Management
3. Attendance Management
4. Leave Management
5. Payroll Management
6. Bulk Data Import Management

The system aims to reduce manual HR work, improve data consistency, simplify payroll processing, provide convenient employee self-service functions, and allow businesses to import existing HR data from Excel efficiently.

---

# 15. Project Information

**Project Name:** HRMs – HR Management System for SMEs  
**Business Model:** B2B – Business-to-Business  
**Project Type:** Web-based Human Resource Management System  
**Target Customers:** Small and Medium-sized Enterprises (SMEs)  
**System Scope:** Employee Working Stage Management  
**Platform Model:** SaaS  
**Repository:** GitHub / Gitea  
**Development Duration:** 10 Weeks  
**Development Model:** 3 Iterations  
**Status:** In Development