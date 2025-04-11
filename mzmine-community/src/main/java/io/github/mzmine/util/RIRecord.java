/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This holds information from the RI field in spectral libraries for gas chromatography The RI
 * field is of form "s=RI/CI/n_samples n=RI/CI/n_samples p=RI/CI/n_samples" (CI denotes confidence
 * interval) s denotes semipolar, n denotes nonpolar, and p denotes polar If there are 0 samples for
 * s or n or p, then that part is skipped If there is one sample, then n_samples and CI are skipped
 * Unclear if non-NIST other formats differ
 */
public class RIRecord {

  private static final Logger logger = Logger.getLogger(RIRecord.class.getName());

  private static final Map<String, RIColumn> stringsToColumnTypesMap = Map.ofEntries(
      entry("s=", RIColumn.SEMIPOLAR), entry("SemiStdNP=", RIColumn.SEMIPOLAR),
      entry("n=", RIColumn.NONPOLAR), entry("StdNP=", RIColumn.NONPOLAR),
      entry("p=", RIColumn.POLAR), entry("Polar=", RIColumn.POLAR), entry("a=", RIColumn.DEFAULT));
  
  // uses list instead of map to lower the memory footprint.
  // small list with few items is better than hashmap and still fast in lookup
  private final List<RIRecordPart> records;

  public RIRecord(String record) {
    this.records = parse(record);
  }

  private static List<RIRecordPart> parse(String line) {
    String[] records = line.split("\\s+");

    ArrayList<RIRecordPart> parsedRecords = new ArrayList<>();

    for (String record : records) {
      try {
        // Record is just a number, equivalent to a={number}
        parsedRecords.add(new RIRecordPart(RIColumn.DEFAULT, Float.parseFloat(record), null, null));
      } catch (NumberFormatException ne) {
        String discoveredKey = null;
        RIColumn currentType = null;

        for (Map.Entry<String, RIColumn> entry : stringsToColumnTypesMap.entrySet()) {
          String key = entry.getKey();
          // Converting keys to lower-case is slower but the map is more readable
          if (record.toLowerCase().startsWith(key.toLowerCase())) {
            discoveredKey = key;
            currentType = entry.getValue();
            break;
          }

        }

        if (discoveredKey != null) {
          try {
            String[] recordValues = record.substring(discoveredKey.length()).split("/");
            if (recordValues.length > 0) {
              // RI should never be null
              final Float ri = recordValues[0] != null ? Float.parseFloat(recordValues[0]) : null;
              if (ri == null) {
                continue;
              }

              Float ci = null;
              Integer count = null;

              if (recordValues.length > 2 && recordValues[1] != null && recordValues[2] != null) {
                ci = Float.parseFloat(recordValues[1]);
                count = Integer.parseInt(recordValues[2]);
              }
              parsedRecords.add(new RIRecordPart(currentType, ri, ci, count));
            }
          } catch (Exception e) {
            logger.warning("Failed to parse RI record: " + line);
          }
        }

      }
    }
    parsedRecords.trimToSize(); // save memory
    // sort to simplify toString
    parsedRecords.sort(Comparator.comparingInt(p -> p.column.ordinal()));
    return parsedRecords;
  }

  public String toString() {
    // TODO previous implementation did not add the DEFAULT column type to the string. now it does
    return records.stream().map(RIRecordPart::toString).collect(Collectors.joining(" "));
  }

  public Float getRI(RIColumn type) {
    int defaultIndex = -1;
    for (int i = 0; i < records.size(); i++) {
      final RIRecordPart part = records.get(i);
      if (part.column == type) {
        return part.ri();
      } else if (part.column == RIColumn.DEFAULT) {
        defaultIndex = i;
      }
    }
    return defaultIndex == -1 ? null : records.get(defaultIndex).ri();
  }

  /**
   * @param ri
   * @param ci    confidence interval
   * @param count
   */
  protected record RIRecordPart(RIColumn column, float ri, Float ci, Integer count) {

    @Override
    public String toString() {
      String base = "%s=%f".formatted(column.getShortDefinition(), ri);
      if (ci != null && count != null) {
        return base + "/%f/%d".formatted(ci, count);
      }
      return base;
    }
  }

}


