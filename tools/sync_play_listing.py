"""Generate app/src/main/play from store/, where the store listing is written and reviewed.

The publishing plugin reads one folder per language. Keeping one source here means a change to
store/listing.md or its pictures reaches Google Play by running this and publishing, with no
second copy to forget.

    python tools/sync_play_listing.py

Touches only files on this machine. Fails, writing nothing, if any text is over Google's limits.
"""
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STORE = ROOT / "store"
PLAY = ROOT / "app" / "src" / "main" / "play"

DEFAULT_LANGUAGE = "ar"
CONTACT_EMAIL = "info@cdit.co"
CONTACT_WEBSITE = "https://cdit.co"

# The headings of store/listing.md, and the codes Google Play knows those languages by.
LANGUAGES = {
    "Arabic": ["ar"],
    "English": ["en-US"],
    "Urdu": ["ur"],
    "Hindi": ["hi-IN"],
    "Indonesian": ["id"],
    "Turkish": ["tr-TR"],
    "Spanish": ["es-419", "es-ES"],
    "Portuguese (Brazil)": ["pt-BR"],
}

LIMITS = {"title": 30, "short-description": 80, "full-description": 4000}


def read_listing() -> dict:
    text = (STORE / "listing.md").read_text(encoding="utf-8")
    listing = {}
    for section in re.split(r"(?m)^## ", text)[1:]:
        name = section.splitlines()[0].strip()
        blocks = re.findall(r"(?s)```\r?\n(.*?)\r?\n```", section)
        if name not in LANGUAGES or len(blocks) != 3:
            sys.exit(f"unexpected section in store/listing.md: {name} ({len(blocks)} text blocks)")
        listing[name] = dict(zip(LIMITS, blocks))
    missing = set(LANGUAGES) - set(listing)
    if missing:
        sys.exit("missing from store/listing.md: " + ", ".join(sorted(missing)))
    for name, fields in listing.items():
        for field, value in fields.items():
            if len(value) > LIMITS[field]:
                sys.exit(f"{name} {field} is {len(value)} characters, limit {LIMITS[field]}")
    return listing


def copy_pictures(code: str) -> None:
    """Arabic has its own pictures; a language without any would show Arabic, so the rest get English."""
    graphics = STORE / "graphics"
    arabic = code == DEFAULT_LANGUAGE
    feature = graphics / ("feature-1024x500.png" if arabic else "feature-en-1024x500.png")
    shots = graphics / ("screenshots-ar" if arabic else "screenshots-en")

    target = PLAY / "listings" / code / "graphics"
    if target.exists():
        shutil.rmtree(target)
    (target / "feature-graphic").mkdir(parents=True)
    shutil.copyfile(feature, target / "feature-graphic" / "feature.png")
    (target / "phone-screenshots").mkdir()
    for shot in sorted(shots.glob("*.png")):
        shutil.copyfile(shot, target / "phone-screenshots" / shot.name)
    if arabic:
        (target / "icon").mkdir()
        shutil.copyfile(graphics / "icon-512.png", target / "icon" / "icon.png")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    listing = read_listing()

    write(PLAY / "default-language.txt", DEFAULT_LANGUAGE)
    write(PLAY / "contact-email.txt", CONTACT_EMAIL)
    write(PLAY / "contact-website.txt", CONTACT_WEBSITE)

    for name, codes in LANGUAGES.items():
        for code in codes:
            folder = PLAY / "listings" / code
            for field, value in listing[name].items():
                write(folder / f"{field}.txt", value)
            copy_pictures(code)
            count = len(list((folder / "graphics" / "phone-screenshots").glob("*.png")))
            print(f"{code:7} title {len(listing[name]['title']):2}  screenshots {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
