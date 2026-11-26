-- ===================================================================
-- NirmaanIQ: AI-Powered Construction Intelligence Platform
-- Complete PostgreSQL Database Schema for Supabase
-- Target Tables: 24 Production Tables (Fresh Installation)
-- ===================================================================

-- Enable pgcrypto extension for UUID generation if not already active
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. PROFILES
-- Maps to Supabase Auth users (auth.users)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    phone_number VARCHAR(50) UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ACTIVATION', -- PENDING_ACTIVATION, ACTIVE, SUSPENDED
    avatar_url TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. ROLES
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE, -- ROLE_BUILDER, ROLE_CONTRACTOR, ROLE_CLIENT
    description VARCHAR(255)
);

-- Seed basic roles
INSERT INTO roles (id, name, description)
VALUES 
    (gen_random_uuid(), 'ROLE_BUILDER', 'Master builder with project creation and management rights'),
    (gen_random_uuid(), 'ROLE_CONTRACTOR', 'Contractor with operational site update and tracking rights'),
    (gen_random_uuid(), 'ROLE_CLIENT', 'Client with read-only project access')
ON CONFLICT (name) DO NOTHING;

-- 3. USER_ROLES
CREATE TABLE IF NOT EXISTS user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

-- 4. PROJECTS
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_code VARCHAR(32) NOT NULL UNIQUE, -- Unique human-readable code e.g. PRJ-9X8K2M
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING', -- PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, ARCHIVED
    planned_start_date DATE,
    planned_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    created_by UUID NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_projects_code ON projects(project_code);
CREATE INDEX IF NOT EXISTS idx_projects_created_by ON projects(created_by);

-- 5. PROJECT_MEMBERS
CREATE TABLE IF NOT EXISTS project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL, -- OWNER, CONTRACTOR, CLIENT
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    joined_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_project_members_user ON project_members(user_id);
CREATE INDEX IF NOT EXISTS idx_project_members_project ON project_members(project_id);

-- 6. OTP_CHALLENGES
-- Stores hashed contractor verification codes (5 min expiry, max 5 attempts, one-time use)
CREATE TABLE IF NOT EXISTS otp_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL DEFAULT 'CONTRACTOR_LOGIN',
    attempts_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_otp_email_purpose ON otp_challenges(email, purpose);

-- 7. ACTIVITIES
CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    activity_type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_activities_project ON activities(project_id, created_at DESC);

-- 8. PROJECT_PHASES
CREATE TABLE IF NOT EXISTS project_phases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_order INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED
    start_date DATE,
    end_date DATE,
    target_completion_date DATE,
    budget NUMERIC(14, 2),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_phases_project ON project_phases(project_id, sequence_order ASC);

-- 9. PROGRESS_UPDATES
CREATE TABLE IF NOT EXISTS progress_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phase_id UUID REFERENCES project_phases(id) ON DELETE SET NULL,
    submitted_by UUID NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    work_category VARCHAR(100),
    area_completed NUMERIC(12, 2),
    area_unit VARCHAR(50),
    percentage_completed NUMERIC(5, 2) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    work_completed_today TEXT,
    blockers_delays TEXT,
    has_blockers BOOLEAN NOT NULL DEFAULT FALSE,
    weather_notes VARCHAR(255),
    update_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_progress_project ON progress_updates(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_progress_date ON progress_updates(project_id, update_date DESC);

-- 10. MEDIA_FILES
-- Integrated with Supabase Storage bucket ('construction-media')
CREATE TABLE IF NOT EXISTS media_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    progress_update_id UUID REFERENCES progress_updates(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    media_category VARCHAR(50) NOT NULL, -- SITE_PHOTO, BLUEPRINT, INVOICE, DOCUMENT, OTHER
    caption TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_media_project ON media_files(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_media_progress ON media_files(progress_update_id);

-- 11. MATERIAL_ITEMS
CREATE TABLE IF NOT EXISTS material_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    item_code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    unit_of_measure VARCHAR(50) NOT NULL, -- BAGS, KG, TONNES, LITRES, SQFT, UNITS
    minimum_threshold NUMERIC(12, 2) DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_material_code UNIQUE (project_id, item_code)
);
CREATE INDEX IF NOT EXISTS idx_material_project ON material_items(project_id);

-- 12. MATERIAL_TRANSACTIONS
CREATE TABLE IF NOT EXISTS material_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    material_item_id UUID NOT NULL REFERENCES material_items(id) ON DELETE CASCADE,
    transaction_type VARCHAR(50) NOT NULL, -- RECEIVED, USED, ADJUSTMENT, RETURN
    quantity NUMERIC(12, 2) NOT NULL,
    unit_price NUMERIC(12, 2),
    total_cost NUMERIC(14, 2),
    reference_number VARCHAR(100),
    notes TEXT,
    recorded_by UUID NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    transaction_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_mat_trans_project ON material_transactions(project_id, transaction_date DESC);

-- 13. INVENTORY_LEVELS
CREATE TABLE IF NOT EXISTS inventory_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    material_item_id UUID NOT NULL REFERENCES material_items(id) ON DELETE CASCADE,
    current_stock NUMERIC(12, 2) NOT NULL DEFAULT 0,
    reserved_stock NUMERIC(12, 2) DEFAULT 0,
    last_updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_inventory_item UNIQUE (project_id, material_item_id)
);
CREATE INDEX IF NOT EXISTS idx_inventory_project ON inventory_levels(project_id);

-- 14. BUDGET_LINES
CREATE TABLE IF NOT EXISTS budget_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phase_id UUID REFERENCES project_phases(id) ON DELETE SET NULL,
    category VARCHAR(50) NOT NULL, -- MATERIALS, LABOUR, EQUIPMENT, SUBCONTRACTOR, PERMITS, OVERHEAD, CONTINGENCY, OTHER
    planned_amount NUMERIC(14, 2) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_budget_project ON budget_lines(project_id);

-- 15. COST_ENTRIES
CREATE TABLE IF NOT EXISTS cost_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    budget_line_id UUID REFERENCES budget_lines(id) ON DELETE SET NULL,
    amount NUMERIC(14, 2) NOT NULL,
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category VARCHAR(100) NOT NULL,
    vendor VARCHAR(255),
    invoice_number VARCHAR(100),
    description TEXT,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, PAID, REJECTED
    recorded_by UUID NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_cost_entries_project ON cost_entries(project_id, expense_date DESC);

-- 16. LABOUR_RECORDS
CREATE TABLE IF NOT EXISTS labour_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phase_id UUID REFERENCES project_phases(id) ON DELETE SET NULL,
    work_date DATE NOT NULL DEFAULT CURRENT_DATE,
    trade_type VARCHAR(50) NOT NULL, -- MASON, CARPENTER, ELECTRICIAN, PLUMBER, LABOURER, SUPERVISOR, OTHER
    head_count INT NOT NULL DEFAULT 1,
    hours_worked NUMERIC(6, 2) NOT NULL,
    hourly_rate NUMERIC(10, 2),
    total_cost NUMERIC(14, 2),
    remarks TEXT,
    recorded_by UUID NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_labour_project ON labour_records(project_id, work_date DESC);

-- 17. PRODUCTIVITY_METRICS
CREATE TABLE IF NOT EXISTS productivity_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    planned_value NUMERIC(14, 2),
    earned_value NUMERIC(14, 2),
    actual_cost NUMERIC(14, 2),
    cost_performance_index NUMERIC(6, 3),
    schedule_performance_index NUMERIC(6, 3),
    calculated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_productivity_project ON productivity_metrics(project_id, metric_date DESC);

-- 18. WEATHER_CONTEXT
CREATE TABLE IF NOT EXISTS weather_context (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    recorded_date DATE NOT NULL,
    condition VARCHAR(100),
    temperature NUMERIC(5, 2),
    humidity NUMERIC(5, 2),
    rainfall_mm NUMERIC(6, 2),
    impact_on_work VARCHAR(50) NOT NULL DEFAULT 'NONE', -- NONE, MINOR_DELAY, SEVERE_DELAY, WORK_SUSPENDED
    notes TEXT,
    recorded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_weather_project ON weather_context(project_id, recorded_date DESC);

-- 19. RISK_PREDICTIONS
CREATE TABLE IF NOT EXISTS risk_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    risk_category VARCHAR(50) NOT NULL, -- SCHEDULE, BUDGET, QUALITY, SAFETY, WEATHER
    risk_level VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    probability NUMERIC(5, 2),
    impact_score NUMERIC(5, 2),
    prediction_summary TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, MITIGATED, RESOLVED
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_risk_project ON risk_predictions(project_id, generated_at DESC);

-- 20. EXPLANATION_FACTORS
CREATE TABLE IF NOT EXISTS explanation_factors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_prediction_id UUID NOT NULL REFERENCES risk_predictions(id) ON DELETE CASCADE,
    factor_name VARCHAR(255) NOT NULL,
    importance_weight NUMERIC(5, 2),
    explanation_text TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_factors_risk ON explanation_factors(risk_prediction_id);

-- 21. RECOMMENDATIONS
CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    risk_prediction_id UUID REFERENCES risk_predictions(id) ON DELETE SET NULL,
    category VARCHAR(100),
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    title VARCHAR(255) NOT NULL,
    action_steps TEXT NOT NULL,
    expected_benefit TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DISMISSED, IMPLEMENTED
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_recommendations_project ON recommendations(project_id, generated_at DESC);

-- 22. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- SYSTEM, INVITATION, PROGRESS_UPDATE, RISK_ALERT, INVENTORY_WARNING, BUDGET_ALERT
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id, created_at DESC);

-- 23. AUDIT_LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, VERIFY, ACTIVATE
    performed_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    details TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp DESC);

-- 24. MEDIA_AI_ANALYSES
-- Persistent AI site photograph visual analysis history
CREATE TABLE IF NOT EXISTS media_ai_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_file_id UUID NOT NULL REFERENCES media_files(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    analyzed_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    ai_provider VARCHAR(100),
    ai_model VARCHAR(100),
    analysis_summary TEXT,
    detected_work_category VARCHAR(100),
    estimated_completion_percentage NUMERIC(5, 2),
    detected_issues JSONB,
    recommendations JSONB,
    raw_analysis JSONB,
    error_message TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_ai_analysis_media ON media_ai_analyses(media_file_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analysis_project ON media_ai_analyses(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analysis_status ON media_ai_analyses(status);

-- ===================================================================
-- 25. AUTOMATIC PROFILE CREATION TRIGGER (auth.users -> public.profiles)
-- ===================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, status, created_at, updated_at)
    VALUES (
        new.id,
        new.email,
        COALESCE(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''),
        'PENDING_ACTIVATION',
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        updated_at = NOW();
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ===================================================================
-- 26. ROW LEVEL SECURITY (RLS) ON ALL 24 PUBLIC TABLES
-- ===================================================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_phases ENABLE ROW LEVEL SECURITY;
ALTER TABLE progress_updates ENABLE ROW LEVEL SECURITY;
ALTER TABLE media_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE material_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE material_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_levels ENABLE ROW LEVEL SECURITY;
ALTER TABLE budget_lines ENABLE ROW LEVEL SECURITY;
ALTER TABLE cost_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE labour_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE productivity_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE weather_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE risk_predictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE explanation_factors ENABLE ROW LEVEL SECURITY;
ALTER TABLE recommendations ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE media_ai_analyses ENABLE ROW LEVEL SECURITY;

-- ===================================================================
-- 27. PRIVILEGE SETUP (FAIL-CLOSED DIRECT POSTGREST ACCESS)
-- Direct client access via anon or authenticated roles is completely revoked.
-- All operations are mediated exclusively through the Spring Boot backend.
-- ===================================================================
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;
REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM anon, authenticated;

ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM anon, authenticated;

-- Preserve backend access for PostgreSQL superuser and service_role
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO postgres, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO postgres, service_role;

-- ===================================================================
-- 28. SQL VERIFICATION QUERY
-- Run this query to verify RLS is enabled on all 24 public application tables
-- ===================================================================
-- Expected Result: Exactly 24 rows returned with rls_enabled = true
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
