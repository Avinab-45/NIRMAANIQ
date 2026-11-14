import { PortalUser, UserRole, OrganisationRegistrationData, ContractorRegistrationData, ClientRegistrationData } from '../../types/auth';

const STORAGE_USERS_KEY = 'nirmaaniq_portal_users';
const STORAGE_CURRENT_USER_KEY = 'nirmaaniq_current_user';
const STORAGE_ACTIVE_OTP_KEY = 'nirmaaniq_active_otp';

// Pre-seeded mock accounts for seamless demonstration
const INITIAL_MOCK_USERS: PortalUser[] = [
  {
    id: 'user-org-1',
    role: 'organisation',
    email: 'org@nirmaaniq.com',
    name: 'Godrej Infra Builders Ltd.',
    approvalStatus: 'approved',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'user-org-2',
    role: 'organisation',
    email: 'pending.org@nirmaaniq.com',
    name: 'L&T Civil Horizon JV',
    approvalStatus: 'pending',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'user-contractor-1',
    role: 'contractor',
    email: 'contractor@nirmaaniq.com',
    name: 'Rajesh Sharma (Apex Concrete Works)',
    phone: '+91 98765 43210',
    approvalStatus: 'pending',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'user-contractor-2',
    role: 'contractor',
    email: 'approved.contractor@nirmaaniq.com',
    name: 'Sunil Verma (Verma Electrical & MEP)',
    phone: '+91 91234 56789',
    approvalStatus: 'approved',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'user-client-1',
    role: 'client',
    email: 'client@nirmaaniq.com',
    name: 'Vikramaditya Oberoi',
    address: 'Plot 42, Bandra-Kurla Complex, Mumbai, Maharashtra',
    approvalStatus: 'approved',
    createdAt: new Date().toISOString(),
  },
];

class AuthService {
  private getUsers(): PortalUser[] {
    try {
      const stored = localStorage.getItem(STORAGE_USERS_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (e) {
      console.error('Error reading portal users from localStorage', e);
    }
    this.saveUsers(INITIAL_MOCK_USERS);
    return INITIAL_MOCK_USERS;
  }

  private saveUsers(users: PortalUser[]): void {
    try {
      localStorage.setItem(STORAGE_USERS_KEY, JSON.stringify(users));
    } catch (e) {
      console.error('Error saving portal users to localStorage', e);
    }
  }

  public getCurrentUser(): PortalUser | null {
    try {
      const stored = localStorage.getItem(STORAGE_CURRENT_USER_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (e) {
      console.error('Error reading current user', e);
    }
    return null;
  }

  public setCurrentUser(user: PortalUser | null): void {
    if (user) {
      localStorage.setItem(STORAGE_CURRENT_USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_CURRENT_USER_KEY);
    }
  }

  public findUserByEmailAndRole(email: string, role: UserRole): PortalUser | undefined {
    const users = this.getUsers();
    return users.find(u => u.email.toLowerCase() === email.trim().toLowerCase() && u.role === role);
  }

  public registerOrganisation(data: OrganisationRegistrationData): { success: boolean; user?: PortalUser; error?: string } {
    const users = this.getUsers();
    const existing = users.find(u => u.email.toLowerCase() === data.email.trim().toLowerCase() && u.role === 'organisation');
    if (existing) {
      return { success: false, error: 'An organisation with this email is already registered.' };
    }

    const newUser: PortalUser = {
      id: `org-${Date.now()}`,
      role: 'organisation',
      email: data.email.trim().toLowerCase(),
      name: data.organisationName.trim(),
      approvalStatus: 'pending', // Per requirements: Organisation registrations have a "Pending Approval" status
      createdAt: new Date().toISOString(),
    };

    users.push(newUser);
    this.saveUsers(users);
    return { success: true, user: newUser };
  }

  public registerContractor(data: ContractorRegistrationData): { success: boolean; user?: PortalUser; error?: string } {
    const users = this.getUsers();
    const existing = users.find(u => u.email.toLowerCase() === data.email.trim().toLowerCase() && u.role === 'contractor');
    if (existing) {
      return { success: false, error: 'A contractor with this email is already registered.' };
    }

    const newUser: PortalUser = {
      id: `cnt-${Date.now()}`,
      role: 'contractor',
      email: data.email.trim().toLowerCase(),
      name: data.fullName.trim(),
      phone: data.mobile.trim(),
      approvalStatus: 'pending', // Per requirements: Contractor registrations have a "Pending Approval" status
      createdAt: new Date().toISOString(),
    };

    users.push(newUser);
    this.saveUsers(users);
    return { success: true, user: newUser };
  }

  public registerClient(data: ClientRegistrationData): { success: boolean; user?: PortalUser; error?: string } {
    const users = this.getUsers();
    const existing = users.find(u => u.email.toLowerCase() === data.email.trim().toLowerCase() && u.role === 'client');
    if (existing) {
      return { success: false, error: 'A client account with this email is already registered.' };
    }

    const newUser: PortalUser = {
      id: `cli-${Date.now()}`,
      role: 'client',
      email: data.email.trim().toLowerCase(),
      name: data.fullName.trim(),
      address: data.address.trim(),
      approvalStatus: 'approved', // Clients have direct access per requirements
      createdAt: new Date().toISOString(),
    };

    users.push(newUser);
    this.saveUsers(users);
    return { success: true, user: newUser };
  }

  // Generate Demo OTP
  public requestOtp(email: string, role: UserRole): { success: boolean; otp: string; user?: PortalUser; error?: string } {
    const user = this.findUserByEmailAndRole(email, role);
    if (!user) {
      return {
        success: false,
        otp: '',
        error: `No registered ${role} found with this email. Please register first.`,
      };
    }

    // Standard demo OTP is 123456 or a valid 6-digit code
    const demoOtp = '123456';
    const otpData = {
      email: email.trim().toLowerCase(),
      role,
      otp: demoOtp,
      expiresAt: Date.now() + 5 * 60 * 1000, // 5 minutes
    };
    localStorage.setItem(STORAGE_ACTIVE_OTP_KEY, JSON.stringify(otpData));

    return {
      success: true,
      otp: demoOtp,
      user,
    };
  }

  public verifyOtp(email: string, role: UserRole, enteredOtp: string): { success: boolean; user?: PortalUser; error?: string } {
    const cleanEmail = email.trim().toLowerCase();
    const cleanOtp = enteredOtp.trim();

    // Verify user exists
    const user = this.findUserByEmailAndRole(cleanEmail, role);
    if (!user) {
      return { success: false, error: 'User record not found.' };
    }

    // Check OTP
    const storedOtpRaw = localStorage.getItem(STORAGE_ACTIVE_OTP_KEY);
    let validOtp = '123456'; // Default demo OTP always accepted in development mode
    if (storedOtpRaw) {
      try {
        const parsed = JSON.parse(storedOtpRaw);
        if (parsed.email === cleanEmail && parsed.role === role) {
          validOtp = parsed.otp;
        }
      } catch (e) {
        console.error(e);
      }
    }

    if (cleanOtp !== validOtp && cleanOtp !== '123456') {
      return { success: false, error: 'Incorrect verification code. Please check your code or use demo OTP 123456.' };
    }

    // Set current user
    this.setCurrentUser(user);
    return { success: true, user };
  }

  // Helper for demo testing to toggle approval status of pending organisations and contractors
  public toggleApprovalStatus(userId: string): PortalUser | null {
    const users = this.getUsers();
    const userIndex = users.findIndex(u => u.id === userId);
    if (userIndex === -1) return null;

    const currentStatus = users[userIndex].approvalStatus;
    const newStatus = currentStatus === 'approved' ? 'pending' : 'approved';
    users[userIndex].approvalStatus = newStatus;
    this.saveUsers(users);

    const currentUser = this.getCurrentUser();
    if (currentUser && currentUser.id === userId) {
      currentUser.approvalStatus = newStatus;
      this.setCurrentUser(currentUser);
    }

    return users[userIndex];
  }

  public logout(): void {
    this.setCurrentUser(null);
    localStorage.removeItem(STORAGE_ACTIVE_OTP_KEY);
  }
}

export const authService = new AuthService();
