import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { PortalLayout } from '../components/PortalLayout';
import { RegistrationSuccess } from '../components/RegistrationSuccess';
import { authService } from '../services/authService';

export const ContractorRegisterPage: React.FC = () => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [agreeToTerms, setAgreeToTerms] = useState(false);

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [registeredName, setRegisteredName] = useState('');

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!fullName.trim()) {
      errs.fullName = 'Full Name or Trade Contractor Lead is required';
    } else if (fullName.trim().length < 3) {
      errs.fullName = 'Name must be at least 3 characters';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      errs.email = 'Email address is required';
    } else if (!emailRegex.test(email.trim())) {
      errs.email = 'Please provide a valid email address';
    }

    const cleanMobile = mobile.replace(/[^0-9]/g, '');
    if (!mobile.trim()) {
      errs.mobile = 'Mobile Number is required for site biometric synchronization';
    } else if (cleanMobile.length < 10) {
      errs.mobile = 'Please enter a valid 10-digit Indian mobile number';
    }

    if (!password) {
      errs.password = 'Password is required';
    } else if (password.length < 8) {
      errs.password = 'Password must be at least 8 characters';
    } else if (!/[A-Z]/.test(password)) {
      errs.password = 'Password must include at least one uppercase letter';
    } else if (!/[0-9]/.test(password)) {
      errs.password = 'Password must include at least one number';
    }

    if (!confirmPassword) {
      errs.confirmPassword = 'Please confirm your password';
    } else if (password !== confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }

    if (!agreeToTerms) {
      errs.agreeToTerms = 'You must accept the Contractor Service Agreement to proceed';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const res = authService.registerContractor({
      fullName,
      email,
      mobile,
      password,
      agreeToTerms,
    });

    if (res.success && res.user) {
      setRegisteredEmail(res.user.email);
      setRegisteredName(res.user.name);
      setIsSubmitted(true);
    } else {
      setErrors({ form: res.error || 'Contractor registration failed. Please try again.' });
    }
  };

  if (isSubmitted) {
    return (
      <PortalLayout
        activeRole="contractor"
        title="Contractor Registration"
        subtitle="Worksite Execution &amp; Labor Telemetry Onboarding"
      >
        <RegistrationSuccess
          role="contractor"
          email={registeredEmail}
          name={registeredName}
          isPendingApproval={true}
        />
      </PortalLayout>
    );
  }

  return (
    <PortalLayout
      activeRole="contractor"
      title="Contractor Registration"
      subtitle="Join verified trade contractor networks with automated daily progress reporting, biometric muster sync and dispute-free invoicing."
    >
      <div className="space-y-6">
        <div>
          <div className="flex items-center justify-between">
            <span className="font-label-sm text-label-sm text-secondary font-bold uppercase tracking-wider">
              Worksite Contractor // Trade Account
            </span>
            <Link
              to="/portal/contractor/login"
              className="text-body-sm text-secondary font-semibold hover:underline"
            >
              Already registered? Sign In
            </Link>
          </div>
          <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface mt-1">
            Contractor Registration
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant mt-1">
            Enroll your civil, electrical, or structural contracting crew for site task dispatch.
          </p>
        </div>

        {errors.form && (
          <div className="p-3.5 bg-error-container/40 border border-error/30 text-error rounded-xl font-body-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px]">error</span>
            <span>{errors.form}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Full Name */}
          <div>
            <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
              Full Name / Contractor In-Charge <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Ramesh Chandra (Apex Structural Co.)"
                className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.fullName
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-secondary focus:ring-2 focus:ring-secondary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                badge
              </span>
            </div>
            {errors.fullName && <p className="text-body-sm text-error mt-1">{errors.fullName}</p>}
          </div>

          {/* Email Address */}
          <div>
            <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
              Email Address <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="contractor@buildops.in"
                className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.email
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-secondary focus:ring-2 focus:ring-secondary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                mail
              </span>
            </div>
            {errors.email && <p className="text-body-sm text-error mt-1">{errors.email}</p>}
          </div>

          {/* Mobile Number */}
          <div>
            <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
              Site Mobile Number <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type="tel"
                value={mobile}
                onChange={(e) => setMobile(e.target.value)}
                placeholder="+91 98765 43210 (SMS & WhatsApp DPR)"
                className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.mobile
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-secondary focus:ring-2 focus:ring-secondary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                phone_android
              </span>
            </div>
            {errors.mobile && <p className="text-body-sm text-error mt-1">{errors.mobile}</p>}
          </div>

          {/* Password */}
          <div>
            <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
              Password <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Minimum 8 chars, 1 uppercase, 1 number"
                className={`w-full h-11 pl-10 pr-10 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.password
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-secondary focus:ring-2 focus:ring-secondary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                lock
              </span>
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-on-surface transition-colors"
                aria-label="Toggle password visibility"
              >
                <span className="material-symbols-outlined text-[18px]">
                  {showPassword ? 'visibility_off' : 'visibility'}
                </span>
              </button>
            </div>
            {errors.password && <p className="text-body-sm text-error mt-1">{errors.password}</p>}
          </div>

          {/* Confirm Password */}
          <div>
            <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
              Confirm Password <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter your password"
                className={`w-full h-11 pl-10 pr-10 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.confirmPassword
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-secondary focus:ring-2 focus:ring-secondary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                lock_reset
              </span>
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-on-surface transition-colors"
                aria-label="Toggle confirm password visibility"
              >
                <span className="material-symbols-outlined text-[18px]">
                  {showConfirmPassword ? 'visibility_off' : 'visibility'}
                </span>
              </button>
            </div>
            {errors.confirmPassword && (
              <p className="text-body-sm text-error mt-1">{errors.confirmPassword}</p>
            )}
          </div>

          {/* Terms & Conditions Acceptance */}
          <div className="pt-2">
            <label className="flex items-start gap-3 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={agreeToTerms}
                onChange={(e) => setAgreeToTerms(e.target.checked)}
                className="mt-1 w-4 h-4 rounded border-outline-variant text-secondary focus:ring-secondary/20 cursor-pointer accent-secondary"
              />
              <span className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                I accept the <span className="text-secondary font-semibold hover:underline">Contractor Worksite Protocol</span>,{' '}
                <span className="text-secondary font-semibold hover:underline">Labor Biometric Safety Agreement</span>, and acknowledge verification requirements prior to site muster access.
              </span>
            </label>
            {errors.agreeToTerms && (
              <p className="text-body-sm text-error mt-1">{errors.agreeToTerms}</p>
            )}
          </div>

          {/* Register Button */}
          <button
            type="submit"
            className="w-full h-12 mt-4 inline-flex items-center justify-center gap-2 rounded-xl bg-secondary text-on-secondary font-headline-sm text-headline-sm font-semibold hover:bg-secondary-container hover:text-on-secondary-container transition-all shadow-md cursor-pointer"
          >
            <span>Register Contractor Account</span>
            <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
          </button>
        </form>

        <div className="pt-4 border-t border-outline-variant/30 text-center font-body-sm text-body-sm text-on-surface-variant">
          Already approved?{' '}
          <Link to="/portal/contractor/login" className="text-secondary font-semibold hover:underline">
            Sign In with OTP
          </Link>
        </div>
      </div>
    </PortalLayout>
  );
};
