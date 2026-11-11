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

## Phase 1 — Users and sign-in (Feature 1: FR-001 – FR-009)

Goal: professors and students sign in with an email code or Google and land on their own page.

> **Change of plan (Nov 9):** Microsoft sign-in was dropped because CIT-U's Microsoft tenant
> requires IT admin approval ("Need admin approval"). Sign-in is now an emailed code (Brevo) or
> Google. The SRS was updated to match; FR numbers did not change.

Steps (do them one at a time):
- [x] 1a. `auth/User` entity, `Role` enum, `UserRepository`, `GlobalExceptionHandler`
- [x] 1b. ~~Register the app in Microsoft Entra ID~~ (dropped, see note above)
- [x] 1c. Remove Microsoft sign-in: drop the uncommitted MSAL work, remove `@azure/msal-browser`,
      the Entra README section and `ENTRA_*` settings. In `User`, replace `msObjectId` with an
      optional `googleSubject`, and find users by email (see ERD)
- [ ] 1d. Brevo: account + verified sender (README guide), `integration/EmailService` + `BrevoEmailService`.
      Send one test email to your @cit.edu inbox and check it doesn't land in Junk
- [ ] 1e. Email-code sign-in (FR-001, FR-002, NFR-008): `LoginCode` entity, `LoginCodeService`,
      `POST /api/auth/code` and `POST /api/auth/code/verify`, sign-in page (email, then code),
      HttpOnly session cookie for 8 hours (library: `spring-boot-starter-security`)
- [ ] 1f. Google sign-in (FR-001, FR-002): Google Cloud OAuth client ID (README guide),
      "Sign in with Google" button (Google Identity Services script), `POST /api/auth/google`,
      backend verifies the ID token with Google's public keys
      (library: `spring-boot-starter-oauth2-resource-server`), `integration/GoogleIdentityProvider`
- [ ] 1g. Create the account on first sign-in, ask email-code users for their name (FR-003),
      and show the same name form to Google users when Google doesn't send both
      `given_name` and `family_name`,
      role from the professor email list in settings (FR-004)
- [ ] 1h. Student ID linking page on first sign-in (FR-005 – FR-007)
- [ ] 1i. Professor and student dashboards (empty for now), sign out, 403 on wrong role (FR-008, FR-009)

Done when: you can sign in both ways with your @cit.edu email and see the correct dashboard for your role.

Ask Claude Code: *"Start Phase 1, step 1c from docs/BUILD_PLAN.md."*

---

## Phase 2 — Sections and sessions (Feature 2: FR-010 – FR-016)

- [ ] `CourseSection` and `ClassSession` entities, enums `SessionType`, `DeliveryMode`, `Semester`
- [ ] Create, edit, archive sections; add weekly sessions
- [ ] Reject end time before start time and overlapping sessions (FR-013)
- [ ] Session hours computed from start/end time (FR-014)
- [ ] Professor dashboard shows today's sessions in time order (FR-016)

Done when: a professor can create IT342 G01 with a Thursday lecture and a Thursday lab.

Ask Claude Code: *"Start Phase 2 from docs/BUILD_PLAN.md."*

---

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
- [ ] Deploy frontend (Vercel/Netlify) and backend (Render/Railway); the database is already online on Supabase
- [ ] Update README with the live links and screenshots
