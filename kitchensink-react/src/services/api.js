import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/kitchensink/v1/members';
const API_KEY = 'your-secret-api-key-change-in-production'; // Should be in env variable

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': API_KEY,
  },
});

// Request interceptor for correlation ID
api.interceptors.request.use((config) => {
  const correlationId = localStorage.getItem('correlationId') || 
    `client-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  localStorage.setItem('correlationId', correlationId);
  config.headers['X-Correlation-ID'] = correlationId;
  return config;
});

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Server responded with error
      console.error('API Error:', error.response.data);
    } else if (error.request) {
      // Request made but no response
      console.error('Network Error:', error.request);
      error.message = 'Network error. Please check if the API server is running.';
    } else {
      // Something else happened
      console.error('Error:', error.message);
    }
    return Promise.reject(error);
  }
);

export const getMembers = (page = 0, size = 10) => {
  return api.get('', {
    params: {
      page,
      size,
      sort: 'name,asc',
    },
  });
};

export const getMemberById = (id) => {
  return api.get(`/${id}`);
};

export const createMember = (memberData) => {
  return api.post('', memberData);
};

export const updateMember = (id, memberData) => {
  return api.put(`/${id}`, memberData);
};

export const deleteMember = (id) => {
  return api.delete(`/${id}`);
};

export const searchMembers = (name) => {
  if (!name || name.trim() === '') {
    throw new Error('Name parameter is required for search');
  }
  return api.get('/search', { params: { name } });
};

export default api;

