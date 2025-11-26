import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProfile, requestEmailChangeOtp, updateField, updateFields } from '../services/authApi';
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

  const handleUpdateField = async (fieldName, value, otp = null) => {
    if (isAdmin) {
      // Admin: update immediately (single field or batch)
      setLoading(true);
      setError(null);
      try {
        const fieldUpdate = { fieldName, value };
        if (otp) fieldUpdate.otp = otp;
        
        const response = await updateFields(userId, [fieldUpdate]);
        if (response.data && response.data.success) {
          setSuccess(`${fieldName} updated successfully`);
          setEditingField(null);
          loadProfile();
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to update field');
      } finally {
        setLoading(false);
      }
    } else {
      // User: add to pending updates
      setPendingUpdates(prev => ({
        ...prev,
        [fieldName]: { value, otp }
      }));
      setEditingField(null);
      setEditValue('');
    }
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
        const update = { fieldName, value: data.value };
        // Only OTP is needed for email changes
        if (data.otp) update.otp = data.otp;
        return update;
      });

      const response = await updateFields(userId, fieldUpdates);
      if (response.data && response.data.success) {
        const count = fieldUpdates.length;
        setSuccess(`${count} update request(s) created successfully`);
        setPendingUpdates({});
        setEditingField(null);
        setEmailOtpStep(null);
        setEmailOtp('');
        setEmailOtpId(null);
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
    setEmailOtpStep(null);
    setEmailOtp('');
    setEmailOtpId(null);
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
    await handleUpdateField('email', newEmail, emailOtp);
    if (!error) {
      setEmailOtpStep(null);
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

  const hasPendingUpdates = !isAdmin && Object.keys(pendingUpdates).length > 0;

  return (
    <div className="profile-container">
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
                <button onClick={() => handleUpdateField('name', editValue)} className="btn-primary" disabled={loading}>
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
                <button onClick={() => handleUpdateField('phoneNumber', editValue)} className="btn-primary" disabled={loading}>
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

