/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Reproducible persistence measurement for compact row factors and the full applied parameter
 * payload. Invoke with {@code --large} only on a machine provisioned for the stated 4 GiB/60 s
 * persistence budget; the default control case is deliberately small.
 */
public final class NormalizationPersistenceBenchmark {

  private NormalizationPersistenceBenchmark() {}

  public static void main(final String[] args) throws Exception {
    final boolean large = List.of(args).contains("--large");
    final int rows = large ? 100_000 : 1_000;
    final int files = large ? 100 : 4;
    ManagementFactory.getMemoryPoolMXBeans().forEach(MemoryPoolMXBean::resetPeakUsage);
    final long constructionStart = System.nanoTime();
    final BenchmarkData data = createSummary(rows, files);
    final long constructed = System.nanoTime();
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary, data.summary());
    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final long saved = System.nanoTime();
    final IntensityNormalizerParameters loaded = new IntensityNormalizerParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);
    final long loadedAt = System.nanoTime();
    if (!data.summary().equals(loaded.getValue(
        IntensityNormalizerParameters.hiddenNormalizationSummary))) {
      throw new IllegalStateException("Persistence roundtrip changed row factors or unavailable reasons.");
    }
    System.out.printf("rows=%d files=%d factors=%d unavailable=%d nodes=%d xmlBytes=%d gzipXmlBytes=%d "
            + "constructMs=%.1f saveMs=%.1f loadMs=%.1f peakHeapMiB=%.1f%n", rows, files,
        data.factors(), data.unavailable(),
        countElements(xml), xml.getBytes(StandardCharsets.UTF_8).length, gzipBytes(xml),
        (constructed - constructionStart) / 1_000_000d, (saved - constructed) / 1_000_000d,
        (loadedAt - saved) / 1_000_000d, peakHeapBytes() / 1024d / 1024d);
  }

  private static BenchmarkData createSummary(final int rows, final int files) {
    final List<RawFileNormalizationFunction> functions = new ArrayList<>(files);
    long factorCount = 0;
    long unavailableCount = 0;
    for (int file = 0; file < files; file++) {
      final Map<Integer, Double> factors = new LinkedHashMap<>(rows);
      final Map<Integer, String> unavailableReasons = new LinkedHashMap<>();
      for (int row = 1; row <= rows; row++) {
        if (row % 10 == 0) unavailableReasons.put(row, "missing QC");
        else factors.put(row, 1d + file * 1e-6 + row * 1e-12);
      }
      factorCount += factors.size();
      unavailableCount += unavailableReasons.size();
      functions.add(new RawFileNormalizationFunction(new RawDataFilePlaceholder("file-" + file, null),
          LocalDateTime.of(2026, 1, 1, 0, 0), new FeatureRowNormalizationFunction(factors,
          unavailableReasons)));
    }
    return new BenchmarkData(new IntensityNormalizationSummary(functions), factorCount,
        unavailableCount);
  }

  private static int countElements(final String xml) {
    int count = 0;
    for (int index = 0; (index = xml.indexOf('<', index)) >= 0; index++) if (index + 1 < xml.length()
        && Character.isLetter(xml.charAt(index + 1))) count++;
    return count;
  }

  private static long gzipBytes(final String xml) throws Exception {
    try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
      gzip.write(xml.getBytes(StandardCharsets.UTF_8));
      gzip.finish();
      return bytes.size();
    }
  }

  private static long peakHeapBytes() {
    return ManagementFactory.getMemoryPoolMXBeans().stream()
        .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP)
        .mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
  }

  private record BenchmarkData(IntensityNormalizationSummary summary, long factors,
                               long unavailable) {}
}
