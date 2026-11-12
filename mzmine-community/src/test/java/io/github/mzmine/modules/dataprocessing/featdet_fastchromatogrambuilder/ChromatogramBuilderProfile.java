/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import static io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBenchmarkDatasets.PROPERTY;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBuilderBenchmark.Run;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBuilderBenchmark.Settings;
import io.github.mzmine.project.ProjectService;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Profiles the speed and the memory of the chromatogram builders on the first file of each data set
 * of {@link ChromatogramBenchmarkDatasets} with the preset tolerance, and on synthetic data of
 * growing size. Run with
 * <pre>
 * gradlew :mzmine-community:benchmark --tests "*ChromatogramBuilderProfile*"
 *     -Dmzmine.test.chrombench.only=GC-EI-QTOF -Dmzmine.test.chrombench.profile.jfr=true
 * </pre>
 * Methods: {@code builder} is {@link FastChromatogramBuilder#build} alone, {@code fast} the file
 * task of the fast builder including the feature creation, {@code adap} the ADAP chromatogram
 * builder task. For each method:
 * <ul>
 *   <li>time: median and min of the timed runs</li>
 *   <li>allocated: bytes allocated by the thread in a run, including garbage</li>
 *   <li>retained: live heap of the result (chromatograms or feature list) after a full GC</li>
 *   <li>peak: max live heap above the heap before the run, sampled with full GCs from a second
 *   thread during an extra run. A lower bound, short peaks between samples are missed.</li>
 *   <li>pass boundaries (builder): live heap at the end of pass 1, at the start of pass 2 after the
 *   consolidation and at the end of pass 2 before the finalization, measured with full GCs when the
 *   builder resets or finishes the scans</li>
 * </ul>
 * Options as system properties with the prefix {@value ChromatogramBenchmarkDatasets#PROPERTY}:
 * {@code profile.methods=builder,fast,adap}, {@code profile.runs=<timed runs>},
 * {@code profile.jfr=true} records the timed runs of each method and summarizes the time by phase,
 * the hot methods and the allocation, {@code profile.scaling=<max factor>} of the synthetic data.
 * The report and JFR files go to {@code build/reports/chromatogram-builder-profile}, e.g.,
 * {@code jfr view hot-methods <file>} for more views.
 */
@Tag("benchmark")
@TestInstance(Lifecycle.PER_CLASS)
class ChromatogramBuilderProfile {

  private static final Logger logger = Logger.getLogger(ChromatogramBuilderProfile.class.getName());
  private static final Path OUT = Path.of("build/reports/chromatogram-builder-profile");
  private static final int JFR_TOP = 25;

  private final List<String> report = new ArrayList<>();

  @BeforeAll
  void init() {
    MZmineTestUtil.startMzmineCore();
  }

  @NotNull List<ChromatogramBenchmarkDataset> datasets() {
    return ChromatogramBenchmarkDatasets.selected();
  }

  /**
   * A profiled method.
   */
  private enum Method {
    BUILDER, FAST, ADAP;

    @NotNull String label() {
      return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the methods whose callees are the phases in the JFR summary
     */
    @NotNull List<String> jfrEntries() {
      return switch (this) {
        case BUILDER -> List.of("FastChromatogramBuilder.build");
        case FAST -> List.of("FastChromatogramFileTask.run", "FastChromatogramBuilder.build");
        case ADAP -> List.of("ModularADAPChromatogramBuilderTask.run");
      };
    }
  }

  /**
   * Result of one run.
   *
   * @param output the chromatograms or the feature list, kept to measure the retained memory
   */
  private record Output(long nanos, long allocatedBytes, @Nullable Object output,
                        @Nullable FastChromatogramBuilderStatistics statistics) {

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("datasets")
  void profile(@NotNull ChromatogramBenchmarkDataset dataset) throws Exception {
    final String unavailable = ChromatogramBenchmarkDatasets.unavailableReason(dataset);
    Assumptions.assumeTrue(unavailable == null, unavailable);
    MZmineTestUtil.clearProjectAndLibraries();
    final TaskResult imported = MZmineTestUtil.importFiles(dataset.paths().subList(0, 1), 3600,
        dataset.importParameters());
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, imported, imported.description());
    final RawDataFile file = ProjectService.getProject().getCurrentRawDataFiles().getFirst();
    final Settings settings = Settings.of(dataset);
    final Scan[] scans = settings.scanSelection().getMatchingScans(file);
    final long dataPoints = Arrays.stream(ChromatogramBenchmarkUtils.massListMzs(scans))
        .mapToLong(m -> m.length).sum();

    report.add("## %s\n".formatted(dataset.name()));
    report.add("%s, tolerance %s, %s, %d MS1 scans, %d data points\n".formatted(dataset.describe(),
        settings.tolerance(), file.getName(), scans.length, dataPoints));
    final List<String> jfrSummaries = new ArrayList<>();
    final List<String> rows = new ArrayList<>();
    FastChromatogramBuilderStatistics statistics = null;
    for (final Method method : methods()) {
      final String jfrName = "%s_%s".formatted(dataset.name().replaceAll("[^A-Za-z0-9]+", "_"),
          method.label());
      final MethodProfile profile = profileMethod(method, file, scans, settings, dataPoints,
          jfrName);
      rows.add(profile.row());
      if (profile.jfrSummary() != null) {
        jfrSummaries.add(profile.jfrSummary());
      }
      if (profile.statistics() != null) {
        statistics = profile.statistics();
      }
    }
    report.add(MethodProfile.HEADER);
    report.addAll(rows);
    if (statistics != null) {
      report.add("\nFast builder: " + statistics + "\n");
    }
    report.addAll(jfrSummaries);
    report.add("");
    writeReport();
  }

  /**
   * The builders on synthetic data of growing size, the number of scans and ions grow by the same
   * factor at a constant density of signals and noise. Time and allocation per data point should
   * stay constant for a linear algorithm.
   */
  @Test
  void scaling() throws Exception {
    final int maxFactor = Integer.getInteger(PROPERTY + "profile.scaling", 8);
    final Settings settings = ChromatogramBuilderBenchmark.SYNTHETIC_SETTINGS;
    report.add("## Scaling on synthetic data\n");
    report.add("""
        2000 ions and 300 noise signals per scan for each 1200 scans, tolerance %s, min \
        consecutive %d, min group intensity %.0f, min height %.0f
        """.formatted(settings.tolerance(), settings.minConsecutive(),
        settings.minGroupIntensity(), settings.minHeight()));
    report.add(MethodProfile.HEADER);
    for (int factor = 1; factor <= maxFactor; factor *= 2) {
      final SyntheticLcmsData data = ChromatogramBuilderBenchmark.randomIons(2000 * factor,
          1200 * factor, 300, 0.5, 1.5, 0, factor);
      final RawDataFile file = ChromatogramBenchmarkUtils.toRawDataFile(data,
          "synthetic_x" + factor);
      final Scan[] scans = settings.scanSelection().getMatchingScans(file);
      for (final Method method : methods()) {
        final MethodProfile profile = profileMethod(method, file, scans, settings,
            data.numDataPoints(), null);
        report.add(profile.row().replaceFirst("^\\| ", "| x%d, %d scans, ".formatted(factor,
            scans.length)));
      }
      writeReport();
    }
    report.add("");
    writeReport();
  }

  /**
   * @param jfrName file name of the JFR recording without extension, null for no recording
   */
  @NotNull
  private MethodProfile profileMethod(@NotNull Method method, @NotNull RawDataFile file,
      @NotNull Scan[] scans, @NotNull Settings settings, long dataPoints,
      @Nullable String jfrName) throws IOException {
    final int runs = Integer.getInteger(PROPERTY + "profile.runs", 7);
    // warm up
    release(run(method, file, scans, settings, null));

    final Recording recording =
        jfrName != null && Boolean.getBoolean(PROPERTY + "profile.jfr") ? startRecording() : null;
    final long[] nanos = new long[runs];
    final long[] allocated = new long[runs];
    FastChromatogramBuilderStatistics statistics = null;
    for (int r = 0; r < runs; r++) {
      final Output output = run(method, file, scans, settings, null);
      nanos[r] = output.nanos();
      allocated[r] = output.allocatedBytes();
      statistics = output.statistics();
      release(output);
    }
    String jfrSummary = null;
    if (recording != null) {
      recording.stop();
      Files.createDirectories(OUT);
      final Path jfr = OUT.resolve(jfrName + ".jfr");
      recording.dump(jfr);
      recording.close();
      jfrSummary = JfrProfileSummary.summarize(jfr, "%s, %s, %d runs".formatted(file.getName(),
              method.label(), runs), Thread.currentThread().getName(), method.jfrEntries(),
          JFR_TOP);
    }

    // retained memory of the result, and the live heap at the pass boundaries of the builder
    final long baseline = usedHeapAfterGc();
    final CheckpointScans checkpoints =
        method == Method.BUILDER ? new CheckpointScans(scanAccess(file, scans), baseline) : null;
    final Output kept = run(method, file, scans, settings, checkpoints);
    final long retained = usedHeapAfterGc() - baseline;
    Reference.reachabilityFence(kept.output());
    release(kept);

    // peak live heap, sampled during one more run
    final long peakBaseline = usedHeapAfterGc();
    final long peak;
    final int peakSamples;
    try (final PeakSampler sampler = new PeakSampler()) {
      release(run(method, file, scans, settings, null));
      sampler.stop();
      peak = sampler.max() - peakBaseline;
      peakSamples = sampler.samples();
    }

    Arrays.sort(nanos);
    Arrays.sort(allocated);
    final MethodProfile profile = new MethodProfile(method, dataPoints, nanos[runs / 2], nanos[0],
        allocated[runs / 2], retained, peak, peakSamples, checkpoints, statistics, jfrSummary);
    logger.info(() -> "%s %s: %s".formatted(file.getName(), method.label(), profile.row()));
    return profile;
  }

  @NotNull
  private static Recording startRecording() throws IOException {
    try {
      final Recording recording = new Recording(Configuration.getConfiguration("profile"));
      // decision: denser sampling than the profile settings (10 ms, 300/s), the runs are short
      recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(1));
      recording.enable("jdk.ObjectAllocationSample").with("throttle", "2000/s");
      recording.start();
      return recording;
    } catch (java.text.ParseException e) {
      throw new IOException(e);
    }
  }

  /**
   * @param builderScans the scans of the fast builder, null for the scans of the file. Only used by
   *                     the builder method.
   */
  @NotNull
  private static Output run(@NotNull Method method, @NotNull RawDataFile file,
      @NotNull Scan[] scans, @NotNull Settings settings, @Nullable MzIntensityScans builderScans) {
    return switch (method) {
      case BUILDER -> {
        final FastChromatogramBuilder builder = new FastChromatogramBuilder(settings.tolerance(),
            settings.minConsecutive(), settings.minGroupIntensity(), settings.minHeight());
        final MzIntensityScans input =
            builderScans != null ? builderScans : scanAccess(file, scans);
        final long allocatedBefore = ChromatogramBenchmarkUtils.allocatedBytes();
        final long start = System.nanoTime();
        final List<BuiltChromatogram> chromatograms = builder.build(input, null, null);
        final long nanos = System.nanoTime() - start;
        final long allocated = ChromatogramBenchmarkUtils.allocatedBytes() - allocatedBefore;
        Assertions.assertNotNull(chromatograms);
        yield new Output(nanos, allocated, chromatograms, builder.getStatistics());
      }
      case FAST ->
          taskOutput(ChromatogramBuilderBenchmark.runFastTask(file, settings, true, false));
      case ADAP -> taskOutput(ChromatogramBuilderBenchmark.runAdap(file, settings, true, false));
    };
  }

  @NotNull
  private static Output taskOutput(@NotNull Run run) {
    return new Output(run.nanos(), run.allocatedBytes(), run.flist(), run.statistics());
  }

  /**
   * Removes a feature list of a task from the project.
   */
  private static void release(@NotNull Output output) {
    if (output.output() instanceof FeatureList flist) {
      ProjectService.getProject().removeFeatureList(flist);
    }
  }

  @NotNull
  private static MzIntensityScans scanAccess(@NotNull RawDataFile file, @NotNull Scan[] scans) {
    return new ScanDataAccessScans(
        EfficientDataAccess.of(file, ScanDataType.MASS_LIST, Arrays.asList(scans)));
  }

  @NotNull
  private static List<Method> methods() {
    return Arrays.stream(System.getProperty(PROPERTY + "profile.methods", "builder,fast,adap")
            .split(",")).map(String::trim).filter(s -> !s.isEmpty())
        .map(s -> Method.valueOf(s.toUpperCase(Locale.ROOT))).toList();
  }

  /**
   * @return the used heap after two full garbage collections, the live heap
   */
  static long usedHeapAfterGc() {
    System.gc();
    System.gc();
    return usedHeap();
  }

  private static long usedHeap() {
    return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
  }

  /**
   * Profile of one method on one file.
   *
   * @param checkpoints the live heap at the pass boundaries of the builder, null for the tasks
   */
  private record MethodProfile(@NotNull Method method, long dataPoints, long medianNanos,
                               long minNanos, long medianAllocated, long retained, long peak,
                               int peakSamples, @Nullable CheckpointScans checkpoints,
                               @Nullable FastChromatogramBuilderStatistics statistics,
                               @Nullable String jfrSummary) {

    static final String HEADER = """
        | method | median ms | min ms | ns per data point | allocated MB | allocated bytes per data point | retained MB | peak MB (samples) | builder live MB: end pass 1 / start pass 2 / end pass 2 |
        |---|---|---|---|---|---|---|---|---|""";

    @NotNull String row() {
      return "| %s | %.1f | %.1f | %.1f | %.1f | %.1f | %.1f | %.1f (%d) | %s |".formatted(
          method.label(), medianNanos / 1e6, minNanos / 1e6,
          (double) medianNanos / Math.max(1, dataPoints), medianAllocated / 1e6,
          (double) medianAllocated / Math.max(1, dataPoints), retained / 1e6, peak / 1e6,
          peakSamples, checkpoints == null ? "" : checkpoints.describe());
    }
  }

  /**
   * Measures the live heap at the pass boundaries of the builder with full garbage collections: the
   * builder resets the scans before each pass and reads them to the end.
   */
  private static final class CheckpointScans implements MzIntensityScans {

    private final @NotNull MzIntensityScans scans;
    private final long baseline;
    private final LongArrayList passStarts = new LongArrayList();
    private final LongArrayList passEnds = new LongArrayList();
    private int resets;

    /**
     * @param baseline the live heap before the build
     */
    CheckpointScans(@NotNull MzIntensityScans scans, long baseline) {
      this.scans = scans;
      this.baseline = baseline;
    }

    @Override
    public int getNumberOfScans() {
      return scans.getNumberOfScans();
    }

    @Override
    public void reset() {
      if (resets++ > 0) {
        passStarts.add(usedHeapAfterGc() - baseline);
      }
      scans.reset();
    }

    @Override
    public boolean nextScan() {
      final boolean next = scans.nextScan();
      if (!next) {
        passEnds.add(usedHeapAfterGc() - baseline);
      }
      return next;
    }

    @Override
    public int getNumberOfDataPoints() {
      return scans.getNumberOfDataPoints();
    }

    @Override
    public double getMz(int index) {
      return scans.getMz(index);
    }

    @Override
    public double getIntensity(int index) {
      return scans.getIntensity(index);
    }

    @NotNull String describe() {
      return "%.1f / %.1f / %.1f".formatted(passEnds.isEmpty() ? 0 : passEnds.getLong(0) / 1e6,
          passStarts.isEmpty() ? 0 : passStarts.getLong(0) / 1e6,
          passEnds.size() < 2 ? 0 : passEnds.getLong(1) / 1e6);
    }
  }

  /**
   * Samples the live heap with full garbage collections from a daemon thread, at most half of the
   * time is spent in these collections.
   */
  private static final class PeakSampler implements AutoCloseable {

    private final Thread thread;
    private volatile boolean running = true;
    private volatile long max;
    private volatile int samples;

    PeakSampler() {
      thread = Thread.ofPlatform().daemon().name("heap sampler").start(this::sample);
    }

    private void sample() {
      while (running) {
        final long start = System.nanoTime();
        System.gc();
        max = Math.max(max, usedHeap());
        samples++;
        LockSupport.parkNanos(Math.max(5_000_000L, System.nanoTime() - start));
      }
    }

    long max() {
      return max;
    }

    int samples() {
      return samples;
    }

    void stop() {
      running = false;
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void close() {
      stop();
    }
  }

  private void writeReport() throws IOException {
    Files.createDirectories(OUT);
    final String text = String.join("\n", report) + "\n";
    Files.writeString(OUT.resolve("profile.md"), text);
    logger.info("Chromatogram builder profile\n" + text);
  }
}
