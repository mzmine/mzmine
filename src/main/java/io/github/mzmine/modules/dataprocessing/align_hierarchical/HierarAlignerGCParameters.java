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

package io.github.mzmine.modules.dataprocessing.align_hierarchical;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.text.NumberFormat;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class HierarAlignerGCParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("Text file", "*.txt"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter peakListName = new StringParameter("Feature list name",
      "Feature list name", "Aligned feature list");

  // Clustering linkage strategy
  public static final ComboParameter<ClusteringLinkageStrategyType> linkageStartegyType_0 = new ComboParameter<ClusteringLinkageStrategyType>(
      "Clustering strategy",
      "What strategy shall be used for the clustering algorithm decision making (See: \"Hierarchical clustering\" algorithms in general).",
      ClusteringLinkageStrategyType.values(), ClusteringLinkageStrategyType.AVERAGE);

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();
  public static final DoubleParameter MZWeight = new DoubleParameter("Weight for m/z",
      "Weight for chemical similarity. Score for perfectly matching m/z values.");

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();
  public static final DoubleParameter RTWeight = new DoubleParameter("Weight for RT",
      "Weight for retention times similarity. Score for perfectly matching RT values.");

  public static final DoubleParameter minScore = new DoubleParameter("Minimum score",
      "Minimum score for blast to be considered as successful "
          + "(WARN: 'Pearson correlation' similarity method can imply scores < 0.0 and/or > 1.0)",
      NumberFormat.getNumberInstance(), HierarAlignerGCTask.MIN_SCORE_ABSOLUTE);

  // *** GLG HACK: Added...
  public static final BooleanParameter useKnownCompoundsAsRef = new BooleanParameter(
      "Use RT recalibration", "If checked, uses compounds with known identities to ease alignment",
      true);
  public static final BooleanParameter useDetectedMzOnly = new BooleanParameter(
      "Use DETECTED m/z only",
      "If checked, uses simplified spectra resulting from a previous 'merge step' to compute chemical similarity score",
      false);

  public static final RTToleranceParameter RTToleranceAfter = new RTToleranceParameter(
      "RT tolerance post-recalibration",
      "Ignored if \"Use RT recalibration\" is unchecked. Maximum allowed difference between two RT values after RT recalibration");

  public static final BooleanParameter exportDendrogramTxt = new BooleanParameter(
      "Export dendrogram as TXT/CDT",
      "If checked, exports the clustering resulting dendrogram to the given TXT file.", false);
  public static final FileNameParameter dendrogramTxtFilename = new FileNameParameter(
      "Dendrogram output text filename", " Requires \"Export dendrogram as TXT\" checked."
      + " Name of the resulting TXT/CDT file to write the clustering resulting dendrogram to."
      + " If the file already exists, it will be overwritten.", extensions, FileSelectionType.SAVE);

  /**
   * GLG HACK: temporarily removed for clarity public static final BooleanParameter
   * SameChargeRequired = new BooleanParameter( "Require same charge state", "If checked, only rows
   * having same charge state can be aligned");
   * <p>
   * public static final BooleanParameter SameIDRequired = new BooleanParameter( "Require same ID",
   * "If checked, only rows having same compound identities (or no identities) can be aligned");
   * <p>
   * public static final OptionalModuleParameter compareIsotopePattern = new
   * OptionalModuleParameter( "Compare isotope pattern", "If both peaks represent an isotope
   * pattern, add isotope pattern score to match score", new IsotopePatternScoreParameters());
   **/

  // Since clustering is now order independent, option removed!
  public HierarAlignerGCParameters() {
    super(new Parameter[]{peakLists, linkageStartegyType_0, peakListName, MZTolerance, MZWeight,
            RTTolerance, RTWeight, minScore, exportDendrogramTxt, dendrogramTxtFilename},
        "https://mzmine.github.io/mzmine_documentation/module_docs/align_hierarch/align_hierarch.html");
  }

}
