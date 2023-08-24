/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
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
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore.Result;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.ParsingUtils;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class MsMsQualityExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MsMsQualityExportTask.class.getName());
  final int numRows;
  final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
  private final FeatureList[] featureLists;
  private final File exportFile;
  private final String separator = ";";
  private final Map<IMSRawDataFile, MobilityScanDataAccess> mobScanAccessMap = new HashMap<>();
  private final MZTolerance msmsFormulaTolerance;
  private final boolean annotatedOnly;
  private final ParameterSet parameterSet;
  private final ParameterSet folParams;
  private final MassDetector factorOfLowest = MassDetectionParameters.factorOfLowest;
  private final boolean matchCompoundToFlist;
  private int processedRows;

  protected MsMsQualityExportTask(@NotNull Instant moduleCallDate, ParameterSet parameterSet) {
    super(null, moduleCallDate);
    this.parameterSet = parameterSet;

    msmsFormulaTolerance = parameterSet.getValue(MsMsQualityExportParameters.formulaTolerance);
    featureLists = parameterSet.getValue(MsMsQualityExportParameters.flists)
        .getMatchingFeatureLists();
    exportFile = parameterSet.getValue(MsMsQualityExportParameters.file);
    matchCompoundToFlist = parameterSet.getValue(
        MsMsQualityExportParameters.matchCompoundNameToFlist);
    annotatedOnly = parameterSet.getValue(MsMsQualityExportParameters.onlyCompoundAnnotated);

    folParams = MZmineCore.getConfiguration().getModuleParameters(FactorOfLowestMassDetector.class)
        .cloneParameterSet();
    folParams.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, 1d);

    numRows = Arrays.stream(featureLists).mapToInt(FeatureList::getNumberOfRows).sum();
  }

  private static double getChimerityScore(ModularFeature feature,
      MobilityScanDataAccess mobScanAccess, MergedMsMsSpectrum mergedMsMs, PasefMsMsInfo info,
      Double window) {
    final List<String> spotNames = getSpotNames(mergedMsMs);
    double isolationPurityScore = 0d;
    int numMs1 = 0;
    if (!spotNames.isEmpty()) {
      for (String spotName : spotNames) {
        final Frame ms1Frame = mobScanAccess.getEligibleFrames().stream().filter(
            f -> f instanceof ImagingFrame img && img.getMaldiSpotInfo() != null
                && img.getMaldiSpotInfo().spotName().contains(spotName)).findFirst().orElse(null);
        if (ms1Frame != null) {
          isolationPurityScore += getPurityForFrame(feature, mobScanAccess, ms1Frame, info, window);
          numMs1++;
        }
      }
    } else {
      final Frame ms1Frame = (Frame) feature.getRepresentativeScan();
      isolationPurityScore += getPurityForFrame(feature, mobScanAccess, ms1Frame, info, window);
      numMs1++;
    }
    return isolationPurityScore / numMs1;
  }

  @NotNull
  private static List<String> getSpotNames(MergedMsMsSpectrum mergedMsMs) {
    final List<String> spotNames = mergedMsMs.getSourceSpectra().stream()
        .<String>mapMulti((s, c) -> {
          if (s instanceof MobilityScan ms && ms.getFrame() instanceof ImagingFrame img
              && img.getMaldiSpotInfo() != null) {
            c.accept(img.getMaldiSpotInfo().spotName());
          }
        }).distinct().toList();
    return spotNames;
  }

  /**
   * The isolation purity for the feature in the selected frame.
   *
   * @param feature       The feature.
   * @param mobScanAccess A mobility scan data access. Must contain the selected frame.
   * @param frame         The frame to check.
   * @param info          The ms2 isolation info
   * @param window        The window size around the precursor m/z to check.
   * @return The purity (sum of precursor intensity divided by full intensity in the isolation
   * window)
   */
  private static double getPurityForFrame(ModularFeature feature,
      MobilityScanDataAccess mobScanAccess, Frame frame, PasefMsMsInfo info, Double window) {
    mobScanAccess.jumpToFrame((Frame) frame);
    final double isolationPurityScore = IonMobilityUtils.getPurityInMzAndMobilityRange(
        feature.getMZ(), mobScanAccess, RangeUtils.rangeAround(feature.getMZ(), window),
        info.getMobilityRange(), true);
    return isolationPurityScore;
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
          String.join(separator, "feature_list", SpectrumMsMsQuality.getHeader(separator)));
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
      logger.log(Level.WARNING, "Error exporting feature list quality.", e);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processImsFeature(BufferedWriter writer, FeatureList featureList,
      ModularFeature feature, FeatureAnnotation annotation, String formula) throws IOException {
    final IMSRawDataFile imsFile = (IMSRawDataFile) feature.getRawDataFile();
    final MobilityScanDataAccess mobScanAccess = mobScanAccessMap.computeIfAbsent(imsFile,
        f -> new MobilityScanDataAccess(f, MobilityScanDataType.MASS_LIST,
            (List<Frame>) f.getFrames(1)));

    for (Scan msmsScan : feature.getAllMS2FragmentScans()) {
      final MergedMsMsSpectrum mergedMsMs = (MergedMsMsSpectrum) msmsScan;
      if (mergedMsMs.getMergingType() != MergingType.PASEF_SINGLE) {
        continue;
      }
      final SpectrumMsMsQuality quality = getImsMsMsQuality(feature, formula, mobScanAccess,
          msmsScan, mergedMsMs, annotation);

      writer.write(featureList.getName() + separator);
      writer.write(quality.toCsvString(separator));
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
    final double isolationChimerityScore = getChimerityScore(feature, mobScanAccess, mergedMsMs,
        info, window);

    final MSMSScore score;

    if (formula != null && annotation.getAdductType() != null) {
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

      score = MSMSScoreCalculator.evaluateMSMS(msmsFormulaTolerance, molecularFormula, dataPoints,
          msmsScan.getPrecursorMz(), msmsScan.getPrecursorCharge(), dataPoints.length);
    } else {
      score = new MSMSScore(Result.SUCCESS_WITHOUT_FORMULA);
    }

    final Double tic = msmsScan.getTIC();
    final Double bpi = msmsScan.getBasePeakIntensity();
    final List<String> spotNames = getSpotNames(mergedMsMs);
    final IonTimeSeries<? extends Scan> eic = feature.getFeatureData();
    final List<? extends Scan> scans = eic.getSpectra().stream().filter(
            s -> s instanceof ImagingFrame img && spotNames.contains(img.getMaldiSpotInfo().spotName()))
        .toList();
    final double precursorIntensity = scans.stream().map(eic::getIntensityForSpectrum)
        .mapToDouble(Double::doubleValue).sum();
    final PasefMsMsInfo ms2Info = (PasefMsMsInfo) msmsScan.getMsMsInfo();
    final String mobRange = ParsingUtils.rangeToString((Range) ms2Info.getMobilityRange());
    final String precursorMz = "%.4f".formatted(ms2Info.getIsolationMz());
    return new SpectrumMsMsQuality(feature.getRow().getID(), (float) isolationChimerityScore, score,
        msmsScan.getNumberOfDataPoints(), (float) ScanUtils.getSpectralEntropy(msmsScan),
        (float) ScanUtils.getNormalizedSpectralEntropy(msmsScan),
        (float) ScanUtils.getWeightedSpectralEntropy(msmsScan),
        (float) ScanUtils.getNormalizedWeightedSpectralEntropy(msmsScan), annotation, tic, bpi,
        precursorIntensity, spotNames, mobRange, precursorMz,
        ScanUtils.extractCollisionEnergies(msmsScan));
  }
}
