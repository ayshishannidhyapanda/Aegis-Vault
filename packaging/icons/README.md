# AegisVault-J Icon Conversion Guide

This directory contains icon assets for AegisVault-J packaging.

## Source Icon

- `aegisvault-j.svg` - Vector source (256x256 viewBox)

## Required Formats

| Platform | Format | Required Sizes |
|----------|--------|----------------|
| Windows | `.ico` | 16, 32, 48, 64, 128, 256 |
| Linux | `.png` | 256x256 or 512x512 |
| macOS | `.icns` | 16, 32, 64, 128, 256, 512, 1024 |

## Conversion Commands

### SVG to PNG (all platforms)

Using ImageMagick:
```bash
magick convert aegisvault-j.svg -resize 256x256 aegisvault-j.png
magick convert aegisvault-j.svg -resize 1024x1024 aegisvault-j-1024.png
```

Using Inkscape:
```bash
inkscape aegisvault-j.svg --export-type=png --export-width=256 --export-filename=aegisvault-j.png
```

### Windows ICO

Using ImageMagick:
```bash
magick convert aegisvault-j.png -define icon:auto-resize=256,128,64,48,32,16 aegisvault-j.ico
```

Or create from multiple PNGs:
```bash
magick convert icon-16.png icon-32.png icon-48.png icon-64.png icon-128.png icon-256.png aegisvault-j.ico
```

### macOS ICNS

Using macOS iconutil:
```bash
# Create iconset directory
mkdir aegisvault-j.iconset

# Generate required sizes
sips -z 16 16     aegisvault-j-1024.png --out aegisvault-j.iconset/icon_16x16.png
sips -z 32 32     aegisvault-j-1024.png --out aegisvault-j.iconset/icon_16x16@2x.png
sips -z 32 32     aegisvault-j-1024.png --out aegisvault-j.iconset/icon_32x32.png
sips -z 64 64     aegisvault-j-1024.png --out aegisvault-j.iconset/icon_32x32@2x.png
sips -z 128 128   aegisvault-j-1024.png --out aegisvault-j.iconset/icon_128x128.png
sips -z 256 256   aegisvault-j-1024.png --out aegisvault-j.iconset/icon_128x128@2x.png
sips -z 256 256   aegisvault-j-1024.png --out aegisvault-j.iconset/icon_256x256.png
sips -z 512 512   aegisvault-j-1024.png --out aegisvault-j.iconset/icon_256x256@2x.png
sips -z 512 512   aegisvault-j-1024.png --out aegisvault-j.iconset/icon_512x512.png
sips -z 1024 1024 aegisvault-j-1024.png --out aegisvault-j.iconset/icon_512x512@2x.png

# Convert to icns
iconutil -c icns aegisvault-j.iconset -o aegisvault-j.icns

# Cleanup
rm -rf aegisvault-j.iconset
```

## Alternative Tools

- **GIMP**: Export as .ico or use plugin for .icns
- **Inkscape**: Export to PNG at various sizes
- **Online converters**: icoconvert.com, cloudconvert.com

## Icon Design Guidelines

- Use simple, recognizable shapes
- Ensure visibility at 16x16
- Use consistent color palette
- Test against light and dark backgrounds
