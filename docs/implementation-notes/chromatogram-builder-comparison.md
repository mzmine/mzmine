# Chromatogram builder comparison

## Intention

Developers compare the chromatograms of two builders, e.g., ADAP and the fast chromatogram builder,
on the same raw data file. The module (Visualization > Feature list > Chromatogram builder
comparison) opens a tab that shows the chromatograms of both lists side by side, groups the
chromatograms of both lists by m/z and flags potential issues in a sortable table, so that missing
data points, holes and chromatograms of only one list are quick to find.

## Decisions

- The parameters are exactly two feature lists: the first is A, the second B, the view can swap
  them. The lists need a raw data file in common, the first shared file is compared.
- A group is a connected set of chromatograms: each chromatogram connects to the chromatogram of
  the other list with the closest m/z within the tolerance. Split signals or duplicates therefore
  join the group of their closest partner. There is no retention time condition, both builders
  build one chromatogram per m/z channel over the whole run. Dense m/z regions can chain a few
  chromatograms into one group.
- Several chromatograms of one side are a split if two never share a scan and one has a data point
  within 4 scans of the apex of the other, at least the apex intensity / 5: one ion in several
  chromatograms, the merge rule of the fast builder. Otherwise they are multiple: separate peaks or
  co-eluting ions that the other list has in one chromatogram. On a QE QC file, most "multiple in
  B" were peaks at other retention times that ADAP put into one wide chromatogram.
- A chromatogram of only one list that fails the filter of the other builder (no segment of the
  min consecutive scans above the min group intensity reaches the min height) is "only A, low
  segment". It is expected: ADAP checks the min height anywhere in the chromatogram, the fast
  builder within the segment. The filters come from the applied methods of the lists, and 100 of
  108 "only A" of the QC file were such chromatograms. "Unexpected issues" hides them.
- "Taken by" names the chromatograms of the same list that hold the data points a side misses:
  signals of the other side in scans without own data point, data points of the mass lists in
  holes, and all signals of the other side if the side has no chromatogram. "Unused" data points
  are in no chromatogram of that list. This separates a lost signal from one taken by a neighbor
  or split off. A double click selects the group of the first chromatogram if the filter shows
  it.
- Missing data points are "taken" instead of "missing", a low severity after "multiple", if other
  chromatograms of the list hold all of them and one holds the most intense in a run of at least
  the min consecutive scans of the builder (5 if the filter is unknown). The resolver can resolve
  the peak there: the fast builder joins a separate peak that ends right before an intense ion 1
  to 2 tolerances away into the channel of the ion, and the resolver separates both again (few
  features lost in `ChromatogramBuilderBenchmark`, see the fast builder note). Rejected: all
  missing data points taken as the only condition, it would hide the apex of an ion that a
  co-eluting side chromatogram takes in one scan, an earlier defect of the fast builder. A
  feature level check would need a resolver in the tool.
- The m/z tolerance defaults to the larger tolerance of both builders from the applied methods
  (the fast builder stores the estimate for auto), otherwise 0.002 m/z or 10 ppm. The signal
  threshold defaults to the larger minimum absolute height of both builders, otherwise 0. Both are
  editable in the view and trigger a new comparison.
- Only signals (data points of at least the threshold) count: a side misses a scan if the other
  side has a signal there. A side is the union of all its chromatograms in the group, the highest
  data point per scan, so a split signal does not miss the data points of its other part.
- The scans are the union of the selected scans of both lists (all MS1 scans if a list has none).
  A scan only counts as missing or as a hole for a side whose builder used it.
- A hole is a gap of up to 5 scans between two signals of the same side, and only scans whose mass
  list has a data point within the tolerance of the flank m/z count. Without this check a sparse raw
  signal produced the same holes in both lists, which says nothing about the builders.
- Different data points: both sides have a data point in the same scan with a different m/z, one of
  them a signal. Both builders copy the data points of the same mass lists, so the same data point
  has the same m/z.
- The table sorts by severity by default: only in one list, missing, split, holes, multiple,
  taken, different data points, the expected low segment last. Within one severity the most intense
  groups come first, their issues are the most reliable.
- Both charts are in a `ChartGroup` that combines the zoom of both axes. The overlay option shows
  both lists in the first chart and hides the second. Selecting a group sets both axes to its
  signals directly; `ChartGroup.resetZoom()` fails if a chart of the group is empty (only one list
  has the chromatogram, or the overlay): the auto range of the empty chart resets the domain of the
  other chart, no intensity range is found and `applyRange(null)` throws.
- Single missing scans are value markers, consecutive ones interval markers with an outline that
  keeps narrow regions visible, both in the background in a soft color of the side. Scans where the
  other side has a signal are more opaque than holes that neither side fills.
- The tab keeps a copy of the detected data points of all chromatograms of both lists while it is
  open.
