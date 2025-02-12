/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Map.entry;


// This holds information from the RI field in spectral libraries for gas chromatography
//   The RI field is of form "s=RI/CI/n_samples n=RI/CI/n_samples p=RI/CI/n_samples" (CI denotes confidence interval)
//   s denotes semipolar, n denotes nonpolar, and p denotes polar
//   If there are 0 samples for s or n or p, then that part is skipped
//   If there is one sample, then n_samples and CI are skipped
//   Unclear if non-NIST other formats differ


public class RIRecord
{
    private static final Logger logger = Logger.getLogger(RIRecord.class.getName());
    private Map<RIColumn, RIRecordPart> map;
    static private final Map<String, RIColumn> stringsToColumnTypesMap = Map.ofEntries(
        entry("s=", RIColumn.SEMIPOLAR),
        entry("SemiStdNP=", RIColumn.SEMIPOLAR),
        entry("n=", RIColumn.NONPOLAR),
        entry("StdNP=", RIColumn.NONPOLAR),
        entry("p=", RIColumn.POLAR),
        entry("Polar=", RIColumn.POLAR),
        entry("a=", RIColumn.DEFAULT)
    );

    public RIRecord(Map<RIColumn, RIRecordPart> map) {
        this.map = map;
    }

    public RIRecord(String record) {
        this.map = parse(record);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean hasPrev = false;

        if (map.containsKey(RIColumn.SEMIPOLAR)) {
            Integer s_ri = map.get(RIColumn.SEMIPOLAR).ri();
            Integer s_count = map.get(RIColumn.SEMIPOLAR).count();
            Integer s_ci = map.get(RIColumn.SEMIPOLAR).ci();

            // s_ri != null should always be true
            sb.append(s_ri != null ? String.format("s=%d", s_ri) : "");
            sb.append((s_ri != null && s_ci != null && s_count != null ) ? String.format("/%d/%d", s_ci, s_count) : "");
            hasPrev = s_ri != null;
        }

        if (map.containsKey(RIColumn.NONPOLAR)) {
            Integer n_ri = map.get(RIColumn.NONPOLAR).ri();
            Integer n_count = map.get(RIColumn.NONPOLAR).count();
            Integer n_ci = map.get(RIColumn.NONPOLAR).ci();

            // n_ri != null should always be true
            sb.append(hasPrev && n_ri != null ? " " : "");
            sb.append(n_ri != null ? String.format("n=%d", n_ri) : "");
            sb.append((n_ri != null && n_ci != null && n_count != null ) ? String.format("/%d/%d", n_ci, n_count) : "");
            hasPrev = hasPrev || n_ri != null;
        }


        if (map.containsKey(RIColumn.POLAR)) {
            Integer p_ri = map.get(RIColumn.POLAR).ri();
            Integer p_count = map.get(RIColumn.POLAR).count();
            Integer p_ci = map.get(RIColumn.POLAR).ci();

            // n_ri != null should always be true
            sb.append(hasPrev && p_ri != null ? " " : "");
            sb.append(p_ri != null ? String.format("n=%d", p_ri) : "");
            sb.append((p_ri != null && p_ci != null&& p_count != null ) ? String.format("/%d/%d", p_ci, p_count) : "");
            hasPrev = hasPrev || p_ri != null;
        }

        return sb.toString();
    }
    private Map<RIColumn, RIRecordPart> parse(String line) {
        String[] records = line.split("\\s+");

        Map<RIColumn, RIRecordPart> recordsMap = new EnumMap<>(RIColumn.class);

        for(String record : records) {
            try {
                Integer.parseInt(record);
                recordsMap.put(RIColumn.DEFAULT, new RIRecordPart(Integer.parseInt(record), null, null));
            } catch (NumberFormatException ne)  {
                String discoveredKey = null;
                RIColumn currentType = null;

                for (Map.Entry<String, RIColumn> entry : stringsToColumnTypesMap.entrySet()) {
                    String key = entry.getKey();
                    // Converting keys to lower-case is slower but the map is more readable
                    if(record.toLowerCase().startsWith(key.toLowerCase())) {
                        discoveredKey = key;
                        currentType = entry.getValue();
                        break;
                    }

                }

                if (discoveredKey != null) {
                    try {
                        String[] recordValues = record.substring(discoveredKey.length()).split("/");
                        if (recordValues.length > 0) {
                            recordsMap.put(currentType, new RIRecordPart(
                                recordValues[0] != null ? Integer.parseInt(recordValues[0]) : null,
                                recordValues.length > 2 && recordValues[1] != null && recordValues[2] != null ? Integer.parseInt(recordValues[1]) : null,
                                recordValues.length > 2 && recordValues[1] != null && recordValues[2] != null ? Integer.parseInt(recordValues[2]) : null));
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to parse RI record: " + line);
                    }
                }

            }





        }
        return recordsMap;
    }

    public Integer getRI(RIColumn type) {
        if (map.containsKey(type)) {
            return map.get(type).ri();
        }
        else if (map.containsKey(RIColumn.DEFAULT)) {
            return map.get(RIColumn.DEFAULT).ri();
        }
        else {
            return null;
        }
    }

    protected record RIRecordPart(Integer ri, Integer ci, Integer count) {
    }

}


