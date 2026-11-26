import React, { createContext, useContext, useState, useEffect } from 'react';
import { PortalUser } from '../../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  currentUser: PortalUser | null;
  setCurrentUser: (user: PortalUser | null) => void;
  logout: () => void;
  toggleApproval: (userId: string) => PortalUser | null;
  refreshUser: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUserState] = useState<PortalUser | null>(() => authService.getCurrentUser());

  useEffect(() => {
    setCurrentUserState(authService.getCurrentUser());
  }, []);

  const setCurrentUser = (user: PortalUser | null) => {
    authService.setCurrentUser(user);
    setCurrentUserState(user);
  };

  const logout = () => {
    authService.logout();
    setCurrentUserState(null);
  };

  const toggleApproval = (userId: string) => {
    const updated = authService.toggleApprovalStatus(userId);
    if (updated && currentUser && currentUser.id === userId) {
      setCurrentUserState({ ...updated });
    }
    return updated;
  };

  const refreshUser = () => {
    setCurrentUserState(authService.getCurrentUser());
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        setCurrentUser,
        logout,
        toggleApproval,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
