import React from 'react';
import { enterpriseClients, testimonialsData } from '../../data/mockData';

export const Testimonials: React.FC = () => {
  return (
    <section id="testimonials" className="w-full bg-surface-container-low py-20 lg:py-28 relative">
      <div className="w-full max-w-7xl mx-auto px-6 lg:px-12">
        {/* Enterprise Logos Bar */}
        <div className="text-center mb-16">
          <span className="font-label-md text-label-md text-on-surface-variant font-semibold uppercase tracking-widest">
            Trusted by India's Top Developers &amp; EPC Conglomerates
          </span>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-8 lg:gap-14 opacity-75">
            {enterpriseClients.map((client, idx) => (
              <span key={idx} className="font-headline-lg text-headline-lg font-bold text-on-surface tracking-tight">
                {client}
              </span>
            ))}
          </div>
        </div>

        {/* Testimonial Quotes Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {testimonialsData.map((item, idx) => (
            <div key={idx} className="bg-surface-container-lowest p-8 rounded-2xl shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-1 text-tertiary mb-4">
                  {[...Array(5)].map((_, sIdx) => (
                    <span key={sIdx} className="material-symbols-outlined text-[18px]">
                      star
                    </span>
                  ))}
                </div>
                <p className="font-body-md text-body-md text-on-surface leading-relaxed mb-6 italic">
                  {item.quote}
                </p>
              </div>

              <div className="flex items-center gap-3 pt-4 border-0">
                <div className={`w-10 h-10 rounded-full ${item.initialsBgClass} flex items-center justify-center ${item.initialsTextClass} font-bold`}>
                  {item.authorInitials}
                </div>
                <div>
                  <div className="font-headline-sm text-headline-sm text-on-surface font-semibold">
                    {item.authorName}
                  </div>
                  <div className="font-body-sm text-body-sm text-on-surface-variant">
                    {item.authorRole}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
