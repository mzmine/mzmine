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

package io.github.mzmine.modules.dataprocessing.filter_ims_msms_refinement;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetectorParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsMs2RefinementTask extends AbstractTask {

  private static final MassDetector fol = MassDetectionParameters.factorOfLowest;
  private final FeatureList[] flists;
  private final int minNumPoints;
  private final ParameterSet folParam;
  private final ParameterSet param;
  private final Boolean useFOL;
  private final Boolean useMinPoints;
  private final long numRows;
  private int processedRows = 0;

  public ImsMs2RefinementTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ParameterSet param) {
    super(storage, moduleCallDate);
    this.param = param;

    flists = param.getParameter(ImsMs2RefinementParameters.flists).getValue()
        .getMatchingFeatureLists();
    useMinPoints = param.getParameter(ImsMs2RefinementParameters.minNumPoints).getValue();
    minNumPoints = param.getParameter(ImsMs2RefinementParameters.minNumPoints)
        .getEmbeddedParameter().getValue();
    useFOL = param.getParameter(ImsMs2RefinementParameters.noiseLevel).getValue();
    final double folNoiseLevel = param.getParameter(ImsMs2RefinementParameters.noiseLevel)
        .getEmbeddedParameter().getValue();

    folParam = MZmineCore.getConfiguration().getModuleParameters(FactorOfLowestMassDetector.class)
        .cloneParameterSet();
    folParam.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, 2.5);

    numRows = Arrays.stream(flists).mapToInt(FeatureList::getNumberOfRows).sum();
  }

  @Override
  public String getTaskDescription() {
    return "Processing row " + processedRows + "/" + numRows;
  }

  @Override
  public double getFinishedPercentage() {
    return processedRows / (double) numRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (FeatureList flist : flists) {
      for (FeatureListRow row : flist.getRows()) {
        for (ModularFeature f : row.getFeatures()) {
          if (f == null || f.getFeatureStatus() == FeatureStatus.UNKNOWN) {
            continue;
          }
          if (!FeatureUtils.isImsFeature(f)) {
            continue;
          }

          if (useMinPoints) {
            List<Scan> refinedMinPointsMsMs = applyMinNumberOfPoints(f, minNumPoints);
            f.setAllMS2FragmentScans(refinedMinPointsMsMs);
          }

          if (useFOL) {
            applyFOLMassDetector((ModularFeatureList) flist, f);
          }
        }
        processedRows++;
      }
      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(ImsMs2RefinementModule.class, param,
              getModuleCallDate()));
      if (isCanceled()) {
        return;
      }
    }
    setStatus(TaskStatus.FINISHED);
  }

  private void applyFOLMassDetector(ModularFeatureList flist, ModularFeature f) {
    for (Scan msms : f.getAllMS2FragmentScans()) {
      if (!(msms instanceof MergedMsMsSpectrum merged)
          || !(merged.getMsMsInfo() instanceof PasefMsMsInfo info)) {
        continue;
      }

      final SimpleMassList massList = new SimpleMassList(flist.getMemoryMapStorage(),
          fol.getMassValues(msms, folParam));
      msms.addMassList(massList);
    }
  }

  @NotNull
  private List<Scan> applyMinNumberOfPoints(ModularFeature f, int minNumPoints) {
    List<Scan> refinedMsMs = new ArrayList<>();
    for (Scan msms : f.getAllMS2FragmentScans()) {
      if (!(msms instanceof MergedMsMsSpectrum merged)
          || !(merged.getMsMsInfo() instanceof PasefMsMsInfo info)) {
        refinedMsMs.add(msms);
        continue;
      }

      Double minIntensity = ScanUtils.getLowestIntensity(msms);

      final MergedMsMsSpectrum refined = (MergedMsMsSpectrum) SpectraMerging.mergeSpectra(
          merged.getSourceSpectra(), SpectraMerging.pasefMS2MergeTol, IntensityMergingType.SUMMED,
          merged.getMergingType(), null, minIntensity, minNumPoints, merged.getCenterFunction(),
          getMemoryMapStorage());
      if (refined.getNumberOfDataPoints() > 0) {
        refinedMsMs.add(refined);
      }
    }
    return refinedMsMs;
  }
}
