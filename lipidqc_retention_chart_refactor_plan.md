# LipidQC Retention Chart Refactor Plan

## Scope

This note captures the planned refactor of
`io.github.mzmine.modules.visualization.dash_lipidqc.retention.EquivalentCarbonNumberPane`
to:

- use one persistent `SimpleXYChart<PlotXYDataProvider>` instance instead of constructing
  per-mode charts,
- model each visual mode through chart specs and provider-backed datasets,
- split currently multi-series selectable datasets into separate single-series datasets,
- keep combined carbon/DBE mode on manual selection during phase 1,
- extend cursor selection in phase 2 so dataset-mapped range axes are handled correctly.

The current relevant code lives mainly in:

-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/EquivalentCarbonNumberPane.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionComputationTask.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/simplechart/SimpleXYChart.java`
-
`mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/model/PlotCursorUtils.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/RenderedValueAxis.java`
-
`mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/model/PlotCursorConfigModel.java`

## Goals

- Reduce duplication in chart construction across retention modes.
- Align the pane with `SimpleXYChart` and provider-based dataset infrastructure.
- Make cursor-driven point selection work for all single-axis and split-series modes.
- Defer the multi-range-axis cursor upgrade to a second isolated phase.
- Keep one visible cursor marker even for the two-range-axis combined mode.

## Constraints And Decisions

- Use one provider class and vary behavior by renderer/spec, not by creating multiple provider
  classes.
- Do not add `rangeAxisIndex` to `PlotCursorPosition` unless phase 2 proves it is necessary.
- For cursor opt-out, use provider policy rather than a hardcoded allowlist in cursor logic.
- Keep combined mode manual in phase 1.
- Keep only one visible cursor marker in plots.

## Target Architecture

### Main Pane

`EquivalentCarbonNumberPane` should own:

- the option controls already present,
- one persistent `SimpleXYChart<PlotXYDataProvider>`,
- one `RetentionChartController`,
- scheduling and forwarding of `RetentionComputationResult`.

It should stop creating per-mode `EChartViewer` instances.

### Controller Layer

Add a controller that translates neutral computation results into chart state:

- `RetentionChartController`
- responsibilities:
    - reset chart state,
    - apply axis configuration,
    - build datasets and renderers from chart specs,
    - install selection handling,
    - update title text,
    - manage overlays and selected-point highlighting,
    - keep combined-mode manual selection until phase 2.

### Spec Layer

Add small immutable spec types:

- `RetentionChartSpec`
- `RetentionAxisSpec`
- `RetentionDatasetSpec`
- `RetentionDatasetRole`

Suggested roles:

- `PRIMARY_POINTS`
- `REGRESSION`
- `FALSE_POSITIVE`
- `FALSE_NEGATIVE`
- `SELECTED_POINT`

`RetentionDatasetSpec` should carry:

- dataset/provider instance,
- renderer,
- mapped range axis index,
- dataset role,
- cursor-selectable flag or equivalent provider-driven policy.

### Provider Layer

Add one provider class only:

- `RetentionPointProvider implements PlotXYDataProvider, XYItemObjectProvider<RetentionPointRef>`

Add one item record:

- `RetentionPointRef`
- suggested fields:
    - `FeatureListRow row`
    - `MatchedLipid match`
    - `double x`
    - `double y`
    - `String label`
    - `String tooltip`

`RetentionPointProvider` should be configurable for:

- series key,
- color,
- labels/tooltips,
- selectability,
- displayed point role.

Regression and overlay datasets should use the same provider class, but be configured as
non-selectable.

## Phase 1: Shared Chart Refactor

### 1. Introduce The Persistent Shared Chart

Update `EquivalentCarbonNumberPane` to:

- create one `SimpleXYChart<PlotXYDataProvider>` in the constructor,
- set it as the center node when chart data is available,
- reuse it across all mode updates,
- stop replacing the whole chart node in `applyComputationResult`.

Expected code impact:

- remove `setCenter(chart)` branching from per-mode methods,
- centralize chart setup and reuse.

### 2. Add The Chart Controller

Create `RetentionChartController` in the retention package.

Responsibilities:

- `apply(RetentionComputationResult result)`
- `applyPlaceholder(String text)`
- `applySpec(RetentionChartSpec spec)`
- `resetChart()`
- `installManualCombinedSelection()` for phase 1
- shared highlight and overlay application

The pane should delegate visual updates to this controller rather than constructing charts itself.

### 3. Add Chart Spec Classes

Create immutable records/classes:

- `RetentionChartSpec`
- `RetentionAxisSpec`
- `RetentionDatasetSpec`
- `RetentionDatasetRole`

`RetentionChartSpec` should capture:

- title text,
- domain axis label,
- one or more range axis specs,
- ordered dataset specs,
- optional selected-point axis synchronizer info,
- counts/flags needed for subtitle or title suffix generation.

### 4. Move From Chart-Specific Datasets To Neutral Payload Data

Adapt the sealed payload records in the retention package so they carry neutral point-series data
instead of chart-viewer-ready datasets whenever practical.

Keep the same payload type split:

- `EcnRetentionPayload`
- `DbeRetentionPayload`
- `CombinedRetentionPayload`
- `TotalLipidClassRetentionPayload`

But change their contents toward:

- one or more logical point-series descriptors,
- selected values,
- selected class/match metadata,
- axis metadata,
- precomputed counts needed for UI text.

### 5. Refactor RetentionComputationTask To Produce Series Data

In `RetentionComputationTask`:

- keep the existing mode selection and validation logic,
- replace direct dependence on viewer-facing datasets where possible,
- build lists of logical point records or small series records for each mode,
- keep combined-mode axis semantics explicit in the payload.

This step should not yet change cursor infrastructure.

### 6. Add The Single Provider Class

Create `RetentionPointProvider`.

Requirements:

- implements `PlotXYDataProvider`,
- implements `XYItemObjectProvider<RetentionPointRef>`,
- exposes one logical series only,
- can return labels and tooltips per point,
- holds a configurable series key and color,
- supports later cursor opt-out policy.

This provider should become the common representation for all plotted XY data in this pane.

### 7. Build Mode-Specific Specs And Split Total-Class Series Together

Create a builder/factory such as `RetentionChartSpecFactory` with methods:

- `buildEcnSpec(...)`
- `buildDbeSpec(...)`
- `buildCombinedSpec(...)`
- `buildTotalClassSpec(...)`

These methods should:

- translate payload data into `RetentionPointProvider` instances,
- create matching renderers,
- create axis specs,
- create overlay dataset specs,
- configure regression datasets and selected-point datasets,
- preserve title suffix logic and FP/FN counts,
- split total-class multi-series data during spec construction rather than as a later separate
  step.

For total-class mode specifically:

- stop using one selectable multi-series dataset,
- turn each logical trend line into one `RetentionPointProvider`,
- map each provider to one chart dataset,
- give each dataset its own color and renderer,
- keep all of them on the same range axis.

This is the key phase-1 change that makes cursor-based selection feasible for total-class mode
without extending `PlotCursorPosition`.

### 8. Keep Combined Mode Manual In Phase 1

Combined carbon/DBE mode should:

- still use the shared `SimpleXYChart`,
- still configure two range axes on `chart.getXYPlot()`,
- still map datasets to those axes,
- still use `SelectedPointAxisSynchronizer`,
- continue to use manual selection logic in phase 1.

Do not move combined mode onto cursor-based selection yet.

### 9. Reuse One Provider For Overlays And Regression Datasets

Keep regression lines and overlays on the same provider model.

Implementation direction:

- regression provider instances produce only the two line endpoints,
- FP/FN overlay provider instances produce outlined points,
- selected-point provider instances produce one highlighted point.

Selection policy will be disabled for those provider instances in phase 2.

### 10. Keep Existing Helper Logic Until The Shared Path Is Stable

It is acceptable in phase 1 to temporarily keep:

- axis range helper methods,
- title formatting helpers,
- selected-point highlight helpers,
- false-positive and false-negative detection helpers.

Move or consolidate them only after the new shared chart path is working.

### 11. Validation For Phase 1

Compile:

- `:mzmine-community:compileJava`

Manual UI checks:

- switching between all retention modes,
- title updates and counts,
- ECN point selection,
- DBE point selection,
- total-class selection on split datasets,
- selected-point highlighting,
- false-positive and false-negative overlays,
- combined mode still selecting rows correctly through manual handling.

## Phase 2: Cursor Infrastructure Upgrade

### 1. Add Dataset-Based Axis Resolution Helpers

Extend `RenderedValueAxis` with dataset-based helpers:

- `domainOfDataset(XYPlot plot, XYDataset dataset)`
- `rangeOfDataset(XYPlot plot, XYDataset dataset)`

These methods should:

- accept the dataset object, not dataset index,
- resolve the index internally via `plot.indexOf(dataset)` if needed,
- resolve the axis mapped to that dataset,
- fall back to the plot default axis if no mapping exists.

This keeps the public helper API dataset-oriented and avoids leaking plot indexing concerns.

### 2. Update PlotCursorUtils To Be Dataset-Axis-Aware

Refactor cursor hit-testing in `PlotCursorUtils` to:

- iterate all datasets in the plot,
- iterate all series defensively,
- resolve the domain and range axes for the current dataset through the new
  `RenderedValueAxis` helpers,
- compute screen distance using those dataset-mapped axes,
- choose the closest selectable data point.

This is the core change needed for dual-range-axis charts.

### 3. Add Cursor Selectability Policy To PlotXYDataProvider

Add a default method to `PlotXYDataProvider`:

```java
default boolean isCursorSelectable() {
  return true;
}
```

Then:

- `RetentionPointProvider` returns `true` for primary point datasets,
- returns `false` for regression and overlay datasets.

This supports the "one provider class" requirement while allowing instance-level policy.

### 4. Forward The Provider Policy Through ColoredXYDataset

Add a delegating method on `ColoredXYDataset`, for example:

```java
public boolean isCursorSelectable()
```

Implementation:

- if the wrapped provider is a `PlotXYDataProvider`, delegate to `provider.isCursorSelectable()`,
- otherwise default to `true`.

Then `PlotCursorUtils` can skip datasets that are provider-backed and marked non-selectable.

### 5. Keep PlotCursorPosition Unchanged Unless Testing Proves Otherwise

Do not add `rangeAxisIndex` to `PlotCursorPosition` initially.

Rationale:

- phase 1 makes selectable datasets single-series,
- phase 2 will be able to resolve dataset-mapped axes from the plot and dataset object,
- the selected dataset should be enough context for the current pane use case.

Revisit only if later requirements need cursor state to remain self-contained outside plot context.

### 6. Keep One Visible Cursor Marker

Do not introduce one range marker per axis.

Instead:

- keep one domain marker,
- keep one range marker,
- when the active selection belongs to a dataset on the secondary range axis, move the single
  range marker to that axis context.

The dual-range-axis case should remain special handling rather than becoming the default marker
model for all charts.

### 7. Switch Combined Mode To Cursor-Based Selection

After dataset-aware cursor hit-testing works:

- remove the combined-mode manual click selection path,
- use the shared cursor-driven selection path already used by the other modes,
- keep `SelectedPointAxisSynchronizer` for zoom behavior.

### 8. Validation For Phase 2

Compile:

- `:mzmine-community:compileJava`

Manual UI checks:

- combined-mode selection on carbon axis,
- combined-mode selection on DBE axis,
- correct row resolution from cursor-based selection,
- cursor ignoring regression and overlay datasets,
- single visible marker behavior,
- zoom interactions after a selected point is present.

## Suggested File-Level Work Breakdown

Likely new files:

-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionChartController.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionChartSpec.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionAxisSpec.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionDatasetSpec.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionDatasetRole.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionPointProvider.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionPointRef.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionChartSpecFactory.java`

Likely modified files:

-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/EquivalentCarbonNumberPane.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/RetentionComputationTask.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/EcnRetentionPayload.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/DbeRetentionPayload.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/CombinedRetentionPayload.java`
-
`mzmine-community/src/main/java/io/github/mzmine/modules/visualization/dash_lipidqc/retention/TotalLipidClassRetentionPayload.java`
-
`mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/simplechart/providers/PlotXYDataProvider.java`
-
`mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/simplechart/datasets/ColoredXYDataset.java`
-
`mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/model/PlotCursorUtils.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/RenderedValueAxis.java`
-
`mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/model/PlotCursorConfigModel.java`

## Decision Summary

### Point 1: One provider class

Accepted.

The plan uses one `RetentionPointProvider` and varies renderer/spec/config only.

### Point 2: Avoid adapting PlotCursorPosition if possible

Accepted for the initial implementation.

The plan avoids adding `rangeAxisIndex` to `PlotCursorPosition` and instead uses dataset-based
axis lookup from `RenderedValueAxis` and `PlotCursorUtils`.

### Point 3: Cursor opt-out policy

Accepted with a provider-level default method instead of a pure marker.

The proposed approach is:

- add `isCursorSelectable()` to `PlotXYDataProvider`,
- default to `true`,
- let provider instances opt out by returning `false`.

This works better than a pure empty marker because selectability must vary per instance while using
only one provider class.

### Point 4: Keep the plan separated into two steps

Accepted.

Phase 1 performs the shared chart refactor and keeps combined selection manual.
Phase 2 upgrades cursor infrastructure and then switches combined mode over.

### Point 5: Keep only one marker

Accepted.

The plan keeps one visible range marker and treats the second range axis as a special case for
marker relocation rather than introducing multiple markers.

## Implementation Notes

- Prefer small neutral records for payload content rather than passing chart-ready datasets through
  the computation layer.
- Preserve existing helper logic until the new controller/spec path is stable, then collapse
  duplicates.
- Keep overlays ordered after primary datasets so manual and cursor selection stay predictable.
- Be cautious when converting total-class mode: the split-dataset change affects coloring, legend
  behavior, and selection logic at the same time.
