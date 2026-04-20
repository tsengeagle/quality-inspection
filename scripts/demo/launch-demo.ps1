[CmdletBinding()]
param(
    [ValidateSet('Start', 'Stop', 'Status', 'Restart')]
    [string]$Action = 'Start',
    [switch]$RunPreflight
)

$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$FrontendRoot = Join-Path $RepoRoot 'frontends\quality-inspection-frontend'
$BackendRoot = Join-Path $RepoRoot 'services\quality-inspection-service'
$RuntimeRoot = Join-Path $RepoRoot '.demo-runtime'
$BackendPidFile = Join-Path $RuntimeRoot 'backend.pid'
$FrontendPidFile = Join-Path $RuntimeRoot 'frontend.pid'
$BackendOutLog = Join-Path $RuntimeRoot 'backend.out.log'
$BackendErrLog = Join-Path $RuntimeRoot 'backend.err.log'
$FrontendOutLog = Join-Path $RuntimeRoot 'frontend.out.log'
$FrontendErrLog = Join-Path $RuntimeRoot 'frontend.err.log'
$DemoApiKey = 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ0ei11c2VyIjoie1widXNlclwiOntcInVzZXJfaWRcIjpcImRlbW8tbWFuYWdlclwiLFwidXNlcl9uYW1lXCI6XCJEZW1vIE1hbmFnZXJcIixcInVzZXJfcm9sZVwiOlt7XCJyb2xlX25hbWVcIjpcImhvc3BpdGFsX21hbmFnZXJcIixcImVuYWJsZWRcIjp0cnVlfV0sXCJob3NwaXRhbF9pZFwiOlwiSDAwMVwifX0ifQ.demo'
$BackendReadyUrl = 'http://localhost:8080/api/quality-inspection/review-projects/PROJECT-001/sampling/execute-context'
$FrontendReadyUrl = 'http://localhost:4200'
$ProxyReadyUrl = 'http://localhost:4200/api/quality-inspection/review-projects/PROJECT-001/sampling/execute-context'

function Initialize-DemoRuntimeRoot {
    if (-not (Test-Path $RuntimeRoot)) {
        New-Item -ItemType Directory -Path $RuntimeRoot | Out-Null
    }
}

function Test-RequiredPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    if (-not (Test-Path $Path)) {
        throw "缺少 $Description：$Path"
    }
}

function Get-TrackedPid {
    param([string]$PidFile)

    if (-not (Test-Path $PidFile)) {
        return $null
    }

    $rawValue = (Get-Content -Path $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if (-not $rawValue) {
        return $null
    }

    return [int]$rawValue
}

function Remove-TrackedPid {
    param([string]$PidFile)

    if (Test-Path $PidFile) {
        Remove-Item -Path $PidFile -Force -ErrorAction SilentlyContinue
    }
}

function Stop-TrackedProcess {
    param(
        [string]$PidFile,
        [string]$Name
    )

    $trackedPid = Get-TrackedPid -PidFile $PidFile
    if (-not $trackedPid) {
        Write-Host "[$Name] 無 tracked process。"
        return
    }

    $process = Get-Process -Id $trackedPid -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $trackedPid -Force -ErrorAction Stop
        Write-Host "[$Name] 已停止 PID=$trackedPid"
    } else {
        Write-Host "[$Name] PID=$trackedPid 已不存在，清除 pid file。"
    }

    Remove-TrackedPid -PidFile $PidFile
}

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

function Wait-UntilReady {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Probe,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$LogFile,
        [int]$TimeoutSec = 120,
        [int]$PollIntervalSec = 2,
        [System.Diagnostics.Process]$Process
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if ($Process -and $Process.HasExited) {
            Write-Host "[$Name] 啟動程序已提前結束，以下為最後 log："
            if (Test-Path $LogFile) {
                Get-Content -Path $LogFile -Tail 40
            }
            throw "$Name 啟動失敗。"
        }

        if (& $Probe) {
            Write-Host "[$Name] ready"
            return
        }

        Start-Sleep -Seconds $PollIntervalSec
    }

    Write-Host "[$Name] 超時，以下為最後 log："
    if (Test-Path $LogFile) {
        Get-Content -Path $LogFile -Tail 40
    }
    throw "$Name 在 $TimeoutSec 秒內未 ready。"
}

function Start-ManagedProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$WorkingRoot,
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [Parameter(Mandatory = $true)]
        [string]$PidFile,
        [Parameter(Mandatory = $true)]
        [string]$StdOutLog,
        [Parameter(Mandatory = $true)]
        [string]$StdErrLog,
        [Parameter(Mandatory = $true)]
        [scriptblock]$ReadyProbe,
        [int]$TimeoutSec = 120
    )

    $process = Start-Process -FilePath 'pwsh' `
        -ArgumentList @('-NoLogo', '-NoProfile', '-Command', "Set-Location '$WorkingRoot'; $Command") `
        -RedirectStandardOutput $StdOutLog `
        -RedirectStandardError $StdErrLog `
        -PassThru

    $process.Id | Set-Content -Path $PidFile
    Write-Host "[$Name] 已啟動 PID=$($process.Id)"

    try {
        Wait-UntilReady -Probe $ReadyProbe -Name $Name -LogFile $StdOutLog -TimeoutSec $TimeoutSec -Process $process
    } catch {
        Stop-TrackedProcess -PidFile $PidFile -Name $Name
        throw
    }
}

function Start-DemoEnvironment {
    Initialize-DemoRuntimeRoot

    Test-RequiredPath -Path (Join-Path $BackendRoot 'build.gradle') -Description 'backend build.gradle'
    Test-RequiredPath -Path (Join-Path $BackendRoot 'settings.gradle') -Description 'backend settings.gradle'
    Test-RequiredPath -Path (Join-Path $BackendRoot 'gradlew.bat') -Description 'backend gradlew.bat'
    Test-RequiredPath -Path (Join-Path $BackendRoot 'src\main\resources\application.yml') -Description 'backend application.yml'
    Test-RequiredPath -Path (Join-Path $FrontendRoot 'package.json') -Description 'frontend package.json'
    Test-RequiredPath -Path (Join-Path $FrontendRoot 'playwright.config.ts') -Description 'frontend playwright config'

    if (Test-HttpEndpoint -Url $BackendReadyUrl -Headers @{ 'api-key' = $DemoApiKey }) {
        Write-Host '[backend] 已有可用 instance，直接重用。'
    } else {
        Start-ManagedProcess `
            -Name 'backend' `
            -WorkingRoot $BackendRoot `
            -Command '.\gradlew.bat bootRun' `
            -PidFile $BackendPidFile `
            -StdOutLog $BackendOutLog `
            -StdErrLog $BackendErrLog `
            -ReadyProbe { Test-HttpEndpoint -Url $BackendReadyUrl -Headers @{ 'api-key' = $DemoApiKey } } `
            -TimeoutSec 180
    }

    if (Test-HttpEndpoint -Url $ProxyReadyUrl) {
        Write-Host '[frontend] 已有可用 standalone + proxy，直接重用。'
    } else {
        Start-ManagedProcess `
            -Name 'frontend' `
            -WorkingRoot $FrontendRoot `
            -Command 'npm run start:standalone' `
            -PidFile $FrontendPidFile `
            -StdOutLog $FrontendOutLog `
            -StdErrLog $FrontendErrLog `
            -ReadyProbe { (Test-HttpEndpoint -Url $FrontendReadyUrl) -and (Test-HttpEndpoint -Url $ProxyReadyUrl) } `
            -TimeoutSec 180
    }

    Write-Host ''
    Write-Host 'Demo launcher ready:'
    Write-Host '  UI-006  http://localhost:4200/sampling/PROJECT-001'
    Write-Host '  UI-007  http://localhost:4200/sampling/batches'
    Write-Host '  UI-011  http://localhost:4200/cases/CASE-011/assign'
    Write-Host ''
    Write-Host 'Logs:'
    Write-Host "  backend  $BackendOutLog"
    Write-Host "  frontend $FrontendOutLog"

    if ($RunPreflight) {
        Write-Host ''
        Write-Host '[preflight] 執行 Playwright preflight...'
        Push-Location $FrontendRoot
        try {
            & cmd /c 'set QI_PLAYWRIGHT_FLOW=preflight&& npx playwright test e2e/src/demo-preflight.e2e.spec.ts --workers=1'
        } finally {
            Pop-Location
        }
    }
}

function Get-DemoStatus {
    $backendReady = Test-HttpEndpoint -Url $BackendReadyUrl -Headers @{ 'api-key' = $DemoApiKey }
    $frontendReady = Test-HttpEndpoint -Url $FrontendReadyUrl
    $proxyReady = Test-HttpEndpoint -Url $ProxyReadyUrl

    [pscustomobject]@{
        backendReady = $backendReady
        frontendReady = $frontendReady
        proxyReady = $proxyReady
        trackedBackendPid = Get-TrackedPid -PidFile $BackendPidFile
        trackedFrontendPid = Get-TrackedPid -PidFile $FrontendPidFile
        backendLog = $BackendOutLog
        frontendLog = $FrontendOutLog
    } | Format-List
}

switch ($Action) {
    'Start' {
        Start-DemoEnvironment
    }
    'Stop' {
        Stop-TrackedProcess -PidFile $FrontendPidFile -Name 'frontend'
        Stop-TrackedProcess -PidFile $BackendPidFile -Name 'backend'
    }
    'Status' {
        Get-DemoStatus
    }
    'Restart' {
        Stop-TrackedProcess -PidFile $FrontendPidFile -Name 'frontend'
        Stop-TrackedProcess -PidFile $BackendPidFile -Name 'backend'
        Start-DemoEnvironment
    }
}
