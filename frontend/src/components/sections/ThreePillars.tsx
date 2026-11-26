import React from 'react';
import { pillarsData } from '../../data/mockData';

export const ThreePillars: React.FC = () => {
  return (
    <section id="pillars" className="w-full bg-surface-container-lowest py-20 lg:py-28 relative">
      <div className="w-full max-w-7xl mx-auto px-6 lg:px-12">
        {/* Section Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-16 gap-6">
          <div className="max-w-2xl">
            <span className="font-label-md text-label-md text-secondary font-semibold uppercase tracking-widest bg-secondary-fixed/50 px-3 py-1 rounded-full">
              Holistic Infrastructure OS
            </span>
            <h2 className="font-headline-xl text-headline-xl font-bold text-on-surface mt-3">
              Three Pillars of Industrial Intelligence
            </h2>
            <p className="font-body-lg text-body-lg text-on-surface-variant mt-2">
              Engineered from ground-level Indian worksite nuances to executive boardroom precision.
            </p>
          </div>
          <div className="font-label-md text-label-md text-on-surface-variant">
            DEPLOYMENT READY ON PRIVATE CLOUD &amp; EDGE
          </div>
        </div>

        {/* 3 Pillars Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {pillarsData.map((pillar, idx) => (
            <div
              key={idx}
              className={`bg-surface-container-low rounded-2xl p-8 flex flex-col justify-between shadow-sm transition-all hover:shadow-lg hover:-translate-y-1 relative ${
                idx === 1 ? 'overflow-hidden' : ''
              }`}
            >
              {idx === 1 && (
                <div className="absolute top-0 right-0 w-32 h-32 bg-secondary-fixed/30 rounded-full blur-2xl pointer-events-none"></div>
              )}

              <div>
                <div className={`w-12 h-12 rounded-xl ${pillar.iconBgClass} flex items-center justify-center ${pillar.iconTextClass} mb-6`}>
                  <span className="material-symbols-outlined text-[26px]">{pillar.icon}</span>
                </div>
                <div className={`font-label-sm text-label-sm ${pillar.badgeColorClass} uppercase font-bold tracking-wider mb-2`}>
                  {pillar.number}
                </div>
                <h3 className="font-headline-lg text-headline-lg font-bold text-on-surface mb-3">
                  {pillar.title}
                </h3>
                <p className="font-body-md text-body-md text-on-surface-variant mb-6 leading-relaxed">
                  {pillar.description}
                </p>

                <div className="space-y-3 mb-8">
                  {pillar.features.map((feat, fIdx) => (
                    <div key={fIdx} className="flex items-center gap-2.5 font-body-sm text-body-sm text-on-surface font-medium">
                      <span className={`w-1.5 h-1.5 rounded-full ${
                        idx === 0 ? 'bg-primary' : idx === 1 ? 'bg-secondary' : 'bg-tertiary'
                      }`}></span>
                      <span>{feat}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Feature micro-screenshot visual */}
              <div className="bg-surface-container rounded-xl p-4">
                <div className="flex items-center justify-between text-on-surface-variant font-label-sm text-label-sm mb-2">
                  <span>{pillar.statLabel}</span>
                  <span className={`${pillar.statBadgeColorClass} font-bold`}>{pillar.statBadge}</span>
                </div>
                <div className={`font-data-metric text-[22px] ${pillar.statValueColorClass || 'text-on-surface'} font-semibold`}>
                  {pillar.statValue}
                </div>
                <div className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                  {pillar.statSubtext}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
