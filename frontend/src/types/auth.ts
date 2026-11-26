export type UserRole = 'organisation' | 'contractor' | 'client';

export type ApprovalStatus = 'pending' | 'approved' | 'rejected';

export interface PortalUser {
  id: string;
  role: UserRole;
  email: string;
  name: string;
  phone?: string;
  address?: string;
  approvalStatus: ApprovalStatus;
  createdAt: string;
}

export interface OrganisationRegistrationData {
  organisationName: string;
  email: string;
  password: string;
  agreeToTerms: boolean;
}

export interface ContractorRegistrationData {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  agreeToTerms: boolean;
}

export interface ClientRegistrationData {
  fullName: string;
  email: string;
  address: string;
  password: string;
  agreeToTerms: boolean;
}

export interface RoleInfo {
  role: UserRole;
  title: string;
  subtitle: string;
  description: string;
  icon: string;
  badgeText: string;
  badgeColorClass: string;
  accentColor: string;
  capabilities: string[];
}
