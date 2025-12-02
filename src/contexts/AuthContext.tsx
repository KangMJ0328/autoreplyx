import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authAPI, User } from '../lib/api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
  refreshUser: () => Promise<void>;
}

interface RegisterData {
  email: string;
  password: string;
  password_confirmation: string;
  brand_name: string;
  industry: string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 초기 로드 시 사용자 정보 확인
  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('access_token');

      if (token) {
        try {
          const userData = await authAPI.me();
          setUser(userData);
        } catch (error) {
          // 토큰이 유효하지 않으면 삭제
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
        }
      }

      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const response = await authAPI.login(email, password);
    setUser(response.user);
  };

  const register = async (data: RegisterData) => {
    const response = await authAPI.register(data);
    setUser(response.user);
  };

  const logout = async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      // 에러 무시
    }
    setUser(null);
  };

  const updateUser = (userData: User) => {
    setUser(userData);
  };

  const refreshUser = async () => {
    try {
      const userData = await authAPI.me();
      setUser(userData);
    } catch (error) {
      // 에러 시 무시
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        updateUser,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
