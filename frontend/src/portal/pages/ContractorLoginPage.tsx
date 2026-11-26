import React from 'react';
import { RoleLoginContainer } from '../components/RoleLoginContainer';

export const ContractorLoginPage: React.FC = () => {
  return (
    <RoleLoginContainer
      role="contractor"
      roleDisplayName="Contractor"
      roleTag="Worksite Trade Crews & Site Engineers"
      defaultDemoEmail="contractor@nirmaaniq.com"
      pendingDemoEmail="approved.contractor@nirmaaniq.com"
      accentColorClass="text-secondary"
    />
  );
};
