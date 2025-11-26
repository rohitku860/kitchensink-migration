# Caching Strategy Plan

## Analysis

### 1. Profile by ID (`GET /v1/profile/{userId}`)
- **Cacheable:** ✅ YES
- **Strategy:** Direct cache with userId as key
- **Cache Key:** `userId`
- **Eviction:** On user update/delete
- **Data Size:** Single user object (~2-5 KB)

### 2. Admin Users List with Pagination (`GET /v1/admin/users?page=0&size=10`)
- **Challenge:** 
  - Each page request is different (page number, size, sort)
  - Cache key would be: `page:0:size:10:sort:name`
  - If user updated, need to invalidate ALL pages
  - With 1000 users, 10 per page = 100 different cache entries
- **Strategy:** ❌ DON'T cache the page itself
  - Instead: Cache individual users by ID
  - When fetching a page, individual users may already be in cache
  - Benefits indirectly through user cache
- **Data Size:** If cached, each page = 10 users × 5 KB = 50 KB × 100 pages = 5 MB

### 3. Search Users (`GET /v1/admin/users/search?name=john`)
- **Challenge:**
  - Infinite possible search terms
  - Each search term = different cache entry
  - Results change when users created/updated/deleted
  - Cache would grow unbounded
- **Strategy:** ❌ DON'T cache search results
  - Instead: Cache individual users by ID
  - When search returns results, individual users may already be in cache
  - Benefits indirectly through user cache
- **Data Size:** If cached, could be thousands of entries

### 4. Role Lookups (`getRoleById`, `getRoleNameByUserId`)
- **Cacheable:** ✅ YES
- **Strategy:** Cache roles (stable data, rarely changes)
- **Cache Key:** `roleId` or `userId:roleName`
- **Eviction:** Only when roles are modified (rare)
- **Data Size:** Small (~1 KB per role)

## Recommended Implementation

### Phase 1: Direct Caching (High Value, Low Risk)
1. ✅ **User by ID** - `getUserById()`
   - Cache key: `userId`
   - Evict on: create, update, delete
   - Benefits: Profile API, and indirectly helps pagination/search

2. ✅ **Role by ID** - `getRoleById()`
   - Cache key: `roleId`
   - Evict on: role update (rare)

3. ✅ **Role Name by User ID** - `getRoleNameByUserId()`
   - Cache key: `userId:roleName`
   - Evict on: user role change, user delete

### Phase 2: Skip (Not Recommended)
- ❌ **Pagination results** - Too many cache entries, complex invalidation
- ❌ **Search results** - Unbounded cache growth, complex invalidation

## Cache Configuration

Update cache names to match current entities:
- `userById` (instead of `memberById`)
- `roleById` (new)
- `roleNameByUserId` (new)

Keep `members` cache for backward compatibility or remove if not used.

## Cache Eviction Strategy

```java
// On user create/update/delete
@CacheEvict(value = "userById", key = "#id")
@CacheEvict(value = "roleNameByUserId", key = "#id + ':roleName'")

// On role update (rare)
@CacheEvict(value = "roleById", key = "#id")
```

## Expected Benefits

1. **Profile API:** 100% cache hit after first access (until eviction)
2. **Pagination:** Partial benefit - users already in cache won't need DB lookup
3. **Search:** Partial benefit - users already in cache won't need DB lookup
4. **Role Lookups:** 100% cache hit (roles rarely change)

## Cache Size Estimation

- **User cache:** 1000 users × 5 KB = 5 MB (max)
- **Role cache:** 10 roles × 1 KB = 10 KB (max)
- **Role name cache:** 1000 users × 0.5 KB = 500 KB (max)
- **Total:** ~5.5 MB (well within 1000 entry limit per cache)

