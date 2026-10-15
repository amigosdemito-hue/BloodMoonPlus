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
    foreach ($name in @("token.txt", ".token")) {
        $tokFile = Join-Path $PSScriptRoot $name
        if (-not (Test-Path $tokFile)) { continue }
        foreach ($line in ([System.IO.File]::ReadAllText($tokFile, [System.Text.Encoding]::UTF8) -split "`r?`n")) {
            $v = $line.Trim()
            if ($v -and -not $v.StartsWith("#")) { $env:MODRINTH_TOKEN = $v; break }
        }
        if ($env:MODRINTH_TOKEN) { break }
    }
}
if (-not $env:MODRINTH_TOKEN) {
    throw "Falta el token. Pegalo en modrinth\token.txt (o define MODRINTH_TOKEN). Crea un token en https://modrinth.com/settings/personal-access-tokens"
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
$changelog = if (Test-Path $changelogFile) {
    [System.IO.File]::ReadAllText($changelogFile, [System.Text.Encoding]::UTF8)
} else { "Version $VersionNumber" }

# La API exige el ID base62 del proyecto (no el slug) en project_id.
$projectId = $ProjectSlug
try {
    $pr = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$ProjectSlug" `
        -Headers @{ Authorization = $env:MODRINTH_TOKEN } -TimeoutSec 30
    $projectId = $pr.id
} catch {
    Write-Host "No se pudo resolver el proyecto '$ProjectSlug' (revisa el slug y el token)." -ForegroundColor Red
    throw
}
Write-Host "Proyecto : $ProjectSlug -> id=$projectId"

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
    project_id    = $projectId
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

# Lee el cuerpo real de un error HTTP (PowerShell 5.1 no lo muestra por defecto)
function Get-ApiError($err) {
    try {
        $resp = $err.Exception.Response
        if ($resp) {
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $body = $sr.ReadToEnd()
            if ($body) { return "HTTP $([int]$resp.StatusCode): $body" }
        }
    } catch { }
    if ($err.ErrorDetails -and $err.ErrorDetails.Message) { return $err.ErrorDetails.Message }
    return $err.Exception.Message
}

try {
    $resp = Invoke-RestMethod -Method Post -Uri "https://api.modrinth.com/v2/version" `
        -Headers @{ Authorization = $env:MODRINTH_TOKEN } `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $body
} catch {
    Write-Host "FALLO al subir la version:" -ForegroundColor Red
    Write-Host (Get-ApiError $_)
    throw
}

Write-Host "OK. Version publicada: $($resp.id)"
Write-Host "URL: https://modrinth.com/plugin/$ProjectSlug/version/$VersionNumber"
