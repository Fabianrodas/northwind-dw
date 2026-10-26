-- Run once as the database administrator while connected to northwind_pentaho.
-- Set the role password separately in pgAdmin or with `\password northwind_dw_app` in psql.
-- Do not put a password in this file.

CREATE ROLE northwind_dw_app LOGIN;
GRANT CONNECT ON DATABASE northwind_pentaho TO northwind_dw_app;
GRANT USAGE ON SCHEMA public TO northwind_dw_app;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO northwind_dw_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO northwind_dw_app;
