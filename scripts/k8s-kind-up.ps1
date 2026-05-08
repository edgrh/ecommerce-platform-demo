# 一键：Maven 打包 + 构建镜像 + kind 集群 + load 镜像 + kubectl apply
# 需已安装：Docker、kubectl、kind、mvn
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
if ((Test-LocalPortInUse 8080) -or (Test-LocalPortInUse 8081)) {
    Write-Host ""
    Write-Host "【阻塞】检测到本机 8080 或 8081 正在监听（多为未关闭的 java -jar 网关/业务）。" -ForegroundColor Red
    Write-Host "Maven 无法覆盖 target 里的 jar，请先关闭这两个进程，再重新运行本脚本。" -ForegroundColor Yellow
    Write-Host "（在运行 java -jar 的窗口按 Ctrl+C，或在任务管理器中结束对应 java.exe）" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host "==> mvn package"
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven 失败。若仍提示 jar 无法 rename，请确认已关闭所有占用 target 下 jar 的进程（含 IDE 内嵌运行）。" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "==> docker build"
docker build -f docker/elasticsearch/Dockerfile -t bcommerce/elasticsearch:local docker/elasticsearch
docker build -f docker/commerce/Dockerfile -t bcommerce/commerce:local .
docker build -f docker/gateway/Dockerfile -t bcommerce/gateway:local .

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

Write-Host "==> kubectl apply"
kubectl apply -f "$Root/k8s/"

Write-Host "==> 等待 commerce（可能需数分钟，MySQL/ES 冷启动）"
kubectl -n bcommerce rollout status deployment/bcommerce-commerce --timeout=600s

Write-Host "完成。访问示例: kubectl -n bcommerce port-forward svc/bcommerce-gateway 8080:8080"
Write-Host "或 NodePort: http://127.0.0.1:30080 (视集群而定)"
