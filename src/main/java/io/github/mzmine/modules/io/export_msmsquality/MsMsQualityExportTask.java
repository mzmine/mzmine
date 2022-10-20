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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.tools.msmsscore.MSMSIntensityScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.RangeUtils;
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
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class MsMsQualityExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MsMsQualityExportTask.class.getName());

  private final FeatureList[] featureLists;
  private final File exportPath;
  private final char separator = ';';
  final int numRows;

  private final Map<IMSRawDataFile, MobilityScanDataAccess> mobScanAccessMap = new HashMap<>();
  private final MZTolerance msmsFormulaTolerance;
  private final boolean annotatedOnly = true;
  private final ParameterSet parameterSet;
  private int processedRows;

  protected MsMsQualityExportTask(@NotNull Instant moduleCallDate, ParameterSet parameterSet) {
    super(null, moduleCallDate);
    this.parameterSet = parameterSet;

    msmsFormulaTolerance = parameterSet.getValue(MsMsQualityExportParameters.formulaTolerance);
    featureLists = parameterSet.getValue(MsMsQualityExportParameters.flists)
        .getMatchingFeatureLists();
    exportPath = parameterSet.getValue(MsMsQualityExportParameters.path);

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

    for (FeatureList featureList : featureLists) {

      if (!exportPath.exists()) {
        exportPath.mkdirs();
      }

      final File resultsFile = new File(exportPath, featureList.getName());
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile))) {

        /*final double[] collisionEnergies = featureList.stream()
            .flatMap(row -> row.getAllFragmentScans().stream())
            .filter(scan -> scan.getMsMsInfo() != null)
            .mapToDouble(scan -> scan.getMsMsInfo().getActivationEnergy()).distinct().sorted()
            .toArray();*/

        writer.write(
            "Compound" + separator + "chimerity" + separator + "explained_intensity" + separator
                + "explained_peaks" + separator + "num_peaks" + separator + "spectral_entropy"
                + separator + "normalized_entropy" + separator + "weighted_entropy" + separator
                + "normalized_weighted_entropy" + separator + "collision_energy");

        for (FeatureListRow row : featureList.getRows()) {
          final ModularFeature feature = (ModularFeature) row.getBestFeature();
          final RawDataFile file = feature.getRawDataFile();

          final List<CompoundDBAnnotation> annotations = feature.get(
              CompoundDatabaseMatchesType.class);
          final CompoundDBAnnotation annotation =
              annotations != null && !annotations.isEmpty() ? annotations.get(0) : null;
          final String formula = annotation != null ? annotation.getFormula() : null;

          if (annotatedOnly && formula == null) {
            processedRows++;
            continue;
          }

          if (file instanceof IMSRawDataFile imsFile
              && feature.getFeatureData() instanceof IonMobilogramTimeSeries) {

            final MobilityScanDataAccess mobScanAccess = mobScanAccessMap.computeIfAbsent(imsFile,
                f -> new MobilityScanDataAccess(f, MobilityScanDataType.CENTROID,
                    (List<Frame>) f.getFrames(1)));

            for (Scan msmsScan : feature.getAllMS2FragmentScans()) {
              final MergedMsMsSpectrum mergedMsMs = (MergedMsMsSpectrum) msmsScan;
              final SpectrumMsMsQuality quality = getImsMsMsQuality(feature, formula, mobScanAccess,
                  msmsScan, mergedMsMs, annotation);

              writer.write(quality.toCsvString(separator));
              writer.write(String.format("%.1f", mergedMsMs.getCollisionEnergy()));
              writer.newLine();
            }
          }

          processedRows++;
        }
      } catch (IOException e) {
        logger.warning(() -> "Error exporting feature list " + featureList.getName());
      }
    }
  }

  private SpectrumMsMsQuality getImsMsMsQuality(ModularFeature feature, String formula,
      MobilityScanDataAccess mobScanAccess, Scan msmsScan, MergedMsMsSpectrum mergedMsMs,
      CompoundDBAnnotation annotation) {

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
      final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(
          formula, SilentChemObjectBuilder.getInstance());
      peakFormulaScore = MSMSScoreCalculator.evaluateMSMS(molecularFormula, msmsScan,
          msmsFormulaTolerance, msmsScan.getNumberOfDataPoints());
      intensityFormulaScore = MSMSIntensityScoreCalculator.evaluateMSMS(molecularFormula, msmsScan,
          msmsFormulaTolerance, msmsScan.getNumberOfDataPoints());
    }

    return new SpectrumMsMsQuality((float) isolationChimerityScore, intensityFormulaScore,
        peakFormulaScore, msmsScan.getNumberOfDataPoints(),
        (float) ScanUtils.getSpectralEntropy(msmsScan),
        (float) ScanUtils.getNormalizedSpectralEntropy(msmsScan),
        (float) ScanUtils.getWeightedSpectralEntropy(msmsScan),
        (float) ScanUtils.getNormalizedWeightedSpectralEntropy(msmsScan), annotation);

  }
}
