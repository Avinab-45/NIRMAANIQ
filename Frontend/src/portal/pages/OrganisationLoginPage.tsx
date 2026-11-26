import React from 'react';
import { RoleLoginContainer } from '../components/RoleLoginContainer';

export const OrganisationLoginPage: React.FC = () => {
  return (
    <RoleLoginContainer
      role="organisation"
      roleDisplayName="Organisation"
      roleTag="Enterprise Builders & EPC General Contractors"
      defaultDemoEmail="org@nirmaaniq.com"
      pendingDemoEmail="pending.org@nirmaaniq.com"
      accentColorClass="text-primary"
    />
  );
};
