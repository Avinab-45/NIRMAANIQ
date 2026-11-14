import React from 'react';
import { useNavigate } from 'react-router-dom';
import { PortalUser } from '../../types/auth';
import { useAuth } from '../context/AuthContext';

interface PendingApprovalProps {
  user: PortalUser;
}

export const PendingApproval: React.FC<PendingApprovalProps> = ({ user }) => {
  const { toggleApproval, logout } = useAuth();
  const navigate = useNavigate();

  const handleSimulateApproval = () => {
    toggleApproval(user.id);
    // Navigate straight to dashboard once simulated approval is triggered!
    navigate(`/portal/${user.role}/dashboard`);
  };

  const handleLogout = () => {
    logout();
    navigate('/portal/select-role');
  };

  return (
    <div className="min-h-screen bg-surface flex flex-col justify-between p-6">
      <div className="max-w-2xl mx-auto my-auto w-full bg-surface-container-lowest border border-outline-variant/40 rounded-3xl p-8 lg:p-12 shadow-xl">
        {/* Header Badge */}
        <div className="flex items-center justify-between mb-8 pb-6 border-b border-outline-variant/30">
          <div className="flex items-center gap-3">
            <span className="w-12 h-12 rounded-xl bg-error-container/40 text-error flex items-center justify-center">
              <span className="material-symbols-outlined text-[28px]">pending_actions</span>
            </span>
            <div>
              <span className="font-label-sm text-label-sm text-error font-semibold uppercase tracking-wider">
                Status: Pending Approval
              </span>
              <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface">
                Account Verification in Progress
              </h2>
            </div>
          </div>

          <span className="px-3 py-1 bg-surface-container-high rounded-full font-label-sm text-label-sm text-on-surface-variant uppercase font-medium">
            {user.role}
          </span>
        </div>

        {/* Content Box */}
        <div className="space-y-6">
          <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
            Hello <span className="font-semibold text-on-surface">{user.name}</span>, your registration under{' '}
            <span className="font-mono text-primary font-medium">{user.email}</span> is currently under administrative review.
          </p>

          <div className="bg-surface-container-low rounded-2xl p-5 border border-outline-variant/30 space-y-3">
            <h3 className="font-headline-sm text-headline-sm font-semibold text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-tertiary text-[20px]">shield</span>
              <span>Statutory Infrastructure Vetting</span>
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
              To safeguard site telemetry, biometric muster rolls, and contractor accounts, our engineering compliance team audits:
            </p>
            <ul className="space-y-2 font-body-sm text-body-sm text-on-surface-variant pl-2">
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                <span>Corporate Registration &amp; GSTIN Verification</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                <span>RERA License Alignment &amp; Civil Contractor Credentials</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                <span>Site Telemetry Authorization &amp; Escrow Security Protocol</span>
              </li>
            </ul>
          </div>

          {/* Interactive Demo Control Bar */}
          <div className="bg-secondary-container/20 border border-secondary/40 rounded-2xl p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 font-label-sm text-label-sm font-bold text-secondary uppercase tracking-wider">
                <span className="material-symbols-outlined text-[18px]">build</span>
                <span>Developer / Reviewer Demo Tool</span>
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                You are currently previewing the <span className="font-semibold text-error">Pending Approval</span> state. Click below to simulate instant approval.
              </p>
            </div>

            <button
              type="button"
              onClick={handleSimulateApproval}
              className="px-4 py-2.5 bg-secondary text-on-secondary rounded-xl font-headline-sm text-headline-sm font-semibold hover:bg-secondary-container hover:text-on-secondary-container transition-all shadow-md shrink-0 flex items-center gap-2 cursor-pointer"
            >
              <span>Approve Account (Demo)</span>
              <span className="material-symbols-outlined text-[18px]">verified</span>
            </button>
          </div>

          {/* Action Row */}
          <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-outline-variant/30">
            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-2 font-body-md text-on-surface-variant hover:text-error transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">logout</span>
              <span>Sign Out</span>
            </button>

            <button
              type="button"
              onClick={() => navigate('/portal/select-role')}
              className="inline-flex items-center gap-2 font-body-md text-primary hover:text-primary-container transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">manage_accounts</span>
              <span>Select Another Role</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
