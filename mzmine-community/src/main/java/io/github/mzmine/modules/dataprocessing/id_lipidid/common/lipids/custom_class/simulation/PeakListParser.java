package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Reads a pasted MS2 peak list. Supports one peak per line with m/z and intensity separated by
 * spaces, tabs, commas or semicolons (e.g., copied from mzmine, a spreadsheet or an MGF file) and
 * the compact "mz:intensity mz:intensity" format of spectral libraries. A line with only an m/z gets
 * the intensity 100.
 */
public final class PeakListParser {

  private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?(?:[eE][-+]?\\d+)?");
  private static final Pattern COLON_PAIR = Pattern.compile(
      "(\\d+(?:\\.\\d+)?)\\s*:\\s*(\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)");

  private PeakListParser() {
  }

  /**
   * @param text pasted peak list
   * @return the peaks sorted by m/z, lines without numbers like headers are skipped
   */
  public static @NotNull DataPoint[] parse(@NotNull String text) {
    final List<DataPoint> peaks = new ArrayList<>();
    for (final String line : text.split("\\R")) {
      final String trimmed = line.strip();
      if (trimmed.isEmpty() || !Character.isDigit(trimmed.charAt(0))) {
        // decision: headers and MGF keys like PEPMASS= are not peaks
        continue;
      }
      final Matcher pairs = COLON_PAIR.matcher(trimmed);
      boolean foundPair = false;
      while (pairs.find()) {
        foundPair = true;
        peaks.add(new SimpleDataPoint(Double.parseDouble(pairs.group(1)),
            Double.parseDouble(pairs.group(2))));
      }
      if (foundPair) {
        continue;
      }
      // decision: a comma is a decimal separator only if the line also uses tabs or spaces
      final String normalized = trimmed.matches(".*[\\t ].*") ? trimmed.replace(',', '.')
          : trimmed.replace(',', ' ');
      final Matcher numbers = NUMBER.matcher(normalized.replace(';', ' '));
      final List<Double> values = new ArrayList<>();
      while (numbers.find() && values.size() < 2) {
        values.add(Double.parseDouble(numbers.group()));
      }
      if (!values.isEmpty() && values.getFirst() > 0) {
        peaks.add(new SimpleDataPoint(values.getFirst(), values.size() > 1 ? values.get(1) : 100));
      }
    }
    peaks.sort(Comparator.comparingDouble(DataPoint::getMZ));
    return peaks.toArray(DataPoint[]::new);
  }
}
