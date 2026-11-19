# Builds a signed, installable SecPocket APK without Gradle.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\build.ps1
#
# The first run creates keystore\release.keystore. Keep that file if you want
# future builds to upgrade the installed app instead of requiring a reinstall.

param(
    [switch]$SkipVerify
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$app = Join-Path $root "app\src\main"
$res = Join-Path $app "res"
$assets = Join-Path $app "assets"
$manifest = Join-Path $app "AndroidManifest.xml"
$javaSource = Join-Path $app "java"

$sdkCandidates = @(@(
    (Join-Path $env:LOCALAPPDATA "SecPocket\sdk"),
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "build-tools\35.0.0\aapt2.exe")) })

if ($sdkCandidates.Count -eq 0) {
    throw "Android SDK with build-tools 35.0.0 not found. Set ANDROID_SDK_ROOT or install the portable SDK under %LOCALAPPDATA%\SecPocket\sdk."
}
$sdk = $sdkCandidates[0]
$buildTools = Join-Path $sdk "build-tools\35.0.0"
$androidJar = Join-Path $sdk "platforms\android-35\android.jar"
if (-not (Test-Path $androidJar)) {
    throw "android.jar for platform 35 not found at $androidJar"
}

$portableJdk = Join-Path $env:LOCALAPPDATA "SecPocket\toolchain\jdk-21.0.13+9"
if (Test-Path (Join-Path $portableJdk "bin\javac.exe")) {
    $jdkHome = $portableJdk
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
    $jdkHome = $env:JAVA_HOME
} else {
    throw "A JDK is required. Set JAVA_HOME or install the portable JDK under %LOCALAPPDATA%\SecPocket\toolchain."
}

$env:JAVA_HOME = $jdkHome
$env:PATH = (Join-Path $jdkHome "bin") + ";" + $env:PATH

$javac = Join-Path $jdkHome "bin\javac.exe"
$jar = Join-Path $jdkHome "bin\jar.exe"
$keytool = Join-Path $jdkHome "bin\keytool.exe"
$aapt2 = Join-Path $buildTools "aapt2.exe"
$d8 = Join-Path $buildTools "d8.bat"
$zipalign = Join-Path $buildTools "zipalign.exe"
$apksigner = Join-Path $buildTools "apksigner.bat"

$build = Join-Path $root "build"
$gen = Join-Path $build "gen"
$classes = Join-Path $build "classes"
$dex = Join-Path $build "dex"
$dist = Join-Path $root "dist"
$keystoreDir = Join-Path $root "keystore"
$keystore = Join-Path $keystoreDir "release.keystore"
$unsigned = Join-Path $build "app-unsigned.apk"
$aligned = Join-Path $build "app-aligned.apk"
$final = Join-Path $dist "SecPocket-1.0.apk"
$versionName = "1.0"
$versionCode = "1"

Write-Host ""
Write-Host "SecPocket build" -ForegroundColor Cyan
Write-Host "  SDK        $sdk"
Write-Host "  JDK        $jdkHome"
Write-Host ""

if (Test-Path $build) {
    Remove-Item -Recurse -Force $build
}
New-Item -ItemType Directory -Force -Path $build, $gen, $classes, $dex, $dist, $keystoreDir | Out-Null

# The signing password is never stored in this script. It comes from the
# SECPOCKET_STOREPASS environment variable or from the gitignored
# keystore\keystore.properties file. A first run with no keystore generates a
# random password and writes it to that file.
$keystoreProps = Join-Path $keystoreDir "keystore.properties"
$storePass = $env:SECPOCKET_STOREPASS
if (-not $storePass -and (Test-Path $keystoreProps)) {
    $propLine = Get-Content $keystoreProps | Where-Object { $_ -match '^\s*storePassword\s*=' } | Select-Object -First 1
    if ($propLine) {
        $storePass = ($propLine -split '=', 2)[1].Trim()
    }
}
if (-not $storePass -and -not (Test-Path $keystore)) {
    $randomBytes = New-Object byte[] 24
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($randomBytes)
    $storePass = [Convert]::ToBase64String($randomBytes)
    Set-Content -Path $keystoreProps -Value "storePassword=$storePass" -Encoding ASCII
    Write-Host "==> Generated a new keystore password in keystore\keystore.properties" -ForegroundColor Yellow
}
if (-not $storePass) {
    throw "Release keystore password not found. Set SECPOCKET_STOREPASS or add storePassword=... to keystore\keystore.properties."
}

Write-Host "==> Compiling resources" -ForegroundColor Cyan
& $aapt2 compile --dir $res -o (Join-Path $build "res.zip")
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed ($LASTEXITCODE)" }

Write-Host "==> Linking resources, manifest and assets" -ForegroundColor Cyan
& $aapt2 link `
    -o $unsigned `
    -I $androidJar `
    --manifest $manifest `
    -R (Join-Path $build "res.zip") `
    -A $assets `
    --java $gen `
    --min-sdk-version 26 `
    --target-sdk-version 34 `
    --version-code $versionCode `
    --version-name $versionName `
    --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed ($LASTEXITCODE)" }

Write-Host "==> Compiling Java sources" -ForegroundColor Cyan
$javaSources = @(Get-ChildItem $javaSource -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$rSources = @(Get-ChildItem $gen -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac --release 8 -encoding UTF-8 -classpath $androidJar -d $classes $javaSources $rSources
if ($LASTEXITCODE -ne 0) { throw "javac failed ($LASTEXITCODE)" }

Write-Host "==> Building classes.dex" -ForegroundColor Cyan
$classesJar = Join-Path $build "classes.jar"
& $jar --create --file $classesJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed ($LASTEXITCODE)" }
& $d8 --release --min-api 26 --lib $androidJar --output $dex $classesJar
if ($LASTEXITCODE -ne 0) { throw "d8 failed ($LASTEXITCODE)" }
$dexFile = Join-Path $dex "classes.dex"
if (-not (Test-Path $dexFile)) { throw "d8 did not produce classes.dex" }

Write-Host "==> Adding classes.dex to the APK" -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($unsigned, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $existingDex = $zip.GetEntry("classes.dex")
    if ($null -ne $existingDex) { $existingDex.Delete() }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $zip, $dexFile, "classes.dex",
        [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
} finally {
    $zip.Dispose()
}

Write-Host "==> Aligning the APK" -ForegroundColor Cyan
& $zipalign -f -p 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed ($LASTEXITCODE)" }

if (-not (Test-Path $keystore)) {
    Write-Host "==> Creating the release keystore (first run only)" -ForegroundColor Cyan
    & $keytool -genkeypair -v `
        -keystore $keystore `
        -storepass $storePass `
        -keypass $storePass `
        -alias "secpocket" `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10000 `
        -dname "CN=SecPocket, OU=Field Library, O=SecPocket, L=Paris, C=FR"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed ($LASTEXITCODE)" }
}

Write-Host "==> Signing the APK" -ForegroundColor Cyan
& $apksigner sign `
    --ks $keystore `
    --ks-key-alias "secpocket" `
    --ks-pass "pass:$storePass" `
    --key-pass "pass:$storePass" `
    --out $final `
    $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed ($LASTEXITCODE)" }

if (-not (Test-Path $final)) { throw "Signed APK was not produced at $final" }

if (-not $SkipVerify) {
    Write-Host ""
    Write-Host "==> Verifying signature" -ForegroundColor Cyan
    & $apksigner verify --verbose --print-certs $final
    if ($LASTEXITCODE -ne 0) { throw "apksigner verify failed ($LASTEXITCODE)" }

    Write-Host ""
    Write-Host "==> APK badging" -ForegroundColor Cyan
    & $aapt2 dump badging $final
    if ($LASTEXITCODE -ne 0) { throw "aapt2 dump badging failed ($LASTEXITCODE)" }
}

$hash = (Get-FileHash -Algorithm SHA256 $final).Hash
$size = [math]::Round((Get-Item $final).Length / 1KB, 1)
Write-Host ""
Write-Host "Built: $final" -ForegroundColor Green
Write-Host "Size:  $size KB"
Write-Host "SHA256: $hash"
Write-Host ""
Write-Host "Install on a connected device with:"
Write-Host "  $sdk\platform-tools\adb.exe install -r `"$final`""
