param(
    [switch]$WithDatabaseIntegration
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$savedTestEnvironment = @{}
Push-Location $projectRoot

try {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "No se encontró Maven. Instala Maven y vuelve a ejecutar este script."
    }
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw "No se encontró Git; se necesita para revisar qué archivos están versionados."
    }
    $netbeansActionsPath = Join-Path $projectRoot "nbactions.xml"
    if (-not (Test-Path $netbeansActionsPath)) {
        throw "NetBeans debe iniciar la aplicación con el objetivo javafx:run definido en nbactions.xml."
    }
    [xml]$netbeansActions = Get-Content -Raw $netbeansActionsPath
    $runAction = @($netbeansActions.actions.action | Where-Object { $_.actionName -eq "run" })
    if ($runAction.Count -ne 1 -or @($runAction[0].goals.goal) -notcontains "javafx:run") {
        throw "La acción Run de NetBeans debe usar javafx:run para iniciar la aplicación JavaFX."
    }
    if ($WithDatabaseIntegration) {
        $localDatabaseSettings = @{}
        if (Test-Path -LiteralPath ".env") {
            foreach ($sourceLine in Get-Content -LiteralPath ".env") {
                $line = $sourceLine.Trim()
                if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
                    continue
                }
                if ($line.StartsWith("export ")) {
                    $line = $line.Substring(7).Trim()
                }
                $separator = $line.IndexOf("=")
                if ($separator -le 0) {
                    continue
                }

                $key = $line.Substring(0, $separator).Trim()
                $value = $line.Substring($separator + 1).Trim()
                if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                    ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                    $value = $value.Substring(1, $value.Length - 2)
                } else {
                    $inlineComment = $value.IndexOf(" #")
                    if ($inlineComment -ge 0) {
                        $value = $value.Substring(0, $inlineComment).Trim()
                    }
                }
                $localDatabaseSettings[$key] = $value
            }
        }

        foreach ($suffix in @("HOST", "PORT", "NAME", "USER", "PASSWORD")) {
            $testVariable = "NORTHWIND_TEST_DB_$suffix"
            $applicationVariable = "NORTHWIND_DB_$suffix"
            $existingValue = [Environment]::GetEnvironmentVariable($testVariable, "Process")
            $savedTestEnvironment[$testVariable] = $existingValue
            if ($null -eq $existingValue -and $localDatabaseSettings.ContainsKey($applicationVariable)) {
                [Environment]::SetEnvironmentVariable($testVariable, $localDatabaseSettings[$applicationVariable], "Process")
            }
        }

        if ([string]::IsNullOrWhiteSpace($env:NORTHWIND_TEST_DB_PORT)) {
            throw "Configura la base local en .env o define NORTHWIND_TEST_DB_PORT para habilitar la integración con PostgreSQL."
        }
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

    $historicalCredentialPaths = @(& git -c $gitSafetyOption -C $projectRoot log --all --format= --name-only -- .env postgresql_login.txt "*password*.txt" "*login*.txt" | Where-Object {
        -not [string]::IsNullOrWhiteSpace($_) -and $_ -ne ".env.example"
    } | Sort-Object -Unique)
    if ($LASTEXITCODE -ne 0) {
        throw "Git no pudo revisar los nombres de archivos en el historial."
    }
    if ($historicalCredentialPaths.Count -gt 0) {
        throw "El historial contiene rutas de posibles credenciales. Cámbialas y limpia el historial antes de publicar."
    }

    $ignoreRules = Get-Content .gitignore
    if ($ignoreRules -notcontains ".env" -or $ignoreRules -notcontains "!.env.example") {
        throw "Comprueba que .gitignore excluya .env y permita compartir .env.example."
    }
    & git -c $gitSafetyOption -C $projectRoot check-ignore --quiet --no-index -- .env
    if ($LASTEXITCODE -ne 0) {
        throw "Git no está ignorando .env."
    }

    $environmentExample = Get-Content -Raw .env.example
    $samplePasswordLine = $environmentExample -split "`r?`n" | Where-Object { $_ -match '^\s*NORTHWIND_DB_PASSWORD\s*=' } | Select-Object -First 1
    if ($null -eq $samplePasswordLine -or ($samplePasswordLine -split '=', 2)[1].Trim().Length -gt 0) {
        throw ".env.example no debe incluir una contraseña utilizable."
    }

    $projectPom = Get-Content -Raw pom.xml
    if ($projectPom -match 'com\.itextpdf|itext\.version') {
        throw "La dependencia PDF debe respetar la licencia elegida para el código del proyecto."
    }
    if ($projectPom -notmatch '<postgresql\.version>42\.7\.14</postgresql\.version>' -or
        $projectPom -notmatch '<pdfbox\.version>3\.0\.8</pdfbox\.version>') {
        throw "Comprueba que el controlador PostgreSQL y PDFBox tengan las versiones revisadas."
    }
    if ($projectPom -notmatch '<name>MIT License</name>' -or
        (Get-Content -TotalCount 1 LICENSE) -ne "MIT License") {
        throw "La licencia declarada en Maven debe coincidir con el archivo LICENSE."
    }

    $readOnlySqlPath = Join-Path $projectRoot "src/main/resources/db/create_readonly_user.sql"
    $readOnlySql = Get-Content -Raw $readOnlySqlPath
    if ($readOnlySql -notmatch '(?i)GRANT\s+SELECT\s+ON\s+ALL\s+TABLES' -or
        $readOnlySql -match '(?i)\b(INSERT|UPDATE|DELETE|TRUNCATE)\b') {
        throw "El usuario de la aplicación debe conservar solo permisos de consulta."
    }

    foreach ($requiredDocumentation in @("LICENSE", "SECURITY.md", "THIRD-PARTY-NOTICES.md")) {
        if (-not (Test-Path -LiteralPath $requiredDocumentation)) {
            throw "Falta el documento $requiredDocumentation."
        }
    }

    $sqlPath = Join-Path $projectRoot "src/main/resources/db/northwind_dw.sql"
    $sql = Get-Content -Raw $sqlPath
    if ($sql -match '(?im)^\s*(DROP|CREATE|ALTER)\s+DATABASE\b|^\s*\\connect\b|^\s*ALTER\s+.*\bOWNER\s+TO\b') {
        throw "El SQL incluido contiene una orden de base de datos o propietario que no debe distribuirse."
    }

    Write-Host "Revisiones estáticas completadas: credenciales, historial, dependencias, permisos, licencia y SQL portable."
    Write-Host "Ejecutando pruebas y empaquetado Maven..."
    & mvn verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven encontró una prueba fallida o no pudo preparar la aplicación."
    }
    Write-Host "Revisión completada."
}
finally {
    Pop-Location
    foreach ($testVariable in $savedTestEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($testVariable, $savedTestEnvironment[$testVariable], "Process")
    }
}
