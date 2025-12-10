import axios, { AxiosInstance, AxiosError } from 'axios';

// API 기본 설정
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// Axios 인스턴스 생성
const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: 30000,
});

// 요청 인터셉터: 토큰 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 에러 처리 및 토큰 갱신
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    // 401 에러: 토큰 만료
    if (error.response?.status === 401) {
      // 리프레시 토큰으로 갱신 시도
      const refreshToken = localStorage.getItem('refresh_token');

      if (refreshToken && originalRequest && !originalRequest.headers['X-Retry']) {
        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refresh_token: refreshToken,
          });

          const { access_token } = response.data;
          localStorage.setItem('access_token', access_token);

          // 원래 요청 재시도
          originalRequest.headers.Authorization = `Bearer ${access_token}`;
          originalRequest.headers['X-Retry'] = 'true';
          return api(originalRequest);
        } catch (refreshError) {
          // 갱신 실패: 로그아웃
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          window.location.href = '/login';
        }
      } else {
        // 로그인 페이지로 리다이렉트
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default api;

// ============================================
// 타입 정의
// ============================================

export interface User {
  id: number;
  email: string;
  brand_name: string;
  industry: string;
  business_hours: string | null;
  address: string | null;
  description: string | null;
  contact_email: string | null;
  contact_phone: string | null;
  reservation_slug: string | null;
  ai_enabled: boolean;
  ai_tone: 'professional' | 'friendly' | 'formal' | 'casual';
  banned_words: string[];
  created_at: string;
}

export interface Channel {
  id: number;
  channel_type: 'instagram' | 'kakao' | 'naver';
  account_id: string;
  account_name: string | null;
  webhook_status: 'active' | 'inactive' | 'error';
  token_expires_at: string | null;
  is_active: boolean;
}

export interface AutoRule {
  id: number;
  name: string;
  match_type: 'EXACT' | 'CONTAINS' | 'REGEX';
  keywords: string;
  response_template: string;
  include_reservation_link: boolean;
  include_estimate_link: boolean;
  priority: number;
  channel: string | null;
  cooldown_seconds: number;
  active_hours_start: string | null;
  active_hours_end: string | null;
  is_active: boolean;
  trigger_count: number;
  created_at: string;
}

export interface MessageLog {
  id: number;
  channel: 'instagram' | 'kakao' | 'naver';
  sender_id: string;
  sender_name: string | null;
  received_message: string;
  response_message: string | null;
  response_type: 'rule' | 'ai' | 'manual' | 'none';
  matched_rule_id: number | null;
  ai_tokens_used: number;
  processing_time_ms: number | null;
  created_at: string;
}

export interface Reservation {
  id: number;
  customer_name: string;
  phone: string;
  email: string | null;
  service: string;
  date: string;
  time: string;
  requests: string | null;
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed' | 'no_show';
  source: 'chat' | 'web' | 'manual';
  created_at: string;
}

export interface EstimateRequest {
  id: number;
  customer_name: string;
  phone: string;
  email: string | null;
  service_type: string;
  details: string;
  budget: string | null;
  preferred_date: string | null;
  status: 'pending' | 'quoted' | 'accepted' | 'rejected' | 'completed';
  quoted_amount: number | null;
  created_at: string;
}

export interface Subscription {
  id: number;
  plan: 'free' | 'pro' | 'agency';
  status: 'active' | 'trialing' | 'cancelled' | 'expired' | 'past_due';
  price_monthly: number;
  daily_message_limit: number | null;
  features: Record<string, boolean>;
  trial_ends_at: string | null;
  current_period_end: string | null;
}

export interface DashboardStats {
  today: {
    total_messages: number;
    rule_responses: number;
    ai_responses: number;
    avg_response_time_ms: number;
  };
  monthly: {
    total_messages: number;
    rule_responses: number;
    ai_responses: number;
    ai_tokens_used: number;
    ai_cost_estimate: number;
  };
  by_channel: {
    channel: string;
    message_count: number;
    response_rate: number;
  }[];
  top_rules: {
    rule_id: number;
    rule_name: string;
    trigger_count: number;
  }[];
  daily_trend: {
    date: string;
    message_count: number;
  }[];
}

export interface PaginatedResponse<T> {
  data: T[];
  meta: {
    current_page: number;
    last_page: number;
    per_page: number;
    total: number;
  };
}

export interface ApiError {
  error: string;
  message: string;
  errors?: Record<string, string[]>;
}

// ============================================
// Auth API
// ============================================

export const authAPI = {
  login: async (email: string, password: string) => {
    const response = await api.post<{
      access_token: string;
      refresh_token: string;
      token_type: string;
      expires_in: number;
      user: User;
    }>('/auth/login', { email, password });

    localStorage.setItem('access_token', response.data.access_token);
    localStorage.setItem('refresh_token', response.data.refresh_token);

    return response.data;
  },

  register: async (data: {
    email: string;
    password: string;
    password_confirmation: string;
    brand_name: string;
    industry: string;
  }) => {
    // Convert to backend expected format (camelCase for brandName)
    const requestData = {
      email: data.email,
      password: data.password,
      password_confirmation: data.password_confirmation,
      brandName: data.brand_name,
      industry: data.industry,
    };
    const response = await api.post<{
      access_token: string;
      refresh_token: string;
      user: User;
    }>('/auth/register', requestData);

    localStorage.setItem('access_token', response.data.access_token);
    localStorage.setItem('refresh_token', response.data.refresh_token);

    return response.data;
  },

  logout: async () => {
    await api.post('/auth/logout');
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  },

  me: async () => {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },

  googleLogin: () => {
    window.location.href = `${API_BASE_URL}/auth/google`;
  },

  instagramLogin: () => {
    window.location.href = `${API_BASE_URL}/auth/instagram`;
  },
};

// ============================================
// Channels API
// ============================================

export const channelsAPI = {
  list: async () => {
    const response = await api.get<Channel[]>('/channels');
    return response.data;
  },

  // Instagram OAuth 연동 - auth_url로 리다이렉트
  connectInstagram: async () => {
    const response = await api.post<{ auth_url: string }>('/channels/instagram/connect');
    return response.data;
  },

  // 인스타그램 개발 모드 연동 (Mock)
  mockConnectInstagram: async (username: string) => {
    const response = await api.post('/channels/instagram/mock-connect', { username });
    return response.data;
  },

  // 카카오톡 OAuth 연동 - auth_url로 리다이렉트
  connectKakao: async () => {
    const response = await api.post<{ auth_url: string }>('/channels/kakao/connect');
    return response.data;
  },

  // 네이버 OAuth 연동 - auth_url로 리다이렉트
  connectNaver: async () => {
    const response = await api.post<{ auth_url: string }>('/channels/naver/connect');
    return response.data;
  },

  // 카카오톡 개발 모드 연동 (Mock)
  mockConnectKakao: async (channelId: string) => {
    const response = await api.post('/channels/kakao/mock-connect', { channel_id: channelId });
    return response.data;
  },

  // 네이버 톡톡 개발 모드 연동 (Mock)
  mockConnectNaver: async (talktalkId: string) => {
    const response = await api.post('/channels/naver/mock-connect', { talktalk_id: talktalkId });
    return response.data;
  },

  disconnect: async (channel: string) => {
    await api.delete(`/channels/${channel}`);
  },

  getStatus: async (channel: string) => {
    const response = await api.get<{
      connected: boolean;
      webhook_status: string;
      token_expires_at: string | null;
    }>(`/channels/${channel}/status`);
    return response.data;
  },

  refreshToken: async (channel: string) => {
    const response = await api.post<Channel>(`/channels/${channel}/refresh`);
    return response.data;
  },
};

// ============================================
// Rules API
// ============================================

export const rulesAPI = {
  list: async () => {
    const response = await api.get<AutoRule[]>('/rules');
    return response.data;
  },

  get: async (id: number) => {
    const response = await api.get<AutoRule>(`/rules/${id}`);
    return response.data;
  },

  create: async (data: Omit<AutoRule, 'id' | 'trigger_count' | 'created_at'>) => {
    const response = await api.post<AutoRule>('/rules', data);
    return response.data;
  },

  update: async (id: number, data: Partial<AutoRule>) => {
    const response = await api.put<AutoRule>(`/rules/${id}`, data);
    return response.data;
  },

  delete: async (id: number) => {
    await api.delete(`/rules/${id}`);
  },

  toggleActive: async (id: number) => {
    const response = await api.patch<AutoRule>(`/rules/${id}/toggle`);
    return response.data;
  },

  test: async (message: string, channel: string) => {
    const response = await api.post<{
      matched_rule: AutoRule | null;
      response_type: 'rule' | 'ai' | 'none';
      response_text: string;
      ai_tokens_used?: number;
      cached?: boolean;
    }>('/rules/test', { message, channel });
    return response.data;
  },

  reorder: async (rules: { id: number; priority: number }[]) => {
    const response = await api.post<AutoRule[]>('/rules/reorder', { rules });
    return response.data;
  },
};

// ============================================
// AI API
// ============================================

export const aiAPI = {
  testMessage: async (message: string, channel: string = 'instagram') => {
    const response = await api.post<{
      matched_rule: AutoRule | null;
      response_type: 'rule' | 'ai';
      response_text: string;
      ai_tokens_used: number;
      cached: boolean;
      would_trigger_cooldown: boolean;
    }>('/ai/test-message', { message, channel });
    return response.data;
  },

  generateFaq: async (count: number = 5) => {
    const response = await api.post<{
      faqs: { keyword: string; response: string }[];
      tokens_used: number;
    }>('/ai/generate-faq', { count });
    return response.data;
  },

  generateResponse: async (message: string) => {
    const response = await api.post<{
      response: string;
      tokens_used: number;
      cached: boolean;
    }>('/ai/generate-response', { message });
    return response.data;
  },
};

// ============================================
// Reservations API
// ============================================

export const reservationsAPI = {
  list: async (params?: {
    status?: string;
    date_from?: string;
    date_to?: string;
    page?: number;
    per_page?: number;
  }) => {
    const response = await api.get<PaginatedResponse<Reservation>>('/reservations', { params });
    return response.data;
  },

  get: async (id: number) => {
    const response = await api.get<Reservation>(`/reservations/${id}`);
    return response.data;
  },

  updateStatus: async (id: number, status: Reservation['status']) => {
    const response = await api.patch<Reservation>(`/reservations/${id}/status`, { status });
    return response.data;
  },

  addNote: async (id: number, note: string) => {
    const response = await api.post<Reservation>(`/reservations/${id}/notes`, { note });
    return response.data;
  },

  // 공개 API (예약 페이지용)
  getPublicPage: async (slug: string) => {
    const response = await api.get<{
      brand_name: string;
      title: string;
      description: string;
      fields: { name: string; label: string; type: string; required: boolean; options?: string[] }[];
      available_slots: { date: string; times: string[] }[];
    }>(`/public/reservation/${slug}`);
    return response.data;
  },

  createPublic: async (slug: string, data: {
    customer_name: string;
    phone: string;
    email?: string;
    service: string;
    date: string;
    time: string;
    requests?: string;
  }) => {
    const response = await api.post<{
      message: string;
      reservation_id: number;
    }>(`/public/reservation/${slug}`, data);
    return response.data;
  },
};

// ============================================
// Estimates API
// ============================================

export const estimatesAPI = {
  list: async (params?: {
    status?: string;
    page?: number;
    per_page?: number;
  }) => {
    const response = await api.get<PaginatedResponse<EstimateRequest>>('/estimates', { params });
    return response.data;
  },

  get: async (id: number) => {
    const response = await api.get<EstimateRequest>(`/estimates/${id}`);
    return response.data;
  },

  sendQuote: async (id: number, data: { amount: number; message: string }) => {
    const response = await api.post<EstimateRequest>(`/estimates/${id}/quote`, data);
    return response.data;
  },

  updateStatus: async (id: number, status: EstimateRequest['status']) => {
    const response = await api.patch<EstimateRequest>(`/estimates/${id}/status`, { status });
    return response.data;
  },

  // 공개 API (견적 페이지용)
  createPublic: async (slug: string, data: {
    customer_name: string;
    phone: string;
    email?: string;
    service_type: string;
    details: string;
    budget?: string;
    preferred_date?: string;
  }) => {
    const response = await api.post<{
      message: string;
      estimate_id: number;
    }>(`/public/estimate/${slug}`, data);
    return response.data;
  },
};

// ============================================
// Logs API
// ============================================

export const logsAPI = {
  list: async (params?: {
    channel?: string;
    response_type?: string;
    date_from?: string;
    date_to?: string;
    search?: string;
    page?: number;
    per_page?: number;
  }) => {
    const response = await api.get<PaginatedResponse<MessageLog>>('/logs', { params });
    return response.data;
  },

  get: async (id: number) => {
    const response = await api.get<MessageLog>(`/logs/${id}`);
    return response.data;
  },

  export: async (params?: {
    channel?: string;
    date_from?: string;
    date_to?: string;
  }) => {
    const response = await api.get('/logs/export', {
      params,
      responseType: 'blob',
    });

    // 파일 다운로드 트리거
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `message-logs-${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  },
};

// ============================================
// Dashboard API
// ============================================

export interface ChartDataPoint {
  date: string;
  label: string;
  count: number;
}

export interface ChannelStat {
  channel: string;
  count: number;
}

export interface ResponseStat {
  type: string;
  count: number;
}

export interface DashboardSummary {
  connected_channels: number;
  active_rules: number;
  pending_reservations: number;
}

export const dashboardAPI = {
  stats: async () => {
    const response = await api.get<DashboardStats>('/dashboard/stats');
    return response.data;
  },

  recentActivity: async (limit: number = 10) => {
    const response = await api.get<MessageLog[]>('/dashboard/recent-activity', {
      params: { limit },
    });
    return response.data;
  },

  chart: async (days: number = 7) => {
    const response = await api.get<ChartDataPoint[]>('/dashboard/chart', {
      params: { days },
    });
    return response.data;
  },

  channelStats: async () => {
    const response = await api.get<ChannelStat[]>('/dashboard/channel-stats');
    return response.data;
  },

  responseStats: async () => {
    const response = await api.get<ResponseStat[]>('/dashboard/response-stats');
    return response.data;
  },

  summary: async () => {
    const response = await api.get<DashboardSummary>('/dashboard/summary');
    return response.data;
  },
};

// ============================================
// Profile API
// ============================================

export const profileAPI = {
  get: async () => {
    const response = await api.get<User>('/profile');
    return response.data;
  },

  update: async (data: Partial<User>) => {
    const response = await api.put<User>('/profile', data);
    return response.data;
  },

  updatePassword: async (data: {
    current_password: string;
    password: string;
    password_confirmation: string;
  }) => {
    await api.put('/profile/password', data);
  },

  deleteAccount: async (password: string) => {
    await api.delete('/profile', { data: { password } });
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  },
};

// ============================================
// Subscription API
// ============================================

export const subscriptionAPI = {
  get: async () => {
    const response = await api.get<Subscription>('/subscription');
    return response.data;
  },

  upgrade: async (plan: 'pro' | 'agency') => {
    const response = await api.post<{
      checkout_url: string;
    }>('/subscription/upgrade', { plan });
    return response.data;
  },

  cancel: async () => {
    await api.post('/subscription/cancel');
  },

  resume: async () => {
    const response = await api.post<Subscription>('/subscription/resume');
    return response.data;
  },

  getUsage: async () => {
    const response = await api.get<{
      today_messages: number;
      daily_limit: number | null;
      ai_tokens_used: number;
      plan: string;
    }>('/subscription/usage');
    return response.data;
  },
};

// ============================================
// Webhook API (내부 테스트용)
// ============================================

export const webhookAPI = {
  testInstagram: async (message: string, senderId: string = 'test_user') => {
    const response = await api.post('/webhook/instagram/test', {
      message,
      sender_id: senderId,
    });
    return response.data;
  },
};
