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
/*
 * author Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import com.google.common.collect.Range;
import com.sun.javafx.application.HostServicesDelegate;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import java.text.NumberFormat;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Hyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parameters used by CentWaveDetector.
 */
public class ADAPResolverParameters extends GeneralResolverParameters {

  private static final SNEstimatorChoice[] SNESTIMATORS = {new IntensityWindowsSNEstimator(),
      new WaveletCoefficientsSNEstimator()};

  public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
      "Peak duration range", "Range of acceptable peak lengths",
      MZmineCore.getConfiguration().getRTFormat(), true, Range.closed(0.0, 10.0));

  public static final DoubleRangeParameter RT_FOR_CWT_SCALES_DURATION = new DoubleRangeParameter(
      "RT wavelet range",
      "Upper and lower bounds of retention times to be used for setting the wavelet scales. Choose a range that that simmilar to the range of peak widths expected to be found from the data.",
      MZmineCore.getConfiguration().getRTFormat(), true, true, Range.closed(0.001, 0.1));

  // public static final DoubleRangeParameter PEAK_SCALES = new
  // DoubleRangeParameter(
  // "Wavelet scales",
  // "Range wavelet widths (smallest, largest) in minutes", MZmineCore
  // .getConfiguration().getRTFormat(), Range.closed(0.25, 5.0));
  public static final ModuleComboParameter<SNEstimatorChoice> SN_ESTIMATORS = new ModuleComboParameter<SNEstimatorChoice>(
      "S/N estimator", "SN description", SNESTIMATORS, SNESTIMATORS[0]);

  public static final DoubleParameter SN_THRESHOLD = new DoubleParameter("S/N threshold",
      "Signal to noise ratio threshold", NumberFormat.getNumberInstance(), 10.0, 0.0, null);

  public static final DoubleParameter COEF_AREA_THRESHOLD = new DoubleParameter(
      "Coefficient/area threshold",
      "This is a threshold for the maximum coefficient (inner product) divided by the area "
          + "under the curve of the feature. Filters out bad peaks.",
      NumberFormat.getNumberInstance(), 110.0, 0.0, null);

  public static final DoubleParameter MIN_FEAT_HEIGHT = new DoubleParameter("min feature height",
      "Minimum height of a feature. Should be the same, or similar to, the value - min start intensity - "
          + "set in the chromatogram building.", NumberFormat.getNumberInstance(), 10.0, 0.0, null);

  public ADAPResolverParameters() {
    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, dimension,
        SN_THRESHOLD, SN_ESTIMATORS, MIN_FEAT_HEIGHT, COEF_AREA_THRESHOLD, PEAK_DURATION,
        RT_FOR_CWT_SCALES_DURATION},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_adap/adap-resolver.html");

  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html>ADAP Module Disclaimer:"
        + "<br> If you use the  ADAP Chromatogram Deconvolution Module, please cite the "
        + "<a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following article:"
        + "<br><a href=\"http://pubs.acs.org/doi/abs/10.1021/acs.analchem.7b00947\"> Myers OD, Sumner SJ, Li S, Barnes S, Du X: One Step Forward for Reducing False Positive and False Negative "
        + "<br>Compound Identifications from Mass Spectrometry Metabolomics Data: New Algorithms for Constructing Extracted "
        + "<br>Ion Chromatograms and Detecting Chromatographic Peaks. Anal Chem 2017, DOI: 10.1021/acs.analchem.7b00947</a>"
        + "</html>";

    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public FeatureResolver getResolver() {
    throw new UnsupportedOperationException("Legacy resolving is not supported by ADAPResolver.");
  }

  @Override
  public @Nullable Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {
    return new ADAPResolver(parameterSet, flist);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
