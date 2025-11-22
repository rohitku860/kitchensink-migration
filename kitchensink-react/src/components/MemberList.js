import React, { useState } from 'react';
import MemberEditModal from './MemberEditModal';
import api from '../services/api';

const MemberList = ({
  members,
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
  onUpdate,
  onDelete,
  loading,
  isSearchResults,
}) => {
  const [editingMember, setEditingMember] = useState(null);


  const handleEdit = (member) => {
    setEditingMember(member);
  };

  const handleUpdate = async (memberData) => {
    await onUpdate(editingMember.id, memberData);
    setEditingMember(null);
  };

  const handleCancelEdit = () => {
    setEditingMember(null);
  };

  const handleRestUrlClick = async (e, url) => {
    e.preventDefault();
    try {
      // Extract the endpoint path from the full URL
      const urlObj = new URL(url.startsWith('http') ? url : `http://localhost:8081${url}`);
      const path = urlObj.pathname;
      
      // Use axios to fetch with API key in headers
      const response = await api.get(path.replace('/kitchensink/v1/members', ''));
      
      // Open new window with formatted JSON
      const newWindow = window.open('', '_blank');
      if (newWindow) {
        newWindow.document.write(`
          <html>
            <head>
              <title>REST API Response</title>
              <style>
                body {
                  font-family: monospace;
                  padding: 20px;
                  background-color: #f5f5f5;
                }
                pre {
                  background-color: #fff;
                  padding: 15px;
                  border-radius: 5px;
                  border: 1px solid #ddd;
                  overflow-x: auto;
                }
              </style>
            </head>
            <body>
              <h2>REST API Response</h2>
              <p><strong>URL:</strong> ${url}</p>
              <pre>${JSON.stringify(response.data, null, 2)}</pre>
            </body>
          </html>
        `);
        newWindow.document.close();
      }
    } catch (error) {
      alert(`Error fetching data: ${error.response?.data?.message || error.message}`);
    }
  };

  const handleAllMembersUrlClick = async (e) => {
    e.preventDefault();
    try {
      // Use axios to fetch all members with API key in headers
      const response = await api.get('', {
        params: {
          page: 0,
          size: 100,
          sort: 'name,asc'
        }
      });
      
      // Open new window with formatted JSON
      const newWindow = window.open('', '_blank');
      if (newWindow) {
        newWindow.document.write(`
          <html>
            <head>
              <title>REST API Response - All Members</title>
              <style>
                body {
                  font-family: monospace;
                  padding: 20px;
                  background-color: #f5f5f5;
                }
                pre {
                  background-color: #fff;
                  padding: 15px;
                  border-radius: 5px;
                  border: 1px solid #ddd;
                  overflow-x: auto;
                }
              </style>
            </head>
            <body>
              <h2>REST API Response - All Members</h2>
              <p><strong>URL:</strong> /kitchensink/v1/members</p>
              <pre>${JSON.stringify(response.data, null, 2)}</pre>
            </body>
          </html>
        `);
        newWindow.document.close();
      }
    } catch (error) {
      alert(`Error fetching data: ${error.response?.data?.message || error.message}`);
    }
  };

  if (loading && members.length === 0) {
    return (
      <div className="table-section">
        <h2>Members</h2>
        <div className="loading">Loading members...</div>
      </div>
    );
  }

  return (
    <>
      <div className="table-section">
        <h2>Members {isSearchResults && <span style={{ fontSize: '14px', color: '#666' }}>(Search Results)</span>}</h2>
        {members.length === 0 ? (
          <div className="empty-state">
            <em>No registered members.</em>
          </div>
        ) : (
          <>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone #</th>
                    <th>REST URL</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {members.map((member) => (
                    <tr key={member.id}>
                      <td>{member.id}</td>
                      <td>{member.name}</td>
                      <td>{member.email}</td>
                      <td>{member.phoneNumber}</td>
                      <td>
                        <a 
                          href={`http://localhost:8081${member.restUrl || `/kitchensink/v1/members/${member.id}`}`}
                          onClick={(e) => handleRestUrlClick(e, `http://localhost:8081${member.restUrl || `/kitchensink/v1/members/${member.id}`}`)}
                          style={{ color: '#0066cc', textDecoration: 'underline', cursor: 'pointer' }}
                        >
                          {member.restUrl || `/kitchensink/v1/members/${member.id}`}
                        </a>
                      </td>
                      <td>
                        <div className="action-buttons">
                          <button
                            className="btn btn-secondary btn-small"
                            onClick={() => handleEdit(member)}
                            disabled={loading}
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-danger btn-small"
                            onClick={() => onDelete(member.id)}
                            disabled={loading}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: '10px', borderTop: '2px solid #ddd' }}>
                      REST URL for all members: <a 
                        href="http://localhost:8081/kitchensink/v1/members"
                        onClick={(e) => handleAllMembersUrlClick(e)}
                        style={{ color: '#0066cc', textDecoration: 'underline', cursor: 'pointer' }}
                      >
                        /kitchensink/v1/members
                      </a>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            {!isSearchResults && (
              <div className="pagination">
                <button
                  onClick={() => onPageChange(currentPage - 1)}
                  disabled={currentPage === 0 || loading}
                >
                  Previous
                </button>
                <span className="pagination-info">
                  Page {currentPage + 1} of {totalPages || 1} ({totalElements} total)
                </span>
                <button
                  onClick={() => onPageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1 || loading}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {editingMember && (
        <MemberEditModal
          member={editingMember}
          onUpdate={handleUpdate}
          onCancel={handleCancelEdit}
          loading={loading}
        />
      )}
    </>
  );
};

export default MemberList;

