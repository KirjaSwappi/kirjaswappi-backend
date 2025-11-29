# SOLUTION: Admin Users Not Found

## Root Cause ✅
**Spring Data MongoDB was NOT properly configured to use your database name!**

From your logs:
```
Connected to database: test
Collection 'admin_users' does NOT exist in database 'test'
```

Even though you set `MONGODB_DATABASE=kirjaswappi`, the application was connecting to `test` instead.

## Why This Happened

Your `DatabaseConfig.java` was creating a `MongoClient` but **NOT explicitly configuring Spring Data MongoDB** to use the database name from `spring.data.mongodb.database`.

When Spring Data MongoDB isn't explicitly told which database to use via a `MongoDatabaseFactory` bean, it falls back to:
1. The database name in the MongoDB URI (if present)
2. Or the default database name `test`

Your configuration was missing:
- `MongoDatabaseFactory` bean that explicitly sets the database name
- `MongoTemplate` bean that uses that factory

So even though you had `MONGODB_DATABASE=kirjaswappi` set, Spring Data MongoDB wasn't using it!

## The Fix Applied ✅

I've updated your `DatabaseConfig.java` to **explicitly configure** Spring Data MongoDB with the correct database:

**What Changed:**
1. Added `MongoDatabaseFactory` bean that explicitly uses `spring.data.mongodb.database`
2. Added `MongoTemplate` bean that uses the factory
3. Added logging to show which database is being used
4. Made these beans `@Primary` to ensure they're used

**The key fix:**
```java
@Bean
@Primary
public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
    // Explicitly set the database name, ignoring any database in the URI
    return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
}
```

This ensures that **regardless of what's in the URI**, Spring Data MongoDB will use the database name from `MONGODB_DATABASE` environment variable.

**You don't need to change your environment variables!** Your existing configuration will now work correctly.

## How to Verify

After making the change, restart your application and look for these log lines:

```
MONGODB_DATABASE env var: kirjaswappi
Actually connected to database: kirjaswappi
Collection 'admin_users' exists: true
Documents in 'admin_users' collection: 2
AdminUserRepository.findAll() returned: 2 users
```

## What You'll See Now

When you restart your application, you'll see these new log lines:

```
MongoDB Configuration:
  - URI hosts: [db.kirjaswappi.fi:27017]
  - Database from URI: test (or null)
  - Configured database name: kirjaswappi
Creating MongoDatabaseFactory with database: kirjaswappi
```

Then the debug listener will show:
```
MONGODB_DATABASE env var: kirjaswappi
Actually connected to database: kirjaswappi  ← This should now match!
Collection 'admin_users' exists: true
Documents in 'admin_users' collection: 2
```

## Expected Result After Fix

Once you restart the application, you should see:
- ✅ Application connects to `kirjaswappi` database (not `test`)
- ✅ `findAll()` returns 2 admin users
- ✅ All admin endpoints work correctly
- ✅ No more empty results

## Technical Details

The problem was that Spring Boot's auto-configuration for MongoDB was creating a `MongoTemplate` that was using the database name from the URI (or defaulting to `test`) rather than using the value from `spring.data.mongodb.database`.

By explicitly creating a `MongoDatabaseFactory` bean with `@Primary`, we ensure that Spring Data MongoDB uses the correct database name for all operations.

