---
name: documentation-writer
description: Create or update user-facing technical documentation from implementation details and existing docs. Use when asked to write markdown wiki/module docs, follow a documentation template, preserve legacy pages, fact-check claims against code, and report inconsistencies or migration gaps.
---

# Documentation Writer

## Core workflow

1. Define scope and constraints.

- Extract the requested target location, audience, template/style requirements, and explicit
  do-not-touch files.
- Convert user instructions into an acceptance checklist before writing.

2. Gather authoritative sources.

- Read the documentation template/style guide first:
  `..\mzmine_documentation\docs\contribute_docu_template.md`
- Read current implementation code and parameters before drafting behavior claims.
- Read legacy docs only as examples or migration context.

3. Build a fact map from code.

- List each user-facing claim you plan to document (inputs, outputs, defaults, formulas, errors,
  side effects).
- Attach each claim to a concrete source location (file and line).
- Mark uncertain claims and resolve them before writing.

4. Draft the new documentation page.

- Follow the required template sections and heading structure.
- Prefer concise, user-facing wording; include formulas and decision rules only where they change
  outcomes.
- Add warnings/tips for failure modes, prerequisites, and edge cases.
- Keep content general enough for long-term maintenance; avoid one-off troubleshooting details
  unless requested.

5. Integrate into navigation when needed.

- If the request is for user-facing wiki docs, add/update navigation entries so the page is
  discoverable.
- Do not modify unrelated or protected legacy pages unless explicitly requested.

6. Validate and sanity-check.

- Re-read the final markdown for consistency with the fact map.
- Run docs build/lint if available; if unavailable, report that clearly.
- Check for broken relative links and missing assets referenced by the page.

7. Report results and mismatches.

- Summarize what was created/updated.
- Explicitly report inconsistencies found between legacy docs and current implementation.
- Distinguish confirmed facts from inferred behavior.

## Writing standards

- Use implementation-truth over historical wording.
- Keep parameter descriptions action-oriented: what it controls, valid options, defaults, and
  practical impact.
- Document output artifacts (new columns, stored metadata, generated files, or persisted
  parameters).
- Call out behavior that may surprise users (legacy scaling, interpolation rules, fallback logic,
  hidden persistence).
- Avoid copying outdated statements from older docs without verification.

## Output checklist

Before finishing, confirm:

- Target markdown file was created in the requested location.
- Template/style requirements were followed.
- Legacy files were left unchanged if requested.
- New page is linked in docs navigation when applicable.
- Fact-check findings were reported with concrete references.