from PIL import Image
from pathlib import Path

src_path = Path(r"d:\work\lyulf\mind\android\playstore-icon_transparent.png")
res = Path(r"d:\work\lyulf\mind\android\app\src\main\res")
src = Image.open(src_path).convert("RGBA")
print("source", src.size, src.mode)


def fit_on_canvas(img, canvas_size, content_ratio, canvas_rgba):
    canvas = Image.new("RGBA", (canvas_size, canvas_size), canvas_rgba)
    max_side = int(canvas_size * content_ratio)
    logo = img.copy()
    logo.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
    x = (canvas_size - logo.width) // 2
    y = (canvas_size - logo.height) // 2
    canvas.paste(logo, (x, y), logo)
    return canvas


drawable = res / "drawable"
drawable.mkdir(exist_ok=True)
old_xml = drawable / "ic_launcher_foreground.xml"
if old_xml.exists():
    old_xml.unlink()

# Adaptive foreground: transparent around mark (system places on white bg)
fg = fit_on_canvas(src, 1080, 0.72, (0, 0, 0, 0))
fg.save(drawable / "ic_launcher_foreground.png", "PNG")
print("fg", (drawable / "ic_launcher_foreground.png").stat().st_size)

# Splash logo: keep transparency (Compose draws on white)
src.save(drawable / "ic_splash_logo.png", "PNG")
print("splash", src.size)

# Legacy full icons on white
for folder, size in {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}.items():
    d = res / folder
    d.mkdir(exist_ok=True)
    icon = fit_on_canvas(src, size, 0.88, (255, 255, 255, 255))
    icon.save(d / "ic_launcher.png", "PNG")
    # also round alias same file
    icon.save(d / "ic_launcher_round.png", "PNG")
    print(folder, size)

print("done")
