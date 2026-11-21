# SAGIP Documentary

**SAGIP: Smart AI Guard for Incident Prevention**
OOP 101 – Object-Oriented Programming | CS-2101 | 1st Semester, AY 2026-2027
Batangas State University – The National Engineering University, Alangilan Campus (College of Informatics and Computing Sciences)

This folder is the development log of our project. Each file records what the team did, decided, and produced on a given date, from the first brainstorm to the current build.

## Team Rocket (Team 10)

| Member | Role |
| --- | --- |
| Jacob L. Cornejo | Project Leader / System Design Lead (backend) |
| John Dan Racel C. Eslit | Lead Programmer (front-end) |
| Martin Joshua S. Camantigue | Programmer / Documentation Lead (front-end) |

Instructor: Ms. Fatima Marie P. Agdon, MSCS

## Project in One Paragraph

SAGIP is a Java-based desktop application that uses a live camera to scan a room for hazards during an earthquake or fire scenario. It displays what it detects, assigns each hazard a danger level, describes it, and recommends corrective actions.

## Timeline

| Date | Entry | Highlights |
| --- | --- | --- |
| October 5, 2026 | [2026-10-05 – Topic Brainstorming](2026-10-05_Topic-Brainstorming.md) | Explored topics; settled on a Java-based gym app (virtual gym card, fitness tracker). |
| October 6, 2026 | [2026-10-06 – Change of Concept](2026-10-06_Change-of-Concept.md) | After consulting our professor, pivoted to a danger detector for earthquakes and fires. |
| October 29, 2026 | [2026-10-29 – Development Kickoff](2026-10-29_Development-Kickoff.md) | Proposal accepted; coding started and work was split between backend and front-end. |
| November 3, 2026 | [2026-11-03 – UI Blueprint and Timeline](2026-11-03_UI-Blueprint-and-Timeline.md) | UI blueprint created, planning meeting held, repository commits began, task timeline drafted. |
| November 5, 2026 | [2026-11-05 – Camera Module Completed](2026-11-05_Camera-Module-Completed.md) | Danger-detector camera finished. |
| November 9, 2026 | [2026-11-09 – Main Page and Camera Page](2026-11-09_Main-Page-and-Camera-Page.md) | Main page and camera page created. |

## Repository Layout (at time of writing)

```
SAGIP/
├── Documentary/              <- this folder
├── FrontEnd/
│   └── MainPage.java         <- main page UI
└── SAGIP-CAMERA--main/       <- camera and hazard detection module
    └── DisasterHazardSystemApp.java
```
