# Build Plan — CheckedIn

We build one phase at a time. Each phase ends with something you can run and
show. Tick the boxes as you go. Requirement IDs (FR-xxx) come from
`docs/CheckedIn_SRS.pdf`.

**How to start a phase in Claude Code:** copy the "Ask Claude Code" line of the
phase. Claude Code reads `CLAUDE.md` automatically, so it already knows the rules.

---

## Phase 0 — Project setup ✅ (done together with Claude)

Goal: backend and frontend run and talk to each other.

- [x] Folder structure, `CLAUDE.md`, `README.md`, docs
- [x] Spring Boot backend with `GET /api/health` (checks the database)
- [x] React frontend home page that shows backend and database status
- [x] **You:** create a Supabase project, then create `backend/.env` from `.env.example` with its connection details
- [x] **You:** run backend and frontend, see "Backend: UP" and "Database: UP"
- [x] **You:** `git init`, first commit, push to GitHub

---

## Phase 1 — Users and sign-in (Feature 1: FR-001 – FR-009) ✅

Goal: professors and students sign in with an email code or GitHub and land on their own page.

> **Change of plan (Nov 9):** Microsoft sign-in was dropped because CIT-U's Microsoft tenant
> requires IT admin approval ("Need admin approval"). Sign-in is now an emailed code (Brevo) or
> Google. The SRS was updated to match; FR numbers did not change.
>
> **Change of plan (Nov 12, SRS v1.2):** Google was replaced by **GitHub** on the instructor's
> advice (a Google app in testing mode only allows listed test users). Everyone now enters their
> name on first sign-in. FR numbers did not change.

Steps (do them one at a time):
- [x] 1a. `auth/User` entity, `Role` enum, `UserRepository`, `GlobalExceptionHandler`
- [x] 1b. ~~Register the app in Microsoft Entra ID~~ (dropped, see note above)
- [x] 1c. Remove Microsoft sign-in: drop the uncommitted MSAL work, remove `@azure/msal-browser`,
      the Entra README section and `ENTRA_*` settings. In `User`, replace `msObjectId` with an
      optional `googleSubject`, and find users by email (see ERD)
- [x] 1d. Brevo: account + verified sender (README guide), `integration/EmailService` + `BrevoEmailService`.
      Send one test email to your @cit.edu inbox and check it doesn't land in Junk
- [x] 1e. Email-code sign-in (FR-001, FR-002, NFR-008): `LoginCode` entity, `LoginCodeService`,
      `POST /api/auth/code` and `POST /api/auth/code/verify`, sign-in page (email, then code),
      HttpOnly session cookie for 8 hours (library: `spring-boot-starter-security`)
- [x] 1f. GitHub sign-in (FR-001, FR-002, NFR-008): GitHub OAuth App with client ID and secret
      (README guide), "Sign in with GitHub" button that goes to `/oauth2/authorization/github`,
      Spring Security `oauth2Login` (library: `spring-boot-starter-oauth2-client`),
      `integration/GitHubIdentityProvider` reads `/user/emails` and accepts only a verified
      @cit.edu email; `User.googleSubject` becomes `githubId` (Long). Same session cookie as 1e
- [x] 1g. Create the account on first sign-in after EVERY user (both methods) enters their
      first and last name (FR-003), role from the professor email list in settings (FR-004)
- [x] 1h. Student ID linking page on first sign-in (FR-005; FR-006: one ID per account).
      Linking enrollments and the last-name check (rest of FR-006, FR-007) moved to Phase 4,
      because there are no class lists yet
- [x] 1i. Professor and student dashboards (empty for now), sign out, 403 on wrong role (FR-008, FR-009)
      Next to sign-out, a short hint: "Using a shared computer? Also sign out of GitHub."
      (signing out of CheckedIn does not sign you out of GitHub, which would sign the next person in)
- [x] Follow-up: a menu with only the pages of the user's role (name, role badge, Sign out, current page
      highlighted) and a frontend route guard, `RequireRole`: signed out -> sign-in page, other role -> own
      dashboard (FR-008). The backend's 401/403 rules stay the real protection.

Done when: you can sign in both ways with your @cit.edu email and see the correct dashboard for your role.

Ask Claude Code: *"Start Phase 1, step 1c from docs/BUILD_PLAN.md."*

---

## Phase 2 — Sections and sessions (Feature 2: FR-010 – FR-016) ✅

- [x] `CourseSection` and `ClassSession` entities, enums `SessionType`, `DeliveryMode`, `Semester`
- [x] Create, edit, archive sections; add weekly sessions
- [x] Reject end time before start time and overlapping sessions (FR-013)
- [x] Session hours computed from start/end time (FR-014)
- [x] Professor dashboard shows today's sessions in time order (FR-016)

Done when: a professor can create IT342 G01 with a Thursday lecture and a Thursday lab.

Ask Claude Code: *"Start Phase 2 from docs/BUILD_PLAN.md."*

---

## Increment 2 — User profile and password management (SRS v1.4) ✅

Built on branches `feature/*` → `develop` → `main` (see "How we use Git branches" in the README).
Password sign-in is ADDED as a third way to sign in; email code and GitHub keep working as before.
Requirement IDs: "Increment 2 / SRS v1.4" until the SRS v1.4 numbers are final.

- [x] Step 1 – `feature/user-profile` (PR #1): `/profile` page for both roles, linked in the menu ("Profile").
      Shows email, role, student ID (students) and the day the account was created (read-only, with the
      reason). First and last name can be changed: required, trimmed, 1–50 letters, spaces, hyphens,
      apostrophes and periods ("Ma. Cristina", "Dela Cruz"). `GET`/`PUT /api/profile` always use the
      signed-in user from the session (no user ID in the URL or body). One name rule for the profile and the
      first-sign-in name form (`NameRules`), now 50 characters for both (was 60 on the name form).
- [x] Step 2 – `feature/password-sign-in`: `users.password_hash` (BCrypt only; a database rule refuses anything
      else; never in any response). Password rules in `PasswordRules` / `passwordRules.js`: 8–64 characters, a
      letter and a number, not the email, both boxes the same. The first-sign-in name form also asks for
      "Create a password" + "Confirm password" (email-code and GitHub sign-ups). "Sign in with password" on the
      sign-in page: the same "Email or password is incorrect." for a wrong email, a wrong password or an
      account without a password; 5 wrong passwords for one email -> password sign-in blocked for 15 minutes
      (`password_lockouts`, numbers in application.properties); the same 8-hour session as the other methods.
      Also: role hints on the profile, and the header on one centred line with a one-line GitHub hint.
- [x] Steps 3 + 4 – `feature/password-reset-and-change` (one branch):
  - [x] "Forgot password?" on the sign-in page -> `/forgot-password`: always the same answer ("If that email has an
        account, we sent a reset link"). Brevo email with a single-use link to `/reset-password?token=…`:
        `password_reset_tokens` stores only the SHA-256 hash of a random 256-bit token, 30 minutes, at most 3 links
        per 15 minutes per email (silently). Using a link saves the new password, uses up every open link of that
        user and lifts a lockout; used, expired or unknown links get clear messages. This is also how accounts
        from before Increment 2 set their first password.
  - [x] Change password on the profile page: current password (BCrypt check), new + confirm (same rules, must
        differ from the current one). Afterwards the old password no longer works, open reset links are used up,
        and the session stays. Accounts without a password see "Set a password" (link to "Forgot password?").
  - [x] Profile shows the student ID only for students (a professor's leftover student ID is hidden).
- [x] Release: one pull request `develop` → `main` ("Increment 2 release", PR #6)

## Phase 3 — Room and lab layout builder (Feature 3: FR-017 – FR-023)

- [ ] `RoomLayout` and `Seat` entities, `CellType` enum (SEAT, WORKSTATION, TABLE, AISLE, NO_CHAIR)
- [ ] Layout builder page: set rows/columns, paint cells with a tool palette
- [ ] Auto-number workstations, no duplicates (FR-019)
- [ ] Usable seat count, only SEAT and WORKSTATION (FR-020)
- [ ] Reuse layouts by room name; only the creator edits (FR-021)
- [ ] Link in-person and hybrid sessions to a layout (FR-015)

Done when: you can draw NGE207 as a lab and GLE201 as a lecture room with tables.

---

## Phase 4 — Class list import (Feature 4: FR-024 – FR-029)

- [ ] `Enrollment` entity, `EnrollmentStatus` enum
- [ ] Upload .xlsx (library: Apache POI `poi-ooxml`), preview rows with errors, confirm import
- [ ] Add, edit, remove a single student
- [ ] Link student accounts to enrollments with the same student ID and flag last-name mismatches
      (FR-006, FR-007), both when a class list is imported and when a student links their ID later
      (`AuthService.linkStudentNumber`). Moved here from step 1h
- [ ] Downloadable blank template

Done when: importing a 42-row Excel file shows a preview and saves only valid rows.

---

## Phase 5 — Seat assignment and auto-arrange (Feature 5: FR-030 – FR-035)

- [ ] `SeatAssignment` entity
- [ ] Assign/move/swap students on the seat plan
- [ ] `SeatArrangementService`: alphabetical by last name, 4 corners × 2 directions,
      skip TABLE, AISLE, NO_CHAIR; extra students go to "Unassigned"
- [ ] Unit tests for all 8 corner/direction combinations

Done when: one click arranges a section A–Z from the corner you choose.

---

## Phase 6 — Seat-map attendance (Feature 6: FR-036 – FR-045)

- [ ] `ClassMeeting`, `AttendanceRecord` entities, `AttendanceStatus`, `MeetingStatus` enums
- [ ] Open today's meeting; everyone starts PRESENT
- [ ] Seat map view (tap to cycle colors), list view (online/field), hybrid view
- [ ] Save all changes in one request; edit past meetings; "No Class" option
- [ ] Live counts of P / L / A / E

Done when: you can check a 42-student class in under 2 minutes (NFR-016).

---

## Phase 7 — Absence hours and drops (Feature 7: FR-046 – FR-051)

- [ ] `AbsenceCalculator` with the rules from `application.properties`
- [ ] Recompute after every save; DROPPED at 18 hours, back to ACTIVE below 18
- [ ] At-risk list (12+ hours)
- [ ] Unit tests using the example in SRS Appendix B (answer: 8 hours)

---

## Phase 8 — Excuses with Cloudinary (Feature 8: FR-052 – FR-058)

- [ ] `ExcuseRequest` entity, `integration/FileStorageService` + `CloudinaryStorageService`
- [ ] Student uploads JPG/PNG/PDF ≤ 5 MB within 7 days
- [ ] Professor approves/rejects; approved record turns EXCUSED; signed links (10 min)

---

## Phase 9 — Student attendance page (Feature 9: FR-059 – FR-063)

- [ ] Student dashboard: enrollments, history, lates, absence hours, remaining hours, excuse status

---

## Phase 10 — Email notifications with Brevo (Feature 10: FR-064 – FR-069)

- [ ] `NotificationLog` entity (`EmailService` + `BrevoEmailService` already exist from step 1d)
- [ ] Emails at 12 h, 15 h, dropped, excuse result; each warning once; 3 retries

---

## Phase 11 — Excel export (Feature 11: FR-070 – FR-071)

- [ ] Download section attendance as .xlsx (P/L/A/E per date + totals)

---

## Phase 12 — Testing, polish, deployment

- [ ] Check every FR and NFR in the SRS against the app (use SRS Appendix A)
- [ ] Usability test: 3 people check attendance for 42 students
- [ ] Before deployment: change the test account password listed in the Increment 2 report
      (`docs/Increment-2-Completion-Report.pdf`, Section 5)
- [ ] Deploy frontend (Vercel/Netlify) and backend (Render/Railway); the database is already online on Supabase
- [ ] Update README with the live links and screenshots
