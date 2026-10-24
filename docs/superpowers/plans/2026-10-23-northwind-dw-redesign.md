# Northwind DW Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the local Northwind dashboard with secure configuration, a testable data layer, responsive JavaFX views, reliable exports, and plain-language setup instructions.

**Architecture:** Keep JavaFX and PostgreSQL, with separate config, repository, model, service, and UI packages. Load all reports on a background task through one JDBC connection, and export the current report data rather than taking asynchronous UI screenshots.

**Tech Stack:** Java 21, JavaFX 21, Maven, PostgreSQL JDBC, JUnit 5, iText PDF.

**Spec:** `docs/superpowers/specs/2026-10-23-northwind-dw-redesign-design.md`

## Global Constraints

- Work only on branch `main`; do not create a branch or worktree.
- Do not copy `postgresql_login.txt` or commit `.env`.
- Load `NORTHWIND_DB_HOST`, `NORTHWIND_DB_PORT`, `NORTHWIND_DB_NAME`, `NORTHWIND_DB_USER`, and `NORTHWIND_DB_PASSWORD` from environment variables or `.env`.
- Keep monetary values as `BigDecimal` and use prepared statements for report filters.
- Keep the SQL download portable and non-destructive to an existing database.
- Use one database connection for a dashboard refresh and keep database work off the JavaFX thread.
- Preserve all six reports, PDF dashboard export, and SQL download; remove the fixed external QR action and splash animation.

## Review Focus

- PostgreSQL is unavailable at launch: show a useful connection state and keep the window responsive; test failed connection handling and empty data.
- Malformed port or missing environment file: validate defaults and report invalid configuration without revealing the password; test config parsing and environment precedence.
- Decimal amounts and categories with no revenue: preserve scale and avoid division by zero; test query results against the temporary PostgreSQL fixture.
- More employees and years than the five hard-coded examples: render all returned rows; test query result counts against the source database.
- Export directory or file errors: keep the app open and show the error; test generated PDF/SQL files are non-empty and have expected signatures/content.

---

### Task 1: Secure configuration and repository foundation

**Files:**
- Create: `.gitignore`
- Create: `.env.example`
- Create: `docs/engineering-review.md`
- Create: `docs/superpowers/specs/2026-10-23-northwind-dw-redesign-design.md`
- Create: `docs/superpowers/plans/2026-10-23-northwind-dw-redesign.md`

**Interfaces:** No runtime interfaces. The example file defines the five `NORTHWIND_DB_*` keys used by later tasks.

- [ ] Record behavior, static findings, and the failed no-database launch in `docs/engineering-review.md`.
- [ ] Ignore `.env`, credential text files, Maven output, and temporary PostgreSQL data; keep `.env.example` trackable.
- [ ] Commit the documentation and security baseline as `docs: record Northwind review and secure config baseline`.

### Task 2: Database configuration and analytics repository

**Files:**
- Create: `src/main/java/com/northwinddw/config/DatabaseConfig.java`
- Create: `src/main/java/com/northwinddw/config/EnvFile.java`
- Create: `src/main/java/com/northwinddw/data/ConnectionFactory.java`
- Create: `src/main/java/com/northwinddw/data/AnalyticsRepository.java`
- Create: `src/main/java/com/northwinddw/model/DashboardData.java`
- Create: `src/main/java/com/northwinddw/model/RevenueSummary.java`
- Create: `src/main/java/com/northwinddw/model/ReportRows.java`
- Create: `src/test/java/com/northwinddw/config/DatabaseConfigTest.java`
- Create: `src/test/java/com/northwinddw/data/AnalyticsRepositoryIntegrationTest.java`
- Modify: `pom.xml`

**Interfaces:** `DatabaseConfig.from(Map<String,String>, Path)` returns validated connection settings. `ConnectionFactory.open()` returns a new `Connection`. `AnalyticsRepository.loadDashboard(Integer year)` returns `DashboardData` and owns one connection for its seven prepared queries.

- [ ] Test `.env` parsing, system-environment precedence, defaults, and invalid ports.
- [ ] Run `mvn test` and confirm the new config tests fail before implementation.
- [ ] Add typed report records with `BigDecimal` monetary values and implement the six analytics plus summary query.
- [ ] Run unit tests and the opt-in integration test against the isolated PostgreSQL fixture; check one connection is used per load and no fixed employee list remains.
- [ ] Commit as `feat: add secure config and analytics repository`.

### Task 3: Responsive JavaFX dashboard

**Files:**
- Create: `src/main/java/com/northwinddw/Main.java`
- Create: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Create: `src/main/resources/fxml/dashboard.fxml`
- Create: `src/main/resources/css/dashboard.css`
- Remove: old splash, singleton, controller, and model sources from the empty destination if any were introduced.

**Interfaces:** `DashboardController` receives `AnalyticsRepository` and `DashboardExporter`; refresh loads `DashboardData` in a JavaFX `Task` and updates all six chart cards on the UI thread.

- [ ] Build a six-card dashboard with text labels, visible keyboard focus, tooltips, refresh, PDF, and SQL actions.
- [ ] Add loading, connected, empty-result, and failed-connection states.
- [ ] Remove splash and decorative transitions; use a resizable window and responsive card layout.
- [ ] Run the app against the temporary database, verify all six cards and filter refresh, then run it with the DB stopped and verify the app stays responsive.
- [ ] Commit as `feat: build responsive dashboard and loading states`.

### Task 4: Portable SQL and data exports

**Files:**
- Create: `src/main/java/com/northwinddw/service/DashboardExporter.java`
- Create: `src/main/resources/db/northwind_dw.sql`
- Create: `src/test/java/com/northwinddw/service/DashboardExporterTest.java`
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`

**Interfaces:** `DashboardExporter.exportPdf(DashboardData, Path)` writes a PDF summary. `DashboardExporter.copySql(Path)` copies the single packaged SQL resource to the chosen destination.

- [ ] Convert the source dump into a portable, non-destructive script without database creation, `DROP DATABASE`, local ownership, or credentials.
- [ ] Test PDF signature and text, SQL export content, empty paths, and write failures.
- [ ] Run exports from the UI against real report rows and confirm the files open and are non-empty.
- [ ] Commit as `feat: add portable SQL and report exports`.

### Task 5: User guide, screenshots, and final verification

**Files:**
- Create/modify: `README.md`
- Create: `docs/screenshots/dashboard.png`
- Create: `docs/screenshots/connection-error.png`
- Create: `scripts/verify.ps1`
- Modify: `docs/engineering-review.md`

**Interfaces:** `scripts/verify.ps1` runs the Maven unit/build checks and optional PostgreSQL integration checks when a test JDBC URL is provided.

- [ ] Write Spanish run instructions in plain language and embed screenshots of the working dashboard and connection message.
- [ ] Run `mvn test`, `mvn package`, static secret/tracked-file checks, all report queries, export checks, and visual inspection.
- [ ] Drop only the temporary test database, stop its PostgreSQL server, and remove the temporary cluster.
- [ ] Confirm `.env` is ignored and no secrets or test data are staged.
- [ ] Commit as `docs: add user guide screenshots and verification notes`.
