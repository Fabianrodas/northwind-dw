# Security and licensing baseline

## Intent

Prepare Northwind DW for a public academic repository: protect local database credentials, reduce the application's database privileges, correct the project's license, and remove avoidable dependency security and licensing risks without adding services that require ongoing operation.

## Goals

- Require explicit PostgreSQL credentials from environment variables or an ignored `.env` file; never accept a blank password or ship a usable password default.
- Recommend and document a read-only database account, and make application connections read-only as a second guard.
- Keep all database values parameterized.
- Use a PDF dependency whose license can accompany an MIT-licensed project and update dependencies with relevant security fixes.
- Add concise MIT, third-party licensing, and security guidance suited to an academic desktop application.
- Verify configuration, tests, packaging, database access, and PDF export without committing local credentials.

## Non-goals

- Production hosting, authentication, user roles, automated release infrastructure, or long-term vulnerability response.
- Treating the public Northwind sample dataset as private or sensitive data.
- Reworking the dashboard's appearance or changing its report contents.

## Constraints

- Preserve Java 21, JavaFX, PostgreSQL, the current dashboard behavior, and the existing `main` branch.
- Keep `.env` local and ignored. Keep `.env.example` safe to publish.
- Leave the locally configured database available for the user after verification.
- Do not store administrator credentials in the application configuration after provisioning.

## Approaches

1. Add only a license and a disclaimer. This is small but leaves an unsafe administrator-account default, a stale vulnerable JDBC driver, and a PDF dependency whose license conflicts with the project's MIT intent.
2. Add local credential and read-only protections, update the affected database driver, replace iText with Apache PDFBox, and document licensing and security limits. This addresses concrete risks while keeping the project local and self-contained. **Selected.**
3. Add continuous scanning, hosted deployment controls, and a formal response process. This adds maintenance overhead that does not fit a single-user academic desktop project.

## Implementation decisions

- Treat an unset or blank database password as a configuration error.
- Use a dedicated read-only PostgreSQL login for the app; the administrator login is only for preparing the database.
- Replace iText with the current supported PDFBox 3.0 line and retain the existing PDF report content.
- Document that the MIT license covers Northwind DW's own code and that third-party libraries retain their own licenses.
- State plainly that this academic sample has no promise of ongoing security maintenance and should not be used with confidential production data.
- Keep verification local and avoid adding CI or online services.
