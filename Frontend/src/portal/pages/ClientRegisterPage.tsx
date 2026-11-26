import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { PortalLayout } from '../components/PortalLayout';
import { RegistrationSuccess } from '../components/RegistrationSuccess';
import { authService } from '../services/authService';

export const ClientRegisterPage: React.FC = () => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
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
      errs.fullName = 'Full Name or Client Representative is required';
    } else if (fullName.trim().length < 3) {
      errs.fullName = 'Name must be at least 3 characters';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      errs.email = 'Email address is required';
    } else if (!emailRegex.test(email.trim())) {
      errs.email = 'Please provide a valid email address';
    }

    if (!address.trim()) {
      errs.address = 'Primary Address or Corporate Location is required';
    } else if (address.trim().length < 6) {
      errs.address = 'Please enter a complete address with city & state';
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
      errs.agreeToTerms = 'You must accept the Project Client Portal Agreement to proceed';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const res = authService.registerClient({
      fullName,
      email,
      address,
      password,
      agreeToTerms,
    });

    if (res.success && res.user) {
      setRegisteredEmail(res.user.email);
      setRegisteredName(res.user.name);
      setIsSubmitted(true);
    } else {
      setErrors({ form: res.error || 'Client registration failed. Please try again.' });
    }
  };

  if (isSubmitted) {
    return (
      <PortalLayout
        activeRole="client"
        title="Client Registration"
        subtitle="Infrastructure Stakeholder &amp; Homebuyer Portal Access"
      >
        <RegistrationSuccess
          role="client"
          email={registeredEmail}
          name={registeredName}
          isPendingApproval={false}
        />
      </PortalLayout>
    );
  }

  return (
    <PortalLayout
      activeRole="client"
      title="Client Registration"
      subtitle="Register as an infrastructure asset owner, property investor, or institutional client to commission builds and inspect live digital twin milestones."
    >
      <div className="space-y-6">
        <div>
          <div className="flex items-center justify-between">
            <span className="font-label-sm text-label-sm text-tertiary font-bold uppercase tracking-wider">
              Project Client // Asset Owner
            </span>
            <Link
              to="/portal/client/login"
              className="text-body-sm text-tertiary font-semibold hover:underline"
            >
              Already registered? Sign In
            </Link>
          </div>
          <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface mt-1">
            Client Registration
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant mt-1">
            Track verified 4D construction progress, review statutory RERA escrow milestones, and initiate new proposals.
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
              Full Name / Corporate Entity <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Vikramaditya Oberoi / Sterling Commercial Estates"
                className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.fullName
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-tertiary focus:ring-2 focus:ring-tertiary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                person
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
                placeholder="client@investment-infra.com"
                className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.email
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-tertiary focus:ring-2 focus:ring-tertiary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                mail
              </span>
            </div>
            {errors.email && <p className="text-body-sm text-error mt-1">{errors.email}</p>}
          </div>

          {/* Address */}
          <div>
            <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-1.5">
              Principal Address / Project Jurisdiction <span className="text-error">*</span>
            </label>
            <div className="relative">
              <input
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Plot 42, Bandra-Kurla Complex, Mumbai, Maharashtra"
                className={`w-full h-11 pl-10 pr-4 font-body-md text-body-md bg-surface-container-lowest border rounded-xl outline-none transition-all ${
                  errors.address
                    ? 'border-error focus:ring-2 focus:ring-error/20'
                    : 'border-outline-variant focus:border-tertiary focus:ring-2 focus:ring-tertiary/20'
                }`}
              />
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">
                location_on
              </span>
            </div>
            {errors.address && <p className="text-body-sm text-error mt-1">{errors.address}</p>}
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
                    : 'border-outline-variant focus:border-tertiary focus:ring-2 focus:ring-tertiary/20'
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
                    : 'border-outline-variant focus:border-tertiary focus:ring-2 focus:ring-tertiary/20'
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
                className="mt-1 w-4 h-4 rounded border-outline-variant text-tertiary focus:ring-tertiary/20 cursor-pointer accent-tertiary"
              />
              <span className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                I agree to the <span className="text-tertiary font-semibold hover:underline">Client Portal Terms</span>,{' '}
                <span className="text-tertiary font-semibold hover:underline">RERA Verification Access Guidelines</span>, and consent to electronic milestone notices.
              </span>
            </label>
            {errors.agreeToTerms && (
              <p className="text-body-sm text-error mt-1">{errors.agreeToTerms}</p>
            )}
          </div>

          {/* Register Button */}
          <button
            type="submit"
            className="w-full h-12 mt-4 inline-flex items-center justify-center gap-2 rounded-xl bg-tertiary text-on-tertiary font-headline-sm text-headline-sm font-semibold hover:bg-tertiary-container hover:text-on-tertiary-container transition-all shadow-md cursor-pointer"
          >
            <span>Register Client Account</span>
            <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
          </button>
        </form>

        <div className="pt-4 border-t border-outline-variant/30 text-center font-body-sm text-body-sm text-on-surface-variant">
          Already registered?{' '}
          <Link to="/portal/client/login" className="text-tertiary font-semibold hover:underline">
            Sign In with OTP
          </Link>
        </div>
      </div>
    </PortalLayout>
  );
};
