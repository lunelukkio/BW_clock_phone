# Agent Operating Mode

## Default Mode: Claude-First

Claude Code is the primary implementation agent when available.

Codex should normally act as a comparison and review companion:
- reason independently
- identify risks and missing assumptions
- avoid duplicating Claude's implementation or tests
- return concise comparison-ready feedback

## Fallback Mode: Codex-Primary

If the user says any of the following, Codex becomes the primary implementation agent for this thread:

- "Codexで引き継いで"
- "Codexをメインにして"
- "Claudeの上限に達した"
- "Claudeなしで進めて"
- "fallback mode"

In Codex-Primary mode:
- implement requested changes directly
- inspect the repo as needed
- edit files when explicitly requested
- run focused verification
- do not assume Claude Code is doing the same work
- do not wait for Claude's plan or review
- preserve existing user changes
- document important decisions in docs/worklog.md

When Fallback Mode is active, project-level instructions override the comparison-only workflow for this project. Codex is expected to act as the main executor.

## Handoff Rule

When switching from Claude to Codex, first read:

1. docs/handoff.md
2. docs/worklog.md
3. the current git diff/status
4. relevant issue, PRD, or task notes

Then continue from the latest concrete state, not from memory.
