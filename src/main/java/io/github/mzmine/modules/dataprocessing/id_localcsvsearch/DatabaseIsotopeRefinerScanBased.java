package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.FormulaUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IMolecularFormula;
import uk.ac.ebi.jmzml.model.mzml.ScanList;

public class DatabaseIsotopeRefinerScanBased {


  private static final Logger logger = Logger.getLogger(DatabaseIsotopeRefiner.class.getName());

  public static void refine(List<FeatureListRow> rows, MZTolerance mzTolerance,
      RTTolerance rtTolerance, double minIntensity, double minIsotopeScore)
      throws CloneNotSupportedException {

    final Map<RawDataFile, ScanDataAccess> accessMap = new HashMap<>();

    List<DataPoint> rowsFoundIsotope = new ArrayList<>();
    for (FeatureListRow row : rows) {
      List<CompoundDBAnnotation> annotations = row.getCompoundAnnotations();

      if (annotations.isEmpty()) {
        continue;
      }
      final Feature feature = row.getBestFeature();

//      ScanDataAccess access = accessMap.get(feature.getRawDataFile());
//      if(access == null) {
//        access = EfficientDataAccess.of(feature.getRawDataFile(), ScanDataType.CENTROID);
//        accessMap.put(feature.getRawDataFile(), access);
//      }

      final ScanDataAccess access = accessMap.computeIfAbsent(feature.getRawDataFile(),
          file -> EfficientDataAccess.of(file, ScanDataType.CENTROID));
      if (!access.jumpToScan(feature.getRepresentativeScan())) {
        continue;
      }

      float bestScore = 0;
      for (CompoundDBAnnotation annotation : annotations) {
        if (annotation.getFormula() == null || annotation.getAdductType() == null) {
          continue;
        }
        String formula = annotation.getFormula();
        IonType adductType = annotation.getAdductType();
        IMolecularFormula molecularFormula = FormulaUtils.neutralizeFormulaWithHydrogen(formula);
        assert molecularFormula != null;
        IMolecularFormula ionFormula = adductType.addToFormula(molecularFormula);
        final IsotopePattern predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(
            ionFormula, 0.01, mzTolerance.getMzToleranceForMass(row.getAverageMZ()),
            adductType.getCharge(), adductType.getPolarity(), false);
//        predictedIsotopePattern = IsotopePatternCalculator.removeDataPointsBelowIntensity(predictedIsotopePattern,
//            minIntensity);
        //searching for isotopes in representativeScan of Feature if an isotope is found,
        // FeatureListRow of isotope is deleted
        IsotopePatternMatcher isotopePatternMatcher = new IsotopePatternMatcher(
            predictedIsotopePattern, minIntensity);

        for (int i = 0; i < access.getNumberOfDataPoints(); i++) {
          final double mz = access.getMzValue(i);
          final double intensity = access.getIntensityValue(i);
          boolean foundIsotope = isotopePatternMatcher.offerDataPoint(mz, intensity,
              row.getBestFeature().getRT(), row.getBestFeature().getRT(), mzTolerance, rtTolerance);
          if (foundIsotope) {
            rowsFoundIsotope.add(new SimpleDataPoint(mz, intensity));
            logger.info("Isotope peak found for " + row.getAverageMZ() + " at m/z " + intensity);
          }
        }

        if (isotopePatternMatcher.matches()) {
          final IsotopePattern measuredIsotopePattern = isotopePatternMatcher.measuredIsotopePattern(
              mzTolerance);
          assert measuredIsotopePattern != null;

          if (IsotopePatternScoreCalculator.checkMatch(predictedIsotopePattern,
              measuredIsotopePattern, mzTolerance, 10.0, minIsotopeScore)) {
            final float isotopePatternScore = isotopePatternMatcher.score(mzTolerance);
            annotation.put(IsotopePatternType.class, measuredIsotopePattern);
            annotation.put(IsotopePatternScoreType.class, isotopePatternScore);
            if (isotopePatternScore >= bestScore) {
              bestScore = isotopePatternScore;

              Feature bestFeature = row.getBestFeature();
              ((ModularFeature) bestFeature).set(CompoundNameType.class,
                  annotation.getCompoundName());
              ((ModularFeature) bestFeature).set(IsotopePatternType.class, measuredIsotopePattern);
              ((ModularFeature) bestFeature).set(IsotopePatternScoreType.class,
                  isotopePatternScore);
              logger.info("Full isotope pattern found for m/z " + row.getAverageMZ());
            }

          }

        }

        // sort annotations by compound score
        List<CompoundDBAnnotation> compoundAnnotations = new ArrayList<>(
            row.getCompoundAnnotations());
        compoundAnnotations.sort((o1, o2) -> {
          float score1 = Objects.requireNonNullElse(o1.get(IsotopePatternScoreType.class), 0f);
          float score2 = Objects.requireNonNullElse(o2.get(IsotopePatternScoreType.class), 0f);
          return Double.compare(score1, score2) * -1;
        });
        row.setCompoundAnnotations(compoundAnnotations);


      }


    }

  }
}



