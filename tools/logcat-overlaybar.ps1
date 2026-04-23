. "$PSScriptRoot\\android-env.ps1"

$adb = Get-AdbPath
$packageName = "com.example.overlaybar"

& $adb logcat --pid=$(& $adb shell pidof -s $packageName)
