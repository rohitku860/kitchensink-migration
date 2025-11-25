import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { requestLoginOtp, verifyOtpAndLogin } from '../services/authApi';
import './Login.css';

function Login() {
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [step, setStep] = useState('email'); // 'email' or 'otp'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const navigate = useNavigate();

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await requestLoginOtp(email);
      if (response.data && response.data.success) {
        setSuccess('OTP sent to your email');
        setStep('otp');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await verifyOtpAndLogin(email, otp);
      if (response.data && response.data.success) {
        const { token, userId, role, email: userEmail, name } = response.data.data;
        
        // Store token and user info
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify({ userId, role, email: userEmail, name }));
        
        // Redirect based on role
        if (role === 'ADMIN') {
          navigate('/admin/dashboard');
        } else {
          navigate('/my-profile');
        }
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    setStep('email');
    setOtp('');
    setError(null);
    setSuccess(null);
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Login</h2>
        {step === 'email' ? (
          <form onSubmit={handleRequestOtp}>
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="Enter your email"
              />
            </div>
            {error && <div className="message error">{error}</div>}
            {success && <div className="message success">{success}</div>}
            <button type="submit" disabled={loading} className="btn-primary">
              {loading ? 'Sending...' : 'Send OTP'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp}>
            <div className="form-group">
              <label htmlFor="otp">Enter OTP</label>
              <input
                type="text"
                id="otp"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                required
                placeholder="Enter 6-digit OTP"
                maxLength="6"
              />
              <small>OTP sent to {email}</small>
            </div>
            {error && <div className="message error">{error}</div>}
            {success && <div className="message success">{success}</div>}
            <div className="button-group">
              <button type="button" onClick={handleBack} className="btn-secondary">
                Back
              </button>
              <button type="submit" disabled={loading} className="btn-primary">
                {loading ? 'Verifying...' : 'Verify & Login'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default Login;

