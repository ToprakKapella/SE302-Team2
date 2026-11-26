# SE302-Team2 – Exam Scheduler

SE302 Software Engineering course project – Section 2, Team 2.

**Team:** Toprak Ege Güldoğan, Elif Eylül Tütüncü, Arda Korkmaz, Arda Baran Günhan, Gökberk Erken

Exam Scheduler is a Windows desktop application for the student affairs office. It imports the classrooms with their capacities and the courses with their registered students from CSV files, and generates an exam schedule for a given number of days and slots per day. No student has two exams in consecutive slots of a day, and no student has more than two exams per day. The schedule can be viewed per classroom, per student, per course and per day.

## Repository structure

```
docs/
├── M1_RequirementsDocument/     Milestone 1 – Requirements Document (LaTeX)
├── M2_DesignDocument/           Milestone 2 – Design Document (LaTeX)
│   ├── sections/                one file per section
│   └── figures/                 diagrams and screen designs (PNG + Mermaid source)
└── team/                        notes of the team members
exam-scheduler/                  application (Java, JavaFX user interface)
├── sampleData/                  sample CSV files
└── src/
    ├── main/java/com/team2/examscheduler/
    │   ├── model/               data classes
    │   ├── io/                  CSV import and validation
    │   ├── constraints/         constraint checking
    │   ├── scheduler/           schedule generation
    │   ├── views/               the four schedule views
    │   └── gui/                 windows, menus and help
    ├── main/resources/          help file
    └── test/java/com/team2/examscheduler/   tests
```

Empty folders contain a `.gitkeep` file only so that Git keeps them; do not delete it.

## Who works where

| Folder | Owner |
|---|---|
| `docs/M2_DesignDocument/sections/01_intro.tex`, `exam-scheduler/` build files, installer | Toprak Ege Güldoğan |
| `docs/M2_DesignDocument/sections/02_structural.tex`, `io/`, `main/resources/` | Elif Eylül Tütüncü |
| `03_behavioral.tex` (3.1), `04_ui.tex` (4.1), `model/`, `gui/` | Arda Korkmaz |
| `03_behavioral.tex` (3.2, 3.3), `scheduler/`, `constraints/`, `test/` | Arda Baran Günhan |
| `03_behavioral.tex` (3.4), `04_ui.tex` (4.2), `views/` | Gökberk Erken |

## Milestones

| Milestone | Due | Status |
|---|---|---|
| M1 – Requirements Document | 15.11.2026 | submitted |
| M2 – Design Document | 06.12.2026 | in progress |
| M3 – Project Delivery (installer, source, help, release) | 20.12.2026 | planned |

Project management board: https://trello.com/b/ArhCHmrc/se302-team-2
