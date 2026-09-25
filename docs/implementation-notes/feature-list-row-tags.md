# Feature list row tags

## Intention

Provide every feature-list row with lightweight, user-editable tags shown as compact checkboxes in
the feature table. Each feature list owns the ordered labels used as checkbox tooltips, so projects
retain the meaning and number of their tags.

## Decisions

- A tag is identified by its zero-based position in the feature list's ordered label list. Row
  values use an unbounded bit set, so the preference may grow beyond the four default tags.
- The default labels are `Tag 1` through `Tag 4`. The table shows at least one checkbox per
  configured label and wraps them at the current column width, including after resizing.
- Reducing the label count through the feature-list preferences clears every row's tag bits beyond
  the new count. Renaming labels or increasing the count does not alter existing row values.
- The Tags row type is present and visible on every newly created feature list. Checkbox actions
  update the row immediately, while preference changes use the existing feature-table refresh path.
- Each virtualized table cell owns one reusable checkbox pane. The pane owns and reuses its
  checkboxes, labels, and selection state. If a stored bit set has a set bit beyond the configured
  labels, the cell adds unlabeled checkboxes through that index so editing cannot discard the bit.
  Those checkboxes disappear when the cell receives a value within the configured count.
- Row-type filters expose the same palette-colored checkboxes. The filter bar keeps a compact
  three-column layout for the default tags. When that layout matches the Tags column, it is editable
  directly;
  otherwise hovering opens a popover wrapped to the column's current
  width. The popover's bottom-left corner aligns with the filter's bottom-left corner so it grows
  upward over the table. The compact checkboxes hide
  without releasing their layout space. Click and keyboard activation can also open the popover.
  `Exact` requires the row's whole
  tag set to equal the selection; `Any` requires at least one selected tag to overlap. Presets and
  headless parameters store the selection as comma-separated zero-based indices.
- Tag filters are entirely positional and never depend on tag labels for matching or persistence.
  Labels are only tooltips; a multi-list editor shows enough checkboxes for the largest selected tag
  count. The compact filter preview shows the default number of tags and an overflow count when
  needed.
