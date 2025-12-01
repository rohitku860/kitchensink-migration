import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/kitchensink/v1';
const API_KEY = 'your-secret-api-key-change-in-production';

const authApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': API_KEY,
  },
});

// Request interceptor for JWT token
authApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const correlationId = localStorage.getItem('correlationId') || 
    `client-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  localStorage.setItem('correlationId', correlationId);
  config.headers['X-Correlation-ID'] = correlationId;
  return config;
});

// Response interceptor for error handling
authApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Unauthorized - clear token and redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth endpoints
export const requestLoginOtp = (email) => {
  return authApi.post('/auth/login/request-otp', { email });
};

export const verifyOtpAndLogin = (email, otp) => {
  return authApi.post('/auth/login/verify', { email, otp });
};

// Profile endpoints
export const getProfile = (userId) => {
  return authApi.get(`/profile/${userId}`);
};

export const requestEmailChangeOtp = (userId, newEmail) => {
  return authApi.post(`/profile/${userId}/email/request-otp`, { newEmail });
};

export const updateField = (userId, fieldName, value, otp = null) => {
  const fieldUpdate = { fieldName, value };
  if (otp) fieldUpdate.otp = otp;
  return authApi.put(`/profile/${userId}`, [fieldUpdate]);
};

export const updateFields = (userId, fieldUpdates) => {
  return authApi.put(`/profile/${userId}`, fieldUpdates);
};

export const getUserUpdateRequests = (userId) => {
  return authApi.get(`/profile/${userId}/update-requests`);
};

// My Profile endpoints (using ProfileController with userId from localStorage)
const getCurrentUserId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  return user.userId;
};

export const getMyProfile = () => {
  const userId = getCurrentUserId();
  return authApi.get(`/profile/${userId}`);
};

export const requestMyEmailChangeOtp = (newEmail) => {
  const userId = getCurrentUserId();
  return authApi.post(`/profile/${userId}/email/request-otp`, { newEmail });
};

export const updateMyField = (fieldName, value, otp = null) => {
  const userId = getCurrentUserId();
  return updateField(userId, fieldName, value, otp);
};

export const updateMyFields = (fieldUpdates) => {
  const userId = getCurrentUserId();
  return updateFields(userId, fieldUpdates);
};

export const getMyUpdateRequests = () => {
  const userId = getCurrentUserId();
  return authApi.get(`/profile/${userId}/update-requests`);
};

export const revokeUpdateRequest = (requestId) => {
  const userId = getCurrentUserId();
  return authApi.delete(`/profile/${userId}/update-requests/${requestId}`);
};

// Admin endpoints
export const getAllUsers = (cursor = null, size = 10, direction = 'next') => {
  const params = { size };
  if (cursor) {
    params.cursor = cursor;
    params.direction = direction;
  }
  return authApi.get('/admin/users', { params });
};

export const searchUsers = (name) => {
  return authApi.get('/admin/users/search', { params: { name } });
};

export const createUser = (userData) => {
  return authApi.post('/admin/users', userData);
};

export const updateUser = (userId, userData) => {
  return authApi.put(`/admin/users/${userId}`, userData);
};

export const deleteUser = (userId) => {
  return authApi.delete(`/admin/users/${userId}`);
};

export const getPendingUpdateRequests = () => {
  return authApi.get('/admin/update-requests');
};

export const approveUpdateRequest = (requestId) => {
  return authApi.post(`/admin/update-requests/${requestId}/approve`);
};

export const rejectUpdateRequest = (requestId, reason) => {
  return authApi.post(`/admin/update-requests/${requestId}/reject`, { reason });
};

export default authApi;

