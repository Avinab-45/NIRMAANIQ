# NirmaanIQ - Construction Intelligence Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()
[![Tests](https://img.shields.io/badge/Tests-106%20Passed-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue.svg)](https://supabase.com)

A robust, enterprise-structured Spring Boot 3 backend for **NirmaanIQ**, an AI-powered construction intelligence platform. It seamlessly connects **Builders**, **Contractors**, and **Clients** with strict role-based access control, project lifecycle tracking, Supabase Storage media management, material inventory, financial budget tracking, EVM productivity metrics, and an honest AI insights layer.

---

## 1. Technology Stack

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Apache Maven 3.9+
- **Security**: Spring Security 6 & Spring OAuth2 Resource Server (Supabase JWT verification)
- **Database**: PostgreSQL on Supabase (`spring.jpa.hibernate.ddl-auto=validate`)
- **Storage**: Supabase Storage REST API (private bucket with signed URLs)
- **Validation**: Jakarta Validation API
- **Testing**: JUnit 5, Mockito, Spring Security Test, Spring Boot Test Slice (`@WebMvcTest`)
- **Boilerplate Reduction**: Lombok

---

## 2. Platform Architecture & User Roles

```
                           +------------------------+
                           |  Supabase Auth / JWKS  |
                           +-----------+------------+
                                       | Verified JWT (sub claim)
                                       v
                    +--------------------------------------+
                    |  Spring Security Resource Server     |
                    |  SupabaseJwtAuthenticationConverter  |
                    +------------------+-------------------+
                                       |
                   +-------------------+-------------------+
                   |                   |                   |
                   v                   v                   v
            [ ROLE_BUILDER ]   [ ROLE_CONTRACTOR ]   [ ROLE_CLIENT ]
             Owner Builder      Site Operations       Read-Only Stakeholder
                   |                   |                   |
                   +-------------------+-------------------+
                                       |
                                       v
                     +----------------------------------+
                     |       ProjectAccessService       |
                     |  (IDOR Prevention & Enforcer)    |
                     +-----------------+----------------+
                                       |
        +-------------+----------------+----------------+-------------+
        |             |                |                |             |
        v             v                v                v             v
    Projects       Phases           Progress          Media       Materials &
    & Members      & Budgets        & Labour         Storage       Inventory
```

### Role Matrix & Permissions:
1. **Builder**:
   - Invitation/approval-based activation via `POST /api/auth/builder/activate`.
   - Full control over projects they own.
   - Auto-generates unique project codes (e.g. `PRJ-9X8K2M`).
   - Assigns Contractors and Clients by registered verified email.
   - Manages budgets, phases, and reviews operational data.
2. **Contractor**:
   - Authenticated through Supabase Auth + secondary 6-digit email OTP verification (`otp_challenges`).
   - Accesses only projects where they are an active assigned member.
   - Submits daily progress reports, site photos/documents, material transactions, expenses, and labour records.
3. **Client**:
   - Authenticated through Supabase Email OTP (passwordless one-time email passcode via custom SMTP).
   - Strictly **read-only** access to projects assigned to them. All write/delete endpoints reject Clients with `403 Forbidden`.
4. **Security Rule**:
   - A `projectCode` is only an identifier, never an authentication mechanism.
   - `ProjectAccessService` strictly validates user identity, active status, role, and project membership on every single project operation to eliminate IDOR vulnerabilities.

---

## 3. Implemented Modules & Endpoints

| Module | HTTP Method | Endpoint | Authorization |
|---|---|---|---|
| **System** | `GET` | `/api/health` | Public |
| **Auth** | `POST` | `/api/auth/builder/activate` | Authenticated (JWT) |
| | `GET` | `/api/auth/builder/me` | `ROLE_BUILDER` |
| | `POST` | `/api/auth/contractor/request-otp` | Authenticated (JWT) |
| | `POST` | `/api/auth/contractor/verify-otp` | Authenticated (JWT) |
| | `GET` | `/api/auth/client/me` | `ROLE_CLIENT` |
| **Projects** | `POST` | `/api/projects` | `ROLE_BUILDER` |
| | `GET` | `/api/projects` | Builder / Contractor / Client (Paginated) |
| | `GET` | `/api/projects/{projectCode}` | Assigned Member / Owner |
| | `PUT` | `/api/projects/{projectCode}` | Builder Owner Only |
| | `DELETE` | `/api/projects/{projectCode}` | Builder Owner Only |
| **Members** | `POST` | `/api/projects/{projectCode}/members` | Builder Owner Only |
| | `GET` | `/api/projects/{projectCode}/members` | Assigned Member / Owner |
| | `DELETE` | `/api/projects/{projectCode}/members/{userId}` | Builder Owner Only |
| **Phases** | `POST` | `/api/projects/{projectCode}/phases` | Builder Owner Only |
| | `GET` | `/api/projects/{projectCode}/phases` | Assigned Member / Owner |
| | `PUT` | `/api/projects/{projectCode}/phases/{phaseId}` | Write Access (Builder / Contractor) |
| **Progress** | `POST` | `/api/projects/{projectCode}/progress` | Write Access (Daily area, work category, percentage) |
| | `GET` | `/api/projects/{projectCode}/progress` | Assigned Member / Owner (Paginated, date filters) |
| | `GET` | `/api/projects/{projectCode}/progress/date/{date}` | Assigned Member / Owner (Date-wise reports) |
| | `GET` | `/api/projects/{projectCode}/progress/latest` | Assigned Member / Owner |
| **Media** | `POST` | `/api/projects/{projectCode}/media` | Write Access (Multipart file, progress report linking) |
| | `GET` | `/api/projects/{projectCode}/media` | Assigned Member / Owner (Signed URLs) |
| | `DELETE` | `/api/projects/{projectCode}/media/{mediaId}` | Owner / Uploader |
| | `POST` | `/api/projects/{projectCode}/media/{mediaId}/analyze` | Write Access (Trigger AI vision inspection; supports `?reanalyze=true`) |
| | `GET` | `/api/projects/{projectCode}/media/{mediaId}/analyses` | Assigned Member / Owner (Photo analysis history) |
| | `GET` | `/api/projects/{projectCode}/media/{mediaId}/analyses/latest` | Assigned Member / Owner (Latest photo analysis) |
| **Materials** | `POST` | `/api/projects/{projectCode}/materials` | Write Access (Initializes 0 stock) |
| | `GET` | `/api/projects/{projectCode}/materials` | Assigned Member / Owner |
| | `POST` | `/api/projects/{projectCode}/materials/transactions` | Write Access (Adjusts inventory stock) |
| | `GET` | `/api/projects/{projectCode}/inventory` | Assigned Member / Owner (Stock & Alerts) |
| **Budgets** | `POST` | `/api/projects/{projectCode}/budgets` | Builder Owner Only |
| | `GET` | `/api/projects/{projectCode}/budgets` | Assigned Member / Owner |
| | `POST` | `/api/projects/{projectCode}/expenses` | Write Access (Builder / Contractor) |
| | `GET` | `/api/projects/{projectCode}/expenses` | Assigned Member / Owner (Paginated) |
| | `GET` | `/api/projects/{projectCode}/financial-summary` | Assigned Member / Owner |
| **Labour** | `POST` | `/api/projects/{projectCode}/labour` | Write Access (Builder / Contractor) |
| | `GET` | `/api/projects/{projectCode}/labour` | Assigned Member / Owner (Paginated) |
| **AI Insights** | `GET` | `/api/projects/{projectCode}/ai/risks` | Assigned Member / Owner (Honest Status) |
| | `GET` | `/api/projects/{projectCode}/ai/recommendations` | Assigned Member / Owner |
| | `GET` | `/api/projects/{projectCode}/ai/productivity` | Assigned Member / Owner |
| | `GET` | `/api/projects/{projectCode}/ai/analyses` | Assigned Member / Owner (Project-wide AI history) |
| | `GET` | `/api/projects/{projectCode}/ai/analyses/{analysisId}` | Assigned Member / Owner (Analysis by ID) |
| **Notifications**| `GET` | `/api/notifications` | Authenticated User (Paginated) |
| | `PATCH` | `/api/notifications/{notificationId}/read` | Authenticated Recipient |

---

## 4. Prerequisites

- **Java**: JDK 21 or newer (`java -version`)
- **Maven**: Version 3.8+ (`mvn -version`)
- **Supabase Account**: A free Supabase project for PostgreSQL and Supabase Auth.

---

## 5. Configuration & Environment Variables

Copy `.env.example` to `.env` or set the environment variables in your environment:

```bash
cp .env.example .env
```

Key environment variables:
| Variable | Description | Example |
|---|---|---|
| `DB_URL` | Supabase PostgreSQL JDBC connection URL | `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Supabase database user | `postgres` |
| `DB_PASSWORD` | Supabase database password | `your-db-password` |
| `SUPABASE_URL` | Supabase project URL | `https://<ref>.supabase.co` |
| `SUPABASE_JWKS_URL` | Supabase JWKS public keys endpoint | `https://<ref>.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_SERVICE_ROLE_KEY` | Backend service-role key | `eyJh...` |
| `SUPABASE_STORAGE_BUCKET` | Supabase storage bucket | `construction-media` |
| `BUILDER_ACTIVATION_CODE` | Secret invitation code for builder signups | `BUILDER-ACTIVATE-SECRET-2026` |
| `MAIL_HOST` | SMTP server for OTP emails | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP account email | `your-email@gmail.com` |
| `MAIL_PASSWORD` | SMTP app password | `app-specific-password` |
| `ML_SERVICE_ENABLED` | Toggle YOLO11 FastAPI ML microservice integration | `true` or `false` (default: `true`) |
| `ML_SERVICE_URL` | Root URL of teammate's FastAPI service | `http://localhost:8000` |
| `ML_SERVICE_API_KEY` | Optional shared secret API key for FastAPI | `your-secret-key` |
| `ML_SERVICE_PREDICT_ENDPOINT` | Prediction endpoint path on FastAPI | `/predict` |
| `ML_SERVICE_FILE_PARAM_NAME` | Form parameter name for uploaded image | `file` |

---

## 6. How to Build, Test, and Run

### Step 1: Run Schema or Migration in Supabase
- **Fresh Install**: Paste and execute [`supabase_schema.sql`](supabase_schema.sql) in your Supabase SQL Editor. This provisions all **24 production tables**, installs the auth trigger, enables Row Level Security (RLS) on all tables, and revokes direct PostgREST privileges from `anon` and `authenticated`.
- **Existing Database**: Execute [`supabase_migration_timestamps.sql`](supabase_migration_timestamps.sql) to safely convert timestamps to UTC without timezone drift, add daily progress columns, link media files, and create `media_ai_analyses`.

### Step 2: Compile the Project
```bash
mvn clean compile
```

### Step 3: Run the Test Suite
All 126 unit, slice, and context tests execute offline using the portable Maven Surefire configuration:
```bash
mvn test
```

### Step 4: Run the Application Locally
```bash
mvn spring-boot:run
```
Or package to an executable JAR:
```bash
mvn clean package -DskipTests
java -jar target/construction-backend-1.0.0.jar
```

The server starts on `http://localhost:8080`.
Verify health:
```bash
curl http://localhost:8080/api/health
```

---

## 7. Postman Collection

Import [`postman_collection.json`](postman_collection.json) directly into Postman:
1. Open Postman -> **Import** -> Select `postman_collection.json`.
2. In the collection variables, configure:
   - `baseUrl`: `http://localhost:8080/api`
   - `builderToken`: Supabase JWT for Builder
   - `contractorToken`: Supabase JWT for Contractor
   - `clientToken`: Supabase JWT for Client
   - `projectCode`: E.g. `PRJ-9X8K2M`

---

## 8. Database Compatibility

Detailed entity-to-table mappings, foreign keys, and column constraints across all 24 tables are documented in [`DATABASE_COMPATIBILITY_CHECKLIST.md`](DATABASE_COMPATIBILITY_CHECKLIST.md).

---

## 9. External Configuration Requirements

- **Supabase Storage**: Create a private bucket named `construction-media` (Public = OFF).
- **Supabase Client Authentication**: Clients authenticate passwordless via Supabase Email OTP with custom Gmail SMTP.
- **SMTP Server**: For sending contractor 6-digit OTP verification emails, configure Gmail or custom SMTP in `.env`.
- **AI & ML Integration**: The backend contains a fully decoupled integration framework ready for your teammate's YOLO11 Python FastAPI microservice (see [`YOLO11_FASTAPI_INTEGRATION.md`](YOLO11_FASTAPI_INTEGRATION.md)). When `ML_SERVICE_ENABLED=false` or when FastAPI is not yet running, the backend operates 100% normally and all project, photo, and progress functionality executes independently. Secondary visual analysis via `AiVisionClient` and `GeminiVisionClient` remains available as an optional fallback without requiring Gemini credentials for core operations.
