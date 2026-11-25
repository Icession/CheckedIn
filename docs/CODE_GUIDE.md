# Code Guide — where everything is

Use this page to find any piece of code quickly, or to explain the project
structure during the code walkthrough.

## 1. Big picture

```
CheckedIn/
├── CLAUDE.md          Rules for Claude Code (and for anyone writing code here)
├── README.md          How to install and run the project
├── docs/
│   ├── CheckedIn_SRS.pdf   Requirements (source of truth)
│   ├── BUILD_PLAN.md       Build order, phase by phase
│   ├── CODE_GUIDE.md       This file
│   └── diagrams/           ERD, use case, class, activity, sequence diagrams
├── backend/           Spring Boot REST API (Java)
└── frontend/          React app (JavaScript)
```

How a request travels through the system:

```
React page  →  src/api/*.js  →  HTTP /api/...  →  Controller  →  Service  →  Repository  →  PostgreSQL (Supabase)
(shows UI)     (fetch calls)                       (receives)     (rules)     (database)
```

Example: when a professor saves attendance, `AttendancePage.jsx` calls
`attendanceApi.js`, which sends `PUT /api/meetings/{id}/records` to
`AttendanceController`, which asks `AttendanceService` to save, which uses
`AttendanceRecordRepository` to write to the database.

## 2. Backend layout (package by feature)

All Java code is under `backend/src/main/java/edu/cit/checkedin/`.
Each folder is one feature from the SRS, so you can go straight from a
requirement to its code.

| Package | SRS feature | What it holds |
|---|---|---|
| `common/` | — | Setup checks (health check, test email), global error handler, shared config, security config |
| `auth/` | Feature 1: Sign-in and roles | `User`, `Role`, `LoginCode`, email-code and GitHub sign-in, student ID linking |
| `profile/` | Increment 2: the user's own profile | `ProfileController` (`GET`/`PUT /api/profile`), `ProfileService`, `dto/ProfileResponse`, `dto/UpdateProfileRequest` (name rules shared with the first-sign-in form in `auth/NameRules`) |
| `password/` | Increment 2: passwords | `PasswordController` (`POST /api/auth/password/sign-in`), `PasswordSignInService`, `PasswordRules` (shared rules and messages), `PasswordLockout` + `PasswordLockoutRepository` (5 wrong tries -> 15-minute block), `PasswordSettings`, `dto/PasswordSignInRequest` |
| `section/` | Feature 2: Sections and sessions | `CourseSection`, `ClassSession`, schedule rules |
| `layout/` | Feature 3: Layout builder | `RoomLayout`, `Seat`, `CellType` |
| `roster/` | Feature 4: Class list import | `Enrollment`, Excel import and validation |
| `seating/` | Feature 5: Seat assignment | `SeatAssignment`, alphabetical auto-arrange |
| `attendance/` | Feature 6: Attendance checking | `ClassMeeting`, `AttendanceRecord` |
| `absence/` | Feature 7: Absence hours and drops | `AbsenceCalculator`, at-risk list |
| `excuse/` | Feature 8: Excuses | `ExcuseRequest`, Cloudinary upload |
| `student/` | Feature 9: Student attendance page | Read-only endpoints for students |
| `notification/` | Feature 10: Emails | `NotificationLog`, Brevo email sending |
| `export/` | Feature 11: Excel export | Builds the .xlsx file |
| `integration/` | External systems | `IdentityProvider`, `FileStorageService`, `EmailService` interfaces and their GitHub / Cloudinary / Brevo classes |

Inside every feature folder, files follow the same pattern:

```
section/
├── CourseSection.java              Entity (a table in the database)
├── CourseSectionRepository.java    Database queries
├── SectionService.java             Business rules
├── SectionController.java          REST endpoints (/api/sections/...)
└── dto/
    ├── CreateSectionRequest.java   JSON the frontend sends
    └── SectionResponse.java        JSON the backend returns
```

Tests mirror the same folders under `backend/src/test/java/edu/cit/checkedin/`.

## 3. Frontend layout

All React code is under `frontend/src/`.

| Folder / file | What it holds |
|---|---|
| `main.jsx` | Starts the app |
| `App.jsx` | List of all pages (routes). Start here to find which component shows a URL |
| `pages/` | One file per screen, e.g. `AttendancePage.jsx` |
| `components/` | Small reusable pieces, e.g. `StatusPill.jsx`, `SeatCell.jsx` |
| `api/` | Every call that leaves the browser: to the backend (one file per feature, e.g. `sectionApi.js`) (GitHub sign-in is a normal link to the backend, `/oauth2/authorization/github`) |
| `utils/` | Small shared helpers, e.g. `format.js` (labels for codes like `IN_PERSON`, times as "6:00 PM") |
| `styles/theme.css` | All colors and sizes, including attendance status colors |
| `styles/global.css` | Shared basic styles |

When a feature has several pages, group them: `pages/attendance/AttendancePage.jsx`.

## 4. Naming cheat sheet

| Thing | Pattern | Example |
|---|---|---|
| Entity | singular noun | `AttendanceRecord` |
| Repository | `<Entity>Repository` | `AttendanceRecordRepository` |
| Service | `<Feature>Service` | `AttendanceService` |
| Controller | `<Feature>Controller` | `AttendanceController` |
| Request DTO | `<Action><Thing>Request` | `SaveAttendanceRequest` |
| Response DTO | `<Thing>Response` | `MeetingResponse` |
| Enum | singular noun | `AttendanceStatus` |
| React page | `<Name>Page.jsx` | `SeatPlanPage.jsx` |
| React component | `<Name>.jsx` | `SeatCell.jsx` |
| API file | `<feature>Api.js` | `attendanceApi.js` |

## 5. Quick finder

| I want to find... | Look in |
|---|---|
| The 3-lates / 18-hours rule | `absence/AbsenceCalculator.java` |
| The session overlap and time rules (FR-013) | `section/SessionScheduleChecker.java` |
| How seats are auto-arranged A–Z | `seating/SeatArrangementService.java` |
| Sign-in page (email code and GitHub button) | `frontend/src/pages/SignInPage.jsx` |
| Who may open which page (403 on the wrong role) | `common/SecurityConfig.java` and `auth/RoleService.java` |
| The menu for each role and the frontend route guard | `frontend/src/components/PageHeader.jsx`, `components/RequireRole.jsx` (around the routes in `App.jsx`), `utils/roles.js`, `hooks/useCurrentUser.js` |
| The profile page (view details, change the name) | `frontend/src/pages/ProfilePage.jsx`, `components/ProfileDetails.jsx`, `components/ProfileNameForm.jsx`, `api/profileApi.js`; name rules in `utils/nameRules.js` (frontend) and `auth/NameRules.java` (backend) |
| Password sign-in and the first password | Backend: `password/PasswordSignInService.java`, `password/PasswordRules.java`, the first password in `auth/AuthService.saveName`. Frontend: `components/PasswordSignInForm.jsx`, `components/NameForm.jsx`, `utils/passwordRules.js`, `signInWithPassword` in `api/authApi.js` |
| Sign out | `auth/SessionService.java` (`POST /api/auth/sign-out` in `auth/AuthController.java`) |
| How sign-in codes are made, hashed and checked | `auth/LoginCodeService.java` |
| GitHub verified-email check | `integration/GitHubIdentityProvider.java` (reads GitHub's verified emails) and `auth/GitHubUserService.java` (requires a verified @cit.edu one) |
| Where sign-in codes are emailed | `integration/BrevoEmailService.java` |
| Where excuse files are uploaded | `integration/CloudinaryStorageService.java` |
| Where emails are sent | `integration/BrevoEmailService.java` |
| The text of every email (subject and body) | `integration/EmailTemplate.java` |
| Rule numbers (3, 18, 12, 15, 7) | `backend/src/main/resources/application.properties` |
| Status colors | `frontend/src/styles/theme.css` |
| All URLs of the app | `frontend/src/App.jsx` |

(Files in this table are created in later phases. See `BUILD_PLAN.md`.)
