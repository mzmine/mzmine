package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopepatternpreview.IsotopePatternTableData;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import smile.math.DoubleArrayList;
import thredds.catalog2.builder.DatasetBuilder;

public class IsotopePatternMatcher {

  final double minIntensity = 0.1;
  IsotopePattern predictedIsotopePattern;
  double[] measuredMZValues;
  double[] measuredIntensities;
  double[] measuredRTs;
  boolean[] mustBeDetectedMZValues;
  String Output;


  IsotopePatternMatcher(IsotopePattern isotopePattern) {
    predictedIsotopePattern = isotopePattern;
    measuredMZValues = new double[isotopePattern.getNumberOfDataPoints()];
    measuredIntensities = new double[isotopePattern.getNumberOfDataPoints()];
    mustBeDetectedMZValues = new boolean[isotopePattern.getNumberOfDataPoints()];
    measuredRTs = new double[isotopePattern.getNumberOfDataPoints()];
    // Definition of mustBeDetected, if intensity is higher than minIntensity then mustBeDetected
    // is true
    for (int i = 0; i < isotopePattern.getNumberOfDataPoints(); i++) {
      if (predictedIsotopePattern.getIntensityValue(i) >= minIntensity) {
        mustBeDetectedMZValues[i] = true;
      }
    }
  }

  public boolean offerDataPoint(double measuredMz, double measuredIntensity, float actualRt,
      float predictedRt, MZTolerance mzTolerance, RTTolerance rtTolerance) {
    // Vergleich measuredMz bzw actualRt mit MZvalues bzw. RTs of predictedIsotopePattern unter
    // berücksichtigung eines MZ Toleranzbereiches, wenn die Werte einander entsprechen,
    // return true und MZs& Intensities werden in measuredMZvalues&measuredIntensities gespeichert,
    // wenn beides matched
    for (int i = 0; i < predictedIsotopePattern.getNumberOfDataPoints(); i++) {
      if (mzTolerance.checkWithinTolerance(measuredMz, predictedIsotopePattern.getMzValue(i))) {
        if (rtTolerance.checkWithinTolerance(actualRt, predictedRt)) {
          measuredMZValues[i] = measuredMz;
          measuredIntensities[i] = measuredIntensity;
          measuredRTs[i] = actualRt;
          return true;
        }
      }
    }
    return false;
  }


  public boolean matches() {
    // Überprüfe ob alle Werte von mustBeDetected in measuredMZValues vorkommen und ob RTs
    // übereinstimmen, dann return true oder false
    int numberOfMatches = 0;
    for (int i = 0; i < mustBeDetectedMZValues.length; i++) {
      if (mustBeDetectedMZValues[i]) {
        if (measuredIntensities[i] == 0d) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * @return The detected isotope pattern, if all necessary peaks were detected. Otherwise null.
   */
  @Nullable
  public IsotopePattern measuredIsotopePattern() {
    if (!matches()) {
      return null;

    }
    DoubleArrayList mzs = new DoubleArrayList();
    DoubleArrayList intensities = new DoubleArrayList();
    for (int i = 0; i < measuredMZValues.length; i++) {
      if (measuredIntensities[i] != 0 && measuredMZValues[i] != 0) {
        mzs.add(measuredMZValues[i]);
        intensities.add(measuredIntensities[i]);
      }
    }

    return new SimpleIsotopePattern(mzs.toArray(), intensities.toArray(),
        IsotopePatternStatus.DETECTED, predictedIsotopePattern.getDescription());
  }

  //new isotopePattern = measuredIsotopePattern
  //List<double> measuredMZValuesList = Arrays.asList(measuredMZValues);
  //DatasetBuilder measuredIsotopePattern = new Dataset (measuredMZValues, measuredIntensities);
  //MassSpectrum measuredIsotopeMassSpectrum = new MassSpectrum (measuredMZValuesList, measuredIntensities);
  //IsotopePattern measuredIsotopePatter = new IsotopePattern (measuredIsotopeMassSpectrum);


}


