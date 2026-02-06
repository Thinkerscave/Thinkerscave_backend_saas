#!/bin/bash

# ThinkersCave Backend - Quick Setup Script
# This script helps you set up the environment variables for the application

set -e  # Exit on error

echo "🚀 ThinkersCave Backend - Environment Setup"
echo "============================================"
echo ""

# Check if .env already exists
if [ -f ".env" ]; then
    echo "⚠️  .env file already exists!"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Setup cancelled. Existing .env file preserved."
        exit 0
    fi
fi

# Copy template
echo "📋 Creating .env file from template..."
cp .env.example .env

echo ""
echo "✅ .env file created!"
echo ""
echo "📝 Now you need to configure the following:"
echo ""

# Database Configuration
echo "1️⃣  DATABASE CONFIGURATION"
echo "   Current: DB_URL=jdbc:postgresql://localhost:5432/thinkerscave_saas"
read -p "   Enter your database URL (or press Enter to keep default): " db_url
if [ ! -z "$db_url" ]; then
    sed -i '' "s|DB_URL=.*|DB_URL=$db_url|" .env
fi

read -p "   Enter database username (default: postgres): " db_user
if [ ! -z "$db_user" ]; then
    sed -i '' "s|DB_USERNAME=.*|DB_USERNAME=$db_user|" .env
fi

read -sp "   Enter database password: " db_pass
echo ""
if [ ! -z "$db_pass" ]; then
    sed -i '' "s|DB_PASSWORD=.*|DB_PASSWORD=$db_pass|" .env
fi

echo ""
echo "2️⃣  JWT CONFIGURATION"
echo "   Generating secure JWT secret..."
if command -v openssl &> /dev/null; then
    jwt_secret=$(openssl rand -base64 64 | tr -d '\n')
    sed -i '' "s|JWT_SECRET=.*|JWT_SECRET=$jwt_secret|" .env
    echo "   ✅ JWT secret generated and configured"
else
    echo "   ⚠️  OpenSSL not found. Please manually generate a JWT secret:"
    echo "   Run: openssl rand -base64 64"
    echo "   Then update JWT_SECRET in .env file"
fi

echo ""
echo "3️⃣  EMAIL CONFIGURATION"
read -p "   Enter email address (for sending emails): " email_user
if [ ! -z "$email_user" ]; then
    sed -i '' "s|MAIL_USERNAME=.*|MAIL_USERNAME=$email_user|" .env
fi

read -sp "   Enter email password (Gmail App Password recommended): " email_pass
echo ""
if [ ! -z "$email_pass" ]; then
    sed -i '' "s|MAIL_PASSWORD=.*|MAIL_PASSWORD=$email_pass|" .env
fi

echo ""
echo "4️⃣  CORS CONFIGURATION"
echo "   Current: http://localhost:3000,http://localhost:4200"
read -p "   Enter allowed origins (comma-separated, or press Enter to keep default): " cors_origins
if [ ! -z "$cors_origins" ]; then
    sed -i '' "s|ALLOWED_ORIGINS=.*|ALLOWED_ORIGINS=$cors_origins|" .env
fi

echo ""
echo "✅ Environment setup complete!"
echo ""
echo "📌 Next steps:"
echo "   1. Review .env file and make any additional changes"
echo "   2. Ensure PostgreSQL is running"
echo "   3. Create database: CREATE DATABASE thinkerscave_saas;"
echo "   4. Run the application: ./mvnw spring-boot:run"
echo ""
echo "📚 For more information, see:"
echo "   - README.md - Setup instructions"
echo "   - SECURITY_DEPLOYMENT.md - Security and deployment guide"
echo ""
echo "🔒 IMPORTANT: Never commit .env file to version control!"
echo ""
