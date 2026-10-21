$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & mvn clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed' }
    & java -Xmx2g -jar target/assignment3-graphs.jar benchmark
    if ($LASTEXITCODE -ne 0) { throw 'Benchmark validation failed' }
    & python scripts/report.py
    if ($LASTEXITCODE -ne 0) { throw 'Report generation failed; install scripts/requirements.txt' }
} finally { Pop-Location }
