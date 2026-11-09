"""桌面竹条的双色取色纹理；不改手持图标。作者：mryt233"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
image = Image.new("RGB", (16, 16), "#b0803a")
for x in range(8, 16):
    for y in range(16):
        image.putpixel((x, y), (206, 162, 80))
image.save(ROOT / "common/src/main/resources/assets/orients_hotpot_chronicles/textures/block/resting_bamboo.png")
