import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './portal/context/AuthContext';
import { LandingPage } from './pages/LandingPage';
import { RoleSelectionPage } from './portal/pages/RoleSelectionPage';

import { OrganisationRegisterPage } from './portal/pages/OrganisationRegisterPage';
import { OrganisationLoginPage } from './portal/pages/OrganisationLoginPage';
import { OrganisationDashboardPage } from './portal/pages/OrganisationDashboardPage';

import { ContractorRegisterPage } from './portal/pages/ContractorRegisterPage';
import { ContractorLoginPage } from './portal/pages/ContractorLoginPage';
import { ContractorDashboardPage } from './portal/pages/ContractorDashboardPage';

import { ClientRegisterPage } from './portal/pages/ClientRegisterPage';
import { ClientLoginPage } from './portal/pages/ClientLoginPage';
import { ClientDashboardPage } from './portal/pages/ClientDashboardPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Main Landing / Marketing Website (Preserved completely) */}
          <Route path="/" element={<LandingPage />} />

          {/* Site Portal Role Selection */}
          <Route path="/portal/select-role" element={<RoleSelectionPage />} />

          {/* Organisation Routes */}
          <Route path="/portal/organisation/register" element={<OrganisationRegisterPage />} />
          <Route path="/portal/organisation/login" element={<OrganisationLoginPage />} />
          <Route path="/portal/organisation/dashboard" element={<OrganisationDashboardPage />} />

          {/* Contractor Routes */}
          <Route path="/portal/contractor/register" element={<ContractorRegisterPage />} />
          <Route path="/portal/contractor/login" element={<ContractorLoginPage />} />
          <Route path="/portal/contractor/dashboard" element={<ContractorDashboardPage />} />

          {/* Client Routes */}
          <Route path="/portal/client/register" element={<ClientRegisterPage />} />
          <Route path="/portal/client/login" element={<ClientLoginPage />} />
          <Route path="/portal/client/dashboard" element={<ClientDashboardPage />} />

          {/* Catch-all fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
