param(
    [Parameter(Mandatory=$true)][string]$Tag,
    [Parameter(Mandatory=$true)][string]$Title,
    [Parameter(Mandatory=$true)][string]$CommitMsg,
    [Parameter(Mandatory=$true)][string]$Notes
)

$ErrorActionPreference = "Stop"

git add -A
git commit -m $CommitMsg
git tag -a $Tag -m $Title
git push origin main
git push origin $Tag

$apkName = "ConsumoEnergetico-$Tag.apk"
$apkPath = "$env:USERPROFILE\Desktop\$apkName"
Copy-Item "app\build\outputs\apk\debug\app-debug.apk" $apkPath -Force

$notesFile = "$env:TEMP\release-notes-$Tag.md"
$Notes | Out-File -FilePath $notesFile -Encoding utf8

gh release create $Tag $apkPath `
  --title $Title `
  --notes-file $notesFile `
  --prerelease

Remove-Item $notesFile -Force
Write-Host "Release $Tag publicado en GitHub con $apkName" -ForegroundColor Green
