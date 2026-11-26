import React from 'react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full bg-surface-container-low mt-auto">
      <div className="w-full max-w-7xl mx-auto px-6 lg:px-12 py-12">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-8 mb-12">
          <div className="md:col-span-2">
            <Link to="/" className="flex items-center gap-3 mb-4">
              <img
                alt="NirmaanIQ Logo"
                className="h-7 w-auto object-contain"
                src="https://lh3.googleusercontent.com/aida/ALJJyrBymwrci1_JVMf7mCezzMOqmZtpZ9eGkBO_6QuHL5Ry-7bY0c2ccSROahcSDaEkfpoNZrV2ul5r2eJ7CO-LkANbILtIqT-YgVa8vUzJeo3kfm4IulxlZjsxuWde_9f_mDO2NGBVp0T6TCCgGnYBfTVEsdVpotnNxaLgd75yhJWw4mi3h3kUN0urpFX0c5AU8UOJf-DzIYX-a_N1OkYH6PBwY-3MBqXiW0J6eklJN09b"
              />
              <span className="font-headline-sm text-headline-sm text-on-surface font-bold">
                Nirmaan<span className="text-primary">IQ</span>
              </span>
            </Link>
            <p className="font-body-sm text-body-sm text-on-surface-variant max-w-sm mb-4 leading-relaxed">
              Industrial-grade construction intelligence, combining civil engineering rigor with real-time predictive machine vision across multi-state infrastructure portfolios.
            </p>
            <div className="flex items-center gap-2 text-on-surface-variant font-label-sm text-label-sm">
              <span className="inline-block w-2 h-2 rounded-full bg-secondary"></span>
              <span>ISO 27001 Certified • RERA Compliant Core</span>
            </div>
          </div>

          <div>
            <h4 className="font-label-sm text-label-sm text-on-surface uppercase tracking-wider mb-4">
              Platform
            </h4>
            <ul className="space-y-2.5 font-body-sm text-body-sm text-on-surface-variant">
              <li>
                <a className="hover:text-primary transition-colors" data-path="platform-capabilities" href="#pillars">
                  Site Telemetry
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="ai-intelligence" href="#interactive-preview">
                  Predictive Risk Model
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="solutions" href="#evolution-showcase-section">
                  BIM 4D Digital Twin
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="solutions" href="#solutions">
                  Automated Daily Logs
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-label-sm text-label-sm text-on-surface uppercase tracking-wider mb-4">
              Solutions
            </h4>
            <ul className="space-y-2.5 font-body-sm text-body-sm text-on-surface-variant">
              <li>
                <a className="hover:text-primary transition-colors" data-path="enterprise" href="#evolution-showcase-section">
                  Enterprise Builders
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="enterprise" href="#pillars">
                  General Contractors
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="enterprise" href="#solutions">
                  Urban Metros &amp; Rail
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="customers" href="#testimonials">
                  Case Studies
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-label-sm text-label-sm text-on-surface uppercase tracking-wider mb-4">
              Security &amp; System
            </h4>
            <ul className="space-y-2.5 font-body-sm text-body-sm text-on-surface-variant">
              <li>
                <Link className="hover:text-primary transition-colors" data-path="sign-in" to="/portal/select-role">
                  Engineer Portal
                </Link>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="enterprise" href="#evolution-showcase-section">
                  Audit &amp; Compliance
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="enterprise" href="#solutions">
                  API Documentation
                </a>
              </li>
              <li>
                <a className="hover:text-primary transition-colors" data-path="enterprise" href="#solutions">
                  Cloud Infrastructure
                </a>
              </li>
            </ul>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between pt-6 border-t border-outline-variant/30 text-on-surface-variant font-label-sm text-label-sm">
          <div>© 2025 NirmaanIQ Technologies India Pvt. Ltd. All rights reserved.</div>
          <div className="flex items-center gap-6 mt-4 sm:mt-0">
            <a className="hover:text-on-surface" href="#privacy">
              Privacy Architecture
            </a>
            <a className="hover:text-on-surface" href="#terms">
              Terms of Service
            </a>
            <a className="hover:text-on-surface" href="#status">
              System Status
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
};
