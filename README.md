# HRM Cloud – HR Management Platform for SMEs

# Menber Group 2
Nguyen Hoang Bach - HE190498
Đặng Hoàng Sơn - HE180524
Ngọc Anh: Phạm Ngọc Anh - HE180850
Nguyễn Huy Hoàng - HE181330
Hoàng Lê Đạt - HE160652

## 1. Project Overview

**HRM Cloud** is a cloud-based Human Resource Management platform designed for **Small and Medium-sized Enterprises (SMEs)**.

The project follows the **B2B (Business-to-Business)** business model, in which HRM Cloud provides human resource management software services to businesses through a centralized digital platform.

The system aims to help SMEs digitalize and simplify their HR processes, reduce administrative workload, improve workforce management efficiency, and provide businesses with an affordable and convenient HR management solution.

---

## 2. Business Model

**Model:** B2B – Business-to-Business

The main business relationship is:

**HRM Cloud Provider → SME Customer → HR Management Services**

Businesses can register an organization account, select a service plan, create their company workspace, add employees, and use HRM functions through the platform.

### Main Business Process

**Business Registration → Select Service Plan → Subscription → Create Company Workspace → Add Employees → Use HRM Services → Reports → Renew / Upgrade**

---

## 3. Project Goals

The main goals of HRM Cloud are to:

* Develop a centralized HR management platform for SMEs.
* Digitalize common human resource management processes.
* Reduce manual HR administrative tasks.
* Improve employee information management.
* Support attendance and leave management.
* Support payroll management.
* Support recruitment and employee performance evaluation.
* Provide HR reports for managers.
* Allow businesses to manage HR activities through a single platform.
* Provide a scalable B2B HRM solution for multiple organizations.

---

## 4. Target Users

### Business Administrator

The Business Administrator manages the organization's account, subscription, company information, HR configurations, and user permissions.

### HR Manager

HR Managers can manage employee information, attendance, leave requests, payroll, recruitment, performance, and HR reports.

### Department Manager

Department Managers can manage employees within their departments, approve requests, monitor attendance, and evaluate employee performance.

### Employee

Employees can access their personal information, view attendance records, submit leave requests, view payroll information, and track their performance.

---

## 5. Main Features

### Organization Management

* Business registration
* Company profile management
* Department management
* Role and permission management
* Subscription management

### Employee Management

* Employee profiles
* Employment information
* Department assignment
* Position management
* Employment status
* Employee records

### Attendance Management

* Check-in / Check-out
* Attendance tracking
* Working-hour calculation
* Late/absence records
* Attendance reports

### Leave Management

* Submit leave requests
* Approve/reject leave requests
* Leave balance management
* Leave history

### Payroll Management

* Salary information
* Allowances
* Bonuses
* Deductions
* Payroll calculation
* Payslip management

### Recruitment Management

* Job vacancy management
* Candidate information
* Application tracking
* Interview scheduling
* Recruitment status

### Performance Management

* Performance evaluation
* KPI management
* Employee feedback
* Performance history

### Reporting

* Employee reports
* Attendance reports
* Leave reports
* Payroll reports
* Recruitment reports
* Performance reports

---

## 6. Basic System Workflow

```text
HRM Cloud Provider
        |
        v
Business Registration
        |
        v
Select Service Plan
        |
        v
Create Company Workspace
        |
        v
Configure Organization
        |
        v
Add Employees
        |
        v
+-----------------------------+
|     HR Management System    |
+-----------------------------+
| Employee Management         |
| Attendance Management       |
| Leave Management            |
| Payroll Management          |
| Recruitment Management      |
| Performance Management      |
+-----------------------------+
        |
        v
Reports & Analytics
        |
        v
Subscription Renewal / Upgrade
```

---

## 7. Tools & Technologies

### Requirement & Design

| Tool        | Purpose                                                                                |
| ----------- | -------------------------------------------------------------------------------------- |
| **Figma**   | UI/UX design, wireframes, mockups, and prototypes                                      |
| **Draw.io** | BPMN, Use Case, ERD, Activity, Sequence, and system diagrams                           |
| **ChatGPT** | Requirement analysis, documentation support, brainstorming, and development assistance |
| **Gemini**  | Requirement analysis, research support, documentation, and AI-assisted development     |

### Coding & Debugging

| Tool                   | Purpose                                         |
| ---------------------- | ----------------------------------------------- |
| **Visual Studio Code** | Source-code editing and development             |
| **Visual Studio**      | Application development, debugging, and testing |

### Database

| Tool                                         | Purpose                                                               |
| -------------------------------------------- | --------------------------------------------------------------------- |
| **SQL Server Management Studio 2022 (SSMS)** | Database design, development, querying, administration, and debugging |

### Testing

| Tool                                | Purpose                                              |
| ----------------------------------- | ---------------------------------------------------- |
| **JUnit**                           | Unit testing for Java-based components               |
| **NUnit**                           | Unit testing for .NET-based components               |
| **Postman**                         | API testing and validation                           |
| **Selenium**                        | Automated web application testing                    |
| **AI-powered Test Case Generators** | AI-assisted generation and improvement of test cases |

> JUnit or NUnit will be selected depending on the programming language and framework used in the implementation.

### Project Management & Version Control

| Tool               | Purpose                                                                           |
| ------------------ | --------------------------------------------------------------------------------- |
| **Jira**           | Task management, sprint planning, issue tracking, and project progress monitoring |
| **Git**            | Source-code version control                                                       |
| **GitHub / Gitea** | Repository hosting, collaboration, branch management, and code review             |

### Deployment

| Tool       | Purpose                                     |
| ---------- | ------------------------------------------- |
| **Docker** | Application containerization and deployment |

---

## 8. Development Process

The project follows a structured software development process:

```text
Requirement Analysis
        ↓
Business Process Analysis
        ↓
System Design
        ↓
UI/UX Design
        ↓
Database Design
        ↓
Implementation
        ↓
Testing
        ↓
Deployment
        ↓
Maintenance & Improvement
```

### Phase 1 – Requirement Analysis

Identify business requirements, user requirements, system requirements, actors, use cases, and business processes.

**Tools:** ChatGPT, Gemini, Jira

### Phase 2 – System & UI/UX Design

Design the business process, system architecture, database structure, diagrams, wireframes, and user interfaces.

**Tools:** Figma, Draw.io

### Phase 3 – Development

Implement frontend, backend, business logic, APIs, and database integration.

**Tools:** VS Code / Visual Studio, Git

### Phase 4 – Testing

Perform unit testing, API testing, integration testing, and automated UI testing.

**Tools:** JUnit / NUnit, Postman, Selenium, AI-powered test case generators

### Phase 5 – Deployment

Package the application and its dependencies into containers for consistent deployment.

**Tool:** Docker

---

## 9. Project Management Workflow

The development team uses Jira and Git-based version control to manage the project.

```text
Product Backlog
      ↓
Sprint Planning
      ↓
Task Assignment
      ↓
Development Branch
      ↓
Coding & Testing
      ↓
Commit & Push
      ↓
Pull Request / Code Review
      ↓
Merge
      ↓
Sprint Review
```

Team members should create separate branches for assigned features and use meaningful commit messages to maintain a clear development history.

---

## 10. Proposed Repository Structure

```text
HRM-Cloud/
│
├── frontend/
│   └── # Frontend application
│
├── backend/
│   └── # Backend application and APIs
│
├── database/
│   ├── schema/
│   ├── scripts/
│   └── sample-data/
│
├── tests/
│   ├── unit/
│   ├── api/
│   └── automation/
│
├── docs/
│   ├── requirements/
│   ├── business-process/
│   ├── diagrams/
│   ├── database-design/
│   └── ui-ux/
│
├── docker/
│   └── # Docker configuration
│
├── .gitignore
├── docker-compose.yml
└── README.md
```

---

## 11. Expected Outcome

At the end of the project, HRM Cloud is expected to provide a functional prototype of a **B2B Human Resource Management platform for SMEs**.

The system should demonstrate how a cloud-based software application can support both the business relationship between the HRM service provider and SME customers and the internal HR processes of each registered organization.

---

## 12. Project Information

**Project Name:** HRM Cloud – HR Management Platform for SMEs
**Business Model:** B2B – Business-to-Business
**Project Type:** Web-based Human Resource Management System
**Target Customers:** Small and Medium-sized Enterprises (SMEs)
**Repository:** DX Laboratory – Gitea Service
**Status:** In Development
