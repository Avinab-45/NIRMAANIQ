import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserRole } from '../../types/auth';

interface RoleCardData {
  role: UserRole;
  name: string;
  category: string;
  description: string;
  icon: string;
  iconBg: string;
  iconColor: string;
  badgeText: string;
  badgeBg: string;
  features: string[];
  loginPath: string;
  registerPath: string;
}

const ROLES: RoleCardData[] = [
  {
    role: 'organisation',
    name: 'Organisation',
    category: 'Enterprise Builders & EPCs',
    description:
      'Manage multi-site construction portfolios, coordinate contractor packages, ingest 4D BIM twins, and monitor autonomous machine risk alerts.',
    icon: 'domain',
    iconBg: 'bg-primary-fixed',
    iconColor: 'text-primary',
    badgeText: 'PORTFOLIO OS',
    badgeBg: 'bg-primary-fixed/40 text-primary',
    features: [
      'Autonomous LiDAR & Point-cloud Auditing',
      'Unified Developer Portfolio Cockpit',
      'Subcontractor Ledger & Muster Reconciliation',
      'AI Schedule Variance & Monsoon Curing Alerts',
    ],
    loginPath: '/portal/organisation/login',
    registerPath: '/portal/organisation/register',
  },
  {
    role: 'contractor',
    name: 'Contractor',
    category: 'Field Execution & Trade Crews',
    description:
      'Execute assigned packages, log multi-lingual voice Daily Progress Reports (DPR), report delay anomalies, and synchronize labor biometrics.',
    icon: 'engineering',
    iconBg: 'bg-secondary-fixed',
    iconColor: 'text-on-secondary-fixed-variant',
    badgeText: 'JOBSITE TELEMETRY',
    badgeBg: 'bg-secondary-fixed/50 text-secondary',
    features: [
      'Voice-to-Text DPR in Hindi, English & Odia',
      'Labor Biometric Gate Sync & Turnstile OCR',
      'Geo-fenced Snag Logging with Offline Sync',
      'Material Inward Gate-Passes & Weighbridge Sync',
    ],
    loginPath: '/portal/contractor/login',
    registerPath: '/portal/contractor/register',
  },
  {
    role: 'client',
    name: 'Client',
    category: 'Owners & Infrastructure Investors',
    description:
      'Submit new project requirements, track verified milestone progressions, view 3D digital twins, and monitor statutory RERA escrow compliance.',
    icon: 'real_estate_agent',
    iconBg: 'bg-tertiary-fixed',
    iconColor: 'text-on-tertiary-fixed-variant',
    badgeText: 'STAKEHOLDER TRUST',
    badgeBg: 'bg-tertiary-fixed/50 text-tertiary',
    features: [
      'New Project Scope & Budget Submission',
      'Live 3D Construction Milestone Walkthroughs',
      'RERA Milestone Escrow Release Verification',
      'Direct Builder Communication & Photo Feeds',
    ],
    loginPath: '/portal/client/login',
    registerPath: '/portal/client/register',
  },
];

export const RoleSelectionPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-surface flex flex-col justify-between">
      {/* Top Navigation Bar */}
      <header className="w-full bg-surface/90 backdrop-blur-md border-b border-outline-variant/30 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-3">
            <img
              alt="NirmaanIQ Logo"
              className="h-8 w-auto object-contain"
              src="https://lh3.googleusercontent.com/aida/ALJJyrBymwrci1_JVMf7mCezzMOqmZtpZ9eGkBO_6QuHL5Ry-7bY0c2ccSROahcSDaEkfpoNZrV2ul5r2eJ7CO-LkANbILtIqT-YgVa8vUzJeo3kfm4IulxlZjsxuWde_9f_mDO2NGBVp0T6TCCgGnYBfTVEsdVpotnNxaLgd75yhJWw4mi3h3kUN0urpFX0c5AU8UOJf-DzIYX-a_N1OkYH6PBwY-3MBqXiW0J6eklJN09b"
            />
            <span className="font-headline-md text-headline-md text-on-surface tracking-tight font-bold">
              Nirmaan<span className="text-primary">IQ</span>
            </span>
          </Link>

          <Link
            to="/"
            className="inline-flex items-center gap-1.5 font-body-sm text-body-sm font-medium text-primary hover:text-primary-container transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">arrow_back</span>
            <span>Back to Marketing Website</span>
          </Link>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-6 lg:px-12 py-12 lg:py-16 flex flex-col items-center">
        {/* Header Tag & Title */}
        <div className="text-center max-w-3xl mx-auto mb-12">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-fixed/40 text-primary font-label-sm text-label-sm font-semibold tracking-wider uppercase mb-4">
            <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
            Role-Calibrated Site Architecture
          </div>
          <h1 className="font-headline-xl text-headline-xl lg:text-[40px] lg:leading-[48px] font-bold text-on-surface tracking-tight">
            Select Your Workspace Role
          </h1>
          <p className="font-body-lg text-body-lg text-on-surface-variant mt-3 leading-relaxed">
            Choose your portal role to access dedicated telemetry, milestone governance, and verified field reporting engines.
          </p>
        </div>

        {/* 3 Distinct Role Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 w-full items-stretch">
          {ROLES.map((role) => (
            <div
              key={role.role}
              className="bg-surface-container-lowest border border-outline-variant/40 hover:border-primary/40 rounded-3xl p-8 flex flex-col justify-between shadow-sm hover:shadow-xl transition-all duration-300 relative group overflow-hidden"
            >
              {/* Subtle top indicator highlight */}
              <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-transparent via-primary-container/30 to-transparent group-hover:via-primary transition-all"></div>

              <div>
                {/* Header row: Icon + Category Badge */}
                <div className="flex items-center justify-between mb-6">
                  <div className={`w-14 h-14 rounded-2xl ${role.iconBg} flex items-center justify-center ${role.iconColor} shadow-sm`}>
                    <span className="material-symbols-outlined text-[32px]">{role.icon}</span>
                  </div>
                  <span className={`px-3 py-1 rounded-full font-label-sm text-[11px] font-bold uppercase tracking-wider ${role.badgeBg}`}>
                    {role.badgeText}
                  </span>
                </div>

                <div className="font-label-sm text-label-sm text-outline font-semibold uppercase tracking-wider mb-1">
                  {role.category}
                </div>

                <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface mb-3 group-hover:text-primary transition-colors">
                  {role.name}
                </h2>

                <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed mb-6">
                  {role.description}
                </p>

                {/* Key Capabilities */}
                <div className="space-y-2.5 mb-8 pt-4 border-t border-outline-variant/30">
                  <div className="font-label-sm text-[11px] text-outline uppercase font-semibold mb-2">
                    Key Telemetry Features:
                  </div>
                  {role.features.map((feat, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 font-body-sm text-body-sm text-on-surface">
                      <span className="material-symbols-outlined text-secondary text-[16px] shrink-0 mt-0.5">
                        check_circle
                      </span>
                      <span>{feat}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Bottom Card Actions */}
              <div className="pt-4 border-t border-outline-variant/30 space-y-3">
                <button
                  type="button"
                  onClick={() => navigate(role.loginPath)}
                  className="w-full h-12 inline-flex items-center justify-center gap-2 rounded-xl bg-primary text-on-primary font-headline-sm text-headline-sm font-semibold hover:bg-primary-container hover:text-on-primary-container transition-all shadow-md group-hover:shadow-lg cursor-pointer"
                >
                  <span>Continue as {role.name}</span>
                  <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                </button>

                <div className="flex items-center justify-between text-body-sm px-1">
                  <span className="text-on-surface-variant text-[13px]">New to NirmaanIQ?</span>
                  <Link
                    to={role.registerPath}
                    className="text-primary font-semibold hover:underline text-[13px] flex items-center gap-0.5"
                  >
                    <span>Register Account</span>
                    <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Security & System Readiness Footer Strip */}
        <div className="mt-12 flex flex-wrap items-center justify-center gap-6 text-label-sm text-label-sm text-outline-variant">
          <span className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-secondary"></span>
            <span>Role-Based Telemetry Isolation</span>
          </span>
          <span>•</span>
          <span>Dual-Factor CAPTCHA + OTP Protection</span>
          <span>•</span>
          <span>RERA Statutory Framework Compatible</span>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full py-6 text-center text-body-sm text-on-surface-variant/70 border-t border-outline-variant/20">
        <p>© 2025 NirmaanIQ Technologies India Pvt. Ltd. • Enterprise Construction Intelligence</p>
      </footer>
    </div>
  );
};
