# Analytics Chart Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Northwind's six reports use readable, data-appropriate charts with direct values and clear axes, then document real database captures and release version 2.0.0.

**Architecture:** Retain JavaFX Charts and the existing dashboard structure. Replace category pie comparisons with ranked bars, keep ordinal/time series as lines, switch employee sales to bars for a single selected year, and add data labels/axis formatting while preserving the chart nodes used by PDF and QR export.

**Tech Stack:** Java 21, JavaFX 21.0.10, PostgreSQL 16, Maven, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-26-analytics-chart-redesign-design.md`

## Global Constraints

- Work only on `main`; do not create branches or worktrees.
- Keep JavaFX Charts and add no chart-library dependency.
- Keep PostgreSQL access read-only; use `.env` locally and never print or commit its password.
- Keep missing quarterly observations missing; do not invent zero values or fit a regression to the three annual observations.
- Preserve light/dark themes, responsive cards, year filtering, PDF, and QR behavior.
- Real README captures must come from `127.0.0.1:5432/northwind_pentaho` loaded with the locally configured `postgres` credentials.
- Set Maven version `2.0.0` and publish tag/release `v2.0.0` only after all checks pass.

## Review Focus

- The year-filtered employee report must switch to a ranked bar comparison and restore its line trend for all years; test both views and verify detail return restores the correct active chart.
- Sparse boundary quarters must not render as zero or connect across missing quarters; test the returned observations and inspect real-data screenshots.
- Short bars, long customer labels, and the 16-country supplier report must retain visible values and category labels at the minimum card width; test resizing and inspect overview/detail captures.
- PDF and QR snapshots must contain the active chart after employee-view switching; exercise the existing export snapshot path for both employee modes.
- Real screenshots must load from the configured database without recording connection values; assert the screenshot source reaches the connected state and keep a test-fixture mode for normal automated tests.

---

### Task 1: Define chart formatting and time categories

**Files:**
- Modify: `src/main/java/com/northwinddw/ui/DashboardFormat.java`
- Test: `src/test/java/com/northwinddw/ui/DashboardFormatTest.java`

**Interfaces:**
- `DashboardFormat.chartUsd(BigDecimal amount)` returns compact, locale-stable USD text for data labels.
- `DashboardFormat.quarterLabel(String sourceQuarter)` maps English and existing abbreviated quarter values to `T1`–`T4`, preserving unknown values visibly.

- [x] **Step 1: Add failing formatting tests** for negative/zero/large USD values, exact cents in full tooltips, and `First`/`T1` normalization to `T1` (same for the four quarters).
- [x] **Step 2: Run** `mvn -q -Dtest=DashboardFormatTest test`; confirm the new methods are missing.
- [x] **Step 3: Implement** compact USD labels and ordinal quarter normalization without changing `DashboardFormat.usd`.
- [x] **Step 4: Run** `mvn -q -Dtest=DashboardFormatTest test`; confirm all cases pass.
- [x] **Step 5: Commit** as `feat: add compact chart value formatting`.

### Task 2: Match chart type, axes, and employee period view

**Files:**
- Modify: `src/main/resources/fxml/dashboard.fxml`
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Test: `src/test/java/com/northwinddw/ui/DashboardDetailViewTest.java`

**Interfaces:**
- Category, country, customer, supplier, and selected-year employee comparisons use `BarChart<Number, String>` with labeled numeric X and categorical Y axes.
- `employeeTrendChart` is a `LineChart<String, Number>` for all-year data; `employeeYearChart` is a `BarChart<Number, String>` for a selected year. Both remain in one responsive chart host so detail navigation and exports use the currently active child.
- `DashboardController.renderEmployees(List<EmployeeRevenue> rows, Integer selectedYear)` renders only the matching trend or single-year comparison.

- [x] **Step 1: Add failing JavaFX assertions** that category revenue is a bar chart; each axis has its intended dimension/unit; line series are ordered by year/quarter; employee view is a trend for all years and a ranked bar for one year; absent quarters stay absent.
- [x] **Step 2: Run** `mvn -q -Dtest=DashboardDetailViewTest test`; confirm the pie chart and single employee chart violate the assertions.
- [x] **Step 3: Update FXML** for labeled axes, replace category pie with horizontal bars, and add both employee chart choices inside a responsive host.
- [x] **Step 4: Update controller rendering** to normalize/chronologically order quarter labels, preserve missing values, switch employee charts by selected period, and restore the active node on return from detail.
- [x] **Step 5: Run** `mvn -q -Dtest=DashboardDetailViewTest test`; verify all report cards, year changes, detail return, and chart selection.
- [x] **Step 6: Commit** as `feat: use data-appropriate charts and axes`.

### Task 3: Add visible values, meaningful colors, and responsive labels

**Files:**
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Modify: `src/main/resources/css/dashboard.css`
- Test: `src/test/java/com/northwinddw/ui/DashboardDetailViewTest.java`
- Modify: `src/test/java/com/northwinddw/ui/DashboardScreenshotTest.java`

**Interfaces:**
- Every revenue/count bar and observed line point has a visible compact value label and an exact tooltip/accessibility description.
- A single ranked measure uses one semantic series color; years and employees use distinct legend colors. Labels remain readable in both themes.

- [x] **Step 1: Add failing view tests** for visible labels on every plotted datum, exact-value tooltips, zero-based bar axes, legends for multi-series trends, accessible summaries, and stable series colors in both themes.
- [x] **Step 2: Run** `mvn -q -Dtest=DashboardDetailViewTest test`; confirm current charts have no visible data labels and some summaries omit measure/context.
- [x] **Step 3: Add chart data labels** to bar marks and line symbols while retaining tooltips with full values and category/year names; include category share in category detail labels.
- [x] **Step 4: Define theme-aware chart palette and label styles** for line/bar colors, axes, gridlines, legends, data labels, and selected/hover states; keep text contrast and color-independent series names.
- [x] **Step 5: Run the focused view test** and the screenshot test with deterministic fixture data; inspect light/dark captures at overview/detail sizes.
- [x] **Step 6: Commit** as `feat: label and style analytics charts`.

### Task 4: Capture the real database, update project documentation, and set version

**Files:**
- Modify: `src/test/java/com/northwinddw/ui/DashboardScreenshotTest.java`
- Modify: `README.md`, `pom.xml`
- Create/modify: `CHANGELOG.md`
- Regenerate: `docs/screenshots/dashboard.png`, `dashboard-dark.png`, `dashboard-detail.png`, `dashboard-detail-dark.png`

**Interfaces:**
- `NORTHWIND_SCREENSHOT_DATA_SOURCE=database` makes the screenshot test load `DatabaseConfig` and `AnalyticsRepository`; the default remains fixture data for automated tests.
- The screenshot test verifies a connected database before saving captures and never prints connection credentials.

- [x] **Step 1: Add failing screenshot-test coverage** for the database-backed source selector and connected-state requirement while preserving the deterministic fixture path.
- [x] **Step 2: Implement the real-data screenshot mode** using the current `.env` and the existing read-only JDBC settings.
- [x] **Step 3: Generate and inspect all four README captures** with `NORTHWIND_DB_USER=postgres`, `NORTHWIND_SCREENSHOT_DATA_SOURCE=database`, and the local database; confirm real amounts, labels, axes, both themes, and detail views are legible.
- [x] **Step 4: Update README and changelog** with the actual dashboard values/captures and version 2.0.0 release notes; update Maven project version to `2.0.0`.
- [ ] **Step 5: Run** `.\scripts\verify.ps1 -WithDatabaseIntegration`, the database-backed screenshot test, and `mvn javafx:run` with PostgreSQL to confirm dynamic startup.
- [x] **Step 6: Verify** `.env` remains ignored and absent from the staged file list, branch is `main`, and no database writes occurred.
- [x] **Step 7: Commit** as `docs: prepare Northwind DW v2.0.0`.

### Task 5: Review and publish v2.0.0

**Files:**
- Release: Git tag `v2.0.0` and GitHub release for `Fabianrodas/northwind-dw`.

- [x] **Step 1: Run final verification** and inspect `git diff --check`, release version, staged files, and `.env` exclusion.
- [x] **Step 2: Review the completed chart redesign and release changes; resolve any important findings and rerun verification.**
- [ ] **Step 3: Confirm remote `main` can receive the committed release history** without overwriting newer remote work.
- [ ] **Step 4: Publish** `v2.0.0` with concise release notes, including the improved six chart views and real-data README captures.
- [ ] **Step 5: Verify** the release tag/URL and final local/remote status.

## Self-review

- Spec coverage: all seven displayed states (six reports plus the employee single-year state), axes, labels, colors, themes, responsive screenshots, real-data sourcing, versioning, and publication have explicit tasks.
- Test coverage: empty/missing-quarter data, all-years vs. one-year filters, minimum card width, both themes, export snapshots, connection failure, and actual database screenshots are assigned to the responsible tasks.
- Scope: no new reporting features or dependencies; all tasks retain the existing dashboard and use the requested local `main` workflow.
