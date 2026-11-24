# Replacing the mzML importer

**Implemented on `mzml-rewrite`.** The forward-only reader consumes one XML subtree per method.
Spectrum, chromatogram and frame conversion own the scientific interpretation. The design and
acceptance criteria below record the reviewed plan.
The [implementation and verification](#implementation-and-verification) section records the outcome.

The production baseline is `91767f75ee`. The [import reference](mzml-import-reference.md) records
the vendor cases, history and caller contracts.
The [synthetic tests](../mzmine-community/src/test/java/import_data/MzMLSyntheticImportTest.java)
exercise those contracts against three XML providers.

## 1. Make the call tree follow the document

In mzML a **spectrum** owns a scan list, precursor and product lists, and binary arrays. A **scan**
inside that list carries acquisition metadata such as retention time, drift time, and scan windows.
Keeping those names in the reader avoids confusing an XML scan with the MZmine scan eventually built
from a spectrum.

```text
readDocument
  indexedmzML → readMzML; skip indexList, indexListOffset, fileChecksum
  mzML        → readMzML
    readReferenceableParamGroupList
    readFileDescription / readSampleList / readSoftwareList / readInstrumentConfigurationList
    readRun
      readSpectrumList
        readSpectrum
          readScanList → readScan → readScanWindowList
          readPrecursorList → readPrecursor → readIsolationWindow / readSelectedIonList / readActivation
          readProductList → readProduct → readIsolationWindow
          readBinaryDataArrayList → readBinaryDataArray
      readChromatogramList
        readChromatogram
          readPrecursor / readProduct
          readBinaryDataArrayList → readBinaryDataArray
```

**Cursor contract.** A method starts on its own `START_ELEMENT` and returns on its matching
`END_ELEMENT`. Only the parent advances to the next sibling. The parent dispatches on the immediate
child's local name. A leaf that calls `getElementText()` already satisfies the return contract.
Unknown children use a depth-counted skip on standard StAX events. The task chooses the XML provider
when opening the input. The document, header and record readers accept any `XMLStreamReader`. Empty
elements use the same contract. This eliminates the manual pop after `<binary>`, ancestor searches,
and shared nullable `Vars` fields. Use ordinary loops for trivial list containers. One class per XML
tag would add little here.

```java
// Sketch inside readPrecursor. The cursor starts on <precursor>.
final var precursor = new MzMLPrecursorElement(attribute(reader, "spectrumRef"));
while(reader.

nextTag() ==XMLStreamConstants.START_ELEMENT){
    switch(reader.

getLocalName()){
    case"isolationWindow"->precursor.

setIsolationWindow(readIsolationWindow(reader));
    case"selectedIonList"->precursor.

setSelectedIonList(readSelectedIonList(reader));
    case"activation"->precursor.

setActivation(readActivation(reader));
default ->

skipElement(reader);
  }
      }
      return precursor;
```

Each record owns its metadata until conversion completes. Share a document-level map keyed by
parameter-group ID. Each owning element applies inline CVs, user parameters, and referenced
parameters to the same destination. For example, a reference inside a product window updates that
window, and a reference inside a binary array sets the same precision, compression, type, and unit
fields as inline declarations. Preserve the header mapper and run-reference resolution. Replace the
header parser's dependency on `TagTracker` at the same time. Include the source identity, spectrum
or chromatogram ID, element, and XML location in parse errors.

## 2. Separate XML, data processing, and task ownership

| Owner                                | Responsibility and lifetime                                                                                                                                                                                                                                                                                                                                                                                                              |
|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MzMLReader`                         | Consume a single input stream once. Hold the XML cursor, header/group lookup, and current element's state. Hand completed records to the importer as the document advances. Use the same read path for files and converter/archive streams.                                                                                                                                                                                              |
| Spectrum and chromatogram conversion | Reuse `ConversionUtils`, the header mapper, peak decoder, and scan processor implementations where their contracts fit. For LC/MS, apply the import filter using native scan identity before renumbering. For IMS, see the filtering row in section 3. Decode selected MS arrays, sort paired peaks, crop, mass-detect, and denormalize in the configured order. Keep UV wavelength spectra and detector traces on their existing paths. |
| LC/MS assembly                       | Store processed peak segments immediately. Retain only metadata and segment handles needed to construct the final scans. Check strict increases in both RT and scan number among retained scans. On failure, stably sort by RT and renumber from 1. Keep the native ID available for precursor interpretation.                                                                                                                           |
| IMS assembly                         | Own the current frame's arrays, compact metadata for completed frames, and the file-wide mobility index. Process and pack each completed frame once, then release transient peak arrays. Build final frame and precursor indices after the mobility axis is known.                                                                                                                                                                       |
| Import task and wrappers             | Own progress, cancellation, input/process lifetime, validation, applied methods, and project publication. An import returns a raw file and its declared, parsed-before-filter, and converted counts. A wrapper can inspect that result directly without querying the status of a child task it never ran. Publish once, after validation.                                                                                                |

**Unicode and indexing.** Parse ordinary and indexed mzML sequentially, including when the source is
a regular file. Binary payloads come from the cursor while positioned inside their `<binary>`
element. Never derive a byte position from a Java string length, StAX character location, or number
of characters read. The index footer can be skipped, so this approach also works on a pipe. Start
both input paths with the same explicit UTF-8 reader as the current file path and verify the
existing invalid-header fixtures before changing encoding behavior. Test Chinese and Korean text, an
accented character, and a supplementary character **ahead of several distinct binary payloads**. Run
the same bytes through a file and a non-seekable stream that returns short chunks, including splits
inside UTF-8 characters. Assert the metadata and every subsequent peak pair.

**Binary hot path.** Read each array's descriptor and its `<binary>` text, keeping the text only
until `</spectrum>`. Classify MS, UV, and combined IMS there. Check the source frame number, MS
level, RT, polarity, spectrum type, and scan definition on the first spectrum before decoding any
peaks, including for combined spectra. For an individual-spectra frame that passes, retain decoded
peaks until the frame closes. A later spectrum may extend its mobility range into the requested
interval. Then pack or discard the complete frame. A combined spectrum already contains its frame's
mobility coordinates. Reuse the working element-text decoder, including little-endian numeric types,
zero-length arrays, zlib, Numpress, combined compression, and vendor whitespace. Profile before
introducing early payload skipping, chunked base64 decoding, or parallelism.

## 3. Resolve the scientific behaviors at their owner

| Case                               | Proposed behavior and acceptance test                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Frame boundaries                   | For contiguous individual mobility spectra, compare RT first. A changed RT starts a frame. Only when RT is equal, check for a change of Waters function and start a frame if the function changed. Test consecutive spectra covering equal RT and function, changed RT with the same function, and equal RT with different functions. Flush the final frame explicitly and treat each combined spectrum as its own frame. Preserve the established handling of UV and chromatograms in IMS runs.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Mobility axis and Waters functions | Build the reference axis from function 1 mobilities (`function=1` in Waters native IDs; all spectra in non-Waters files). Map every other function's mobilities onto that axis with the existing tolerance. If any mobility of a function lies outside the function 1 axis, that function gets its own axis built from its own mobilities, and its frames use it. Test two Waters functions: one fully inside the function 1 axis and one extending beyond it. Assert each frame's axis, coordinates, and peak pairs.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Frame selection                    | Group mobility spectra before filtering. Apply scan-number range and every-nth selection to frame numbers. Evaluate MS level, RT, polarity, spectrum type, and scan definition once per frame. For a mobility-range selection, use the frame's own acquired mobility coverage before padding to a shared axis. Keep every bin and its peaks if the frame matches, and drop the entire frame otherwise. Apply the same rule to combined spectra. Test a partially overlapping frame and a disjoint frame with the same shared-axis coordinates. An every-2nd filter over three frames should keep frames 1 and 3 with all bins intact.                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Packing                            | Sort each accepted frame's lightweight bin descriptors into canonical axis order **before** writing packed peaks. Keep drift time ascending and TIMS descending. Use the existing tolerance/spacing calculation for multiple coordinates and handle a singleton directly.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Gaps and offsets                   | Pack measured bins into one m/z segment and one intensity segment per frame. At finalization walk axis order, carrying the cumulative point count. A missing bin consumes zero points and has base-peak index `-1`, including trailing gaps. Flush the last run of equal mobility in combined arrays. Test leading, interior, trailing, all-filtered, singleton, and descending inputs, with exact peak pairs and isolation ranges at every bin. Within one frame, treat coordinates within ±0.000002 for TIMS or ±0.00002 for other mobility types as repeated bins, using the same tolerance as axis construction. Keep the first acquired spectrum and report discarded spectra in one `RawFileWarning` per file. It uses a new `RawFileWarningType` and its details give the dropped spectrum count, the number of affected frames, and the first frame and mobility. Mark the type critical, because data is dropped. Test a frame with a repeated drift time: the first spectrum's peaks are kept, later frames are unaffected, and exactly one warning is attached. |
| Precursor interpretation           | Parse every precursor independently, then use separate LC/MS, IMS, and MRM conversion rules. Preserve target-versus-selected m/z precedence, unequal isolation offsets, charge, EAD energy, nullable IMS collision energy, and MSⁿ ordering fallbacks. Accept isolation-only DIA when enough window information exists. Use the same safe numeric parsing for `inf` in all fallback paths. Assert bounds and mobility-index ranges after padding.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Multiplexed MS2 (deferred)         | Representing simultaneous MS2 isolation targets requires a data-model change and is outside this replacement. Keep `keepsMultiplexedMs2PrecursorsAtMsLevelTwo` disabled as a record of the limitation. Existing tests for true MSⁿ precursor chains remain part of cutover acceptance.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Detector data and malformed input  | Normalize chromatogram seconds to minutes, retain traces already in minutes, preserve MRM Q1/Q3, polarity, CAD units, deduplication, and chromatogram-only success. Maintain the accepted missing-array case in `skipsSpectrumWithoutBinaryArrays`. For a present m/z array with absent intensity or inconsistent lengths, report the offending record. Retain required-attribute checks and the specific combined-timsTOF diagnostic. Verify trace lengths before indexing decoded arrays.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |

## 4. Remove the local MSDK packages

- Keep the public import module and parameter-set identities used by saved batches. Introduce
  `MzMLImportTask` behind them. Keep task naming and file identity stable for direct file input,
  converter stdout, retained conversions, and archives.
- Put the reader in `import_rawdata_mzml`. Move actively used XML vocabulary and value types into a
  small `model` package, binary descriptors and codecs into `binary`, and the existing header types
  into `header`. Keep `ConversionUtils` in the import package. Collapse list wrappers and temporary
  MSDK raw-file/scan adapters as their last consumers disappear. Preserve original license notices
  in moved sources.
- Audit references across the whole repository before deletion. In particular, `ChromatogramType`
  has consumers in detector data models, UI, and native importers. Move that shared enum to its
  consuming domain and preserve its serialized values. Update precursor helpers used by `MsMsInfo`
  and `MSnInfoImpl` with focused changes. Check the copied exporter, encoder, and random-access
  helpers for live callers individually. Existing export paths also import the **external**
  `io.github.msdk` library. The removal target is the local importer subtree.
- Cut over direct and advanced dispatch, the legacy module, `RawDataFileUtils` including mzTab-M
  loads, ZIP/GZIP, ThermoRawFileParser, and both MSConvert paths. Give advanced archive import one
  owner for mass detection. Preserve converter count checks and duplicate-file handling. Thermo uses
  the same emptiness rule as MSConvert: fail only when there are no spectra and no chromatograms or
  other detector data, so chromatogram-only output (for example TSQ MRM) imports. Exercise
  cancellation, partial converter output, empty results, and chromatogram-only Thermo output before
  removing the old task.

## 5. Implementation sequence

1. **Freeze the baseline.** Run the updated synthetic, optional-CV, header, vendor-fixture, and
   advanced mass-list equality tests. Record outputs and benchmark inputs at a pinned revision with
   the local fixture changes included. Document deliberate output differences. Keep disabled defect
   tests as requests for corrected behavior.
2. **Build the element reader alongside the existing entry point.** Implement header, LC/MS,
   precursors, binary arrays, and chromatograms against the same test import helper. Compare both
   readers' output on the compatibility cases. Add adjacent-sibling, unknown-subtree, reference-only
   binary/product, and Unicode chunk-boundary fixtures. Enable
   `convertsChromatogramTimeArrayInSeconds` on the new reader.
3. **Implement IMS packing and finalization.** Reuse the batched processor and final storage
   contract. Add tests for final combined bins and trailing placeholders. Enable
   `acceptsDescendingDriftTimesWithinAFrame` and `appliesMobilityRangeFilterDuringImport`. Add the
   frame-level filter, Waters function axis, and repeated-coordinate warning tests from section 3.
   Strengthen them to check exact axis length, coordinates, peak counts, and intensities so a
   dropped bin cannot pass unnoticed. Run mapped-storage and advanced mass-list comparisons.
4. **Complete precursor and caller integration.** Verify supported LC/MS, IMS, and MRM precursor
   outputs. Run a saved batch, archive, converter-stream, truncated-stream, cancellation, and
   publication check. Exercise wrapper result retrieval and verify the applied method appears
   exactly once.
5. **Switch and delete.** After correctness and performance acceptance, switch production dispatch
   in one change. Remove the old parser, tracker, adapter classes, and residual local package
   imports. Compile every consumer, verify affected export and project/batch round trips, and rerun
   the accepted corpus. Keep the baseline revision and measurements for comparisons after deletion.

## 6. Performance acceptance

Use [ImportSpeedTestMain](../mzmine-community/src/test/java/import_data/speed/ImportSpeedTestMain.java)
and its existing JSON-lines timing and storage measurements. It currently hard-codes a local
dataset, an advanced filter, and in-memory storage. Parameterize dataset, input mode, processing
config, and storage mode for the comparison. Time opening the input through construction of the
final usable file. Measure converter execution separately by replaying captured mzML bytes through a
pipe.

### Selected local test data

| Input                                                                                                                                                                                                     | Baseline and replacement runs                                                                                                                                                                                                                                                            |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Thermo folder, `D:\OneDrive - mzio GmbH\Example data - Documents\Thermo\20 years mzmine`                                                                                                                  | Include every mzML recursively (25 at inventory). Record the relative paths and sizes in the baseline results. Compare scan counts, point counts, and precursor metadata per file. Report elapsed time for the full set using the same file order and import concurrency in both builds. |
| SCIEX ZenoTOF 8600, `D:\OneDrive - mzio GmbH\Example data - Documents\SCIEX\ZenoTof 8600\NIST 1950 + Pesticide (ZTScan 3)\8600_Meta_Pos_ZTScan_05.0Da_CE30_17min_060-609_NIST1950_iDQuant-0.0625_01.mzML` | Use this 5,182,947,149-byte file for large-input throughput and peak heap. Replay the same bytes through a non-seekable stream and compare output with the direct file import.                                                                                                           |

Supply these paths to the benchmark at run time. Record which acquisition and encoding cases the
baseline actually encounters. Use the existing vendor fixtures for cases absent from this corpus,
including mapped IMS where needed.

### Workloads and measurements

| Workload                                                                   | What it reveals                                                              |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------|
| Large profile LC/MS, default import and a selective advanced filter        | XML dispatch overhead, avoided decoding, peak allocations, mass detection    |
| Centroid LC/MS with zlib and Numpress; identical XML from disk and a pipe  | Decoder throughput, single-pass streaming, source parity                     |
| Individual and combined IMS, including sparse frames, using mapped storage | Frame flush cost, offsets, peak heap and temporary storage, redundant copies |
| Chromatogram-heavy and mixed UV/MS data                                    | Detector conversion cost and long-lived trace allocations                    |

Run old and new builds on the same machine, JDK, heap, dataset bytes, storage drive, cache
conditions, and processing settings. Warm up each build, alternate their order, and collect at least
five measured imports per representative workload. Record median wall time, spread, input MiB/s,
output scan/frame and point counts, peak heap, GC time, and mapped storage. Use JFR allocation
sampling on a separate diagnostic run. Compare scientific outputs before interpreting throughput,
especially where fixing a dropped bin increases the amount of data retained.

The approved cutover limits are a maximum **5% increase in median wall time** on each representative
workload and a maximum **10% increase in peak heap** on mapped IMS, with no additional file-wide
peak copy. If measurement noise overlaps a threshold, repeat the paired runs. Investigate repeatable
regressions per workload. Baseline measurements belong to step 1.

## Decisions from review

1. The whole replacement, including the behavior fixes, the wrapper/publication contract, and the
   package removal, lands on one branch.
2. IMS scan-number and every-nth filters apply after frame grouping, to frame numbers.
3. Function 1 mobilities define the axis. Other Waters functions map onto it, or get their own axis
   if any of their mobilities lie outside it.
4. Repeated mobility coordinates within a frame: keep the first spectrum and add a `RawFileWarning`.
5. Thermo accepts chromatogram-only output like MSConvert. Import fails only without spectra and
   chromatograms.
6. The binary path keeps today's read-then-decode-at-`</spectrum>` approach.
7. The parser depends only on the standard StAX cursor. The benchmark selects Aalto, Woodstox, or
   the JDK's built-in provider for the same file and stream entry points.
8. Files and streams use sequential UTF-8 import. RT is the first frame-boundary check. At equal RT,
   a change of Waters function also starts a frame.
9. Mobility-range selection keeps or drops a complete frame, including for combined spectra.
10. The per-workload wall-time and mapped-IMS heap limits in section 6 are approved.

## Implementation and verification

`MzMLImportTask` opens both files and converter streams as UTF-8 and supplies a standard StAX
cursor. Aalto is the production default. A caller can pass a different `XMLInputFactory` to a single
import without changing global JVM properties. The document reader handles the indexed wrapper, run
and lists. The record reader handles the children of a single spectrum or chromatogram. Each reader
leaves the cursor on its own closing tag. Indexed files take the same sequential path as streams, so
text before a binary array never contributes to a byte
seek. [Task](../mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLImportTask.java) · [Document reader](../mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLReader.java) · [Record reader](../mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLRecordReader.java)

LC/MS conversion releases each spectrum’s XML metadata once its final scan exists. Scan numbers are
repaired after filtering only when the retained sequence fails its ordering check. IMS groups by RT
first, checks Waters function at equal RT, evaluates the whole frame’s acquired mobility range, then
sorts and packs its bins. Frame metadata comes from the first acquired spectrum even if another bin
sorts first. Axis padding uses zero-length slices and preserves the isolation state of the last
measured bin across a trailing
gap. [LC/MS assembly](../mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLScanAssembler.java) · [Frame assembly](../mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLFrameAssembler.java) · [Mobility axis](../mzmine-community/src/main/java/io/github/mzmine/modules/io/import_rawdata_mzml/MzMLMobilityAxis.java)

Direct, advanced, archive, Thermo and MSConvert imports use the new task. Converter wrappers check
declared versus parsed spectrum counts before publication and accept chromatogram-only output. The
legacy import module and parameter class names still resolve from saved batches. The copied parser,
tag tracker and local MSDK package have been removed. Shared tag and accession classes live under
`model`, codecs under `binary`, and acquisition metadata under `header`.

### Evidence from the supplied data

The comparison used the same machine and JDK 26, one warm-up, then five measured imports per build.
Each row shows median elapsed seconds through construction of the usable raw file. The Thermo time
is the sum of the 25 file imports in sorted path order. File mode includes opening the file. Stream
mode starts with an open input stream. Output verification runs outside the timer.

| Workload                                                | Original | Replacement (Aalto) | Output checked                                                                                               |
|---------------------------------------------------------|---------:|--------------------:|--------------------------------------------------------------------------------------------------------------|
| Thermo folder, all 25 files                             |  5.002 s |             4.496 s | 116,471 scans, 32,711,838 peaks                                                                              |
| SCIEX ZenoTOF, 5.18 GB, file                            | 27.606 s |            25.671 s | 664,984 scans, 102,045,491 peaks                                                                             |
| Same SCIEX bytes, stream                                | 25.914 s |            22.256 s | Same scans, peaks and precursor fingerprint as file input                                                    |
| TIMS, 51,120 individual spectra, mapped                 |  0.747 s |             0.598 s | 48 frames, 117,024 peaks                                                                                     |
| Combined drift-time IMS, mapped                         |  0.060 s |             0.060 s | 2,048 frames, 262,144 peaks                                                                                  |
| Detector-only, 4,096 chromatograms                      |  0.093 s |             0.072 s | 524,288 time/intensity pairs, matching fingerprint in a separate verification run                            |
| Mixed MS and UV spectra, 90 measured runs per build     |  0.034 s |             0.024 s | 1,024 MS spectra interleaved with 1,024 UV spectra. Synthetic test checks wavelength/intensity pairs and RT. |
| GC Orbitrap advanced import, 30 measured runs per build |  0.329 s |             0.286 s | 1,255 filtered MS1 scans, 92,861 peaks                                                                       |

For each supplied LC/MS file, the comparison fingerprinted scan order, scan number, RT, MS level,
peak pairs and precursor fields, including individual MSⁿ stages. All 25 Thermo file fingerprints
agreed across the baseline, Aalto, Woodstox and the JDK. The SCIEX file and stream fingerprints
agreed with the baseline. In a separate provider comparison, the full Thermo folder took 5.111 s
with Aalto, 5.670 s with Woodstox and 6.460 s with the JDK. These provider medians came from a
separate run series.

For SCIEX file input, one replacement iteration took 908 s. The other four took 24.2–26.0 s. The
cause of the outlier was unverified and it is included in the median. For the advanced workload, the
first short comparison measured 0.316 s versus 0.283 s. Two further 15-run series reversed the
process order. The replacement medians were 0.303 s and 0.269 s versus 0.352 s and 0.299 s. Each
series produced the same output fingerprint.

The larger mapped workload repeated the three-frame Numpress TIMS fixture sixteen times with
distinct IDs and RTs. Median sampled peak heap was 0.38 GB versus 0.52 GB for the baseline. Combined
IMS used 0.07 GB versus 0.08 GB. The 250 ms sampler can miss short allocation peaks, particularly on
the 60 ms combined workload. Sparse-bin placement, base peaks and precursor ranges are checked per
bin in the synthetic suite. The performance fixtures are generated inputs and have different
acquisition characteristics from a full vendor IMS run.

Separate JFR runs measured identical mapped usage in both implementations. Individual TIMS used
2,281,344 bytes and combined IMS used 4,456,448 bytes. Each import reserved a 1 GB mapping. Array
copying and string creation accounted for the largest allocation samples on individual TIMS. On the
short combined workload the instrumented median was 0.126 s versus 0.102 s. The uninstrumented
medians in the table differed by less than 1%. For mixed UV/MS, the first five-run medians were
0.046 s versus 0.041 s. The table reports the longer repeat with process order reversed.

### Reproducing a comparison

Run [MzMLReaderBenchmark](../mzmine-community/src/test/java/import_data/speed/MzMLReaderBenchmark.java)
on the community test runtime classpath with the project JDK and preview enabled. Give it a file or
directory, input mode, iteration count and provider. The following program arguments exercise stream
input, mapped storage and advanced processing.

```text
"<SCIEX mzML>" stream 5 woodstox --verify
"<TIMS mzML>" file 5 aalto --mapped --memory
"<GC Orbitrap mzML>" file 15 jdk --advanced --verify
```

`--advanced` selects MS1 from 30–35 minutes, crops to 100–1,000 m/z and applies factor-of-lowest
mass detection. Choose a file covering that RT interval. `--verify` emits fingerprints and trace
point counts. `--memory` records sampled peak heap, GC time, and used and reserved mapped bytes.
Iteration zero is warm-up. Baseline JVMs loaded the original compiled task from a frozen classpath.
The comparison driver used the same benchmark logic and selected the original task constructor,
which creates its own Aalto reader.

### Integration coverage and limits

- The Pro composite build uses the community importer. Its UV and MRM workflows use the data-model
  `ChromatogramType`, and the task controller recognizes `MzMLImportTask` as a free import task.
  Both UV wizard tests and all 26 task-controller tests passed. On the nine-file Pro MRM batch, the
  original and replacement Aalto importers produced matching per-file fingerprints for all 370,531
  chromatogram time/intensity pairs. The integration test logs CSV mismatches without failing JUnit.
  Both builds reported the same 184 differences from the expected CSV, all in FWHM, asymmetry, or
  tailing factor.
- The original three-provider verification passed 149 tests and skipped the deferred multiplexed MS2
  fixture once per provider. Routine synthetic regressions now use the production Aalto
  configuration. The benchmark can still select any of the three providers. The suite covers split
  UTF-8 code points before binary data, ZIP and GZIP, mixed UV/MS, malformed paired arrays, and
  active versus inactive isolation at a trailing mobility gap.
- The new task matched the existing default and advanced snapshots for seven mzML fixtures,
  including invalid header/character fixtures, Orbitrap MSⁿ, GC profile and TIMS. The
  missing-optional-CV fixtures imported successfully. Detector data-type serialization and trace
  selection tests also passed.
- The bundled ThermoRawFileParser imported the Astral RAW fixture through stdout, producing 16,099
  scans with one applied method and one published file. A saved batch round trip with the historical
  module name loaded without warnings and imported all 521 DOM scans.
- ZIP/GZIP publication, retained MSConvert reuse, split converter preambles, early EOF,
  declared-count mismatch and cancellation have regression tests. Live ProteoWizard conversion was
  not exercised. The application-dispatched vendor and mass-detection equality suites exit at the
  local sign-in check. The direct snapshot comparison and real Thermo run above exercise the
  underlying import paths independently.
- Lossless multiplexed MS2 remains deferred until the data model can represent simultaneous
  isolation targets. Its fixture remains disabled.
