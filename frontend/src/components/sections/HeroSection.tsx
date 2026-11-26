import React from 'react';
import { HeroParticleWave } from './HeroParticleWave';

export const HeroSection: React.FC = () => {
  return (
    <section className="relative w-full bg-[#030712] text-inverse-on-surface overflow-hidden pt-12 pb-0 lg:pt-16">
      {/* Atmospheric Ambient Glows & Technical Grid Overlay */}
      <div className="absolute inset-0 pointer-events-none opacity-20 z-0">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-secondary-fixed blur-[120px]"></div>
        <div className="absolute top-1/2 -right-32 w-[32rem] h-[32rem] rounded-full bg-primary-container blur-[150px]"></div>
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff0a_1px,transparent_1px),linear-gradient(to_bottom,#ffffff0a_1px,transparent_1px)] bg-[size:4rem_4rem]"></div>
      </div>

      <div className="relative w-full max-w-7xl mx-auto px-6 lg:px-12 flex flex-col items-center z-10">
        {/* Announcement Pill Badge */}
        <div className="inline-flex items-center gap-2.5 px-4 py-1.5 rounded-full bg-surface-container-highest/20 backdrop-blur-md border-0 shadow-sm mb-8 transition-transform hover:scale-105 cursor-pointer">
          <span className="w-2 h-2 rounded-full bg-secondary-fixed animate-pulse"></span>
          <span className="font-label-sm text-label-sm text-secondary-fixed tracking-wide uppercase font-semibold">
            ✨ NirmaanIQ 3.0 Live — Autonomous Predictive Construction AI
          </span>
          <span className="material-symbols-outlined text-secondary-fixed text-[16px]">
            chevron_right
          </span>
        </div>

        {/* Headline & Subtitle */}
        <div className="text-center max-w-4xl mx-auto mb-10">
          <h1 className="font-headline-xl lg:text-[54px] lg:leading-[62px] font-bold text-inverse-on-surface tracking-tight mb-6">
            Build Smarter.{' '}
            <span className="bg-gradient-to-r from-secondary-fixed via-primary-fixed to-inverse-primary bg-clip-text text-transparent">
              Monitor Better.
            </span>{' '}
            Predict Earlier.
          </h1>
          <p className="font-body-lg text-body-lg text-outline-variant max-w-3xl mx-auto leading-relaxed">
            The AI-powered construction management &amp; intelligence ecosystem that combines physical jobsite execution, automated LiDAR progress tracking, and predictive risk foresight across India's premier infrastructure and commercial builds.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row items-center gap-4 mb-6 z-20">
          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-xl bg-primary-container text-on-primary font-headline-sm text-headline-sm font-semibold shadow-lg hover:bg-primary transition-all"
            data-path="book-demo"
            href="#book-demo"
          >
            <span>Get Started with Enterprise Demo</span>
            <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
          </a>
          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-xl bg-surface-container-highest/10 hover:bg-surface-container-highest/25 text-inverse-on-surface font-headline-sm text-headline-sm font-medium backdrop-blur-md transition-all"
            href="#interactive-preview"
          >
            <span className="material-symbols-outlined text-secondary-fixed text-[20px]">
              play_circle
            </span>
            <span>Explore Interactive Architecture</span>
          </a>
        </div>
      </div>

      {/* Construction-Themed Interactive Architectural Skyline Feature */}
      <HeroParticleWave />
    </section>
  );
};
