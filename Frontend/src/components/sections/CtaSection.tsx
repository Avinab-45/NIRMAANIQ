import React from 'react';
import { Link } from 'react-router-dom';
import { WaveDivider } from '../ui/WaveDivider';

export const CtaSection: React.FC = () => {
  return (
    <section className="w-full bg-[#030712] text-inverse-on-surface py-20 lg:py-28 relative overflow-hidden">
      {/* Layered wave separator top */}
      <WaveDivider variant="cta-top" />

      {/* Background Technical Accents */}
      <div className="absolute inset-0 pointer-events-none opacity-20">
        <div className="absolute bottom-0 right-10 w-[36rem] h-[36rem] rounded-full bg-primary blur-[140px]"></div>
        <div className="absolute top-0 left-10 w-96 h-96 rounded-full bg-secondary-fixed blur-[130px]"></div>
      </div>

      <div className="relative w-full max-w-5xl mx-auto px-6 lg:px-12 text-center flex flex-col items-center">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-highest/20 font-label-sm text-label-sm text-secondary-fixed uppercase tracking-wider mb-6">
          <span className="material-symbols-outlined text-[16px]">verified</span> Enterprise Tier Readiness
        </div>

        <h2 className="font-headline-xl lg:text-[48px] lg:leading-[56px] font-bold text-inverse-on-surface mb-6 max-w-3xl">
          Ready to Transform Site Operations Across Your Portfolio?
        </h2>

        <p className="font-body-lg text-body-lg text-outline-variant max-w-2xl mb-10 leading-relaxed">
          Join leading builders orchestrating over 180+ active sites. Schedule a custom 1-on-1 architecture demonstration with our civil intelligence specialists.
        </p>

        {/* Multi-Language Capability Preview Strip */}
        <div className="bg-inverse-surface/80 backdrop-blur-md px-6 py-3 rounded-2xl flex flex-wrap items-center justify-center gap-6 mb-10 shadow-lg">
          <span className="font-label-sm text-label-sm text-outline-variant uppercase">
            Native Worksite Dialects:
          </span>
          <span className="font-label-md text-label-md text-inverse-on-surface font-medium hover:text-secondary-fixed cursor-pointer transition-colors">
            English <span className="text-secondary-fixed">●</span>
          </span>
          <span className="font-label-md text-label-md text-inverse-on-surface font-medium hover:text-secondary-fixed cursor-pointer transition-colors">
            हिन्दी (Hindi Voice DPR) <span className="text-secondary-fixed">●</span>
          </span>
          <span className="font-label-md text-label-md text-inverse-on-surface font-medium hover:text-secondary-fixed cursor-pointer transition-colors">
            ଓଡ଼ିଆ (Odia Field Telemetry)
          </span>
        </div>

        {/* Action Cluster */}
        <div className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto">
          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-8 py-4 rounded-xl bg-primary-container text-on-primary font-headline-sm text-headline-sm font-semibold shadow-xl hover:bg-primary transition-all"
            data-path="book-demo"
            href="#book-demo"
          >
            <span>Book Enterprise Consultation</span>
            <span className="material-symbols-outlined text-[18px]">calendar_month</span>
          </a>
          <Link
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-8 py-4 rounded-xl bg-surface-container-highest/20 hover:bg-surface-container-highest/30 text-inverse-on-surface font-headline-sm text-headline-sm font-medium transition-all"
            data-path="sign-in"
            to="/portal/select-role"
          >
            <span>Sign In to Site Portal</span>
            <span className="material-symbols-outlined text-[18px]">login</span>
          </Link>
        </div>

        <div className="mt-8 flex items-center gap-6 font-label-sm text-label-sm text-outline-variant">
          <span>No Credit Card Required</span>
          <span>•</span>
          <span>Enterprise SLA Available</span>
          <span>•</span>
          <span>14-Day Pilot Program</span>
        </div>
      </div>
    </section>
  );
};
