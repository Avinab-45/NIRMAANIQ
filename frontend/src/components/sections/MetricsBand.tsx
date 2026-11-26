import React from 'react';
import { metricsData } from '../../data/mockData';

export const MetricsBand: React.FC = () => {
  return (
    <section className="w-full bg-surface-container-lowest py-16 shadow-sm relative z-30">
      <div className="w-full max-w-7xl mx-auto px-6 lg:px-12">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          {metricsData.map((metric) => (
            <div
              key={metric.id}
              className="flex flex-col bg-surface-container-low p-6 rounded-2xl transition-all hover:shadow-md hover:bg-surface-container"
            >
              <div className="flex items-center justify-between mb-3">
                <span className={`font-label-sm text-label-sm ${metric.iconColor} font-semibold uppercase tracking-wider`}>
                  {metric.category}
                </span>
                <span className={`material-symbols-outlined ${metric.iconColor} text-[20px]`}>
                  {metric.icon}
                </span>
              </div>
              <div className={`font-data-metric text-data-metric ${metric.valueColor} font-bold tracking-tight mb-2`}>
                {metric.value}
              </div>
              <p className="font-headline-sm text-headline-sm text-on-surface font-semibold mb-1">
                {metric.title}
              </p>
              <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                {metric.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
