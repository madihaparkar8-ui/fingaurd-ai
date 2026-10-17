import axios from 'axios';
import { AuthResponse, User, Investment, Journey, RiskReport, GroupedSignals, InfluenceAnalysis, AuditEvent, ConsentRecord } from '../types';

const api = axios.create({
  baseURL: '/api',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('fingaurd_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register') && window.location.pathname !== '/') {
        localStorage.removeItem('fingaurd_token');
        localStorage.removeItem('fingaurd_user');
      }
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/login', credentials);
    return res.data;
  },
  register: async (data: { name: string; email: string; password: string }): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/register', data);
    return res.data;
  },
  getMe: async (): Promise<User> => {
    const res = await api.get<User>('/auth/me');
    return res.data;
  },
  demoLogin: async (): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/demo/login');
    return res.data;
  },
};

export const investmentService = {
  getAll: async (): Promise<Investment[]> => {
    const res = await api.get<Investment[]>('/investments');
    return res.data;
  },
  create: async (data: Partial<Investment>): Promise<Investment> => {
    const res = await api.post<Investment>('/investments', data);
    return res.data;
  },
  update: async (id: number, data: Partial<Investment>): Promise<Investment> => {
    const res = await api.put<Investment>(`/investments/${id}`, data);
    return res.data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/investments/${id}`);
  },
  importCsv: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post<{ totalRows: number; importedCount: number; failedCount: number; errors: string[] }>(
      '/investments/import',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return res.data;
  },
};

export const journeyService = {
  getJourney: async (userId: number): Promise<Journey> => {
    const res = await api.get<Journey>(`/users/${userId}/journey`);
    return res.data;
  },
  getMyJourney: async (): Promise<Journey> => {
    const res = await api.get<Journey>('/users/journey');
    return res.data;
  },
};

export const riskService = {
  analyse: async (data: {
    amount: number;
    date: string;
    entity: string;
    communicationText?: string;
    consentGranted?: boolean;
  }): Promise<RiskReport> => {
    const res = await api.post<RiskReport>('/risk/analyse', data);
    return res.data;
  },
  getReport: async (reportId: number): Promise<RiskReport> => {
    const res = await api.get<RiskReport>(`/risk/${reportId}`);
    return res.data;
  },
  getSignals: async (reportId: number): Promise<GroupedSignals> => {
    const res = await api.get<GroupedSignals>(`/risk/${reportId}/signals`);
    return res.data;
  },
  getHistory: async (): Promise<RiskReport[]> => {
    const res = await api.get<RiskReport[]>('/risk/history');
    return res.data;
  },
};

export const communicationService = {
  analyse: async (data: { text: string; consentGranted: boolean }): Promise<InfluenceAnalysis> => {
    const res = await api.post<InfluenceAnalysis>('/communications/analyse', data);
    return res.data;
  },
};

export const demoService = {
  seed: async (type: string) => {
    const res = await api.post<{ status: string; preset: string; message: string }>(`/demo/seed?type=${type}`);
    return res.data;
  },
};

export const consentService = {
  get: async (purpose = 'COMMUNICATION_ANALYSIS'): Promise<ConsentRecord> => {
    const res = await api.get<ConsentRecord>(`/consent?purpose=${purpose}`);
    return res.data;
  },
  set: async (purpose: string, granted: boolean): Promise<ConsentRecord> => {
    const res = await api.post<ConsentRecord>('/consent', { purpose, granted });
    return res.data;
  },
};

export const auditService = {
  getEvents: async (): Promise<AuditEvent[]> => {
    const res = await api.get<AuditEvent[]>('/audit');
    return res.data;
  },
};

export default api;
