package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;

public record MrmTransition(double q1mass, double q3mass,
                            IonTimeSeries<? extends Scan> chromatogram) {

  @Override
  public String toString() {
    return "%.2f -> %.2f".formatted(q1mass, q3mass);
  }
}
