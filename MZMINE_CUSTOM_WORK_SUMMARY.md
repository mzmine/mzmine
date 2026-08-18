# MZmine Custom Work Summary

Generated: 2026-08-16

Repository: `C:\Users\Robert\Documents\mzmine`

Base branch: `master`

Base commit at time of summary: `925a4ce3d9`

Latest build (post-review, all fixes applied):

`C:\Users\Robert\Documents\mzmine\mzmine-community\build\jpackage\mzmine\mzmine.exe`

This is the build to share. The bundled MSPepSearch runtime ships alongside it under
`build\jpackage\mzmine\external_tools\nist_mspepsearch\`. The earlier `jpackage-*` folders listed
at the bottom of this document predate the review fixes and should not be distributed.

## High-level summary

This MZmine working tree has been customized into a GC-MS/NIST-focused build. The main work areas are:

- Direct import support for Agilent ChemStation `.D` folders.
- Batch-mode selection of multiple folder-based raw-data inputs.
- Headless NIST MSPepSearch EI matching through the local NIST installation.
- Raw Data Overview NIST match display, filtering, labeling, and explicit search.
- Cleaner aligned feature list annotation behavior.
- Chart/export usability improvements.
- Debug logging and tests for the new import and NIST pieces.

The work is on branch `feature/gcms-nist-mspepsearch-and-importers`, rebuilt as a single commit on
top of current upstream `master`, and open as a pull request against `mzmine/mzmine`.

## Direct `.D` folder import

Added native support for importing legacy Agilent ChemStation GC-MS `.D` folders containing `DATA.MS`, instead of forcing conversion through MSConvert.

Main behavior:

- `.D` folders are detected as supported raw data inputs.
- ChemStation `DATA.MS` is parsed directly into MZmine raw scans.
- Scan retention time, nominal m/z values, and intensities are streamed into the project.
- Batch-mode and drag/drop workflows can accept folder inputs.
- Import errors now include a diagnostic log path.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_chemstation/ChemStationMsParser.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_chemstation/ChemStationImportTask.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_chemstation/ChemStationImportLog.java`
- `mzmine-community/src/main/java/io/github/mzmine/util/RawDataFileType.java`
- `mzmine-community/src/main/java/io/github/mzmine/util/RawDataFileTypeDetector.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_all/AllSpectralDataImportModule.java`
- `mzmine-community/src/main/java/io/github/mzmine/parameters/parametertypes/filenames/FileNamesComponent.java`
- `utils/src/main/java/io/github/mzmine/util/files/ExtensionFilters.java`

Tests added:

- `mzmine-community/src/test/java/io/github/mzmine/modules/io/import_rawdata_chemstation/ChemStationMsParserTest.java`

## HAPSITE import

Added a working INFICON HAPSITE importer for `.hps`/GPIR-style records. It is fully wired into
the import pipeline, not just groundwork: `RawDataFileType.INFICON_HAPSITE`, the file-type
detector, `ExtensionFilters`, `MSConvertImportTask` (excluded from conversion), and both task
factories in `AllSpectralDataImportModule`.

Four container layouts are supported: native `HapsScan`, full-scan GPIR, compact GPIR, and the
older 1014-byte GPIR layout. Retention time is recovered by least-squares fitting scan numbers
against `MMmSSs` timestamps scraped from the embedded report, falling back to a per-layout
calibration when the report yields fewer than two usable points.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_hapsite/HapsiteHpsParser.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_hapsite/HapsiteImportTask.java`

Tests added:

- `mzmine-community/src/test/java/io/github/mzmine/modules/io/import_rawdata_hapsite/HapsiteHpsParserTest.java`

## Batch-mode folder handling

Improved file/folder selection so folder-based data sources can be added in batch workflows without only opening one folder at a time.

Main behavior:

- Batch raw-data import can accept multiple selected folders.
- Folder inputs are recognized alongside file inputs.
- Already-loaded raw files can be skipped so batch step 1 does not block rerunning downstream steps.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/parameters/parametertypes/filenames/FileNamesComponent.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_all/AllSpectralDataImportModule.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_msconvert/MSConvertImportTask.java`

## Headless NIST MSPepSearch EI module

Reworked the NIST module into a headless EI search that calls `MSPepSearch64.exe` directly and stores results as spectral library annotations.

This **replaces** the previous GUI-automation module, which drove `nistms$.exe` through its Windows
interface and required the NIST "Automation" option. That code path is gone.

**Breaking change:** the parameter set version moved from 3 to 6. Existing batch files containing a
NIST MS search step will prompt for reconfiguration; both the NIST library path and the new
MSPepSearch directory must be set.

Main behavior:

- Module name is now `NIST MSPepSearch (headless EI)`.
- Searches local licensed NIST `mainlib` and/or `replib`.
- MSPepSearch is NOT redistributed. A "MSPepSearch directory" picker sits next to the NIST
  library picker; point it at a folder extracted from the free NIST download at
  https://chemdata.nist.gov/dokuwiki/doku.php?id=peptidew:mspepsearch
- Stores multiple hits per feature row.
- Stores and parses forward match factor and reverse match factor.
- Keeps NIST library data local to the workstation.
- Uses raw apex fallback if deconvoluted pseudo-spectrum has no qualifying hits.
- Raw apex fallback can be filtered by local baseline prominence percentage.
- Maximum hits and minimum match factor are module parameters.
- Explicit single-RT NIST searches can run from Raw Data Overview.

End-user setup guide, with screenshots: `docs/nist-mspepsearch/README.md`

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchModule.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchParameters.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchTask.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistLibrarySelection.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMatchUtils.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistCommonNameResolver.java`

MSPepSearch runtime (not in version control):

The binaries are gitignored. `external_tools/nist_mspepsearch/README.txt` is tracked and carries
the download link and setup steps. If the runtime is extracted into that folder, packaged builds
still bundle it and the module works with the parameter left empty.

Tests added:

- `mzmine-community/src/test/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchTest.java`

Current NIST regression tests passing:

- `prefersCommonNicotineNameByCas`
- `parsesQuotedTabOutput`
- `acceptsNistRootOrMsSearchDirectory`
- `chartPeakGroupingPrioritizesRetentionTime`
- `parsesReverseMatchFactorFromStoredNistComment`
- `prefersConciseEquivalentNistName`
- `rawApexRetryUsesPercentAboveLocalBaseline`

## NIST result UI and Raw Data Overview

Added a NIST matches view and connected it to Raw Data Overview so stored NIST matches can be browsed and visualized per raw file.

Main behavior:

- NIST matches can be filtered dynamically to the selected raw data file.
- Matches are collapsed by retention time so duplicate candidates for the same peak do not flood the view.
- Each collapsed retention time can expose alternate candidates.
- The best candidate can be selected for display.
- Double-clicking a NIST hit opens spectral match details.
- Preferred concise/common names are shown where available, such as `Nicotine`.
- FMF and RMF are exposed separately.
- Match-score text was removed from the closed Preferred Annotation cell, while dropdown options still show scores.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/gui/MZmineGUI.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/mainwindow/MainWindow.fxml`
- `mzmine-community/src/main/java/io/github/mzmine/gui/mainwindow/MainWindowController.java`
- `mzmine-community/src/main/java/io/github/mzmine/datamodel/features/types/fx/PreferredEditComboCellFactory.java`

## In-chart NIST labels

Added NIST match labels directly on Raw Data Overview chromatograms.

Main behavior:

- Right-click chart toggle: show/hide NIST match labels.
- Toolbar toggle: show/hide NIST labels.
- Labels are off by default.
- Labels refresh immediately when enabled.
- Labels follow the selected raw file.
- Clicking/scrubbing the chromatogram highlights the nearest NIST match.
- Label placement prioritizes retention time so nearby peaks with the same base ion do not collapse onto one peak.
- Labels anchor near the actual chromatogram point instead of sliding to a taller neighboring peak.
- Chart labels show compound name only, not match score.
- Per-match label visibility can be toggled from the NIST match table.
- Per-match label orientation can be changed from the NIST match table.
- A chart-wide `NIST: Vertical` / `NIST: Horizontal` toggle was added.
- Vertical is the default orientation.
- Label font size can be adjusted from the toolbar.
- FMF/RMF display filters control which stored NIST labels are drawn.
- FMF/RMF filters now start at the active NIST cutoff, step by 25, accept typed values, and show a live count of passing labels.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/rawdataoverview/RawDataOverviewWindowController.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogram/TICPlot.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogramandspectra/ChromatogramAndSpectraVisualizer.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/mainwindow/MainWindowController.java`

## Explicit NIST search from a clicked chromatogram peak

Added right-click support in Raw Data Overview to run NIST against the peak at the clicked retention time.

Main behavior:

- Context menu action: run NIST search at clicked peak apex.
- The clicked RT is snapped to the closest local chromatogram apex.
- The nearest processed feature row is found so results can be stored.
- Explicit search validation does not require manually selecting a feature list in the regular NIST batch parameter dialog.
- Results refresh into the NIST match UI and chart labels after completion.

Important file:

- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/rawdataoverview/RawDataOverviewWindowController.java`

## Raw Data Overview toolbar controls

Added a left-side quick-control area to the `Chromatogram parameters` section while leaving the existing XIC controls on the right alone.

Controls added:

- `NIST labels` checkbox.
- `NIST: Vertical` / `NIST: Horizontal` toggle.
- FMF filter.
- RMF filter.
- Base peak / TIC view selector.
- Line color picker.
- Line width spinner.
- Label font size spinner.
- Fit/reset zoom button.
- Export graphics button.
- Live NIST label count.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogramandspectra/ChromatogramAndSpectraVisualizer.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/rawdataoverview/RawDataOverviewWindowController.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogram/TICPlot.java`

## Chart and graphics export fixes

Improved chart usability and export-preview behavior.

Main behavior:

- Double-click reset now restores the chromatogram Y-axis to zero.
- Graphics Export preview updates more reliably after changing settings.
- Chart theme controls such as background, line width, and label/font settings are more responsive in the preview.
- The Raw Data Overview toolbar now provides a direct Export button.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/graphicsexport/GraphicsExportDialogFX.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/EChartViewer.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/model/PlotCursorUtils.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogram/TICPlot.java`

## Feature list and annotation UI

Improved aligned feature list behavior for annotation review.

Main behavior:

- Right-click NIST search from aligned feature list was adjusted to respect the selected row.
- NIST matches and local compound database matches can be viewed as separate annotation sources.
- Preferred Annotation is treated as the editable final user-facing assignment.
- Closed Preferred Annotation cells show the chosen name without appending the match score.
- Dropdown candidates still show scores so alternatives can be compared.
- The column chooser `+` button was widened to a 32 px click target and given a tooltip.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/featurelisttable_modular/FeatureTableContextMenu.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/featurelisttable_modular/TableColumnMenuHelper.java`
- `mzmine-community/src/main/java/io/github/mzmine/datamodel/features/types/fx/PreferredEditComboCellFactory.java`

## Diagnostic logging

Added more logging around `.D`/ChemStation imports and NIST search decisions so failures can be diagnosed after the fact.

All of it goes through `java.util.logging`, so it lands in the standard mzmine log and follows the
normal log-level configuration. Routine ChemStation progress events are logged at `FINE`; failures
at `WARNING`. There is no separate log file.

Examples:

- ChemStation import start, parser-opened, success, cancellation, no-scan, and error events.
- NIST row searches and hit counts.
- Raw-apex fallback attempts.
- Raw-apex fallback skips caused by the baseline prominence filter.
- Explicit NIST search selected RT, clicked RT, and snapped apex RT.

Important files:

- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_chemstation/ChemStationImportLog.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchTask.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/rawdataoverview/RawDataOverviewWindowController.java`

## Code review and fixes

A full review of the changeset was run on 2026-08-16. Twelve findings were raised and all twelve
are fixed. The substantive ones:

- **Batch rerun dropped metadata import and recoloring.** The "all files already loaded" early
  return in `AllSpectralDataImportModule` skipped creation of `AllSpectralDataImportMainTask`,
  which is the only thing that imports the configured metadata file and recolors blanks/QCs.
  The early return is gone; `BatchTask.setLastFilesIfAllDataImportStep` already provides the
  rerunnable-batch behavior on the normal path.
- **Silent zero-hit NIST searches.** If MSPepSearch emits `/OUTTAB` column titles other than
  `Rank`/`MF`/`Name`, no header is recognized and every hit is dropped. That was indistinguishable
  from "nothing scored above MinMF". It now logs an explicit warning naming the expected columns.
- **NIST chart-label state now lives in `NistChartLabelState`.** Previously a getter defaulted to
  "visible" while its setter silently did nothing when no Raw Data Overview window was open, so
  label checkboxes in the NIST matches table discarded every change. The shared holder also means
  a change repaints *every* open overview window, not just whichever registered last.
- **HAPSITE retention times with a single report peak.** A one-point fit is not a calibration, but
  the guard only rejected zero points, so those files got an uncalibrated 1 s/scan slope instead of
  the layout's known calibration. All uncalibrated fits now report zero usable points.
- **A stray `.ms` file could divert a modern Agilent `.d` to the legacy parser.** The ChemStation
  header probe is deliberately permissive; it now runs only after `AcqData`, Bruker and Waters
  layouts have been ruled out. The conventional `DATA.MS` name still takes priority.
- **Per-row NIST failures no longer abort the run.** A timeout or unreadable row is logged and
  skipped; the remaining rows still search. Cancellation reports `CANCELED` rather than `ERROR`.
- **Temp-directory cleanup no longer throws from `finally`**, which could mask the real result or
  the original exception.
- ChemStation diagnostics moved from an unbounded `~/mzmine_chemstation_import.log` to
  `java.util.logging`.

Known limitation, not a defect: MSPepSearch's tabular output carries scores but no peak list, so
NIST annotations have an empty library spectrum. Mirror plots will show nothing on the library
side. The annotation comment records why. Fabricating peaks from the query spectrum would
manufacture a perfect self-match, so the arrays stay genuinely empty.

## Latest verification run

```powershell
.\gradlew.bat :mzmine-community:compileJava :mzmine-community:test --tests "io.github.mzmine.modules.io.import_rawdata_chemstation.*" --tests "io.github.mzmine.modules.io.import_rawdata_hapsite.*" --tests "io.github.mzmine.modules.dataprocessing.id_nist.*" :mzmine-community:jpackageImage --console=plain
```

Result: `BUILD SUCCESSFUL`, **24 tests, 0 failures, 0 errors, 0 skipped** across all three test
classes. Four of those tests are new regression tests covering the fixes above.

- `NistMsSearchTest` — 7 tests
- `ChemStationMsParserTest` — 10 tests (2 new: AcqData precedence, renamed payload)
- `HapsiteHpsParserTest` — 7 tests (2 new: one-point and two-point report time fits)

Note that before this run the ChemStation and HAPSITE test classes had never actually been
executed; only the NIST result XML existed.

## Packaged test builds created

These are the jpackage folders created while iterating:

- `jpackage-aligned-nist-mapping`
- `jpackage-batch-folders`
- `jpackage-chromatogram-toolbar`
- `jpackage-chromatogram-toolbar-refresh`
- `jpackage-clicked-apex`
- `jpackage-common-names`
- `jpackage-dynamic-nist`
- `jpackage-folder-checklist`
- `jpackage-graphics-live-preview`
- `jpackage-graphics-preview`
- `jpackage-import-logging`
- `jpackage-nist-aligned-row-search`
- `jpackage-nist-apex-fallback`
- `jpackage-nist-baseline-filter`
- `jpackage-nist-clean-labels`
- `jpackage-nist-consolidated`
- `jpackage-nist-explicit-fixed`
- `jpackage-nist-explicit-rt`
- `jpackage-nist-explicit-rt-priority`
- `jpackage-nist-explicit-validation`
- `jpackage-nist-filter-controls`
- `jpackage-nist-interactive`
- `jpackage-nist-label-controls`
- `jpackage-nist-label-font13`
- `jpackage-nist-label-repaint`
- `jpackage-nist-matches`
- `jpackage-nist-orientation-controls`
- `jpackage-nist-orientation-toggle`
- `jpackage-nist-rt-dropdown`
- `jpackage-nist-rt-label-anchor`
- `jpackage-nist-rt-labels`
- `jpackage-nist-selected-scan`
- `jpackage-preferred-name-only`
- `jpackage-rerunnable-batch`
- `jpackage-reset-zero`
- `jpackage-scrub`
- `jpackage-wide-column-button`

All of the folders above predate the code-review fixes. The current build is
`mzmine-community\build\jpackage\mzmine\mzmine.exe`; these can be deleted.

## Current tracked files changed

- `mzmine-community/src/main/java/io/github/mzmine/datamodel/features/types/fx/PreferredEditComboCellFactory.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/MZmineGUI.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/graphicsexport/GraphicsExportDialogFX.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/EChartViewer.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/chartbasics/gui/javafx/model/PlotCursorUtils.java`
- `mzmine-community/src/main/java/io/github/mzmine/gui/mainwindow/MainMenu.fxml`
- `mzmine-community/src/main/java/io/github/mzmine/gui/mainwindow/MainWindow.fxml`
- `mzmine-community/src/main/java/io/github/mzmine/gui/mainwindow/MainWindowController.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchModule.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchParameters.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMsSearchTask.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_all/AllSpectralDataImportModule.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_msconvert/MSConvertImportTask.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogram/TICPlot.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/chromatogramandspectra/ChromatogramAndSpectraVisualizer.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/featurelisttable_modular/FeatureTableContextMenu.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/featurelisttable_modular/TableColumnMenuHelper.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/rawdataoverview/RawDataOverviewWindowController.java`
- `mzmine-community/src/main/java/io/github/mzmine/parameters/parametertypes/filenames/FileNamesComponent.java`
- `mzmine-community/src/main/java/io/github/mzmine/util/RawDataFileType.java`
- `mzmine-community/src/main/java/io/github/mzmine/util/RawDataFileTypeDetector.java`
- `utils/src/main/java/io/github/mzmine/util/files/ExtensionFilters.java`

## Current untracked additions

- `external_tools/nist_mspepsearch/`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistCommonNameResolver.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistLibrarySelection.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/id_nist/NistMatchUtils.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/visualization/rawdataoverview/NistChartLabelState.java`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_chemstation/`
- `mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_hapsite/`
- `mzmine-community/src/test/java/io/github/mzmine/modules/dataprocessing/id_nist/`
- `mzmine-community/src/test/java/io/github/mzmine/modules/io/import_rawdata_chemstation/`
- `mzmine-community/src/test/java/io/github/mzmine/modules/io/import_rawdata_hapsite/`

## Before committing

- The MSPepSearch binaries are gitignored and are not in the branch history. Only the README with
  download instructions is tracked.
- Building requires **JDK 26** as of the current upstream master (was 25).

## Notes and follow-up ideas

- The work is currently local and uncommitted.
- The latest build is intended as the one to test first.
- The NIST MSPepSearch executable is bundled, but licensed `mainlib`/`replib` still come from the local NIST install, usually `C:\NIST23\MSSEARCH`.
- The NIST labels are intentionally off by default in Raw Data Overview.
- Batch relabeling is still best done from the aligned feature list / Preferred Annotation workflow.
- A future pass could consolidate the many jpackage test folders after the final build is chosen.
