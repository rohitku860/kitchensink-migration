import React, { useState } from 'react';

const SearchMembers = ({ onSearch, onClear, loading }) => {
  const [searchName, setSearchName] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (searchName.trim()) {
      onSearch(searchName.trim());
    }
  };

  const handleClear = () => {
    setSearchName('');
    onClear();
  };

  return (
    <div className="form-section">
      <h2>Search Members</h2>
      <p>Search by name (case-insensitive).</p>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="searchName">Search by Name:</label>
          <input
            type="text"
            id="searchName"
            name="searchName"
            value={searchName}
            onChange={(e) => setSearchName(e.target.value)}
            placeholder="Enter name to search"
            disabled={loading}
          />
        </div>

        <div className="button-group">
          <button type="submit" className="btn btn-primary" disabled={loading || !searchName.trim()}>
            {loading ? 'Searching...' : 'Search'}
          </button>
          <button type="button" className="btn btn-secondary" onClick={handleClear} disabled={loading}>
            Clear
          </button>
        </div>
      </form>
    </div>
  );
};

export default SearchMembers;

