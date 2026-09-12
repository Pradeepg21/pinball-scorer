# Store Assets

Play Console listing graphics for **Retro Pocket Pinball**, generated procedurally
(no external art) so they match the in-app icon and neon-cosmic-reactor theme.

## Files

| File | Size | Used for |
|---|---|---|
| `icon-512.png` | 512x512, opaque PNG | Play Console → "App icon" (hi-res icon) |
| `feature-graphic-1024x500.png` | 1024x500, opaque PNG | Play Console → "Feature graphic" |

Both meet Play Console's format rules: exact dimensions, no alpha/transparency,
well under the size limits.

## Regenerating / editing

Everything is drawn by `generate_assets.py` using Pillow + numpy (already
available in this machine's Python — no extra install needed):

```
cd store-assets
python generate_assets.py
```

To tweak the look, edit the `CONFIG` block at the top of the script — title
text, tagline, and every color are named constants — then re-run. There's no
separate "source" design file to keep in sync; the script *is* the source.

## Still needed before closed testing

These aren't generated here and need real device captures:

- **Phone screenshots** (min 2, up to 8) — run the app (`store-assets` has
  nothing to do with this) and capture: main menu, difficulty select,
  mid-game action (ball near a bumper/net), and the high-scores leaderboard.
- **Short description / full description** — a first draft already exists at
  `../.artifacts/play_store_listing.artifact.md`; review and paste into
  Play Console's listing page.
- **Signed release App Bundle** — Play Console requires a signed `.aab`. This
  machine has no upload keystore yet; see the note left in the conversation
  about generating one (or locating an existing one if this app was ever
  uploaded before) before running `./gradlew bundleRelease` with a signing
  config wired up.
