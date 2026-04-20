[CmdletBinding()]
param(
    [ValidateSet('preflight', 'demo', 'e2e')]
    [string]$Flow
)

$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$FrontendRoot = Join-Path $RepoRoot 'frontends\quality-inspection-frontend'
$DemoApiKey = 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ0ei11c2VyIjoie1widXNlclwiOntcInVzZXJfaWRcIjpcImRlbW8tbWFuYWdlclwiLFwidXNlcl9uYW1lXCI6XCJEZW1vIE1hbmFnZXJcIixcInVzZXJfcm9sZVwiOlt7XCJyb2xlX25hbWVcIjpcImhvc3BpdGFsX21hbmFnZXJcIixcImVuYWJsZWRcIjp0cnVlfV0sXCJob3NwaXRhbF9pZFwiOlwiSDAwMVwifX0ifQ.demo'
$BackendReadyUrl = 'http://localhost:8080/api/quality-inspection/review-projects/PROJECT-001/sampling/execute-context'
$FrontendReadyUrl = 'http://localhost:4200'
$ProxyReadyUrl = 'http://localhost:4200/api/quality-inspection/review-projects/PROJECT-001/sampling/execute-context'

function Test-HttpEndpoint {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [hashtable]$Headers,
        [int[]]$ExpectedStatus = @(200),
        [int]$TimeoutSec = 5
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -Headers $Headers -TimeoutSec $TimeoutSec
        return $ExpectedStatus -contains [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return $ExpectedStatus -contains [int]$_.Exception.Response.StatusCode
        }

        return $false
    }
}

function Assert-DemoEnvironmentReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FlowName
    )

    $commandName = switch ($FlowName) {
        'preflight' { 'demo:preflight' }
        'demo' { 'demo:run' }
        'e2e' { 'demo:e2e' }
    }

    $backendReady = Test-HttpEndpoint -Url $BackendReadyUrl -Headers @{ 'api-key' = $DemoApiKey }
    $frontendReady = Test-HttpEndpoint -Url $FrontendReadyUrl
    $proxyReady = Test-HttpEndpoint -Url $ProxyReadyUrl

    if ($backendReady -and $frontendReady -and $proxyReady) {
        return
    }

    Write-Host "[$commandName] required environment is not ready."
    Write-Host "  backendReady=$backendReady"
    Write-Host "  frontendReady=$frontendReady"
    Write-Host "  proxyReady=$proxyReady"
    Write-Host ''
    Write-Host 'demo launcher is the official startup entry.'
    Write-Host 'Run npm run demo:launch first, or use npm run demo:launch:preflight for first-time operators.'
    Write-Host "$commandName only runs Playwright against an already-started live standalone chain."
    throw "$commandName prerequisites not met."
}

$playwrightSpec = switch ($Flow) {
    'preflight' { 'e2e/src/demo-preflight.e2e.spec.ts' }
    'demo' { 'e2e/src/demo-showcase.e2e.spec.ts --headed' }
    'e2e' { 'e2e/src/live-standalone.e2e.spec.ts' }
}

Push-Location $FrontendRoot
try {
    Assert-DemoEnvironmentReady -FlowName $Flow

    & cmd /c "set QI_PLAYWRIGHT_FLOW=$Flow&& npx playwright test $playwrightSpec --workers=1"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
