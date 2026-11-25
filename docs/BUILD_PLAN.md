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

## Phase 3 — Room and lab layout builder (Feature 3: FR-017 – FR-023)

- [ ] `RoomLayout` and `Seat` entities, `CellType` enum (SEAT, WORKSTATION, TABLE, AISLE, NO_CHAIR)
- [ ] Layout builder page: set rows/columns, paint cells with a tool palette
- [ ] Auto-number workstations, no duplicates (FR-019)
- [ ] Auto-number: prefix (e.g. "PC-"), start number, reading or snake order, all seats or only the
      selected cells; labels are plain numbers (PC-1, not PC-01); duplicates refused when typed and shown in red (FR-019)
- [ ] "Apply to" (workstations / seats / both, each with its own prefix), two numbers per type: "Start at"
      (set only by the professor; Auto-number of all cells starts there, so it gives the same result every time)
      and "Next" (the counter for dragging and for numbering a selected block; afterwards last number + 1)
- [ ] Numbering by dragging the Number tool as a renumbering brush: each cell once per drag; untouched seats of
      the same type give up their label and are marked; the other type's labels block (FR-019)
- [ ] Changing the room size (live: 400 ms after typing stops, at once with the ▲▼ arrows) continues the pattern
      of cells and renumbers every seat from Start at, as one undo step; no old labels come back as duplicates
- [ ] Numbering order: "Start from" a corner (front/back row, left/right side of the screen) and "Direction"
      (same direction or snake)
- [ ] The room is shown from the teacher's desk: turned around, the front row at the bottom next to the board
      bar. Only the drawing turns; stored rows and columns stay the same (row 0 = front)
- [ ] Unsaved work kept in this browser (localStorage) and offered after a refresh (Restore / Discard);
      a warning before closing, reloading or following a link while there are unsaved changes
- [ ] Select tool that selects or unselects by dragging (Esc clears), keyboard shortcuts Q W E R T A S and Z to undo
- [ ] Builder layout: one toolbar, the grid with a side panel (legend and counts, or numbering),
      a bottom bar with Save, Undo and the one message spot that stays visible; the side panel moves below the grid on phones

> FR-019 now covers numbering with a prefix (e.g. PC-1 … PC-56), separate numbering for seats and
> workstations, numbering by dragging, and renumbering when the room size changes. Update the FR-019
> wording in the SRS after Phase 3.
- [ ] Usable seat count, only SEAT and WORKSTATION (FR-020)
- [ ] Reuse layouts by room name; only the creator edits (FR-021)
- [ ] Link in-person and hybrid sessions to a layout (FR-015); older sessions get a "Choose layout" action
- [ ] "Front of room" marker above the grid (FR-023)

> FR-022 (warn before a layout change removes a seat with a student) moved to Phase 5, because
> there are no seat assignments yet. Seats keep their `seat_id` when a layout is edited, so Phase 5 can add it.

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
- [ ] Warn before a layout change removes a seat that has a student, and move that student to
      "Unassigned" if the professor confirms (FR-022). Moved here from Phase 3

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
