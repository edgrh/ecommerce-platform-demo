# UTF-8 BOM for Windows PowerShell 5.x
# 一键启动（尽量不需要手动开多个 PowerShell 窗口）：
# 1) K8s 一键部署/更新（Docker Desktop Kubernetes）
# 2) 后台启动 gateway 的 port-forward (8080->8080)
# 3) 可选：启动前端 npm dev（同一窗口前台运行；关闭窗口即可）
#
# Usage:
#   powershell -ExecutionPolicy Bypass -NoProfile -File .\scripts\one-click.ps1
#   powershell -ExecutionPolicy Bypass -NoProfile -File .\scripts\one-click.ps1 -SkipFrontend
#
param(
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Stop-ProcessByLocalPort([int] $Port) {
    try {
        $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($c -and $c.OwningProcess) {
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    } catch {
        # ignore
    }
}

Write-Host "==> 0) Free local ports 8080/8081 (cleanup old port-forward/java)" -ForegroundColor Cyan
Stop-ProcessByLocalPort 8080
Stop-ProcessByLocalPort 8081

Write-Host "==> 1) Deploy/Update Kubernetes stack (Docker Desktop)" -ForegroundColor Cyan
powershell -ExecutionPolicy Bypass -NoProfile -File "$Root\scripts\k8s-kind-up.ps1" -DockerDesktop
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> 2) Start port-forward in background: localhost:8080 -> svc/bcommerce-gateway:8080" -ForegroundColor Cyan
# 若 8080 被占用（上次 port-forward 残留），先清掉
Stop-ProcessByLocalPort 8080

# 后台启动一个独立 powershell 进程做 port-forward，当前窗口继续往下走
$pfCmd = "kubectl -n bcommerce port-forward svc/bcommerce-gateway 8080:8080"
Start-Process -WindowStyle Hidden -FilePath "powershell" -ArgumentList @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-Command", $pfCmd
) | Out-Null

Start-Sleep -Seconds 1
Write-Host "Gateway is available at: http://127.0.0.1:8080" -ForegroundColor Green

if ($SkipFrontend) {
    Write-Host "==> Skip frontend. You can start it later: cd frontend; npm run dev" -ForegroundColor Yellow
    exit 0
}

Write-Host "==> 3) Start frontend dev server (Ctrl+C to stop)" -ForegroundColor Cyan
Set-Location "$Root\frontend"
if (Test-Path "$Root\frontend\package-lock.json") {
    # 如果依赖已安装，npm 会很快；未安装也能自动提示
    npm run dev
} else {
    npm run dev
}

