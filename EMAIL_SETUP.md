# Email Configuration Guide

## Gmail Setup (Current Configuration)

The application is currently configured to use Gmail SMTP. To send emails, you need to set up Gmail App Password.

### Step 1: Enable 2-Step Verification
1. Go to https://myaccount.google.com/
2. Click on **Security** in the left sidebar
3. Under "Signing in to Google", enable **2-Step Verification** if not already enabled

### Step 2: Generate App Password
1. Go back to **Security** page
2. Under "Signing in to Google", click on **App passwords**
3. Select app: **Mail**
4. Select device: **Other (Custom name)** and enter "Kitchensink App"
5. Click **Generate**
6. Copy the 16-character password (it will look like: `abcd efgh ijkl mnop`)

### Step 3: Configure Credentials

#### Option A: Environment Variables (Recommended for Production)
Set these environment variables before starting the application:

**Linux/Mac:**
```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-16-char-app-password
```

**Windows (Command Prompt):**
```cmd
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-16-char-app-password
```

**Windows (PowerShell):**
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-char-app-password"
```

#### Option B: application.properties (For Development Only)
⚠️ **WARNING: Never commit passwords to version control!**

Edit `application.properties`:
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
```

#### Option C: application-local.properties (Recommended for Local Development)
Create a file `src/main/resources/application-local.properties` (this file should be in `.gitignore`):

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-char-app-password
```

Then run with profile:
```bash
java -jar -Dspring.profiles.active=local your-app.jar
```

### Step 4: Verify Configuration
The application will use these settings:
- **Host:** smtp.gmail.com
- **Port:** 587
- **Protocol:** SMTP with STARTTLS
- **Authentication:** Enabled

## Alternative Email Providers

### Outlook/Office 365
```properties
spring.mail.host=smtp.office365.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### Yahoo Mail
```properties
spring.mail.host=smtp.mail.yahoo.com
spring.mail.port=587
spring.mail.username=your-email@yahoo.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### SendGrid (Recommended for Production)
```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=your-sendgrid-api-key
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Amazon SES
```properties
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=your-ses-smtp-username
spring.mail.password=your-ses-smtp-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Testing Email Configuration

After setting up credentials, test by:
1. Starting the application
2. Triggering an email (e.g., login OTP request)
3. Check application logs for:
   - `Email sent successfully to: ...` (success)
   - `Failed to send email to: ...` (error - check credentials)

## Troubleshooting

### Common Issues:

1. **"Authentication failed"**
   - Verify you're using App Password, not regular password
   - Check if 2-Step Verification is enabled
   - Ensure username is correct (full email address)

2. **"Connection timeout"**
   - Check firewall settings
   - Verify SMTP host and port are correct
   - Check if your network blocks port 587

3. **"Could not connect to SMTP host"**
   - Verify `spring.mail.host` is correct
   - Check if port 587 is accessible
   - Try port 465 with SSL instead:
     ```properties
     spring.mail.port=465
     spring.mail.properties.mail.smtp.ssl.enable=true
     spring.mail.properties.mail.smtp.starttls.enable=false
     ```

4. **"Email sending is disabled"**
   - Check `app.email.enabled=true` in properties
   - Or set `EMAIL_ENABLED=true` environment variable

## Security Best Practices

1. ✅ **Never commit passwords to Git**
   - Use environment variables
   - Use `.gitignore` for local config files
   - Use secrets management in production (AWS Secrets Manager, HashiCorp Vault, etc.)

2. ✅ **Use App Passwords, not regular passwords**
   - More secure
   - Can be revoked independently
   - Doesn't expose your main account password

3. ✅ **Rotate passwords regularly**
   - Generate new App Passwords periodically
   - Revoke old ones when no longer needed

4. ✅ **Use dedicated email accounts for applications**
   - Don't use personal email accounts
   - Create service accounts for production

## Current Configuration

The application is configured to read from:
- Environment variables: `MAIL_USERNAME` and `MAIL_PASSWORD`
- Fallback to: `application.properties` if env vars not set
- Default username: `rohitku860@gmail.com` (if nothing is set)

To override, set environment variables or update `application.properties`.

