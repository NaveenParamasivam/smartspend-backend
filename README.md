# SmartSpend — Backend API

> Secure, full-featured expense tracking REST API built with Spring Boot 3.2

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-yellow)](https://jwt.io/)
[![JUnit](https://img.shields.io/badge/Tests-JUnit%205-25A162?logo=junit5)](https://junit.org/junit5/)

---

## 🔗 Links

| Resource | URL |
|---|---|
| 🚀 Live API Base URL | `https://YOUR_RENDER_URL/api` |
| 🖥️ Frontend Live App | `https://YOUR_VERCEL_URL` |
| 🖥️ Frontend Repo | [smartspend-frontend](https://github.com/NaveenParamasivam/smartspend-frontend) |
| 🧪 Test Report | [View on GitHub Pages](https://naveenparamasivam.github.io/smartspend-backend/) |

---

## 📋 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Project Structure](#-project-structure)
- [API Endpoints](#-api-endpoints)
- [Getting Started Locally](#-getting-started-locally)
- [Environment Variables](#-environment-variables)
- [Running Tests](#-running-tests)
- [Test Report](#-test-report)
- [Deployment](#-deployment)

---

## 📌 Project Overview

SmartSpend is a secure and user-friendly Expense Tracker REST API that enables authenticated users to perform full CRUD operations on their expenses and analyze them through a powerful filtering system.

---

## 🛠 Tech Stack

| Technology | Purpose |
|---|---|
| Spring Boot 3.2 | Core framework |
| Spring MVC | REST API layer |
| Spring Security | Authentication & authorization |
| Spring Data JPA + Hibernate | ORM & database access |
| JWT (jjwt 0.11.5) | Stateless token authentication |
| BCrypt | Password hashing |
| JavaMailSender | Email verification & password reset |
| Hibernate Validator | Request validation |
| WebSocket (STOMP) | Real-time notifications |
| iText 5.5.13 | PDF report generation |
| Apache POI 5.2.5 | Excel report generation |
| MySQL 8 | Primary database |
| JUnit 5 + Mockito + MockMvc | Testing |
| Maven | Build tool |

---

## ✨ Features

### 🔐 Registration & Authentication
- Sign-up with **email verification link** (token, 24h expiry)
- Secure login with **BCrypt password encryption** (strength 12)
- **JWT stateless authentication** with role-based access (USER / ADMIN)
- Password reset via **email link** (1h expiry)
- Resend verification email
- Change password (authenticated users)

### 💸 Expense Management
- Full **CRUD** on expense and income transactions
- Transaction fields: **title, amount, category, type (expense/income), date, description**
- 14 categories: Food, Transport, Housing, Health, Entertainment, Education, Shopping, Utilities, Salary, Freelance, Investment, Business, Gift, Other
- **Filter** by date range, category, transaction type, and amount range
- **Sort** by date or amount (asc/desc) with **pagination**

### 🎯 Budget Management
- Set **monthly budgets per category**
- Real-time spend tracking vs budget limit
- **Email + in-app notifications** at 75% (warning) and 100% (exceeded)
- Daily scheduled job for overnight budget checks

### 📊 Dashboard & Reports
- Monthly income/expense summary and net balance
- Category-wise expense breakdown
- Annual monthly trend data
- **PDF export** using iText
- **Excel export** using Apache POI
- Filter transactions by date range, category, or amount

### 🔔 Notifications
- Real-time notifications via **WebSocket (STOMP over SockJS)**
- Persistent notification store with unread count and mark-as-read

---

## 🏗 Project Structure

```
smartspend-backend/
├── src/
│   ├── main/
│   │   ├── java/com/smartspend/
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebSocketConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ExpenseController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── Controllers.java
│   │   │   ├── dto/
│   │   │   │   └── DTOs.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Expense.java
│   │   │   │   ├── Budget.java
│   │   │   │   └── Notification.java
│   │   │   ├── exception/
│   │   │   │   ├── AppException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── BudgetAlertScheduler.java
│   │   │   │   ├── BudgetService.java
│   │   │   │   ├── DashboardService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── ExpenseService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── ReportService.java
│   │   │   │   └── UserService.java
│   │   │   ├── util/
│   │   │   │   └── JwtUtil.java
│   │   │   └── SmartSpendApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/smartspend/
│       │   ├── SmartSpendTests.java         # Unit tests (Mockito)
│       │   └── IntegrationTests.java        # Integration tests (MockMvc + H2)
│       └── resources/
│           └── application-test.properties
└── pom.xml
```

---

## 📡 API Endpoints

### Authentication — `/api/auth`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | ❌ | Register new user |
| POST | `/auth/login` | ❌ | Login — returns JWT token |
| GET | `/auth/verify-email?token=` | ❌ | Verify email address |
| POST | `/auth/resend-verification?email=` | ❌ | Resend verification email |
| POST | `/auth/forgot-password` | ❌ | Send password reset email |
| POST | `/auth/reset-password` | ❌ | Reset password using token |
| POST | `/auth/change-password` | ✅ | Change password |
| GET | `/auth/me` | ✅ | Get current user profile |

### Expenses — `/api/expenses`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/expenses` | ✅ | Create expense / income |
| GET | `/expenses` | ✅ | List with filters & pagination |
| GET | `/expenses/{id}` | ✅ | Get single expense |
| PUT | `/expenses/{id}` | ✅ | Update expense |
| DELETE | `/expenses/{id}` | ✅ | Delete expense |
| GET | `/expenses/report/pdf?from=&to=` | ✅ | Download PDF report |
| GET | `/expenses/report/excel?from=&to=` | ✅ | Download Excel report |

**Filter query params:** `startDate`, `endDate`, `category`, `type`, `minAmount`, `maxAmount`, `sortBy`, `sortDir`, `page`, `size`

### Budgets — `/api/budgets`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/budgets` | ✅ | Create or update budget |
| GET | `/budgets?month=&year=` | ✅ | Get budgets for a month |
| DELETE | `/budgets/{id}` | ✅ | Delete budget |

### Dashboard — `/api/dashboard`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/dashboard?month=&year=` | ✅ | Full dashboard summary |

### Notifications — `/api/notifications`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/notifications` | ✅ | List notifications (paginated) |
| GET | `/notifications/unread-count` | ✅ | Get unread count |
| PUT | `/notifications/mark-all-read` | ✅ | Mark all as read |
| PUT | `/notifications/{id}/read` | ✅ | Mark one as read |

### Users — `/api/users`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/users/profile` | ✅ | Get profile |
| PUT | `/users/profile` | ✅ | Update first/last name |

---

## 🚀 Getting Started Locally

### Prerequisites
- Java 17 or 21
- Maven 3.8+
- MySQL 8.0
- Mailtrap account (free — for email testing in dev)

### 1. Clone the repository

```bash
git clone https://github.com/NaveenParamasivam/smartspend-backend.git
cd smartspend-backend
```

### 2. Create the database

```sql
CREATE DATABASE smartspend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure credentials

Open `src/main/resources/application.properties` and update:

```properties
# Database
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:your_mysql_password}

# JWT — generate with: openssl rand -base64 64
app.jwt.secret=${JWT_SECRET:your_secret_key_here}

# Mailtrap (free at mailtrap.io → Email Testing → Inboxes → SMTP Settings)
spring.mail.username=${MAIL_USERNAME:your_mailtrap_username}
spring.mail.password=${MAIL_PASSWORD:your_mailtrap_password}
```

### 4. Run the application

```bash
mvn spring-boot:run
```

API base URL: `http://localhost:8080/api`

### 5. Load demo data (optional)

```bash
mysql -u root -p smartspend < demo-data.sql
```

| Email | Password | Role |
|---|---|---|
| `demo@smartspend.com` | `Password1` | USER |
| `admin@smartspend.com` | `Password1` | ADMIN |

---

## ⚙️ Environment Variables

| Variable | Description | Dev Default |
|---|---|---|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `password` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | built-in dev key |
| `MAIL_HOST` | SMTP host | `sandbox.smtp.mailtrap.io` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | — |
| `MAIL_PASSWORD` | SMTP password | — |
| `MAIL_FROM` | Sender address | `noreply@smartspend.local` |
| `FRONTEND_URL` | Frontend base URL for email links | `http://localhost:5173` |
| `CORS_ORIGINS` | Allowed CORS origins | `http://localhost:5173` |

> **On Windows/IntelliJ:** Set these directly in `application.properties` as default values, or add them in IntelliJ Run Configurations → Environment Variables.

---

## 🧪 Running Tests

```bash
# Run all 17 tests
mvn test

# Unit tests only
mvn test -Dtest=SmartSpendTests

# Integration tests only
mvn test -Dtest=IntegrationTests

# Generate HTML report
mvn test surefire-report:report
# Output: target/site/surefire-report.html
```

### Test Suite

| File | Type | Count | Coverage |
|---|---|---|---|
| `SmartSpendTests.java` | Unit — Mockito | 8 tests | `AuthService`: register, duplicate email, bad credentials, forgot password, expired token · `ExpenseService`: create, delete, get by id |
| `IntegrationTests.java` | Integration — MockMvc + H2 | 9 tests | Register, login (valid/invalid), get current user, create expense, validation, list expenses, dashboard |
| **Total** | | **17 tests** | |

---

## 🧪 Test Report

> 📊 **[View Full HTML Test Report on GitHub Pages](https://naveenparamasivam.github.io/smartspend-backend/index.html)**
>
>  📊 **Test Report Exported from Intellij:** [Test Results](./Test%20Results%20-%20All_in_smartspend-backend.html)
## 👤 Author

**Naveen Paramasivam**
- GitHub: [@NaveenParamasivam](https://github.com/NaveenParamasivam)
- Frontend Repo: [smartspend-frontend](https://github.com/NaveenParamasivam/smartspend-frontend)