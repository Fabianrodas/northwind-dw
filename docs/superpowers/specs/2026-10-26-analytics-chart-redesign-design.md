# Northwind Analytics Chart Redesign

## Intent

Make the six existing Northwind analyses easier to read and compare, with a chart form that matches the measure and dimension in each report. Keep the app recognizable as the existing JavaFX dashboard, preserve its responsive cards, detail navigation, year filter, PDF/QR export, and light/dark themes, and document real local database values with new README screenshots.

## Current state

The app already loads six typed report lists through `AnalyticsRepository` and renders them in JavaFX Charts. Category revenue uses a pie chart without visible values; country, customer, and supplier charts use horizontal bars but their categorical axes are unlabeled; quarterly and employee revenue use line charts without visible point labels. Most exact values are available only through tooltips.

The configured local Northwind database was queried successfully at `127.0.0.1:5432/northwind_pentaho` as `postgres`, with the existing read-only JDBC mode. It contains 1996–1998 sales, eight product categories, ten top countries/customers, nine employees, and sixteen supplier countries. Quarterly data is sparse at the dataset boundaries (two quarters in 1996, all four in 1997, and two in 1998). No database writes are part of this work.

## Goals

- Replace the category pie with a sorted horizontal bar chart for precise nominal-category comparison.
- Keep sales-by-quarter as an ordered quarter line chart with a separate, color-coded series for each year.
- Show sales-by-employee as a time trend when all years are selected, and as a ranked horizontal bar comparison when one year is selected.
- Keep top-country, top-customer, and supplier-count reports as ranked horizontal bars, with labels that state their category and measure.
- Display values directly on chart marks, retain exact values in tooltips, name both axes, format sales as USD, and use a zero baseline for bar comparisons.
- Use distinct, consistent colors for different year/employee series; use one semantic color for a single-measure ranking so color does not invent another dimension. Preserve readable contrast in light and dark themes.
- Keep chart layout responsive in the overview and detail panel, with accessible chart summaries.
- Regenerate dashboard/detail screenshots from the real local database and update the README to identify the source as local Northwind data.
- Set the Maven application version to `2.0.0`, document the release, and publish a GitHub release/tag `v2.0.0` after review and verification.

## Non-goals

- Add a charting library or change the application framework.
- Change database tables, queries, or stored data except for a justified display-order adjustment.
- Fit a regression model. Regression is a model, not an axis measurement scale; this database has only three annual observations per employee and aggregated quarterly values, so adding a fitted line would overstate evidence.
- Add a second data view, new filters, or new report definitions.

## Approaches considered

1. **Refine JavaFX Charts in place (selected).** Reuse the project's native chart controls, improve labels, axes, data-label nodes, series colors, and switch the employee view between a line and bar chart based on the selected period. This preserves the original look and existing exports while avoiding a new dependency.
2. **Add a third-party chart library.** This would offer more built-in data-label and interaction features, but adds dependency, licensing, and packaging work for six small aggregate reports.
3. **Keep all current chart types and only add color/labels.** This is the smallest edit, but it retains the pie chart's weak category comparisons and a line chart with a single year point, so it does not resolve the main readability problems.

## Visual and analytical decisions

| Report | Dimension and measure | Display |
|---|---|---|
| Quarterly sales | Ordered quarter (T1–T4); continuous sales amount; year series | Line chart; one consistent color and legend entry per year; marks and values identify observed quarters. |
| Category revenue | Nominal product category; continuous sales amount | Horizontal bars sorted highest to lowest; value plus share in tooltip and visible mark label. |
| Top countries | Nominal country; continuous sales amount | Horizontal bars sorted highest to lowest; country label and sales value. |
| Top customers | Nominal customer; continuous sales amount | Horizontal bars sorted highest to lowest; customer label and sales value. |
| Employee sales, all years | Ordered year; continuous sales amount; employee series | Line chart with one color per employee and a readable legend. |
| Employee sales, one selected year | Nominal employee; continuous sales amount | Horizontal bars sorted by sales, because a one-year line has no trend to show. |
| Suppliers by country | Nominal country; discrete supplier count | Horizontal bars sorted by count; integer scale beginning at zero. |

All charts keep exact values in accessible tooltips and chart descriptions. Visible data labels use compact currency where needed; a number-axis title identifies the full unit. Missing quarter observations remain missing and are not filled with zero. The data is aggregated and does not contain paired continuous observations suitable for scatter/regression analysis.

## Constraints

- Use Java 21 and the existing JavaFX Charts stack; add no runtime dependency.
- Preserve report IDs, database read-only behavior, year-selection semantics, and QR/PDF export of the currently displayed chart.
- Use the configured local database at port 5432 and the requested `postgres` account only for read-only loading/captures; never print, store, commit, or publish the password.
- `.env` remains local and ignored. Public screenshots contain only Northwind sample data already intended for this academic app.
- Work only on `main`; create no branches or worktrees. Commit each independently verifiable part.
- Publish `v2.0.0` only after static checks, automated tests, database-backed screenshot review, and a final code review pass.

## Acceptance criteria

1. Each report has a semantically appropriate chart, readable axis titles/units, visible data values, and a full exact-value tooltip.
2. Quarter/year and employee/year series are chronologically ordered and differentiated without relying on color alone.
3. Selecting one year switches employee sales to a ranked bar chart; returning to all years restores the employee time trend.
4. The labels and axes remain legible at overview and detail widths in both themes; no chart mark is clipped at the minimum supported card width.
5. Existing PDF and QR workflows export the same chart that is visible for the chosen period.
6. README overview/detail screenshots are regenerated from the local database, not test fixtures.
7. Static verification, all tests, database integration, package build, and live JavaFX startup pass; `.env` is absent from the release.
8. Maven project version and GitHub release tag are `2.0.0` / `v2.0.0`.
