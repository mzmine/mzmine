# Feature list row tags

## Intention

Provide every feature-list row with lightweight, user-editable tags shown as compact checkboxes in
the feature table. Each feature list owns the ordered labels used as checkbox tooltips, so projects
retain the meaning and number of their tags.

## Decisions

- A tag is identified by its zero-based position in the feature list's ordered label list. Row
  values use an unbounded bit set, so the preference may grow beyond the six default tags.
- The label list size is the checkbox count. The default labels are `Tag 1` through `Tag 6`, shown
  in a three-column by two-row grid; additional labels add further rows.
- Reducing the label count through the feature-list preferences clears every row's tag bits beyond
  the new count. Renaming labels or increasing the count does not alter existing row values.
- The Tags row type is present and visible on every newly created feature list. Checkbox actions
  update the row immediately, while preference changes use the existing feature-table refresh path.
- Each virtualized table cell owns one reusable checkbox grid. Checkbox selection is bound to
  reusable Boolean properties; ordinary item updates only synchronize those properties, while UI
  controls are added or removed solely when the configured label count changes.
- Row-type filters expose the same palette-colored checkbox grid. `Exact` requires the row's whole
  tag set to equal the selection; `Any` requires at least one selected tag to overlap. Presets and
  headless parameters store the selection as comma-separated zero-based indices.
- Tag filters are entirely positional and never depend on tag labels. Filter editors use only the
  configured tag count and identify checkboxes by zero-based index; a multi-list editor shows enough
  checkboxes for the largest selected tag count.
