/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_msmsquality;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetectorParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSIntensityScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class MsMsQualityExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MsMsQualityExportTask.class.getName());

  private final FeatureList[] featureLists;
  private final File exportFile;
  private final String separator = ";";
  final int numRows;

  private final Map<IMSRawDataFile, MobilityScanDataAccess> mobScanAccessMap = new HashMap<>();
  private final MZTolerance msmsFormulaTolerance;
  private final boolean annotatedOnly = true;
  private final ParameterSet parameterSet;
  private final ParameterSet folParams;
  private int processedRows;

  private final MassDetector factorOfLowest = MassDetectionParameters.factorOfLowest;

  final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
  private final boolean matchCompoundToFlist;

  protected MsMsQualityExportTask(@NotNull Instant moduleCallDate, ParameterSet parameterSet) {
    super(null, moduleCallDate);
    this.parameterSet = parameterSet;

    msmsFormulaTolerance = parameterSet.getValue(MsMsQualityExportParameters.formulaTolerance);
    featureLists = parameterSet.getValue(MsMsQualityExportParameters.flists)
        .getMatchingFeatureLists();
    exportFile = parameterSet.getValue(MsMsQualityExportParameters.file);
    matchCompoundToFlist = parameterSet.getValue(
        MsMsQualityExportParameters.matchCompoundNameToFlist);

    folParams = MZmineCore.getConfiguration().getModuleParameters(FactorOfLowestMassDetector.class)
        .cloneParameterSet();
    folParams.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, 1d);

    numRows = Arrays.stream(featureLists).mapToInt(FeatureList::getNumberOfRows).sum();
  }

  @Override
  public String getTaskDescription() {
    return "Rating MSMS spectra of row " + processedRows + "/" + numRows;
  }

  @Override
  public double getFinishedPercentage() {
    return processedRows / (double) numRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile))) {
      writer.write(
          String.join(separator, "feature_list", SpectrumMsMsQuality.getHeader(separator), "spot"));
      writer.newLine();

      for (FeatureList featureList : featureLists) {

        for (FeatureListRow row : featureList.getRows()) {
          final ModularFeature feature = (ModularFeature) row.getBestFeature();
          final RawDataFile file = feature.getRawDataFile();

          final FeatureAnnotation annotation = FeatureUtils.getBestFeatureAnnotation(row);
          final String formula = annotation != null ? annotation.getFormula() : null;

          if (annotatedOnly && formula == null) {
            processedRows++;
            continue;
          }

          if (matchCompoundToFlist && (annotation.getCompoundName() == null
              || !featureList.getName() // annotations may have unsafe characters, flists not
              .contains(FileAndPathUtil.safePathEncode(annotation.getCompoundName())))) {
            processedRows++;
            continue;
          }

          if (FeatureUtils.isImsFeature(feature)) {
            processImsFeature(writer, featureList, feature, annotation, formula);
          }
          processedRows++;
        }

        featureList.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(MsMsQualityExportModule.class, parameterSet,
                getModuleCallDate()));
      }
    } catch (IOException e) {
      logger.warning(() -> "Error exporting feature list quality.");
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processImsFeature(BufferedWriter writer, FeatureList featureList,
      ModularFeature feature, FeatureAnnotation annotation, String formula) throws IOException {
    final IMSRawDataFile imsFile = (IMSRawDataFile) feature.getRawDataFile();
    final MobilityScanDataAccess mobScanAccess = mobScanAccessMap.computeIfAbsent(imsFile,
        f -> new MobilityScanDataAccess(f, MobilityScanDataType.CENTROID,
            (List<Frame>) f.getFrames(1)));

    for (Scan msmsScan : feature.getAllMS2FragmentScans()) {
      final MergedMsMsSpectrum mergedMsMs = (MergedMsMsSpectrum) msmsScan;
      final SpectrumMsMsQuality quality = getImsMsMsQuality(feature, formula, mobScanAccess,
          msmsScan, mergedMsMs, annotation);

      writer.write(featureList.getName() + separator);
      writer.write(quality.toCsvString(separator));
      writer.write(String.format("%s%.1f", separator, mergedMsMs.getCollisionEnergy()));
      /*if (feature.get(MaldiSpotType.class) != null) {
        writer.write(separator + feature.get(MaldiSpotType.class));
      }*/
      writer.newLine();
    }
  }

  private SpectrumMsMsQuality getImsMsMsQuality(ModularFeature feature, String formula,
      MobilityScanDataAccess mobScanAccess, Scan msmsScan, MergedMsMsSpectrum mergedMsMs,
      FeatureAnnotation annotation) {

    final PasefMsMsInfo info = (PasefMsMsInfo) mergedMsMs.getMsMsInfo();
    final Double window = RangeUtils.rangeLength(mergedMsMs.getMsMsInfo().getIsolationWindow());

    mobScanAccess.jumpToFrame((Frame) feature.getRepresentativeScan());
    final double isolationChimerityScore = IonMobilityUtils.getIsolationChimerityScore(
        mergedMsMs.getPrecursorMz(), mobScanAccess,
        RangeUtils.rangeAround(mergedMsMs.getPrecursorMz().doubleValue(), window * 2),
        info.getMobilityRange());

    MSMSScore peakFormulaScore = new MSMSScore(0f, null);
    MSMSScore intensityFormulaScore = new MSMSScore(0f, null);

    if (formula != null) {
      final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
          formula, SilentChemObjectBuilder.getInstance());
      try {
        FormulaUtils.replaceAllIsotopesWithoutExactMass(molecularFormula);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      final IonType adductType = annotation.getAdductType();
      if (adductType.getCDKFormula() != null) {
        molecularFormula.add(adductType.getCDKFormula());
      }

      final double[][] filtered = factorOfLowest.getMassValues(msmsScan, folParams);
      final DataPoint[] dataPoints = DataPointUtils.getDataPoints(filtered[0], filtered[1]);

      peakFormulaScore = MSMSScoreCalculator.evaluateMSMS(msmsFormulaTolerance, molecularFormula,
          dataPoints, msmsScan.getPrecursorMz(), msmsScan.getPrecursorCharge(), dataPoints.length);
      intensityFormulaScore = MSMSIntensityScoreCalculator.evaluateMSMS(msmsFormulaTolerance,
          molecularFormula, dataPoints, msmsScan.getPrecursorMz(), msmsScan.getPrecursorCharge(),
          dataPoints.length);
    }

    return new SpectrumMsMsQuality((float) isolationChimerityScore, intensityFormulaScore,
        peakFormulaScore, msmsScan.getNumberOfDataPoints(),
        (float) ScanUtils.getSpectralEntropy(msmsScan),
        (float) ScanUtils.getNormalizedSpectralEntropy(msmsScan),
        (float) ScanUtils.getWeightedSpectralEntropy(msmsScan),
        (float) ScanUtils.getNormalizedWeightedSpectralEntropy(msmsScan), annotation);
  }
}
