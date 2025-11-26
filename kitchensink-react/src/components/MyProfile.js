import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyProfile, requestMyEmailChangeOtp, updateMyField, updateMyFields, getMyUpdateRequests, revokeUpdateRequest } from '../services/authApi';
import './MyProfile.css';

function MyProfile() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [editingField, setEditingField] = useState(null);
  const [editValue, setEditValue] = useState('');
  const [editIsdCode, setEditIsdCode] = useState('+91');
  const [emailOtpStep, setEmailOtpStep] = useState(null); // 'request' or 'verify'
  const [emailOtp, setEmailOtp] = useState('');
  const [emailOtpId, setEmailOtpId] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [updateRequests, setUpdateRequests] = useState([]);
  const [showUpdateRequests, setShowUpdateRequests] = useState(false);
  const [pendingUpdates, setPendingUpdates] = useState({}); // Store multiple field changes
  const [fieldErrors, setFieldErrors] = useState({});

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const isAdmin = user.role === 'ADMIN';

  useEffect(() => {
    loadProfile();
    loadUpdateRequests();
  }, []);

  const loadProfile = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getMyProfile();
      if (response.data && response.data.success) {
        setProfile(response.data.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const validateField = (fieldName, value, isdCode = null) => {
    const errors = {};
    
    if (fieldName === 'name') {
      if (!value || value.trim().length === 0) {
        errors.name = 'Name is required';
      } else if (!/^[a-zA-Z\s]+$/.test(value)) {
        errors.name = 'Name must contain only letters and spaces';
      } else if (value.length > 100) {
        errors.name = 'Name must not exceed 100 characters';
      }
    } else if (fieldName === 'email') {
      if (!value || value.trim().length === 0) {
        errors.email = 'Email is required';
      } else if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
        errors.email = 'Email must have a valid format and domain';
      } else if (value.length > 100) {
        errors.email = 'Email must not exceed 100 characters';
      }
    } else if (fieldName === 'phoneNumber') {
      if (!value || value.trim().length === 0) {
        errors.phoneNumber = 'Phone number is required';
      } else if (!/^[6-9]\d{9}$/.test(value)) {
        errors.phoneNumber = 'Phone number must be a valid Indian mobile number (10 digits starting with 6-9)';
      }
      if (!isdCode || (isdCode !== '+91' && isdCode !== '91')) {
        errors.isdCode = 'ISD code must be +91 for Indian numbers';
      }
    } else if (fieldName === 'city') {
      if (value && !/^[a-zA-Z\s]+$/.test(value)) {
        errors.city = 'City must contain only letters and spaces';
      } else if (value && value.length > 50) {
        errors.city = 'City must not exceed 50 characters';
      }
    } else if (fieldName === 'country') {
      if (value && !/^[a-zA-Z\s]+$/.test(value)) {
        errors.country = 'Country must contain only letters and spaces';
      } else if (value && value.length > 50) {
        errors.country = 'Country must not exceed 50 characters';
      }
    } else if (fieldName === 'dateOfBirth') {
      if (value && value.trim().length > 0) {
        const datePattern = /^\d{2}-\d{2}-\d{4}$/;
        if (!datePattern.test(value)) {
          errors.dateOfBirth = 'Date of birth must be in DD-MM-YYYY format';
        } else {
          const [day, month, year] = value.split('-').map(Number);
          const date = new Date(year, month - 1, day);
          const today = new Date();
          const hundredYearsAgo = new Date();
          hundredYearsAgo.setFullYear(today.getFullYear() - 100);
          
          if (isNaN(date.getTime()) || date.getDate() !== day || date.getMonth() !== month - 1 || date.getFullYear() !== year) {
            errors.dateOfBirth = 'Invalid date';
          } else if (date > today) {
            errors.dateOfBirth = 'Date of birth cannot be a future date';
          } else if (date < hundredYearsAgo) {
            errors.dateOfBirth = 'Date of birth cannot be more than 100 years ago';
          }
        }
      }
    }
    
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleUpdateField = async (fieldName, value, isdCode = null) => {
    if (!validateField(fieldName, value, isdCode)) {
      return;
    }
    
    // Add to pending updates
    const updateData = { value };
    if (fieldName === 'phoneNumber' && isdCode) {
      updateData.isdCode = isdCode;
    }
    
    setPendingUpdates(prev => ({
      ...prev,
      [fieldName]: updateData
    }));
    setEditingField(null);
    setEditValue('');
    setEditIsdCode('+91');
    setFieldErrors({});
  };

  const handleSavePendingUpdates = async () => {
    if (Object.keys(pendingUpdates).length === 0) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      // Convert pending updates to array format
      const fieldUpdates = Object.entries(pendingUpdates).map(([fieldName, data]) => {
        const update = { fieldName, value: data.value || data };
        // Special handling for email - only OTP is needed
        if (fieldName === 'email') {
          update.otp = emailOtp;
        }
        // Special handling for phone number - include ISD code
        if (fieldName === 'phoneNumber' && data.isdCode) {
          update.isdCode = data.isdCode;
        }
        return update;
      });

      const response = await updateMyFields(fieldUpdates);
      if (response.data && response.data.success) {
        const count = fieldUpdates.length;
        setSuccess(`${count} update request(s) created successfully`);
        setPendingUpdates({});
        setEditingField(null);
        setEmailOtpStep(null);
        setEmailOtp('');
        setEmailOtpId(null);
        loadProfile();
        loadUpdateRequests();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create update requests');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelPendingUpdates = () => {
    setPendingUpdates({});
    setEditingField(null);
    setEditValue('');
    setEditIsdCode('+91');
    setEmailOtpStep(null);
    setEmailOtp('');
    setEmailOtpId(null);
    setFieldErrors({});
  };


  const handleRequestEmailOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await requestMyEmailChangeOtp(newEmail);
      if (response.data && response.data.success) {
        setEmailOtpId(response.data.data.otpId);
        setEmailOtpStep('verify');
        setSuccess('OTP sent to new email');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleRequestEmailChange = async () => {
    await handleUpdateField('email', newEmail, emailOtp, emailOtpId);
    if (!error) {
      setEmailOtpStep(null);
    }
  };

  const loadUpdateRequests = async () => {
    try {
      const response = await getMyUpdateRequests();
      if (response.data && response.data.success) {
        setUpdateRequests(response.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load update requests:', err);
    }
  };

  const handleRevokeRequest = async (requestId) => {
    if (!window.confirm('Are you sure you want to revoke this update request?')) {
      return;
    }
    
    setLoading(true);
    setError(null);
    try {
      const response = await revokeUpdateRequest(requestId);
      if (response.data && response.data.success) {
        setSuccess('Update request revoked successfully');
        loadUpdateRequests();
        setTimeout(() => setSuccess(null), 3000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to revoke update request');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PENDING':
        return 'status-badge pending';
      case 'APPROVED':
        return 'status-badge approved';
      case 'REJECTED':
        return 'status-badge rejected';
      default:
        return 'status-badge';
    }
  };

  if (loading && !profile) {
    return <div className="my-profile-container">Loading...</div>;
  }

  if (!profile) return null;

  const hasPendingUpdates = Object.keys(pendingUpdates).length > 0;

  return (
    <div className="my-profile-container">
      {hasPendingUpdates && (
        <div className="pending-updates-banner">
          <div className="pending-updates-content">
            <span>📝 You have {Object.keys(pendingUpdates).length} pending change(s): {Object.keys(pendingUpdates).join(', ')}</span>
            <div className="pending-updates-actions">
              <button onClick={handleSavePendingUpdates} className="btn-primary" disabled={loading}>
                {loading ? 'Saving...' : 'Save All Changes'}
              </button>
              <button onClick={handleCancelPendingUpdates} className="btn-secondary" disabled={loading}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
      <div className="profile-header">
        <div className="header-content">
          <h2>👤 My Profile</h2>
          <p className="profile-subtitle">Manage your personal information and account settings</p>
        </div>
        <div className="header-actions">
          {isAdmin && (
            <button onClick={() => navigate('/admin/dashboard')} className="btn-secondary">
              📊 Admin Dashboard
            </button>
          )}
          <button onClick={handleLogout} className="btn-logout">
            🚪 Logout
          </button>
        </div>
      </div>

      {error && <div className="message error">{error}</div>}
      {success && <div className="message success">{success}</div>}

      {/* User Info Card */}
      <div className="user-info-card">
        <div className="user-avatar">
          <span className="avatar-icon">{profile.name?.charAt(0)?.toUpperCase() || 'U'}</span>
        </div>
        <div className="user-info">
          <h3>{profile.name}</h3>
          <p className="user-email">📧 {profile.email}</p>
          <p className="user-role">🏷️ {profile.role}</p>
        </div>
        <div className="user-stats">
          <div className="stat-item">
            <span className="stat-label">Member Since</span>
            <span className="stat-value">{new Date(profile.registrationDate).toLocaleDateString()}</span>
          </div>
        </div>
      </div>

      <div className="profile-card">
        <div className="card-header">
          <h3>📝 Personal Information</h3>
          <p className="card-subtitle">Update your personal details</p>
        </div>
        <div className="profile-field">
          <label>👤 Name:</label>
          {editingField === 'name' ? (
            <div className="edit-group">
              <input
                type="text"
                value={editValue}
                onChange={(e) => {
                  setEditValue(e.target.value);
                  if (fieldErrors.name) {
                    setFieldErrors({ ...fieldErrors, name: '' });
                  }
                }}
                placeholder={profile.name}
                pattern="[a-zA-Z\s]+"
                title="Name must contain only letters and spaces"
                maxLength="100"
                className={fieldErrors.name ? 'error' : ''}
              />
              {fieldErrors.name && <div className="field-error">{fieldErrors.name}</div>}
              <div className="button-group">
                <button onClick={() => { setEditingField(null); setEditValue(''); setFieldErrors({}); }} className="btn-secondary">
                  Cancel
                </button>
                <button onClick={() => handleUpdateField('name', editValue)} className="btn-primary" disabled={loading}>
                  Request Update
                </button>
              </div>
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.name}</span>
              <button onClick={() => { setEditingField('name'); setEditValue(profile.name); }} className="btn-edit">
                ✏️ Edit
              </button>
            </div>
          )}
        </div>

        <div className="profile-field">
          <label>📧 Email:</label>
          {emailOtpStep ? (
            <div className="edit-group">
              {emailOtpStep === 'request' ? (
                <>
                  <input
                    type="email"
                    value={newEmail}
                    onChange={(e) => {
                      setNewEmail(e.target.value);
                      if (fieldErrors.email) {
                        setFieldErrors({ ...fieldErrors, email: '' });
                      }
                    }}
                    placeholder="Enter new email"
                    pattern="[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"
                    title="Email must have a valid format and domain"
                    maxLength="100"
                    className={fieldErrors.email ? 'error' : ''}
                  />
                  {fieldErrors.email && <div className="field-error">{fieldErrors.email}</div>}
                  <div className="button-group">
                    <button onClick={() => { setEmailOtpStep(null); setNewEmail(''); }} className="btn-secondary">
                      Cancel
                    </button>
                    <button onClick={handleRequestEmailOtp} className="btn-primary" disabled={loading}>
                      Request OTP
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <input
                    type="text"
                    value={emailOtp}
                    onChange={(e) => setEmailOtp(e.target.value)}
                    placeholder="Enter OTP"
                    maxLength="6"
                  />
                  <div className="button-group">
                    <button onClick={() => { setEmailOtpStep(null); setEmailOtp(''); }} className="btn-secondary">
                      Cancel
                    </button>
                    <button onClick={handleRequestEmailChange} className="btn-primary" disabled={loading}>
                      Request Change
                    </button>
                  </div>
                </>
              )}
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.email}</span>
              <button onClick={() => { setEmailOtpStep('request'); setNewEmail(''); }} className="btn-edit">
                🔄 Change Email
              </button>
            </div>
          )}
        </div>

        <div className="profile-field">
          <label>📱 Phone Number:</label>
          {editingField === 'phoneNumber' ? (
            <div className="edit-group">
              <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start' }}>
                <div style={{ flex: '0 0 80px' }}>
                  <label style={{ fontSize: '12px', marginBottom: '5px', display: 'block' }}>ISD Code:</label>
                  <input
                    type="text"
                    value={editIsdCode}
                    onChange={(e) => {
                      const value = e.target.value;
                      if (value === '+91' || value === '91' || value === '') {
                        setEditIsdCode(value || '+91');
                        if (fieldErrors.isdCode) {
                          setFieldErrors({ ...fieldErrors, isdCode: '' });
                        }
                      }
                    }}
                    placeholder="+91"
                    maxLength="3"
                    className={fieldErrors.isdCode ? 'error' : ''}
                    style={{ width: '100%' }}
                  />
                  {fieldErrors.isdCode && <div className="field-error" style={{ fontSize: '11px', marginTop: '2px' }}>{fieldErrors.isdCode}</div>}
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ fontSize: '12px', marginBottom: '5px', display: 'block' }}>Phone Number:</label>
                  <input
                    type="text"
                    value={editValue}
                    onChange={(e) => {
                      setEditValue(e.target.value.replace(/\D/g, '').slice(0, 10));
                      if (fieldErrors.phoneNumber) {
                        setFieldErrors({ ...fieldErrors, phoneNumber: '' });
                      }
                    }}
                    placeholder="Enter 10-digit mobile number"
                    pattern="[6-9]\d{9}"
                    title="Phone number must be a valid Indian mobile number (10 digits starting with 6-9)"
                    maxLength="10"
                    className={fieldErrors.phoneNumber ? 'error' : ''}
                    style={{ width: '100%' }}
                  />
                  {fieldErrors.phoneNumber && <div className="field-error" style={{ fontSize: '11px', marginTop: '2px' }}>{fieldErrors.phoneNumber}</div>}
                </div>
              </div>
              <div className="button-group" style={{ marginTop: '10px' }}>
                <button onClick={() => { setEditingField(null); setEditValue(''); setEditIsdCode('+91'); setFieldErrors({}); }} className="btn-secondary">
                  Cancel
                </button>
                <button onClick={() => handleUpdateField('phoneNumber', editValue, editIsdCode || '+91')} className="btn-primary" disabled={loading}>
                  Request Update
                </button>
              </div>
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.isdCode ? `${profile.isdCode} ` : '+91 '}{profile.phoneNumber}</span>
              <button onClick={() => { setEditingField('phoneNumber'); setEditValue(profile.phoneNumber); setEditIsdCode(profile.isdCode || '+91'); }} className="btn-edit">
                ✏️ Edit
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="profile-card">
        <div className="card-header">
          <h3>📍 Additional Information</h3>
          <p className="card-subtitle">Your address and location details</p>
        </div>

        <div className="info-grid">
          <div className="info-item">
            <span className="info-icon">🎂</span>
            <div className="info-content">
              <label>Date of Birth:</label>
              {editingField === 'dateOfBirth' ? (
                <div className="edit-group">
                  <input
                    type="text"
                    value={editValue}
                    onChange={(e) => {
                      let value = e.target.value;
                      // Allow only digits and hyphens, format as DD-MM-YYYY
                      value = value.replace(/[^\d-]/g, '');
                      // Auto-format: DD-MM-YYYY
                      if (value.length <= 2) {
                        // DD
                      } else if (value.length <= 5) {
                        // DD-MM
                        if (value.length === 3 && value[2] !== '-') {
                          value = value.slice(0, 2) + '-' + value.slice(2);
                        }
                      } else if (value.length <= 10) {
                        // DD-MM-YYYY
                        if (value.length === 6 && value[5] !== '-') {
                          value = value.slice(0, 5) + '-' + value.slice(5);
                        }
                      } else {
                        value = value.slice(0, 10);
                      }
                      setEditValue(value);
                      if (fieldErrors.dateOfBirth) {
                        setFieldErrors({ ...fieldErrors, dateOfBirth: '' });
                      }
                    }}
                    placeholder={profile.dateOfBirth || 'DD-MM-YYYY'}
                    pattern="\d{2}-\d{2}-\d{4}"
                    title="Date of birth must be in DD-MM-YYYY format, not be a future date, and not be more than 100 years ago"
                    maxLength="10"
                    className={fieldErrors.dateOfBirth ? 'error' : ''}
                  />
                  {fieldErrors.dateOfBirth && (
                    <div className="field-error">{fieldErrors.dateOfBirth}</div>
                  )}
                  <div className="button-group">
                    <button onClick={() => { setEditingField(null); setEditValue(''); setFieldErrors({}); }} className="btn-secondary">
                      Cancel
                    </button>
                    <button onClick={() => handleUpdateField('dateOfBirth', editValue)} className="btn-primary" disabled={loading}>
                      Request Update
                    </button>
                  </div>
                </div>
              ) : (
                <div className="field-value">
                  <span>{profile.dateOfBirth || 'N/A'}</span>
                  <button onClick={() => { setEditingField('dateOfBirth'); setEditValue(profile.dateOfBirth || ''); }} className="btn-edit">
                    ✏️ Edit
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🏠</span>
            <div className="info-content">
              <label>Address:</label>
              {editingField === 'address' ? (
                <div className="edit-group">
                  <input
                    type="text"
                    value={editValue}
                    onChange={(e) => setEditValue(e.target.value)}
                    placeholder={profile.address || 'Enter address'}
                    maxLength="200"
                  />
                  <div className="button-group">
                    <button onClick={() => { setEditingField(null); setEditValue(''); }} className="btn-secondary">
                      Cancel
                    </button>
                    <button onClick={() => handleUpdateField('address', editValue)} className="btn-primary" disabled={loading}>
                      Request Update
                    </button>
                  </div>
                </div>
              ) : (
                <div className="field-value">
                  <span>{profile.address || 'N/A'}</span>
                  <button onClick={() => { setEditingField('address'); setEditValue(profile.address || ''); }} className="btn-edit">
                    ✏️ Edit
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🏙️</span>
            <div className="info-content">
              <label>City:</label>
              {editingField === 'city' ? (
                <div className="edit-group">
                  <input
                    type="text"
                    value={editValue}
                    onChange={(e) => {
                      const value = e.target.value.replace(/[^a-zA-Z\s]/g, '');
                      setEditValue(value);
                      if (fieldErrors.city) {
                        setFieldErrors({ ...fieldErrors, city: '' });
                      }
                    }}
                    placeholder={profile.city || 'Enter city'}
                    pattern="[a-zA-Z\s]+"
                    title="City must contain only letters and spaces"
                    maxLength="50"
                    className={fieldErrors.city ? 'error' : ''}
                  />
                  {fieldErrors.city && <div className="field-error">{fieldErrors.city}</div>}
                  <div className="button-group">
                    <button onClick={() => { setEditingField(null); setEditValue(''); }} className="btn-secondary">
                      Cancel
                    </button>
                    <button onClick={() => handleUpdateField('city', editValue)} className="btn-primary" disabled={loading}>
                      Request Update
                    </button>
                  </div>
                </div>
              ) : (
                <div className="field-value">
                  <span>{profile.city || 'N/A'}</span>
                  <button onClick={() => { setEditingField('city'); setEditValue(profile.city || ''); }} className="btn-edit">
                    ✏️ Edit
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🌎</span>
            <div className="info-content">
              <label>Country:</label>
              {editingField === 'country' ? (
                <div className="edit-group">
                  <input
                    type="text"
                    value={editValue}
                    onChange={(e) => {
                      const value = e.target.value.replace(/[^a-zA-Z\s]/g, '');
                      setEditValue(value);
                      if (fieldErrors.country) {
                        setFieldErrors({ ...fieldErrors, country: '' });
                      }
                    }}
                    placeholder={profile.country || 'Enter country'}
                    pattern="[a-zA-Z\s]+"
                    title="Country must contain only letters and spaces"
                    maxLength="50"
                    className={fieldErrors.country ? 'error' : ''}
                  />
                  {fieldErrors.country && <div className="field-error">{fieldErrors.country}</div>}
                  <div className="button-group">
                    <button onClick={() => { setEditingField(null); setEditValue(''); }} className="btn-secondary">
                      Cancel
                    </button>
                    <button onClick={() => handleUpdateField('country', editValue)} className="btn-primary" disabled={loading}>
                      Request Update
                    </button>
                  </div>
                </div>
              ) : (
                <div className="field-value">
                  <span>{profile.country || 'N/A'}</span>
                  <button onClick={() => { setEditingField('country'); setEditValue(profile.country || ''); }} className="btn-edit">
                    ✏️ Edit
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">📅</span>
            <div className="info-content">
              <label>Registration Date:</label>
              <span>{new Date(profile.registrationDate).toLocaleDateString()}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Update Requests Section - Only for regular users, not admins */}
      {!isAdmin && (
      <div className="profile-card">
        <div className="card-header">
          <div className="card-header-content">
            <h3>📋 My Update Requests</h3>
            <p className="card-subtitle">Track the status of your profile update requests</p>
          </div>
          <button 
            onClick={() => {
              setShowUpdateRequests(!showUpdateRequests);
              if (!showUpdateRequests) {
                loadUpdateRequests();
              }
            }} 
            className="btn-toggle"
          >
            {showUpdateRequests ? '▼ Hide' : '▶ Show'} ({updateRequests.length})
          </button>
        </div>

        {showUpdateRequests && (
          <div className="update-requests-section">
            {loading && updateRequests.length === 0 ? (
              <div className="loading">Loading requests...</div>
            ) : updateRequests.length === 0 ? (
              <div className="no-requests-message">
                <p>📭 No update requests found</p>
                <p className="sub-message">When you request changes to your profile, they will appear here.</p>
              </div>
            ) : (
              <div className="requests-list">
                {updateRequests.map((request) => (
                  <div key={request.id} className="request-card">
                    <div className="request-info">
                      <div className="request-header">
                        <div className="request-title">
                          <strong>📝 {request.fieldName ? request.fieldName.charAt(0).toUpperCase() + request.fieldName.slice(1) : 'Update Request'}</strong>
                          <span className={getStatusBadgeClass(request.status)}>
                            {request.status}
                          </span>
                        </div>
                        <span className="request-date">
                          {new Date(request.requestedAt).toLocaleString()}
                        </span>
                      </div>
                      <div className="request-details">
                        <div className="detail-item">
                          <span className="detail-label">Old Value:</span>
                          <span className="detail-value old">{request.oldValue || 'N/A'}</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">New Value:</span>
                          <span className="detail-value new">{request.newValue || 'N/A'}</span>
                        </div>
                        {request.status === 'REJECTED' && request.rejectionReason && (
                          <div className="detail-item">
                            <span className="detail-label">Rejection Reason:</span>
                            <span className="detail-value rejected">{request.rejectionReason}</span>
                          </div>
                        )}
                        {request.status === 'APPROVED' && request.reviewedAt && (
                          <div className="detail-item">
                            <span className="detail-label">Approved On:</span>
                            <span className="detail-value">{new Date(request.reviewedAt).toLocaleString()}</span>
                          </div>
                        )}
                      </div>
                    </div>
                    {request.status === 'PENDING' && (
                      <div className="request-actions">
                        <button
                          onClick={() => handleRevokeRequest(request.id)}
                          className="btn-revoke"
                          disabled={loading}
                          title="Revoke this pending request"
                        >
                          ❌ Revoke
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
      )}
    </div>
  );
}

export default MyProfile;

