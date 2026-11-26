import React, { useState, useEffect } from 'react';

interface OtpVerificationProps {
  email: string;
  role: string;
  onVerify: (otp: string) => void;
  onResend: () => void;
  onBack: () => void;
  isLoading?: boolean;
  errorMessage?: string;
  demoOtp?: string;
}

export const OtpVerification: React.FC<OtpVerificationProps> = ({
  email,
  role,
  onVerify,
  onResend,
  onBack,
  isLoading = false,
  errorMessage,
  demoOtp = '123456',
}) => {
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(30);
  const [canResend, setCanResend] = useState(false);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else {
      setCanResend(true);
    }
  }, [countdown]);

  const handleResendClick = () => {
    if (!canResend) return;
    setCountdown(30);
    setCanResend(false);
    setOtp('');
    onResend();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.trim().length === 6) {
      onVerify(otp.trim());
    }
  };

  const handleAutoFill = () => {
    setOtp(demoOtp);
  };

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-1.5 text-body-sm text-on-surface-variant hover:text-primary transition-colors mb-4"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          <span>Change Email Address</span>
        </button>

        <h2 className="font-headline-lg text-headline-lg font-bold text-on-surface">
          Enter Verification Code
        </h2>
        <p className="font-body-md text-body-md text-on-surface-variant mt-1.5">
          We simulated an OTP dispatched for <span className="font-semibold text-on-surface">{email}</span> ({role}).
        </p>
      </div>

      {/* Clearly Labelled Demo Environment Banner */}
      <div className="bg-secondary-container/20 border border-secondary/30 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-start gap-3">
          <span className="material-symbols-outlined text-secondary text-[22px] mt-0.5">
            developer_mode
          </span>
          <div>
            <div className="font-label-sm text-label-sm font-bold text-secondary uppercase tracking-wider">
              Demo Authentication Active
            </div>
            <p className="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
              Simulated verification code: <span className="font-mono font-bold text-on-surface text-[15px]">{demoOtp}</span>
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleAutoFill}
          className="px-3 py-1.5 bg-secondary text-on-secondary rounded-lg font-label-sm text-label-sm font-semibold hover:bg-secondary-container hover:text-on-secondary-container transition-all shadow-sm shrink-0"
        >
          Auto-fill Demo OTP
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block font-body-sm text-body-sm font-medium text-on-surface mb-2">
            6-Digit One-Time Password (OTP)
          </label>
          <input
            type="text"
            maxLength={6}
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
            placeholder="• • • • • •"
            className="w-full h-14 text-center font-mono text-[26px] tracking-[0.5em] bg-surface-container-lowest border border-outline-variant focus:border-primary focus:ring-2 focus:ring-primary/20 rounded-xl outline-none transition-all"
            autoFocus
          />

          {errorMessage && (
            <p className="text-body-sm text-error flex items-center gap-1.5 mt-2">
              <span className="material-symbols-outlined text-[16px]">error</span>
              <span>{errorMessage}</span>
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={otp.length !== 6 || isLoading}
          className={`w-full h-12 inline-flex items-center justify-center gap-2 rounded-xl font-headline-sm text-headline-sm font-semibold text-on-primary transition-all shadow-md ${
            otp.length === 6 && !isLoading
              ? 'bg-primary hover:bg-primary-container hover:text-on-primary-container cursor-pointer'
              : 'bg-outline-variant/60 cursor-not-allowed opacity-70'
          }`}
        >
          {isLoading ? (
            <>
              <span className="w-5 h-5 border-2 border-on-primary border-t-transparent rounded-full animate-spin"></span>
              <span>Verifying Telemetry Access...</span>
            </>
          ) : (
            <>
              <span>Verify OTP &amp; Access Portal</span>
              <span className="material-symbols-outlined text-[18px]">verified_user</span>
            </>
          )}
        </button>

        {/* Resend Countdown */}
        <div className="flex items-center justify-between pt-2 border-t border-outline-variant/30 text-body-sm">
          <span className="text-on-surface-variant">Didn't receive the code?</span>
          {canResend ? (
            <button
              type="button"
              onClick={handleResendClick}
              className="text-primary font-semibold hover:underline flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-[16px]">restart_alt</span>
              <span>Resend OTP</span>
            </button>
          ) : (
            <span className="font-mono text-outline font-medium">
              Resend in {countdown < 10 ? `0${countdown}` : countdown}s
            </span>
          )}
        </div>
      </form>
    </div>
  );
};
