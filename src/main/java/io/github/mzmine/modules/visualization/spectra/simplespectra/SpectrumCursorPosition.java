package io.github.mzmine.modules.visualization.spectra.simplespectra;

import io.github.mzmine.datamodel.Scan;

public class SpectrumCursorPosition {

  final double intensity;
  final double mz;
  final Scan scan;

  public SpectrumCursorPosition(double intensity, double mz,
      Scan scan) {
    this.intensity = intensity;
    this.mz = mz;
    this.scan = scan;
  }

  public double getIntensity() {
    return intensity;
  }

  public double getMz() {
    return mz;
  }

  public Scan getScan() {
    return scan;
  }
}
