# UTF-8 BOM: required for Windows PowerShell 5.x (Chinese comments / paths).
# Redeploy: mvn + docker build + (kind load if needed) + rollout restart.
#
# Usage (if "running scripts is disabled", prefix: powershell -ExecutionPolicy Bypass -NoProfile -File)
#   .\scripts\k8s-redeploy.ps1
#   .\scripts\k8s-redeploy.ps1 -DockerDesktop
#   .\scripts\k8s-redeploy.ps1 -SkipMaven -SkipDocker
#   .\scripts\k8s-redeploy.ps1 -Elasticsearch
#
# Note: Do not use [WORD] inside double-quoted Write-Host strings — PS5 parses [ as subexpression.
#
param(
    [switch]$DockerDesktop,
    [switch]$SkipMaven,
    [switch]$SkipDocker,
    [switch]$SkipRestart,
    [switch]$Elasticsearch
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

if (-not $SkipMaven) {
    if ((Test-LocalPortInUse 8080) -or (Test-LocalPortInUse 8081)) {
        Write-Host ""
        Write-Host "BLOCKED: ports 8080 or 8081 in use (stop java gateway/commerce or run scripts\stop-local-bcommerce.ps1)." -ForegroundColor Red
        Write-Host ""
        exit 1
    }
}

if (-not (Test-CommandExists "kubectl")) {
    Write-Host "kubectl not found." -ForegroundColor Red
    exit 1
}

if (-not $SkipMaven) {
    Write-Host "==> mvn package"
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if (-not $SkipDocker) {
    Write-Host "==> docker build (commerce + gateway)"
    docker build -f docker/commerce/Dockerfile -t bcommerce/commerce:local .
    docker build -f docker/gateway/Dockerfile -t bcommerce/gateway:local .
    if ($Elasticsearch) {
        Write-Host "==> docker build elasticsearch"
        docker build -f docker/elasticsearch/Dockerfile -t bcommerce/elasticsearch:local docker/elasticsearch
    }
}

$contextNames = @(kubectl config get-contexts -o name 2>$null)
$hasDockerDesktopCtx = $contextNames -contains "docker-desktop"
$cluster = "bcommerce"
$hasKind = Test-CommandExists "kind"
$kindClusterExists = $false
if ($hasKind) {
    $kindClusterExists = $null -ne (kind get clusters 2>$null | Where-Object { $_ -eq $cluster })
}

if ($DockerDesktop) {
    if ($hasDockerDesktopCtx) {
        kubectl config use-context docker-desktop | Out-Null
        Write-Host "==> kubectl context: docker-desktop" -ForegroundColor Cyan
    } else {
        Write-Host "ERROR: -DockerDesktop was set but kubectl context docker-desktop was not found. Enable K8s in Docker Desktop." -ForegroundColor Red
        exit 1
    }
} elseif ($hasKind -and $kindClusterExists) {
    kubectl config use-context "kind-$cluster" | Out-Null
    Write-Host "==> kind load (commerce + gateway)"
    kind load docker-image bcommerce/commerce:local --name $cluster
    kind load docker-image bcommerce/gateway:local --name $cluster
    if ($Elasticsearch) {
        kind load docker-image bcommerce/elasticsearch:local --name $cluster
    }
} elseif ($hasDockerDesktopCtx) {
    kubectl config use-context docker-desktop | Out-Null
    Write-Host "==> Using Docker Desktop Kubernetes (no kind load). Cluster name $cluster not required." -ForegroundColor Cyan
} else {
    Write-Host "ERROR: No usable cluster: kind not installed or no kind cluster $cluster, and kubectl has no docker-desktop context." -ForegroundColor Red
    Write-Host "Fix: install kind and run scripts\k8s-kind-up.ps1, OR enable Kubernetes in Docker Desktop." -ForegroundColor Yellow
    exit 1
}

if (-not $SkipRestart) {
    $nsExists = $null -ne (kubectl get namespace bcommerce -o name 2>$null)
    if (-not $nsExists) {
        Write-Host "ERROR: namespace bcommerce does not exist. Deploy manifests first:" -ForegroundColor Red
        Write-Host "  kubectl apply -f k8s/" -ForegroundColor Yellow
        Write-Host "Or full first-time setup: powershell -ExecutionPolicy Bypass -File scripts\k8s-kind-up.ps1 (-DockerDesktop if using Docker Desktop K8s)" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "==> kubectl rollout restart (commerce + gateway)"
    kubectl -n bcommerce rollout restart deployment/bcommerce-commerce
    kubectl -n bcommerce rollout restart deployment/bcommerce-gateway
    if ($Elasticsearch) {
        kubectl -n bcommerce rollout restart deployment/elasticsearch
    }
    Write-Host "==> waiting for rollout (commerce may take a while)"
    kubectl -n bcommerce rollout status deployment/bcommerce-commerce --timeout=600s
    kubectl -n bcommerce rollout status deployment/bcommerce-gateway --timeout=300s
}

Write-Host "Done."
