#!/usr/bin/env pwsh
# Run WebBootExample with proper Java module environment
# Usage: ./run-example.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Build the project
Write-Host "Building project..." -ForegroundColor Cyan
mvn clean install -DskipTests -q
if ($LASTEXITCODE -ne 0) {
	Write-Host "Build failed!" -ForegroundColor Red
	exit 1
}

# Get dependency classpath to a temp file
Write-Host "Resolving dependencies..." -ForegroundColor Cyan
$tempFile = [System.IO.Path]::GetTempFileName()
mvn -pl druvu-lib-web-example dependency:build-classpath "-Dmdep.outputFile=$tempFile" -DincludeScope=runtime -q
$depClasspath = Get-Content $tempFile -Raw
Remove-Item $tempFile

# Locate the example jar by glob, not by a hard-coded version — the pom's version moves and a
# literal here silently goes stale. Exclude the sources/javadoc jars the package phase also builds.
$exampleJar = Get-ChildItem "druvu-lib-web-example/target/druvu-lib-web-example-*.jar" |
	Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
	Select-Object -First 1
if (-not $exampleJar) {
	Write-Host "Example jar not found in druvu-lib-web-example/target" -ForegroundColor Red
	exit 1
}

# Build module path using JARs
$modulePath = @(
	$exampleJar.FullName
	$depClasspath.Trim()
) -join [System.IO.Path]::PathSeparator

# Open browser after a short delay (background job)
Start-Job -ScriptBlock {
	Start-Sleep -Seconds 3
	Start-Process "http://localhost:8081/web-test/"
} | Out-Null

# Run with module path
Write-Host "Running WebBootExample on port 8081..." -ForegroundColor Cyan
java --module-path $modulePath --add-modules ALL-MODULE-PATH --module com.druvu.web.example/com.druvu.web.example.WebBootExample
