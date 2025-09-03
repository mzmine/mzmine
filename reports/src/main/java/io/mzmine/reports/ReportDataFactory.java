package io.mzmine.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReportDataFactory {

  public ReportDataFactory() {

  }

  public static Collection getBeanCollection() {

    List<FeatureDetail> data = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      data.add(new FeatureDetail(523.2123 + 5 * i, 12.2 + i * 0.3, 1512 + i, null,
          "C:\\Users\\Steffen\\Documents\\report_figures\\eic.png",
          "C:\\Users\\Steffen\\Documents\\report_figures\\mobilogram.png",
          "C:\\Users\\Steffen\\Documents\\report_figures\\libmatch.png",
          "Exact mass: 2318\ndelta m/z: 0.005\nInternal ID: 13sijawDSA8\nCAS: 231-2365-21",
          "C:\\Users\\Steffen\\Documents\\report_figures\\uv.png",
          "C:\\Users\\Steffen\\Documents\\report_figures\\structure.png"));
    }
    return data;
  }

  public static void main(String args[]) {
    System.out.println(System.getProperty("java.version"));
  }

}
