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
  navigate,
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

  const handleRestUrlClick = (e, url) => {
    e.preventDefault();
    // Extract the endpoint path from the full URL
    const urlObj = new URL(url.startsWith('http') ? url : `http://localhost:8081${url}`);
    const path = urlObj.pathname;
    
    // Navigate to the API response viewer route with encoded path
    // This creates a proper URL that can be bookmarked and reloaded
    const encodedPath = encodeURIComponent(path);
    navigate(`/api/${encodedPath}`);
  };

  const handleAllMembersUrlClick = (e) => {
    e.preventDefault();
    // Navigate to the API response viewer route for all members
    const path = '/kitchensink/v1/members?page=0&size=100&sort=name,asc';
    const encodedPath = encodeURIComponent(path);
    navigate(`/api/${encodedPath}`);
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

