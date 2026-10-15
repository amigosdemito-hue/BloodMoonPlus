# Sube una version del plugin a Modrinth usando la API v2.
#
# Uso:
#   $env:MODRINTH_TOKEN = "mrp_xxx"
#   .\upload.ps1 -ProjectSlug "bloodmoon-plus" -VersionNumber "1.0.0" -GameVersions "26.2","26.3"
#
# Requiere que el proyecto YA exista en Modrinth.

param(
    [Parameter(Mandatory = $true)][string]   $ProjectSlug,
    [Parameter(Mandatory = $true)][string]   $VersionNumber,
    [Parameter(Mandatory = $false)][string[]]$GameVersions = @("26.2", "26.3"),
    [Parameter(Mandatory = $false)][string]   $JarPath = "",
    [Parameter(Mandatory = $false)][string]   $Channel = "release",
    [Parameter(Mandatory = $false)][string[]]$Loaders = @("paper"),
    [Parameter(Mandatory = $false)][switch]   $DryRun
)

$ErrorActionPreference = "Stop"

if (-not $env:MODRINTH_TOKEN) {
    throw "Falta la variable de entorno MODRINTH_TOKEN. Crea un token en https://modrinth.com/settings/personal-access-tokens"
}

$root = Split-Path -Parent $PSScriptRoot
if (-not $JarPath) {
    $JarPath = Get-ChildItem (Join-Path $root "build\libs") -Filter "BloodMoonPlus-*.jar" -ErrorAction SilentlyContinue |
               Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $JarPath -or -not (Test-Path $JarPath)) {
    throw "No se encontro el .jar. Compila con '.\gradlew.bat build' o pasa -JarPath."
}

$changelogFile = Join-Path $PSScriptRoot "CHANGELOG.md"
$changelog = if (Test-Path $changelogFile) { Get-Content $changelogFile -Raw } else { "Version $VersionNumber" }

$versionName = "$VersionNumber"

# Construye el cuerpo multipart/form-data
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

function Field($name, $value) {
    "--$boundary$LF" +
    "Content-Disposition: form-data; name=`"$name`"$LF$LF" +
    "$value$LF"
}

$dataJson = @{
    name          = $versionName
    version_number= $VersionNumber
    changelog     = $changelog
    dependencies  = @()
    game_versions = $GameVersions
    version_type  = $Channel
    loaders       = $Loaders
    featured      = $false
    project_id    = $ProjectSlug
    file_parts    = @("file")
    primary_file  = "file"
} | ConvertTo-Json -Depth 6 -Compress

$sb = New-Object System.Text.StringBuilder
[void]$sb.Append((Field "data" $dataJson))

$fileBytes = [System.IO.File]::ReadAllBytes($JarPath)
$fileName  = [System.IO.Path]::GetFileName($JarPath)

$header = "--$boundary$LF" +
          "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$LF" +
          "Content-Type: application/java-archive$LF$LF"

$bodyStart = [System.Text.Encoding]::UTF8.GetBytes($sb.ToString() + $header)
$bodyEnd   = [System.Text.Encoding]::UTF8.GetBytes("$LF--$boundary--$LF")

$body = New-Object byte[] ($bodyStart.Length + $fileBytes.Length + $bodyEnd.Length)
[System.Buffer]::BlockCopy($bodyStart, 0, $body, 0, $bodyStart.Length)
[System.Buffer]::BlockCopy($fileBytes, 0, $body, $bodyStart.Length, $fileBytes.Length)
[System.Buffer]::BlockCopy($bodyEnd, 0, $body, $bodyStart.Length + $fileBytes.Length, $bodyEnd.Length)

Write-Host "Jar:      $JarPath"
Write-Host "Version:  $VersionNumber ($Channel)  loaders=$($Loaders -join ',')  mc=$($GameVersions -join ',')"

if ($DryRun) {
    Write-Host "[DryRun] No se sube nada."
    return
}

$resp = Invoke-RestMethod -Method Post -Uri "https://api.modrinth.com/v2/version" `
    -Headers @{ Authorization = $env:MODRINTH_TOKEN } `
    -ContentType "multipart/form-data; boundary=$boundary" `
    -Body $body

Write-Host "OK. Version publicada: $($resp.id)"
Write-Host "URL: https://modrinth.com/plugin/$ProjectSlug/version/$VersionNumber"
