# AI tool disclosure

## AI tools used to build the project

| Tool | Used for | How the output was handled |
|---|---|---|
| Claude Code (Anthropic) | Writing and refactoring backend and frontend code, writing tests, drafting documentation, and reviewing changes | Reviewed, run and tested by the team before it was committed. Final design and behaviour decisions were made by the team |

## How the tools were used

- For each feature, a developer described the task, reviewed the plan the assistant proposed and approved or changed it before code was written.
- Generated code was read by a developer, built and tested locally (unit tests and integration tests against PostgreSQL) and merged only through pull requests with passing CI.
- The planning engine's output was cross-checked against the organisers' feasibility checker.

## Work that was not AI-assisted

- The product and screen design (the Designathon Figma file), the scope of each role and the order of the walkthrough.
- Architecture and engineering decisions: the modular monolith, one order service for status changes, the demo clock, schema-first migrations and the offline sync rules.
- Acceptance and testing of every feature by the team on the dev server, and the demo video.

## AI in the running product

None. The application contains no chatbot, no LLM feature, no pre-trained model and no call to an external AI or machine-learning service. The planning engine is rule-based and deterministic, and arrival windows come from a formula.

## Data

The competition dataset is not part of the repository and was not committed. The repository's test data and frontend sample data are made up.
