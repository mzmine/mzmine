package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;

/**
 * @param chromatogram The individidual transitions. Important note: the masses in the
 *                     {@link IonTimeSeries} must be the q1 mass.
 */
public record MrmTransition(double q1mass, double q3mass,
                            IonTimeSeries<? extends Scan> chromatogram) {

  public MrmTransition(double q1mass, double q3mass, IonTimeSeries<? extends Scan> chromatogram) {
    this.q1mass = q1mass;
    this.q3mass = q3mass;
    this.chromatogram = chromatogram;

    checkChromatogramMasses(q1mass, chromatogram);
  }

  private static void checkChromatogramMasses(double q1mass, IonTimeSeries<? extends Scan> chromatogram) {
    if (chromatogram.getNumberOfValues() != 0
        && Double.compare(chromatogram.getMZ(0), q1mass) != 0) {
      // this must be the case to not alter the feature m/z if the {@link FeatureDataType} is updated.
      throw new IllegalArgumentException(
          "The m/zs in this chromatogram are not equal to the q1 mass.");
    }
  }

  @Override
  public String toString() {
    return "%.2f -> %.2f".formatted(q1mass, q3mass);
  }

  public MrmTransition with(IonTimeSeries<? extends Scan> chromatogram) {
    checkChromatogramMasses(q1mass, chromatogram);
    return new MrmTransition(q1mass, q3mass, chromatogram);
  }

  public boolean sameIsolations(MrmTransition other) {
    return Double.compare(q1mass, other.q1mass) == 0 && Double.compare(q3mass, other.q3mass) == 0;
  }
}
