import React from 'react';
import { Navbar } from '../components/layout/Navbar';
import { HeroSection } from '../components/sections/HeroSection';
import { MetricsBand } from '../components/sections/MetricsBand';
import { RealityComparison } from '../components/sections/RealityComparison';
import { ThreePillars } from '../components/sections/ThreePillars';
import { BuildingEvolutionShowcase } from '../components/sections/BuildingEvolutionShowcase';
import { Testimonials } from '../components/sections/Testimonials';
import { CtaSection } from '../components/sections/CtaSection';
import { Footer } from '../components/layout/Footer';

export const LandingPage: React.FC = () => {
  return (
    <div className="bg-surface font-body-md text-body-md text-on-surface antialiased min-h-screen flex flex-col justify-between">
      <Navbar />
      <main className="w-full pt-16 bg-surface flex-1">
        <div className="flex flex-col w-full overflow-hidden">
          <HeroSection />
          <MetricsBand />
          <RealityComparison />
          <ThreePillars />
          <BuildingEvolutionShowcase />
          <Testimonials />
          <CtaSection />
        </div>
      </main>
      <Footer />
    </div>
  );
};
