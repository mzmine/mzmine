# QC normalization persistence benchmark

`FeatureRowNormalizationFunction` persists each raw-file function as one packed XML payload. This
benchmark measures that payload through the complete hidden normalization-summary parameter in an
`IntensityNormalizerParameters` set. It does not measure a complete project archive: the reported
compressed size is gzip-compressed parameter XML only.

Run from the Gradle root:

```zsh
./gradlew --no-daemon -I docs/benchmarks/normalization-persistence.init.gradle \
  :mzmine-community:runNormalizationPersistenceBenchmark
```

The task compiles test classes, starts each benchmark mode with `-Xmx4g`, writes no project, and
checks that the loaded `IntensityNormalizationSummary` equals the saved summary exactly. That check
covers exact factor doubles and unavailable-reason maps. DOM elements are counted in the serialized
parameter XML. Peak heap is the sum of each JVM heap memory pool's high-water usage; it is a useful
upper-bound indicator, not a process RSS measurement.

The persistence budget is a 4 GiB heap cap and 60 seconds combined save/load time at
100,000 rows x 100 files. All measured cases meet that budget. These are
serialization measurements; they do not include the rest of an open project or model fitting.

| Mode | Rows x files | Available factors | Unavailable outcomes | DOM elements | XML bytes | Gzip XML bytes | Construct / save / load | Peak heap |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Small mixed availability | 1,000 x 4 | 3,600 | 400 | 59 | 65,826 | 22,955 | 6.0 / 386.8 / 18.7 ms | 32.7 MiB |
| Large mixed availability | 100,000 x 100 | 9,000,000 | 1,000,000 | 251 | 154,697,068 | 59,824,354 | 431.6 / 1,325.3 / 631.9 ms | 3,258.0 MiB |
| Large all available | 100,000 x 100 | 10,000,000 | 0 | 249 | 160,027,860 | 61,620,781 | construction included in save / 1,933.0 / 735.9 ms | 3,931.0 MiB |

The mixed-availability measurements include the two hidden source-provenance fields; the
all-available measurement omits them and shows the higher observed peak heap. The packed format
keeps XML node growth bounded by functions and parameter structure rather than row/file pairs, but
decoded maps still dominate heap use at the large dimension.
