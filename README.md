# NirmaanIQ - Enterprise Construction Intelligence Platform

Frontend application built with React, Vite, TypeScript, Tailwind CSS, and Three.js, faithfully converted from the approved Google Stitch design export.

## Architecture & Directory Structure

```
nirmaaniq-app/
├── index.html                   # HTML entry point with Google Fonts & Material Symbols
├── package.json                 # Dependencies (React 18, Vite 5, Tailwind 3, Three.js)
├── postcss.config.js            # PostCSS configuration
├── tailwind.config.js           # Exact design tokens matching DESIGN.md
├── tsconfig.json                # TypeScript configuration
├── vite.config.ts               # Vite configuration
└── src/
    ├── main.tsx                 # React DOM root render
    ├── App.tsx                  # Core layout integrating all narrative sections
    ├── index.css                # Global base styles & scrollbar reset
    ├── types/
    │   └── index.ts             # TypeScript interfaces for stages, metrics & pillars
    ├── data/
    │   └── mockData.ts          # Centralized data for telemetry, stages & testimonials
    └── components/
        ├── layout/
        │   ├── Navbar.tsx       # Navigation header with language select & responsive drawer
        │   └── Footer.tsx       # Enterprise footer with multi-column links & ISO badges
        ├── ui/
        │   └── WaveDivider.tsx  # Exact mathematical SVG wave section transitions
        └── sections/
            ├── HeroSection.tsx              # Hero container with announcement & typography
            ├── HeroParticleWave.tsx         # 16,560-vertex Three.js KPIT particle wave
            ├── MetricsBand.tsx              # 4-card value metrics telemetry band
            ├── RealityComparison.tsx        # Legacy site chaos vs NirmaanIQ comparison
            ├── ThreePillars.tsx             # Jobsite Ops, Predictive Intel, Stakeholder Trust
            ├── BuildingEvolutionShowcase.tsx# 4-stage interactive SVG architectural blueprint
            ├── Testimonials.tsx             # Enterprise developer quotes & client logos
            └── CtaSection.tsx               # Enterprise consultation CTA & worksite dialects
```

## Getting Started

### Development Server
```bash
npm run dev
```

### Production Build
```bash
npm run build
```

### Preview Production Build
```bash
npm run preview
```
