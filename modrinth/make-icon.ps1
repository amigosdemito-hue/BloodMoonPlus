# Genera modrinth/icon.png (256x256) para el proyecto.
# Requiere Windows PowerShell (usa System.Drawing del .NET Framework).
# Uso:  powershell -ExecutionPolicy Bypass -File .\make-icon.ps1

Add-Type -AssemblyName System.Drawing

$size = 256
$out  = Join-Path $PSScriptRoot 'icon.png'

$bmp = [System.Drawing.Bitmap]::new($size, $size)
$g   = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

try {
    # --- Fondo: degradado oscuro rojizo ---
    $rect = [System.Drawing.Rectangle]::new(0, 0, $size, $size)
    $bg = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $rect,
        [System.Drawing.Color]::FromArgb(255, 26, 7, 18),
        [System.Drawing.Color]::FromArgb(255, 6, 3, 9),
        90)
    $g.FillRectangle($bg, $rect)
    $bg.Dispose()

    # --- Estrellas (evitando el centro) ---
    $rnd = [System.Random]::new(7)
    for ($i = 0; $i -lt 80; $i++) {
        $x = $rnd.Next(6, $size - 6)
        $y = $rnd.Next(6, $size - 6)
        $dist = [Math]::Sqrt([Math]::Pow($x - 128, 2) + [Math]::Pow($y - 128, 2))
        if ($dist -lt 96) { continue }
        $r = 0.4 + $rnd.NextDouble() * 1.3
        $a = 110 + [int]($rnd.NextDouble() * 120)
        $b = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($a, 255, 220, 225))
        $g.FillEllipse($b, [float]($x - $r), [float]($y - $r), [float]($r * 2), [float]($r * 2))
        $b.Dispose()
    }

    # --- Glow de la luna (capas translúcidas) ---
    for ($i = 16; $i -ge 1; $i--) {
        $rr    = 74 + $i * 2.4
        $alpha = [int](6 + (16 - $i) * 1.1)
        $gb = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($alpha, 210, 25, 35))
        $g.FillEllipse($gb, [float](128 - $rr), [float](128 - $rr), [float]($rr * 2), [float]($rr * 2))
        $gb.Dispose()
    }

    # --- Disco de la luna ---
    $mg = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.Rectangle]::new(50, 50, 156, 156),
        [System.Drawing.Color]::FromArgb(255, 240, 70, 68),
        [System.Drawing.Color]::FromArgb(255, 118, 10, 20),
        135)
    $g.FillEllipse($mg, 50, 50, 156, 156)
    $mg.Dispose()

    # --- Cráteres ---
    $craters = @(
        @(100, 88, 18), @(162, 122, 13), @(124, 162, 11),
        @(152, 182, 7),  @(82, 142, 8),  @(146, 96, 6)
    )
    foreach ($c in $craters) {
        $cb = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(60, 88, 6, 14))
        $g.FillEllipse($cb, [float]($c[0] - $c[2]), [float]($c[1] - $c[2]), [float]($c[2] * 2), [float]($c[2] * 2))
        $cb.Dispose()
    }

    # --- Sombra de fase (media luna sutil) ---
    $shadow = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(90, 8, 2, 8))
    $g.FillEllipse($shadow, 86, 44, 150, 150)
    $shadow.Dispose()

    # --- Bisel claro superior ---
    $hi = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(70, 255, 170, 170), 2)
    $g.DrawArc($hi, 54, 54, 148, 148, 200, 120)
    $hi.Dispose()

    # --- Borde ---
    $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(80, 255, 90, 90), 2)
    $g.DrawEllipse($pen, 9, 9, 238, 238)
    $pen.Dispose()

    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "Icono generado: $out"
}
finally {
    $g.Dispose()
    $bmp.Dispose()
}
