# UTF-8 BOM: required for Windows PowerShell 5.x to parse Chinese comments reliably.
# One-shot: mvn package + docker build + (kind OR Docker Desktop K8s) + kubectl apply.
# Requires: Docker, kubectl, mvn. Either kind (on PATH) OR Docker Desktop Kubernetes.
#
# Usage:
#   .\scripts\k8s-kind-up.ps1
#   .\scripts\k8s-kind-up.ps1 -DockerDesktop
#
param(
    [switch]$DockerDesktop
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Test-LocalPortInUse([int] $Port) {
    try {
        $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        return $null -ne $c
    } catch {
        return $false
    }
}

function Test-CommandExists([string] $Name) {
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

if ((Test-LocalPortInUse 8080) -or (Test-LocalPortInUse 8081)) {
    Write-Host ""
    Write-Host "[BLOCKED] Ports 8080 or 8081 are in use (often java -jar gateway/commerce)." -ForegroundColor Red
    Write-Host "Stop those processes so Maven can repackage jars, then run this script again." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

if (-not (Test-CommandExists "kubectl")) {
    Write-Host "kubectl not found. Install kubectl and add it to PATH." -ForegroundColor Red
    exit 1
}

Write-Host "==> mvn package"
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven failed. If repackage cannot rename jar, stop IDE/java holding target/*.jar." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "==> docker build"
docker build -f docker/elasticsearch/Dockerfile -t bcommerce/elasticsearch:local docker/elasticsearch
docker build -f docker/commerce/Dockerfile -t bcommerce/commerce:local .
docker build -f docker/gateway/Dockerfile -t bcommerce/gateway:local .

if ($DockerDesktop) {
    Write-Host "==> Docker Desktop Kubernetes (skip kind / no kind load)" -ForegroundColor Cyan
    $contexts = kubectl config get-contexts -o name 2>$null
    $dd = $contexts | Where-Object { $_ -eq "docker-desktop" }
    if ($dd) {
        kubectl config use-context docker-desktop
    } else {
        Write-Host ""
        Write-Host "[ERROR] kubectl context ""docker-desktop"" not found." -ForegroundColor Red
        Write-Host "Enable Kubernetes in Docker Desktop (Settings -> Kubernetes), wait until ready." -ForegroundColor Yellow
        Write-Host "Then run: kubectl config get-contexts" -ForegroundColor Yellow
        Write-Host ""
        exit 1
    }
} else {
    if (-not (Test-CommandExists "kind")) {
        Write-Host ""
        Write-Host "[ERROR] ""kind"" not installed or not on PATH." -ForegroundColor Red
        Write-Host "Install: https://kind.sigs.k8s.io/docs/user/quick-start/#installation" -ForegroundColor Gray
        Write-Host "Or use Docker Desktop K8s:" -ForegroundColor Yellow
        Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\k8s-kind-up.ps1 -DockerDesktop" -ForegroundColor Green
        Write-Host ""
        exit 1
    }

    $cluster = "bcommerce"
    $exists = kind get clusters 2>$null | Where-Object { $_ -eq $cluster }
    if (-not $exists) {
        Write-Host "==> kind create cluster $cluster"
        kind create cluster --name $cluster
    }
    kubectl config use-context "kind-$cluster"

    Write-Host "==> kind load docker-image"
    kind load docker-image bcommerce/elasticsearch:local --name $cluster
    kind load docker-image bcommerce/commerce:local --name $cluster
    kind load docker-image bcommerce/gateway:local --name $cluster
}

Write-Host "==> kubectl apply"
kubectl apply -f "$Root/k8s/"

# 压测 Job 不再随默认部署自动创建，避免 16G 笔记本一键启动时误触发高压任务。
# 需要压测时单独执行：
#   kubectl -n bcommerce apply -f "$Root/perf/k6-job.yaml"

Write-Host "==> Wait for commerce rollout (MySQL/ES cold start may take minutes)"
kubectl -n bcommerce rollout status deployment/bcommerce-commerce --timeout=600s

Write-Host "Done. Example: kubectl -n bcommerce port-forward svc/bcommerce-gateway 8080:8080"
Write-Host "Or NodePort: http://127.0.0.1:30080 (depends on cluster)"
