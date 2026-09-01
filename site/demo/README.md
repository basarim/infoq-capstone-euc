# Interactive demo

A self-contained companion to `../index.html` — the proposal, the EUC, the
architecture, an interactive replay of all six golden-set cases, the
evaluation-criteria mapping, and one real captured Langfuse trace, all in
one file. No build step, no dependencies.

```bash
python3 -m http.server -d site/demo 8000   # then open http://localhost:8000
```

## No live calls, no secrets

Nothing on this page calls a network endpoint at runtime — the six-case
replay, the DeepEval panel, and the evaluation scoring are all computed
client-side in JS, reproducing the reference implementation's logic rather
than calling it. The one Langfuse trace shown under "See sample trace" is
static data, pasted in verbatim from a real run captured earlier and
fetched back via the Langfuse API — not a live query.

The only credential-shaped string in the file is a Langfuse **public** key
(`pk-lf-...`), inside that same pasted trace's metadata. Public keys are
non-secret by design — Langfuse's own SDK requires the secret key as well
to authenticate any read or write, so this key alone grants no access.
Nothing else in this file — no `ANTHROPIC_API_KEY`, no `LANGFUSE_SECRET_KEY`
— appears anywhere. If that's ever untrue after an edit, it's a bug: run
this before publishing:

```bash
grep -niE "sk-ant|sk-lf-|ANTHROPIC_API_KEY|LANGFUSE_SECRET_KEY|secret_key|Bearer [A-Za-z0-9]" site/demo/index.html
```

An empty result is the only acceptable one.

## Theme and structure

Same token system and dark/light handling as `../index.html` — every colour
is a `:root` custom property, redefined in the dark-mode blocks. Tabs are
real `<a href="#name">` elements so each one opens correctly in a new tab;
`showTab()` keeps the visible panel and the URL hash in sync either way.
