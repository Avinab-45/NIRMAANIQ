import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserRole } from '../../types/auth';
import { useAuth } from '../context/AuthContext';
import { PendingApproval } from './PendingApproval';

interface DashboardPlaceholderProps {
  role: UserRole;
  roleTitle: string;
  roleIcon: string;
  badgeBg: string;
  badgeText: string;
  accentColor: string;
  description: string;
  upcomingFeatures: string[];
}

export const DashboardPlaceholder: React.FC<DashboardPlaceholderProps> = ({
  role,
  roleTitle,
  roleIcon,
  badgeBg,
  badgeText,
  accentColor,
  description,
  upcomingFeatures,
}) => {
  const { currentUser, logout, toggleApproval } = useAuth();
  const navigate = useNavigate();

  // If user is not logged in or role mismatch, fallback to mock user for preview or redirect
  const user = currentUser && currentUser.role === role ? currentUser : null;

  // Handle case where user is not logged in: prompt them to sign in
  if (!user) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-surface-container-lowest border border-outline-variant/40 rounded-3xl p-8 text-center shadow-xl space-y-4">
          <div className="w-14 h-14 rounded-2xl bg-surface-container-high flex items-center justify-center mx-auto text-on-surface-variant">
            <span className="material-symbols-outlined text-[30px]">lock</span>
          </div>
          <h2 className="font-headline-md text-headline-md font-bold text-on-surface">
            Authentication Required
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Please sign in with your registered email and OTP to access the {roleTitle} workspace.
          </p>
          <div className="pt-2">
            <Link
              to={`/portal/${role}/login`}
              className="inline-flex items-center justify-center w-full h-11 bg-primary text-on-primary rounded-xl font-headline-sm text-headline-sm font-semibold hover:bg-primary-container transition-all"
            >
              Go to {roleTitle} Login
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Check pending approval status for organisation and contractor
  if (user.approvalStatus === 'pending') {
    return <PendingApproval user={user} />;
  }

  const handleLogout = () => {
    logout();
    navigate('/portal/select-role');
  };

  return (
    <div className="min-h-screen bg-surface flex flex-col justify-between">
      {/* Top Application Header */}
      <header className="w-full bg-surface/90 backdrop-blur-md border-b border-outline-variant/30 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
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

            <span className={`px-2.5 py-1 rounded-full font-label-sm text-[11px] font-bold uppercase tracking-wider ${badgeBg}`}>
              {badgeText}
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex flex-col text-right">
              <span className="font-headline-sm text-[13px] font-semibold text-on-surface">
                {user.name}
              </span>
              <span className="font-label-sm text-[11px] text-on-surface-variant font-mono">
                {user.email}
              </span>
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-outline-variant/40 hover:bg-surface-container-high text-body-sm font-medium text-on-surface-variant hover:text-error transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">logout</span>
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Dashboard Placeholder Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-10 lg:py-12">
        {/* Welcome Card */}
        <div className="bg-surface-container-lowest border border-outline-variant/40 rounded-3xl p-8 lg:p-12 shadow-sm mb-8 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-80 h-80 bg-secondary-fixed/10 rounded-full blur-3xl pointer-events-none"></div>

          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
            <div className="flex items-start gap-5">
              <div className={`w-16 h-16 rounded-2xl ${accentColor} flex items-center justify-center shadow-sm shrink-0`}>
                <span className="material-symbols-outlined text-[36px]">{roleIcon}</span>
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="w-2.5 h-2.5 rounded-full bg-secondary"></span>
                  <span className="font-label-sm text-label-sm text-secondary font-bold uppercase tracking-wider">
                    Authenticated Session Active • Status: {user.approvalStatus.toUpperCase()}
                  </span>
                </div>
                <h1 className="font-headline-xl text-headline-xl lg:text-[36px] font-bold text-on-surface">
                  Welcome to {roleTitle} Workspace
                </h1>
                <p className="font-body-md text-body-md text-on-surface-variant mt-1.5 max-w-2xl">
                  {description}
                </p>
              </div>
            </div>

            {/* Status Pill */}
            <div className="flex flex-col gap-2 shrink-0">
              <div className="px-4 py-2.5 bg-surface-container-low rounded-xl border border-outline-variant/40 flex items-center gap-3">
                <span className="material-symbols-outlined text-secondary text-[22px]">verified_user</span>
                <div>
                  <div className="font-label-sm text-[11px] text-outline uppercase font-semibold">
                    Security Stamp
                  </div>
                  <div className="font-body-sm text-body-sm font-semibold text-on-surface">
                    Dual-Factor OTP Verified
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Modular Feature Readiness Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 bg-surface-container-low rounded-3xl p-8 border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between pb-4 border-b border-outline-variant/30">
              <h2 className="font-headline-md text-headline-md font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">construction</span>
                <span>Upcoming {roleTitle} Management Capabilities</span>
              </h2>
              <span className="px-3 py-1 bg-primary-fixed/30 text-primary font-label-sm text-label-sm font-semibold rounded-full uppercase">
                Phase 2 Modules
              </span>
            </div>

            <p className="font-body-sm text-body-sm text-on-surface-variant">
              This dashboard placeholder confirms successful authentication. The complete full-stack interface for these modules will be connected in subsequent phases:
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
              {upcomingFeatures.map((feat, idx) => (
                <div key={idx} className="bg-surface-container-lowest p-4 rounded-2xl flex items-start gap-3 border border-outline-variant/20 shadow-sm">
                  <span className="w-8 h-8 rounded-xl bg-secondary-fixed/30 text-secondary flex items-center justify-center shrink-0 mt-0.5">
                    <span className="material-symbols-outlined text-[18px]">check</span>
                  </span>
                  <div>
                    <h3 className="font-headline-sm text-[14px] font-semibold text-on-surface">{feat}</h3>
                    <p className="font-body-sm text-[12px] text-on-surface-variant mt-0.5">
                      Ready for backend database integration &amp; live telemetry sync.
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Actions & Demo Tools Side Panel */}
          <div className="bg-surface-container-lowest rounded-3xl p-8 border border-outline-variant/40 space-y-6 shadow-sm flex flex-col justify-between">
            <div className="space-y-4">
              <h3 className="font-headline-sm text-headline-sm font-bold text-on-surface">
                Session Controls
              </h3>

              <div className="space-y-2">
                <Link
                  to="/portal/select-role"
                  className="w-full h-11 inline-flex items-center justify-center gap-2 rounded-xl bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-body-md font-medium transition-colors"
                >
                  <span className="material-symbols-outlined text-[18px]">manage_accounts</span>
                  <span>Switch Workspace Role</span>
                </Link>

                <Link
                  to="/"
                  className="w-full h-11 inline-flex items-center justify-center gap-2 rounded-xl bg-surface-container-low hover:bg-surface-container-high text-on-surface font-body-md font-medium transition-colors"
                >
                  <span className="material-symbols-outlined text-[18px]">home</span>
                  <span>Return to Marketing Site</span>
                </Link>
              </div>

              {/* Demo Status Toggle Button */}
              {(role === 'organisation' || role === 'contractor') && (
                <div className="pt-4 border-t border-outline-variant/30">
                  <div className="font-label-sm text-[11px] text-outline uppercase font-semibold mb-2">
                    Reviewer Demo Tool
                  </div>
                  <button
                    type="button"
                    onClick={() => toggleApproval(user.id)}
                    className="w-full py-2.5 px-3 bg-error-container/30 hover:bg-error-container/50 text-error rounded-xl font-label-sm text-label-sm font-semibold transition-colors flex items-center justify-center gap-1.5"
                  >
                    <span className="material-symbols-outlined text-[16px]">swap_horiz</span>
                    <span>Test Toggle to 'Pending Approval'</span>
                  </button>
                </div>
              )}
            </div>

            <div className="pt-4 border-t border-outline-variant/30">
              <button
                type="button"
                onClick={handleLogout}
                className="w-full h-11 inline-flex items-center justify-center gap-2 rounded-xl bg-error/10 hover:bg-error/20 text-error font-headline-sm text-headline-sm font-semibold transition-colors"
              >
                <span className="material-symbols-outlined text-[18px]">logout</span>
                <span>Sign Out of Portal</span>
              </button>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full py-6 text-center text-body-sm text-on-surface-variant/70 border-t border-outline-variant/20">
        <p>© 2025 NirmaanIQ Technologies India Pvt. Ltd. • Industrial Intelligence Telemetry Portal</p>
      </footer>
    </div>
  );
};
