# Landing page

A self-contained landing page for the Executable Use Cases project. No build
step, no dependencies, no network calls — open `index.html` in a browser, or
serve the folder:

```bash
python3 -m http.server -d site 8000   # then open http://localhost:8000
```

## Files

| | |
|---|---|
| `index.html` | The whole page — markup, styles, fonts and script inline |
| `og-card.png` | 1200×630 social preview image referenced by the OpenGraph tags |

## Fonts

Faustina (display), Karla (body) and DM Mono (labels and data) are embedded
directly in `index.html` as base64 `@font-face` rules, subset to latin. That
is ~157 KB of the file's ~257 KB, and it buys three things: the page renders
identically offline, there is no flash of fallback text, and it has no
external dependency at all. Only the eight family/weight/style combinations
the page actually uses are embedded — if you add a new weight, embed it too
or it will be synthesised.

## Theme

The page follows the visitor's `prefers-color-scheme` and also carries its own
toggle, which persists to `localStorage`. Every colour is defined as a token on
`:root`, redefined in the dark blocks; components only ever reference tokens.
Adding a colour literal to a component rule will break one of the two themes.

## Social preview

`og:image` is a relative path, which the major scrapers resolve against the
page URL. If you deploy somewhere that needs it absolute, replace the two
`og-card.png` values with the full URL.

To regenerate the card after a copy change, rebuild it from the same design
tokens at 1200×630 — the text on it is not read from the page.

## Content

Copy is drawn from `docs/proposal.md` and the root README. The pipeline stages
in section 03 — stage IDs, `type`, `onFailure` and `group` ordering — are taken
from the real EUC at
`src/main/resources/euc/grant-fit-assessment/grant-fit-assessment.json`. If that
file changes, update section 03 to match; it is the one part of the page that
makes a factual claim about the code.
