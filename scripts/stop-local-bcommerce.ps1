# Stop processes listening on 8080 (gateway) / 8081 (commerce) so Maven can repackage jars.
# Run as Administrator if Stop-Process fails.
$ErrorActionPreference = "SilentlyContinue"

function Stop-ListenersOnPort([int] $Port) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) {
        Write-Host "Port $Port : no listener."
        return
    }
    $procIds = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $procIds) {
        try {
            $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
            $name = if ($p) { $p.ProcessName } else { "?" }
            Write-Host "Stopping PID=$procId ($name) on port $Port"
            Stop-Process -Id $procId -Force -ErrorAction Stop
        } catch {
            Write-Warning "Could not stop PID=$procId : $_ (try Admin PowerShell)"
        }
    }
}

Write-Host "=== Release ports 8080 / 8081 ===" -ForegroundColor Cyan
Stop-ListenersOnPort 8080
Stop-ListenersOnPort 8081
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "If Maven still cannot rename jar to .original:" -ForegroundColor Yellow
Write-Host "  1) Stop Spring Boot Run/Debug in Cursor/IDE" -ForegroundColor Yellow
Write-Host "  2) End leftover java.exe in Task Manager" -ForegroundColor Yellow
Write-Host "  3) Temporarily exclude project folder from AV real-time scan" -ForegroundColor Yellow
