package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.featuredata.MzSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.naming.Name;
import org.graphstream.algorithm.generator.PointsOfInterestGenerator.Parameter;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.relaxng.datatype.DatatypeBuilder;

public class DatabaseIsotopeRefiner {

  private static final Logger logger = Logger.getLogger(DatabaseIsotopeRefiner.class.getName());

  public static void refine(List<FeatureListRow> rows, MZTolerance mzTolerance,
      RTTolerance rtTolerance, double minIntensity, double minIsotopeScore)
      throws CloneNotSupportedException {
    List<FeatureListRow> rowsFoundIsotope = new ArrayList<>();
    for (FeatureListRow row : rows) {
      List<CompoundDBAnnotation> annotations = row.getCompoundAnnotations();

      if (annotations.isEmpty()) {
        continue;
      }

      for (CompoundDBAnnotation annotation : annotations) {

        if (annotation.getFormula() == null || annotation.getAdductType() == null) {
          continue;
        }
        String formula = annotation.getFormula();
        IonType adductType = annotation.getAdductType();
        IMolecularFormula molecularFormula = FormulaUtils.neutralizeFormulaWithHydrogen(formula);
        assert molecularFormula != null;
        IMolecularFormula ionFormula = adductType.addToFormula(molecularFormula);
        IsotopePattern isotopePattern = IsotopePatternCalculator.calculateIsotopePattern(ionFormula,
            minIntensity, mzTolerance.getMzToleranceForMass(row.getAverageMZ()),
            adductType.getCharge(), adductType.getPolarity(), false);
        isotopePattern = IsotopePatternCalculator.removeDataPointsBelowIntensity(isotopePattern,
            minIntensity);

        //searching for isotopes in FeatureList, if an isotope is found,
        // FeatureListRow of isotope is deleted
        IsotopePatternMatcher isotopePatternMatcher = new IsotopePatternMatcher(isotopePattern,
            minIntensity);
        for (FeatureListRow rowB : rows) {
          boolean foundIsotope = isotopePatternMatcher.offerDataPoint(rowB.getBestFeature().getMZ(),
              rowB.getBestFeature().getHeight(), rowB.getBestFeature().getRT(),
              row.getBestFeature().getRT(), mzTolerance, rtTolerance);
          if (foundIsotope) {
            rowsFoundIsotope.add(rowB);
            logger.info(
                "Isotope peak found for " + row.getAverageMZ() + " at m/z " + rowB.getAverageMZ());
          }
        }
        if (isotopePatternMatcher.matches()) {
          if (IsotopePatternScoreCalculator.checkMatch(isotopePattern,
              Objects.requireNonNull(isotopePatternMatcher.measuredIsotopePattern(mzTolerance)), mzTolerance,
              1000.0, minIsotopeScore)) {
            float isotopePatternScore = IsotopePatternScoreCalculator.getSimilarityScore(
                isotopePattern,
                Objects.requireNonNull(isotopePatternMatcher.measuredIsotopePattern(mzTolerance)), mzTolerance,
                1000.0);
            annotation.put(IsotopePatternScoreType.class, isotopePatternScore);
            Feature bestFeature = row.getBestFeature();
            ((ModularFeature) bestFeature).set(IsotopePatternType.class,
                isotopePatternMatcher.measuredIsotopePattern(mzTolerance));
            logger.info("Full isotope pattern found for m/z " + row.getAverageMZ());
          }
        }

      }

    }
    /**
     *- annotations leer oder was drin?
     *  - wenn nicht -> continue;
     *  - database hits auslesen
     *  - formel auslesen (neutrale formel)
     *  - addukt auslesen (ionisation)
     *  - addukt auf formel anwenden
     *  - isotopenmuster berechnen
     *  - restlichen rows prüfen auf:
     *    - gleiche rt, erwartete mz
     *    - ergebnis speichern
     *    - score berechnen?
     *    - rows die ein isotop sind rauslöschen?
     *    - rows als isotopenmuster setzen (zur hauptrow)
     */

/*      CompoundDBAnnotation annotation;
      annotation.getFormula();
      MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formel, SilentChemObjectBuilder.getInstance());
      IonType adductType = annotation.getAdductType();
      adductType.addToFormula();
      IsotopePatternCalculator.calculateIsotopePattern(molFormula, 0.01, formula.getCharge(), Polarity);
      rows.get(i).getAverageMZ();
      rows.get(i).getAverageRT();*/
  }
}

