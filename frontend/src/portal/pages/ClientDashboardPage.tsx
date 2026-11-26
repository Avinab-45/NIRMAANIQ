import React from 'react';
import { DashboardPlaceholder } from '../components/DashboardPlaceholder';

export const ClientDashboardPage: React.FC = () => {
  return (
    <DashboardPlaceholder
      role="client"
      roleTitle="Client"
      roleIcon="real_estate_agent"
      badgeBg="bg-tertiary-fixed text-on-tertiary-fixed-variant"
      badgeText="CLIENT // STAKEHOLDER PORTAL"
      accentColor="bg-tertiary-fixed text-on-tertiary-fixed-variant"
      description="Infrastructure owner and investor console for commissioning new builds, tracking 3D digital twin progress, and verifying statutory RERA escrow compliance."
      upcomingFeatures={[
        'Submit new construction project scopes, locations, and budgets',
        'Review proposal status and contractor tenders from organisations',
        'Inspect live 3D milestone progress walkthroughs & drone flythroughs',
        'Statutory RERA escrow milestone certifications & builder updates',
      ]}
    />
  );
};
