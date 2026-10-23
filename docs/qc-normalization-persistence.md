# QC normalization persistence benchmark

These measurements describe the 2026-10-22 history-only persistence layout. Active functions are
now also persisted in feature-list preferences, so the archive sizes and timings below do not
measure the current combined preferences/history layout. That layout needs a fresh benchmark
before these numbers can be used as its performance baseline.

`FeatureRowNormalizationFunction` persists each raw-file function as one packed XML payload. The
benchmark has two independent measurements:

- Parameter XML round trips serialize the complete hidden normalization-summary parameter in an
  `IntensityNormalizerParameters` set.
- Feature-list archive round trips use the native `FeatureListSaveTask` and `FeatureListLoadTask`
  paths with four resident raw-file identities and a 10,000-row modular feature list.

The feature-list archive is deliberately not a full `.mzmine` project measurement. It excludes raw
data archives, project metadata, user parameters, and other feature lists. The archive benchmark
therefore establishes the persistence cost of a representative normalized feature list, while full
project save/load remains outside this acceptance boundary.

Run from the Gradle root:

```zsh
./gradlew --no-daemon -I docs/benchmarks/normalization-persistence.init.gradle \
  :mzmine-community:runNormalizationPersistenceBenchmark
```

The task compiles test classes and starts every mode in a separate JVM with `-Xmx4g`. The parameter
cases check that the loaded `IntensityNormalizationSummary` equals the saved summary exactly, which
captures exact factor doubles and unavailable-reason maps. The archive cases check the loaded row
count, raw-file bindings, selected row factors, and source/loaded feature intensities. DOM elements
are counted in serialized parameter XML. The heap-pool high-water sum adds each JVM heap memory
pool's high-water usage; it is a diagnostic, not a process RSS measurement or a total peak-heap
measurement. Pool maxima can occur at different times, so their sum can exceed the JVM `-Xmx4g`
cap.

The parameter persistence budget is a 4 GiB heap cap and 60 seconds combined save/load time at
100,000 rows x 100 files. Archive control and normalized runs are separate JVMs so their high-water
heap figures are comparable without subtracting one process peak from another. Source and loaded
feature lists are intentionally resident together during the archive-load measurement; the reported
heap-pool high-water sum is not an incremental normalization-only heap cost.

The following run was recorded on 2026-10-22 using JDK 26 on macOS aarch64, with a 4 GiB JVM heap cap. The command
completed in 26 seconds. The heap-pool high-water sum is shown exactly as measured and must not be
read as total heap usage, RSS, or normalization overhead.

| Parameter mode | Rows x files | Available factors | Unavailable outcomes | DOM elements | XML bytes | Gzip XML bytes | Construct / save / load | Heap-pool high-water sum |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Small mixed availability | 1,000 x 4 | 3,600 | 400 | 59 | 65,826 | 22,955 | 5.4 / 380.4 / 17.9 ms | 46.1 MiB |
| Small all available | 1,000 x 4 | 4,000 | 0 | 59 | 67,870 | 24,539 | 5.3 / 388.8 / 18.3 ms | 32.4 MiB |
| Large mixed availability | 100,000 x 100 | 9,000,000 | 1,000,000 | 251 | 154,697,068 | 59,824,354 | 433.9 / 1,314.2 / 677.0 ms | 3,214.0 MiB |
| Large all available | 100,000 x 100 | 10,000,000 | 0 | 251 | 160,027,968 | 61,620,827 | 550.9 / 1,389.3 / 730.1 ms | 4,257.0 MiB |

| Feature-list archive mode | Rows x files | Source / loaded feature lists resident | ZIP bytes | Construct / save / load | Heap-pool high-water sum |
| --- | ---: | ---: | ---: | ---: | ---: |
| Control history only | 10,000 x 4 | 1 / 1 | 408,884 | 794.5 / 319.9 / 303.7 ms | 266.8 MiB |
| Control history plus packed normalization | 10,000 x 4 | 1 / 1 | 654,469 | 1,058.0 / 324.9 / 335.0 ms | 274.0 MiB |

The archive result adds 245,585 ZIP bytes in this run. It does not establish a total project archive
size or a standalone incremental heap cost. The mixed-availability parameter measurements include
unavailable-reason records; all-available mode is selectable through
`normalizationPersistenceSmallAllAvailable` and `normalizationPersistenceLargeAllAvailable`. The
packed format keeps XML node growth bounded by functions and parameter structure rather than
row/file pairs, but decoded maps still dominate heap use at the large dimension.
