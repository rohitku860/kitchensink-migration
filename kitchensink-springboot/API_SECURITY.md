# API Security Configuration

## API Key Authentication

The API is protected by API key authentication. All requests to the API endpoints must include a valid API key in the request header.

### How It Works

1. **API Key Header**: All requests must include `X-API-Key` header with a valid API key
2. **Validation**: The API key is validated against the configured key in `application.properties`
3. **Public Endpoints**: Actuator, Swagger, and API docs endpoints are excluded from API key validation

### Configuration

#### Set API Key

**Option 1: Environment Variable (Recommended for Production)**
```bash
export API_KEY=your-secure-api-key-here
```

**Option 2: application.properties**
```properties
app.api.key=your-secure-api-key-here
app.api.enabled=true
```

**Option 3: Disable API Key (Development Only)**
```properties
app.api.enabled=false
```

### Making API Requests

#### Using cURL
```bash
curl -H "X-API-Key: your-secure-api-key-here" \
     -H "Content-Type: application/json" \
     http://localhost:8081/api/v1/members
```

#### Using Postman
1. Add header: `X-API-Key: your-secure-api-key-here`
2. Make your request

#### Using JavaScript/Fetch
```javascript
fetch('http://localhost:8081/api/v1/members', {
  method: 'GET',
  headers: {
    'X-API-Key': 'your-secure-api-key-here',
    'Content-Type': 'application/json'
  }
})
```

#### Using Axios
```javascript
axios.get('http://localhost:8081/api/v1/members', {
  headers: {
    'X-API-Key': 'your-secure-api-key-here'
  }
})
```

### CORS Configuration

The API is configured to accept requests only from allowed origins:

```properties
app.api.allowed-origins=http://localhost:3000,http://localhost:8080
```

**For Production:**
- Set specific frontend URLs
- Remove localhost origins
- Use HTTPS URLs only

### Public Endpoints (No API Key Required)

- `/actuator/*` - Health checks and metrics
- `/swagger-ui.html` - Swagger UI
- `/api-docs` - OpenAPI documentation
- `/v3/api-docs` - OpenAPI JSON

### Error Responses

**Missing API Key:**
```json
{
  "error": "API key is required. Please provide X-API-Key header.",
  "code": "UNAUTHORIZED"
}
```
HTTP Status: `401 Unauthorized`

**Invalid API Key:**
```json
{
  "error": "Invalid API key.",
  "code": "UNAUTHORIZED"
}
```
HTTP Status: `401 Unauthorized`

### Security Best Practices

1. **Use Strong API Keys**: Generate random, long, unique API keys
2. **Environment Variables**: Never commit API keys to version control
3. **Rotate Keys**: Regularly rotate API keys
4. **HTTPS Only**: Always use HTTPS in production
5. **Rate Limiting**: Already implemented (60 requests/minute per IP)
6. **CORS**: Restrict allowed origins to your frontend domains only

### Generating Secure API Keys

**Using OpenSSL:**
```bash
openssl rand -hex 32
```

**Using UUID:**
```bash
uuidgen
```

**Using Python:**
```python
import secrets
secrets.token_urlsafe(32)
```

### Example: Complete Request

```bash
curl -X POST http://localhost:8081/api/v1/members \
  -H "X-API-Key: your-secure-api-key-here" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: optional-correlation-id" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "1234567890"
  }'
```

### Testing API Key

**Test with valid key:**
```bash
curl -H "X-API-Key: your-secure-api-key-here" \
     http://localhost:8081/api/v1/members
```

**Test without key (should fail):**
```bash
curl http://localhost:8081/api/v1/members
```

**Test with invalid key (should fail):**
```bash
curl -H "X-API-Key: wrong-key" \
     http://localhost:8081/api/v1/members
```

