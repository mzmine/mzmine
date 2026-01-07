/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.modules.dataprocessing.filter_diams2.rt_corr.DiaMs2RtCorrParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class DiaMs2CorrParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final ScanSelectionParameter ms2ScanSelection = new ScanSelectionParameter(
      "MS2 scan selection", "Select the ms2 scans you want included for DIA MS2 correlation.",
      new ScanSelection(2));

  public static final ModuleOptionsEnumComboParameter<DiaCorrelationOptions> algorithm = new ModuleOptionsEnumComboParameter<>(
      "Algorithm", "Select the algorithm you want to use to pair pseudo-MS2 spectra.\n%s".formatted(
      DiaCorrelationOptions.getDescriptions()), DiaCorrelationOptions.RT_CORRELATION);

  public DiaMs2CorrParameters() {
    super(flists, ms2ScanSelection, algorithm);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();

    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance",
        DiaMs2RtCorrParameters.ms2ScanToScanAccuracy.cloneParameter());
    nameParameterMap.put("m/z tolerance (scan-to-scan)",
        DiaMs2RtCorrParameters.ms2ScanToScanAccuracy.cloneParameter());

    nameParameterMap.put("Minimum feature intensity",
        DiaMs2RtCorrParameters.minMs1Intensity.cloneParameter());
    nameParameterMap.put("Minimum fragment intensity",
        DiaMs2RtCorrParameters.minMs2Intensity.cloneParameter());
    nameParameterMap.put("Minimum pearson correlation",
        DiaMs2RtCorrParameters.minPearson.cloneParameter());
    nameParameterMap.put("Number of correlated points",
        DiaMs2RtCorrParameters.numCorrPoints.cloneParameter());
    nameParameterMap.put("Advanced", DiaMs2RtCorrParameters.advanced.cloneParameter());

    return nameParameterMap;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    final boolean isOldVersion = !loadedParams.containsKey("Algorithm");

    if (isOldVersion) {
      // activate old correlation option
      setParameter(algorithm, DiaCorrelationOptions.RT_CORRELATION);

      ModuleOptionsEnumComboParameter<DiaCorrelationOptions> algorithmParameter = getParameter(
          algorithm);

      final ParameterSet ms2RtParam = new DiaMs2RtCorrParameters().cloneParameterSet();
      Parameter<?> param = loadedParams.get("m/z tolerance (scan-to-scan)");
      if (param instanceof MZToleranceParameter mzTol) {
        ms2RtParam.setParameter(DiaMs2RtCorrParameters.ms2ScanToScanAccuracy, mzTol.getValue());
      }

      param = loadedParams.get("m/z tolerance");
      if (param instanceof MZToleranceParameter mzTol) {
        ms2RtParam.setParameter(DiaMs2RtCorrParameters.ms2ScanToScanAccuracy, mzTol.getValue());
      }

      param = loadedParams.get("Minimum feature intensity");
      if (param instanceof DoubleParameter minMs1) {
        ms2RtParam.setParameter(DiaMs2RtCorrParameters.minMs1Intensity, minMs1.getValue());
      }

      param = loadedParams.get("Minimum fragment intensity");
      if (param instanceof DoubleParameter minMs2) {
        ms2RtParam.setParameter(DiaMs2RtCorrParameters.minMs2Intensity, minMs2.getValue());
      }

      param = loadedParams.get("Minimum pearson correlation");
      if (param instanceof DoubleParameter minPearson) {
        ms2RtParam.setParameter(DiaMs2RtCorrParameters.minPearson, minPearson.getValue());
      }

      param = loadedParams.get("Number of correlated points");
      if (param instanceof IntegerParameter numCorr) {
        ms2RtParam.setParameter(DiaMs2RtCorrParameters.numCorrPoints, numCorr.getValue());
      }

      algorithmParameter.setEmbeddedParameters(ms2RtParam);
    }

    if (isOldVersion && !loadedParams.containsKey("Advanced")) {
      ModuleOptionsEnumComboParameter<DiaCorrelationOptions> algorithmParameter = getParameter(
          algorithm);
      algorithmParameter.getEmbeddedParameters()
          .setParameter(DiaMs2RtCorrParameters.advanced, false);
    }
  }
}
