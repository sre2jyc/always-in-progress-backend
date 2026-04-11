## Authentication Design

TaskFlow uses **JWT-based stateless authentication** built with **Spring Security**, **BCrypt**, and a custom request filter.

### How it works

#### 1. User Registration

* Users register via `POST /auth/register`.
* Passwords are hashed using **BCrypt** (strength 12) before being stored.
* Raw passwords are never persisted.

#### 2. Login

* Users log in via `POST /auth/login`.
* Email + password are validated against stored credentials.
* On success, the backend generates a **JWT access token** containing:

  * `userId`
  * `email`
* Token expiry is set to **24 hours**, as required.

#### 3. Protected Routes

* All non-auth endpoints require:

  ```text id="auth1"
  Authorization: Bearer <token>
  ```
* A custom JWT filter intercepts each request:

  * extracts token from header
  * validates signature and expiry
  * extracts `userId`
  * sets authenticated user in Spring Security context

#### 4. Authorization

Business rules are enforced at the service layer:

* only project owners can update/delete projects
* only project owners / task creators can delete tasks

This keeps:

```text id="auth2"
authentication = identity verification
authorization = business access control
```

---

## Why this approach

### Chosen because:

* simple and widely used for REST APIs
* stateless → easy to scale horizontally
* no server-side session storage needed
* fits Docker / containerized deployment well

---

## Tradeoffs / Limitations

### Current tradeoffs

* **No refresh token flow**
  Users must log in again after token expiry.

* **JWT invalidation is limited**
  If a token is leaked, it remains valid until expiry.

* **No RBAC / roles yet**
  Current permissions are ownership-based.

* **No device / session tracking**
  Cannot revoke a single device session.

---

## Better options / future improvements

### 1. Refresh token flow

* short-lived access tokens
* long-lived refresh tokens
* safer and more user-friendly

### 2. Token blacklist / revocation

* store revoked JWT IDs in Redis
* immediate logout support

### 3. Role-Based Access Control (RBAC)

* owner / editor / viewer roles
* finer project permissions

### 4. OAuth / SSO

* Google / GitHub login
* enterprise-friendly auth


---

