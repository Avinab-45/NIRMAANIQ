import React, { useState, useEffect } from 'react';

interface CaptchaVerificationProps {
  onValidate: (isValid: boolean) => void;
  error?: string;
}

export const CaptchaVerification: React.FC<CaptchaVerificationProps> = ({ onValidate, error }) => {
  const [captchaCode, setCaptchaCode] = useState('');
  const [userInput, setUserInput] = useState('');
  const [isVerified, setIsVerified] = useState(false);

  // Generate random 5-character alphanumeric captcha
  const generateCaptcha = () => {
    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'; // Avoid confusing chars like 0, O, 1, I
    let result = '';
    for (let i = 0; i < 5; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setCaptchaCode(result);
    setUserInput('');
    setIsVerified(false);
    onValidate(false);
  };

  useEffect(() => {
    generateCaptcha();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.toUpperCase();
    setUserInput(val);
    const valid = val === captchaCode;
    setIsVerified(valid);
    onValidate(valid);
  };

  const handleQuickFill = () => {
    setUserInput(captchaCode);
    setIsVerified(true);
    onValidate(true);
  };

  return (
    <div className="space-y-2">
      <label className="block font-body-sm text-body-sm font-medium text-on-surface">
        Security Verification <span className="text-error">*</span>
      </label>

      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        {/* Captcha Display Box */}
        <div className="relative flex items-center justify-between px-4 py-2.5 bg-on-background text-inverse-on-surface rounded-lg select-none border border-outline/30 overflow-hidden min-w-[160px]">
          {/* Subtle noise grid lines */}
          <div className="absolute inset-0 bg-[radial-gradient(#89f5e715_1px,transparent_1px)] [background-size:8px_8px] pointer-events-none"></div>
          <div className="absolute inset-0 bg-[linear-gradient(45deg,transparent_45%,rgba(137,245,231,0.15)_50%,transparent_55%)] pointer-events-none"></div>

          <span className="font-mono text-[20px] font-bold tracking-widest text-secondary-fixed select-none letter-spacing-3 line-through decoration-primary-container/40">
            {captchaCode}
          </span>

          <button
            type="button"
            onClick={generateCaptcha}
            title="Refresh CAPTCHA"
            className="p-1 rounded text-outline-variant hover:text-secondary-fixed transition-colors focus:outline-none"
            aria-label="Generate new security code"
          >
            <span className="material-symbols-outlined text-[18px]">refresh</span>
          </button>
        </div>

        {/* Captcha Input */}
        <div className="relative flex-1">
          <input
            type="text"
            maxLength={5}
            value={userInput}
            onChange={handleChange}
            placeholder="Enter security code"
            className={`w-full h-11 px-3.5 font-mono text-[14px] uppercase bg-surface-container-lowest border rounded-lg outline-none transition-all ${
              isVerified
                ? 'border-secondary focus:ring-2 focus:ring-secondary/20'
                : error
                ? 'border-error focus:ring-2 focus:ring-error/20'
                : 'border-outline-variant focus:border-primary focus:ring-2 focus:ring-primary/20'
            }`}
          />
          {isVerified && (
            <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-secondary text-[20px]">
              check_circle
            </span>
          )}
        </div>

        {/* Demo Fast-Fill Button */}
        <button
          type="button"
          onClick={handleQuickFill}
          className="px-2.5 py-2 text-label-sm font-label-sm font-medium text-primary hover:text-primary-container bg-primary-fixed/30 hover:bg-primary-fixed/60 rounded-lg transition-colors whitespace-nowrap"
          title="Demo shortcut: Auto-enter CAPTCHA code"
        >
          Auto-fill
        </button>
      </div>

      {error && !isVerified && (
        <p className="text-body-sm text-error flex items-center gap-1.5 mt-1">
          <span className="material-symbols-outlined text-[16px]">error</span>
          <span>{error}</span>
        </p>
      )}
    </div>
  );
};
