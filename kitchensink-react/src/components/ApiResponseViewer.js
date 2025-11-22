import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';

const ApiResponseViewer = () => {
  const params = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Get the path from params - react-router v6 uses '*' for catch-all
  const pathParam = params['*'] || '';
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [url, setUrl] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        // Decode the path parameter
        const decodedPath = decodeURIComponent(pathParam || '');
        
        // Handle query parameters if present
        let apiPath = '';
        let fullUrl = '';
        
        if (decodedPath.includes('?')) {
          // Has query parameters (e.g., /kitchensink/v1/members?page=0&size=100)
          const [path, queryString] = decodedPath.split('?');
          fullUrl = decodedPath;
          apiPath = path.startsWith('/kitchensink/v1/members') 
            ? path.replace('/kitchensink/v1/members', '')
            : path;
          apiPath = apiPath + '?' + queryString;
        } else {
          // No query parameters (e.g., /kitchensink/v1/members/123)
          fullUrl = decodedPath;
          apiPath = decodedPath.startsWith('/kitchensink/v1/members') 
            ? decodedPath.replace('/kitchensink/v1/members', '')
            : decodedPath;
        }
        
        setUrl(fullUrl);
        
        // Fetch data from API
        const response = await api.get(apiPath);
        setData(response.data);
      } catch (err) {
        setError(err.response?.data?.message || err.message || 'Failed to fetch data');
        console.error('Error fetching data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [pathParam, location.key]);

  const handleBack = () => {
    navigate('/');
  };

  if (loading) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        <div>Loading...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '20px' }}>
        <div style={{ color: 'red', marginBottom: '10px' }}>Error: {error}</div>
        <button onClick={handleBack} style={{ padding: '8px 16px', cursor: 'pointer' }}>
          Back to Home
        </button>
      </div>
    );
  }

  return (
    <div style={{ 
      fontFamily: 'monospace', 
      padding: '20px', 
      backgroundColor: '#f5f5f5',
      minHeight: '100vh'
    }}>
      <div style={{ marginBottom: '20px' }}>
        <button 
          onClick={handleBack}
          style={{ 
            padding: '8px 16px', 
            cursor: 'pointer',
            marginBottom: '10px',
            backgroundColor: '#0066cc',
            color: 'white',
            border: 'none',
            borderRadius: '4px'
          }}
        >
          ← Back to Home
        </button>
        <h2>REST API Response</h2>
        <p><strong>URL:</strong> {url}</p>
        <p><strong>Timestamp:</strong> {new Date().toLocaleString()}</p>
      </div>
      <div style={{
        backgroundColor: '#fff',
        padding: '15px',
        borderRadius: '5px',
        border: '1px solid #ddd',
        overflowX: 'auto'
      }}>
        <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
          {JSON.stringify(data, null, 2)}
        </pre>
      </div>
      <div style={{ marginTop: '20px' }}>
        <button 
          onClick={() => window.location.reload()}
          style={{ 
            padding: '8px 16px', 
            cursor: 'pointer',
            backgroundColor: '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            marginRight: '10px'
          }}
        >
          🔄 Reload (Fetch Latest Data)
        </button>
      </div>
    </div>
  );
};

export default ApiResponseViewer;

