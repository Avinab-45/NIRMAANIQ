import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import skylineImage from '../../assets/images/blue_construction_skyline.jpg';

export const HeroParticleWave: React.FC = () => {
  const containerRef = useRef<HTMLDivElement>(null);
  const skylineWrapperRef = useRef<HTMLDivElement>(null);
  const bgLayerRef = useRef<HTMLDivElement>(null);
  const midLayerRef = useRef<SVGSVGElement>(null);
  const foreLayerRef = useRef<SVGSVGElement>(null);

  // Mouse interaction targets for smooth 3D tilt & parallax (static on touch devices)
  const targetTiltRef = useRef({ x: 0, y: 0 });
  const currentTiltRef = useRef({ x: 0, y: 0 });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    function getDims() {
      const w = container?.clientWidth || window.innerWidth || 1440;
      const h = container?.clientHeight || window.innerHeight || 500;
      return { w: Math.max(w, 320), h: Math.max(h, 240) };
    }

    let dims = getDims();

    // Scene & Camera setup for Planar LiDAR Telemetry
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(46, dims.w / dims.h, 1, 3500);
    camera.position.set(0, 180, 560);
    camera.lookAt(0, -30, 0);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance',
    });
    renderer.setSize(dims.w, dims.h);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setClearColor(0x000000, 0);

    const domElement = renderer.domElement;
    domElement.style.position = 'absolute';
    domElement.style.top = '0';
    domElement.style.left = '0';
    domElement.style.width = '100%';
    domElement.style.height = '100%';
    domElement.style.zIndex = '3'; // Floats across the wireframe construction grid
    domElement.style.pointerEvents = 'none';
    container.appendChild(domElement);

    // Particle Grid Parameters: 16,560 Particles (AMOUNTX 180 x AMOUNTY 92)
    const SEPARATION = 16;
    const AMOUNTX = 180;
    const AMOUNTY = 92;
    const numParticles = AMOUNTX * AMOUNTY;

    const positions = new Float32Array(numParticles * 3);
    const scales = new Float32Array(numParticles);
    const colors = new Float32Array(numParticles * 3);

    // High-contrast vivid emerald green and cyan dots
    const colorDeepGreen = new THREE.Color(0x15803d);
    const colorVividNeon = new THREE.Color(0x16a34a);
    const colorBrightLime = new THREE.Color(0x22c55e);
    const colorDeepTeal = new THREE.Color(0x00d2ff); // High-tech cyan matching blue blueprint

    let i = 0;
    let j = 0;
    for (let ix = 0; ix < AMOUNTX; ix++) {
      for (let iy = 0; iy < AMOUNTY; iy++) {
        const posX = ix * SEPARATION - (AMOUNTX * SEPARATION) / 2;
        const posZ = iy * SEPARATION - (AMOUNTY * SEPARATION) / 2;
        const posY = -72; // Planar LiDAR survey plane along the skyline foundation baseline

        positions[i] = posX;
        positions[i + 1] = posY;
        positions[i + 2] = posZ;

        scales[j] = 1.0;

        const ratioX = ix / AMOUNTX;
        const ratioY = iy / AMOUNTY;

        const mixed = colorVividNeon
          .clone()
          .lerp(colorBrightLime, Math.sin(ratioX * Math.PI) * 0.85)
          .lerp(colorDeepTeal, ratioY * 0.55)
          .lerp(colorDeepGreen, (1.0 - Math.sin(ratioX * Math.PI)) * 0.35);

        colors[i] = mixed.r;
        colors[i + 1] = mixed.g;
        colors[i + 2] = mixed.b;

        i += 3;
        j++;
      }
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('scale', new THREE.BufferAttribute(scales, 1));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

    const vertexShader = `
      attribute float scale;
      attribute vec3 color;
      varying vec3 vColor;
      varying float vDepth;

      void main() {
        vColor = color;
        vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
        vDepth = -mvPosition.z;
        gl_PointSize = scale * (380.0 / -mvPosition.z);
        gl_Position = projectionMatrix * mvPosition;
      }
    `;

    const fragmentShader = `
      varying vec3 vColor;
      varying float vDepth;

      void main() {
        vec2 coord = gl_PointCoord - vec2(0.5);
        float dist = length(coord);
        if (dist > 0.5) discard;

        float alpha = smoothstep(0.5, 0.04, dist);
        float depthFade = clamp((1800.0 - vDepth) / 1100.0, 0.35, 1.0);

        gl_FragColor = vec4(vColor * 1.15, alpha * depthFade * 0.98);
      }
    `;

    const material = new THREE.ShaderMaterial({
      vertexShader: vertexShader,
      fragmentShader: fragmentShader,
      transparent: true,
      depthTest: false,
      blending: THREE.AdditiveBlending,
    });

    const particles = new THREE.Points(geometry, material);
    scene.add(particles);

    // Mouse Interaction Tracking (for Three.js camera & particles)
    let mouseX = 0;
    let mouseY = 0;
    let targetMouseX = 0;
    let targetMouseY = 0;

    function onPointerMove(event: PointerEvent) {
      if (!container) return;
      if (event.pointerType === 'touch') return; // Touch devices remain static

      const rect = container.getBoundingClientRect();
      const relX = event.clientX - (rect.left + rect.width / 2);
      const relY = event.clientY - (rect.top + rect.height / 2);

      targetMouseX = relX * 0.7;
      targetMouseY = relY * 0.5;

      // Normalized coordinates (-1 to +1) for smooth architectural parallax & 3D tilt
      const normX = Math.max(-1, Math.min(1, relX / (rect.width / 2)));
      const normY = Math.max(-1, Math.min(1, relY / (rect.height / 2)));
      targetTiltRef.current = { x: normX, y: normY };
    }

    function onPointerLeave() {
      // Smoothly return everything to its original neutral position when cursor leaves
      targetMouseX = 0;
      targetMouseY = 0;
      targetTiltRef.current = { x: 0, y: 0 };
    }

    container.addEventListener('pointermove', onPointerMove, { passive: true });
    container.addEventListener('pointerleave', onPointerLeave, { passive: true });

    function onWindowResize() {
      dims = getDims();
      camera.aspect = dims.w / dims.h;
      camera.updateProjectionMatrix();
      renderer.setSize(dims.w, dims.h);
    }

    window.addEventListener('resize', onWindowResize);

    let animationFrameId: number;
    let count = 0;

    function animate() {
      animationFrameId = requestAnimationFrame(animate);

      // Smooth interpolation for Three.js camera
      mouseX += (targetMouseX - mouseX) * 0.06;
      mouseY += (targetMouseY - mouseY) * 0.06;

      camera.position.x += (mouseX - camera.position.x) * 0.04;
      camera.position.y += (-mouseY + 180 - camera.position.y) * 0.04;
      camera.lookAt(0, -30, 0);

      // Smooth interpolation for Building Parallax & 3D Tilt (No bouncing, returns gently to 0)
      currentTiltRef.current.x += (targetTiltRef.current.x - currentTiltRef.current.x) * 0.06;
      currentTiltRef.current.y += (targetTiltRef.current.y - currentTiltRef.current.y) * 0.06;

      const curX = currentTiltRef.current.x;
      const curY = currentTiltRef.current.y;

      // Apply subtle 3D tilt to the skyline container
      if (skylineWrapperRef.current) {
        skylineWrapperRef.current.style.transform = `perspective(1200px) rotateX(${curY * -2.4}deg) rotateY(${curX * 3.2}deg)`;
      }

      // Parallax shifts across the architectural layers
      if (bgLayerRef.current) {
        bgLayerRef.current.style.transform = `translate3d(${curX * -10}px, ${curY * -4}px, 0)`;
      }
      if (midLayerRef.current) {
        midLayerRef.current.style.transform = `translate3d(${curX * -22}px, ${curY * -9}px, 0)`;
      }
      if (foreLayerRef.current) {
        foreLayerRef.current.style.transform = `translate3d(${curX * -36}px, ${curY * -15}px, 0)`;
      }

      // Update Three.js particles: High-precision LiDAR survey grid (NO wave hills)
      const posAttr = particles.geometry.attributes.position;
      const scaleAttr = particles.geometry.attributes.scale;
      const posArray = posAttr.array as Float32Array;
      const scaleArray = scaleAttr.array as Float32Array;

      let idx = 0;
      let sIdx = 0;

      for (let ix = 0; ix < AMOUNTX; ix++) {
        for (let iy = 0; iy < AMOUNTY; iy++) {
          const pX = posArray[idx];
          const pZ = posArray[idx + 2];
          const dist = Math.sqrt(Math.pow(pX - mouseX, 2) + Math.pow(pZ - mouseY * 1.3, 2));

          // Subtle interactive cursor ripple (zero wave hills; planar survey datum)
          const cursorDistort = Math.sin(dist * 0.02 - count * 1.2) * Math.max(0, (280 - dist) * 0.2);

          // Planar LiDAR scan telemetry surface across baseline
          posArray[idx + 1] = -72 + cursorDistort;

          // Crisp LiDAR point scaling
          scaleArray[sIdx] = 4.2 + Math.max(0, (280 - dist) / 280) * 7.5;

          idx += 3;
          sIdx++;
        }
      }

      posAttr.needsUpdate = true;
      scaleAttr.needsUpdate = true;

      count += 0.035;
      renderer.render(scene, camera);
    }

    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);
      container.removeEventListener('pointermove', onPointerMove);
      container.removeEventListener('pointerleave', onPointerLeave);
      window.removeEventListener('resize', onWindowResize);
      geometry.dispose();
      material.dispose();
      renderer.dispose();
      if (domElement && domElement.parentNode === container) {
        container.removeChild(domElement);
      }
    };
  }, []);

  return (
    <div
      ref={containerRef}
      id="interactive-preview"
      className="relative w-full h-[480px] lg:h-[560px] overflow-hidden z-10 select-none cursor-crosshair bg-[#030712]"
    >
      {/* Scoped CSS Keyframe Animations for Subtle Construction Activity */}
      <style>{`
        @keyframes craneSlewWest {
          0% { transform: rotate(0deg); }
          50% { transform: rotate(10deg); }
          100% { transform: rotate(0deg); }
        }
        @keyframes craneSlewCenter {
          0% { transform: rotate(0deg); }
          50% { transform: rotate(-12deg); }
          100% { transform: rotate(0deg); }
        }
        @keyframes craneSlewEast {
          0% { transform: rotate(4deg); }
          50% { transform: rotate(-8deg); }
          100% { transform: rotate(4deg); }
        }
        @keyframes hoistCycleWest {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(24px); }
        }
        @keyframes hoistCycleCenter {
          0%, 100% { transform: translateY(18px); }
          50% { transform: translateY(0); }
        }
        @keyframes hoistCycleEast {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(16px); }
        }
        @keyframes beaconPulseAmber {
          0%, 100% { opacity: 0.35; transform: scale(0.9); }
          50% { opacity: 1; transform: scale(1.4); }
        }
        @keyframes beaconPulseCyan {
          0%, 100% { opacity: 0.4; transform: scale(0.95); }
          50% { opacity: 1; transform: scale(1.35); }
        }
        @keyframes laserSweepScan {
          0% { stroke-dashoffset: 1440; }
          100% { stroke-dashoffset: -1440; }
        }
        @keyframes constellationBlink {
          0%, 100% { opacity: 0.25; }
          50% { opacity: 0.75; }
        }
        @keyframes floorLightShimmer {
          0%, 100% { opacity: 0.3; }
          50% { opacity: 0.85; }
        }
      `}</style>

      {/* 3D PERSPECTIVE WRAPPER FOR THE BLUE ARCHITECTURAL SKYLINE */}
      <div
        ref={skylineWrapperRef}
        className="absolute inset-0 w-full h-full pointer-events-none z-0"
        style={{
          transformStyle: 'preserve-3d',
          transformOrigin: '50% 80%',
          willChange: 'transform',
        }}
      >
        {/* ========================================================
            LAYER 0: HIGH-RESOLUTION BLUE CONSTRUCTION CITYSCAPE BACKDROP
            Exact visual match to the architectural reference image
            ======================================================== */}
        <div
          ref={bgLayerRef}
          className="absolute inset-0 w-full h-full pointer-events-none"
          style={{ willChange: 'transform' }}
        >
          {/* Horizon Atmospheric Cyan Glow */}
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_50%_75%,rgba(0,180,255,0.22)_0%,rgba(0,102,255,0.1)_45%,transparent_75%)] pointer-events-none z-0"></div>

          {/* High-Fidelity Blue Cityscape Image */}
          <img
            src={skylineImage}
            alt="NirmaanIQ Blue Architectural Construction Cityscape"
            className="w-full h-full object-cover object-bottom select-none opacity-95 pointer-events-none filter brightness-105 contrast-110"
          />

          {/* Top Fade Gradient for clean Hero Section text readability */}
          <div className="absolute top-0 left-0 right-0 h-44 bg-gradient-to-b from-[#030712] via-[#030712]/85 to-transparent pointer-events-none z-10"></div>

          {/* Left & Right Vignettes for seamless horizon integration */}
          <div className="absolute inset-y-0 left-0 w-32 bg-gradient-to-r from-[#030712] to-transparent pointer-events-none z-10"></div>
          <div className="absolute inset-y-0 right-0 w-32 bg-gradient-to-l from-[#030712] to-transparent pointer-events-none z-10"></div>

          {/* Bottom Ground Bedrock Transition */}
          <div className="absolute bottom-0 left-0 right-0 h-10 bg-gradient-to-t from-[#030712] to-transparent pointer-events-none z-10"></div>
        </div>

        {/* ========================================================
            LAYER 1: ANIMATED TOWER CRANES & STRUCTURAL CONSTRUCTION ACTIVITY
            Interactive SVG overlay positioned over the cityscape towers
            ======================================================== */}
        <svg
          ref={midLayerRef}
          className="absolute inset-0 w-full h-full object-cover pointer-events-none z-1"
          viewBox="0 0 1440 540"
          preserveAspectRatio="xMidYMid slice"
          aria-hidden="true"
          style={{ willChange: 'transform' }}
        >
          <defs>
            {/* Luminous Blueprint Neon Glow Filter */}
            <filter id="neonBlueGlow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="2" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>

            <linearGradient id="laserBeamGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#00d2ff" stopOpacity="0" />
              <stop offset="50%" stopColor="#00d2ff" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#00d2ff" stopOpacity="0" />
            </linearGradient>

            <linearGradient id="craneMastSteel" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#00f0ff" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#0055ff" stopOpacity="0.6" />
            </linearGradient>
          </defs>

          {/* ----------------------------------------------------
              CONSTRUCTION ACTIVITY 1: WEST TOWER CRANE (x: ~245)
              Slowly slewing jib + lifting hoist block
              ---------------------------------------------------- */}
          <g id="animated-crane-west">
            {/* Crane Mast Tower with cross-lattice bracing */}
            <rect x="245" y="140" width="8" height="90" fill="url(#craneMastSteel)" stroke="#00f0ff" strokeWidth="0.8" />
            <path d="M 245,145 L 253,155 M 245,165 L 253,175 M 245,185 L 253,195 M 245,205 L 253,215" stroke="#00f0ff" strokeWidth="0.6" strokeOpacity="0.8" />
            {/* Slewing Deck */}
            <rect x="242" y="136" width="14" height="4" fill="#00f0ff" />

            {/* Rotating Jib Assembly around mast axis (249, 138) */}
            <g
              style={{
                transformOrigin: '249px 138px',
                animation: 'craneSlewWest 18s ease-in-out infinite',
              }}
            >
              {/* Horizontal Hammerhead Working Jib */}
              <line x1="249" y1="138" x2="350" y2="138" stroke="#00f0ff" strokeWidth="2" filter="url(#neonBlueGlow)" />
              {/* Counter-jib & Counterweight */}
              <line x1="249" y1="138" x2="200" y2="138" stroke="#00f0ff" strokeWidth="1.5" />
              <rect x="194" y="134" width="10" height="7" fill="#ffb703" stroke="#ffffff" strokeWidth="0.5" />
              {/* A-Frame Tower Peak */}
              <polygon points="245,138 249,118 253,138" fill="#0055ff" stroke="#00f0ff" strokeWidth="1" />
              <line x1="249" y1="118" x2="320" y2="138" stroke="#00f0ff" strokeWidth="0.8" strokeOpacity="0.7" />
              <line x1="249" y1="118" x2="205" y2="138" stroke="#00f0ff" strokeWidth="0.8" strokeOpacity="0.7" />

              {/* Hoist Trolley Cable & Suspended Precast Payload */}
              <g
                style={{
                  animation: 'hoistCycleWest 12s ease-in-out infinite',
                }}
              >
                <line x1="315" y1="138" x2="315" y2="188" stroke="#00d2ff" strokeWidth="1" strokeDasharray="3 2" />
                <rect x="309" y="188" width="12" height="6" fill="#00f0ff" stroke="#ffffff" strokeWidth="0.5" />
              </g>

              {/* Aviation Warning Flashing Beacon on Jib Tip */}
              <circle
                cx="350"
                cy="138"
                r="3"
                fill="#ffb703"
                style={{
                  transformOrigin: '350px 138px',
                  animation: 'beaconPulseAmber 2s ease-in-out infinite',
                }}
              />
            </g>
          </g>

          {/* ----------------------------------------------------
              CONSTRUCTION ACTIVITY 2: CENTER-LEFT CLIMBING CRANE (x: ~525)
              Angled luffing boom slewing with amber beacon
              ---------------------------------------------------- */}
          <g id="animated-crane-center-left">
            <rect x="525" y="180" width="9" height="110" fill="url(#craneMastSteel)" stroke="#00f0ff" strokeWidth="0.8" />
            <path d="M 525,185 L 534,195 M 525,205 L 534,215 M 525,225 L 534,235 M 525,245 L 534,255" stroke="#00f0ff" strokeWidth="0.6" strokeOpacity="0.8" />
            <rect x="522" y="176" width="15" height="4" fill="#00f0ff" />

            <g
              style={{
                transformOrigin: '529px 178px',
                animation: 'craneSlewCenter 22s ease-in-out infinite',
              }}
            >
              {/* Luffing Jib */}
              <line x1="529" y1="178" x2="610" y2="105" stroke="#00f0ff" strokeWidth="2.2" filter="url(#neonBlueGlow)" />
              <line x1="522" y1="178" x2="610" y2="105" stroke="#00f0ff" strokeWidth="0.9" />
              {/* Counter-jib & Amber Counterweight */}
              <line x1="529" y1="178" x2="485" y2="186" stroke="#00f0ff" strokeWidth="1.5" />
              <rect x="478" y="183" width="11" height="8" fill="#ffb703" stroke="#ffffff" strokeWidth="0.5" />

              {/* Vertical Hoist Cable & Hook */}
              <g
                style={{
                  animation: 'hoistCycleCenter 14s ease-in-out infinite',
                }}
              >
                <line x1="605" y1="110" x2="605" y2="175" stroke="#00d2ff" strokeWidth="1" strokeDasharray="3 2" />
                <rect x="599" y="175" width="12" height="7" fill="#00f0ff" stroke="#ffffff" strokeWidth="0.5" />
              </g>

              {/* Apex Flashing Beacon */}
              <circle
                cx="610"
                cy="105"
                r="3"
                fill="#ffb703"
                style={{
                  transformOrigin: '610px 105px',
                  animation: 'beaconPulseAmber 2.4s ease-in-out infinite',
                }}
              />
            </g>
          </g>

          {/* ----------------------------------------------------
              CONSTRUCTION ACTIVITY 3: CENTRAL SUPERTALL CLIMBING CRANE (x: ~840)
              Heavy Tower Crane slewing over the central skyscraper
              ---------------------------------------------------- */}
          <g id="animated-crane-supertall">
            <rect x="840" y="90" width="10" height="95" fill="url(#craneMastSteel)" stroke="#00f0ff" strokeWidth="1" />
            <path d="M 840,95 L 850,105 M 840,115 L 850,125 M 840,135 L 850,145 M 840,155 L 850,165" stroke="#00f0ff" strokeWidth="0.7" strokeOpacity="0.8" />
            <rect x="837" y="86" width="16" height="5" fill="#00f0ff" />

            <g
              style={{
                transformOrigin: '845px 88px',
                animation: 'craneSlewEast 16s ease-in-out infinite',
              }}
            >
              <line x1="845" y1="88" x2="980" y2="88" stroke="#00f0ff" strokeWidth="2.5" filter="url(#neonBlueGlow)" />
              <line x1="845" y1="88" x2="785" y2="88" stroke="#00f0ff" strokeWidth="1.8" />
              <rect x="778" y="84" width="12" height="9" fill="#ffb703" stroke="#ffffff" strokeWidth="0.5" />

              {/* Apex A-Frame */}
              <polygon points="840,88 845,64 850,88" fill="#0055ff" stroke="#00f0ff" strokeWidth="1.2" />
              <line x1="845" y1="64" x2="940" y2="88" stroke="#00f0ff" strokeWidth="0.9" strokeOpacity="0.7" />
              <line x1="845" y1="64" x2="790" y2="88" stroke="#00f0ff" strokeWidth="0.9" strokeOpacity="0.7" />

              {/* Hoist cable & precast module */}
              <g
                style={{
                  animation: 'hoistCycleEast 10s ease-in-out infinite',
                }}
              >
                <line x1="935" y1="88" x2="935" y2="150" stroke="#00d2ff" strokeWidth="1.2" strokeDasharray="3 2" />
                <rect x="928" y="150" width="14" height="7" fill="#00f0ff" stroke="#ffffff" strokeWidth="0.5" />
              </g>

              {/* Warning Beacon */}
              <circle
                cx="845"
                cy="64"
                r="3"
                fill="#ffb703"
                style={{
                  transformOrigin: '845px 64px',
                  animation: 'beaconPulseAmber 1.8s ease-in-out infinite',
                }}
              />
            </g>
          </g>

          {/* ----------------------------------------------------
              CONSTRUCTION ACTIVITY 4: EAST HIGH-RISE CRANE (x: ~1185)
              Jib lifting structural rebar cage
              ---------------------------------------------------- */}
          <g id="animated-crane-east">
            <rect x="1185" y="130" width="8" height="85" fill="url(#craneMastSteel)" stroke="#00f0ff" strokeWidth="0.8" />
            <rect x="1182" y="126" width="14" height="4" fill="#00f0ff" />

            <g
              style={{
                transformOrigin: '1189px 128px',
                animation: 'craneSlewWest 20s ease-in-out infinite',
              }}
            >
              <line x1="1189" y1="128" x2="1310" y2="128" stroke="#00f0ff" strokeWidth="2" filter="url(#neonBlueGlow)" />
              <line x1="1189" y1="128" x2="1140" y2="128" stroke="#00f0ff" strokeWidth="1.5" />
              <rect x="1134" y="124" width="10" height="8" fill="#ffb703" stroke="#ffffff" strokeWidth="0.5" />

              <polygon points="1185,128 1189,108 1193,128" fill="#0055ff" stroke="#00f0ff" strokeWidth="1" />
              <line x1="1189" y1="108" x2="1275" y2="128" stroke="#00f0ff" strokeWidth="0.8" strokeOpacity="0.7" />

              <g
                style={{
                  animation: 'hoistCycleWest 13s ease-in-out infinite',
                }}
              >
                <line x1="1270" y1="128" x2="1270" y2="195" stroke="#00d2ff" strokeWidth="1" strokeDasharray="3 2" />
                <rect x="1264" y="195" width="12" height="6" fill="#00f0ff" />
              </g>

              <circle
                cx="1310"
                cy="128"
                r="3"
                fill="#ffb703"
                style={{
                  transformOrigin: '1310px 128px',
                  animation: 'beaconPulseAmber 2.2s ease-in-out infinite',
                }}
              />
            </g>
          </g>

          {/* ----------------------------------------------------
              TELEMETRY DATA ARCS & CONSTELLATION NODES
              Linking crane tips & building spires (BIM IoT Network)
              ---------------------------------------------------- */}
          <g id="iot-constellation-network" opacity="0.6">
            {/* Curved spline data paths between towers */}
            <path
              d="M 249,118 Q 430,70 610,105"
              fill="none"
              stroke="#00d2ff"
              strokeWidth="1"
              strokeDasharray="4 6"
              style={{ animation: 'laserSweepScan 15s linear infinite' }}
            />
            <path
              d="M 610,105 Q 730,50 845,64"
              fill="none"
              stroke="#00d2ff"
              strokeWidth="1"
              strokeDasharray="5 7"
              style={{ animation: 'laserSweepScan 18s linear infinite' }}
            />
            <path
              d="M 845,64 Q 1010,40 1189,108"
              fill="none"
              stroke="#00d2ff"
              strokeWidth="1"
              strokeDasharray="4 6"
              style={{ animation: 'laserSweepScan 20s linear infinite' }}
            />

            {/* Glowing Network Telemetry Nodes */}
            <circle cx="249" cy="118" r="3" fill="#00f0ff" style={{ animation: 'constellationBlink 3s ease-in-out infinite' }} />
            <circle cx="610" cy="105" r="3" fill="#00f0ff" style={{ animation: 'constellationBlink 2.5s ease-in-out infinite' }} />
            <circle cx="845" cy="64" r="3.5" fill="#00f0ff" style={{ animation: 'constellationBlink 3.2s ease-in-out infinite' }} />
            <circle cx="1189" cy="108" r="3" fill="#00f0ff" style={{ animation: 'constellationBlink 2.8s ease-in-out infinite' }} />
          </g>

          {/* Active Vertical Laser True Plumb Line on Central Tower */}
          <line x1="710" y1="100" x2="710" y2="520" stroke="#00f0ff" strokeWidth="1" strokeDasharray="6 4" strokeOpacity="0.5" />
          <rect x="665" y="240" width="90" height="18" fill="#020b18" stroke="#00f0ff" strokeWidth="1" rx="4" opacity="0.9" />
          <text x="710" y="252" fill="#00f0ff" fontFamily="monospace" fontSize="8" letterSpacing="0.5" textAnchor="middle">
            0.00° TRUE PLUMB
          </text>
        </svg>

        {/* ========================================================
            LAYER 2: FOREGROUND GROUND DATUM & HORIZONTAL LASER BEAMS
            Connected baseline grid matching the reference blueprint
            ======================================================== */}
        <svg
          ref={foreLayerRef}
          className="absolute inset-0 w-full h-full object-cover pointer-events-none z-2"
          viewBox="0 0 1440 540"
          preserveAspectRatio="xMidYMid slice"
          aria-hidden="true"
          style={{ willChange: 'transform' }}
        >
          {/* Horizontal Laser Level Projection Datum Beams */}
          <line
            x1="0"
            y1="230"
            x2="1440"
            y2="230"
            stroke="url(#laserBeamGrad)"
            strokeWidth="1.5"
            strokeDasharray="8 6"
            style={{ animation: 'laserSweepScan 16s linear infinite' }}
          />
          <line
            x1="0"
            y1="390"
            x2="1440"
            y2="390"
            stroke="url(#laserBeamGrad)"
            strokeWidth="1.2"
            strokeDasharray="6 4"
            style={{ animation: 'laserSweepScan 20s linear infinite' }}
          />

          {/* Unified Ground Baseline Grid & Foundation Stratum */}
          <line x1="0" y1="520" x2="1440" y2="520" stroke="#00f0ff" strokeWidth="1.8" strokeDasharray="6 4" strokeOpacity="0.8" />
          <rect x="0" y="525" width="1440" height="15" fill="#030712" />
        </svg>
      </div>

      {/* Interactive HUD Floating Badges on Particle Stream */}
      <div className="absolute bottom-16 left-6 lg:left-16 z-20 hidden md:flex items-center gap-3 bg-[#020b18]/90 border border-[#00f0ff]/40 px-4 py-2 rounded-xl backdrop-blur-md shadow-2xl pointer-events-none">
        <span className="w-2.5 h-2.5 rounded-full bg-[#00f0ff] animate-ping"></span>
        <div className="flex flex-col">
          <span className="font-label-sm text-[11px] text-[#00f0ff] font-bold uppercase tracking-wider font-mono">
            4D Blue Skyline Telemetry
          </span>
          <span className="font-body-sm text-[12px] text-inverse-on-surface">
            Active Architectural Cityscape · Interactive 3D Tilt
          </span>
        </div>
      </div>

      <div className="absolute bottom-16 right-6 lg:right-16 z-20 hidden md:flex items-center gap-3 bg-[#020b18]/90 border border-outline/30 px-4 py-2 rounded-xl backdrop-blur-md pointer-events-none">
        <span className="material-symbols-outlined text-[#00f0ff] text-[18px]">stream</span>
        <span className="font-label-sm text-[12px] text-[#00f0ff] font-mono font-medium">
          EMERALD &amp; CYAN TELEMETRY • 16,560 VERTICES
        </span>
      </div>

      <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/10 via-transparent to-transparent pointer-events-none"></div>
    </div>
  );
};