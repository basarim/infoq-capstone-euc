# Project orientation

**Executable Use Cases (EUC)** — a capstone for the InfoQ AI Engineering
Certification. The research question: can a machine-readable use case provide a
stable link between business intent, AI implementation, and evaluation as an
AI-native application evolves?

`docs/proposal.md` is the authoritative statement of the idea. `site/index.html`
is the public landing page and must agree with it. The Python prototype under
`src/` implements the Grant Fit Assessment case study.

## Writing style — applies to every document, page, and diagram

The author's voice. Match it in `docs/`, `site/`, READMEs, and any new prose.

**Friendly but authoritative, and fact-driven.** Warm enough to read easily,
confident enough to be trusted, and never asserting more than the evidence
supports. Those three pull against each other; when they do, facts win.

### Narrative

- **Take the reader through a story.** Each section should answer a question the
  previous section raised, and end by raising the next one. Comprehension comes
  from the arc, not from completeness of coverage.
- Open concretely — a situation the reader recognises — before naming the
  abstraction.
- Prefer a real example over a hypothetical one.

### Language

- **Avoid technical jargon.** Say what a thing does, not what pattern it
  instantiates. If a term of art is genuinely needed, define it once in plain
  words and then use it consistently.
- Plain words over impressive ones. Short sentences carry weight better.
- Active voice. Name who does what.
- No hype, no marketing register, no exclamation. Confidence comes from
  specificity.

### Facts

- **Cite numbers and attribute them.** "McKinsey reports roughly $3 of
  change-management spend for every $1 of model development" beats "change
  management is expensive."
- Distinguish what is **built** from what is **planned** from what is
  **hypothesised**. Never let the three blur — the status sections exist to keep
  them apart.
- State limitations plainly. A negative result is a legitimate finding here, and
  saying so is part of the argument's credibility.
- Hedge findings, not design decisions. "This may reduce rework" is honest;
  "we might perhaps consider a rule" is weak.

### Diagrams

Diagrams are load-bearing, not decoration. Reach for one whenever a relationship
is easier to see than to read.

- **Object diagrams** for the relationship between an EUC and its parts — this is
  the preferred way to show structure. Show a real named instance with real
  values, not an abstract class box.
- **Context diagrams** for where a thing sits among its neighbours, and
  especially for what it does *not* do.
- **Interaction / sequence diagrams** for behavior over time, including the
  branches that matter.
- JSON or YAML is fine for showing what a sample EUC literally looks like, but it
  supplements the object diagram rather than replacing it.
- Every diagram needs a caption or lead-in saying what to read off it.

In Markdown use Mermaid (GitHub renders it). In `site/index.html` use hand-authored
inline SVG — that file is standalone and has no Mermaid runtime.

## Landing page constraints (`site/index.html`)

- Single self-contained file. No build step, no network calls. The three webfonts
  are embedded as base64 `@font-face` rules; only the family/weight/style
  combinations actually used are embedded.
- Every colour is a token on `:root`, redefined in the
  `@media (prefers-color-scheme: dark)` and `:root[data-theme="dark"]` blocks.
  **Never put a colour literal in a component rule** — it will break one theme.
- Wide content (code blocks, tables, diagrams) scrolls inside its own
  `overflow-x: auto` container. The page body must never scroll sideways.
- Verify in a browser at 360/390/768/1024/1600px before committing.

## Keeping the documents in agreement

`docs/proposal.md`, `site/index.html`, and the committed EUC JSON make claims
about each other. When one changes, check the others:

- The proposal is the source of truth for the concept.
- The landing page must not claim more than the proposal does.
- Any statement about what the code does must be checked against the code.
