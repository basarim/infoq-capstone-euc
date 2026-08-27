# Landing page

A self-contained landing page for the Executable Use Cases project. No build
step and no dependencies — open `index.html` in a browser, or serve the folder:

```bash
python3 -m http.server -d site 8000   # then open http://localhost:8000
```

Everything is inline except the three webfonts (Faustina, Karla, DM Mono),
which load from Google Fonts. Without network access the page falls back to
Georgia / Helvetica / a system monospace and still lays out correctly.

The page adapts to the visitor's light/dark preference and carries its own
theme toggle, which remembers the choice in `localStorage`.

## Content

Copy is drawn from `docs/proposal.md` and the README. The pipeline stages,
filter names, policies and expected outcomes are read from the real EUC at
`src/main/resources/euc/grant-fit-assessment/grant-fit-assessment.json` — if
that file changes, update section 03 to match.
