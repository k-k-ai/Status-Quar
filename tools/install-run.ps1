. "$PSScriptRoot\\android-env.ps1"

$repoRoot = Get-RepoRoot
$gradle = Get-GradleWrapperPath
$adb = Get-AdbPath
$packageName = "com.example.overlaybar"
$activityName = "com.example.overlaybar/.MainActivity"

Push-Location $repoRoot
try {
    & $gradle :app:installDebug
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    & $adb wait-for-device
    & $adb shell am start -n $activityName
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
