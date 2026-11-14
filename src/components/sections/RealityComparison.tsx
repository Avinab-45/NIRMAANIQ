import React from 'react';
import { legacyPoints, platformPoints } from '../../data/mockData';

export const RealityComparison: React.FC = () => {
  return (
    <section id="solutions" className="w-full bg-surface py-20 lg:py-28 relative">
      <div className="w-full max-w-7xl mx-auto px-6 lg:px-12">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <span className="font-label-md text-label-md text-primary font-semibold uppercase tracking-widest bg-primary-fixed/40 px-3 py-1 rounded-full">
            Systemic Transformation
          </span>
          <h2 className="font-headline-xl text-headline-xl font-bold text-on-surface mt-4 mb-4">
            Fragmented Site Chaos vs. Autonomous Unified Intelligence
          </h2>
          <p className="font-body-lg text-body-lg text-on-surface-variant">
            Indian construction megaprojects lose up to 22% of margin to uncoordinated paperwork, unverified manual logs, and delayed fault discovery.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-stretch">
          {/* Legacy Methods Card */}
          <div className="bg-error-container/20 rounded-2xl p-8 flex flex-col justify-between shadow-sm">
            <div>
              <div className="flex items-center gap-3 mb-6">
                <span className="w-10 h-10 rounded-xl bg-error-container flex items-center justify-center text-error">
                  <span className="material-symbols-outlined text-[22px]">warning</span>
                </span>
                <div>
                  <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                    The Fragmented Legacy Method
                  </h3>
                  <p className="font-label-sm text-label-sm text-error font-medium uppercase">
                    High Risk • Information Blindspots
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                {legacyPoints.map((item, idx) => (
                  <div key={idx} className="bg-surface-container-lowest p-4 rounded-xl flex items-start gap-3.5">
                    <span className="material-symbols-outlined text-error text-[20px] shrink-0 mt-0.5">
                      cancel
                    </span>
                    <div>
                      <h4 className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                        {item.title}
                      </h4>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">
                        {item.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mt-8 pt-4 bg-surface-container-lowest/60 rounded-xl p-4 flex items-center justify-between font-label-md text-label-md text-error font-semibold">
              <span>Average Margin Loss</span>
              <span>-18.4% to -24.0%</span>
            </div>
          </div>

          {/* NirmaanIQ AI Platform Card */}
          <div className="bg-surface-container-low rounded-2xl p-8 flex flex-col justify-between shadow-md">
            <div>
              <div className="flex items-center gap-3 mb-6">
                <span className="w-10 h-10 rounded-xl bg-secondary-container flex items-center justify-center text-on-secondary-container">
                  <span className="material-symbols-outlined text-[22px]">smart_toy</span>
                </span>
                <div>
                  <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                    The NirmaanIQ Platform
                  </h3>
                  <p className="font-label-sm text-label-sm text-secondary font-medium uppercase">
                    Continuous Telemetry • Predictive Assurance
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                {platformPoints.map((item, idx) => (
                  <div key={idx} className="bg-surface-container-lowest p-4 rounded-xl flex items-start gap-3.5 shadow-sm">
                    <span className="material-symbols-outlined text-secondary text-[20px] shrink-0 mt-0.5">
                      check_circle
                    </span>
                    <div>
                      <h4 className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                        {item.title}
                      </h4>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">
                        {item.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mt-8 pt-4 bg-secondary-container/30 rounded-xl p-4 flex items-center justify-between font-label-md text-label-md text-on-secondary-container font-semibold">
              <span>Verified Efficiency Gain</span>
              <span>+32.6% Accelerated Milestone Delivery</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
