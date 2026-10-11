#!/usr/bin/env pwsh
# A small MCP client for the example server: the handshake, the tool list and the two tools, each request and reply
# printed as one line. Start the server first (./Start-Example.ps1 mcp) and take the token from its log.
#   ./druvu-lib-web-examples/druvu-lib-web-example-mcp/Invoke-McpExample.ps1 -Token <token>
#   ./druvu-lib-web-examples/druvu-lib-web-example-mcp/Invoke-McpExample.ps1 -Token <token> -Tool time_now -Arguments @{ zone = 'Europe/Zurich' }
#   From cmd or bash, where no hashtable literal exists, the arguments go as JSON text:
#   pwsh -File Invoke-McpExample.ps1 -Token <token> -Tool time_now -Arguments '{"zone":"Europe/Zurich"}'
#
# Runs on PowerShell 7 and on Windows PowerShell 5.1. Keep this file ASCII: 5.1 reads a BOM-less file as ANSI.
param(
	[Parameter(Mandatory = $true)]
	[string]$Token,
	[string]$Url = 'http://localhost:8082/mcp',
	[string]$Tool,
	[object]$Arguments = @{}
)

$ErrorActionPreference = 'Stop'
$headers = @{ Authorization = "Bearer $Token"; 'MCP-Protocol-Version' = '2025-06-18' }
$script:id = 0

# One JSON-RPC request: printed, sent, the reply printed and returned as an object.
function Send-Rpc([string]$Method, $Params) {
	$script:id++
	$message = [ordered]@{ jsonrpc = '2.0'; id = $script:id; method = $Method }
	if ($null -ne $Params) { $message.params = $Params }
	$body = $message | ConvertTo-Json -Depth 10 -Compress
	Write-Host "> $body" -ForegroundColor Cyan
	try {
		$reply = Invoke-RestMethod -Uri $Url -Method Post -Headers $headers -ContentType 'application/json' -Body $body
	}
	catch {
		Write-Host "< HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
		if ($_.Exception.Response.StatusCode.value__ -eq 401) {
			Write-Host '  Not the token this server printed: it makes a new one at every boot.' -ForegroundColor Red
		}
		exit 1
	}
	Write-Host "< $($reply | ConvertTo-Json -Depth 10 -Compress)" -ForegroundColor Green
	return $reply
}

# A notification has no id and gets no body back, only the status.
function Send-Notification([string]$Method) {
	$body = @{ jsonrpc = '2.0'; method = $Method } | ConvertTo-Json -Compress
	Write-Host "> $body" -ForegroundColor Cyan
	$response = Invoke-WebRequest -Uri $Url -Method Post -Headers $headers -ContentType 'application/json' -Body $body -UseBasicParsing
	Write-Host "< HTTP $($response.StatusCode)" -ForegroundColor Green
}

if ($Tool) {
	if ($Arguments -is [string]) { $Arguments = $Arguments | ConvertFrom-Json }
	Send-Rpc 'tools/call' @{ name = $Tool; arguments = $Arguments } | Out-Null
	exit 0
}

Send-Rpc 'initialize' ([ordered]@{ protocolVersion = '2025-06-18'; capabilities = @{}; clientInfo = @{ name = 'Invoke-McpExample'; version = '1' } }) | Out-Null
Send-Notification 'notifications/initialized'
Send-Rpc 'tools/list' $null | Out-Null
Send-Rpc 'tools/call' @{ name = 'echo'; arguments = @{ text = 'hello from PowerShell' } } | Out-Null
Send-Rpc 'tools/call' @{ name = 'time_now'; arguments = @{ zone = 'Europe/Zurich' } } | Out-Null
