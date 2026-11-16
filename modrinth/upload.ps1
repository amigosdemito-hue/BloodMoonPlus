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
    [Parameter(Mandatory = $false)][string]   $PatchVersion = "",
    [Parameter(Mandatory = $false)][switch]   $SyncOnly = $false,
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

# ---------------------------------------------------------------- ficha del proyecto
# Sincroniza el resumen y la descripcion publicados con modrinth/summary.txt y
# modrinth/description.md (la ficha vive en el proyecto, no en la version).
# Ojo: $pid es automatica y de solo lectura en PowerShell; no se puede reutilizar.
function Sync-ProjectFicha([string]$targetProjectId) {
    $sf = Join-Path $PSScriptRoot "summary.txt"
    $bf = Join-Path $PSScriptRoot "description.md"
    $patch = @{}
    if (Test-Path $sf) { $patch.description = [System.IO.File]::ReadAllText($sf, [System.Text.Encoding]::UTF8).Trim() }
    if (Test-Path $bf) { $patch.body        = [System.IO.File]::ReadAllText($bf, [System.Text.Encoding]::UTF8) }
    if ($patch.Count -eq 0) { Write-Host "Nada que sincronizar."; return }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes(($patch | ConvertTo-Json -Depth 6 -Compress))
    try {
        Invoke-RestMethod -Method Patch -Uri "https://api.modrinth.com/v2/project/$targetProjectId" `
            -Headers @{ Authorization = $env:MODRINTH_TOKEN } `
            -ContentType "application/json; charset=utf-8" `
            -Body $bytes -TimeoutSec 60 | Out-Null
        Write-Host "Ficha actualizada (resumen $($patch.description.Length) chars, descripcion $($patch.body.Length) chars)." -ForegroundColor Green
    } catch {
        Write-Host "FALLO al actualizar la ficha:" -ForegroundColor Red
        if ($_.Exception.Response) {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "CUERPO: $($sr.ReadToEnd())" -ForegroundColor Red
        } else {
            Write-Host $_.Exception.Message -ForegroundColor Red
        }
        throw
    }
}

if ($SyncOnly) {
    $id = (Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$ProjectSlug" `
            -Headers @{ Authorization = $env:MODRINTH_TOKEN } -TimeoutSec 30).id
    Write-Host "Project id=$id"
    Sync-ProjectFicha $id
    return
}

# ---------------------------------------------------------------- changelog
# Se toma SOLO la seccion de esta version desde modrinth/CHANGELOG.md.
# Antes se usaba $m.Groups[1] con un regex SIN grupo de captura: siempre venia vacio y
# todas las versiones subian el changelog de relleno "BloodMoonPlus <version>".
function Get-VersionChangelog([string]$version) {
    $changelogFile = Join-Path $PSScriptRoot "CHANGELOG.md"
    if (-not (Test-Path $changelogFile)) { return "" }
    $md      = [System.IO.File]::ReadAllText($changelogFile, [System.Text.Encoding]::UTF8)
    $escaped = [regex]::Escape($version)
    $m       = [regex]::Match($md, "(?ms)^## \[$escaped\][^\r\n]*\r?\n(?<body>.*?)(?=^## \[|\z)")
    if ($m.Success) { return $m.Groups["body"].Value.Trim() }
    return ""
}

$cl = Get-VersionChangelog $VersionNumber
if (-not $cl) { $cl = "BloodMoonPlus $VersionNumber" }
Write-Host "Changelog=$($cl.Length) chars"

if ($PatchVersion) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes((@{ changelog = $cl } | ConvertTo-Json -Depth 4 -Compress))
    try {
        Invoke-RestMethod -Method Patch -Uri "https://api.modrinth.com/v2/version/$PatchVersion" `
            -Headers @{ Authorization = $env:MODRINTH_TOKEN } `
            -ContentType "application/json; charset=utf-8" `
            -Body $bytes -TimeoutSec 60 | Out-Null
        Write-Host "Changelog actualizado en la version $PatchVersion" -ForegroundColor Green
    } catch {
        Write-Host "FALLO al actualizar el changelog:" -ForegroundColor Red
        if ($_.Exception.Response) {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "CUERPO: $($sr.ReadToEnd())" -ForegroundColor Red
        } else {
            Write-Host $_.Exception.Message -ForegroundColor Red
        }
        throw
    }
    return
}

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
    Write-Host "URL=https://modrinth.com/plugin/$ProjectSlug/version/$VersionNumber"
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

if ($SyncProject) { Sync-ProjectFicha $projectId }
