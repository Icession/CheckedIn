# Diagram sources

These files draw every diagram in `docs/CheckedIn_SRS.pdf` and in `docs/diagrams/`.
Edit the source, render it, and replace the PNG one folder up.

## How to render

PlantUML diagrams (`.puml`) need Java and the PlantUML jar (https://plantuml.com/download).
Run from this folder, because the `.puml` files include the shared `*.iuml` style files:

```bash
java -jar plantuml.jar -tpng erd.puml
```

The use case diagram is drawn by `uc_svg.py` (Python 3, no extra libraries). It writes
`usecase.svg`. Open that file in a browser and export or screenshot it as a PNG.

## Source → image

| Source | Image in `docs/diagrams/` | SRS figure |
|---|---|---|
| `arch.puml` | `architecture.png` | 1. System architecture |
| `erd.puml` | `erd.png` | 2. ERD (Crow's Foot) |
| `uc_svg.py` | `use-case.png` | 3. Use case diagram |
| `act_signin.puml` | `activity-signin.png` | 4. Activity: sign in |
| `act_import.puml` | `activity-import.png` | 5. Activity: class list import |
| `act_attendance.puml` | `activity-attendance.png` | 6. Activity: check attendance |
| `act_excuse.puml` | `activity-excuse.png` | 7. Activity: excuses |
| `class_domain.puml` | `class-domain.png` | 8. Class: domain model |
| `class_service.puml` | `class-services.png` | 9. Class: sign-in and sections (built in Phases 1–2) |
| `class_profile_password.puml` | `class-profile-password.png` | 10. Class: profile and password management (built in Increment 2) |
| `class_service_planned.puml` | `class-services-planned.png` | 11. Class: planned (Phases 3–11) |
| `seq_signin.puml` | `sequence-signin.png` | 12. Sequence: sign in and sign out |
| `seq_attendance.puml` | `sequence-attendance.png` | 13. Sequence: attendance |
| `seq_excuse.puml` | `sequence-excuse.png` | 14. Sequence: excuses |

Shared styles: `common.iuml` (all), `act_style.iuml`, `cls_style.iuml`, `seq_style.iuml`.

## Keeping things in sync

When a phase adds or renames classes, update the "built" class diagram for that part of the app and move the
finished parts out of `class_service_planned.puml`: sign-in and sections in `class_service.puml`, profile and
passwords (Increment 2) in `class_profile_password.puml`. Each diagram is kept narrow enough to read in the SRS. Use the real class and method names from
the code. Changing a diagram in the SRS also means adding a line to the SRS revision history.
