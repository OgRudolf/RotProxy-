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

# 3. Build both supported Minecraft targets
Write-Host ""
Write-Host "[...] Building RotProxy for Minecraft 1.21.10 and 1.21.11 (first run downloads Fabric - may take a few minutes)..." -ForegroundColor Yellow
Write-Host ""

$targets = @(
    @{
        Minecraft = "1.21.10"
        Yarn = "1.21.10+build.3"
        FabricApi = "0.138.4+1.21.10"
        Loader = "0.17.2"
        Dependency = ">=1.21.10 <1.21.11"
    },
    @{
        Minecraft = "1.21.11"
        Yarn = "1.21.11+build.4"
        FabricApi = "0.141.3+1.21.11"
        Loader = "0.19.1"
        Dependency = ">=1.21.11 <1.21.12"
    }
)

& .\gradlew.bat clean
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "=== BUILD FAILED ===" -ForegroundColor Red
    Write-Host "Gradle clean failed before building." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

$builtJars = @()

foreach ($target in $targets) {
    Write-Host "[...] Building Minecraft $($target.Minecraft)..." -ForegroundColor Yellow
    & .\gradlew.bat build `
        "-Pminecraft_version=$($target.Minecraft)" `
        "-Pyarn_mappings=$($target.Yarn)" `
        "-Pfabric_version=$($target.FabricApi)" `
        "-Ploader_version=$($target.Loader)" `
        "-Pminecraft_dependency=$($target.Dependency)"

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "=== BUILD FAILED ===" -ForegroundColor Red
        Write-Host "Minecraft $($target.Minecraft) build failed." -ForegroundColor Yellow
        Write-Host ""
        Read-Host "Press Enter to exit"
        exit 1
    }

    $jar = Get-ChildItem -Path ".\build\libs\" -Filter "rotproxy-*+mc$($target.Minecraft).jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*-sources*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($jar) {
        $builtJars += $jar.FullName
        Write-Host "[OK] $($jar.Name)" -ForegroundColor Green
    }
}

if ($builtJars.Count -gt 0) {
    Write-Host ""
    Write-Host "=== BUILD SUCCESSFUL! ===" -ForegroundColor Green
    Write-Host ""
    foreach ($jarPath in $builtJars) {
        Write-Host "Mod jar: $jarPath" -ForegroundColor Cyan
    }
    Write-Host "Copy the matching version jar to: $env:APPDATA\.minecraft\mods\" -ForegroundColor Cyan
    Write-Host ""
    $open = Read-Host "Open build/libs folder now? (y/n)"
    if ($open -eq "y") { explorer ".\build\libs\" }
} else {
    Write-Host ""
    Write-Host "=== BUILD FAILED ===" -ForegroundColor Red
    Write-Host "No jar files were found after the builds finished." -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Press Enter to exit"
