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

package io.mzmine.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Used in JasperStudio to generate example data by calling one of the static methods.
 */
public class ReportDataFactory {

  public ReportDataFactory() {

  }

  public static void main(String[] args) {
    System.out.println(getBeanCollection().toString());
  }

  public static Collection<FeatureDetail> getBeanCollection() {

    List<FeatureDetail> data = new ArrayList<>();
    for (int i = 0; i < 3; i++) {

      List<TwoColumnRow> rows = new ArrayList<>();
      rows.add(
          new TwoColumnRow("C:\\Users\\Steffen\\Documents\\report_figures\\eic.png", "Fig 1: Eic",
              "C:\\Users\\Steffen\\Documents\\report_figures\\mobilogram.png",
              "Fig 2: Mobilogram"));
      rows.add(new TwoColumnRow("C:\\Users\\Steffen\\Documents\\report_figures\\libmatch.png",
          "Fig 3: Library match", "C:\\Users\\Steffen\\Documents\\report_figures\\uv.png",
          "Fig 4: Correlation"));

      data.add(new FeatureDetail("Feature summary of " + i, String.valueOf(i),
          String.valueOf(523.2123 + 5 * i), String.valueOf(12.2 + i * 0.3),
          String.valueOf(1512 + i),
          "Exact mass: 2318\ndelta m/z: 0.005\nInternal ID: 13sijawDSA8\nCAS: 231-2365-21",
          "C:\\Users\\Steffen\\Documents\\report_figures\\structure.png", "Additional text", rows,
          new ArrayList<>()));
    }
    return data;
  }


  public static Collection<FeatureSummary> getSummary() {
    List<FeatureSummary> summaries = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      summaries.add(
          new FeatureSummary(i + "", 137 + 15.3 * i + "", 1 + 0.3 * i + "", 60 + 5.2 * i + "",
              1E5 + 1.5E4 * i + "", 1E3 + 1.5E2 * i + "", "Compound " + i, String.valueOf(1000 + i),
              0.33 + ""));
    }
    return summaries;
  }

}
