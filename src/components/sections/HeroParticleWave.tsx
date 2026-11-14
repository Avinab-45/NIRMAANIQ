import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

// Same particle budget as the original wave: 180 x 92 = 16,560 particles.
const NUM_PARTICLES = 180 * 92;
const GROUND_Y = -230;

// 1 = the original camera parallax strength. A skyline is a vertical scene, so
// full strength swings it too far sideways; 0.35 keeps it lively but readable.
const PARALLAX = 0.35;

// true = blue construction palette, false = the original green / teal palette.
const USE_BLUE = true;

const KIND_STATIC = 0;
const KIND_JIB = 1;
const KIND_CABLE = 2;
const KIND_BLINK = 3;
const KIND_MOTE = 4;

interface Crane {
  x: number;
  y: number;
  z: number;
  phase: number;
}

interface Skyline {
  base: Float32Array;
  size: Float32Array;
  kind: Uint8Array;
  aux: Float32Array;
  dim: Float32Array;
  cranes: Crane[];
}

function mulberry32(seed: number) {
  let a = seed;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/**
 * Lays out every one of the 16,560 particles as part of a construction skyline:
 * towers with stepped crowns and spires, two buildings still under construction
 * (scaffolding + exposed rebar), two tower cranes, a ground plane, a fainter
 * back row of buildings for depth, and floating site-data motes.
 * Deterministic: same layout on every load.
 */
function buildSkyline(): Skyline {
  const N = NUM_PARTICLES;
  const G = GROUND_Y;
  const rand = mulberry32(20261114);

  const base = new Float32Array(N * 3);
  const size = new Float32Array(N);
  const kind = new Uint8Array(N);
  const aux = new Float32Array(N * 3);
  const dim = new Float32Array(N);
  const cranes: Crane[] = [];
  let n = 0;

  const add = (
    x: number,
    y: number,
    z: number,
    s: number,
    k: number = KIND_STATIC,
    a0 = 0,
    a1 = 0,
    a2 = 0,
    d = 1
  ) => {
    if (n >= N) return;
    const i3 = n * 3;
    base[i3] = x;
    base[i3 + 1] = y;
    base[i3 + 2] = z;
    size[n] = s;
    kind[n] = k;
    aux[i3] = a0;
    aux[i3 + 1] = a1;
    aux[i3 + 2] = a2;
    dim[n] = d;
    n++;
  };

  const line = (
    x0: number,
    y0: number,
    z0: number,
    x1: number,
    y1: number,
    z1: number,
    step: number,
    s: number,
    d = 1
  ) => {
    const len = Math.hypot(x1 - x0, y1 - y0, z1 - z0);
    const c = Math.max(1, Math.round(len / step));
    for (let i = 0; i <= c; i++) {
      const t = i / c;
      add(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t, s, KIND_STATIC, 0, 0, 0, d);
    }
  };

  // Box outline: 4 vertical edges + top rectangle (front, back, two sides).
  const shell = (x0: number, x1: number, y0: number, y1: number, zf: number, zb: number) => {
    for (const x of [x0, x1]) {
      for (const z of [zf, zb]) line(x, y0, z, x, y1, z, 5, 5.2);
    }
    line(x0, y1, zf, x1, y1, zf, 5, 5.2);
    line(x0, y1, zb, x1, y1, zb, 5, 5.2);
    line(x0, y1, zb, x0, y1, zf, 5, 5.2);
    line(x1, y1, zb, x1, y1, zf, 5, 5.2);
  };

  // Window grid; a few windows are "lit" and softly pulse.
  const windows = (
    x0: number,
    x1: number,
    yA: number,
    yB: number,
    z: number,
    stepX: number,
    stepY: number,
    keep: number,
    litChance: number,
    d: number
  ) => {
    for (let y = yA; y < yB; y += stepY) {
      for (let x = x0; x < x1; x += stepX) {
        if (rand() > keep) continue;
        const lit = rand() < litChance;
        add(x, y, z, lit ? 6 : 4.2, lit ? KIND_BLINK : KIND_STATIC, rand() * 6.28, 0, 0, d);
      }
    }
  };

  // Scaffolding: vertical poles, horizontal ledgers and diagonal braces.
  const scaffold = (x0: number, x1: number, y0: number, y1: number, z: number) => {
    const cols = Math.max(2, Math.round((x1 - x0) / 14));
    const rows = Math.max(1, Math.round((y1 - y0) / 22));
    for (let c = 0; c <= cols; c++) {
      const x = x0 + ((x1 - x0) * c) / cols;
      line(x, y0, z, x, y1, z, 4, 3.8);
    }
    for (let r = 0; r <= rows; r++) {
      const y = y0 + ((y1 - y0) * r) / rows;
      line(x0, y, z, x1, y, z, 4, 3.8);
    }
    for (let c = 0; c < cols; c++) {
      for (let r = 0; r < rows; r++) {
        if ((c + r) % 2 !== 0) continue;
        const xa = x0 + ((x1 - x0) * c) / cols;
        const xb = x0 + ((x1 - x0) * (c + 1)) / cols;
        const ya = y0 + ((y1 - y0) * r) / rows;
        const yb = y0 + ((y1 - y0) * (r + 1)) / rows;
        line(xa, ya, z, xb, yb, z, 4, 3.4);
      }
    }
  };

  // ---- Main skyline -------------------------------------------------------
  const widths = [70, 75, 85, 55, 75, 80, 70, 75, 75, 85, 70, 70, 80, 65, 85, 70, 80];
  const heights = [120, 190, 270, 150, 230, 320, 180, 250, 140, 300, 330, 200, 260, 160, 310, 190, 130];
  const UNFINISHED = [4, 11];
  const STEPPED = [2, 7, 14];
  const SPIRES = [5, 10];

  let cursor = -680;
  const blds = widths.map((w, i) => {
    const b = { x0: cursor, x1: cursor + w, h: heights[i] };
    cursor += w + 6;
    return b;
  });

  blds.forEach((b, i) => {
    const w = b.x1 - b.x0;
    const top = G + b.h;
    const unfinished = UNFINISHED.includes(i);
    const solidTop = unfinished ? G + Math.round(b.h * 0.7) : top;

    shell(b.x0, b.x1, G, solidTop, 14, -14);
    windows(b.x0 + 8, b.x1 - 3, G + 12, solidTop - 8, 14.5, 8, 10, 0.85, 0.1, 1);
    for (let y = G + 30; y < solidTop - 2; y += 30) {
      line(b.x0, y, 14, b.x1, y, 14, 5, 3.6, 0.8);
    }

    if (STEPPED.includes(i)) {
      const t1 = top + 40;
      const t2 = t1 + 26;
      shell(b.x0 + 10, b.x1 - 10, top, t1, 9, -9);
      windows(b.x0 + 16, b.x1 - 12, top + 8, t1 - 6, 9.5, 8, 10, 0.85, 0.1, 1);
      shell(b.x0 + 24, b.x1 - 24, t1, t2, 5, -5);
    }

    if (SPIRES.includes(i)) {
      const cx = (b.x0 + b.x1) / 2;
      line(cx, top, 0, cx, top + 60, 0, 3, 4);
      add(cx, top + 63, 0, 11, KIND_BLINK, 0, 0, 0, 1.3); // aviation beacon
    }

    if (unfinished) {
      for (let c = 0; c <= 4; c++) {
        const x = b.x0 + (w * c) / 4;
        line(x, solidTop, 14, x, top, 14, 4, 3.8);
        line(x, solidTop, -14, x, top, -14, 4, 3.8);
        line(x, top, 14, x, top + 16, 14, 3, 2.8, 0.9); // exposed rebar
      }
      for (let y = solidTop + 22; y <= top; y += 22) {
        line(b.x0, y, 14, b.x1, y, 14, 5, 3.8);
        line(b.x0, y, -14, b.x1, y, -14, 5, 3.8);
      }
      scaffold(b.x0 - 6, b.x1 + 6, solidTop - 40, top + 10, 24);
    }
  });

  // Skybridge between two towers, over the lower building between them.
  {
    const bx0 = blds[5].x1;
    const bx1 = blds[7].x0;
    for (const z of [14, -14]) {
      line(bx0, G + 232, z, bx1, G + 232, z, 4, 4);
      line(bx0, G + 246, z, bx1, G + 246, z, 4, 4);
    }
    for (let x = bx0 + 10; x < bx1; x += 12) line(x, G + 232, 14, x, G + 246, 14, 4, 3.4);
  }

  // ---- Tower cranes (rotating jib, moving hook) ----------------------------
  const makeCrane = (cx: number, mastTop: number, phase: number, trolley: number) => {
    const ci = cranes.length;
    const z = 40;
    const pivotY = mastTop + 30;
    cranes.push({ x: cx, y: pivotY, z, phase });

    // Lattice mast (static)
    line(cx - 5, G, z, cx - 5, pivotY, z, 4, 4.4);
    line(cx + 5, G, z, cx + 5, pivotY, z, 4, 4.4);
    for (let k = 0; G + k * 10 < pivotY; k++) {
      const y0 = G + k * 10;
      const up = Math.min(10, pivotY - y0);
      const fromX = k % 2 === 0 ? cx - 5 : cx + 5;
      const toX = k % 2 === 0 ? cx + 5 : cx - 5;
      line(fromX, y0, z, toX, y0 + up, z, 4, 3.8);
    }
    line(cx, pivotY, z, cx, pivotY + 26, z, 3, 4.4); // tower head

    // Jib + counter-jib lattice (rotates)
    for (let lx = -55; lx <= 150; lx += 3) {
      add(cx + lx, pivotY, z, 4.6, KIND_JIB, ci, lx, 0);
      add(cx + lx, pivotY + 10, z, 4.6, KIND_JIB, ci, lx, 10);
    }
    for (let seg = 0; seg < 20; seg++) {
      const lx0 = -55 + seg * 10;
      for (let u = 1; u <= 3; u++) {
        const f = u / 4;
        const ly = seg % 2 === 0 ? 10 * f : 10 - 10 * f;
        add(cx + lx0 + 10 * f, pivotY + ly, z, 3.6, KIND_JIB, ci, lx0 + 10 * f, ly);
      }
    }
    // Counterweight block
    for (let lx = -52; lx <= -40; lx += 3) {
      for (let ly = -14; ly <= -2; ly += 3) {
        add(cx + lx, pivotY + ly, z, 5.4, KIND_JIB, ci, lx, ly);
      }
    }
    // Hoist cable + hook (length oscillates)
    for (let q = 0; q <= 20; q++) {
      add(cx + trolley, pivotY - q * 3, z, 3.4, KIND_CABLE, ci, trolley, q / 20);
    }
    for (let q = 1; q <= 8; q++) {
      add(cx + trolley, pivotY - 60, z, 5.6, KIND_CABLE, ci, trolley, 1 + q * 0.012);
    }
  };

  makeCrane((blds[4].x0 + blds[4].x1) / 2, G + blds[4].h + 55, 0.6, 100);
  makeCrane((blds[11].x0 + blds[11].x1) / 2, G + blds[11].h + 55, 3.4, 85);

  // ---- Ground plane --------------------------------------------------------
  for (const z of [-40, 30, 55, 80, 105, 130]) {
    for (let x = -900; x <= 900; x += 9) add(x, G, z, 3.8, KIND_STATIC, 0, 0, 0, 0.7);
  }
  line(-900, G, 14, 900, G, 14, 6, 4.2, 0.8);

  // ---- Fainter back row of buildings (depth) -------------------------------
  blds.forEach((b) => {
    const hh = Math.round(b.h * 0.78 + rand() * 30);
    const x0 = b.x0 + 22;
    const x1 = b.x1 + 14;
    const top = G + hh;
    const z = -130;
    line(x0, G, z, x0, top, z, 6, 4, 0.5);
    line(x1, G, z, x1, top, z, 6, 4, 0.5);
    line(x0, top, z, x1, top, z, 6, 4, 0.5);
    windows(x0 + 8, x1 - 4, G + 14, top - 8, z, 11, 13, 0.7, 0.06, 0.5);
  });

  // ---- Remaining particles: floating site-data motes -----------------------
  while (n < N) {
    add(-760 + rand() * 1520, G + 40 + rand() * 385, -160 + rand() * 220, 3, KIND_MOTE, rand() * 6.28, 0, 0, 0.45);
  }

  return { base, size, kind, aux, dim, cranes };
}

export const HeroParticleWave: React.FC = () => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    function getDims() {
      const w = container?.clientWidth || window.innerWidth || 1440;
      const h = container?.clientHeight || window.innerHeight || 500;
      return { w: Math.max(w, 320), h: Math.max(h, 240) };
    }

    let dims = getDims();

    // Scene & Camera setup (unchanged)
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(46, dims.w / dims.h, 1, 3500);
    camera.position.set(0, 190, 560);
    camera.lookAt(0, -35, 0);

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
    domElement.style.zIndex = '1';
    container.appendChild(domElement);

    // Same 16,560 particles, now laid out as a construction skyline
    const sky = buildSkyline();
    const N = NUM_PARTICLES;
    const positions = new Float32Array(sky.base);
    const scales = new Float32Array(sky.size);
    const colors = new Float32Array(N * 3);

    const colorDeep = new THREE.Color(USE_BLUE ? 0x1e40af : 0x15803d);
    const colorVivid = new THREE.Color(USE_BLUE ? 0x2563eb : 0x16a34a);
    const colorBright = new THREE.Color(USE_BLUE ? 0x60a5fa : 0x22c55e);
    const colorTeal = new THREE.Color(USE_BLUE ? 0x22d3ee : 0x0d9488);

    for (let p = 0; p < N; p++) {
      const i3 = p * 3;
      const ratioX = Math.min(1, Math.max(0, (sky.base[i3] + 760) / 1520));
      const ratioY = Math.min(1, Math.max(0, (sky.base[i3 + 1] - GROUND_Y) / 420));
      const mixed = colorVivid
        .clone()
        .lerp(colorBright, Math.sin(ratioX * Math.PI) * 0.85)
        .lerp(colorTeal, ratioY * 0.45)
        .lerp(colorDeep, (1.0 - Math.sin(ratioX * Math.PI)) * 0.35);
      mixed.multiplyScalar(sky.dim[p]);
      colors[i3] = mixed.r;
      colors[i3 + 1] = mixed.g;
      colors[i3 + 2] = mixed.b;
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('scale', new THREE.BufferAttribute(scales, 1));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

    // Shaders, additive blending: unchanged
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
    particles.frustumCulled = false;
    scene.add(particles);

    // Widen the skyline on very wide screens, narrow it on phones
    function applyFit() {
      const aspect = dims.w / dims.h;
      particles.scale.x = Math.min(1.5, Math.max(0.5, aspect / 2.67));
    }
    applyFit();

    // Mouse Interaction & Lerp (same smoothing as before)
    let mouseX = 0;
    let mouseY = 0;
    let targetMouseX = 0;
    let targetMouseY = 0;
    let hover = 0;
    let hoverTarget = 0;

    function onPointerMove(event: PointerEvent) {
      if (!container) return;
      const rect = container.getBoundingClientRect();
      const relX = event.clientX - (rect.left + rect.width / 2);
      const relY = event.clientY - (rect.top + rect.height / 2);
      targetMouseX = relX * 0.7;
      targetMouseY = relY * 0.5;
      hoverTarget =
        event.clientX >= rect.left &&
        event.clientX <= rect.right &&
        event.clientY >= rect.top &&
        event.clientY <= rect.bottom
          ? 1
          : 0;
    }

    window.addEventListener('pointermove', onPointerMove, { passive: true });

    function onWindowResize() {
      dims = getDims();
      camera.aspect = dims.w / dims.h;
      camera.updateProjectionMatrix();
      renderer.setSize(dims.w, dims.h);
      applyFit();
    }

    window.addEventListener('resize', onWindowResize);

    const cranes = sky.cranes;
    const craneCos = new Float32Array(cranes.length);
    const craneSin = new Float32Array(cranes.length);
    const craneLen = new Float32Array(cranes.length);
    const RIPPLE_RADIUS = 230;

    let animationFrameId: number;
    let count = 0;

    function animate() {
      animationFrameId = requestAnimationFrame(animate);

      mouseX += (targetMouseX - mouseX) * 0.06;
      mouseY += (targetMouseY - mouseY) * 0.06;
      hover += (hoverTarget - hover) * 0.08;

      camera.position.x += (mouseX * PARALLAX - camera.position.x) * 0.04;
      camera.position.y += (-mouseY * PARALLAX + 190 - camera.position.y) * 0.04;
      camera.lookAt(0, -35, 0);

      // Cursor position in skyline space
      const unitsPerPx = 545 / dims.h;
      const cursorX = ((mouseX / 0.7) * unitsPerPx) / particles.scale.x;
      const cursorY = -83 - (mouseY / 0.5) * unitsPerPx;

      for (let c = 0; c < cranes.length; c++) {
        const theta = count * 0.12 + cranes[c].phase;
        craneCos[c] = Math.cos(theta);
        craneSin[c] = Math.sin(theta);
        craneLen[c] = 46 + 44 * (0.5 + 0.5 * Math.sin(count * 0.55 + cranes[c].phase * 2));
      }

      const posAttr = particles.geometry.attributes.position;
      const scaleAttr = particles.geometry.attributes.scale;
      const posArray = posAttr.array as Float32Array;
      const scaleArray = scaleAttr.array as Float32Array;
      const { base, size, kind, aux } = sky;

      for (let p = 0; p < N; p++) {
        const i3 = p * 3;
        let x = base[i3];
        let y = base[i3 + 1];
        let z = base[i3 + 2];
        let s = size[p];
        const k = kind[p];

        if (k === KIND_JIB || k === KIND_CABLE) {
          const ci = aux[i3] | 0;
          const cr = cranes[ci];
          const lx = aux[i3 + 1];
          x = cr.x + lx * craneCos[ci];
          z = cr.z + lx * craneSin[ci];
          y = k === KIND_JIB ? cr.y + aux[i3 + 2] : cr.y - aux[i3 + 2] * craneLen[ci];
        } else if (k === KIND_BLINK) {
          s *= 0.75 + 0.55 * Math.sin(count * 1.6 + aux[i3]);
        } else if (k === KIND_MOTE) {
          const ph = aux[i3];
          y += Math.sin(count * 0.5 + ph) * 7;
          x += Math.cos(count * 0.35 + ph) * 5;
        }

        // Cursor ripple: nearby particles are pushed outward and pulse in depth
        if (hover > 0.01) {
          const dx = x - cursorX;
          const dy = y - cursorY;
          const d = Math.sqrt(dx * dx + dy * dy);
          const f = Math.max(0, (RIPPLE_RADIUS - d) / RIPPLE_RADIUS) * hover;
          if (f > 0) {
            const push = f * f * 30;
            const inv = 1 / (d + 0.001);
            x += dx * inv * push;
            y += dy * inv * push;
            z += Math.sin(d * 0.035 - count * 1.4) * f * 34;
            s += f * 5;
          }
        }

        posArray[i3] = x;
        posArray[i3 + 1] = y;
        posArray[i3 + 2] = z;
        scaleArray[p] = s;
      }

      posAttr.needsUpdate = true;
      scaleAttr.needsUpdate = true;

      count += 0.035;
      renderer.render(scene, camera);
    }

    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('pointermove', onPointerMove);
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
      className="relative w-full h-[460px] lg:h-[540px] overflow-hidden z-10"
    >
      {/* Interactive HUD Floating Badges on Particle Stream */}
      <div className="absolute bottom-20 left-6 lg:left-16 z-20 hidden md:flex items-center gap-3 bg-inverse-surface/85 border border-secondary-fixed/40 px-4 py-2 rounded-xl backdrop-blur-md shadow-xl">
        <span className="w-2.5 h-2.5 rounded-full bg-secondary-fixed animate-ping"></span>
        <div className="flex flex-col">
          <span className="font-label-sm text-[11px] text-secondary-fixed font-bold uppercase tracking-wider">
            Site Telemetry Mesh
          </span>
          <span className="font-body-sm text-[12px] text-inverse-on-surface">
            Interactive 3D Construction Skyline
          </span>
        </div>
      </div>

      <div className="absolute bottom-20 right-6 lg:right-16 z-20 hidden md:flex items-center gap-3 bg-inverse-surface/85 border border-outline/30 px-4 py-2 rounded-xl backdrop-blur-md">
        <span className="material-symbols-outlined text-secondary-fixed text-[18px]">stream</span>
        <span className="font-label-sm text-[12px] text-secondary-fixed font-mono font-medium">
          LIDAR SITE TELEMETRY • 16,560 VERTICES
        </span>
      </div>

      <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/20 via-transparent to-transparent pointer-events-none"></div>
    </div>
  );
};
