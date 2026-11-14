import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

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

    // Scene & Camera setup
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

    // Particle Grid Parameters matching KPIT reference
    const SEPARATION = 16;
    const AMOUNTX = 180;
    const AMOUNTY = 92;
    const numParticles = AMOUNTX * AMOUNTY;

    const positions = new Float32Array(numParticles * 3);
    const scales = new Float32Array(numParticles);
    const colors = new Float32Array(numParticles * 3);

    // High-contrast vivid emerald green and cyan dots
    const colorDeepGreen = new THREE.Color(0x15803d); // Deep forest emerald green
    const colorVividNeon = new THREE.Color(0x16a34a); // Rich punchy green
    const colorBrightLime = new THREE.Color(0x22c55e); // Signature KPIT vibrant green
    const colorDeepTeal = new THREE.Color(0x0d9488); // Solid deep teal-cyan

    let i = 0;
    let j = 0;
    for (let ix = 0; ix < AMOUNTX; ix++) {
      for (let iy = 0; iy < AMOUNTY; iy++) {
        const posX = ix * SEPARATION - (AMOUNTX * SEPARATION) / 2;
        const posZ = iy * SEPARATION - (AMOUNTY * SEPARATION) / 2;
        const posY = 0;

        positions[i] = posX;
        positions[i + 1] = posY;
        positions[i + 2] = posZ;

        scales[j] = 1.0;

        const ratioX = ix / AMOUNTX;
        const ratioY = iy / AMOUNTY;

        const mixed = colorVividNeon
          .clone()
          .lerp(colorBrightLime, Math.sin(ratioX * Math.PI) * 0.85)
          .lerp(colorDeepTeal, ratioY * 0.45)
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

    // Mouse Interaction & Lerp
    let mouseX = 0;
    let mouseY = 0;
    let targetMouseX = 0;
    let targetMouseY = 0;

    function onPointerMove(event: PointerEvent) {
      if (!container) return;
      const rect = container.getBoundingClientRect();
      const relX = event.clientX - (rect.left + rect.width / 2);
      const relY = event.clientY - (rect.top + rect.height / 2);
      targetMouseX = relX * 0.7;
      targetMouseY = relY * 0.5;
    }

    window.addEventListener('pointermove', onPointerMove, { passive: true });

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

      mouseX += (targetMouseX - mouseX) * 0.06;
      mouseY += (targetMouseY - mouseY) * 0.06;

      camera.position.x += (mouseX - camera.position.x) * 0.04;
      camera.position.y += (-mouseY + 190 - camera.position.y) * 0.04;
      camera.lookAt(0, -35, 0);

      const posAttr = particles.geometry.attributes.position;
      const scaleAttr = particles.geometry.attributes.scale;
      const posArray = posAttr.array as Float32Array;
      const scaleArray = scaleAttr.array as Float32Array;

      let idx = 0;
      let sIdx = 0;

      for (let ix = 0; ix < AMOUNTX; ix++) {
        for (let iy = 0; iy < AMOUNTY; iy++) {
          const wave1 = Math.sin((ix + count) * 0.22) * 46;
          const wave2 = Math.sin((iy + count) * 0.34) * 46;
          const wave3 = Math.cos((ix + iy + count * 0.55) * 0.14) * 32;

          const pX = posArray[idx];
          const pZ = posArray[idx + 2];
          const dist = Math.sqrt(Math.pow(pX - mouseX, 2) + Math.pow(pZ - mouseY * 1.3, 2));
          const cursorDistort = Math.sin(dist * 0.018 - count * 1.1) * Math.max(0, (320 - dist) * 0.22);

          posArray[idx + 1] = wave1 + wave2 + wave3 + cursorDistort - 75;

          scaleArray[sIdx] =
            (Math.sin((ix + count) * 0.22) + 1) * 7.5 +
            (Math.sin((iy + count) * 0.34) + 1) * 7.5 +
            5.5;

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
            KPIT Mesh Dynamics
          </span>
          <span className="font-body-sm text-[12px] text-inverse-on-surface">
            Interactive 3D Undulating Waveform
          </span>
        </div>
      </div>

      <div className="absolute bottom-20 right-6 lg:right-16 z-20 hidden md:flex items-center gap-3 bg-inverse-surface/85 border border-outline/30 px-4 py-2 rounded-xl backdrop-blur-md">
        <span className="material-symbols-outlined text-secondary-fixed text-[18px]">stream</span>
        <span className="font-label-sm text-[12px] text-secondary-fixed font-mono font-medium">
          EMERALD &amp; CYAN TELEMETRY • 16,560 VERTICES
        </span>
      </div>

      <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/20 via-transparent to-transparent pointer-events-none"></div>
    </div>
  );
};
