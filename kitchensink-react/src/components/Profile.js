import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProfile, updateName, updatePhoneNumber, requestEmailChangeOtp, updateEmail, raiseUpdateRequest } from '../services/authApi';
import './Profile.css';

function Profile() {
  const { userId } = useParams();
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

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const isAdmin = user.role === 'ADMIN';
  const isOwnProfile = user.userId === userId;

  useEffect(() => {
    loadProfile();
  }, [userId]);

  const loadProfile = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getProfile(userId);
      if (response.data && response.data.success) {
        setProfile(response.data.data);
      }
    } catch (err) {
      if (err.response?.status === 403) {
        setError('You do not have permission to access this profile');
      } else {
        setError(err.response?.data?.message || 'Failed to load profile');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePhone = async () => {
    if (isAdmin) {
      // Admin can update directly
      setLoading(true);
      try {
        const response = await updatePhoneNumber(userId, editValue);
        if (response.data && response.data.success) {
          setSuccess('Phone number updated successfully');
          setEditingField(null);
          loadProfile();
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to update phone number');
      } finally {
        setLoading(false);
      }
    } else {
      // User must raise request
      setLoading(true);
      try {
        const response = await raiseUpdateRequest(userId, 'phoneNumber', editValue);
        if (response.data && response.data.success) {
          setSuccess('Update request created successfully');
          setEditingField(null);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to create update request');
      } finally {
        setLoading(false);
      }
    }
  };

  const handleRequestEmailOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await requestEmailChangeOtp(userId, newEmail);
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

  const handleUpdateEmail = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await updateEmail(userId, newEmail, emailOtp, emailOtpId);
      if (response.data && response.data.success) {
        if (isAdmin) {
          setSuccess('Email updated successfully');
          setEmailOtpStep(null);
          loadProfile();
        } else {
          setSuccess('Email change request created successfully');
          setEmailOtpStep(null);
        }
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update email');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateName = async () => {
    if (isAdmin) {
      // Admin can update directly
      setLoading(true);
      try {
        const response = await updateName(userId, editValue);
        if (response.data && response.data.success) {
          setSuccess('Name updated successfully');
          setEditingField(null);
          loadProfile();
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to update name');
      } finally {
        setLoading(false);
      }
    } else {
      // User must raise request
      setLoading(true);
      try {
        const response = await raiseUpdateRequest(userId, 'name', editValue);
        if (response.data && response.data.success) {
          setSuccess('Update request created successfully');
          setEditingField(null);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to create update request');
      } finally {
        setLoading(false);
      }
    }
  };

  if (loading && !profile) {
    return <div className="profile-container">Loading...</div>;
  }

  if (error && !profile) {
    return (
      <div className="profile-container">
        <div className="message error">{error}</div>
        <button onClick={() => navigate(isAdmin ? '/admin/dashboard' : '/profile/' + user.userId)}>
          Go Back
        </button>
      </div>
    );
  }

  if (!profile) return null;

  return (
    <div className="profile-container">
      <div className="profile-header">
        <div className="header-content">
          <h2>👤 User Profile</h2>
          {isAdmin && (
            <p className="profile-subtitle">Viewing profile for User ID: {userId}</p>
          )}
        </div>
        {isAdmin && (
          <div className="header-actions">
            <button 
              onClick={() => navigate('/admin/dashboard')} 
              className="btn-secondary"
            >
              📊 Back to Dashboard
            </button>
          </div>
        )}
      </div>

      {error && <div className="message error">{error}</div>}
      {success && <div className="message success">{success}</div>}

      <div className="profile-card">
        <div className="profile-field">
          <label>Name:</label>
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
                  {isAdmin ? 'Update' : 'Request Update'}
                </button>
              </div>
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.name}</span>
              {isOwnProfile && (
                <button onClick={() => { setEditingField('name'); setEditValue(profile.name); }} className="btn-edit">
                  Edit
                </button>
              )}
            </div>
          )}
        </div>

        <div className="profile-field">
          <label>Email:</label>
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
                    <button onClick={handleUpdateEmail} className="btn-primary" disabled={loading}>
                      {isAdmin ? 'Update Email' : 'Request Change'}
                    </button>
                  </div>
                </>
              )}
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.email}</span>
              {isOwnProfile && (
                <button onClick={() => { setEmailOtpStep('request'); setNewEmail(''); }} className="btn-edit">
                  Change Email
                </button>
              )}
            </div>
          )}
        </div>

        <div className="profile-field">
          <label>Phone Number:</label>
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
                  {isAdmin ? 'Update' : 'Request Update'}
                </button>
              </div>
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.isdCode ? `${profile.isdCode} ` : ''}{profile.phoneNumber}</span>
              {isOwnProfile && (
                <button onClick={() => { setEditingField('phoneNumber'); setEditValue(profile.phoneNumber); }} className="btn-edit">
                  Edit
                </button>
              )}
            </div>
          )}
        </div>

        <div className="profile-field">
          <label>ISD Code:</label>
          <span>{profile.isdCode || 'N/A'}</span>
        </div>

        <div className="profile-field">
          <label>Date of Birth:</label>
          <span>{profile.dateOfBirth || 'N/A'}</span>
        </div>

        <div className="profile-field">
          <label>Address:</label>
          <span>{profile.address || 'N/A'}</span>
        </div>

        <div className="profile-field">
          <label>City:</label>
          <span>{profile.city || 'N/A'}</span>
        </div>

        <div className="profile-field">
          <label>Country:</label>
          <span>{profile.country || 'N/A'}</span>
        </div>

        <div className="profile-field">
          <label>Role:</label>
          <span>{profile.role}</span>
        </div>

        <div className="profile-field">
          <label>Registration Date:</label>
          <span>{new Date(profile.registrationDate).toLocaleDateString()}</span>
        </div>
      </div>
    </div>
  );
}

export default Profile;

