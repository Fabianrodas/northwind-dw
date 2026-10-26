# Chart Detail and Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users open each Northwind report in an accessible, responsive detail view and export it as a PDF or download the chart image on a phone through a QR code.

**Architecture:** Keep one JavaFX stage and the existing dashboard header, filters, theme, and full-dashboard actions. Swap the report grid for a detail panel that reuses the selected chart; build the PDF from filtered data and encode a short-lived local download link in the QR.

**Tech Stack:** Java 21, JavaFX 21.0.10, PDFBox 3.0.8, ZXing Core 3.5.4, Maven, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-26-chart-detail-export-design.md`

## Global Constraints

- Work only on `main`; do not create branches or worktrees.
- Preserve the current dashboard, six reports, year filter, refresh, full-dashboard PDF, SQL export, and light/dark themes.
- Do not send chart images to an external host. Serve only the current PNG through a random, temporary URL on one private network interface.
- The QR download link expires after 10 minutes and closes when the application exits; scans require the phone and computer on the same local network.
- Detail PDF and QR content must represent the active report and selected period.
- Keep layout dimensions responsive and all new controls accessible by keyboard.

## Review Focus

- Reopening a report after changing year must show the latest filtered rows and period in both exports; test report-content generation with both `null` and a selected year.
- Empty report data must still produce a readable PDF and valid, scannable image link; test an empty `DashboardData` for every report type.
- PDF output must contain exactly the selected report and its chart image; assert report headings, excluded headings, and one embedded image.
- QR download links must round-trip through ZXing for all report types. The local endpoint must serve only the selected PNG as an attachment, reject unknown paths/methods, and close on exit or expiration.
- Returning from detail must restore the same grid position and theme; verify through a JavaFX view test at wide and narrow widths.

## File Map

- Create `DashboardReport.java`: stable identifiers and labels for the six charts.
- Create `DashboardReportContent.java`: filtered report title, context, period, and table rows.
- Create `DashboardQrService.java`: local QR matrix generation and PNG saving.
- Create `ChartDownloadServer.java`: short-lived download of one chart PNG over the local network.
- Modify `DashboardExporter.java`: export a selected report, chart snapshot, and its rows to PDF.
- Modify `DashboardController.java` and `dashboard.fxml`: accessible detail navigation and filter-aware actions.
- Modify `dashboard.css`: detail layout, focus, hover, and responsive chart styling in both themes.
- Extend test classes for report content, QR decoding, PDF image/text, and the detail navigation flow.
- Modify `pom.xml`, `THIRD-PARTY-NOTICES.md`, `README.md`, and screenshots for the new QR dependency and user-facing feature.

---

### Task 1: Define report content and local QR export

**Files:**
- Create: `src/main/java/com/northwinddw/model/DashboardReport.java`
- Create: `src/main/java/com/northwinddw/model/DashboardReportContent.java`
- Test: `src/test/java/com/northwinddw/model/DashboardReportContentTest.java`
- Create: `src/main/java/com/northwinddw/service/DashboardQrService.java`
- Test: `src/test/java/com/northwinddw/service/DashboardQrServiceTest.java`
- Modify: `pom.xml`
- Modify: `THIRD-PARTY-NOTICES.md`

**Interfaces:**
- `DashboardReport` identifies the six reports and supplies each display title and short context.
- `DashboardReportContent.from(DashboardReport report, DashboardData data, Integer selectedYear)` returns immutable `title`, `context`, `period`, `headers`, `rows`, and `qrText` values.
- `DashboardQrService.encode(String text, int size)` returns a ZXing `BitMatrix`; `savePng(BitMatrix matrix, Path destination)` writes a PNG.

- [x] **Step 1: Add failing report-content tests** for all six mappings, selected/all-year labels, report rows, empty datasets, and a QR summary containing only the selected report.
- [x] **Step 2: Run the focused content test** with Maven; confirm it fails because the report definitions and content builder are missing.
- [x] **Step 3: Implement the report enum and immutable content builder** with deterministic row order and a short human-readable context for each existing chart.
- [x] **Step 4: Run the focused content test** and confirm all six cases pass, including empty lists and selected-year data.
- [x] **Step 5: Add ZXing Core 3.5.4** and add its Apache 2.0 notice to `THIRD-PARTY-NOTICES.md`.
- [x] **Step 6: Add QR round-trip tests** that encode each report summary—including empty report data—save/read a PNG, decode it with ZXing, and assert the original text is recovered.
- [x] **Step 7: Implement `DashboardQrService`** using the dependency-free `core` artifact, rejecting blank text and invalid image sizes with clear exceptions.
- [x] **Step 8: Run the focused QR tests** and confirm the QR is readable for all six reports and long labels.
- [x] **Step 9: Commit** as `feat: build report summaries and QR export`.

### Task 2: Export the selected chart as a PDF

**Files:**
- Modify: `src/main/java/com/northwinddw/service/DashboardExporter.java`
- Modify: `src/test/java/com/northwinddw/service/DashboardExporterTest.java`

**Interfaces:**
- `DashboardExporter.exportReportPdf(DashboardReportContent content, BufferedImage chartImage, Path destination)` writes only the selected report, its chart image, context, period, and rows.
- Existing `exportPdf(DashboardData, Path)` remains the full-dashboard export used from the home toolbar.
- `DashboardController` captures the JavaFX chart on the UI thread, converts that snapshot to `BufferedImage`, and then runs PDF writing in the existing background executor.

- [x] **Step 1: Add a failing PDF test** that passes a known chart image and one report content object, then checks the PDF text, page resources, one embedded image, and absence of the other five report headings.
- [x] **Step 2: Run the focused PDF test** with Maven; confirm the report-specific exporter method is missing.
- [x] **Step 3: Implement `exportReportPdf`** with PDFBox and a single-page chart image scaled inside the printable area, followed by the matching report rows with overflow pagination.
- [x] **Step 4: Run PDF exporter tests** and confirm the selected report, image, values, pagination, and unwritable-path error behavior pass.
- [x] **Step 5: Commit** as `feat: export individual report charts to PDF` (`288b3a1`).

### Task 3: Add the detail view and persistent dashboard controls

**Files:**
- Modify: `src/main/resources/fxml/dashboard.fxml`
- Modify: `src/main/resources/css/dashboard.css`
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Test: `src/test/java/com/northwinddw/ui/DashboardDetailViewTest.java`

**Interfaces:**
- `DashboardController.openDetail(DashboardReport)` swaps the grid for its detail, moves the selected existing chart into the detail host, and records the prior scroll position.
- `DashboardController.returnToGrid()` restores the chart to its card, returns to the previous grid position, and moves keyboard focus back to the selected report.
- `DashboardController.exportSelectedReportPdf()` and `showSelectedReportQr()` use `currentData`, the active year, and the selected report.

- [x] **Step 1: Add failing JavaFX view tests** for opening each report, accessible Enter/Space activation, returning to the grid, preserving theme/period/scroll, and changing the selected year while in detail.
- [x] **Step 2: Run the focused view test**; confirm it fails because the detail panel and navigation are not present.
- [x] **Step 3: Add a responsive detail panel** with Back, title, context, a growable chart host, `Exportar gráfico PDF`, `Generar QR`, and `Guardar QR como PNG` in the QR dialog.
- [x] **Step 4: Make each existing report card keyboard-focusable and clickable**, and wire its accessible name and visible focus state to the matching report.
- [x] **Step 5: Swap the grid and detail panel without replacing the stage**, hiding summary cards in detail while keeping theme, year, refresh, full PDF, SQL, and connection state in the shared header.
- [x] **Step 6: Reuse and restore the original chart node**, preserve scroll/selection on back, and keep chart sizing responsive when the detail viewport changes.
- [x] **Step 7: Wire report-specific PDF export and the QR preview/save flow**, with errors shown beside the relevant action and the current theme applied to dialogs.
- [x] **Step 8: Run the focused JavaFX tests** at wide and narrow widths and confirm all report, navigation, filtering, theme, and accessibility assertions pass.
- [x] **Step 9: Commit** as `feat: add responsive chart detail navigation` (`f84febd`).

### Task 4: Document and verify the completed feature

**Files:**
- Modify: `src/test/java/com/northwinddw/ui/DashboardScreenshotTest.java`
- Create/modify: `docs/screenshots/dashboard-detail.png`, `docs/screenshots/dashboard-detail-dark.png`
- Modify: `README.md`
- Modify: `scripts/verify.ps1` only if a new static dependency/license check is needed.

- [x] **Step 1: Capture the detail view** in dark and light themes using deterministic test data, save `dashboard-detail-dark.png` and `dashboard-detail.png`, and inspect both at desktop and narrow widths.
- [x] **Step 2: Add the detail screenshot and explain the drill-down, chart PDF, and QR behavior** in the README without replacing existing light/dark/error captures.
- [x] **Step 3: Run `.\scripts\verify.ps1 -WithDatabaseIntegration`** and confirm static checks, all tests, package build, database integration, and clean credential checks pass.
- [x] **Step 4: Launch `mvn javafx:run`** and verify all six cards, keyboard/mouse navigation, year changes, theme, and responsive sizing in the JavaFX view test; use the live PostgreSQL app to confirm a chart PDF and QR preview/PNG save; use the ZXing round-trip test to verify QR readability.
- [x] **Step 5: Verify the working tree stays on `main`, `.env` remains ignored, and no project data or credentials entered Git.**
- [x] **Step 6: Commit** as `docs: document chart details and exports`.

### Task 5: Make QR scanning download the chart and add the Northwind logo

**Files:**
- Create: `src/main/java/com/northwinddw/service/ChartDownloadServer.java`
- Test: `src/test/java/com/northwinddw/service/ChartDownloadServerTest.java`
- Modify: `src/main/java/com/northwinddw/ui/DashboardController.java`
- Modify: `src/main/resources/fxml/dashboard.fxml`, `src/main/resources/css/dashboard.css`, and `src/main/java/com/northwinddw/Main.java`
- Create: `src/main/resources/images/northwind.png`
- Modify: `DashboardReportContent` and its tests to remove the obsolete plain-text QR summary.
- Modify: `README.md`, `SECURITY.md`, this spec, and generated screenshots.

**Interfaces:**
- `ChartDownloadServer.startForLocalNetwork(byte[] image, String fileName)` serves one PNG on a private IPv4 address and returns an opaque download URI.
- `ChartDownloadServer.close()` disables the link and releases its bounded worker resources.
- The QR encoder receives the download URI; Safari receives `Content-Disposition: attachment` with the chart PNG.

- [x] **Step 1: Add failing tests** for the limited download route, exact PNG attachment headers/body, rejection of unknown paths and methods, server shutdown, a decodable URL QR, logo loading, and the arrow-free Back label.
- [x] **Step 2: Run focused tests** and confirm the JavaFX view test detects the missing logo. The first local server test also exposed a Java 21 `HttpServer` selector failure in this environment; isolated evidence showed a standard loopback `ServerSocket` works.
- [x] **Step 3: Implement the bounded local socket service** with a random token, one exact download route, capped request/header sizes, request timeouts, a 10-minute expiry, and shutdown cleanup.
- [x] **Step 4: Connect QR generation to the rendered chart PNG**, retain saved QR behavior, clean up links on app exit, and remove the former plain-text summary payload.
- [x] **Step 5: Add the supplied logo to the app header/window icon, remove the arrow from «Regresar», and update the README/security notes and both-theme screenshots.**
- [x] **Step 6: Run the local download and full verification tests, launch the JavaFX app, inspect the layout, and commit the finished changes on `main`.**

## Self-review

- Coverage: six report definitions, filtered data, same-window navigation, Back, persistent theme and controls, single-report PDF, QR preview/PNG, responsive behavior, accessibility, README, and screenshots all have explicit steps.
- Inputs: all-years vs selected year, each chart type, empty results, long QR text, and narrow/wide windows are covered in the tasks above.
- Compatibility: the full-dashboard PDF/SQL actions and database queries remain unchanged; only a small QR encoder is added and no hosted service is introduced.
- Execution: Native implementation in this session on `main`, as requested; no branch or worktree is used.
