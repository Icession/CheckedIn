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
- **External systems:** Google Sign-In (one-click login), Brevo (sign-in codes and notification emails), Cloudinary (excuse files)

## What you need installed

- JDK 17 or newer (`java -version`)
- Node.js 20 or newer (`node -v`)
- A free Supabase account (https://supabase.com)
- A CIT-U email address (`@cit.edu`) for signing in
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
| New values in `.env` are ignored | Restart the backend and `npm run dev`. Both read `.env` only when they start |
