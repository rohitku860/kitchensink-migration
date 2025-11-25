import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getAllUsers,
  searchUsers,
  createUser,
  updateUser,
  deleteUser,
  getPendingUpdateRequests,
  approveUpdateRequest,
  rejectUpdateRequest,
} from '../services/authApi';
import './AdminDashboard.css';

function AdminDashboard() {
  const navigate = useNavigate();
  const [activeSection, setActiveSection] = useState('users'); // 'users', 'create', 'requests'
  const [users, setUsers] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchMode, setIsSearchMode] = useState(false);
  const [updateRequests, setUpdateRequests] = useState([]);
  const [newUser, setNewUser] = useState({ 
    name: '', 
    email: '', 
    isdCode: '+91',
    phoneNumber: '',
    dateOfBirth: '',
    address: '',
    city: '',
    country: ''
  });
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [editingUser, setEditingUser] = useState(null);
  const [showEditForm, setShowEditForm] = useState(false);
  const [editUserData, setEditUserData] = useState({
    name: '',
    email: '',
    isdCode: '+91',
    phoneNumber: '',
    dateOfBirth: '',
    address: '',
    city: '',
    country: ''
  });
  
  // Helper function to format validation errors
  const formatValidationError = (err) => {
    const errorData = err.response?.data;
    if (!errorData) {
      return err.response?.data?.message || 'An error occurred';
    }
    
    // Check if it's a validation error with field-specific errors
    if (errorData.data && typeof errorData.data === 'object' && !Array.isArray(errorData.data)) {
      const fieldErrors = errorData.data;
      const fieldNames = Object.keys(fieldErrors);
      
      if (fieldNames.length > 0) {
        // Store field-specific errors for display
        setFieldErrors(fieldErrors);
        
        // Create a formatted message with field names and errors
        const errorMessages = fieldNames.map(field => {
          const fieldLabel = field.charAt(0).toUpperCase() + field.slice(1).replace(/([A-Z])/g, ' $1');
          return `${fieldLabel}: ${fieldErrors[field]}`;
        });
        
        return errorMessages.join('; ');
      }
    }
    
    // Clear field errors if not a validation error
    setFieldErrors({});
    return errorData.message || 'An error occurred';
  };

  useEffect(() => {
    if (activeSection === 'users' && !isSearchMode) {
      loadUsers();
    } else if (activeSection === 'requests') {
      loadUpdateRequests();
    }
  }, [activeSection, currentPage]);

  const loadUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getAllUsers(currentPage, 10);
      if (response.data && response.data.success) {
        const pageData = response.data.data;
        setUsers(pageData.content || []);
        setCurrentPage(pageData.number || 0);
        setTotalPages(pageData.totalPages || 0);
        setTotalElements(pageData.totalElements || 0);
        setIsSearchMode(false);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const loadUpdateRequests = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getPendingUpdateRequests();
      if (response.data && response.data.success) {
        setUpdateRequests(response.data.data || []);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load update requests');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!searchQuery.trim()) {
      loadUsers();
      return;
    }
    setLoading(true);
    setError(null);
    setIsSearchMode(true);
    try {
      const response = await searchUsers(searchQuery);
      if (response.data && response.data.success) {
        setUsers(response.data.data || []);
        setSuccess(`Found ${response.data.data.length} user(s)`);
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const handleClearSearch = () => {
    setSearchQuery('');
    setIsSearchMode(false);
    loadUsers();
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await createUser(newUser);
      if (response.data && response.data.success) {
        setSuccess('User created successfully');
        setNewUser({ 
          name: '', 
          email: '', 
          isdCode: '+91',
          phoneNumber: '',
          dateOfBirth: '',
          address: '',
          city: '',
          country: ''
        });
        setShowCreateForm(false);
        setFieldErrors({});
        loadUsers();
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      const errorMessage = formatValidationError(err);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleApproveRequest = async (requestId) => {
    setLoading(true);
    setError(null);
    try {
      const response = await approveUpdateRequest(requestId);
      if (response.data && response.data.success) {
        setSuccess('Update request approved');
        loadUpdateRequests();
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to approve request');
    } finally {
      setLoading(false);
    }
  };

  const handleRejectRequest = async (requestId) => {
    const reason = window.prompt('Enter rejection reason:');
    if (!reason) return;
    
    setLoading(true);
    setError(null);
    try {
      const response = await rejectUpdateRequest(requestId, reason);
      if (response.data && response.data.success) {
        setSuccess('Update request rejected');
        loadUpdateRequests();
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reject request');
    } finally {
      setLoading(false);
    }
  };

  const handleEditUser = (user) => {
    setEditingUser(user);
    setEditUserData({
      name: user.name || '',
      email: user.email || '',
      isdCode: user.isdCode || '+91',
      phoneNumber: user.phoneNumber || '',
      dateOfBirth: user.dateOfBirth || '',
      address: user.address || '',
      city: user.city || '',
      country: user.country || ''
    });
    setFieldErrors({});
    setError(null);
    setShowEditForm(true);
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    if (!editingUser) return;
    
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await updateUser(editingUser.id, editUserData);
      if (response.data && response.data.success) {
        setSuccess('User updated successfully');
        setShowEditForm(false);
        setEditingUser(null);
        setFieldErrors({});
        loadUsers();
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      const errorMessage = formatValidationError(err);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (userId, userName) => {
    if (!window.confirm(`Are you sure you want to delete user "${userName}"? This action cannot be undone.`)) {
      return;
    }
    
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await deleteUser(userId);
      if (response.data && response.data.success) {
        setSuccess('User deleted successfully');
        loadUsers();
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete user');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="admin-dashboard">
      <div className="dashboard-header">
        <h1>Admin Dashboard</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/my-profile')} className="btn-profile">
            My Profile
          </button>
          <button onClick={() => {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            navigate('/login');
          }} className="btn-logout">
            Logout
          </button>
        </div>
      </div>

      <div className="dashboard-nav">
        <button
          className={activeSection === 'users' ? 'active' : ''}
          onClick={() => { setActiveSection('users'); loadUsers(); }}
        >
          <span className="nav-icon">👥</span> Users
        </button>
        <button
          className={activeSection === 'create' ? 'active' : ''}
          onClick={() => { 
            setActiveSection('create'); 
            setShowCreateForm(true);
            setFieldErrors({});
            setError(null);
          }}
        >
          <span className="nav-icon">➕</span> Create User
        </button>
        <button
          className={activeSection === 'requests' ? 'active' : ''}
          onClick={() => { setActiveSection('requests'); loadUpdateRequests(); }}
        >
          <span className="nav-icon">📋</span> Update Requests
          {updateRequests.length > 0 && (
            <span className="badge">{updateRequests.length}</span>
          )}
        </button>
      </div>

      {error && <div className="message error">{error}</div>}
      {success && <div className="message success">{success}</div>}

      {activeSection === 'users' && (
        <div className="section-content">
          <div className="section-header">
            <h2>User Management</h2>
            <div className="search-container">
              <form onSubmit={handleSearch} className="search-form">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search users by name..."
                  className="search-input"
                />
                <button type="submit" className="btn-search" disabled={loading}>
                  🔍 Search
                </button>
                {isSearchMode && (
                  <button type="button" onClick={handleClearSearch} className="btn-clear">
                    Clear
                  </button>
                )}
              </form>
            </div>
          </div>

          {loading ? (
            <div className="loading">Loading...</div>
          ) : (
            <>
              <div className="users-stats">
                {isSearchMode ? (
                  <span>Search Results: {users.length} user(s)</span>
                ) : (
                  <span>Total Users: {totalElements}</span>
                )}
              </div>
              <div className="users-table-container">
                <table className="users-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Phone</th>
                      <th>Role</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.length === 0 ? (
                      <tr>
                        <td colSpan="5" className="no-data">No users found</td>
                      </tr>
                    ) : (
                      users.map((user) => (
                        <tr key={user.id}>
                          <td>{user.name}</td>
                          <td>{user.email}</td>
                          <td>{user.isdCode ? `${user.isdCode} ` : ''}{user.phoneNumber}</td>
                          <td>
                            <span className={`role-badge ${user.role.toLowerCase()}`}>
                              {user.role}
                            </span>
                          </td>
                          <td>
                            <div className="action-buttons">
                              <button
                                onClick={() => window.open(`/profile/${user.id}`, '_blank')}
                                className="btn-action btn-view"
                                title="View Profile in New Tab"
                              >
                                👁️ View
                              </button>
                              <button
                                onClick={() => handleEditUser(user)}
                                className="btn-action btn-edit"
                                title="Edit User"
                              >
                                ✏️ Edit
                              </button>
                              <button
                                onClick={() => handleDeleteUser(user.id, user.name)}
                                className="btn-action btn-delete"
                                title="Delete User"
                                disabled={loading}
                              >
                                🗑️ Delete
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
              {!isSearchMode && totalPages > 1 && (
                <div className="pagination">
                  <button
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 0 || loading}
                    className="btn-pagination"
                  >
                    ← Previous
                  </button>
                  <span className="page-info">
                    Page {currentPage + 1} of {totalPages}
                  </span>
                  <button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1 || loading}
                    className="btn-pagination"
                  >
                    Next →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {activeSection === 'create' && (
        <div className="section-content">
          <div className="section-header">
            <h2>Create New User</h2>
            <button onClick={() => {
              setShowCreateForm(false);
              setFieldErrors({});
              setError(null);
            }} className="btn-close">✕</button>
          </div>
          {showCreateForm && (
            <form onSubmit={handleCreateUser} className="create-user-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Name: <span className="required">*</span></label>
                  <input
                    type="text"
                    value={newUser.name}
                    onChange={(e) => {
                      setNewUser({ ...newUser, name: e.target.value });
                      if (fieldErrors.name) {
                        setFieldErrors({ ...fieldErrors, name: '' });
                      }
                    }}
                    required
                    placeholder="Enter full name (letters only)"
                    pattern="[a-zA-Z\s'-]+"
                    title="Name must contain only letters, spaces, hyphens, and apostrophes"
                    className={fieldErrors.name ? 'error' : ''}
                  />
                  {fieldErrors.name && (
                    <div className="field-error">{fieldErrors.name}</div>
                  )}
                </div>
                <div className="form-group">
                  <label>Email: <span className="required">*</span></label>
                  <input
                    type="email"
                    value={newUser.email}
                    onChange={(e) => {
                      setNewUser({ ...newUser, email: e.target.value });
                      if (fieldErrors.email) {
                        setFieldErrors({ ...fieldErrors, email: '' });
                      }
                    }}
                    required
                    placeholder="Enter email address"
                    className={fieldErrors.email ? 'error' : ''}
                  />
                  {fieldErrors.email && (
                    <div className="field-error">{fieldErrors.email}</div>
                  )}
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>ISD Code:</label>
                  <input
                    type="text"
                    value={newUser.isdCode}
                    onChange={(e) => setNewUser({ ...newUser, isdCode: e.target.value })}
                    placeholder="+91 (optional)"
                    pattern="^$|^\+?[0-9]+$"
                    title="ISD code must be numeric with optional + prefix or empty"
                  />
                </div>
                <div className="form-group">
                  <label>Phone Number: <span className="required">*</span></label>
                  <input
                    type="text"
                    value={newUser.phoneNumber}
                    onChange={(e) => {
                      setNewUser({ ...newUser, phoneNumber: e.target.value.replace(/\D/g, '') });
                      if (fieldErrors.phoneNumber) {
                        setFieldErrors({ ...fieldErrors, phoneNumber: '' });
                      }
                    }}
                    required
                    placeholder="Enter phone number (digits only)"
                    pattern="[0-9]{10,15}"
                    title="Phone number must be 10-15 digits"
                    maxLength="15"
                    className={fieldErrors.phoneNumber ? 'error' : ''}
                  />
                  {fieldErrors.phoneNumber && (
                    <div className="field-error">{fieldErrors.phoneNumber}</div>
                  )}
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>Date of Birth:</label>
                  <input
                    type="date"
                    value={newUser.dateOfBirth}
                    onChange={(e) => setNewUser({ ...newUser, dateOfBirth: e.target.value })}
                    placeholder="YYYY-MM-DD"
                  />
                </div>
                <div className="form-group">
                  <label>Country:</label>
                  <input
                    type="text"
                    value={newUser.country}
                    onChange={(e) => setNewUser({ ...newUser, country: e.target.value })}
                    placeholder="Enter country"
                    maxLength="50"
                  />
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>City:</label>
                  <input
                    type="text"
                    value={newUser.city}
                    onChange={(e) => setNewUser({ ...newUser, city: e.target.value })}
                    placeholder="Enter city"
                    maxLength="50"
                  />
                </div>
                <div className="form-group">
                  <label>Address:</label>
                  <input
                    type="text"
                    value={newUser.address}
                    onChange={(e) => setNewUser({ ...newUser, address: e.target.value })}
                    placeholder="Enter address"
                    maxLength="200"
                  />
                </div>
              </div>
              
              <div className="form-actions">
                <button type="submit" disabled={loading} className="btn-primary">
                  {loading ? 'Creating...' : 'Create User'}
                </button>
                <button type="button" onClick={() => setShowCreateForm(false)} className="btn-secondary">
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      {showEditForm && editingUser && (
        <div className="modal-overlay" onClick={() => {
          setShowEditForm(false);
          setEditingUser(null);
          setFieldErrors({});
          setError(null);
        }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="section-header">
              <h2>Edit User</h2>
              <button onClick={() => {
                setShowEditForm(false);
                setEditingUser(null);
                setFieldErrors({});
                setError(null);
              }} className="btn-close">✕</button>
            </div>
            {error && <div className="message error">{error}</div>}
            {success && <div className="message success">{success}</div>}
            <form onSubmit={handleUpdateUser} className="create-user-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Name: <span className="required">*</span></label>
                  <input
                    type="text"
                    value={editUserData.name}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, name: e.target.value });
                      if (fieldErrors.name) {
                        setFieldErrors({ ...fieldErrors, name: '' });
                      }
                    }}
                    required
                    placeholder="Enter full name (letters only)"
                    pattern="[a-zA-Z\s'-]+"
                    title="Name must contain only letters, spaces, hyphens, and apostrophes"
                    className={fieldErrors.name ? 'error' : ''}
                  />
                  {fieldErrors.name && (
                    <div className="field-error">{fieldErrors.name}</div>
                  )}
                </div>
                <div className="form-group">
                  <label>Email: <span className="required">*</span></label>
                  <input
                    type="email"
                    value={editUserData.email}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, email: e.target.value });
                      if (fieldErrors.email) {
                        setFieldErrors({ ...fieldErrors, email: '' });
                      }
                    }}
                    required
                    placeholder="Enter email address"
                    className={fieldErrors.email ? 'error' : ''}
                  />
                  {fieldErrors.email && (
                    <div className="field-error">{fieldErrors.email}</div>
                  )}
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>ISD Code:</label>
                  <input
                    type="text"
                    value={editUserData.isdCode}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, isdCode: e.target.value });
                      if (fieldErrors.isdCode) {
                        setFieldErrors({ ...fieldErrors, isdCode: '' });
                      }
                    }}
                    placeholder="+91 (optional)"
                    pattern="^$|^\+?[0-9]+$"
                    title="ISD code must be numeric with optional + prefix or empty"
                    className={fieldErrors.isdCode ? 'error' : ''}
                  />
                  {fieldErrors.isdCode && (
                    <div className="field-error">{fieldErrors.isdCode}</div>
                  )}
                </div>
                <div className="form-group">
                  <label>Phone Number: <span className="required">*</span></label>
                  <input
                    type="text"
                    value={editUserData.phoneNumber}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, phoneNumber: e.target.value.replace(/\D/g, '') });
                      if (fieldErrors.phoneNumber) {
                        setFieldErrors({ ...fieldErrors, phoneNumber: '' });
                      }
                    }}
                    required
                    placeholder="Enter phone number (digits only)"
                    pattern="[0-9]{10,15}"
                    title="Phone number must be 10-15 digits"
                    maxLength="15"
                    className={fieldErrors.phoneNumber ? 'error' : ''}
                  />
                  {fieldErrors.phoneNumber && (
                    <div className="field-error">{fieldErrors.phoneNumber}</div>
                  )}
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>Date of Birth:</label>
                  <input
                    type="date"
                    value={editUserData.dateOfBirth}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, dateOfBirth: e.target.value });
                      if (fieldErrors.dateOfBirth) {
                        setFieldErrors({ ...fieldErrors, dateOfBirth: '' });
                      }
                    }}
                    placeholder="YYYY-MM-DD"
                    className={fieldErrors.dateOfBirth ? 'error' : ''}
                  />
                  {fieldErrors.dateOfBirth && (
                    <div className="field-error">{fieldErrors.dateOfBirth}</div>
                  )}
                </div>
                <div className="form-group">
                  <label>Country:</label>
                  <input
                    type="text"
                    value={editUserData.country}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, country: e.target.value });
                      if (fieldErrors.country) {
                        setFieldErrors({ ...fieldErrors, country: '' });
                      }
                    }}
                    placeholder="Enter country"
                    maxLength="50"
                    className={fieldErrors.country ? 'error' : ''}
                  />
                  {fieldErrors.country && (
                    <div className="field-error">{fieldErrors.country}</div>
                  )}
                </div>
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label>City:</label>
                  <input
                    type="text"
                    value={editUserData.city}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, city: e.target.value });
                      if (fieldErrors.city) {
                        setFieldErrors({ ...fieldErrors, city: '' });
                      }
                    }}
                    placeholder="Enter city"
                    maxLength="50"
                    className={fieldErrors.city ? 'error' : ''}
                  />
                  {fieldErrors.city && (
                    <div className="field-error">{fieldErrors.city}</div>
                  )}
                </div>
                <div className="form-group">
                  <label>Address:</label>
                  <input
                    type="text"
                    value={editUserData.address}
                    onChange={(e) => {
                      setEditUserData({ ...editUserData, address: e.target.value });
                      if (fieldErrors.address) {
                        setFieldErrors({ ...fieldErrors, address: '' });
                      }
                    }}
                    placeholder="Enter address"
                    maxLength="200"
                    className={fieldErrors.address ? 'error' : ''}
                  />
                  {fieldErrors.address && (
                    <div className="field-error">{fieldErrors.address}</div>
                  )}
                </div>
              </div>
              
              <div className="form-actions">
                <button type="submit" disabled={loading} className="btn-primary">
                  {loading ? 'Updating...' : 'Update User'}
                </button>
                <button 
                  type="button" 
                  onClick={() => {
                    setShowEditForm(false);
                    setEditingUser(null);
                    setFieldErrors({});
                    setError(null);
                  }} 
                  className="btn-secondary"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {activeSection === 'requests' && (
        <div className="section-content">
          <div className="section-header">
            <h2>Pending Update Requests</h2>
            <button onClick={loadUpdateRequests} className="btn-refresh" disabled={loading}>
              🔄 Refresh
            </button>
          </div>
          {loading ? (
            <div className="loading">Loading...</div>
          ) : updateRequests.length === 0 ? (
            <div className="no-data-message">No pending requests</div>
          ) : (
            <div className="requests-list">
              {updateRequests.map((request) => (
                <div key={request.id} className="request-card">
                  <div className="request-info">
                    <div className="request-header">
                      <strong>User ID: {request.userId}</strong>
                      <span className="request-date">
                        {new Date(request.requestedAt).toLocaleString()}
                      </span>
                    </div>
                    <div className="request-details">
                      <div className="detail-item">
                        <span className="detail-label">Field:</span>
                        <span className="detail-value">{request.fieldName}</span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">Old Value:</span>
                        <span className="detail-value old">{request.oldValue}</span>
                      </div>
                      <div className="detail-item">
                        <span className="detail-label">New Value:</span>
                        <span className="detail-value new">{request.newValue}</span>
                      </div>
                    </div>
                  </div>
                  <div className="request-actions">
                    <button
                      onClick={() => handleApproveRequest(request.id)}
                      className="btn-success"
                      disabled={loading}
                    >
                      ✓ Approve
                    </button>
                    <button
                      onClick={() => handleRejectRequest(request.id)}
                      className="btn-danger"
                      disabled={loading}
                    >
                      ✗ Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default AdminDashboard;
