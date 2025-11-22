import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import './App.css';
import MemberRegistration from './components/MemberRegistration';
import MemberList from './components/MemberList';
import SearchMembers from './components/SearchMembers';
import ApiResponseViewer from './components/ApiResponseViewer';
import { getMembers, createMember, updateMember, deleteMember, searchMembers } from './services/api';

function AppContent() {
  const navigate = useNavigate();
  const [members, setMembers] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [searchResults, setSearchResults] = useState(null);

  const loadMembers = async (page = 0, size = 10) => {
    setLoading(true);
    setError(null);
    try {
      const response = await getMembers(page, size);
      if (response.data && response.data.success) {
        const pageData = response.data.data;
        setMembers(pageData.content || []);
        setCurrentPage(pageData.number || 0);
        setTotalPages(pageData.totalPages || 0);
        setTotalElements(pageData.totalElements || 0);
        setSearchResults(null);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load members');
      console.error('Error loading members:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMembers();
  }, []);

  const handleRegister = async (memberData) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await createMember(memberData);
      if (response.data && response.data.success) {
        setSuccess('Member registered successfully!');
        loadMembers(currentPage);
        // Clear form after successful registration
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      const errorData = err.response?.data;
      if (errorData) {
        if (errorData.data && typeof errorData.data === 'object') {
          // Validation errors
          const errorMessages = Object.values(errorData.data).join(', ');
          setError(errorMessages);
        } else {
          setError(errorData.message || 'Failed to register member');
        }
      } else {
        setError('Failed to register member');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (id, memberData) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await updateMember(id, memberData);
      if (response.data && response.data.success) {
        setSuccess('Member updated successfully!');
        loadMembers(currentPage);
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      const errorData = err.response?.data;
      if (errorData) {
        if (errorData.data && typeof errorData.data === 'object') {
          const errorMessages = Object.values(errorData.data).join(', ');
          setError(errorMessages);
        } else {
          setError(errorData.message || 'Failed to update member');
        }
      } else {
        setError('Failed to update member');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this member?')) {
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await deleteMember(id);
      if (response.data && response.data.success) {
        setSuccess('Member deleted successfully!');
        loadMembers(currentPage);
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete member');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (name) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await searchMembers(name);
      if (response.data && response.data.success) {
        setSearchResults(response.data.data || []);
        setSuccess(`Found ${response.data.data.length} member(s)`);
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const handleClearSearch = () => {
    setSearchResults(null);
    loadMembers(currentPage);
  };

  return (
    <div className="App">
      <div className="container">
        <header>
          <h1>Welcome to Kitchensink!</h1>
          <p>Member Registration and Management Application</p>
        </header>

        <div className="content">
          <div className="main-content">
            <MemberRegistration 
              onRegister={handleRegister}
              loading={loading}
            />

            {error && (
              <div className="message error">
                {error}
              </div>
            )}

            {success && (
              <div className="message success">
                {success}
              </div>
            )}

            <SearchMembers 
              onSearch={handleSearch}
              onClear={handleClearSearch}
              loading={loading}
            />

            <MemberList
              members={searchResults || members}
              currentPage={currentPage}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={loadMembers}
              onUpdate={handleUpdate}
              onDelete={handleDelete}
              loading={loading}
              isSearchResults={searchResults !== null}
              navigate={navigate}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<AppContent />} />
        <Route path="/api/*" element={<ApiResponseViewer />} />
      </Routes>
    </Router>
  );
}

export default App;

