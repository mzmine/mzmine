---
name: feature-implementation-notes
description: Create or update a concise developer implementation note when implementing a new MZmine feature or processing module. Use for feature and module implementation work; skip isolated bug fixes, refactors, tests, and documentation-only changes.
---

# Feature implementation notes

When implementing a new feature or processing module, record the intent and consequential design
decisions in `docs/implementation-notes/` before finishing the task.

- Search the folder for an existing note about the feature and update it instead of creating a
  duplicate.
- Name new files with a stable, descriptive kebab-case name.
- Keep the note concise and focused on information that future implementers cannot easily recover
  from the code.
- Preserve decisions explicitly made by the user, including behavior differences between GUI and
  headless execution, compatibility constraints, inference rules, and important edge cases.
- Update the note when implementation changes invalidate an earlier decision.
- Do not turn the note into a changelog, task diary, code walkthrough, or list of speculative future
  work.

Use this structure:

```markdown
# Feature name

## Intention

State the problem and intended outcome.

## Decisions

- Record the important behavioral and architectural decisions.
- Explain non-obvious boundaries and fallback behavior.
```

Add another short section only when it preserves an important implementation contract that does not
fit under the two standard headings.
