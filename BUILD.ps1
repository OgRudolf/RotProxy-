# RotProxy - Setup & Build Script
# Run from inside F:\RotProxy\rotproxy\

Write-Host "=== RotProxy Builder ===" -ForegroundColor Red
Write-Host ""

# 1. Find Java
$javaExe = $null
$commonPaths = @(
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Java",
    "C:\Program Files\Microsoft",
    "C:\Program Files\OpenJDK"
)
foreach ($base in $commonPaths) {
    if (Test-Path $base) {
        $dirs = Get-ChildItem $base -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "21" }
        foreach ($d in $dirs) {
            $candidate = "$($d.FullName)\bin\java.exe"
            if (Test-Path $candidate) {
                $javaExe = $candidate
                break
            }
        }
        if ($javaExe) { break }
    }
}
if (-not $javaExe) {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { $javaExe = $cmd.Source }
}
if (-not $javaExe) {
    Write-Host "[ERROR] Java 21 not found!" -ForegroundColor Red
    Write-Host "Install from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}
$env:JAVA_HOME = Split-Path (Split-Path $javaExe)
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "[OK] Java: $javaExe" -ForegroundColor Green

# 2. Download gradle-wrapper.jar if missing
$wrapperJar = ".\gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host "[...] Downloading gradle-wrapper.jar..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path ".\gradle\wrapper" | Out-Null
    $urls = @(
        "https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar",
        "https://github.com/gradle/gradle/raw/v8.8.0/gradle/wrapper/gradle-wrapper.jar"
    )
    $downloaded = $false
    foreach ($url in $urls) {
        try {
            Invoke-WebRequest -Uri $url -OutFile $wrapperJar -UseBasicParsing
            $downloaded = $true
            Write-Host "[OK] Downloaded gradle-wrapper.jar" -ForegroundColor Green
            break
        } catch {
            Write-Host "    Trying next URL..." -ForegroundColor Gray
        }
    }
    if (-not $downloaded) {
        Write-Host "[ERROR] Could not download gradle-wrapper.jar" -ForegroundColor Red
        Write-Host "Check your internet connection and try again." -ForegroundColor Yellow
        Read-Host "Press Enter to exit"
        exit 1
    }
} else {
    Write-Host "[OK] gradle-wrapper.jar present" -ForegroundColor Green
}

# 3. Build
Write-Host ""
Write-Host "[...] Building RotProxy (first run downloads Fabric - may take a few minutes)..." -ForegroundColor Yellow
Write-Host ""

& .\gradlew.bat build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=== BUILD SUCCESSFUL! ===" -ForegroundColor Green
    $jars = Get-ChildItem -Path ".\build\libs\" -Filter "rotproxy-*.jar" -ErrorAction SilentlyContinue
    $jar = $jars | Where-Object { $_.Name -notlike "*-sources*" } | Select-Object -First 1
    if ($jar) {
        Write-Host ""
        Write-Host "Mod jar: $($jar.FullName)" -ForegroundColor Cyan
        Write-Host "Copy it to: $env:APPDATA\.minecraft\mods\" -ForegroundColor Cyan
        Write-Host ""
        $open = Read-Host "Open build/libs folder now? (y/n)"
        if ($open -eq "y") { explorer ".\build\libs\" }
    }
} else {
    Write-Host ""
    Write-Host "=== BUILD FAILED ===" -ForegroundColor Red
    Write-Host "Common fixes:" -ForegroundColor Yellow
    Write-Host "  - Check internet connection" -ForegroundColor White
    Write-Host "  - Run PowerShell as Administrator" -ForegroundColor White
}

Write-Host ""
Read-Host "Press Enter to exit"
