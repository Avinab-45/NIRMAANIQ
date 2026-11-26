import React, { useState, useEffect, useRef } from 'react';
import { constructionStages, inspectionPhases } from '../../data/mockData';
import { WaveDivider } from '../ui/WaveDivider';

export const BuildingEvolutionShowcase: React.FC = () => {
  const [activeStage, setActiveStage] = useState<number>(2);
  const sectionRef = useRef<HTMLElement>(null);

  const stage = constructionStages[activeStage] || constructionStages[2];

  // Scroll scrubbing integration: smoothly track scroll through the section
  useEffect(() => {
    const handleScroll = () => {
      const section = sectionRef.current;
      if (!section) return;
      const rect = section.getBoundingClientRect();
      const windowHeight = window.innerHeight;

      // When section is in visible viewport
      if (rect.top <= windowHeight * 0.4 && rect.bottom >= windowHeight * 0.4) {
        const totalDist = rect.height - windowHeight * 0.5;
        const progress = Math.min(Math.max((windowHeight * 0.4 - rect.top) / totalDist, 0), 1);
        const nextStage = Math.min(Math.floor(progress * 4) + 1, 4);
        if (nextStage >= 1 && nextStage <= 4) {
          setActiveStage(nextStage);
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <section
      ref={sectionRef}
      id="evolution-showcase-section"
      className="w-full bg-on-background text-inverse-on-surface py-20 lg:py-28 relative"
    >
      {/* Layered Fluid Wave Header Separator */}
      <WaveDivider variant="section4-top" />

      {/* Subtle perimeter ambient particle current */}
      <div className="absolute inset-0 pointer-events-none opacity-25">
        <div className="absolute top-1/4 -left-20 w-80 h-80 rounded-full bg-secondary-fixed/40 blur-[130px]"></div>
        <div className="absolute bottom-1/3 -right-20 w-96 h-96 rounded-full bg-primary-container/40 blur-[150px]"></div>
      </div>

      <div className="w-full max-w-7xl mx-auto px-6 lg:px-12 relative z-10">
        <div className="max-w-3xl mb-16">
          <span className="font-label-md text-label-md text-secondary-fixed uppercase tracking-widest bg-secondary-fixed/20 px-3 py-1 rounded-full">
            Closed-Loop Precision
          </span>
          <h2 className="font-headline-xl text-headline-xl font-bold text-inverse-on-surface mt-4 mb-4">
            The 4-Step Autonomous Inspection Loop
          </h2>
          <p className="font-body-lg text-body-lg text-outline-variant">
            From drone takeoff to contractor reconciliation: how NirmaanIQ transforms raw physics into verifiable project velocity.
          </p>
        </div>

        {/* 4-Step Process Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-16">
          {inspectionPhases.map((phaseItem, idx) => (
            <div key={idx} className="bg-inverse-surface p-6 rounded-2xl flex flex-col justify-between">
              <div>
                <div className="font-label-md text-label-md text-secondary-fixed font-bold mb-3">
                  {phaseItem.phase}
                </div>
                <h3 className="font-headline-md text-headline-md text-inverse-on-surface font-semibold mb-2">
                  {phaseItem.title}
                </h3>
                <p className="font-body-sm text-body-sm text-outline-variant leading-relaxed">
                  {phaseItem.description}
                </p>
              </div>
              <div className="mt-6 flex items-center gap-2 text-secondary-fixed font-label-sm text-label-sm">
                <span className="material-symbols-outlined text-[18px]">{phaseItem.icon}</span>
                <span>{phaseItem.tag}</span>
              </div>
            </div>
          ))}
        </div>

        {/* HIGH-TECH 4-STAGE SCROLL-DRIVEN CONSTRUCTION EVOLUTION SHOWCASE */}
        <div className="w-full bg-inverse-surface/80 border border-outline/30 rounded-3xl p-6 lg:p-10 relative overflow-hidden backdrop-blur-xl shadow-2xl">
          {/* Top Control Bar: Stage Navigator + Live Scrubbing Indicator */}
          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6 pb-8 border-b border-outline/25">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-secondary-fixed/15 text-secondary-fixed font-label-sm text-label-sm font-semibold tracking-wider uppercase mb-2">
                <span className="w-2 h-2 rounded-full bg-secondary-fixed animate-ping"></span>
                4D Kinetic Blueprint • Continuous Structural Evolution
              </div>
              <h3 className="font-headline-lg text-headline-lg font-bold text-inverse-on-surface">
                Tower Horizon Alpha: 48-Floor Commercial Core Progression
              </h3>
              <p className="font-body-sm text-body-sm text-outline-variant mt-1">
                Seamless architectural landmark evolution across all structural milestones. Scrub or select stages to inspect live telemetry.
              </p>
            </div>

            {/* Stage Switcher Tabs */}
            <div className="flex flex-wrap items-center gap-2 bg-on-background/70 p-1.5 rounded-2xl border border-outline/20">
              {[
                { num: 1, label: '01 Foundation' },
                { num: 2, label: '02 Framework' },
                { num: 3, label: '03 Active Envelope & Cranes' },
                { num: 4, label: '04 Autonomous Tower' },
              ].map((btn) => (
                <button
                  key={btn.num}
                  id={`stage-btn-${btn.num}`}
                  onClick={() => setActiveStage(btn.num)}
                  className={`tower-nav-btn px-3.5 py-2 rounded-xl text-left font-label-sm text-label-sm font-semibold transition-all flex items-center gap-2 ${
                    activeStage === btn.num
                      ? 'bg-primary-container text-on-primary shadow-sm'
                      : 'text-outline-variant hover:text-inverse-on-surface hover:bg-surface-container-highest/10'
                  }`}
                >
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${
                      activeStage === btn.num
                        ? 'bg-secondary-fixed'
                        : 'bg-transparent border border-outline-variant'
                    }`}
                  ></span>
                  {btn.label}
                </button>
              ))}
            </div>
          </div>

          {/* Interactive Timeline Slider Scrub Bar */}
          <div className="mt-6 mb-8 bg-on-background/40 p-4 rounded-2xl border border-outline/20 flex flex-col md:flex-row items-center gap-4">
            <div className="flex items-center gap-2.5 shrink-0 text-secondary-fixed font-label-sm text-label-sm font-semibold uppercase">
              <span className="material-symbols-outlined text-[18px]">timeline</span>
              <span>Scrub Evolution:</span>
            </div>
            <div className="relative w-full flex-1 flex items-center">
              <input
                id="tower-scrubber"
                type="range"
                min="1"
                max="4"
                step="1"
                value={activeStage}
                onChange={(e) => setActiveStage(parseInt(e.target.value, 10))}
                className="w-full h-2 bg-inverse-surface rounded-lg appearance-none cursor-pointer accent-secondary-fixed"
                aria-label="Tower evolution stage scrubber"
              />
            </div>
            <div className="flex items-center justify-between md:justify-end gap-4 w-full md:w-auto font-label-sm text-label-sm">
              <span
                id="slider-percentage"
                className="text-secondary-fixed font-mono font-bold bg-secondary-fixed/10 px-2.5 py-1 rounded-md border border-secondary-fixed/20"
              >
                {stage.percentage}
              </span>
              <span id="slider-milestone" className="text-outline-variant">
                {stage.milestone}
              </span>
            </div>
          </div>

          {/* Main Showcase Workspace: Unified Architectural Tower + Live AI Telemetry Overlay Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            {/* Central Tower Visual Canvas & Architectural Blueprint Render (7 cols) */}
            <div className="lg:col-span-7 relative bg-on-background rounded-2xl border border-outline/20 p-6 flex flex-col items-center justify-center min-h-[580px] overflow-hidden">
              {/* Blueprint Technical Background Grids */}
              <div className="absolute inset-0 bg-[radial-gradient(#2563eb18_1px,transparent_1px)] [background-size:16px_16px] pointer-events-none"></div>
              <div className="absolute inset-0 bg-[linear-gradient(to_bottom,transparent_0%,rgba(19,27,46,0.85)_100%)] pointer-events-none"></div>

              {/* Live Drone Coordinate HUD Overlay */}
              <div className="absolute top-4 left-4 z-20 flex items-center gap-2 bg-inverse-surface/80 border border-outline/30 px-3 py-1.5 rounded-lg text-secondary-fixed font-label-sm text-[11px] backdrop-blur-md">
                <span className="w-2 h-2 rounded-full bg-secondary-fixed animate-pulse"></span>
                <span>UAV-SURVEY // SECTOR 4B — 19.0760° N, 72.8777° E</span>
              </div>
              <div className="absolute top-4 right-4 z-20 flex items-center gap-2 bg-inverse-surface/80 border border-outline/30 px-3 py-1.5 rounded-lg text-inverse-on-surface font-label-sm text-[11px] backdrop-blur-md">
                <span className="material-symbols-outlined text-secondary-fixed text-[14px]">grid_4x4</span>
                <span id="canvas-elevation-tag">{stage.elevation}</span>
              </div>

              {/* Dynamic High-Fidelity SVG Architectural Evolution Tower */}
              <div className="relative w-full max-w-[380px] h-[480px] flex items-end justify-center z-10">
                <svg
                  id="tower-evolution-svg"
                  className="w-full h-full overflow-visible transition-all duration-700 ease-out"
                  viewBox="0 0 380 480"
                >
                  <defs>
                    <linearGradient id="glazeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#89f5e7" stopOpacity="0.85" />
                      <stop offset="50%" stopColor="#2563eb" stopOpacity="0.75" />
                      <stop offset="100%" stopColor="#00174b" stopOpacity="0.95" />
                    </linearGradient>
                    <linearGradient id="concreteShear" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" stopColor="#3b4252" />
                      <stop offset="100%" stopColor="#1e2433" />
                    </linearGradient>
                    <linearGradient id="laserBeam" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" stopColor="#89f5e7" stopOpacity="1" />
                      <stop offset="100%" stopColor="#89f5e7" stopOpacity="0" />
                    </linearGradient>
                    <pattern id="rebarMesh" width="10" height="10" patternUnits="userSpaceOnUse">
                      <path
                        d="M 0 5 L 10 5 M 5 0 L 5 10"
                        stroke="#89f5e7"
                        strokeOpacity="0.5"
                        strokeWidth="0.75"
                      />
                    </pattern>
                  </defs>

                  {/* BASE GROUND & SUB-GRADE BEDROCK (Persistent across all stages) */}
                  <g id="bedrock-stratum">
                    <line
                      x1="20"
                      y1="440"
                      x2="360"
                      y2="440"
                      stroke="#737686"
                      strokeWidth="2"
                      strokeDasharray="4 4"
                    />
                    <rect
                      x="50"
                      y="440"
                      width="280"
                      height="35"
                      fill="#131b2e"
                      stroke="#3b4252"
                      strokeWidth="1.5"
                    />
                    <text
                      x="190"
                      y="462"
                      fill="#737686"
                      fontFamily="JetBrains Mono"
                      fontSize="9"
                      letterSpacing="1.5"
                      textAnchor="middle"
                    >
                      SOLID BASALT BEDROCK FOUNDATION STRATUM
                    </text>
                    {/* Subsurface Bored Piles */}
                    <rect x="80" y="440" width="22" height="30" fill="url(#rebarMesh)" stroke="#89f5e7" strokeWidth="1" />
                    <rect x="130" y="440" width="22" height="30" fill="url(#rebarMesh)" stroke="#89f5e7" strokeWidth="1" />
                    <rect x="228" y="440" width="22" height="30" fill="url(#rebarMesh)" stroke="#89f5e7" strokeWidth="1" />
                    <rect x="278" y="440" width="22" height="30" fill="url(#rebarMesh)" stroke="#89f5e7" strokeWidth="1" />
                  </g>

                  {/* STAGE 1 ELEMENTS: FOUNDATION & SUBSTRUCTURE */}
                  <g
                    id="layer-stage-1"
                    className="transition-opacity duration-500"
                    style={{ opacity: activeStage >= 1 ? 1 : 0 }}
                  >
                    {/* Reinforced Mat Foundation Slab */}
                    <rect x="70" y="410" width="240" height="30" fill="url(#concreteShear)" stroke="#89f5e7" strokeWidth="2" />
                    {/* Rebar Cage overlay */}
                    <rect x="75" y="415" width="230" height="20" fill="url(#rebarMesh)" />
                    {/* Sensor Telemetry Nodes in Foundation */}
                    <circle cx="90" cy="425" r="4" fill="#89f5e7" className="animate-ping" />
                    <circle cx="90" cy="425" r="3" fill="#89f5e7" />
                    <circle cx="190" cy="425" r="3" fill="#89f5e7" />
                    <circle cx="290" cy="425" r="3" fill="#89f5e7" />
                    {/* Retaining Soil Anchors */}
                    <line x1="70" y1="410" x2="35" y2="435" stroke="#89f5e7" strokeWidth="1.5" strokeDasharray="3 3" />
                    <line x1="310" y1="410" x2="345" y2="435" stroke="#89f5e7" strokeWidth="1.5" strokeDasharray="3 3" />
                    {/* Laser Level scanning line */}
                    <line x1="50" y1="405" x2="330" y2="405" stroke="#89f5e7" strokeWidth="1.5" strokeOpacity="0.8" />
                    <text x="190" y="401" fill="#89f5e7" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle">
                      RAF FOUNDATION CAST • 4,500 m³ C40/50 POUR ACTIVE
                    </text>
                  </g>

                  {/* STAGE 2 ELEMENTS: SUPERSTRUCTURE & STRUCTURAL FRAMEWORK (Floors 1 - 18) */}
                  <g
                    id="layer-stage-2"
                    className="transition-opacity duration-500"
                    style={{ opacity: activeStage >= 2 ? 1 : 0 }}
                  >
                    {/* Concrete Shear Core Rising (Center) */}
                    <rect x="150" y="270" width="80" height="140" fill="url(#concreteShear)" stroke="#2563eb" strokeWidth="1.5" />
                    {/* Vertical Steel Columns */}
                    <line x1="85" y1="410" x2="85" y2="280" stroke="#89f5e7" strokeWidth="3" />
                    <line x1="125" y1="410" x2="125" y2="280" stroke="#89f5e7" strokeWidth="2.5" />
                    <line x1="255" y1="410" x2="255" y2="280" stroke="#89f5e7" strokeWidth="2.5" />
                    <line x1="295" y1="410" x2="295" y2="280" stroke="#89f5e7" strokeWidth="3" />
                    {/* Slab Floors (1 to 14) */}
                    <line x1="80" y1="385" x2="300" y2="385" stroke="#434655" strokeWidth="3" />
                    <line x1="80" y1="360" x2="300" y2="360" stroke="#434655" strokeWidth="3" />
                    <line x1="80" y1="335" x2="300" y2="335" stroke="#434655" strokeWidth="3" />
                    <line x1="80" y1="310" x2="300" y2="310" stroke="#434655" strokeWidth="3" />
                    <line x1="80" y1="285" x2="300" y2="285" stroke="#2563eb" strokeWidth="3.5" />
                    {/* Shuttering formwork at Floor 14 */}
                    <rect x="80" y="275" width="220" height="10" fill="#2563eb" fillOpacity="0.3" stroke="#89f5e7" strokeWidth="1" strokeDasharray="2 2" />
                    {/* Laser Plumb Alignment Vertical Guides */}
                    <line x1="190" y1="410" x2="190" y2="260" stroke="#89f5e7" strokeWidth="1" strokeDasharray="3 3" />
                    <text x="190" y="255" fill="#89f5e7" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle">
                      LASER PLUMB 0.00° DEVIATION (L14)
                    </text>
                  </g>

                  {/* STAGE 3 ELEMENTS: ACTIVE ENVELOPE, CRANES, MID-RISE GLAZING (Floors 1 - 38) */}
                  <g
                    id="layer-stage-3"
                    className="transition-opacity duration-500"
                    style={{ opacity: activeStage >= 3 ? 1 : 0 }}
                  >
                    {/* Full Height Framework to Floor 38 */}
                    <rect x="80" y="140" width="220" height="270" fill="none" stroke="#2563eb" strokeWidth="1.5" />
                    {/* Partial Curtain Glass Installation (Lower Floors 1-24 clad in reflective glass) */}
                    <rect x="82" y="240" width="216" height="170" fill="url(#glazeGradient)" fillOpacity="0.65" stroke="#89f5e7" strokeWidth="1" />
                    {/* Safety Catch Netting on upper raw floors (25-38) */}
                    <rect x="76" y="140" width="228" height="100" fill="none" stroke="#89f5e7" strokeWidth="1.5" strokeDasharray="3 3" />
                    {/* Upper Core & Columns */}
                    <line x1="150" y1="270" x2="150" y2="130" stroke="#737686" strokeWidth="2" />
                    <line x1="230" y1="270" x2="230" y2="130" stroke="#737686" strokeWidth="2" />
                    {/* Floor slabs grid */}
                    <line x1="80" y1="210" x2="300" y2="210" stroke="#89f5e7" strokeWidth="2" />
                    <line x1="80" y1="175" x2="300" y2="175" stroke="#89f5e7" strokeWidth="2" />
                    <line x1="80" y1="140" x2="300" y2="140" stroke="#2563eb" strokeWidth="3" />
                    {/* Twin Luffing Jib Construction Cranes on Top */}
                    <g id="climbing-crane-left">
                      <rect x="120" y="80" width="10" height="60" fill="#2563eb" stroke="#89f5e7" strokeWidth="1" />
                      <line x1="70" y1="80" x2="150" y2="80" stroke="#89f5e7" strokeWidth="2" />
                      <line x1="70" y1="80" x2="125" y2="60" stroke="#89f5e7" strokeWidth="1.5" />
                      <line x1="150" y1="80" x2="125" y2="60" stroke="#89f5e7" strokeWidth="1.5" />
                      {/* Hoist Cable with load */}
                      <line x1="75" y1="80" x2="75" y2="115" stroke="#89f5e7" strokeWidth="1" />
                      <rect x="70" y="115" width="10" height="6" fill="#89f5e7" />
                      <circle cx="125" cy="58" r="2" fill="#ffb703" className="animate-ping" />
                    </g>
                    <g id="climbing-crane-right">
                      <rect x="250" y="90" width="10" height="50" fill="#2563eb" stroke="#89f5e7" strokeWidth="1" />
                      <line x1="210" y1="90" x2="295" y2="90" stroke="#89f5e7" strokeWidth="2" />
                      <line x1="210" y1="90" x2="255" y2="70" stroke="#89f5e7" strokeWidth="1.5" />
                      <line x1="295" y1="90" x2="255" y2="70" stroke="#89f5e7" strokeWidth="1.5" />
                      <circle cx="255" cy="68" r="2" fill="#ffb703" className="animate-ping" />
                    </g>
                  </g>

                  {/* STAGE 4 ELEMENTS: FULLY COMPLETED ARCHITECTURAL ICON (Full 48-Floor Smart Tower) */}
                  <g
                    id="layer-stage-4"
                    className="transition-opacity duration-500"
                    style={{ opacity: activeStage >= 4 ? 1 : 0 }}
                  >
                    {/* Full Height Sculpted Glass & Steel Monolith */}
                    <polygon
                      points="80,410 70,120 110,40 270,40 310,120 300,410"
                      fill="url(#glazeGradient)"
                      stroke="#89f5e7"
                      strokeWidth="2"
                    />
                    {/* Elegant Architectural Crown Spire */}
                    <line x1="190" y1="40" x2="190" y2="10" stroke="#89f5e7" strokeWidth="3" />
                    <circle cx="190" cy="10" r="3" fill="#89f5e7" className="animate-ping" />
                    {/* Floor Mullions & Horizontal Architectural Brise-Soleil (Floors 1 to 48) */}
                    <path
                      d="M 72,120 L 308,120 M 74,150 L 306,150 M 76,180 L 304,180 M 78,210 L 302,210 M 79,240 L 301,240 M 80,270 L 300,270 M 80,300 L 300,300 M 80,330 L 300,330 M 80,360 L 300,360 M 80,390 L 300,390"
                      stroke="#ffffff"
                      strokeWidth="1"
                      strokeOpacity="0.35"
                    />
                    {/* High-Rise Luminescent Interior Ambient Lights */}
                    <rect x="95" y="60" width="190" height="2" fill="#89f5e7" fillOpacity="0.9" />
                    <rect x="85" y="170" width="210" height="1.5" fill="#89f5e7" fillOpacity="0.7" />
                    <rect x="82" y="290" width="216" height="1.5" fill="#89f5e7" fillOpacity="0.7" />
                    {/* Grand Entrance Canopy Atrium at Ground */}
                    <polygon
                      points="60,410 80,390 300,390 320,410"
                      fill="#2563eb"
                      fillOpacity="0.5"
                      stroke="#89f5e7"
                      strokeWidth="1.5"
                    />
                    <text
                      x="190"
                      y="405"
                      fill="#ffffff"
                      fontFamily="Plus Jakarta Sans"
                      fontSize="8"
                      fontWeight="bold"
                      letterSpacing="2"
                      textAnchor="middle"
                    >
                      SMART IOT PORTAL READY
                    </text>
                  </g>

                  {/* Dynamic Autonomous Drone LiDAR Scan Cone (Interactive Element) */}
                  <g
                    id="lidar-drone-flight"
                    className="transition-transform duration-700"
                    transform={`translate(0, ${stage.droneY - 130})`}
                  >
                    <line
                      id="lidar-ray-1"
                      x1="45"
                      y1="130"
                      x2="160"
                      y2="410"
                      stroke="#89f5e7"
                      strokeWidth="0.75"
                      strokeDasharray="2 3"
                      strokeOpacity="0.6"
                    />
                    <line
                      id="lidar-ray-2"
                      x1="45"
                      y1="130"
                      x2="310"
                      y2="410"
                      stroke="#89f5e7"
                      strokeWidth="0.75"
                      strokeDasharray="2 3"
                      strokeOpacity="0.6"
                    />
                    {/* Drone body */}
                    <circle cx="45" cy="130" r="5" fill="#0053db" stroke="#89f5e7" strokeWidth="1.5" />
                    <line x1="38" y1="130" x2="52" y2="130" stroke="#89f5e7" strokeWidth="1.5" />
                    <circle cx="36" cy="130" r="1.5" fill="#89f5e7" />
                    <circle cx="54" cy="130" r="1.5" fill="#89f5e7" />
                  </g>
                </svg>
              </div>

              {/* Bottom HUD Status Strip */}
              <div className="w-full mt-4 flex items-center justify-between text-outline-variant font-label-sm text-[11px] pt-3 border-t border-outline/20 z-20">
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-secondary-fixed"></span>
                  <span>BIM COORDINATE: Z+168.4m // GRID 4D MATRIX</span>
                </span>
                <span className="text-secondary-fixed font-bold font-mono">AUTONOMOUS MESH: RECONCILED</span>
              </div>
            </div>

            {/* Live Engineering Telemetry & Milestone Intelligence Cards (5 cols) */}
            <div className="lg:col-span-5 flex flex-col justify-between space-y-4">
              {/* Current Milestone Detailed Card */}
              <div className="bg-on-background/70 border border-outline/20 rounded-2xl p-6 backdrop-blur-md">
                <div className="flex items-center justify-between mb-3">
                  <span
                    id="stage-badge"
                    className="px-2.5 py-1 rounded-md bg-secondary-fixed/15 text-secondary-fixed font-label-sm text-label-sm font-semibold uppercase tracking-wider"
                  >
                    {stage.badge}
                  </span>
                  <span
                    id="stage-completion"
                    className="font-data-metric text-[22px] text-secondary-fixed font-bold"
                  >
                    {stage.completion}
                  </span>
                </div>
                <h4
                  id="stage-title"
                  className="font-headline-md text-headline-md text-inverse-on-surface font-bold mb-2"
                >
                  {stage.title}
                </h4>
                <p
                  id="stage-desc"
                  className="font-body-sm text-body-sm text-outline-variant leading-relaxed"
                >
                  {stage.desc}
                </p>
              </div>

              {/* 5 Rich NirmaanIQ AI Telemetry Overlays Floating Beside Structure */}
              <div className="space-y-3">
                {/* Telemetry 1 */}
                <div className="bg-on-background/50 hover:bg-on-background/80 border border-outline/20 hover:border-secondary-fixed/50 transition-all p-3.5 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-lg bg-primary-container/20 text-secondary-fixed flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">straighten</span>
                    </span>
                    <div>
                      <div className="font-label-sm text-[11px] text-outline-variant uppercase">
                        AI Structural Variance Tracker
                      </div>
                      <div
                        id="tel-variance"
                        className="font-headline-sm text-[13px] font-semibold text-inverse-on-surface"
                      >
                        {stage.variance}
                      </div>
                    </div>
                  </div>
                  <span className="material-symbols-outlined text-secondary-fixed text-[18px]">check_circle</span>
                </div>

                {/* Telemetry 2 */}
                <div className="bg-on-background/50 hover:bg-on-background/80 border border-outline/20 hover:border-secondary-fixed/50 transition-all p-3.5 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-lg bg-secondary-fixed/15 text-secondary-fixed flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">view_in_ar</span>
                    </span>
                    <div>
                      <div className="font-label-sm text-[11px] text-outline-variant uppercase">
                        LiDAR Volumetric Rebar Density
                      </div>
                      <div
                        id="tel-density"
                        className="font-headline-sm text-[13px] font-semibold text-inverse-on-surface"
                      >
                        {stage.density}
                      </div>
                    </div>
                  </div>
                  <span className="text-secondary-fixed font-mono text-[12px] font-bold">±2mm</span>
                </div>

                {/* Telemetry 3 */}
                <div className="bg-on-background/50 hover:bg-on-background/80 border border-outline/20 hover:border-secondary-fixed/50 transition-all p-3.5 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-lg bg-tertiary-fixed/15 text-tertiary-fixed flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">thermostat</span>
                    </span>
                    <div>
                      <div className="font-label-sm text-[11px] text-outline-variant uppercase">
                        Autonomous Concrete Curing Index
                      </div>
                      <div
                        id="tel-curing"
                        className="font-headline-sm text-[13px] font-semibold text-inverse-on-surface"
                      >
                        {stage.curing}
                      </div>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded bg-secondary-fixed/10 text-secondary-fixed font-mono text-[11px] font-semibold">
                    PASS
                  </span>
                </div>

                {/* Telemetry 4 */}
                <div className="bg-on-background/50 hover:bg-on-background/80 border border-outline/20 hover:border-secondary-fixed/50 transition-all p-3.5 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-lg bg-primary-fixed/15 text-primary-fixed flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">event_available</span>
                    </span>
                    <div>
                      <div className="font-label-sm text-[11px] text-outline-variant uppercase">
                        BIM 4D Schedule Adherence
                      </div>
                      <div
                        id="tel-schedule"
                        className="font-headline-sm text-[13px] font-semibold text-inverse-on-surface"
                      >
                        {stage.schedule}
                      </div>
                    </div>
                  </div>
                  <span className="material-symbols-outlined text-secondary-fixed text-[18px]">trending_up</span>
                </div>

                {/* Telemetry 5 */}
                <div className="bg-on-background/50 hover:bg-on-background/80 border border-outline/20 hover:border-secondary-fixed/50 transition-all p-3.5 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-lg bg-secondary-container/20 text-secondary-fixed flex items-center justify-center">
                      <span className="material-symbols-outlined text-[18px]">badge</span>
                    </span>
                    <div>
                      <div className="font-label-sm text-[11px] text-outline-variant uppercase">
                        Daily Biometric Crew &amp; Drone Scan
                      </div>
                      <div
                        id="tel-crew"
                        className="font-headline-sm text-[13px] font-semibold text-inverse-on-surface"
                      >
                        {stage.crew}
                      </div>
                    </div>
                  </div>
                  <span className="w-2.5 h-2.5 rounded-full bg-secondary-fixed animate-ping"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Elegant bottom layered contour back into light surface */}
      <WaveDivider variant="section4-bottom" />
    </section>
  );
};
