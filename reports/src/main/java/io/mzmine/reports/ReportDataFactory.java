package io.mzmine.reports;

import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReportDataFactory {

  public ReportDataFactory() {

  }

  public static Collection<FeatureDetail> getBeanCollection() {

    List<FeatureDetail> data = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      data.add(new FeatureDetail("Feature summary of " + i, String.valueOf(i),
          String.valueOf(523.2123 + 5 * i), String.valueOf(12.2 + i * 0.3),
          String.valueOf(1512 + i),
          "Exact mass: 2318\ndelta m/z: 0.005\nInternal ID: 13sijawDSA8\nCAS: 231-2365-21",
          "C:\\Users\\Steffen\\Documents\\report_figures\\structure.png",
          "C:\\Users\\Steffen\\Documents\\report_figures\\eic.png",
          "Fig 1: Eic",
          "C:\\Users\\Steffen\\Documents\\report_figures\\mobilogram.png",
          "Fig 2: Mobilogram",
          "C:\\Users\\Steffen\\Documents\\report_figures\\libmatch.png", "Fig 3: Library match",
          "C:\\Users\\Steffen\\Documents\\report_figures\\uv.png", "Fig 4: Correlation",
          null, null, "Additional text"));
    } return data;
  }

  public static Collection<FeatureSummary> getSummary() {
    List<FeatureSummary> summaries = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      summaries.add(
          new FeatureSummary(i + "", 137 + 15.3 * i + "", 1 + 0.3 * i + "", 60 + 5.2 * i + "", 1E5 + 1.5E4 * i + "",
              1E3 + 1.5E2 * i + "", "Compound " + i, String.valueOf(1000 + i), 0.33 + ""));
    }
    return summaries;
  }

  public static void main(String args[]) {
    System.out.println(System.getProperty("java.version"));
  }

}
