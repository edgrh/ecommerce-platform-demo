# Smoke test against API gateway (8080) or commerce (8081)
param(
    [string] $BaseUrl = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api"

Write-Host "GET /actuator/health (commerce direct 8081 if needed)"
try { Invoke-RestMethod "http://127.0.0.1:8081/actuator/health" } catch { Write-Warning $_ }

Write-Host "`nPOST /api/auth/login merchant"
$login = Invoke-RestMethod -Method Post -Uri "$api/auth/login" -ContentType "application/json" `
    -Body '{"username":"merchant","password":"demo123"}'
Write-Host "role=$($login.role)"

Write-Host "`nGET /api/c/products"
Invoke-RestMethod -Uri "$api/c/products" | ConvertTo-Json -Depth 3

Write-Host "`nGET /api/c/seckill/activities"
Invoke-RestMethod -Uri "$api/c/seckill/activities" | ConvertTo-Json -Depth 4

Write-Host "`nPOST /api/auth/login buyer"
$buyer = Invoke-RestMethod -Method Post -Uri "$api/auth/login" -ContentType "application/json" `
    -Body '{"username":"buyer","password":"demo123"}'
$h = @{ Authorization = "Bearer $($buyer.token)" }

Write-Host "`nGET /api/c/orders (buyer)"
try {
    Invoke-RestMethod -Uri "$api/c/orders" -Headers $h | ConvertTo-Json -Depth 5
} catch {
    Write-Warning $_
}

Write-Host "`nDone. Seckill POST can be added manually with activityId from activities list."
