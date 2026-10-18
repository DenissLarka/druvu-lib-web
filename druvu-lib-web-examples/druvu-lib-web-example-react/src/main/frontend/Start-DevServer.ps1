#!/usr/bin/env pwsh
# Start Vite's dev server for this app, on the Node the Maven build pins rather than whatever Node is installed.
#   ./Start-DevServer.ps1              serves http://localhost:5173/ and opens it; saved edits show up at once
#   ./Start-DevServer.ps1 -NoBrowser   the same without opening a browser tab
# Ctrl+C stops it.
#
# A fresh clone has no node/ yet: the script then runs this module's Maven build up to generate-resources, where
# frontend-maven-plugin installs Node and the packages (switching to JDK 25 first when Use-Jdk25.ps1 is on PATH).
# When package-lock.json changed since the last install (a pull, a Renovate merge), it runs npm ci first.
# npm does not put its own Node on PATH for the tools it starts, so node/ goes first on PATH while the server runs;
# PATH is put back when it stops.
#
# Runs on PowerShell 7 and on Windows PowerShell 5.1. Keep this file ASCII: 5.1 reads a BOM-less file as ANSI.
param(
	[switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$onWindows = $env:OS -eq 'Windows_NT'
$nodeDir = Join-Path $PSScriptRoot 'node'
$node = Join-Path $nodeDir $(if ($onWindows) { 'node.exe' } else { 'node' })
$npm = Join-Path $nodeDir $(if ($onWindows) { 'npm.cmd' } else { 'npm' })

# Maven runs from the reactor root (the highest directory above this module that holds a pom.xml) and builds the module
# with its parents: built alone, the enforcer refuses a module whose parents are not in the reactor.
function Install-NodeWithMaven {
	$moduleDir = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
	$rootDir = $moduleDir
	while (Test-Path (Join-Path (Split-Path $rootDir -Parent) 'pom.xml')) {
		$rootDir = Split-Path $rootDir -Parent
	}
	$module = $moduleDir.Substring($rootDir.Length + 1)
	$useJdk = Get-Command 'Use-Jdk25.ps1' -ErrorAction SilentlyContinue
	if ($useJdk) {
		. $useJdk.Path
	}
	Write-Host "No Node in $nodeDir yet - installing it with the Maven build of $module (a minute the first time) ..." -ForegroundColor Cyan
	mvn -q -f (Join-Path $rootDir 'pom.xml') -pl $module -am generate-resources
	if ($LASTEXITCODE -ne 0) {
		throw 'The Maven build failed, see above.'
	}
}

# npm keeps a copy of the lockfile it last installed from in node_modules; an older copy means package-lock.json has
# changed since.
function Test-PackagesStale {
	$installed = Join-Path $PSScriptRoot 'node_modules/.package-lock.json'
	if (-not (Test-Path $installed)) {
		return $true
	}
	# -Force: on macOS and Linux a dot-file counts as hidden, and Get-Item skips hidden files without it.
	return (Get-Item (Join-Path $PSScriptRoot 'package-lock.json')).LastWriteTime -gt (Get-Item -Force $installed).LastWriteTime
}

if (-not (Test-Path $node)) {
	Install-NodeWithMaven
}

$pathBefore = $env:PATH
Push-Location $PSScriptRoot
try {
	$env:PATH = $nodeDir + [System.IO.Path]::PathSeparator + $env:PATH
	if (Test-PackagesStale) {
		Write-Host 'package-lock.json changed since the last install - running npm ci ...' -ForegroundColor Cyan
		& $npm ci
		if ($LASTEXITCODE -ne 0) {
			throw 'npm ci failed, see above.'
		}
	}
	# The '--' is quoted because Windows PowerShell 5.1 drops a bare one, and npm would then read --open as its own.
	if ($NoBrowser) {
		& $npm run dev
	} else {
		& $npm run dev '--' '--open'
	}
	exit $LASTEXITCODE
}
finally {
	$env:PATH = $pathBefore
	Pop-Location
}
