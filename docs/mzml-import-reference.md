# mzML import reference

This reference records the importer at `91767f75ee`, the vendor cases behind its behavior, and
the regression tests for its replacement. Source links in the historical sections are pinned
to that revision.

## Replacement outcome

Ordinary files, archive entries and converter stdout now use one forward-only StAX reader.
The parser accepts any `XMLStreamReader`. Aalto, Woodstox and the JDK produced identical
per-file scan, peak and precursor fingerprints on all 25 supplied Thermo files. The SCIEX
file and stream also matched the original reader. Frame selection retains all bins on
mobility-range overlap. Axis padding preserves peak pairs and precursor ranges for missing
bins. The simultaneous MS2 isolation case remains deferred pending a data-model change.

The performance comparison covers LC/MS, advanced import, mapped individual and combined
IMS, detector traces and mixed UV/MS. It includes the observed timing outliers, sampling
limits, provider selection and the distinction between a replayed stream and a real vendor
converter. [Implementation, benchmarks and verification](mzml-import-implementation-plan.md#implementation-and-verification)

## Entry points and ownership

| Caller               | Input and result contract                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Direct mzML import   | Standard and advanced import both use `MSDKmzMLImportTask`. The reader handles ordinary and indexed mzML sequentially and determines LC/MS versus IMS from the retained spectra. imzML has its own task. [Import dispatch][dispatch]                                                                                                                                                                                                                                                                                              |
| MSConvert            | Reads converter stdout or an existing or newly converted mzML file. The stdout path skips a converter preamble when supported by the stream and keeps the vendor path as the project identity. The disk path uses the converted mzML path and updates the applied parameters. Reusing an existing conversion must avoid duplicate imports. [MSConvert task][msconvert], [duplicate import fix](https://github.com/mzmine/mzmine/commit/12c4d5e505), [existing conversion fix](https://github.com/mzmine/mzmine/commit/aca6b0bf9c) |
| Thermo RAW           | Uses ThermoRawFileParser stdout with the original RAW path. It controls converter cancellation and checks declared versus parsed spectra before publishing the file. MSConvert applies a similar completeness check. [Thermo task][thermo], [delegation change](https://github.com/mzmine/mzmine/commit/7f703e111d)                                                                                                                                                                                                               |
| ZIP and GZIP         | Reads the first ZIP entry or the GZIP decompressor stream, retaining the archive path and format metadata. Advanced archive import uses the post-import mass-detection wrapper. [Archive task][zip], [advanced dispatch][dispatch]                                                                                                                                                                                                                                                                                                |
| Other direct callers | `RawDataFileUtils.createRawDataImportTasks` (used by mzTab-M import to load referenced raw files) and the legacy `MSDKmzMLImportModule` construct the file task with the default config: m/z sorting only, no filter. [Utility dispatch][raw-utils], [legacy module][legacy-module]                                                                                                                                                                                                                                               |

Thermo rejects a stream with zero parsed spectra even when it carries chromatograms, while
MSConvert accepts chromatogram-only output. A TSQ MRM RAW file therefore imports through MSConvert
but not through ThermoRawFileParser. [Thermo task][thermo]

`importStreamOrFile()` builds the file. The caller adds applied methods and publishes it to the
project after its own checks. The direct task also sets task status. Keep source identity, input
ownership, and publication explicit. Cancellation is checked in the XML event loop and output
conversion. The declared spectrum count, parsed XML spectrum count before filtering, and converted
count after filtering have different meanings. The converted count for IMS counts frames. The
current Aalto file path explicitly decodes UTF-8, while the stream path uses the JVM default
charset. [Task lifecycle][task]

## Spectrum order and peak processing

- Scan identifiers come from `scan=`, then Agilent `scanId=` or `scanid=`, then XML `index + 1`.
  LC/MS checks strict increases in both retention time and scan number after filtering. On a tie or
  a reversal it stably sorts by retention time and renumbers every retained scan from 1. Waters
  conversion introduced this fallback. Preserve native IDs independently if a replacement resolves
  `spectrumRef` into actual scan
  links. [Scan parser][parser], [Waters ordering fix](https://github.com/mzmine/mzmine/commit/263da01cf0)
- Peak order is independent of spectrum order. The default processor sorts each spectrum by m/z
  and moves the paired intensities with it. Advanced import evaluates the scan filter before MS
  peak decoding and runs configured sorting, cropping, mass detection, and injection-time
  denormalization before final storage. Where mass detection runs, the centroided scan or
  mobility spectrum also backs its mass list. Frame and mobility-scan spectrum types can differ.
  [Processor setup][dispatch], [m/z sorting fix](https://github.com/mzmine/mzmine/commit/4ac6b0bf5d), [frame spectrum type change](https://github.com/mzmine/mzmine/commit/cb53438c6c)
- The import scan filter runs on the building scan at `</spectrum>`, before sorting and
  renumbering. Scan-number ranges and every-nth filtering therefore use the native numbers, not
  the numbers visible after import. A spectrum without an m/z array (no `binaryDataArrayList`,
  or only an intensity array) is dropped silently and does not count as an imported scan.
  A spectrum with an m/z array but no intensity array throws.
- Missing schema-required attributes throw instead of falling back:
  `run@defaultInstrumentConfigurationRef`,
  `spectrumList@defaultDataProcessingRef` and `count`, `spectrum@id`, `index` and
  `defaultArrayLength`, `binaryDataArray@encodedLength`, and the chromatogram equivalents. Decide
  per attribute whether a replacement keeps this strictness. [Parser][parser]
- Spectrum retention time comes from the first scan and is stored in minutes. The reader accepts
  seconds and minutes, assumes seconds when the unit is omitted, and uses `-1` when no
  retention-time
  value is found. [Scan metadata][building-scan]

## Ion mobility

- In ordinary IMS mzML, each spectrum is a mobility scan. The reader batches adjacent mobility
  scans with equal retention time and flushes on a retention-time change and at the end of the
  spectrum list. Processing a batch uses the first scan's metadata. The batch is stored as one
  m/z segment and one intensity segment, with scan offsets and scan-local base-peak indices.
  The peak arrays are released and compact metadata is retained. Final frames reuse those segments.
  Summed frame spectra are computed
  separately. [Frame collection][parser], [packed storage][packed],
  [early memory mapping](https://github.com/mzmine/mzmine/commit/1e2b8fca80),
  [base-peak index fix](https://github.com/mzmine/mzmine/commit/bcb0a6a8e9)
- Combined-mobility mzML represents a frame using aligned m/z, intensity, and mobility arrays.
  It checks that all three are present and their declared lengths agree, splits consecutive equal
  mobility values into scans, and uses the same packed storage. The supported mobility array
  defaults to drift time with an unspecified unit. The combined timsTOF accession `MS:1003006`
  produces an explicit unsupported-format error. [Combined spectrum conversion][building-scan],
  [combined IMS support](https://github.com/mzmine/mzmine/commit/6ce81eb754),
  [unsupported TIMS and malformed array errors](https://github.com/mzmine/mzmine/commit/ca6f7ef360)
- Some Agilent conversions omit empty scans. The task constructs a file-wide mobility axis,
  clusters near-equal observed values, and then indexes with a tolerance derived from median
  bin spacing. Drift-time indices ascend with mobility and TIMS indices descend. Each frame has
  the same zero-based axis. A gap before a real scan repeats that scan's storage offset and uses
  a base-peak index of `-1`. The mobility indices used for precursor ranges follow the padded
  axis. [Axis construction][task], [Agilent missing-scan fix](https://github.com/mzmine/mzmine/commit/9b750c1203)
- A cyclic Waters file can mix spectra with and without mobility. The IMS result uses mobility
  spectra, and the mobility-axis pass excludes matching Waters functions other than 1. Current
  frame assembly still iterates the packed scans from those functions. Record the desired
  behavior for mixed functions and for independent frames with equal retention time before
  changing the grouping
  rule. [IMS construction][task], [cyclic Waters accommodation](https://github.com/mzmine/mzmine/commit/3d66e46a82)
- Frame grouping uses retention time only. MS level, polarity, and scan definition can change
  inside a batch, and the frame takes all metadata from its first mobility scan. Mass spectra
  without mobility in an IMS file are dropped, because the IMS file is built only from the
  packed mobility data. UV spectra and chromatograms are still converted.
- Gap filling walks the frame's scans in document order and expects them in axis order: drift
  time ascending, TIMS descending. Descending drift times or a duplicated mobility value in one
  frame throw `ArrayIndexOutOfBoundsException`. With a single distinct mobility value the median
  bin spacing has no input and the import fails. [Padding loop][task]
- The scan filter sees each mobility spectrum as an ordinary scan. Mobility ranges are never
  applied, because the filter only checks mobility on `MobilityScan` and `Frame`. Scan-number
  and every-nth filters remove individual mobility spectra and gap filling then reinserts them as
  empty bins. If one of them was the last observed bin, the trailing-gap defect below moves its
  peaks to the last placeholder. [ScanSelection][scan-selection]

## Precursor interpretation

| Input                          | Existing interpretation                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| LC/MS DDA and DIA              | The first selected ion supplies m/z and charge. An explicit isolation target overrides its m/z. Orbitrap/MSConvert reported differing selected-ion and isolation-target values in a real case. Both offsets are required to build bounds. With a selected-ion list present, a missing usable precursor m/z or a range wider than 15 Da produces DIA info. EAD electron-beam energy takes precedence over CID energy. The value reader tolerates the literal `inf`. [Conversion][msms], [Orbitrap issue #717](https://github.com/mzmine/mzmine/issues/717), [EAD fix](https://github.com/mzmine/mzmine/commit/7a7b81df87), [inf fix](https://github.com/mzmine/mzmine/commit/510bcb0d21) |
| MSⁿ                            | Multiple precursor elements are interpreted as sequential stages. When available on both elements the comparator uses the isolation-window `ms level` user parameter, then numeric scan references, then selected m/z. It reconstructs stages from MS2 onward. The current LC/MS path uses `spectrumRef` for ordering and leaves the parent-scan object null. [Precursor comparator][precursor], [MSⁿ construction][msn], [stage sorting change](https://github.com/mzmine/mzmine/commit/a04d4d42e2)                                                                                                                                                                                    |
| IMS MS/MS                      | Uses selected-ion m/z when available, with the isolation target as fallback. The bounds always use the isolation target and both offsets. Precursor m/z and collision energy identify an event for extension across mobility indices. Missing collision energy is supported. [IMS conversion][conversion], [isolation window fix](https://github.com/mzmine/mzmine/commit/056df0c83c), [nullable collision energy fix](https://github.com/mzmine/mzmine/commit/0dfeb8ce2e)                                                                                                                                                                                                              |
| Precursor count and activation | One precursor element produces MS/MS info. Two or more always take the MSⁿ path, whatever the spectrum's MS level. Activation methods map from CID, low-energy CID, beam-type CID (HCD), and ECD. ETD and other methods become `UNKNOWN`. EAD is recognized only from the electron-beam energy, so an EAD spectrum without it is reported as CID. A precursor without `<activation>` throws a `NullPointerException`. The schema requires the element. [Conversion][msms], [MSⁿ construction][msn]                                                                                                                                                                                      |
| MRM/SRM trace                  | Precursor and product isolation targets provide Q1 and Q3. The trace's m/z is Q3, and Q1 is recorded in MS/MS info alongside available activation energy. Preserve trace polarity. [Chromatogram isolation parsing][chromatogram], [MRM metadata change](https://github.com/mzmine/mzmine/commit/f975adb2fb)                                                                                                                                                                                                                                                                                                                                                                            |

## XML and detector data

- Binary arrays use full element text. Earlier fixes moved away from position-based reads that
  misread data after multibyte characters. Preserve little-endian 32/64-bit float and integer
  decoding, zero-length arrays, zlib, Numpress, and Numpress with zlib. Shimadzu and UNIFI files
  require acceptance of leading-newline binary text and surrounding whitespace. The inflater
  cleanup fix matters for long
  runs. [Decoder][decoder], [multibyte fix](https://github.com/mzmine/mzmine/pull/1619),
  [Shimadzu fix](https://github.com/mzmine/mzmine/commit/e45e8ec6cf),
  [UNIFI fix](https://github.com/mzmine/mzmine/commit/a4352f0386),
  [inflater fix](https://github.com/mzmine/mzmine/commit/069761e4c6)
- A separate startup fix relaxed JAXP size limits after JDK 24 caused large XML imports with
  repeated escaped text to fail. Large indexed files are worth retesting if the replacement
  changes XML providers. [JDK 24 import fix](https://github.com/mzmine/mzmine/commit/7e6c2dd0f3)
- Referenced CV groups need the same element context as inline CVs. The SCIEX repair routes groups
  into scan lists, scans, scan windows, and precursor children. With a scan window present but its
  bounds missing, the reader falls back to the observed-range CVs or the decoded m/z values. The
  latter must still work after binary metadata has been
  released. [SCIEX fix](https://github.com/mzmine/mzmine/pull/3321)
- Header parsing captures the optional start time, referenced instrument configuration, software,
  sample, source file, and acquisition components. The import task treats metadata mapping
  failures as warnings. [Header metadata][header], [task mapping][task]
- UV spectra use wavelength and intensity arrays and bypass the MS filter. Chromatograms use
  time and intensity arrays and become detector traces grouped by type and unit. The conversion
  sorts points, drops duplicate point pairs and empty traces, and accepts a chromatogram-only
  run. Trace length comes from `defaultArrayLength`, not from the decoded array. The time array's
  unit is ignored and values are taken as minutes. CAD traces are recognized only by `CAD` in the
  chromatogram ID and are relabeled from absorbance to ion current in
  picoampere. [Detector conversion][conversion], [empty trace fix](https://github.com/mzmine/mzmine/commit/e06740623b),
  [trace deduplication](https://github.com/mzmine/mzmine/commit/6359b26979),
  [chromatogram-only fix](https://github.com/mzmine/mzmine/commit/2f2b1bc124)

## Defects at the baseline and their regression cases

| Current defect                                                                                                                                                                                                                                                                 | Regression case for corrected behavior                                                                                                                                                                                                                                       |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Combined splitting emits bins only at value changes. It drops the final group or all points in a one-bin spectrum.                                                                                                                                                             | Assert every peak in `[a, a, b, b]` and `[a, a]` survives at the expected index. [Split loop][building-scan], [introducing commit](https://github.com/mzmine/mzmine/commit/197470a870)                                                                                       |
| Trailing gaps repeat the last real scan's start offset. The point count is calculated from the next offset, or from the total for the last bin. That transfers the real scan's peaks to the final placeholder.                                                                 | Import a frame with a nonempty last observed bin followed by missing bins. The real bin must retain its peak pairs and every trailing bin must have zero points. [Padding loop][task], [point-count calculation][storage]                                                    |
| LC/MS returns before reading isolation-only precursor metadata when the selected-ion list is absent. IMS extraction can carry the previous element's target, charge, offsets, or energy into the next element. IMS and MSⁿ fallback ordering can still throw on literal `inf`. | Cover isolation-only DIA, two IMS precursors with different windows and missing CVs on the second, and non-finite selected m/z in each fallback path. [LC/MS conversion][msms], [IMS conversion][conversion], [MSⁿ comparator][precursor]                                    |
| Referenced binary-array CVs go to the enclosing spectrum, while the chromatogram path routes all group refs to chromatogram-level CVs.                                                                                                                                         | Assert a reference-only binary descriptor decodes exactly like inline CVs. Test chromatogram precursor and product groups separately. [Parser dispatch][parser], [mzML schema](https://raw.githubusercontent.com/HUPO-PSI/mzML/master/schema/schema_1.1/mzML1.1.0.xsd)       |
| Mobility-range import filters are ignored. Scan-number filters punch holes into frames.                                                                                                                                                                                        | `skipsBinaryDecodingForFramesRejectedByMetadata` checks selection before decoding. `appliesMobilityRangeFilterDuringImport` checks whole-frame mobility selection. An overlapping frame retains every peak, and a disjoint frame is dropped. [ScanSelection][scan-selection] |
| Frames whose mobility scans are not in axis order, or repeat a mobility value, throw during gap filling.                                                                                                                                                                       | `acceptsDescendingDriftTimesWithinAFrame`, `keepsFirstAcquiredSpectrumForNearlyEqualMobilities`, and `usesTighterToleranceForCombinedTimsBins` check axis ordering, tolerance, retained peaks, and warnings. [Padding loop][task]                                            |
| An MS2 spectrum with several precursor elements (for example Orbitrap MSX multiplexing) is read as MS2 plus MS3 stages. It fails the MS level `assert` when assertions are enabled, as in Gradle tests.                                                                        | `keepsMultiplexedMs2PrecursorsAtMsLevelTwo` (disabled). [MSⁿ construction][msn]                                                                                                                                                                                              |
| Chromatogram time arrays in seconds (`UO:0000010`) are stored as minutes, so traces become 60 times too long.                                                                                                                                                                  | `convertsChromatogramTimeArrayInSeconds` (disabled). [Chromatogram][chromatogram]                                                                                                                                                                                            |
| Wrappers publish an imported file but delegate result retrieval to a child task which remains outside `FINISHED`. In advanced ZIP import this can skip post-import mass detection. Direct import publishes a zero-output file before setting `ERROR`.                          | Assert result retrieval, project contents, follow-up processing, and status agree on success, parse failure, and zero output. [Shared task][task], [archive wrapper][zip], [advanced wrapper][mass-wrapper]                                                                  |

`TagTracker` records a set of ancestor names. `MzMLParser` shares mutable fields between spectrum
and chromatogram callbacks and reconstructs context separately for inline CVs and references.
Reading `getElementText()` also consumes the closing event, requiring a manual tracker pop.
The tracker is a `LinkedHashSet`. A nested element with its parent's name would replace the
parent instead of stacking, and the parent's end tag would then throw. mzML has no such nesting
today, but a stack-based replacement should not inherit this limitation. `inside()` matches any
ancestor, so a group ref inside `<product>` falls through to the spectrum CVs.
These are the places to examine when comparing a replacement's XML handling against the cases
above. [TagTracker][tracker], [XML callbacks][parser]

## Tests

Run the synthetic suite on JDK 26 from the repository root.

```powershell
.\gradlew.bat :mzmine-community:test --tests import_data.MzMLSyntheticImportTest
```

- [MzMLSyntheticImportTest][synthetic] builds minimal XML in the test. It covers file and
  non-seekable stream input, stable spectrum sorting, duplicate native-number renumbering,
  native ID fallback,
  m/z and intensity alignment, multibyte text and binary whitespace, DDA/DIA and EAD,
  the `inf` case, reversed MSⁿ stages, an omitted interior IMS bin with scan-local base peaks and
  isolation bounds, a chromatogram-only MRM run with empty and duplicate trace data, and a
  spectrum without binary arrays. Four defect cases were disabled against the baseline. The
  replacement now runs the
  descending-frame, frame-selection and chromatogram-time cases with each StAX provider. The
  simultaneous MS2 target case stays disabled pending a data-model change.
  These fixtures use uncompressed binary arrays and heap-backed segments. They check packed data
  and metadata semantics without requiring vendor converters.
- [MzMLMissingOptionalCvParamsTest][optional] covers the SCIEX CV reference and range fallback
  cases. [MzMLFileMetadataTest][metadata-test] covers acquisition
  metadata. [MzMLImportTest][import-test]
  and [AdvancedImportMassDetectionEqualityTest][advanced-test] cover vendor fixtures and compare
  mass-list output. The TIMS fixture named `lc-tims-ms-pasef-a_2frames.mzML` contains three frames
  of
  1,065 individual mobility spectra each and represents empty spectra explicitly. Omitted and
  combined mobility bins need targeted inputs. Assert peak arrays, base peaks, and precursor
  ranges per mobility index to catch placement errors that leave aggregate counts intact.

[task]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MSDKmzMLImportTask.java
[parser]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/MzMLParser.java
[tracker]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/util/TagTracker.java
[dispatch]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_all/AllSpectralDataImportModule.java
[msconvert]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_msconvert/MSConvertImportTask.java
[thermo]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_thermo_raw/ThermoRawImportTask.java
[zip]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_zip/ZipImportTask.java
[mass-wrapper]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_all/MsDataImportAndMassDetectWrapperTask.java
[raw-utils]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/util/RawDataFileUtils.java
[legacy-module]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MSDKmzMLImportModule.java
[scan-selection]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/parameters/parametertypes/selectors/ScanSelection.java
[building-scan]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/BuildingMzMLMsScan.java
[packed]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/BuildingMobilityScanStorage.java
[storage]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/datamodel/impl/MobilityScanStorage.java
[msms]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/datamodel/msms/MsMsInfo.java
[msn]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/datamodel/impl/MSnInfoImpl.java
[precursor]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/MzMLPrecursorElement.java
[conversion]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/ConversionUtils.java
[chromatogram]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/MzMLChromatogram.java
[decoder]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/MzMLPeaksDecoder.java
[header]: https://github.com/mzmine/mzmine/blob/91767f75ee/mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/msdk/data/header/MzMLHeaderMetadata.java
[synthetic]: ../mzmine-community/src/test/java/import_data/MzMLSyntheticImportTest.java
[optional]: ../mzmine-community/src/test/java/import_data/MzMLMissingOptionalCvParamsTest.java
[metadata-test]: ../mzmine-community/src/test/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLFileMetadataTest.java
[import-test]: ../mzmine-community/src/test/java/import_data/MzMLImportTest.java
[advanced-test]: ../mzmine-community/src/test/java/import_data/AdvancedImportMassDetectionEqualityTest.java
