import React from 'react';
import { RoleLoginContainer } from '../components/RoleLoginContainer';

export const ClientLoginPage: React.FC = () => {
  return (
    <RoleLoginContainer
      role="client"
      roleDisplayName="Client"
      roleTag="Infrastructure Stakeholders & Asset Owners"
      defaultDemoEmail="client@nirmaaniq.com"
      accentColorClass="text-tertiary"
    />
  );
};
