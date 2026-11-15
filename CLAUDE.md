# CheckedIn — Instructions for Claude Code

Read this file before every task. It explains what we are building, how the
code is organized, and the rules that keep the code easy to read and explain.

## 1. What this project is

CheckedIn is a seat-map attendance system for CIT-U (IT342 G01 individual project).
Professors draw their room or lab layout, seat students on it, and check
attendance by tapping seats. Students see their own attendance and absence hours.

- **Source of truth:** `docs/CheckedIn_SRS.pdf` (Software Requirements Specification).
  Every feature we build comes from it. Requirement IDs look like `FR-031` and `NFR-012`.
- **Build order:** `docs/BUILD_PLAN.md`. Work on ONE phase at a time.
- **Where code goes:** `docs/CODE_GUIDE.md`.

## 2. Tech stack (do not change without asking)

| Part | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3.4, Maven (use the wrapper: `.\mvnw.cmd` in PowerShell) |
| Database | PostgreSQL on Supabase (free plan), Spring Data JPA (Hibernate). Supabase is used ONLY as a database: no Supabase Auth, Storage, or client libraries |
| Frontend | React 19 + Vite, JavaScript (not TypeScript), React Router 7, plain CSS |
| External systems | GitHub Sign-In (one-click login), Brevo (sign-in codes and notification emails), Cloudinary (excuse files) |

Do NOT add: Lombok, TypeScript, Tailwind, Redux, Axios, or any new library
unless I say yes first. If a new library is truly needed, explain why and ask.

## 3. Key business rules (from the SRS)

- Sign-in (Feature 1): two ways, and NO passwords are ever stored.
  1. Email code: user enters an @cit.edu email, Brevo sends a 6-digit code. Codes are saved only
     as a hash, expire after 10 minutes, allow 5 wrong tries, work once, and an email can request
     at most 3 codes every 15 minutes (NFR-008).
  2. GitHub (OAuth App, handled by Spring Security `oauth2Login`): the code-for-token exchange
     happens only on the server (client secret never reaches the browser), the OAuth `state` is
     checked, and the account is accepted only if GitHub's email list (`/user/emails`, scope
     `user:email`) has a VERIFIED address ending in @cit.edu. That email identifies the user;
     the numeric GitHub ID is saved as `githubId`.
  On first sign-in EVERY user (both methods) enters first and last name before the account row
  is saved (FR-003).
  Allowed email domain(s) and the professor email list live in `application.properties` / `.env`.
- Student ID format: `00-0000-000` (regex `^\d{2}-\d{4}-\d{3}$`).
- Attendance statuses and colors: PRESENT = green (P), LATE = yellow (L),
  ABSENT = red (A), EXCUSED = blue (E). Tapping a seat cycles P → L → A → E → P.
- Every new class meeting starts with all ACTIVE students as PRESENT.
- Absence hours = sum of session hours where ABSENT
  + session hours of every 3rd LATE (3rd, 6th, 9th...). EXCUSED counts 0.
- 18 absence hours or more → enrollment becomes DROPPED (back to ACTIVE if edits bring it below 18).
- Warning emails at 12 and 15 hours, and when dropped. Each type sent once per enrollment.
- Layout cell types: SEAT, WORKSTATION, TABLE, AISLE, NO_CHAIR.
  Only SEAT and WORKSTATION can hold a student.
- Rule numbers (3, 18, 12, 15, 7 days) must live in `application.properties`, never hard-coded.

## 4. Code rules (most important)

The code will be explained line by line to the professor, so readability beats cleverness.

1. **Package by feature.** Each feature has its own folder with its controller,
   service, repository, entity, and `dto/` folder. See `docs/CODE_GUIDE.md`.
2. **Thin controllers.** Controllers only receive requests and return responses.
   All rules and logic go in the service.
3. **Clear names.** Full words, no abbreviations: `absenceHours`, not `absHrs`.
   Classes end with their role: `SectionController`, `SectionService`,
   `SectionRepository`, `SectionResponse`, `CreateSectionRequest`.
4. **Comments that explain WHY.** Every class starts with a short comment saying
   what it does and which SRS requirement(s) it covers, e.g. `// Covers FR-030 – FR-035`.
   Add a one-line comment above any method whose purpose is not obvious.
   Do not comment obvious lines.
5. **Small files and methods.** Aim for classes under ~200 lines and methods under ~30 lines.
   If a method gets long, split it into well-named private methods.
6. **DTOs are Java records.** Never return entities directly from controllers.
7. **Write getters/setters by hand** in entities (no Lombok), so nothing is hidden.
8. **Validation** with Jakarta annotations on request DTOs (`@NotBlank`, `@Pattern`, ...).
9. **Errors** go through one global handler (`common/GlobalExceptionHandler`) and return
   `{ "message": "..." }` in plain language that tells the user how to fix it.
10. **Frontend:** one component per file; API calls only in `src/api/`;
    colors and sizes only from `src/styles/theme.css`.
11. **Secrets** (passwords, API keys) only in `.env` files, never in code or git.

## 5. How to work with me

- Before writing code, tell me in 3–5 short bullets what you will create or change.
- Build in small steps. After each step, make sure it compiles/builds.
- After finishing a step, explain what you did in simple words (I'm the one
  presenting this code), and list the files you touched.
- Only build what the SRS asks for. If something is in the SRS "Won't Have" list
  (QR check-in, MS Teams sync, grades, mobile app, admin panel), do not build it.
- When a phase is done, tick its boxes in `docs/BUILD_PLAN.md`.
- Write or update tests for service logic, especially absence-hour computation.

## 6. Commands (Windows)

```bash
# Backend (from the backend folder)
.\mvnw.cmd spring-boot:run      # start the API on http://localhost:8080
.\mvnw.cmd test                 # run backend tests

# Frontend (from the frontend folder)
npm install                   # first time only
npm run dev                   # start the app on http://localhost:5173
npm run lint                  # check code style
npm run build                 # check that it builds
```
