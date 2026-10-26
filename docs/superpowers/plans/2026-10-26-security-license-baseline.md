# Security and licensing baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish Northwind DW with a practical local security baseline, correct MIT licensing, and maintained compatible dependencies.

**Architecture:** The JavaFX app keeps using local PostgreSQL through JDBC, but requires explicit credentials and uses a read-only connection/account. Apache PDFBox replaces iText so the existing PDF feature can ship with the MIT project without the current AGPL dependency.

**Tech Stack:** Java 21, JavaFX 21, PostgreSQL JDBC 42.7.14, Apache PDFBox 3.0.8, Maven, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-26-security-license-baseline-design.md`

## Global Constraints

- Preserve Java 21, JavaFX, PostgreSQL, dashboard behavior, and the existing `main` branch.
- Keep `.env` local and ignored; `.env.example` must not contain a usable password.
- Leave the local database available after verification.
- Do not retain administrator credentials in application configuration after database setup.
- Do not add production hosting, authentication, CI, or online services.

## Review Focus

- Missing, blank, or whitespace-only PostgreSQL password must stop startup with a clear message and no secret in output.
- A configured non-empty password must reach JDBC unchanged and must never appear in `DatabaseConfig.toString()`.
- The app's database connection and provisioned account must reject write operations while still allowing the dashboard's SELECT queries.
- PDF export must remain readable, preserve report values, handle page overflow, and propagate unwritable destination errors.
- SQL import and app login must work without tracking `.env` or a password file.

## File Map

- `DatabaseConfig.java` / `DatabaseConfigTest.java`: require explicit passwords and safe defaults.
- `ConnectionFactory.java` / integration tests: create read-only sessions.
- `src/main/resources/db/create_readonly_user.sql`: provision the minimum app privileges.
- `DashboardExporter.java` / `DashboardExporterTest.java`: generate the same report with PDFBox.
- `pom.xml`: set MIT metadata, update fixed dependencies, remove iText.
- `LICENSE`, `THIRD-PARTY-NOTICES.md`, `SECURITY.md`, `.env.example`, `README.md`: publish clear reuse, setup, and security guidance.

---

### Task 1: Require safe read-only database access

**Files:**
- Modify: `src/test/java/com/northwinddw/config/DatabaseConfigTest.java`
- Modify: `src/main/java/com/northwinddw/config/DatabaseConfig.java`
- Modify: `.env.example`
- Create: `src/main/resources/db/create_readonly_user.sql`
- Modify: `src/main/java/com/northwinddw/data/ConnectionFactory.java`
- Modify: `src/test/java/com/northwinddw/data/AnalyticsRepositoryIntegrationTest.java`

- [ ] Add failing tests that reject missing and blank passwords, accept an explicit password, and confirm password redaction.
- [ ] Run `mvn -Dtest=DatabaseConfigTest test`; confirm the new missing-password assertion fails because empty credentials are currently accepted.
- [ ] Make `DatabaseConfig.from()` require a non-empty password, default the username to `northwind_dw_app`, and keep password redaction.
- [ ] Change `.env.example` to `NORTHWIND_DB_USER=northwind_dw_app` and an empty password field.
- [ ] Add a one-time SQL script that creates the `northwind_dw_app` login and grants CONNECT, schema USAGE, and SELECT only on current/future public tables.
- [ ] Make `ConnectionFactory.from(DatabaseConfig)` mark every opened JDBC connection read-only before returning it.
- [ ] Extend the database integration test to confirm the app can query dashboard data and cannot perform a write.
- [ ] Run `mvn -Dtest=DatabaseConfigTest test` and the database integration test against the local read-only role; confirm all assertions pass.
- [ ] Commit as `security: require explicit read-only database access`.

### Task 2: Replace the incompatible PDF dependency

**Files:**
- Modify: `src/test/java/com/northwinddw/service/DashboardExporterTest.java`
- Modify: `src/main/java/com/northwinddw/service/DashboardExporter.java`
- Modify: `pom.xml`

- [ ] Update the PDF test to load the exported file with PDFBox and assert its extracted report text includes `$123.45`, `1996`, and section headings.
- [ ] Run `mvn -Dtest=DashboardExporterTest test`; confirm the test cannot compile because the PDFBox dependency/API has not been added.
- [ ] Replace `com.itextpdf:layout` with `org.apache.pdfbox:pdfbox:3.0.8` and render a paginated PDF using embedded standard fonts, with title, summary, table headings, and report rows.
- [ ] Run `mvn -Dtest=DashboardExporterTest test`; confirm text, SQL-copy, and unwritable-path tests pass.
- [ ] Commit as `refactor: use Apache PDFBox for report exports`.

### Task 3: Add license, dependency metadata, and security guidance

**Files:**
- Modify: `pom.xml`
- Create: `LICENSE`
- Create: `THIRD-PARTY-NOTICES.md`
- Create: `SECURITY.md`
- Modify: `.gitignore`
- Modify: `README.md`
- Modify: `scripts/verify.ps1`

- [ ] Add the standard MIT license text for Northwind DW's own source.
- [ ] List direct third-party libraries with their separate licenses and link to license texts; clarify that the MIT license does not replace those terms.
- [ ] Document the project's academic/local scope, no supported release or response guarantee, and avoiding confidential real data.
- [ ] Update README setup to create the database, provision the read-only role, place only app credentials in ignored `.env`, and run the existing dashboard. Preserve the screenshots and theme/responsive notes.
- [ ] Add static verification that `.env` is ignored and not tracked, and that the sample has no usable password.
- [ ] Run `.scriptserify.ps1`; confirm all existing and new checks pass.
- [ ] Commit as `docs: add MIT license and security guidance`.

### Task 4: Provision the local database and verify the full app

**Files:**
- Local only: `.env` (never stage or commit)
- Existing: `src/main/resources/db/northwind_dw.sql`

- [ ] Verify the configured PostgreSQL admin credential without printing it; inspect the target database before creating anything.
- [ ] Create and load `northwind_pentaho` only if it does not already contain the project schema; preserve any existing database data.
- [ ] Create or provision `northwind_dw_app` with read-only privileges, set its password locally, and replace admin credentials in `.env` with the app login.
- [ ] Run unit and DB integration tests, build the package, and launch `mvn javafx:run` to verify the dashboard connects and renders real data.
- [ ] Confirm the app role cannot change database data and `git status` shows no tracked credentials.
- [ ] Commit any remaining project changes only after verification; leave `.env` and the local database intact.

## Self-review

- Spec coverage: credentials and privilege boundaries (Task 1), compatible PDF/licensing (Tasks 2–3), static verification and user guidance (Task 3), local dynamic verification (Task 4).
- Interfaces: `ConnectionFactory.from(DatabaseConfig)` remains the app's connection provider; `DashboardExporter.exportPdf(DashboardData, Path)` and `copySql(Path)` remain unchanged for callers.
- Failure modes: missing credentials, write attempts, PDF export failures, existing database preservation, and accidental `.env` staging are assigned explicit checks.
- Scope stays limited to the user's local academic desktop app; no long-lived service or CI is added.
