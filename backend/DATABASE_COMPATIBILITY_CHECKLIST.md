# Database Compatibility Checklist: Supabase PostgreSQL Schema

This checklist confirms exact compatibility between all 24 database tables in Supabase PostgreSQL and the JPA entity mappings in the Construction Intelligence backend (`com.construction.entity.*`).

In compliance with production requirements:
- **`spring.jpa.hibernate.ddl-auto=validate`** is enforced in production.
- Entity mappings use exact snake_case table and column names matching the Supabase schema.
- Primary keys are UUIDs (`gen_random_uuid()` in Postgres / `UUID.randomUUID()` in Java).
- All foreign keys and unique constraints are strictly aligned.

---

## 1. `profiles` Table
- **Entity**: `com.construction.entity.Profile`
- **Description**: Stores user profile details linked to Supabase Auth (`auth.users`).

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key (matches `auth.users.id`) |
| `email` | `VARCHAR(255)` | `String` | `NOT NULL`, `UNIQUE` |
| `full_name` | `VARCHAR(255)` | `String` | Nullable |
| `phone_number` | `VARCHAR(50)` | `String` | `UNIQUE` |
| `status` | `VARCHAR(50)` | `AccountStatus` | `NOT NULL` (Enum: `PENDING_ACTIVATION`, `ACTIVE`, `SUSPENDED`) |
| `avatar_url` | `TEXT` | `String` | Nullable |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL`, Updatable = `false` |
| `updated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | Nullable |

---

## 2. `roles` Table
- **Entity**: `com.construction.entity.Role`
- **Description**: Platform roles.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `name` | `VARCHAR(50)` | `RoleType` | `NOT NULL`, `UNIQUE` (Enum: `ROLE_BUILDER`, `ROLE_CONTRACTOR`, `ROLE_CLIENT`) |
| `description` | `VARCHAR(255)` | `String` | Nullable |

---

## 3. `user_roles` Table
- **Entity**: `com.construction.entity.UserRole`
- **Description**: Join table for user role assignments.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `user_id` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `role_id` | `UUID` | `Role` | FK `roles(id)`, `NOT NULL` |
| `assigned_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| *Constraint* | `UNIQUE (user_id, role_id)` | | Prevents duplicate role assignments |

---

## 4. `projects` Table
- **Entity**: `com.construction.entity.Project`
- **Description**: Core construction projects.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_code` | `VARCHAR(32)` | `String` | `NOT NULL`, `UNIQUE` (e.g. `PRJ-9X8K2M`) |
| `name` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `description` | `TEXT` | `String` | Nullable |
| `location` | `VARCHAR(255)` | `String` | Nullable |
| `status` | `VARCHAR(50)` | `ProjectStatus` | `NOT NULL` (Enum: `PLANNING`, `IN_PROGRESS`, `ON_HOLD`, `COMPLETED`, `ARCHIVED`) |
| `planned_start_date` | `DATE` | `LocalDate` | Nullable |
| `planned_end_date` | `DATE` | `LocalDate` | Nullable |
| `actual_start_date` | `DATE` | `LocalDate` | Nullable |
| `actual_end_date` | `DATE` | `LocalDate` | Nullable |
| `created_by` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` (Builder owner) |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| `updated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | Nullable |

---

## 5. `project_members` Table
- **Entity**: `com.construction.entity.ProjectMember`
- **Description**: Project membership and authorization bindings.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `user_id` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `role` | `VARCHAR(50)` | `ProjectMemberRole` | `NOT NULL` (Enum: `OWNER`, `CONTRACTOR`, `CLIENT`) |
| `status` | `VARCHAR(50)` | `MemberStatus` | `NOT NULL` (Enum: `ACTIVE`, `INACTIVE`) |
| `joined_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| *Constraint* | `UNIQUE (project_id, user_id)` | | Unique member per project |

---

## 6. `otp_challenges` Table
- **Entity**: `com.construction.entity.OtpChallenge`
- **Description**: Stores secure SHA-256 hashes for contractor secondary email verification.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `email` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `otp_hash` | `VARCHAR(255)` | `String` | `NOT NULL` (Never store plain OTP) |
| `purpose` | `VARCHAR(50)` | `String` | `NOT NULL` (`CONTRACTOR_LOGIN`) |
| `attempts_count` | `INT` | `int` | `NOT NULL`, Default `0` |
| `max_attempts` | `INT` | `int` | `NOT NULL`, Default `5` |
| `is_used` | `BOOLEAN` | `boolean` | `NOT NULL`, Default `false` |
| `expires_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` (5 minutes from creation) |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 7. `activities` Table
- **Entity**: `com.construction.entity.Activity`
- **Description**: Activity feeds and project timeline events.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `user_id` | `UUID` | `Profile` | FK `profiles(id)`, Nullable |
| `activity_type` | `VARCHAR(100)` | `String` | `NOT NULL` |
| `title` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `description` | `TEXT` | `String` | Nullable |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 8. `project_phases` Table
- **Entity**: `com.construction.entity.ProjectPhase`
- **Description**: Milestones and sequential project phases.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `name` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `description` | `TEXT` | `String` | Nullable |
| `sequence_order` | `INT` | `int` | `NOT NULL`, Default `1` |
| `status` | `VARCHAR(50)` | `PhaseStatus` | `NOT NULL` (Enum: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `DELAYED`) |
| `start_date` | `DATE` | `LocalDate` | Nullable |
| `end_date` | `DATE` | `LocalDate` | Nullable |
| `target_completion_date` | `DATE` | `LocalDate` | Nullable |
| `budget` | `NUMERIC(14, 2)` | `BigDecimal` | Nullable |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| `updated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | Nullable |

---

## 9. `progress_updates` Table
- **Entity**: `com.construction.entity.ProgressUpdate`
- **Description**: Daily/weekly progress updates submitted by contractors or builders.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `phase_id` | `UUID` | `ProjectPhase` | FK `project_phases(id)`, Nullable |
| `submitted_by` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `work_category` | `VARCHAR(100)` | `String` | Nullable (e.g. BRICKWORK, FOUNDATION, CONCRETE_SLAB) |
| `area_completed` | `NUMERIC(12, 2)` | `BigDecimal` | Nullable (Manual non-negative area completed) |
| `area_unit` | `VARCHAR(50)` | `String` | Nullable (SQFT, SQM, CFT, RFT, UNITS) |
| `percentage_completed` | `NUMERIC(5, 2)` | `BigDecimal` | `NOT NULL` (0.00 to 100.00) |
| `title` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `description` | `TEXT` | `String` | Nullable |
| `work_completed_today` | `TEXT` | `String` | Nullable |
| `blockers_delays` | `TEXT` | `String` | Nullable |
| `has_blockers` | `BOOLEAN` | `boolean` | `NOT NULL` |
| `weather_notes` | `VARCHAR(255)` | `String` | Nullable |
| `update_date` | `DATE` | `LocalDate` | `NOT NULL` |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 10. `media_files` Table
- **Entity**: `com.construction.entity.MediaFile`
- **Description**: Metadata for photos and documents stored in Supabase Storage.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `uploaded_by` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `progress_update_id` | `UUID` | `ProgressUpdate` | FK `progress_updates(id)`, Nullable |
| `file_name` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `storage_path` | `VARCHAR(500)` | `String` | `NOT NULL` |
| `file_size` | `BIGINT` | `Long` | `NOT NULL` (Max 25MB) |
| `mime_type` | `VARCHAR(100)` | `String` | `NOT NULL` |
| `media_category` | `VARCHAR(50)` | `MediaCategory` | `NOT NULL` (Enum: `SITE_PHOTO`, `BLUEPRINT`, `INVOICE`, `DOCUMENT`, `OTHER`) |
| `caption` | `TEXT` | `String` | Nullable |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 11. `material_items` Table
- **Entity**: `com.construction.entity.MaterialItem`
- **Description**: Catalog of material items tracked per project.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `item_code` | `VARCHAR(64)` | `String` | `NOT NULL` |
| `name` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `category` | `VARCHAR(100)` | `String` | Nullable |
| `unit_of_measure` | `VARCHAR(50)` | `String` | `NOT NULL` (e.g. `BAGS`, `TONNES`, `KG`) |
| `minimum_threshold` | `NUMERIC(12, 2)` | `BigDecimal` | Low-stock alert threshold |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| `updated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | Nullable |
| *Constraint* | `UNIQUE (project_id, item_code)` | | Unique code per project |

---

## 12. `material_transactions` Table
- **Entity**: `com.construction.entity.MaterialTransaction`
- **Description**: Stock movements (RECEIVED, USED, ADJUSTMENT, RETURN).

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `material_item_id` | `UUID` | `MaterialItem` | FK `material_items(id)`, `NOT NULL` |
| `transaction_type` | `VARCHAR(50)` | `TransactionType` | `NOT NULL` (Enum: `RECEIVED`, `USED`, `ADJUSTMENT`, `RETURN`) |
| `quantity` | `NUMERIC(12, 2)` | `BigDecimal` | `NOT NULL` |
| `unit_price` | `NUMERIC(12, 2)` | `BigDecimal` | Nullable |
| `total_cost` | `NUMERIC(14, 2)` | `BigDecimal` | Nullable |
| `reference_number` | `VARCHAR(100)` | `String` | Nullable |
| `notes` | `TEXT` | `String` | Nullable |
| `recorded_by` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `transaction_date` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 13. `inventory_levels` Table
- **Entity**: `com.construction.entity.InventoryLevel`
- **Description**: Aggregated real-time stock balances per item.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `material_item_id` | `UUID` | `MaterialItem` | FK `material_items(id)`, `NOT NULL` |
| `current_stock` | `NUMERIC(12, 2)` | `BigDecimal` | `NOT NULL`, Default `0` |
| `reserved_stock` | `NUMERIC(12, 2)` | `BigDecimal` | Default `0` |
| `last_updated` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| *Constraint* | `UNIQUE (project_id, material_item_id)` | | 1:1 inventory record per item |

---

## 14. `budget_lines` Table
- **Entity**: `com.construction.entity.BudgetLine`
- **Description**: Planned budget lines per cost category.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `phase_id` | `UUID` | `ProjectPhase` | FK `project_phases(id)`, Nullable |
| `category` | `VARCHAR(50)` | `CostCategory` | `NOT NULL` (Enum: `MATERIALS`, `LABOUR`, `EQUIPMENT`, etc.) |
| `planned_amount` | `NUMERIC(14, 2)` | `BigDecimal` | `NOT NULL` |
| `description` | `TEXT` | `String` | Nullable |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |
| `updated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | Nullable |

---

## 15. `cost_entries` Table
- **Entity**: `com.construction.entity.CostEntry`
- **Description**: Actual expenses incurred.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `budget_line_id` | `UUID` | `BudgetLine` | FK `budget_lines(id)`, Nullable |
| `amount` | `NUMERIC(14, 2)` | `BigDecimal` | `NOT NULL` |
| `expense_date` | `DATE` | `LocalDate` | `NOT NULL` |
| `category` | `VARCHAR(100)` | `String` | `NOT NULL` |
| `vendor` | `VARCHAR(255)` | `String` | Nullable |
| `invoice_number` | `VARCHAR(100)` | `String` | Nullable |
| `description` | `TEXT` | `String` | Nullable |
| `payment_status` | `VARCHAR(50)` | `PaymentStatus` | `NOT NULL` (Enum: `PENDING`, `APPROVED`, `PAID`, `REJECTED`) |
| `recorded_by` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 16. `labour_records` Table
- **Entity**: `com.construction.entity.LabourRecord`
- **Description**: Daily on-site trade labour logging.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `phase_id` | `UUID` | `ProjectPhase` | FK `project_phases(id)`, Nullable |
| `work_date` | `DATE` | `LocalDate` | `NOT NULL` |
| `trade_type` | `VARCHAR(50)` | `TradeType` | `NOT NULL` (Enum: `MASON`, `CARPENTER`, `ELECTRICIAN`, etc.) |
| `head_count` | `INT` | `int` | `NOT NULL`, Min `1` |
| `hours_worked` | `NUMERIC(6, 2)` | `BigDecimal` | `NOT NULL` |
| `hourly_rate` | `NUMERIC(10, 2)` | `BigDecimal` | Nullable |
| `total_cost` | `NUMERIC(14, 2)` | `BigDecimal` | Auto-calculated |
| `remarks` | `TEXT` | `String` | Nullable |
| `recorded_by` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 17. `productivity_metrics` Table
- **Entity**: `com.construction.entity.ProductivityMetric`
- **Description**: Earned-value management (EVM) and productivity index.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `metric_date` | `DATE` | `LocalDate` | `NOT NULL` |
| `metric_name` | `VARCHAR(100)` | `String` | `NOT NULL` |
| `planned_value` | `NUMERIC(14, 2)` | `BigDecimal` | Nullable |
| `earned_value` | `NUMERIC(14, 2)` | `BigDecimal` | Nullable |
| `actual_cost` | `NUMERIC(14, 2)` | `BigDecimal` | Nullable |
| `cost_performance_index` | `NUMERIC(6, 3)` | `BigDecimal` | CPI = EV / AC |
| `schedule_performance_index` | `NUMERIC(6, 3)` | `BigDecimal` | SPI = EV / PV |
| `calculated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 18. `weather_context` Table
- **Entity**: `com.construction.entity.WeatherContext`
- **Description**: Weather logs and site condition delays.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `recorded_date` | `DATE` | `LocalDate` | `NOT NULL` |
| `condition` | `VARCHAR(100)` | `String` | Nullable |
| `temperature` | `NUMERIC(5, 2)` | `BigDecimal` | Nullable |
| `humidity` | `NUMERIC(5, 2)` | `BigDecimal` | Nullable |
| `rainfall_mm` | `NUMERIC(6, 2)` | `BigDecimal` | Nullable |
| `impact_on_work` | `VARCHAR(50)` | `WeatherImpact` | `NOT NULL` (Enum: `NONE`, `MINOR_DELAY`, `SEVERE_DELAY`, `WORK_SUSPENDED`) |
| `notes` | `TEXT` | `String` | Nullable |
| `recorded_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 19. `risk_predictions` Table
- **Entity**: `com.construction.entity.RiskPrediction`
- **Description**: Predictive risks generated by AI models.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `risk_category` | `VARCHAR(50)` | `RiskCategory` | `NOT NULL` (Enum: `SCHEDULE`, `BUDGET`, `QUALITY`, `SAFETY`, `WEATHER`) |
| `risk_level` | `VARCHAR(50)` | `RiskLevel` | `NOT NULL` (Enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) |
| `probability` | `NUMERIC(5, 2)` | `BigDecimal` | Probability percentage (0.00 to 1.00 or 0 to 100) |
| `impact_score` | `NUMERIC(5, 2)` | `BigDecimal` | Impact score (1.00 to 10.00) |
| `prediction_summary` | `TEXT` | `String` | `NOT NULL` |
| `status` | `VARCHAR(50)` | `RiskStatus` | `NOT NULL` (Enum: `ACTIVE`, `MITIGATED`, `RESOLVED`) |
| `generated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 20. `explanation_factors` Table
- **Entity**: `com.construction.entity.ExplanationFactor`
- **Description**: Explainable AI feature weights for risk predictions.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `risk_prediction_id` | `UUID` | `RiskPrediction` | FK `risk_predictions(id)`, `NOT NULL` |
| `factor_name` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `importance_weight` | `NUMERIC(5, 2)` | `BigDecimal` | Relative feature weight |
| `explanation_text` | `TEXT` | `String` | `NOT NULL` |

---

## 21. `recommendations` Table
- **Entity**: `com.construction.entity.Recommendation`
- **Description**: Actionable AI recommendations for risk resolution.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` |
| `risk_prediction_id` | `UUID` | `RiskPrediction` | FK `risk_predictions(id)`, Nullable |
| `category` | `VARCHAR(100)` | `String` | Nullable |
| `priority` | `VARCHAR(50)` | `Priority` | `NOT NULL` (Enum: `LOW`, `MEDIUM`, `HIGH`, `URGENT`) |
| `title` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `action_steps` | `TEXT` | `String` | `NOT NULL` |
| `expected_benefit` | `TEXT` | `String` | Nullable |
| `status` | `VARCHAR(50)` | `RecommendationStatus` | `NOT NULL` (Enum: `PENDING`, `ACCEPTED`, `DISMISSED`, `IMPLEMENTED`) |
| `generated_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 22. `notifications` Table
- **Entity**: `com.construction.entity.Notification`
- **Description**: User alerts, invitations, low-stock warnings, and progress notices.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `recipient_id` | `UUID` | `Profile` | FK `profiles(id)`, `NOT NULL` |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, Nullable |
| `title` | `VARCHAR(255)` | `String` | `NOT NULL` |
| `message` | `TEXT` | `String` | `NOT NULL` |
| `type` | `VARCHAR(50)` | `NotificationType` | `NOT NULL` (Enum: `SYSTEM`, `INVITATION`, `PROGRESS_UPDATE`, `RISK_ALERT`, `INVENTORY_WARNING`, `BUDGET_ALERT`) |
| `is_read` | `BOOLEAN` | `boolean` | `NOT NULL`, Default `false` |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 23. `audit_logs` Table
- **Entity**: `com.construction.entity.AuditLog`
- **Description**: Security and mutation audit trails.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `entity_name` | `VARCHAR(100)` | `String` | `NOT NULL` (e.g. `projects`, `cost_entries`, `material_transactions`) |
| `entity_id` | `VARCHAR(100)` | `String` | `NOT NULL` |
| `action` | `VARCHAR(50)` | `AuditAction` | `NOT NULL` (Enum: `CREATE`, `UPDATE`, `DELETE`, `VERIFY`, `ACTIVATE`) |
| `performed_by` | `UUID` | `Profile` | FK `profiles(id)`, Nullable |
| `details` | `TEXT` | `String` | Mutation summary |
| `ip_address` | `VARCHAR(45)` | `String` | Nullable |
| `timestamp` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL` |

---

## 24. `media_ai_analyses` Table
- **Entity**: `com.construction.entity.MediaAiAnalysis`
- **Description**: Persistent AI site photograph visual analysis history.

| Column Name | Database Type | JPA Type | Constraints / Notes |
|---|---|---|---|
| `id` | `UUID` | `UUID` | Primary Key |
| `media_file_id` | `UUID` | `MediaFile` | FK `media_files(id)`, `NOT NULL` (Cascade delete) |
| `project_id` | `UUID` | `Project` | FK `projects(id)`, `NOT NULL` (Cascade delete) |
| `analyzed_by` | `UUID` | `Profile` | FK `profiles(id)`, Nullable |
| `status` | `VARCHAR(50)` | `AiAnalysisStatus` | `NOT NULL` (Enum: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`) |
| `ai_provider` | `VARCHAR(100)` | `String` | Nullable (e.g. `GEMINI`) |
| `ai_model` | `VARCHAR(100)` | `String` | Nullable (e.g. `gemini-1.5-flash`) |
| `analysis_summary` | `TEXT` | `String` | Nullable (Comprehensive visual analysis summary) |
| `detected_work_category` | `VARCHAR(100)` | `String` | Nullable (AI-detected category, distinct from manual contractor report) |
| `estimated_completion_percentage` | `NUMERIC(5, 2)` | `BigDecimal` | Nullable (AI estimate, distinct from manual contractor report) |
| `detected_issues` | `JSONB` | `String` | Nullable (`@JdbcTypeCode(SqlTypes.JSON)` JSON array of concerns) |
| `recommendations` | `JSONB` | `String` | Nullable (`@JdbcTypeCode(SqlTypes.JSON)` JSON array of recommendations) |
| `raw_analysis` | `JSONB` | `String` | Nullable (`@JdbcTypeCode(SqlTypes.JSON)` Full raw provider payload) |
| `error_message` | `TEXT` | `String` | Nullable (Recorded when status is `FAILED`) |
| `created_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | `NOT NULL`, Updatable = `false` |
| `completed_at` | `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` | Nullable |

---

## 25. Row Level Security & Privilege Matrix

All 24 tables have Row Level Security (RLS) enabled in Supabase SQL (`ALTER TABLE <table_name> ENABLE ROW LEVEL SECURITY;`).
Direct client privileges from `anon` and `authenticated` roles are revoked so that all database interactions are mediated fail-closed through the Spring Boot backend (`postgres` / `service_role`).

```sql
SELECT tablename, rowsecurity AS rls_enabled
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
All 24 tables must return `rls_enabled = true`.
