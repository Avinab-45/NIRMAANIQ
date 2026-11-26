-- ===================================================================
-- NirmaanIQ: Supabase Database Migration (For Existing Databases)
-- Target: Migrate existing 23-table schema to 24-table production schema
-- Features:
--   1. Explicit UTC Timestamp Conversion (TIMESTAMPTZ -> TIMESTAMP WITHOUT TIME ZONE)
--      Guarantees ZERO timestamp drift regardless of client/session timezone.
--   2. Preserves all existing IDs, foreign keys, constraints, and data records.
--   3. Adds daily construction progress tracking columns to progress_updates.
--   4. Links site photos in media_files to daily progress reports.
--   5. Creates 24th table media_ai_analyses with JSONB issue/recommendation storage.
--   6. Enforces Row Level Security (RLS) across all 24 public tables.
--   7. Revokes direct PostgREST privileges from anon and authenticated roles.
-- ===================================================================

BEGIN;

-- 1. profiles
ALTER TABLE profiles 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 2. user_roles
ALTER TABLE user_roles 
    ALTER COLUMN assigned_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (assigned_at AT TIME ZONE 'UTC'),
    ALTER COLUMN assigned_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 3. projects
ALTER TABLE projects 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 4. project_members
ALTER TABLE project_members 
    ALTER COLUMN joined_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (joined_at AT TIME ZONE 'UTC'),
    ALTER COLUMN joined_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 5. otp_challenges
ALTER TABLE otp_challenges 
    ALTER COLUMN expires_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (expires_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 6. activities
ALTER TABLE activities 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 7. project_phases
ALTER TABLE project_phases 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 8. progress_updates
ALTER TABLE progress_updates 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- Add daily construction progress fields if not already present
ALTER TABLE progress_updates
    ADD COLUMN IF NOT EXISTS work_category VARCHAR(100),
    ADD COLUMN IF NOT EXISTS area_completed NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS area_unit VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_progress_date ON progress_updates(project_id, update_date DESC);

-- 9. media_files
ALTER TABLE media_files 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- Link media files to daily progress reports
ALTER TABLE media_files
    ADD COLUMN IF NOT EXISTS progress_update_id UUID REFERENCES progress_updates(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_media_progress ON media_files(progress_update_id);

-- 10. material_items
ALTER TABLE material_items 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 11. material_transactions
ALTER TABLE material_transactions 
    ALTER COLUMN transaction_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (transaction_date AT TIME ZONE 'UTC'),
    ALTER COLUMN transaction_date SET DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 12. inventory_levels
ALTER TABLE inventory_levels 
    ALTER COLUMN last_updated TYPE TIMESTAMP WITHOUT TIME ZONE USING (last_updated AT TIME ZONE 'UTC'),
    ALTER COLUMN last_updated SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 13. budget_lines
ALTER TABLE budget_lines 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 14. cost_entries
ALTER TABLE cost_entries 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 15. labour_records
ALTER TABLE labour_records 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 16. productivity_metrics
ALTER TABLE productivity_metrics 
    ALTER COLUMN calculated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (calculated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN calculated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 17. weather_context
ALTER TABLE weather_context 
    ALTER COLUMN recorded_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (recorded_at AT TIME ZONE 'UTC'),
    ALTER COLUMN recorded_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 18. risk_predictions
ALTER TABLE risk_predictions 
    ALTER COLUMN generated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (generated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN generated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 19. recommendations
ALTER TABLE recommendations 
    ALTER COLUMN generated_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (generated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN generated_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 20. notifications
ALTER TABLE notifications 
    ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 21. audit_logs
ALTER TABLE audit_logs 
    ALTER COLUMN timestamp TYPE TIMESTAMP WITHOUT TIME ZONE USING (timestamp AT TIME ZONE 'UTC'),
    ALTER COLUMN timestamp SET DEFAULT (NOW() AT TIME ZONE 'UTC');

-- 22. media_ai_analyses (Table 24 - Persistent AI Vision Analysis History)
CREATE TABLE IF NOT EXISTS media_ai_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_file_id UUID NOT NULL REFERENCES media_files(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    analyzed_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ai_provider VARCHAR(100),
    ai_model VARCHAR(100),
    analysis_summary TEXT,
    detected_work_category VARCHAR(100),
    estimated_completion_percentage NUMERIC(5, 2),
    detected_issues JSONB,
    recommendations JSONB,
    raw_analysis JSONB,
    error_message TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    completed_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_ai_analysis_media ON media_ai_analyses(media_file_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analysis_project ON media_ai_analyses(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_analysis_status ON media_ai_analyses(status);

-- 23. AUTOMATIC PROFILE CREATION TRIGGER (auth.users -> public.profiles)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, status, created_at, updated_at)
    VALUES (
        new.id,
        new.email,
        COALESCE(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''),
        'PENDING_ACTIVATION',
        (NOW() AT TIME ZONE 'UTC'),
        (NOW() AT TIME ZONE 'UTC')
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        updated_at = (NOW() AT TIME ZONE 'UTC');
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 24. ROW LEVEL SECURITY (RLS) ON ALL 24 TABLES
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

-- 25. PRIVILEGE SETUP (FAIL-CLOSED DIRECT POSTGREST ACCESS)
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;
REVOKE ALL ON ALL ROUTINES IN SCHEMA public FROM anon, authenticated;

ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM anon, authenticated;

GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO postgres, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO postgres, service_role;

COMMIT;

-- ===================================================================
-- 26. SQL VERIFICATION QUERY
-- Run this query after migration to verify RLS is enabled on all 24 tables:
-- ===================================================================
-- Expected Output: Exactly 24 rows with rls_enabled = true
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
