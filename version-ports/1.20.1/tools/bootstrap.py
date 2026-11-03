# 作者：mryt233
"""从冻结底本建立 1.20.1 工程；已存在的文件不覆盖。"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT.parent / "base" / "0.2.1-alpha"


def copy_tree(source, target):
    for file in source.rglob("*"):
        if file.is_file():
            dest = target / file.relative_to(source)
            if not dest.exists():
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(file, dest)


for name in ("LICENSE", "ASSETS_LICENSE", "gradlew", "gradlew.bat", "build.gradle", "settings.gradle", "gradle.properties"):
    dest = ROOT / name
    if not dest.exists():
        shutil.copy2(BASE / name, dest)
copy_tree(BASE / "gradle", ROOT / "gradle")
copy_tree(BASE / "framework", ROOT / "framework")
for group in ("main", "client", "mixin"):
    copy_tree(BASE / "common" / "src" / group, ROOT / "common" / "src" / group)
for name in ("common", "fabric"):
    (ROOT / name).mkdir(parents=True, exist_ok=True)
    if not (ROOT / name / "build.gradle").exists():
        shutil.copy2(BASE / name / "build.gradle", ROOT / name / "build.gradle")
copy_tree(BASE / "fabric" / "src" / "main", ROOT / "fabric" / "src" / "main")

# 原版网络契约的等价兼容类型；网络 ID 与字段编码不变。
replacements = {
    "net.minecraft.network.RegistryFriendlyByteBuf": "cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf",
    "net.minecraft.network.codec.StreamCodec": "cn.blockforge.orient.framework.compat.StreamCodec",
    "net.minecraft.network.codec.ByteBufCodecs": "cn.blockforge.orient.framework.compat.ByteBufCodecs",
    "net.minecraft.network.protocol.common.custom.CustomPacketPayload": "cn.blockforge.orient.framework.compat.CustomPacketPayload",
    "ResourceLocation.fromNamespaceAndPath(": "new ResourceLocation(",
    ".getFirst()": ".get(0)",
}
for module in ("framework", "common", "fabric"):
    for file in (ROOT / module).rglob("*.java"):
        text = file.read_text(encoding="utf-8-sig")
        for old, new in replacements.items():
            text = text.replace(old, new)
        file.write_text(text, encoding="utf-8")
