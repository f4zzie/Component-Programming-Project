#!/usr/bin/env python3
"""Composes the artifact gallery image from the files produced by the verification run."""
from __future__ import annotations

import csv
import os
from PIL import Image, ImageDraw, ImageFont

SANS = "/usr/share/fonts/liberation/LiberationSans-Regular.ttf"
SANS_BOLD = "/usr/share/fonts/liberation/LiberationSans-Bold.ttf"
MONO = "/usr/share/fonts/liberation/LiberationMono-Regular.ttf"
BG = (255, 255, 255)
INK = (24, 28, 35)
MUTED = (110, 118, 130)


def main():
    gallery = "docs/evidence/gallery"
    rows = []
    if os.path.exists(f"{gallery}/artifact-gallery.csv".replace("artifact", "")) or True:
        pass
    csv_path = "docs/evidence/artifact-gallery.csv"
    with open(csv_path, encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["file"].endswith(".png") and row["kind"].startswith("PNG"):
                rows.append(row)
    # deterministic order: main gallery first, then levels, then ladder, then the adapter
    def key(row):
        name = row["file"]
        return (0 if name.startswith("gallery") else 1 if name.startswith("level") else
                2 if name.startswith("ladder") else 3, name)
    rows.sort(key=key)
    rows = rows[:24]

    cell_w, cell_h = 300, 300
    caption_h = 92
    cols = 6
    rows_count = (len(rows) + cols - 1) // cols
    width = cols * cell_w + 40
    height = 80 + rows_count * (cell_h + caption_h) + 30
    image = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(image)
    draw.text((20, 18), "Figure 6 - Artifact gallery: every image below was decoded back by the independent ZXing decoder",
              font=ImageFont.truetype(SANS_BOLD, 18), fill=INK)
    draw.text((20, 48), "File names, byte sizes, SHA-256 digests and the recovered payloads are listed in docs/evidence/artifact-gallery.csv",
              font=ImageFont.truetype(SANS, 12), fill=MUTED)

    for index, row in enumerate(rows):
        column = index % cols
        line = index // cols
        x = 20 + column * cell_w
        y = 80 + line * (cell_h + caption_h)
        path = os.path.join(gallery, row["file"])
        symbol = Image.open(path).convert("RGB")
        symbol.thumbnail((cell_w - 20, cell_h - 20), Image.NEAREST)
        image.paste(symbol, (x + 10, y + 10))
        draw.rectangle((x + 6, y + 6, x + cell_w - 6, y + cell_h + 6), outline=(220, 224, 230))
        draw.text((x + 8, y + cell_h + 14), row["kind"][:40], font=ImageFont.truetype(SANS_BOLD, 12), fill=INK)
        payload = row["payload"].replace("\n", " ")
        draw.text((x + 8, y + cell_h + 32), payload[:38], font=ImageFont.truetype(MONO, 11), fill=MUTED)
        draw.text((x + 8, y + cell_h + 50), f'{row["bytes"]} bytes  decoded={row["decoded"][:10]}',
                  font=ImageFont.truetype(MONO, 11), fill=MUTED)
        draw.text((x + 8, y + cell_h + 68), row["sha256"][:32] + "...",
                  font=ImageFont.truetype(MONO, 10), fill=(150, 158, 170))

    os.makedirs("docs/diagrams", exist_ok=True)
    out = "docs/diagrams/figure-6-artifact-gallery.png"
    image.save(out)
    print("gallery image:", out, image.size, f"({len(rows)} artifacts)")


if __name__ == "__main__":
    main()
