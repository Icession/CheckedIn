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
| `section/` | Feature 2: Sections and sessions | `CourseSection`, `ClassSession`, schedule rules |
| `layout/` | Feature 3: Layout builder | `RoomLayout`, `Seat`, `CellType`, `RoomType`, grid and number checks |
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
| `hooks/` | Custom React hooks that hold one piece of browser behavior. Layout builder: `useLayoutDraft.js` (the layout being edited, numbering settings, undo, live resizing), `useDraftBackup.js` (keeping unsaved work in this browser, Restore / Discard, warnings before leaving), `useDrawingPress.js` (what a mouse press does with each tool), `useCellDrag.js` (pressing and dragging across cells), `useToolShortcuts.js` (keyboard keys), `useUndoSteps.js` (the undo list) |
| `utils/` | Small shared helpers, e.g. `format.js` (labels for codes like `IN_PERSON`, times as "6:00 PM" in Philippine time). Layout builder: `layoutGrid.js` (cells, growing the grid, tools, keys), `layoutDraft.js` (the layout being edited, resizing, the save JSON), `layoutView.js` (drawing the room from the teacher's desk: turned around, the front row at the bottom), `seatNumbers.js` (numbering settings, Start at, the numbering order from a corner), `seatLabels.js` (checking labels), `autoNumber.js`, `numberingDrag.js` (the Number tool brush), `cellSelection.js` (Select tool) and `draftBackup.js` (the copy kept in localStorage). Their unit tests sit next to them as `*.test.js` and run with `npm test` (Node's built-in test runner, no extra library) |
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
| Which sessions need a room layout (FR-015) | `section/ClassSessionService.java` (`attachLayout`) and `ClassSession.needsLayout()` |
| Automatic seat and workstation numbers (FR-019) | In the builder: `frontend/src/utils/autoNumber.js` (Auto-number: all from Start at, a selected block from Next), `numberingDrag.js` (dragging the Number tool) and `layoutDraft.js` (`resizeDraft`: renumbering after a size change). On save, empty numbers: `layout/RoomLayout.java` (`numberNewSeats`) |
| Duplicate seat numbers and grid size checks | `layout/LayoutGridChecker.java` |
| The layout builder screen | `frontend/src/components/LayoutEditor.jsx` (room details, saving), `LayoutDrawing.jsx` (toolbar, grid, side panel) and `BuilderBottomBar.jsx` (Save, Undo, the one message spot). Pieces: `BuilderToolbar`, `ToolHint`, `LayoutLegend`, `AutoNumberPanel`, `LayoutGrid` |
| Layout builder keyboard shortcuts (Q W E R T A S, Z, Esc) | `TOOL_SHORTCUTS` in `frontend/src/utils/layoutGrid.js`; listened to in `frontend/src/hooks/useToolShortcuts.js` |
| Numbering by dragging (once per drag, untouched seats giving up their label, Next) | `numberCellInDrag` in `frontend/src/utils/numberingDrag.js`; what a mouse press does with each tool: `frontend/src/hooks/useDrawingPress.js` |
| Start at and Next (the two numbers per type) | `frontend/src/utils/seatNumbers.js` (settings) and `frontend/src/hooks/useLayoutDraft.js` (`changeNumbering`, `setNextNumbers`) |
| Undo (Z), also for a resize | `frontend/src/hooks/useLayoutDraft.js` (`rememberForUndo`, `undo`) and `useUndoSteps.js` |
| Select tool rules (select, unselect, which cells may be selected) | `frontend/src/utils/cellSelection.js` |
| New rows and columns continuing the pattern, and renumbering after a resize | `growGrid` in `frontend/src/utils/layoutGrid.js`, `resizeDraft` in `frontend/src/utils/layoutDraft.js` |
| Where numbering starts (corner, left/right as seen on screen) and its direction | `cellsInNumberingOrder` in `frontend/src/utils/seatNumbers.js` |
| The teacher's view of a room (which stored cell is drawn where on screen) | `toScreenPosition` / `fromScreenPosition` in `frontend/src/utils/layoutView.js` (reuse them for the Phase 5 seat picker) |
| Unsaved work kept after a refresh, "Leave this page?" warnings | `frontend/src/hooks/useDraftBackup.js` (+ `utils/draftBackup.js`, `components/RestoreDraftBar.jsx`) |
| Layout cell colors | `frontend/src/styles/theme.css` (`--cell-...`) |
| How seats are auto-arranged A–Z | `seating/SeatArrangementService.java` |
| Sign-in page (email code and GitHub button) | `frontend/src/pages/SignInPage.jsx` |
| Who may open which page (403 on the wrong role) | `common/SecurityConfig.java` and `auth/RoleService.java` |
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
