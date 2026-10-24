# Dashboard Themes and Responsive Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent light and dark dashboard themes and make the Northwind layout scale proportionally with window width.

**Architecture:** Keep one JavaFX view and toggle a theme class on its root, persisting the choice with `java.util.prefs.Preferences`. Isolate report-card sizing in a pure layout helper so responsive breakpoints and chart proportions are easy to verify.

**Tech Stack:** Java 21, JavaFX 21, CSS, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-24-dashboard-themes-responsive-design.md`

## Global Constraints

- Work only on `main`; do not create a branch or worktree.
- Keep the original application unchanged.
- Do not add third-party dependencies.
- Preserve all six reports and existing data/export behavior.
- Keep `.env` excluded from Git.

## Review Focus

- Missing or invalid saved theme must open in dark mode; test absent and invalid preference values.
- Widths at and around breakpoints must choose stable 3/2/1-column layouts; test 1480, 1000, and 800 pixel pane widths.
- Narrow layouts must give charts a positive readable height; test the smallest breakpoint.
- Theme changes must update the whole view and persist; verify with a preferences round trip and JavaFX screenshots.
- Existing FXML controls and event handlers must still load; run the full Maven suite and capture both modes.

---

### Task 1: Theme preference and responsive sizing helpers

**Files:**
- Create: `src/main/java/com/northwinddw/ui/DashboardTheme.java`
- Create: `src/main/java/com/northwinddw/ui/ReportGridLayout.java`
- Test: `src/test/java/com/northwinddw/ui/DashboardThemeTest.java`
- Test: `src/test/java/com/northwinddw/ui/ReportGridLayoutTest.java`

**Interfaces:** `DashboardTheme.load(Preferences)`, `DashboardTheme.save(Preferences)`, and `DashboardTheme.next()` handle persisted themes. `ReportGridLayout.calculate(double paneWidth)` returns columns, card width, card height, and chart height.

- [x] Write tests for default/invalid theme fallback, persistence, and toggling; run them to confirm RED.
- [x] Write tests for 3, 2, and 1-column breakpoints and proportional chart height; run them to confirm RED.
- [x] Implement the helpers with dark as the first-run theme and deterministic breakpoints; run both focused test classes to confirm GREEN.
- [ ] Commit as `feat: add theme preferences and responsive layout rules`.

### Task 2: Apply themes to the dashboard

**Files:**
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Modify: `src/main/resources/fxml/dashboard.fxml`
- Modify: `src/main/resources/css/dashboard.css`

- [x] Add an accessible theme button beside the connection state and apply the selected class to the root.
- [x] Preserve the selection in local preferences when the user changes it.
- [x] Match original indigo colors in dark mode and keep the light palette coherent; style chart plots, axes, legends, fields, focus, and hover states.
- [x] Reflow header controls without changing their actions; load FXML successfully.
- [ ] Commit as `feat: add light and dark dashboard themes`.

### Task 3: Scale cards and window proportions

**Files:**
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Modify: `src/main/java/com/northwinddw/Main.java`
- Modify: `src/main/resources/fxml/dashboard.fxml`

- [x] Bind report card and chart dimensions to `ReportGridLayout.calculate` when the scroll viewport changes width.
- [x] Remove fixed card widths and adjust window size/minimums to available display bounds.
- [x] Verify card positions and widths after resizing to wide, medium, and narrow stage sizes.
- [ ] Commit as `feat: scale dashboard cards with the window`.

### Task 4: Visual review and final verification

**Files:**
- Modify: `src/test/java/com/northwinddw/ui/DashboardScreenshotTest.java`
- Create/modify: `docs/screenshots/dashboard.png`, `docs/screenshots/dashboard-dark.png`, `docs/screenshots/connection-error.png`
- Modify: `README.md`, `docs/engineering-review.md`

- [ ] Capture both connected themes and a failed-connection state; inspect the resulting images.
- [ ] Run `scripts/verify.ps1` after the temporary PostgreSQL fixture is cleaned up.
- [ ] Confirm the working tree contains no `.env` or temporary database and stays on `main`.
- [ ] Commit as `docs: show dashboard themes and responsive behavior`.
