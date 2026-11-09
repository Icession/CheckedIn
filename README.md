# CheckedIn

A seat-map attendance system for CIT-U classes.
Professors draw their room or laboratory, seat their students, and check
attendance by tapping seats (green = present, yellow = late, red = absent,
blue = excused). Students see their own attendance and absence hours.

IT342 – Systems Integration and Architecture 1 · G01 · Kurt Carcueva

| | |
|---|---|
| Requirements | [`docs/CheckedIn_SRS.pdf`](docs/CheckedIn_SRS.pdf) |
| Build progress | [`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) |
| Where the code is | [`docs/CODE_GUIDE.md`](docs/CODE_GUIDE.md) |
| Diagrams | [`docs/diagrams/`](docs/diagrams/) |

## Tech stack

- **Backend:** Java 17, Spring Boot 3.3, Maven, Spring Data JPA
- **Database:** PostgreSQL on Supabase (free plan)
- **Frontend:** React 19, Vite, React Router
- **External systems:** Microsoft Entra ID (sign-in), Cloudinary (excuse files), Brevo (email)

## What you need installed

- JDK 17 or newer (`java -version`)
- Node.js 20 or newer (`node -v`)
- A free Supabase account (https://supabase.com)
- A CIT-U Microsoft account (`@cit.edu`) for signing in
- Git

Maven does not need to be installed: the project includes the Maven Wrapper (`.\mvnw.cmd`).

## Set up the database (one time)

1. Go to https://supabase.com/dashboard and click **New project**.
2. Name it `CheckedIn`, pick region **Southeast Asia (Singapore)**, and set a database
   password. Save the password somewhere safe.
3. When the project is ready, click **Connect** at the top and choose **Session pooler**.
   (Use the session pooler, not "Direct connection", because direct needs IPv6, which
   many home networks don't have.)
4. In the `backend` folder, copy `.env.example` to `.env` and fill in:
   - `DB_URL` → `jdbc:postgresql://<host>:5432/postgres?sslmode=require`
     (replace `<host>` with the pooler host, e.g. `aws-1-ap-southeast-1.pooler.supabase.com`)
   - `DB_USERNAME` → the user shown, e.g. `postgres.abcdefghijklmnop`
   - `DB_PASSWORD` → the password from step 2

Tables are created automatically by the backend (Hibernate) the first time it runs.
Free Supabase projects pause after about a week without use. If that happens, open the
dashboard and click **Restore project**.

## Set up Microsoft sign-in (one time)

Users sign in with their CIT-U Microsoft account (FR-001). For that, CheckedIn must be
registered once in **Microsoft Entra ID**, Microsoft's sign-in service. Registering gives
a **Client ID** that both the frontend and the backend need.

If a screen looks different from these steps, follow Microsoft's own guide:
[Register an application](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app).

### Step 1: Decide where to register the app

Sign in to https://entra.microsoft.com with your `@cit.edu` account, go to
**Entra ID** > **App registrations**, and check whether **New registration** works.
If the admin center or App registrations page won't open, the button is greyed out, or you
get a permission error, you are in situation B.

| Situation | Where to register | Supported account types | Tenant ID to use |
|---|---|---|---|
| **A.** New registration works with your CIT-U account | CIT-U's tenant | **Single tenant only** (CIT-U) | CIT-U's |
| **B.** You are not allowed to register apps at CIT-U | A Microsoft Entra tenant you control (ask your instructor or CIT-U IT if you don't have one) | **Multiple Entra ID tenants** | CIT-U's |
| **C.** Sign-in later shows **"Need admin approval"** and CIT-U IT has not approved the app yet | Same as B | Same as B | Your own tenant's, with test accounts from it |

- In situations A and B, real CIT-U accounts sign in.
- Situation C is the fallback the SRS allows for development (SRS 2.4, "a test Microsoft tenant").
  The test accounts don't end in `@cit.edu`, so the allowed email domain must be changed too
  (step 1d adds that setting).
- Never choose an option that includes **Personal Microsoft accounts**. Only school accounts may sign in.

### Step 2: Register the app

1. In **Entra ID** > **App registrations**, click **New registration**.
2. **Name:** `CheckedIn`. Users see this name when they sign in.
3. **Supported account types:** the option from the table in Step 1.
4. If the page has a **Redirect URI** section, leave it empty (Step 3 adds it). Click **Register**.
5. On the **Overview** page that opens, copy the **Application (client) ID**. This is the Client ID.

### Step 3: Add the redirect URI as a single-page application

After sign-in, Microsoft sends the user back to this address, so it must match exactly.

1. In your app, under **Manage**, click **Authentication**.
2. On the **Redirect URI configuration** tab, click **Add Redirect URI**.
3. Choose the **Single-page application** tile (**not** Web).
4. Enter `http://localhost:5173` (no slash at the end) and click **Configure**.

Why "single-page application": the React app signs in with Microsoft's MSAL library, which uses
the authorization code flow with PKCE. Microsoft allows that flow from a browser only for this
platform. If you see implicit grant checkboxes (**Access tokens**, **ID tokens**), leave them
unticked, because that older flow is not used.

When the frontend is deployed (Phase 12), add its address here the same way.

### Step 4: Put the user's name and email in the sign-in token

By default, Microsoft's ID token does not include the first and last name, and does not always
include the email. CheckedIn needs them to create the account (FR-003) and to compare the last
name with the class list (FR-007).

1. Under **Manage**, click **Token configuration**.
2. Click **Add optional claim** and choose the token type **ID**.
3. Tick **email**, **family_name**, and **given_name**, then click **Add**.
4. If a box asks to turn on the Microsoft Graph permissions these claims need, tick it and click **Add**.

### Step 5: Copy the IDs into the `.env` files

| Value | Where to find it | `backend/.env` | `frontend/.env` |
|---|---|---|---|
| Client ID | App's **Overview** page → **Application (client) ID** | `ENTRA_CLIENT_ID` | `VITE_ENTRA_CLIENT_ID` |
| Tenant ID | Already in `.env.example`: `823cde44-4433-456d-b801-bdf0ab3d41fc` (CIT-U) | `ENTRA_TENANT_ID` | `VITE_ENTRA_TENANT_ID` |

- If you don't have the `.env` files yet, copy each folder's `.env.example` to `.env`.
  If you already have them, copy only the new Entra lines (`ENTRA_...` in the backend,
  `VITE_ENTRA_...` in the frontend) into them. Don't overwrite
  `backend/.env`, because it holds your database password.
- After changing a `.env` file, restart the backend and `npm run dev`. Both read `.env` only when they start.

**Which tenant ID?** Always the tenant where the users' accounts live, which is CIT-U in situations
A and B. In situation B, the **Directory (tenant) ID** on your app's Overview page is *your*
tenant, not CIT-U's, so don't copy that one. To double-check CIT-U's tenant ID, open
https://login.microsoftonline.com/cit.edu/v2.0/.well-known/openid-configuration. The ID is the
long code inside `"issuer"`.

**Why the tenant ID matters:** in step 1d, the backend will accept only sign-in tokens issued by
this tenant. That is the main protection. Microsoft warns that the email in a token "isn't
guaranteed to be correct", so the `@cit.edu` check (FR-002) is a second check on top of it,
not the only one.

### Why there is no client secret

The app registration also has a **Certificates & secrets** page. CheckedIn doesn't use it:

- The React app runs in the user's browser, where anyone can read the code, so it cannot keep
  a secret. Instead, MSAL uses PKCE: a one-time code made fresh for each sign-in.
- The backend never logs in to Microsoft with a password. It only checks the ID token's
  signature with Microsoft's **public** keys (NFR-008).
- The client ID and tenant ID are not secrets either, because they are visible in the browser.
  They are in `.env` only so that local and deployed setups can use different values.

NFR-014 says a Microsoft client secret must be kept in environment variables. This sign-in flow
simply doesn't need one.

## Run the project (Windows)

**1. Backend**

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs on http://localhost:8080. Test it: http://localhost:8080/api/health

**2. Frontend** (in a second terminal)

```bash
cd frontend
npm install                 # first time only
npm run dev
```

Open http://localhost:5173. You should see **Backend: UP** and **Database: UP**.

## Run the tests

```bash
cd backend
.\mvnw.cmd test
```

## Troubleshooting

| Problem | Fix |
|---|---|
| Frontend says "Cannot reach the backend" | Start the backend first and check it says `Started CheckedInApplication` |
| Backend fails with `password authentication failed` | Wrong `DB_PASSWORD` or `DB_USERNAME` in `backend/.env` (the username includes your project ref) |
| Backend fails with `Connection refused`, `UnknownHost`, or `Connection attempt failed` | Check `DB_URL`, use the Session pooler host, check your internet, and make sure the Supabase project is not paused |
| Backend fails with `Could not resolve placeholder 'DB_URL'` | `backend/.env` is missing. Copy it from `.env.example` |
| `JAVA_HOME is not set` | Install JDK 17+ and set `JAVA_HOME` to its folder |
| Port 8080 already in use | Stop the other app, or set `SERVER_PORT=8081` in `backend/.env` and update `frontend/vite.config.js` |
| Microsoft shows **"Need admin approval"** | CIT-U lets only its IT admins approve new apps. Ask CIT-U IT to approve "CheckedIn", or use situation C in "Set up Microsoft sign-in" until they do |
| Microsoft says the redirect URI **does not match** | On the app's **Authentication** page, the redirect URI must be exactly `http://localhost:5173`, under **Single-page application** |
| Error that mentions **"Single-Page Application"** or **cross-origin** | The redirect URI was added under **Web**. Remove it and add it again under **Single-page application** |
| Microsoft says the application **was not found in the directory** | Check the client ID in both `.env` files. In situation B, the app must use **Multiple Entra ID tenants**; if it was registered as "Single tenant only", register it again |
| New values in `.env` are ignored | Restart the backend and `npm run dev`. Both read `.env` only when they start |
| Signed in, but the name or email is missing | Do Step 4 of "Set up Microsoft sign-in" (optional claims) |
