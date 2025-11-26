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
  const [editIsdCode, setEditIsdCode] = useState('+91');
  const [emailOtpStep, setEmailOtpStep] = useState(null); // 'request' or 'verify'
  const [emailOtp, setEmailOtp] = useState('');
  const [emailOtpId, setEmailOtpId] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [pendingUpdates, setPendingUpdates] = useState({});
  const [fieldErrors, setFieldErrors] = useState({});

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

  const handleUpdateField = async (fieldName, value, otp = null, isdCode = null) => {
    if (!validateField(fieldName, value, isdCode)) {
      return;
    }
    
    if (isAdmin) {
      // Admin: update immediately (single field or batch)
      setLoading(true);
      setError(null);
      try {
        const fieldUpdate = { fieldName, value };
        if (otp) fieldUpdate.otp = otp;
        if (fieldName === 'phoneNumber' && isdCode) {
          fieldUpdate.isdCode = isdCode;
        }
        
        const response = await updateFields(userId, [fieldUpdate]);
        if (response.data && response.data.success) {
          setSuccess(`${fieldName} updated successfully`);
          setEditingField(null);
          setEditValue('');
          setEditIsdCode('+91');
          setFieldErrors({});
          loadProfile();
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to update field');
      } finally {
        setLoading(false);
      }
    } else {
      // User: add to pending updates
      const updateData = { value, otp };
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
        // Include ISD code for phone number updates
        if (fieldName === 'phoneNumber' && data.isdCode) {
          update.isdCode = data.isdCode;
        }
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
                <button onClick={() => handleUpdateField('phoneNumber', editValue, null, editIsdCode || '+91')} className="btn-primary" disabled={loading}>
                  {isAdmin ? 'Update' : 'Request Update'}
                </button>
              </div>
            </div>
          ) : (
            <div className="field-value">
              <span>{profile.isdCode ? `${profile.isdCode} ` : '+91 '}{profile.phoneNumber}</span>
              {isOwnProfile && (
                <button onClick={() => { setEditingField('phoneNumber'); setEditValue(profile.phoneNumber); setEditIsdCode(profile.isdCode || '+91'); }} className="btn-edit">
                  Edit
                </button>
              )}
            </div>
          )}
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

