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

package import_data.speed;

import com.google.common.collect.Range;
import import_data.MzMLTestReader;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.main.KeepInMemory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzml.MzMLImportTask;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.MemoryMapStorageStats;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the same sequential workload through file and converter-style stream entry points.
 */
public final class MzMLReaderBenchmark {

  /**
   * @param args input file or directory, file or stream, number of measured iterations, reader, and
   *             optional --memory, --mapped, --advanced and --verify flags
   * @throws IOException if an input cannot be read
   */
  public static void main(final @NotNull String[] args) throws IOException {
    if (args.length < 4 || !Set.of("--memory", "--mapped", "--advanced", "--verify")
        .containsAll(List.of(args).subList(4, args.length))) {
      throw new IllegalArgumentException(
          "Usage: PATH file|stream ITERATIONS aalto|woodstox|jdk [--memory] [--mapped] [--advanced] [--verify]");
    }
    final Path root = Path.of(args[0]).toAbsolutePath();
    final boolean stream = switch (args[1]) {
      case "file" -> false;
      case "stream" -> true;
      default -> throw new IllegalArgumentException("Unknown input mode " + args[1]);
    };
    final List<Path> files;
    try (var paths = Files.walk(root)) {
      files = paths.filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".mzml")).sorted().toList();
    }
    if (files.isEmpty()) {
      throw new IllegalArgumentException("No mzML input under " + root);
    }
    final int iterations = Integer.parseInt(args[2]);
    final boolean mapped = List.of(args).contains("--mapped");
    final boolean sampleMemory = List.of(args).contains("--memory");
    MZmineCore.getConfiguration();
    (mapped ? KeepInMemory.NONE : KeepInMemory.ALL).enforceToMemoryMapping();
    for (int i = 0; i <= iterations; i++) {
      long totalNanos = 0;
      for (final Path path : files) {
        final var parameters = new MSDKmzMLImportParameters().cloneParameterSet();
        try (final InputStream input = stream ? Files.newInputStream(path) : null) {
          final var config = List.of(args).contains("--advanced")
              ? AllSpectralDataImportModule.createSpectralProcessors(
              AdvancedSpectraImportParameters.create(
                  MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 3d, 3d,
                  Range.closed(100d, 1000d), new ScanSelection(1, Range.closed(30f, 35f)), true))
              : ScanImportProcessorConfig.createDefault();
          final var storage = MemoryMapStorage.forRawDataFile();
          final var project = new MZmineProjectImpl();
          final var task = new MzMLImportTask(project, path.toFile(), input, config,
              MSDKmzMLImportModule.class, parameters, Instant.EPOCH, storage,
              MzMLTestReader.valueOf(args[3].toUpperCase(Locale.ROOT)).createFactory());
          final MemorySampler sampler = sampleMemory ? MemorySampler.start() : null;
          final var before = sampleMemory ? MemoryMapStorageStats.snapshot() : null;
          final long start = System.nanoTime();
          final RawDataFile file;
          final long elapsed;
          try {
            file = task.importStreamOrFile();
            elapsed = System.nanoTime() - start;
          } finally {
            if (sampler != null && before != null) {
              final var memory = sampler.stop();
              final var after = MemoryMapStorageStats.snapshot();
              System.out.printf(Locale.ROOT, "STORAGE\t%d\t%d\t%d\t%s%n", i,
                  after.totalUsedBytes() - before.totalUsedBytes(),
                  after.totalReservedBytes() - before.totalReservedBytes(), path);
              System.out.printf(Locale.ROOT, "MEMORY\t%d\t%s\t%s\t%.2f\t%.3f\t%s%n", i, args[3],
                  storage == null ? "heap" : "mapped", memory.peakHeapGB(), memory.gcTimeSeconds(),
                  path);
            }
          }
          if (file == null) {
            throw new IllegalStateException(path + ": " + task.getErrorMessage());
          }
          try {
            totalNanos += elapsed;
            if (List.of(args).contains("--verify")) {
              // Outside the timed region. Includes each peak pair and each LC/MS precursor stage.
              long hash = 1;
              for (final Scan scan : file.getScans()) {
                hash = 31 * hash + scan.getScanNumber();
                hash = 31 * hash + Float.floatToIntBits(scan.getRetentionTime());
                hash = 31 * hash + scan.getMSLevel();
                hash = 31 * hash + precursorHash(scan.getMsMsInfo());
                for (int j = 0; j < scan.getNumberOfDataPoints(); j++) {
                  hash = 31 * hash + Double.doubleToLongBits(scan.getMzValue(j));
                  hash = 31 * hash + Double.doubleToLongBits(scan.getIntensityValue(j));
                }
              }
              System.out.printf(Locale.ROOT, "VERIFY\t%d\t%016x\t%s%n", i, hash, path);
              long tracePoints = 0;
              long traceHash = 1;
              for (final var detector : file.getOtherDataFiles()) {
                final var data = detector.getOtherTimeSeriesData();
                if (data == null) {
                  continue;
                }
                for (final var trace : data.getRawTraces()) {
                  final var series = trace.getFeatureData();
                  tracePoints += series.getNumberOfValues();
                  for (int j = 0; j < series.getNumberOfValues(); j++) {
                    traceHash = 31 * traceHash + Float.floatToIntBits(series.getRetentionTime(j));
                    traceHash = 31 * traceHash + Double.doubleToLongBits(series.getIntensity(j));
                  }
                }
              }
              System.out.printf(Locale.ROOT, "TRACES\t%d\t%d\t%016x\t%s%n", i, tracePoints,
                  traceHash, path);
            }
            long points = 0;
            for (final Scan scan : file.getScans()) {
              if (scan instanceof Frame frame) {
                points += frame.getMobilityScanStorage().getRawTotalNumPoints();
              } else {
                points += scan.getNumberOfDataPoints();
              }
            }
            System.out.printf(Locale.ROOT, "FILE\t%d\t%s\t%d\t%d\t%d\t%.6f\t%s%n", i, args[1],
                Files.size(path), file.getNumOfScans(), points, elapsed / 1e9, path);
          } finally {
            file.close();
          }
        }
      }
      // Iteration zero warms up the VM and the filesystem cache.
      System.out.printf(Locale.ROOT, "TOTAL\t%d\t%s\t%d\t%.6f%n", i, args[1], files.size(),
          totalNanos / 1e9);
    }
  }

  /**
   * @return a stable hash of the precursor fields for comparing separate JVM runs
   */
  private static long precursorHash(@Nullable final MsMsInfo precursor) {
    if (precursor == null) {
      return 0;
    }
    if (precursor instanceof MSnInfoImpl chain) {
      long hash = 1;
      for (final var stage : chain.getPrecursors()) {
        hash = 31 * hash + precursorHash(stage);
      }
      return hash;
    }
    long hash = precursor.getMsLevel();
    hash = 31 * hash + String.valueOf(precursor.getIsolationWindow()).hashCode();
    hash = 31 * hash + String.valueOf(precursor.getActivationEnergy()).hashCode();
    hash = 31 * hash + precursor.getActivationMethod().name().hashCode();
    if (precursor instanceof DDAMsMsInfo dda) {
      hash = 31 * hash + Double.doubleToLongBits(dda.getIsolationMz());
      hash = 31 * hash + String.valueOf(dda.getPrecursorCharge()).hashCode();
    }
    return hash;
  }
}
