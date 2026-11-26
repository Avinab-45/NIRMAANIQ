import React, { useState } from 'react';
import { Link } from 'react-router-dom';

export const Navbar: React.FC = () => {
  const [lang, setLang] = useState('en');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 w-full z-50 bg-surface/90 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.04)]">
      <div className="h-16 w-full max-w-7xl mx-auto px-6 lg:px-12 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link className="flex items-center gap-3" data-path="landing-page" to="/">
            <img
              alt="NirmaanIQ Logo"
              className="h-8 w-auto object-contain"
              src="https://lh3.googleusercontent.com/aida/ALJJyrBymwrci1_JVMf7mCezzMOqmZtpZ9eGkBO_6QuHL5Ry-7bY0c2ccSROahcSDaEkfpoNZrV2ul5r2eJ7CO-LkANbILtIqT-YgVa8vUzJeo3kfm4IulxlZjsxuWde_9f_mDO2NGBVp0T6TCCgGnYBfTVEsdVpotnNxaLgd75yhJWw4mi3h3kUN0urpFX0c5AU8UOJf-DzIYX-a_N1OkYH6PBwY-3MBqXiW0J6eklJN09b"
            />
            <span className="font-headline-md text-headline-md text-on-surface tracking-tight font-bold">
              Nirmaan<span className="text-primary">IQ</span>
            </span>
          </Link>

          <nav className="hidden lg:flex items-center gap-6" data-active-classes="text-primary font-semibold">
            <a
              className="font-body-md text-body-md text-on-surface-variant hover:text-on-surface transition-colors"
              data-path="solutions"
              href="#solutions"
            >
              Solutions
            </a>
            <a
              className="font-body-md text-body-md text-on-surface-variant hover:text-on-surface transition-colors"
              data-path="platform-capabilities"
              href="#pillars"
            >
              Platform Capabilities
            </a>
            <a
              className="font-body-md text-body-md text-on-surface-variant hover:text-on-surface transition-colors"
              data-path="ai-intelligence"
              href="#interactive-preview"
            >
              AI Intelligence
            </a>
            <a
              className="font-body-md text-body-md text-on-surface-variant hover:text-on-surface transition-colors"
              data-path="enterprise"
              href="#evolution-showcase-section"
            >
              Enterprise
            </a>
            <a
              className="font-body-md text-body-md text-on-surface-variant hover:text-on-surface transition-colors"
              data-path="customers"
              href="#testimonials"
            >
              Customers
            </a>
          </nav>
        </div>

        <div className="flex items-center gap-4">
          <div className="relative inline-block text-left">
            <select
              aria-label="Select Language"
              className="bg-surface-container-low text-on-surface font-label-sm text-label-sm px-2.5 py-1.5 rounded-lg appearance-none cursor-pointer pr-7 outline-none hover:bg-surface-container"
              value={lang}
              onChange={(e) => setLang(e.target.value)}
            >
              <option value="en">English</option>
              <option value="hi">हिन्दी</option>
              <option value="od">ଓଡ଼ିଆ</option>
            </select>
            <span className="material-symbols-outlined absolute right-1.5 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none text-[16px]">
              expand_more
            </span>
          </div>

          <Link
            className="font-body-md text-body-md font-medium text-on-surface hover:text-primary px-3 py-1.5 transition-colors hidden sm:inline-block"
            data-path="sign-in"
            to="/portal/select-role"
          >
            Sign In
          </Link>

          <a
            className="inline-flex items-center justify-center font-body-md text-body-md font-medium bg-primary text-on-primary px-4 py-2 rounded-lg hover:bg-primary-container hover:text-on-primary-container shadow-sm transition-all"
            data-path="book-demo"
            href="#book-demo"
          >
            Book Demo
          </a>

          <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
            <span className="material-symbols-outlined text-on-primary text-[18px]">person</span>
          </div>

          <button
            className="lg:hidden p-1 text-on-surface hover:text-primary focus:outline-none"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle navigation menu"
          >
            <span className="material-symbols-outlined text-[24px]">
              {mobileMenuOpen ? 'close' : 'menu'}
            </span>
          </button>
        </div>
      </div>

      {/* Mobile navigation drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden bg-surface border-b border-outline-variant px-6 py-4 flex flex-col gap-3">
          <a
            className="font-body-md text-on-surface-variant hover:text-primary py-1"
            href="#solutions"
            onClick={() => setMobileMenuOpen(false)}
          >
            Solutions
          </a>
          <a
            className="font-body-md text-on-surface-variant hover:text-primary py-1"
            href="#pillars"
            onClick={() => setMobileMenuOpen(false)}
          >
            Platform Capabilities
          </a>
          <a
            className="font-body-md text-on-surface-variant hover:text-primary py-1"
            href="#interactive-preview"
            onClick={() => setMobileMenuOpen(false)}
          >
            AI Intelligence
          </a>
          <a
            className="font-body-md text-on-surface-variant hover:text-primary py-1"
            href="#evolution-showcase-section"
            onClick={() => setMobileMenuOpen(false)}
          >
            Enterprise
          </a>
          <a
            className="font-body-md text-on-surface-variant hover:text-primary py-1"
            href="#testimonials"
            onClick={() => setMobileMenuOpen(false)}
          >
            Customers
          </a>
          <Link
            className="font-body-md text-primary font-medium py-1"
            to="/portal/select-role"
            onClick={() => setMobileMenuOpen(false)}
          >
            Sign In
          </Link>
        </div>
      )}
    </header>
  );
};
