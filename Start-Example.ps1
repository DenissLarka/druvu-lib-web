#!/usr/bin/env pwsh
# Run WebBootExample, the demo app for druvu-lib-web.
# Usage: ./Start-Example.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Build the project
Write-Host "Building project..." -ForegroundColor Cyan
mvn clean install -DskipTests -q
if ($LASTEXITCODE -ne 0) {
	Write-Host "Build failed!" -ForegroundColor Red
	exit 1
}

# Locate the example jar by glob, not by a hard-coded version — the pom's version moves and a
# literal here silently goes stale. Exclude the sources/javadoc jars the package phase also builds.
$exampleJar = Get-ChildItem "druvu-lib-web-example/target/druvu-lib-web-example-*.jar" |
	Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
	Select-Object -First 1
if (-not $exampleJar) {
	Write-Host "Example jar not found in druvu-lib-web-example/target" -ForegroundColor Red
	exit 1
}

# The example module is repackaged by spring-boot-maven-plugin into a self-contained jar
# (BOOT-INF/classes + BOOT-INF/lib), so it launches with `java -jar` and carries its own
# dependencies. Do NOT try to run it on the module path: after repackaging the main class
# lives under BOOT-INF/classes and `--module` cannot resolve it.
Start-Job -ScriptBlock {
	Start-Sleep -Seconds 3
	Start-Process "http://localhost:8081/web-test/"
} | Out-Null

Write-Host "Running WebBootExample on port 8081..." -ForegroundColor Cyan
java -jar $exampleJar.FullName
