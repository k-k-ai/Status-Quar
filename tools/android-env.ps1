function Get-RepoRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-AndroidSdkDir {
    $repoRoot = Get-RepoRoot
    $localPropertiesPath = Join-Path $repoRoot "local.properties"
    if (-not (Test-Path $localPropertiesPath)) {
        throw "local.properties not found at $localPropertiesPath"
    }

    $sdkLine = Get-Content $localPropertiesPath | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if (-not $sdkLine) {
        throw "sdk.dir was not found in local.properties"
    }

    $rawValue = $sdkLine -replace '^sdk\.dir=', ''
    $sdkDir = $rawValue -replace '\\:', ':' -replace '\\\\', '\'
    return $sdkDir
}

function Get-AdbPath {
    $sdkDir = Get-AndroidSdkDir
    $adbPath = Join-Path $sdkDir "platform-tools\\adb.exe"
    if (-not (Test-Path $adbPath)) {
        throw "adb.exe not found at $adbPath"
    }
    return $adbPath
}

function Get-GradleWrapperPath {
    $repoRoot = Get-RepoRoot
    $wrapperPath = Join-Path $repoRoot "gradlew.bat"
    if (-not (Test-Path $wrapperPath)) {
        throw "gradlew.bat not found at $wrapperPath"
    }
    return $wrapperPath
}
