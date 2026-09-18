# 从远程 SPA 前端 JS 中提取 API 路径
# 用法: powershell -File scripts/extract-remote-apis.ps1

$Base = "http://101.37.255.79:8010"
$TempDir = Join-Path $env:TEMP "xhhf-api-scan"
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null

Write-Host "Fetching homepage..."
$html = (Invoke-WebRequest -Uri $Base -TimeoutSec 30 -UseBasicParsing).Content

$jsFiles = [regex]::Matches($html, '(?:src|href)="(/assets/[^"]+\.js)"') |
    ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique

if (-not $jsFiles.Count) {
    Write-Error "No JS assets found in homepage"
    exit 1
}

Write-Host "Found $($jsFiles.Count) JS file(s)"
$allText = ""
foreach ($js in $jsFiles) {
    $url = "$Base$js"
    $out = Join-Path $TempDir ($js -replace '/', '_')
    Write-Host "Downloading $url"
    Invoke-WebRequest -Uri $url -TimeoutSec 60 -UseBasicParsing -OutFile $out
    $allText += (Get-Content $out -Raw -Encoding UTF8) + "`n"
}

# 常见 API 前缀与路径模式
$patterns = @(
    '["''](/(?:prod-api|dev-api|api)/[^''"\s`]+)["'']',
    '["''](/(?:system|monitor|tool|common|auth|login|logout|getInfo|captchaImage)[^''"\s?]+)["'']',
    'url\s*:\s*["'']([^''"]+)["'']',
    'baseURL\s*:\s*["'']([^''"]+)["'']'
)

$found = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach ($pat in $patterns) {
    foreach ($m in [regex]::Matches($allText, $pat)) {
        $path = $m.Groups[1].Value.Trim()
        if ($path -match '\.(js|css|png|jpg|svg|woff|ico)(\?|$)') { continue }
        if ($path.Length -lt 2 -or $path.Length -gt 200) { continue }
        [void]$found.Add($path)
    }
}

# 也尝试 swagger 文档
$docPaths = @(
    '/doc.html', '/swagger-ui/index.html', '/v3/api-docs',
    '/prod-api/v3/api-docs', '/dev-api/v3/api-docs'
)
Write-Host "`nProbing API docs..."
foreach ($p in $docPaths) {
    try {
        $r = Invoke-WebRequest -Uri "$Base$p" -TimeoutSec 10 -UseBasicParsing
        Write-Host "  [OK] $p ($($r.StatusCode))"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code) { Write-Host "  [$code] $p" } else { Write-Host "  [FAIL] $p" }
    }
}

Write-Host "`n========== API paths from frontend JS ($($found.Count)) =========="
$found | Sort-Object | ForEach-Object { Write-Host $_ }

$report = Join-Path $TempDir "api-paths.txt"
$found | Sort-Object | Set-Content $report -Encoding UTF8
Write-Host "`nSaved to: $report"
