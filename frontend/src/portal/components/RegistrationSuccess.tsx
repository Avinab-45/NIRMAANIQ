import React from 'react';
import { Link } from 'react-router-dom';
import { UserRole } from '../../types/auth';

interface RegistrationSuccessProps {
  role: UserRole;
  email: string;
  name: string;
  isPendingApproval: boolean;
}

export const RegistrationSuccess: React.FC<RegistrationSuccessProps> = ({
  role,
  email,
  name,
  isPendingApproval,
}) => {
  const loginPath = `/portal/${role}/login`;

  return (
    <div className="space-y-6 text-center max-w-lg mx-auto py-4">
      {/* Icon Badge */}
      <div className="w-16 h-16 rounded-2xl bg-secondary-container/40 text-on-secondary-container flex items-center justify-center mx-auto shadow-sm">
        <span className="material-symbols-outlined text-[36px]">
          {isPendingApproval ? 'hourglass_top' : 'verified'}
        </span>
      </div>

      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-high text-on-surface-variant font-label-sm text-label-sm font-semibold uppercase tracking-wider mb-3">
          {role.toUpperCase()} REGISTRATION
        </div>
        <h2 className="font-headline-xl text-headline-xl font-bold text-on-surface">
          {isPendingApproval ? 'Registration Submitted' : 'Account Created Successfully'}
        </h2>
        <p className="font-body-md text-body-md text-on-surface-variant mt-2 leading-relaxed">
          Welcome, <span className="font-semibold text-on-surface">{name}</span>. Your account is registered under{' '}
          <span className="font-mono text-primary font-medium">{email}</span>.
        </p>
      </div>

      {isPendingApproval ? (
        <div className="bg-surface-container-low border border-outline-variant/50 rounded-2xl p-5 text-left space-y-3">
          <div className="flex items-center gap-2 font-headline-sm text-headline-sm font-semibold text-tertiary">
            <span className="material-symbols-outlined text-[20px]">policy</span>
            <span>Administrative Verification in Progress</span>
          </div>
          <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
            As an enterprise {role}, your registration has been submitted with <span className="font-semibold text-error">Pending Approval</span> status. Under statutory Indian building compliance (RERA Section 4 &amp; National Building Code), corporate credentials and worksite GSTINs undergo review prior to granting telemetry dispatch access.
          </p>
          <div className="pt-2 border-t border-outline-variant/30 flex items-center justify-between font-label-sm text-label-sm text-outline-variant">
            <span>Estimated Verification:</span>
            <span className="font-mono text-secondary font-bold">24-48 Business Hours</span>
          </div>
        </div>
      ) : (
        <div className="bg-secondary-container/20 border border-secondary/30 rounded-2xl p-5 text-left space-y-2">
          <div className="flex items-center gap-2 font-headline-sm text-headline-sm font-semibold text-secondary">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span>Client Telemetry Access Granted</span>
          </div>
          <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
            Your client profile is active. You can immediately access your project dashboard, review initial milestones, and submit new construction requirements.
          </p>
        </div>
      )}

      {/* Action Buttons */}
      <div className="pt-4 flex flex-col sm:flex-row items-center gap-3 justify-center">
        <Link
          to={loginPath}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-primary text-on-primary font-headline-sm text-headline-sm font-semibold rounded-xl hover:bg-primary-container hover:text-on-primary-container transition-all shadow-md"
        >
          <span>Proceed to {role.charAt(0).toUpperCase() + role.slice(1)} Login</span>
          <span className="material-symbols-outlined text-[18px]">login</span>
        </Link>

        <Link
          to="/portal/select-role"
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-3.5 bg-surface-container-high text-on-surface font-headline-sm text-headline-sm font-medium rounded-xl hover:bg-surface-container-highest transition-colors"
        >
          <span>Role Selector</span>
        </Link>
      </div>
    </div>
  );
};
