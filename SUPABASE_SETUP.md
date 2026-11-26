# Supabase Configuration & Connection Guide

This guide walks you through connecting your Supabase project to the **NirmaanIQ** Spring Boot 3.3.5 / Java 21 backend.

---

## 1. Supabase Database Setup

NirmaanIQ operates with a production PostgreSQL database containing **24 application tables**. Hibernate enforces schema compliance on startup via `spring.jpa.hibernate.ddl-auto=validate`.

### 1.1 Choose Your Connection Mode: Session Pooler vs Direct Connection

| Mode | Host Pattern | Port | IPv4 Support | Recommended For |
|---|---|---|---|---|
| **Session Pooler** *(Recommended)* | `aws-0-<region>.pooler.supabase.com` | `5432` | **Yes (Full)** | Standard broadband, local development, cloud VMs |
| **Transaction Pooler** | `aws-0-<region>.pooler.supabase.com` | `6543` | **Yes (Full)** | High-concurrency serverless |
| **Direct Connection** | `db.<project-ref>.supabase.co` | `5432` | IPv6 Only (in many networks) | Networks with native IPv6 routing |

> [!IMPORTANT]
> If your ISP or local network does not support IPv6, direct connections to `db.<project-ref>.supabase.co:5432` will fail with `Connection refused` or DNS resolution errors. **Always use the Session Pooler (`aws-0-<region>.pooler.supabase.com:5432`) for IPv4 environments.**

#### Configuring `.env`:
```env
# Session Pooler Example (IPv4 compatible):
DB_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.your-supabase-project-ref
DB_PASSWORD=your-database-password
```

---

### 1.2 Fresh Database Setup (New Supabase Projects)

If you are setting up a fresh Supabase database:

1. Open your [Supabase Dashboard](https://supabase.com/dashboard).
2. Navigate to **SQL Editor** -> **New query**.
3. Open [`supabase_schema.sql`](supabase_schema.sql) in this repository.
4. Copy its entire contents, paste it into the Supabase SQL Editor, and click **Run**.
5. This script executes a complete fresh installation:
   - Creates all **24 tables** with UUID primary keys and `TIMESTAMP WITHOUT TIME ZONE` data types.
   - Creates foreign keys, unique constraints, and optimized composite indexes.
   - Installs the `on_auth_user_created` trigger for automated profile provisioning from `auth.users`.
   - Enables Row Level Security (RLS) on all 24 tables.
   - Revokes direct PostgREST client access from `anon` and `authenticated` roles.
   - Grants full access to `postgres` and `service_role`.

---

### 1.3 Existing Database Migration (Migrating Prior 23-Table Schema)

If you have an existing Supabase database with data created previously:

1. Open your [Supabase Dashboard](https://supabase.com/dashboard).
2. Navigate to **SQL Editor** -> **New query**.
3. Open [`supabase_migration_timestamps.sql`](supabase_migration_timestamps.sql) in this repository.
4. Copy its entire contents, paste it into the Supabase SQL Editor, and click **Run**.
5. This migration script is safe and non-destructive:
   - Converts all `TIMESTAMPTZ` columns to `TIMESTAMP WITHOUT TIME ZONE USING (column_name AT TIME ZONE 'UTC')`, guaranteeing zero timezone drift regardless of client session timezone.
   - Adds daily construction progress tracking columns to `progress_updates` (`work_category`, `area_completed`, `area_unit`).
   - Links site photographs in `media_files` to daily reports via `progress_update_id`.
   - Provisions the 24th table `media_ai_analyses` with JSONB detection and recommendation storage.
   - Installs the profile provisioning trigger.
   - Enables RLS on all 24 tables and revokes direct table privileges from `anon` and `authenticated`.

---

### 1.4 Verifying Row Level Security (RLS) on All 24 Tables

Execute the following verification query in the Supabase SQL Editor:

```sql
SELECT 
    schemaname,
    tablename,
    rowsecurity AS rls_enabled
FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename IN (
    'profiles', 'roles', 'user_roles', 'projects', 'project_members', 
    'otp_challenges', 'activities', 'project_phases', 'progress_updates', 
    'media_files', 'material_items', 'material_transactions', 'inventory_levels', 
    'budget_lines', 'cost_entries', 'labour_records', 'productivity_metrics', 
    'weather_context', 'risk_predictions', 'explanation_factors', 'recommendations', 
    'notifications', 'audit_logs', 'media_ai_analyses'
  )
ORDER BY tablename;
```
**Expected Result**: Exactly 24 rows returned with `rls_enabled = true`.

---

## 2. Supabase Authentication Setup

### 2.1 Enable Email Provider & Email OTP
1. In Supabase Dashboard, go to **Authentication** -> **Providers** -> **Email**.
2. Toggle **Enable Email provider** to **ON**.
3. Toggle **Confirm email** to **ON** (Ensures users must verify their email address before accessing platform features).
4. For Clients/Consumers: Toggle **Email OTP** / **Magic Link** to **ON**. Clients authenticate on the frontend without a password by requesting a one-time passcode delivered to their email.

### 2.2 Configure Custom SMTP (Gmail SMTP)
1. Go to **Project Settings** -> **Authentication** -> **SMTP Settings**.
2. Toggle **Enable Custom SMTP** to **ON**.
3. Enter credentials:
   - **Sender Email**: `your-email@gmail.com`
   - **Sender Name**: `NirmaanIQ Platform`
   - **Host**: `smtp.gmail.com`
   - **Port**: `587`
   - **Encryption**: `TLS`
   - **Username**: `your-email@gmail.com`
   - **Password**: `<Your 16-character Google App Password>`
4. Save and send a test email.

### 2.3 JWKS & JWT Settings
1. Go to **Project Settings** -> **API**.
2. Under **JWT Settings**, note your project URL:
   ```env
   SUPABASE_URL=https://<your-project-ref>.supabase.co
   SUPABASE_JWKS_URL=https://<your-project-ref>.supabase.co/auth/v1/.well-known/jwks.json
   ```
3. The Spring Security OAuth2 Resource Server automatically validates tokens using the standard Supabase JWKS endpoint.

---

## 3. Supabase Private Storage Setup

Site photographs, blueprints, invoices, and documents are stored in a private Supabase Storage bucket.

### 3.1 Create Bucket
1. Go to **Storage** -> **Buckets** in your Supabase dashboard.
2. Click **New bucket**.
3. Name: `construction-media` (must match `SUPABASE_STORAGE_BUCKET`).
4. **Public bucket**: Ensure this is toggled **OFF** (Keep bucket **Private**).
5. Click **Save bucket**.

### 3.2 Service Role Key
1. Go to **Project Settings** -> **API**.
2. Under **Project API keys**, locate the `service_role` key (labeled *"secret - reveals all data"*).
3. Copy this key into your `.env`:
   ```env
   SUPABASE_SERVICE_ROLE_KEY=your-supabase-service-role-key-here
   ```
4. **Security Notice**: This key is used strictly by the Spring Boot backend to upload media files, download images for AI analysis, generate signed URLs, and perform cleanup. **Never commit this key to version control or expose it in frontend code.**

---

## 4. Required Backend Environment Variables

Create a `.env` file in the project root:

```env
# Server
SERVER_PORT=8080

# CORS Allowed Origins
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080

# Database (Session Pooler)
DB_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.your-supabase-project-ref
DB_PASSWORD=your-database-password

# Supabase Auth & Storage
SUPABASE_URL=https://your-supabase-project-ref.supabase.co
SUPABASE_JWKS_URL=https://your-supabase-project-ref.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_SERVICE_ROLE_KEY=your-supabase-service-role-key
SUPABASE_STORAGE_BUCKET=construction-media

# Email (Contractor OTP & System Notifications)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-google-app-password
MAIL_FROM=your-email@gmail.com

# Builder Activation (Mandatory secret code)
BUILDER_ACTIVATION_CODE=your-strong-random-builder-activation-secret

# AI Insights (Optional)
AI_PROVIDER_ENABLED=false
AI_PROVIDER_API_KEY=
```

---

## 5. Running Tests and Starting the Backend

### Run Test Suite:
```powershell
mvn clean test
```
All **105 unit and slice tests** will run with zero failures or errors using the portable Maven Surefire configuration.

### Run Backend Application:
```powershell
mvn spring-boot:run
```
Upon startup, Hibernate validates all 24 entities against your Supabase database schema (`ddl-auto=validate`). Once started, check application and database health:
```bash
curl http://localhost:8080/api/health
```
**Expected Response**:
```json
{
  "success": true,
  "message": "Service is running smoothly",
  "data": {
    "status": "UP",
    "service": "construction-backend",
    "version": "1.0.0",
    "database": "Supabase PostgreSQL (Connected)"
  }
}
```
