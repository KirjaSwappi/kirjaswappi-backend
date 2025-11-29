#!/bin/bash

# Quick test script to verify MongoDB database configuration

echo "=== Testing MongoDB Configuration Fix ==="
echo ""

# Start the application in cloud profile
echo "Starting application with cloud profile..."
cd /Users/mak/Desktop/git/kirjaswappi/kirjaswappi-backend

# Run with cloud profile
mvn spring-boot:run -Dspring-boot.run.profiles=cloud 2>&1 | grep -E "(MongoDB Configuration|Connected to database|admin_users|AdminUserRepository)" | head -20

echo ""
echo "=== What to Look For ==="
echo "✅ 'Configured database name: kirjaswappi'"
echo "✅ 'Creating MongoDatabaseFactory with database: kirjaswappi'"
echo "✅ 'Actually connected to database: kirjaswappi'"
echo "✅ 'Collection admin_users exists: true'"
echo "✅ 'Documents in admin_users collection: 2'"
echo "✅ 'AdminUserRepository.findAll() returned: 2 users'"
echo ""
echo "❌ If you see 'Connected to database: test' - something is still wrong"
echo ""

