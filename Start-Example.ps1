#!/usr/bin/env pwsh
# Run one of the examples.
#   ./Start-Example.ps1          the demo site (druvu-lib-web-example-web) on http://localhost:8081/web-test/
#   ./Start-Example.ps1 mcp      the MCP server (druvu-lib-web-example-mcp) on http://localhost:8082/mcp
#   ./Start-Example.ps1 react    the React app (druvu-lib-web-example-react) on http://localhost:8083/
#
# The project builds on JDK 25 (pom java.version; the enforcer refuses any other line, because the
# formatter cannot run on newer javac internals). When Use-Jdk25.ps1, the session-scoped JDK switcher
# from common-scripts, is on PATH, the script switches this process to it first; without it, the
# active java must already be a JDK 25. Keep this file ASCII: PowerShell 5.1 reads a BOM-less file
# as ANSI, so one non-ASCII character in a comment breaks the script there and nowhere else.
param(
	[ValidateSet('web', 'mcp', 'react')]
	[string]$Example = 'web'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
	$useJdk = Get-Command 'Use-Jdk25.ps1' -ErrorAction SilentlyContinue
	if ($useJdk) {
		. $useJdk.Path
	}
	if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
		throw 'No java on PATH - switch to a JDK 25 first (Use-Jdk25.ps1).'
	}

	# Build the example and what it depends on. package, not install: the example only needs the jars
	# in target/, and an install would put SNAPSHOT jars into the shared ~/.m2 that other builds resolve.
	Write-Host "Building druvu-lib-web-example-$Example..." -ForegroundColor Cyan
	mvn -q clean package -DskipTests -pl "druvu-lib-web-examples/druvu-lib-web-example-$Example" -am
	if ($LASTEXITCODE -ne 0) {
		Write-Host "Build failed!" -ForegroundColor Red
		exit 1
	}

	# Locate the jar by glob, not by a hard-coded version: the pom's version moves and a literal here
	# silently goes stale. Exclude the sources/javadoc jars the package phase also builds.
	$jar = Get-ChildItem "druvu-lib-web-examples/druvu-lib-web-example-$Example/target/druvu-lib-web-example-$Example-*.jar" |
			Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
			Select-Object -First 1
	if (-not $jar) {
		Write-Host "Example jar not found in druvu-lib-web-examples/druvu-lib-web-example-$Example/target" -ForegroundColor Red
		exit 1
	}

	# Each example is repackaged by spring-boot-maven-plugin into a self-contained jar (BOOT-INF/classes
	# + BOOT-INF/lib), so it launches with `java -jar` and carries its own dependencies. Do NOT try to
	# run it on the module path: after repackaging the main class lives under BOOT-INF/classes.
	if ($Example -eq 'web') {
		Start-Job -ScriptBlock {
			Start-Sleep -Seconds 3
			Start-Process "http://localhost:8081/web-test/"
		} | Out-Null
		Write-Host "Running the demo site on http://localhost:8081/web-test/ ..." -ForegroundColor Cyan
	} elseif ($Example -eq 'react') {
		Start-Job -ScriptBlock {
			Start-Sleep -Seconds 3
			Start-Process "http://localhost:8083/"
		} | Out-Null
		Write-Host "Running the React app on http://localhost:8083/ ..." -ForegroundColor Cyan
	} else {
		Write-Host "Running the MCP server on http://localhost:8082/mcp - the token is printed below." -ForegroundColor Cyan
	}
	java -jar $jar.FullName
	exit $LASTEXITCODE
}
finally {
	Pop-Location
}
