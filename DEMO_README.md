# Executable Use Case — Capstone Project

### InfoQ Certified AI Engineering Program · August 2026

The interactive demo is deployed here:

https://euc-capstone-znjes.ondigitalocean.app/#proposal

The application is organized into the following sections:

- **PROPOSAL** — Introduces the capstone proposal, the problem being explored, and the motivation behind the project.
- **EUC** — Provides a brief introduction to the Executable Use Case (EUC), what it represents, and how it can serve as a first-class artifact throughout the AI software development lifecycle.
- **ARCHITECTURE** — Presents the demo architecture and shows how the EUC connects business intent to orchestration, execution, evaluation, and observability.
- **THE DEMO** — Provides an interactive **Grant Fit Assessment** use case that demonstrates how an EUC can define expected behavior, criteria, evidence requirements, and outcomes.
- **EVALUATION** — Shows how AI evaluation can be grounded in the EUC, with evaluation criteria tracing directly back to the business expectations defined by the use case.
- **OBSERVABILITY** — Shows how post-deployment traces can be correlated back to the EUC, allowing production behavior to be observed in the context of the original business intent.

## The Benefits

None of this additional structure is worthwhile unless it improves how AI systems are designed, built, evaluated, and operated — and makes life easier for the people responsible for them after they ship.

That is what a machine-readable use case is intended to provide.

### Shared definition of done

**One artifact spanning the AI SDLC**

An EUC is a first-class, structured, machine-readable artifact that defines the business goal, governing rules and policies, expected behavior, evidence requirements, and success criteria.

It gives business and technical stakeholders a shared artifact for agreeing on what **"done"** means — and carries that agreement forward as the capability moves from design to implementation, evaluation, deployment, and production.

### Thin-slice delivery

**One bounded capability at a time**

Each EUC represents a bounded business capability, supporting agile thin-slice delivery. One meaningful end-to-end use case can be designed, implemented, evaluated, and validated at a time.

This keeps development centered on delivering observable business outcomes rather than accumulating disconnected technical components.

### Coding-agent effectiveness

**Less prompting, more doing**

The EUC's structured format gives coding agents explicit context, constraints, expected behavior, and acceptance criteria.

Instead of repeatedly translating requirements into long prompts, a developer can provide a focused instruction such as **"Implement EUC: Grant Fit Assessment."** The EUC supplies much of the context the agent needs, reducing repeated prompting, back-and-forth, and ambiguity.

### Evaluation grounded in business intent

**Evaluate what the business actually asked for**

Evaluation criteria can be derived from and traced back to the EUC rather than being defined independently after implementation.

This creates a direct relationship between **business intent and AI evaluation**, helping teams answer a more meaningful question than simply whether the model performs well: **is the capability still doing what the business intended?**

### Production traceability

**Carry business context into production**

The EUC does not stop being useful once the capability is deployed. Runtime traces can carry the EUC and criterion identifiers that explain which business capability was executing and what expectations applied.

This connects post-deployment observability back to the same business intent used during design, implementation, and evaluation.

### Bounding probabilistic behavior

**Less surface area for uncertainty**

Generative AI is inherently probabilistic. The EUC does not attempt to make probabilistic AI deterministic.

Instead, it helps minimize the surface area exposed to probabilistic behavior by making business intent explicit, executing deterministically where rules and evidence permit, constraining AI reasoning where semantic interpretation is required, and defining how resulting behavior is verified.

## EUC Across the AI SDLC

Together, these benefits position the EUC as a continuous thread across the AI software development lifecycle:

**business intent → implementation → evaluation → deployment → production observability**

Rather than allowing requirements, implementation, evaluation, and monitoring to become separate artifacts that gradually diverge, the EUC provides a common reference point for keeping them aligned.

As models, prompts, retrieval strategies, agents, and surrounding services evolve, the implementation can change while the underlying business intent remains explicit, traceable, and verifiable.