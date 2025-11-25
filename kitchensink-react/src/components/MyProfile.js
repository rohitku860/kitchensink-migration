import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyProfile, updateMyPhoneNumber, requestMyEmailChangeOtp, requestMyEmailChange, raiseMyUpdateRequest, getMyUpdateRequests, revokeUpdateRequest } from '../services/authApi';
import './MyProfile.css';

function MyProfile() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [editingField, setEditingField] = useState(null);
  const [editValue, setEditValue] = useState('');
  const [emailOtpStep, setEmailOtpStep] = useState(null); // 'request' or 'verify'
  const [emailOtp, setEmailOtp] = useState('');
  const [emailOtpId, setEmailOtpId] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [updateRequests, setUpdateRequests] = useState([]);
  const [showUpdateRequests, setShowUpdateRequests] = useState(false);

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

  const handleUpdatePhone = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await updateMyPhoneNumber(editValue);
      if (response.data && response.data.success) {
        setSuccess('Update request created successfully');
        setEditingField(null);
        loadProfile();
        loadUpdateRequests(); // Reload requests after creating new one
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create update request');
    } finally {
      setLoading(false);
    }
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
    setLoading(true);
    setError(null);
    try {
      const response = await requestMyEmailChange(newEmail, emailOtp, emailOtpId);
      if (response.data && response.data.success) {
        setSuccess('Email change request created successfully');
        setEmailOtpStep(null);
        loadProfile();
        loadUpdateRequests(); // Reload requests after creating new one
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create email change request');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateName = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await raiseMyUpdateRequest('name', editValue);
      if (response.data && response.data.success) {
        setSuccess('Update request created successfully');
        setEditingField(null);
        loadUpdateRequests(); // Reload requests after creating new one
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create update request');
    } finally {
      setLoading(false);
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

  return (
    <div className="my-profile-container">
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
                onChange={(e) => setEditValue(e.target.value)}
                placeholder={profile.name}
              />
              <div className="button-group">
                <button onClick={() => { setEditingField(null); setEditValue(''); }} className="btn-secondary">
                  Cancel
                </button>
                <button onClick={handleUpdateName} className="btn-primary" disabled={loading}>
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
                    onChange={(e) => setNewEmail(e.target.value)}
                    placeholder="Enter new email"
                  />
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
              <input
                type="text"
                value={editValue}
                onChange={(e) => setEditValue(e.target.value.replace(/\D/g, ''))}
                placeholder={profile.phoneNumber}
                pattern="[0-9]{10,15}"
                maxLength="15"
              />
              <div className="button-group">
                <button onClick={() => { setEditingField(null); setEditValue(''); }} className="btn-secondary">
                  Cancel
                </button>
                <button onClick={handleUpdatePhone} className="btn-primary" disabled={loading}>
                  Request Update
                </button>
              </div>
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.isdCode ? `${profile.isdCode} ` : ''}{profile.phoneNumber}</span>
              <button onClick={() => { setEditingField('phoneNumber'); setEditValue(profile.phoneNumber); }} className="btn-edit">
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
            <span className="info-icon">🌍</span>
            <div className="info-content">
              <label>ISD Code:</label>
              <span>{profile.isdCode || 'N/A'}</span>
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🎂</span>
            <div className="info-content">
              <label>Date of Birth:</label>
              <span>{profile.dateOfBirth || 'N/A'}</span>
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🏠</span>
            <div className="info-content">
              <label>Address:</label>
              <span>{profile.address || 'N/A'}</span>
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🏙️</span>
            <div className="info-content">
              <label>City:</label>
              <span>{profile.city || 'N/A'}</span>
            </div>
          </div>

          <div className="info-item">
            <span className="info-icon">🌎</span>
            <div className="info-content">
              <label>Country:</label>
              <span>{profile.country || 'N/A'}</span>
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

      {/* Update Requests Section */}
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
    </div>
  );
}

export default MyProfile;

