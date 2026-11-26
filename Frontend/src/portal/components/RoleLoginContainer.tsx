import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserRole } from '../../types/auth';
import { PortalLayout } from './PortalLayout';
import { CaptchaVerification } from './CaptchaVerification';
import { OtpVerification } from './OtpVerification';
import { authService } from '../services/authService';
import { useAuth } from '../context/AuthContext';

interface RoleLoginContainerProps {
  role: UserRole;
  roleDisplayName: string;
  roleTag: string;
  defaultDemoEmail: string;
  pendingDemoEmail?: string;
  accentColorClass: string;
}

export const RoleLoginContainer: React.FC<RoleLoginContainerProps> = ({
  role,
  roleDisplayName,
  roleTag,
  defaultDemoEmail,
  pendingDemoEmail,
  accentColorClass,
}) => {
  const [email, setEmail] = useState('');
  const [isCaptchaValid, setIsCaptchaValid] = useState(false);
  const [captchaError, setCaptchaError] = useState('');
  const [emailError, setEmailError] = useState('');
  const [generalError, setGeneralError] = useState('');

  const [step, setStep] = useState<'input' | 'otp'>('input');
  const [activeDemoOtp, setActiveDemoOtp] = useState('123456');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { setCurrentUser } = useAuth();
  const navigate = useNavigate();

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setEmailError('');
    setCaptchaError('');
    setGeneralError('');

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      setEmailError('Registered email address is required');
      return;
    }
    if (!emailRegex.test(email.trim())) {
      setEmailError('Please enter a valid email format');
      return;
    }

    if (!isCaptchaValid) {
      setCaptchaError('Please enter the security verification code correctly');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      const res = authService.requestOtp(email, role);
      setIsSubmitting(false);

      if (res.success) {
        setActiveDemoOtp(res.otp);
        setStep('otp');
      } else {
        setGeneralError(res.error || `No registered ${role} found with this email. Please register or use a demo account.`);
      }
    }, 400);
  };

  const handleVerifyOtp = (enteredOtp: string) => {
    setIsSubmitting(true);
    setGeneralError('');

    setTimeout(() => {
      const res = authService.verifyOtp(email, role, enteredOtp);
      setIsSubmitting(false);

      if (res.success && res.user) {
        setCurrentUser(res.user);
        // Navigate to appropriate dashboard route
        navigate(`/portal/${role}/dashboard`);
      } else {
        setGeneralError(res.error || 'Invalid OTP code. Please enter 123456 for demo.');
      }
    }, 400);
  };

  const handleResendOtp = () => {
    const res = authService.requestOtp(email, role);
    if (res.success) {
      setActiveDemoOtp(res.otp);
    }
  };

  return (
    <PortalLayout
      activeRole={role}
      title={`${roleDisplayName} Site Portal`}
      subtitle={`Secure authentication for ${roleTag.toLowerCase()} with 2-factor CAPTCHA & OTP telemetry validation.`}
    >
      {step === 'otp' ? (
        <OtpVerification
          email={email}
          role={roleDisplayName}
          onVerify={handleVerifyOtp}
          onResend={handleResendOtp}
          onBack={() => setStep('input')}
          isLoading={isSubmitting}
          errorMessage={generalError}
          demoOtp={activeDemoOtp}
        />
      ) : (
        <div className="space-y-6">
          <div>
            <div className="flex items-center justify-between">
              <span className={`font-label-sm text-label-sm ${accentColorClass} font-bold uppercase tracking-wider`}>
                Dual-Factor Telemetry Authentication
              </span>
              <Link
                to={`/portal/${role}/register`}
                className={`text-body-sm ${accentColorClass} font-semibold hover:underline`}
              >
                Need an account? Register
              </Link>
            </div>
            <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface mt-1">
              Sign In as {roleDisplayName}
            </h2>
            <p className="font-body-md text-body-md text-on-surface-variant mt-1">
              Enter your registered corporate email to receive a secure single-use verification code.
            </p>
          </div>

          {/* Demo Pre-filled Helper Pills */}
          <div className="bg-surface-container-low border border-outline-variant/40 rounded-2xl p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-label-sm text-[11px] text-outline uppercase font-semibold flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[15px]">tips_and_updates</span>
                <span>Quick Test Accounts (Click to Fill):</span>
              </span>
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setEmail(defaultDemoEmail)}
                className="px-2.5 py-1 rounded-lg bg-surface-container-highest/80 hover:bg-surface-container-highest text-on-surface font-mono text-[12px] transition-colors border border-outline-variant/30 text-left"
              >
                <span className="font-sans text-[11px] text-secondary font-bold mr-1">
                  {role === 'contractor' ? '[Pending Demo]' : '[Approved Demo]'}:
                </span>
                {defaultDemoEmail}
              </button>

              {pendingDemoEmail && (
                <button
                  type="button"
                  onClick={() => setEmail(pendingDemoEmail)}
                  className="px-2.5 py-1 rounded-lg bg-surface-container-highest/80 hover:bg-surface-container-highest text-on-surface font-mono text-[12px] transition-colors border border-outline-variant/30 text-left"
                >
                  <span className="font-sans text-[11px] text-error font-bold mr-1">
                    {role === 'contractor' ? '[Approved Demo]' : '[Pending Demo]'}:
                  </span>
                  {pendingDemoEmail}
                </button>
              )}
            </div>
          </div>

          {generalError && (
            <div className="p-3.5 bg-error-container/40 border border-error/30 text-error rounded-xl font-body-sm flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">error</span>
              <span>{generalError}</span>
            </div>
          )}

          <form onSubmit={handleSendOtp} className="space-y-4">
            {/* Email Address */}
            <div>
              <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
                Registered Email Address <span className="text-error">*</span>
              </label>
              <div className="relative">
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={`e.g. ${defaultDemoEmail}`}
                  className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                    emailError
                      ? 'border-error focus:ring-2 focus:ring-error/20'
                      : 'border-outline-variant focus:border-primary focus:ring-2 focus:ring-primary/20'
                  }`}
                />
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                  mail
                </span>
              </div>
              {emailError && <p className="text-body-sm text-error mt-1">{emailError}</p>}
            </div>

            {/* CAPTCHA Verification */}
            <CaptchaVerification
              onValidate={(valid) => {
                setIsCaptchaValid(valid);
                if (valid) setCaptchaError('');
              }}
              error={captchaError}
            />

            {/* Send OTP Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full h-12 mt-4 inline-flex items-center justify-center gap-2 rounded-xl bg-primary text-on-primary font-headline-sm text-headline-sm font-semibold hover:bg-primary-container hover:text-on-primary-container transition-all shadow-md cursor-pointer disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <span className="w-5 h-5 border-2 border-on-primary border-t-transparent rounded-full animate-spin"></span>
                  <span>Generating Single-Use Code...</span>
                </>
              ) : (
                <>
                  <span>Send Verification Code</span>
                  <span className="material-symbols-outlined text-[18px]">send</span>
                </>
              )}
            </button>
          </form>

          {/* Footer Navigation */}
          <div className="pt-4 border-t border-outline-variant/30 flex items-center justify-between text-body-sm">
            <Link
              to="/portal/select-role"
              className="text-on-surface-variant hover:text-primary transition-colors flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-[16px]">arrow_back</span>
              <span>Back to Role Selection</span>
            </Link>

            <Link
              to={`/portal/${role}/register`}
              className={`${accentColorClass} font-semibold hover:underline`}
            >
              Register New {roleDisplayName}
            </Link>
          </div>
        </div>
      )}
    </PortalLayout>
  );
};
