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
package io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.NumberFormat;
import dulab.adap.workflow.TwoStepDecompositionParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.ListDoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV1_5Parameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  // ------------------------------------------------------------------------
  // ----- First-phase parameters -------------------------------------------
  // ------------------------------------------------------------------------

  public static final DoubleParameter MIN_CLUSTER_DISTANCE =
      new DoubleParameter("Min cluster distance (min)", "Minimum distance between any two clusters",
          NumberFormat.getNumberInstance(), 0.01);

  public static final IntegerParameter MIN_CLUSTER_SIZE =
      new IntegerParameter("Min cluster size", "Minimum size of a cluster", 2);

  public static final DoubleParameter MIN_CLUSTER_INTENSITY = new DoubleParameter(
      "Min cluster intensity",
      "If the highest peak in a cluster has the intensity below Minimum Cluster Intensity, the cluster is removed",
      NumberFormat.getNumberInstance(), 500.0);

  // ------------------------------------------------------------------------
  // ----- End of First-phase parameters ------------------------------------
  // ------------------------------------------------------------------------

  // ------------------------------------------------------------------------
  // ----- Second-phase parameters ------------------------------------------
  // ------------------------------------------------------------------------

  public static final DoubleParameter EDGE_TO_HEIGHT_RATIO =
      new DoubleParameter("Min edge-to-height ratio",
          "A peak is considered shared if its edge-to-height ratio is below this parameter",
          NumberFormat.getInstance(), 0.3, 0.0, 1.0);

  public static final DoubleParameter DELTA_TO_HEIGHT_RATIO = new DoubleParameter(
      "Min delta-to-height ratio",
      "A peak is considered shared if its delta (difference between the edges)-to-height ratio is below this parameter",
      NumberFormat.getInstance(), 0.2, 0.0, 1.0);

  public static final BooleanParameter USE_ISSHARED = new BooleanParameter("Find shared peaks",
      "If selected, peaks are marked as Shared if they are composed of two or more peaks", false);

  public static final DoubleParameter MIN_MODEL_SHARPNESS = new DoubleParameter("Min sharpness",
      "Minimum sharpness that the model peak can have", NumberFormat.getNumberInstance(), 10.0);

  public static final DoubleParameter SHAPE_SIM_THRESHOLD =
      new DoubleParameter("Shape-similarity tolerance (0..90)",
          "Shape-similarity threshold is used to find similar peaks",
          NumberFormat.getNumberInstance(), 18.0);

  public static final ComboParameter<String> MODEL_PEAK_CHOICE = new ComboParameter<>(
      "Choice of Model Peak based on",
      "Criterion to choose a model peak in a cluster: either peak with the highest m/z-value or with the highest sharpness",
      FXCollections.observableArrayList(TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_SHARPNESS,
          TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_MZ,
          TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_INTENSITY),
      TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_SHARPNESS);

  public static final ListDoubleRangeParameter MZ_VALUES = new ListDoubleRangeParameter(
      "Exclude m/z-values", "M/z-values to exclude while selecting model peak", false, null);

  // ------------------------------------------------------------------------
  // ----- End of Second-phase parameters -----------------------------------
  // ------------------------------------------------------------------------

  public static final StringParameter SUFFIX = new StringParameter("Suffix",
      "This string is added to feature list name as suffix", "ADAP-GC 3 Peak Decomposition");

  public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
      "Remove original feature list",
      "If checked, original chromatogram will be removed and only the deconvolved version remains");

  public ADAP3DecompositionV1_5Parameters() {
    super(
        new Parameter[] {PEAK_LISTS, MIN_CLUSTER_DISTANCE, MIN_CLUSTER_SIZE, MIN_CLUSTER_INTENSITY,
            USE_ISSHARED, EDGE_TO_HEIGHT_RATIO, DELTA_TO_HEIGHT_RATIO, MIN_MODEL_SHARPNESS,
            SHAPE_SIM_THRESHOLD, MODEL_PEAK_CHOICE, MZ_VALUES, SUFFIX, AUTO_REMOVE},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_hierarch_clustering/featdet_hierarch_clustering.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html>Module Disclaimer:"
        + "<br> If you use this Spectral Deconvolution Module, please cite the "
        + "<a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following article:"
        + "<br><a href=\"http://pubs.acs.org/doi/10.1021/acs.jproteome.7b00633\"> Smirnov A, Jia W, Walker D, Jones D, Du X: ADAP-GC 3.2: Graphical Software Tool for "
        + "<br>Efficient Spectral Deconvolution of Gas Cromatography&mdash;High-Resolution Mass Spectrometry "
        + "<br>Metabolomics Data. J. Proteome Res 2017, DOI: 10.1021/acs.jproteome.7b00633</a>"
        + "</html>";

    final ADAP3DecompositionV1_5SetupDialog dialog =
        new ADAP3DecompositionV1_5SetupDialog(valueCheckRequired, this, message);

    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
