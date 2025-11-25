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

export const updateName = (userId, name) => {
  return authApi.put(`/profile/${userId}/name`, { name });
};

export const updatePhoneNumber = (userId, phoneNumber) => {
  return authApi.put(`/profile/${userId}/phone`, { phoneNumber });
};

export const requestEmailChangeOtp = (userId, newEmail) => {
  return authApi.post(`/profile/${userId}/email/request-otp`, { newEmail });
};

export const updateEmail = (userId, newEmail, otp, otpId) => {
  return authApi.put(`/profile/${userId}/email`, { newEmail, otp, otpId });
};

export const raiseUpdateRequest = (userId, fieldName, newValue) => {
  return authApi.post(`/profile/${userId}/update-request`, { fieldName, newValue });
};

export const getUserUpdateRequests = (userId) => {
  return authApi.get(`/profile/${userId}/update-requests`);
};

// My Profile endpoints
export const getMyProfile = () => {
  return authApi.get('/my-profile');
};

export const updateMyPhoneNumber = (phoneNumber) => {
  return authApi.put('/my-profile/phone', { phoneNumber });
};

export const requestMyEmailChangeOtp = (newEmail) => {
  return authApi.post('/my-profile/email/request-otp', { newEmail });
};

export const requestMyEmailChange = (newEmail, otp, otpId) => {
  return authApi.put('/my-profile/email', { newEmail, otp, otpId });
};

export const raiseMyUpdateRequest = (fieldName, newValue) => {
  return authApi.post('/my-profile/update-request', { fieldName, newValue });
};

export const getMyUpdateRequests = () => {
  return authApi.get('/my-profile/update-requests');
};

export const revokeUpdateRequest = (requestId) => {
  return authApi.delete(`/my-profile/update-requests/${requestId}`);
};

// Admin endpoints
export const getAllUsers = (page = 0, size = 10) => {
  return authApi.get('/admin/users', {
    params: { page, size, sort: 'name,asc' },
  });
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

