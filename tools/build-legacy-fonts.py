"""Generate API-24 static instances of the bundled OFL Outfit variable font.

Run: uv run --with fonttools==4.60.1 python tools/build-legacy-fonts.py
The original license in launcher/FONT-LICENSE-OFL.txt applies to these instances.
"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

fonts = Path(__file__).resolve().parents[1] / "launcher/app/src/main/res/font"
for name, weight in [("extralight", 200), ("light", 300), ("regular", 400), ("medium", 500)]:
    font = TTFont(fonts / "outfit.ttf", recalcTimestamp=False)
    static = instantiateVariableFont(font, {"wght": weight}, inplace=True)
    static.save(fonts / f"outfit_legacy_{name}.ttf")
