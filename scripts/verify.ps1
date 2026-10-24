param(
    [switch]$WithDatabaseIntegration
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot

try {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "No se encontró Maven. Instala Maven y vuelve a ejecutar este script."
    }
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw "No se encontró Git; se necesita para revisar qué archivos están versionados."
    }
    if ($WithDatabaseIntegration -and [string]::IsNullOrWhiteSpace($env:NORTHWIND_TEST_DB_PORT)) {
        throw "Define NORTHWIND_TEST_DB_PORT para habilitar la consulta opcional a PostgreSQL."
    }

    $gitSafetyOption = "safe.directory=$projectRoot"
    $trackedFiles = & git -c $gitSafetyOption -C $projectRoot ls-files
    if ($LASTEXITCODE -ne 0) {
        throw "Git no pudo leer la lista de archivos del proyecto."
    }

    $credentialFiles = @($trackedFiles | Where-Object {
        (($_ -match '(^|/)\.env($|\.)') -and $_ -ne ".env.example") -or
        ($_ -match '(^|/)postgresql_login\.txt$') -or
        ($_ -match '(^|/)[^/]*(password|login)[^/]*\.txt$')
    })
    if ($credentialFiles.Count -gt 0) {
        throw "Se encontraron archivos de acceso versionados: $($credentialFiles -join ', ')"
    }

    $ignoreRules = Get-Content .gitignore
    if ($ignoreRules -notcontains ".env" -or $ignoreRules -notcontains "!.env.example") {
        throw "Comprueba que .gitignore excluya .env y permita compartir .env.example."
    }
    & git -c $gitSafetyOption -C $projectRoot check-ignore --quiet --no-index -- .env
    if ($LASTEXITCODE -ne 0) {
        throw "Git no está ignorando .env."
    }

    $sqlPath = Join-Path $projectRoot "src/main/resources/db/northwind_dw.sql"
    $sql = Get-Content -Raw $sqlPath
    if ($sql -match '(?im)^\s*(DROP|CREATE|ALTER)\s+DATABASE\b|^\s*\\connect\b|^\s*ALTER\s+.*\bOWNER\s+TO\b') {
        throw "El SQL incluido contiene una orden de base de datos o propietario que no debe distribuirse."
    }

    Write-Host "Revisiones estáticas completadas: credenciales, .gitignore y SQL portable."
    Write-Host "Ejecutando pruebas y empaquetado Maven..."
    & mvn verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven encontró una prueba fallida o no pudo preparar la aplicación."
    }
    Write-Host "Revisión completada."
}
finally {
    Pop-Location
}
