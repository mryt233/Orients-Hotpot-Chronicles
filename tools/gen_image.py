"""Generate texture art through an OpenAI-compatible image API (e.g. a relay for gpt-image).

Config comes from environment variables, never from this repo:
  IMAGE_API_BASE_URL  e.g. https://your-relay.example.com/v1   (must include /v1 if the relay needs it)
  IMAGE_API_KEY       the relay's API key
  IMAGE_MODEL         optional, default gpt-image-1

The prompt you pass is only the item-specific part. The script prepends
tools/prompts/base.txt (the shared style rules) and, with --type, tools/prompts/<type>.txt.

Examples:
  python tools/gen_image.py "a pair of wooden chopsticks with red tips" --type item --pixel 32 -o tools/out/chopsticks.png
  python tools/gen_image.py "red clay bricks with grey mortar" --type tile --pixel 16 -o tools/out/bricks.png
  python tools/gen_image.py "copper pot rim, red spicy broth" --type top --pixel 16 --ref existing.png -o tools/out/spicy.png
"""
import argparse
import base64
import io
import os
import sys
from pathlib import Path

import requests
from PIL import Image


def env(name, default=None):
    value = os.environ.get(name, default)
    if not value:
        sys.exit(f"missing environment variable {name}")
    return value


PROMPTS = Path(__file__).parent / "prompts"
TYPES = sorted(p.stem for p in PROMPTS.glob("*.txt") if p.stem != "base")


def build_prompt(args):
    if args.no_base:
        return args.prompt
    parts = [(PROMPTS / "base.txt").read_text(encoding="utf-8")]
    if args.type:
        parts.append((PROMPTS / f"{args.type}.txt").read_text(encoding="utf-8"))
    parts.append("Subject: " + args.prompt)
    return " ".join(p.strip() for p in parts).replace("{size}", str(args.pixel or 16))


def request_image(args):
    base = env("IMAGE_API_BASE_URL").rstrip("/")
    headers = {"Authorization": f"Bearer {env('IMAGE_API_KEY')}"}
    fields = {"model": env("IMAGE_MODEL", "gpt-image-1"), "prompt": build_prompt(args), "size": args.size, "n": 1}
    if args.transparent or args.type == "item":
        fields["background"] = "transparent"

    if args.ref:
        # /images/edits takes multipart; the reference images steer palette and style
        files = [("image[]", (Path(p).name, open(p, "rb"), "image/png")) for p in args.ref]
        resp = requests.post(f"{base}/images/edits", headers=headers, data=fields, files=files, timeout=args.timeout)
    else:
        resp = requests.post(f"{base}/images/generations", headers=headers, json=fields, timeout=args.timeout)

    if resp.status_code != 200:
        sys.exit(f"HTTP {resp.status_code}: {resp.text[:500]}")
    item = resp.json()["data"][0]
    if item.get("b64_json"):
        return base64.b64decode(item["b64_json"])
    # some relays return a URL instead of base64
    return requests.get(item["url"], timeout=args.timeout).content


def pixelate(img, size, colors):
    # BOX averages each source block, which keeps shapes cleaner than NEAREST on AI art
    img = img.convert("RGBA").resize((size, size), Image.BOX)
    if colors:
        alpha = img.getchannel("A").point(lambda a: 255 if a >= 128 else 0)
        rgb = img.convert("RGB").quantize(colors=colors, method=Image.Quantize.MEDIANCUT).convert("RGB")
        img = rgb.convert("RGBA")
        img.putalpha(alpha)
    return img


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("prompt", help="item-specific description; shared style rules are added automatically")
    p.add_argument("-o", "--out", required=True, help="output PNG path")
    p.add_argument("--type", choices=TYPES, help="add the matching tools/prompts/<type>.txt")
    p.add_argument("--no-base", action="store_true", help="send the prompt as-is, without base.txt")
    p.add_argument("--show-prompt", action="store_true", help="print the full prompt and exit without calling the API")
    p.add_argument("--size", default="1024x1024", help="size requested from the API")
    p.add_argument("--ref", nargs="+", help="reference image(s); switches to the edits endpoint")
    p.add_argument("--transparent", action="store_true", help="ask for a transparent background")
    p.add_argument("--pixel", type=int, help="downscale to NxN for a Minecraft texture (e.g. 16)")
    p.add_argument("--colors", type=int, default=0, help="limit palette after --pixel (e.g. 24)")
    p.add_argument("--timeout", type=int, default=300)
    args = p.parse_args()

    if args.show_prompt:
        print(build_prompt(args))
        return

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    img = Image.open(io.BytesIO(request_image(args)))

    if args.pixel:
        # keep the full-resolution original outside src/ so it never lands in the jar
        raw = Path(__file__).parent / "out" / (out.stem + ".raw.png")
        raw.parent.mkdir(exist_ok=True)
        img.save(raw)
        img = pixelate(img, args.pixel, args.colors)
    img.save(out)
    print(out)


if __name__ == "__main__":
    main()
