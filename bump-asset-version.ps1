param(
    [string]$Version = (Get-Date -Format 'yyyyMMdd-HHmmss')
)

$files = @(
    "main.html",
    "index.html",
    "faq.html",
    "privacy-policy.html",
    "terms-of-service.html"
)

foreach ($file in $files) {
    if (-not (Test-Path -LiteralPath $file)) {
        continue
    }

    $content = Get-Content -LiteralPath $file -Raw
    $content = [regex]::Replace($content, 'styles\.css(\?v=[^"]+)?', "styles.css?v=$Version")
    $content = [regex]::Replace($content, 'script\.js(\?v=[^"]+)?', "script.js?v=$Version")
    Set-Content -LiteralPath $file -Value $content -Encoding utf8
    Write-Output "Updated $file -> $Version"
}
