. "$PSScriptRoot\\android-env.ps1"

$repoRoot = Get-RepoRoot
$gradle = Get-GradleWrapperPath

Push-Location $repoRoot
try {
    & $gradle :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
