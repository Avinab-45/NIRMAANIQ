import React from 'react';

interface WaveDividerProps {
  variant: 'hero-bottom' | 'section4-top' | 'section4-bottom' | 'cta-top';
  className?: string;
}

export const WaveDivider: React.FC<WaveDividerProps> = ({ variant, className = '' }) => {
  if (variant === 'hero-bottom') {
    return (
      <div className={`w-full overflow-hidden leading-none -mt-4 relative z-20 pointer-events-none ${className}`}>
        <svg
          className="relative block w-full h-16 lg:h-24 text-surface-container-lowest"
          preserveAspectRatio="none"
          viewBox="0 0 1440 120"
        >
          <path
            d="M0,32L48,42.7C96,53,192,75,288,80C384,85,480,75,576,58.7C672,43,768,21,864,21.3C960,21,1056,43,1152,53.3C1248,64,1344,64,1392,64L1440,64L1440,120L1392,120C1344,120,1248,120,1152,120C1056,120,960,120,864,120C768,120,672,120,576,120C480,120,384,120,288,120C192,120,96,120,48,120L0,120Z"
            fill="currentColor"
          />
        </svg>
      </div>
    );
  }

  if (variant === 'section4-top') {
    return (
      <div className={`absolute top-0 left-0 w-full overflow-hidden leading-none z-10 pointer-events-none -translate-y-[98%] ${className}`}>
        <svg
          className="relative block w-full h-12 lg:h-16 text-on-background"
          preserveAspectRatio="none"
          viewBox="0 0 1440 80"
        >
          <path
            d="M0,32L60,42.7C120,53,240,75,360,74.7C480,75,600,53,720,42.7C840,32,960,32,1080,42.7C1200,53,1320,75,1380,85.3L1440,96L1440,160L1380,160C1320,160,1200,160,1080,160C960,160,840,160,720,160C600,160,480,160,360,160C240,160,120,160,60,160L0,160Z"
            fill="currentColor"
          />
        </svg>
      </div>
    );
  }

  if (variant === 'section4-bottom') {
    return (
      <div className={`w-full overflow-hidden leading-none z-10 pointer-events-none mt-16 -mb-20 lg:-mb-28 ${className}`}>
        <svg
          className="relative block w-full h-14 lg:h-20 text-surface-container-low"
          preserveAspectRatio="none"
          viewBox="0 0 1440 100"
        >
          <path
            d="M0,64L80,69.3C160,75,320,85,480,80C640,75,800,53,960,42.7C1120,32,1280,32,1360,32L1440,32L1440,100L1360,100C1280,100,1120,100,960,100C800,100,640,100,480,100C320,100,160,100,80,100L0,100Z"
            fill="currentColor"
          />
        </svg>
      </div>
    );
  }

  if (variant === 'cta-top') {
    return (
      <div className={`absolute top-0 left-0 w-full overflow-hidden leading-none z-10 pointer-events-none -translate-y-[98%] ${className}`}>
        <svg
          className="relative block w-full h-12 lg:h-16 text-[#030712]"
          preserveAspectRatio="none"
          viewBox="0 0 1440 80"
        >
          <path
            d="M0,24L48,32C96,40,192,56,288,61.3C384,67,480,61,576,48C672,35,768,13,864,16C960,19,1056,45,1152,56C1248,67,1344,61,1392,58.7L1440,56L1440,100L1392,100C1344,100,1248,100,1152,100C1056,100,960,100,864,100C768,100,672,100,576,100C480,100,384,100,288,100C192,100,96,100,48,100L0,100Z"
            fill="currentColor"
          />
        </svg>
      </div>
    );
  }

  return null;
};
