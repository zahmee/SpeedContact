"""Show what Google Play holds for each language's store listing, without changing anything.

    python tools/check_play_listing.py

Reads only: it opens a throwaway "edit", reads the listings and pictures, and deletes the edit.
Needs only a stock Python and openssl, like check_play_auth.py. Never prints the private key
or the access token.
"""
import json
import sys
import urllib.error
import urllib.request

from check_play_auth import KEY, access_token, application_id

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
IMAGE_TYPES = ["icon", "featureGraphic", "phoneScreenshots"]


def main() -> int:
    creds = json.loads(KEY.read_text(encoding="utf-8"))
    headers = {"Authorization": "Bearer " + access_token(creds), "Content-Type": "application/json"}

    def call(url: str, method: str = "GET", body: bytes | None = None):
        request = urllib.request.Request(url, data=body, method=method, headers=headers)
        raw = urllib.request.urlopen(request, timeout=60).read().decode("utf-8")
        return json.loads(raw) if raw.strip() else {}

    edits = BASE + application_id() + "/edits"
    edit = edits + "/" + call(edits, "POST", b"{}")["id"]
    try:
        details = call(edit + "/details")
        print("default language : " + details.get("defaultLanguage", "?"))
        print("contact email    : " + details.get("contactEmail", "-"))
        print("contact website  : " + details.get("contactWebsite", "-"))
        print()
        for listing in call(edit + "/listings").get("listings", []):
            language = listing["language"]
            counts = {}
            for image_type in IMAGE_TYPES:
                images = call(edit + "/listings/" + language + "/" + image_type).get("images", [])
                counts[image_type] = len(images)
            print(f"{language:7} {listing.get('title', ''):32}"
                  f" short {len(listing.get('shortDescription', '')):2}"
                  f" full {len(listing.get('fullDescription', '')):4}"
                  f" | icon {counts['icon']} feature {counts['featureGraphic']}"
                  f" screenshots {counts['phoneScreenshots']}")
    except urllib.error.HTTPError as failure:
        print("HTTP " + str(failure.code) + ": " + failure.read().decode("utf-8")[:500])
        return 1
    finally:
        call(edit, "DELETE")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
