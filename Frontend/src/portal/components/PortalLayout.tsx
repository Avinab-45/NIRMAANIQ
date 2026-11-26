import React from 'react';
import { Link } from 'react-router-dom';
import { UserRole } from '../../types/auth';

interface PortalLayoutProps {
  children: React.ReactNode;
  activeRole?: UserRole;
  title?: string;
  subtitle?: string;
}

const ROLE_CONFIG: Record<UserRole, { name: string; tag: string; icon: string; accentColor: string; highlights: string[] }> = {
  organisation: {
    name: 'Enterprise Organisation',
    tag: 'BUILDER & EPC PLATFORM',
    icon: 'domain',
    accentColor: 'text-primary',
    highlights: [
      'Autonomous LiDAR & 4D BIM Deviation Mapping',
      'Contractor Package Reconciliation & Muster Ledger',
      'AI Concrete Curing Index & Schedule Risk Simulation',
      'Unified Developer Portfolio Cockpit',
    ],
  },
  contractor: {
    name: 'Site Contractor',
    tag: 'WORKSITE EXECUTION ENGINE',
    icon: 'engineering',
    accentColor: 'text-secondary-fixed',
    highlights: [
      'Multi-lingual Voice-to-Text DPR (Hindi, Odia, English)',
      'Subcontractor Turnstile Biometrics & Attendance Sync',
      'Geo-fenced Snag Lists with Offline Field Synchronization',
      'Material Inward Gate-Passes & Weighbridge OCR',
    ],
  },
  client: {
    name: 'Project Client',
    tag: 'INFRASTRUCTURE STAKEHOLDER PORTAL',
    icon: 'real_estate_agent',
    accentColor: 'text-tertiary-fixed-dim',
    highlights: [
      'Live 3D Construction Milestone Walkthroughs',
      'Statutory RERA Escrow Release Validation',
      'Geotagged Photo & Video Progress Verification',
      'Direct Stakeholder Communication & Change Requests',
    ],
  },
};

export const PortalLayout: React.FC<PortalLayoutProps> = ({
  children,
  activeRole,
  title,
  subtitle,
}) => {
  const roleData = activeRole ? ROLE_CONFIG[activeRole] : null;

  return (
    <div className="min-h-screen bg-surface flex flex-col justify-between">
      {/* Top Thin Header Bar */}
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
            <span className="hidden sm:inline-block px-2.5 py-0.5 rounded text-label-sm font-label-sm font-semibold uppercase tracking-wider bg-surface-container-high text-on-surface-variant ml-2 border border-outline-variant/30">
              Site Portal
            </span>
          </Link>

          <div className="flex items-center gap-4">
            <Link
              to="/portal/select-role"
              className="inline-flex items-center gap-1.5 font-body-sm text-body-sm font-medium text-on-surface-variant hover:text-primary transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">manage_accounts</span>
              <span className="hidden md:inline">Change Role</span>
            </Link>

            <Link
              to="/"
              className="inline-flex items-center gap-1.5 font-body-sm text-body-sm font-medium text-primary hover:text-primary-container transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">home</span>
              <span>Back to Website</span>
            </Link>
          </div>
        </div>
      </header>

      {/* Main Split Portal Area */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 lg:py-12 flex items-center justify-center">
        <div className="w-full grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch bg-surface-container-lowest border border-outline-variant/40 rounded-3xl shadow-xl overflow-hidden">
          {/* Left Branded Intelligence Panel */}
          <div className="lg:col-span-5 bg-on-background text-inverse-on-surface p-8 lg:p-12 relative flex flex-col justify-between overflow-hidden">
            {/* Background Grid & Ambient Glows */}
            <div className="absolute inset-0 bg-[radial-gradient(#2563eb25_1px,transparent_1px)] [background-size:20px_20px] pointer-events-none opacity-40"></div>
            <div className="absolute -top-24 -left-24 w-72 h-72 rounded-full bg-secondary-fixed/20 blur-[100px] pointer-events-none"></div>
            <div className="absolute -bottom-24 -right-24 w-72 h-72 rounded-full bg-primary-container/20 blur-[100px] pointer-events-none"></div>

            {/* Top Info */}
            <div className="relative z-10">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-secondary-fixed/15 text-secondary-fixed font-label-sm text-label-sm font-semibold tracking-wider uppercase mb-6 border border-secondary-fixed/20">
                <span className="w-2 h-2 rounded-full bg-secondary-fixed animate-ping"></span>
                {roleData ? roleData.tag : 'CIVIL INTELLIGENCE PLATFORM'}
              </div>

              <h1 className="font-headline-xl text-headline-xl lg:text-[34px] lg:leading-[42px] font-bold text-inverse-on-surface mb-4">
                {title || (roleData ? roleData.name : 'Autonomous Site Management')}
              </h1>

              <p className="font-body-md text-body-md text-outline-variant leading-relaxed">
                {subtitle ||
                  'Unified construction telemetry bridging physical jobsite reality with predictive algorithmic engineering.'}
              </p>

              {/* Role-specific Architecture Highlights */}
              <div className="mt-8 space-y-3">
                {(roleData?.highlights || [
                  'Automated Digital Twin & LiDAR Point Cloud Auditing',
                  'Predictive Risk & Monsoon Concrete Curing Forecast',
                  'Dispute-free Subcontractor Billing & Escrow Releases',
                ]).map((highlight, idx) => (
                  <div key={idx} className="flex items-start gap-3 text-body-sm text-inverse-on-surface/90">
                    <span className="material-symbols-outlined text-secondary-fixed text-[18px] shrink-0 mt-0.5">
                      check_circle
                    </span>
                    <span>{highlight}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Bottom Security / Architecture Badge */}
            <div className="relative z-10 mt-12 pt-6 border-t border-outline/20">
              <div className="flex items-center justify-between text-outline-variant font-label-sm text-[11px]">
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-secondary-fixed"></span>
                  <span>ISO 27001 • RERA CORE COMPLIANT</span>
                </span>
                <span className="font-mono text-secondary-fixed font-semibold">256-BIT ENCRYPTION</span>
              </div>
            </div>
          </div>

          {/* Right Focused Form Panel */}
          <div className="lg:col-span-7 p-6 sm:p-10 lg:p-12 flex flex-col justify-center bg-surface-container-lowest">
            {children}
          </div>
        </div>
      </main>

      {/* Subtle Footer */}
      <footer className="w-full py-6 text-center text-body-sm text-on-surface-variant/70 border-t border-outline-variant/20">
        <p>© 2025 NirmaanIQ Technologies India Pvt. Ltd. All rights reserved. • High-Security Construction Intelligence Mesh</p>
      </footer>
    </div>
  );
};
