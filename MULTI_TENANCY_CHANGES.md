# Multi-Tenancy Fixes - Code Change Notes

## Overview
These changes ensure that tenant schemas are created via the organization registration API, required seed roles exist per tenant, and user registration inserts into the correct tenant schema.

## What Changed
- User registration now sets the tenant context and saves directly into the target schema.
- Schema initialization now always registers the tenant datasource and runs the SQL initializer (safe to re-run).
- Seed data for roles (Admin/User) is inserted during schema initialization.

## Files Updated

### 1) `src/main/java/com/thinkerscave/common/orgm/service/SchemaInitializer.java`
- `createAndInitializeSchema(...)` now:
  - Creates schema only if missing.
  - Always configures the tenant datasource.
  - Always runs the schema SQL script (tables + seed data).

### 2) `src/main/resources/schema_initializer.sql`
- Added seed inserts for `role_master`:
  - `Admin` role
  - `User` role
- Inserts are idempotent via `ON CONFLICT (role_code) DO NOTHING`.

### 3) `src/main/java/com/thinkerscave/common/usrm/service/impl/UserServiceImpl.java`
- `registerUser(...)` now:
  - Defaults `schemaName` to `public` if missing.
  - Initializes schema if non-public.
  - Sets `TenantContext` before save and clears after.
  - Saves the user directly into the tenant schema.

## Flow Now
1. `POST /api/org/register`  - optional
   - Creates schema, registers datasource, creates tables, seeds roles.
   - payload ex:
   -
{                                                                                                                                                                                
"isAGroup": false,                                                                                                                                                             
"parentOrgId": null,                                                                                                                                                           
"orgName": "Thinkers Cave Academy",                                                                                                                                            
"brandName": "ThinkersCave",                                                                                                                                                   
"orgUrl": "https://thinkerscave.example",                                                                                                                                      
"orgType": "SCHOOL",                                                                                                                                                           
"city": "Angul",
"state": "Odisha",                                                                                                                                                             
"establishDate": "2015-06-15",                                                                                                                                                 
"subscriptionType": "BASIC",                                                                                                                                                   
"schemaName": "org_thinkerscave",
"ownerName": "Amit Mohanty",                                                                                                                                                   
"ownerEmail": "amit.mohanty@example.com",                                                                                                                                      
"ownerMobile": "9012345678"                                                                                                                                                    
}
2. `POST /api/v1/users/register` with `schemaName` - directly can use also
   - Inserts user into that schema.

