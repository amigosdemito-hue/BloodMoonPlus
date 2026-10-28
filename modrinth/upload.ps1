#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Sube una versión de EventoPvP a Modrinth.
#>
param(
    [Parameter(Mandatory = $true)][string]   $ProjectSlug,
    [Parameter(Mandatory = $true)][string]   $VersionNumber,
    [Parameter(Mandatory = $false)][string[]]$GameVersions = @("1.20.6","1.20.7","1.21"),
    [Parameter(Mandatory = $false)][string]   $JarPath = "",
    [Parameter(Mandatory = $false)][string]   $Channel = "release",
    [Parameter(Mandatory = $false)][string[]]$Loaders = @("fabric","forge","java_edition","modrinth","paper","quilt"),
    [Parameter(Mandatory = $false)][switch]   $SyncProject = $false,
    [Parameter(Mandatory = $false)][switch]   $DryRun = $false
)
$ErrorActionPreference = "Stop"

$GameVersions = @($GameVersions | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$Loaders      = @($Loaders      | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
if ($GameVersions.Count -eq 0) { throw "-GameVersions vacío" }
if ($Loaders.Count -eq 0)      { throw "-Loaders vacío" }

if (-not $env:MODRINTH_TOKEN) {
    $tokFile = Join-Path $PSScriptRoot "token.txt"
    if (Test-Path $tokFile) {
        foreach ($line in ([System.IO.File]::ReadAllText($tokFile, [System.Text.Encoding]::UTF8) -split "`r?`n")) {
            $v = $line.Trim()
            if ($v -and -not $v.StartsWith("#")) { $env:MODRINTH_TOKEN = $v; break }
        }
    }
}
if (-not $env:MODRINTH_TOKEN) { throw "Falta token de Modrinth." }

if (-not $JarPath) {
    $root = Split-Path -Parent $PSScriptRoot
    $JarPath = Get-ChildItem (Join-Path $root "build\libs") -Filter "BloodMoonPlus-*.jar" -ErrorAction SilentlyContinue |
               Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $JarPath -or -not (Test-Path $JarPath)) { throw "Jar no encontrado." }

$jarFile = [System.IO.Path]::GetFileName($JarPath)
$jarBytes = [System.IO.File]::ReadAllBytes($JarPath)
Write-Host "Jar=$JarPath bytes=$($jarBytes.Length)"
Write-Host "Ver=$VersionNumber channel=$Channel loaders=$($Loaders-join',') mc=$($GameVersions-join',')"

if ($DryRun) { Write-Host "[DryRun] sin subir"; return }

$projectId = $ProjectSlug
try {
    $pr = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$ProjectSlug" `
        -Headers @{ Authorization = $env:MODRINTH_TOKEN } -TimeoutSec 30
    $projectId = $pr.id
    Write-Host "Project id=$projectId"
} catch {
    Write-Host "No se pudo resolver proyecto." -ForegroundColor Red
    Write-Host $_.Exception.Response.StatusCode.value__ -ForegroundColor Red
    throw
}

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
function Field($n,$v) { "--$boundary$LF" + "Content-Disposition: form-data; name=`"$n`"$LF$LF" + "$v$LF" }

# Changelog: se toma SOLO la seccion de esta version desde modrinth/CHANGELOG.md
# (antes leia "nada.md", que no existe, y mandaba texto de otro proyecto).
$cl = ""
$changelogFile = Join-Path $PSScriptRoot "CHANGELOG.md"
if (Test-Path $changelogFile) {
    $md      = [System.IO.File]::ReadAllText($changelogFile, [System.Text.Encoding]::UTF8)
    $escaped = [regex]::Escape($VersionNumber)
    $m       = [regex]::Match($md, "(?ms)^## \[$escaped\][^\r\n]*\r?\n.*?(?=^## \[|\z)")
    if ($m.Success) { $cl = $m.Groups[1].Value.Trim() }
}
if (-not $cl) { $cl = "BloodMoonPlus $VersionNumber" }

$dataJson = @{
    name          = $VersionNumber
    version_number= $VersionNumber
    changelog     = $cl
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
$header = "--$boundary$LF" + "Content-Disposition: form-data; name=`"file`"; filename=`"$jarFile`"$LF" +
          "Content-Type: application/java-archive$LF$LF"
$bodyStart = [System.Text.Encoding]::UTF8.GetBytes($sb.ToString() + $header)
$bodyEnd   = [System.Text.Encoding]::UTF8.GetBytes("$LF--$boundary--$LF")
$body = New-Object byte[] ($bodyStart.Length + $jarBytes.Length + $bodyEnd.Length)
[System.Buffer]::BlockCopy($bodyStart, 0, $body, 0, $bodyStart.Length)
[System.Buffer]::BlockCopy($jarBytes, 0, $body, $bodyStart.Length, $jarBytes.Length)
[System.Buffer]::BlockCopy($bodyEnd, 0, $body, $bodyStart.Length + $jarBytes.Length, $bodyEnd.Length)

Write-Host "Subiendo..."
try {
    $resp = Invoke-RestMethod -Method Post -Uri "https://api.modrinth.com/v2/version" `
        -Headers @{ Authorization = $env:MODRINTH_TOKEN } `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $body -TimeoutSec 300
    Write-Host "OK version=$($resp.id)"
    Write-Host "URL=https://modrinth.com/game/eventopvp/version/$VersionNumber"
} catch {
    Write-Host "FALLO:" -ForegroundColor Red
    if ($_.Exception.Response) {
        $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "CUERPO: $($sr.ReadToEnd())" -ForegroundColor Red
    } else {
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    throw
}

if ($SyncProject) {
    $sf = Join-Path (Split-Path -Parent $PSScriptRoot) "modrinth\summary.txt"
    $bf = Join-Path (Split-Path -Parent $PSScriptRoot) "modrinth\description.md"
    $patch = @{}
    if (Test-Path $sf) { $patch.description = [System.IO.File]::ReadAllText($sf, [System.Text.Encoding]::UTF8).Trim() }
    if (Test-Path $bf) { $patch.body = [System.IO.File]::ReadAllText($bf, [System.Text.Encoding]::UTF8) }
    if ($patch.Count -gt 0) {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes(($patch | ConvertTo-Json -Depth 6 -Compress))
        try {
            Invoke-RestMethod -Method Patch -Uri "https://api.modrinth.com/v2/project/$projectId" `
                -Headers @{ Authorization = $env:MODRINTH_TOKEN } `
                -ContentType "application/json; charset=utf-8" `
                -Body $bytes -TimeoutSec 60 | Out-Null
            Write-Host "Ficha actualizada." -ForegroundColor Green
        } catch {
            Write-Host "Fichas fallida:" -ForegroundColor Red
            if ($_.Exception.Response) {
                $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                Write-Host "CUERPO: $($sr.ReadToEnd())" -ForegroundColor Red
            }
        }
    }
}
