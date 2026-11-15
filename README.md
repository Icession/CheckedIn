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
- **External systems:** GitHub Sign-In (one-click login), Brevo (sign-in codes and notification emails), Cloudinary (excuse files)

## What you need installed

- JDK 17 or newer (`java -version`)
- Node.js 20 or newer (`node -v`)
- A free Supabase account (https://supabase.com)
- A CIT-U email address (`@cit.edu`) for signing in
- A GitHub account (only for "Sign in with GitHub")
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

## Set up Brevo for emails (one time)

CheckedIn emails users their 6-digit sign-in code (FR-001) and, later, absence warnings and
excuse results (FR-064 – FR-069). It sends them through **Brevo**, an email service with a free plan.

If a Brevo screen looks different from these steps, follow Brevo's own guides:
[Create a sender](https://help.brevo.com/hc/en-us/articles/208836149-Create-a-new-sender-From-name-and-From-email),
[Create an API key](https://help.brevo.com/hc/en-us/articles/209467485-Create-and-manage-your-API-keys).

### Step 1: Choose the sender address

The sender is the address the emails come "from". Use **an address made just for CheckedIn**,
for example a new Gmail account such as `checkedin.yourname@gmail.com`, so your personal address
is not shown to students. You must be able to open its inbox once, to verify it.

- **Don't use your `@cit.edu` address as the sender.** Only CIT-U's IT can allow Brevo to send
  emails for `cit.edu`. Without that, Outlook may treat the email as a fake CIT-U email.
- Brevo cannot prove that a free address like Gmail belongs to it, so it sends from one of its
  own `brevosend.com` addresses instead and keeps the name **CheckedIn**. In our test (Nov 2026),
  Outlook showed **From: CheckedIn** `<yourname@12345678.brevosend.com>` and the email arrived
  in the Inbox. That is normal.
  Sending from a domain you own gives the best delivery, but it costs money and is optional.

### Step 2: Create the account and verify the sender

1. Sign up for a free account at https://www.brevo.com. If Brevo asks you to complete your
   profile or confirm your account, do that first.
2. Open the **Senders** settings (Brevo groups them with domains and IPs) and add a sender:
   - **From name:** `CheckedIn`
   - **From email:** the address from Step 1
3. Brevo emails a code or a link to that address. Enter the code (or open the link) to verify it.

### Step 3: Create an API key

1. Open the API key settings. Brevo's [quickstart](https://developers.brevo.com/docs/quickstart)
   has a direct link.
2. On the **API keys** tab, click **Generate a new API key** and name it `CheckedIn backend`.
3. **Copy the key right away. Brevo shows it only once.** Make sure it is an **API key**,
   not an SMTP key.

### Step 4: Add the settings to `backend/.env`

Copy the Brevo lines from `backend/.env.example` into your `backend/.env` and fill them in:

```properties
BREVO_API_KEY=the key from Step 3
BREVO_SENDER_EMAIL=the verified address from Step 1
BREVO_SENDER_NAME=CheckedIn
```

The API key is a secret. Keep it only in `backend/.env`, which git ignores (NFR-011).
Without these settings the app still runs; it just cannot send emails.

### Step 5: Send a test email

In PowerShell, from the `backend` folder (keep the double quotes, PowerShell needs them):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --checkedin.test-email-to=your.name@cit.edu"
```

Replace `your.name@cit.edu` with your own CIT-U address. The app starts, sends one email,
prints a line that starts with **TEST EMAIL SENT** (or **TEST EMAIL NOT SENT** and the reason),
and stops.

Then open your CIT-U Outlook:

- **In the Inbox:** Brevo works.
- **In Junk Email:** open it and mark it as **Not junk**. The sign-in page will remind users to
  check Junk if a code doesn't arrive (SRS 1.6).
- **Not there after a few minutes:** see Troubleshooting below.

## Set up GitHub sign-in (one time)

Users can also sign in with GitHub (FR-001) if their GitHub account has their `@cit.edu`
address added **and verified** (FR-002). For that, CheckedIn needs a GitHub **OAuth App**, which
gives a client ID and a client secret.

If a GitHub screen looks different from these steps, follow GitHub's own guide:
[Creating an OAuth app](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app).

### Step 1: Register the OAuth App

1. On GitHub, click your profile picture (top right) → **Settings** → **Developer settings**
   (bottom of the left sidebar) → **OAuth Apps** → **New OAuth App**. If it is your first app,
   the button says **Register a new application**.
2. Fill in:
   - **Application name:** `CheckedIn`
   - **Homepage URL:** `http://localhost:5173`
   - **Authorization callback URL:** `http://localhost:5173/login/oauth2/code/github` (exactly this)
3. Click **Register application**.

### Step 2: Copy the client ID and create a client secret

1. On the app's page, copy the **Client ID**.
2. Generate a new client secret and **copy it right away**. GitHub may not show it again.

### Step 3: Add them to `backend/.env`

Copy the GitHub lines from `backend/.env.example` into your `backend/.env` and fill them in:

```properties
GITHUB_CLIENT_ID=the Client ID from Step 2
GITHUB_CLIENT_SECRET=the client secret from Step 2
```

The client secret stays only in `backend/.env` (NFR-011). The browser never sees it: the backend
itself swaps GitHub's one-time code for a token (NFR-008). Restart the backend after the change.

### Step 4: Add your `@cit.edu` email on GitHub

On GitHub: **Settings** → **Emails** → add your `@cit.edu` address, then click the link in the
verification email GitHub sends you. It does not need to be your primary or public address, but it
must be **verified**: CheckedIn never trusts an unverified address.

### Step 5: Try it

Open http://localhost:5173, click **Sign in with GitHub**, and approve CheckedIn on GitHub's page.
You come back signed in with your `@cit.edu` email. Next time, GitHub skips the approval page.

When the frontend is deployed (Phase 12), add its address as another callback URL
(`https://<your-site>/login/oauth2/code/github`). GitHub allows several callback URLs per app.

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

Open http://localhost:5173 to sign in: enter your `@cit.edu` email, then the 6-digit code that
arrives by email (Brevo must be set up first, see above), or click **Sign in with GitHub**
(see "Set up GitHub sign-in").

To check the setup, open http://localhost:5173/setup. You should see **Backend: UP** and **Database: UP**.

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
| New values in `.env` are ignored | Restart the backend and `npm run dev`. Both read `.env` only when they start |
| `TEST EMAIL NOT SENT: Brevo is not set up` | Add `BREVO_API_KEY` and `BREVO_SENDER_EMAIL` to `backend/.env` (see "Set up Brevo"), then run the test again |
| `Brevo rejected the email (401 UNAUTHORIZED)` | The API key is wrong: create a new **API key** (not an SMTP key) and paste it without spaces or quotes. If the backend log says Brevo doesn't recognize your IP address, authorize it from Brevo's email or under your name → **Security** → **Authorized IPs** |
| `Brevo rejected the email (400 BAD_REQUEST)` | Usually `BREVO_SENDER_EMAIL` is not a verified sender. Compare it with the address on Brevo's Senders page. The log line just above shows Brevo's own message |
| `Brevo rejected the email` with any other code | Read Brevo's own message in the log line just above it. New Brevo accounts sometimes need their profile completed or the account confirmed first |
| `Could not reach Brevo` | Check your internet connection and try again |
| The test email is in **Junk Email** | Mark it as **Not junk**. For better delivery, send from a domain you own and authenticate it in Brevo (optional) |
| Sign-in page says "Please use your CIT-U email address." | Only `@cit.edu` addresses can sign in (FR-002) |
| Sign-in page says "We couldn't send the code right now" | Brevo could not send the email. The backend log shows why; see the Brevo rows above |
| Sign-in page says "You asked for 3 codes in the last 15 minutes" | Use the newest code you received, or wait 15 minutes. Only the newest code works |
| You sign in, but after reloading the page you are signed out again | The session cookie is marked Secure. Chrome, Edge and Firefox should keep it on `http://localhost`; some browsers (such as Safari) may not. Try Chrome or Edge, or for local testing only add `SESSION_COOKIE_SECURE=false` to `backend/.env`. Never turn it off on the deployed site |
| GitHub says the `redirect_uri` is not associated with this application | The OAuth App's callback URL must be exactly `http://localhost:5173/login/oauth2/code/github`, and open the app at `http://localhost:5173` (not `127.0.0.1`) |
| Clicking **Sign in with GitHub** shows a GitHub error page (often a 404) | `GITHUB_CLIENT_ID` is missing or wrong in `backend/.env` (until it is set, the backend uses a placeholder). Restart the backend after changing it |
| After GitHub, the sign-in page says "Please use your CIT-U email address. Add your @cit.edu email…" | Your GitHub account has no **verified** `@cit.edu` address. Add it under GitHub **Settings** → **Emails** and click the link in GitHub's verification email |
| After GitHub, the sign-in page says "GitHub sign-in did not finish" | Try again. If it keeps happening, read the backend log line that starts with `GitHub sign-in failed`; a wrong `GITHUB_CLIENT_SECRET` shows up there |
