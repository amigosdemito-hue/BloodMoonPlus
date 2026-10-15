# Publica BloodMoonPlus en Modrinth de principio a fin:
#   1) crea el proyecto (si no existe) con icono, resumen, descripcion y enlaces
#   2) sube la version del .jar (reutiliza upload.ps1)
#
# El TOKEN nunca se imprime. Se lee, por orden, de:
#   1) -Token "mrp_xxx"
#   2) variable de entorno $env:MODRINTH_TOKEN
#   3) archivo modrinth\.token  (una sola linea; esta en .gitignore)
#
# Uso:
#   .\publish.ps1
#   .\publish.ps1 -DryRun
#
# Scope necesario del token: PROJECT_CREATE + VERSION_CREATE (+ PROJECT_READ).
# Se crea en https://modrinth.com/settings/personal-access-tokens

param(
    [string]  $ProjectSlug  = "bloodmoon-plus",
    [string]  $Title        = "BloodMoonPlus",
    [string]  $VersionNumber= "",
    [string[]]$GameVersions = @("26.2", "26.3"),
    [string[]]$Loaders      = @("paper"),
    [string]  $Token        = "",
    [switch]  $SkipProject,
    [switch]  $SkipVersion,
    [switch]  $DryRun
)

$ErrorActionPreference = "Stop"
$ProgressPreference    = "SilentlyContinue"
$Api = "https://api.modrinth.com/v2"

# ---------------------------------------------------------------- token
if (-not $Token) { $Token = $env:MODRINTH_TOKEN }
if (-not $Token) {
    foreach ($name in @("token.txt", ".token")) {
        $tokFile = Join-Path $PSScriptRoot $name
        if (-not (Test-Path $tokFile)) { continue }
        foreach ($line in ([System.IO.File]::ReadAllText($tokFile, [System.Text.Encoding]::UTF8) -split "`r?`n")) {
            $v = $line.Trim()
            if ($v -and -not $v.StartsWith("#")) { $Token = $v; break }
        }
        if ($Token) { break }
    }
}
if (-not $Token) {
    throw "Falta el token. Pegalo en modrinth\token.txt, define `$env:MODRINTH_TOKEN o usa -Token."
}
$Headers = @{ Authorization = $Token }

# ---------------------------------------------------------------- metadatos
function Read-Utf8([string]$name, [string]$fallback = "") {
    $p = Join-Path $PSScriptRoot $name
    if (Test-Path $p) { return [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8) }
    return $fallback
}

$summary = (Read-Utf8 "summary.txt").Trim()
$body    = Read-Utf8 "description.md"
$icon    = Join-Path $PSScriptRoot "icon.png"

if (-not $summary) { throw "Falta modrinth\summary.txt" }
if (-not $body)    { throw "Falta modrinth\description.md" }
if (-not (Test-Path $icon)) { throw "Falta modrinth\icon.png" }

# version: parametro > build.gradle.kts > 1.0.0
if (-not $VersionNumber) {
    $root = Split-Path -Parent $PSScriptRoot
    $gradle = Join-Path $root "build.gradle.kts"
    if (Test-Path $gradle) {
        $m = [regex]::Match((Get-Content $gradle -Raw), 'version\s*=\s*"([^"]+)"')
        if ($m.Success) { $VersionNumber = $m.Groups[1].Value }
    }
    if (-not $VersionNumber) { $VersionNumber = "1.0.0" }
}

# ---------------------------------------------------------------- multipart
function New-MultipartBody {
    param(
        [hashtable]$Fields,
        [string]   $FilePath,
        [string]   $FileField = "file",
        [string]   $FileContentType = "application/octet-stream"
    )
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"

    $sb = New-Object System.Text.StringBuilder
    foreach ($k in $Fields.Keys) {
        [void]$sb.Append("--$boundary$LF")
        [void]$sb.Append("Content-Disposition: form-data; name=`"$k`"$LF$LF")
        [void]$sb.Append("$($Fields[$k])$LF")
    }

    $head = $sb.ToString()
    if ($FilePath) {
        $head += "--$boundary$LF" +
                 "Content-Disposition: form-data; name=`"$FileField`"; filename=`"$([System.IO.Path]::GetFileName($FilePath))`"$LF" +
                 "Content-Type: $FileContentType$LF$LF"
    }

    $bytesHead = [System.Text.Encoding]::UTF8.GetBytes($head)
    $bytesFile = [byte[]]@()
    if ($FilePath) { $bytesFile = [System.IO.File]::ReadAllBytes($FilePath) }
    $tail      = if ($FilePath) { "$LF--$boundary--$LF" } else { "--$boundary--$LF" }
    $bytesEnd  = [System.Text.Encoding]::UTF8.GetBytes($tail)

    $body = New-Object byte[] ($bytesHead.Length + $bytesFile.Length + $bytesEnd.Length)
    [System.Buffer]::BlockCopy($bytesHead, 0, $body, 0, $bytesHead.Length)
    [System.Buffer]::BlockCopy($bytesFile, 0, $body, $bytesHead.Length, $bytesFile.Length)
    [System.Buffer]::BlockCopy($bytesEnd,  0, $body, $bytesHead.Length + $bytesFile.Length, $bytesEnd.Length)

    return @{ Boundary = $boundary; Body = $body }
}

# ---------------------------------------------------------------- estado
Write-Host "== BloodMoonPlus -> Modrinth ==" -ForegroundColor Magenta
Write-Host "Proyecto : $Title ($ProjectSlug)"
Write-Host "Version  : $VersionNumber  loaders=$($Loaders -join ',')  mc=$($GameVersions -join ',')"
Write-Host "Icono    : $(Split-Path -Leaf $icon)"
Write-Host "Resumen  : $summary"

$existing = $null
if (-not $SkipProject) {
    try {
        $existing = Invoke-RestMethod -Uri "$Api/project/$ProjectSlug" -Headers $Headers -TimeoutSec 30
    } catch {
        $existing = $null
    }
}

if ($existing) {
    Write-Host "El proyecto ya existe (id=$($existing.id), status=$($existing.status)). No se recrea." -ForegroundColor Yellow
} elseif ($SkipProject) {
    Write-Host "[-SkipProject] Se omite la creacion." -ForegroundColor Yellow
} else {
    function New-ProjectData([bool]$withStatus) {
        $d = @{
            title                 = $Title
            slug                  = $ProjectSlug
            description           = $summary
            body                  = $body
            categories            = @("adventure", "mobs", "game-mechanics", "equipment")
            additional_categories = @()
            project_type          = "mod"
            environment           = @("server_only")
            source_url            = "https://github.com/amigosdemito-hue/BloodMoonPlus"
            issues_url            = "https://github.com/amigosdemito-hue/BloodMoonPlus/issues"
            wiki_url              = $null
            discord_url           = $null
            donation_urls         = @()
        }
        if ($withStatus) { $d.status = "approved" }
        return $d
    }

    $dataJson = (New-ProjectData $true) | ConvertTo-Json -Depth 8 -Compress
    $mp = New-MultipartBody -Fields @{ data = $dataJson } -FilePath $icon -FileField "icon" -FileContentType "image/png"

    if ($DryRun) {
        Write-Host "[DryRun] Crearia el proyecto ($($dataJson.Length) chars de data, peticion de $($mp.Body.Length) bytes)." -ForegroundColor Cyan
    } else {
        Write-Host "Creando proyecto..." -NoNewline
        $proj = $null
        foreach ($withStatus in @($true, $false)) {
            $dj = (New-ProjectData $withStatus) | ConvertTo-Json -Depth 8 -Compress
            $m  = New-MultipartBody -Fields @{ data = $dj } -FilePath $icon -FileField "icon" -FileContentType "image/png"
            try {
                $proj = Invoke-RestMethod -Method Post -Uri "$Api/project" -Headers $Headers `
                    -ContentType "multipart/form-data; boundary=$($m.Boundary)" -Body $m.Body -TimeoutSec 90
                break
            } catch {
                if ($withStatus) {
                    Write-Host " (reintento sin 'status')" -NoNewline -ForegroundColor Yellow
                } else {
                    Write-Host " FALLO" -ForegroundColor Red
                    Write-Host $_.Exception.Message
                    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
                    throw
                }
            }
        }
        Write-Host " OK (id=$($proj.id), status=$($proj.status))" -ForegroundColor Green
    }
}

# ---------------------------------------------------------------- version
if ($SkipVersion) {
    Write-Host "[-SkipVersion] No se sube ninguna version." -ForegroundColor Yellow
} elseif ($DryRun) {
    Write-Host "[DryRun] Subiria la version $VersionNumber." -ForegroundColor Cyan
} else {
    $env:MODRINTH_TOKEN = $Token   # upload.ps1 lo lee de la variable de entorno
    & (Join-Path $PSScriptRoot "upload.ps1") -ProjectSlug $ProjectSlug -VersionNumber $VersionNumber `
        -GameVersions $GameVersions -Loaders $Loaders
}

if (-not $DryRun) {
    Write-Host ""
    Write-Host "Proyecto : https://modrinth.com/plugin/$ProjectSlug" -ForegroundColor Green
    Write-Host "Versiones: https://modrinth.com/plugin/$ProjectSlug/versions" -ForegroundColor Green
}
