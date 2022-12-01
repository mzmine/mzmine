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

package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.collections.FXCollections;

public class CliqueMSParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  private static final NumberFormat LLformatter = new DecimalFormat("#0.000000");

  public static OptionalModuleParameter FILTER = new OptionalModuleParameter(
      "Filter similar features",
      "Marks all except one of the similar features. Two features are similar if their relative change in m/z, rt and intensity are less than the respective tolerances.",
      new SimilarFeatureParameters(), true);

  public static final DoubleParameter TOL = new DoubleParameter("Log-likelihood tolerance",
      "Log likelihood function is maximised for clique formation. The iterations are stopped when the relative absolute change in current log likelihood with respect to the initial log likelihood is less than the log-likelihood tolerance value.",
      LLformatter, 0.000001);
  // Max charge.
  public static final IntegerParameter ISOTOPES_MAX_CHARGE =
      new IntegerParameter("Isotopes max. charge",
          "The maximum charge considered when two features are tested to see they are isotope or not. No isotope will be annotated with a charge greater than this.",
          3, 1, null);

  // Max isotopes.
  public static final IntegerParameter ISOTOPES_MAXIMUM_GRADE = new IntegerParameter(
      "Isotopes max. per cluster",
      "The maximum number of isotopes per cluster. Cluster will be resized if size found greater than this.",
      2, 0, null);

  // Isotope m/z tolerance
  public static final MZToleranceParameter ISOTOPES_MZ_TOLERANCE =
      new MZToleranceParameter("Isotopes mass tolerance",
          "Mass tolerance used when identifying isotopes, Two features are considered isotopes if the difference of their absolute mass difference from the reference mass difference is within this tolerance limit",
          0, 10);

  //Isotope mass difference
  public static final DoubleParameter ISOTOPE_MASS_DIFF = new DoubleParameter(
      "Isotope reference mass difference",
      "The reference mass difference between two features to be considered isotopes",
      LLformatter, 1.003355);

  public static final IntegerParameter ANNOTATE_TOP_MASS = new IntegerParameter(
      "Annotation max annotations",
      " All neutral masses in the group are ordered based on their adduct log-frequencies and their number of adducts. From that list, a number of these many masses are considered for the final annotation.",
      10, 1, null);

  public static final IntegerParameter ANNOTATE_TOP_MASS_FEATURE = new IntegerParameter(
      "Annotation feature max annotation",
      "In addition to 'topmasstotal', for each feature the list of ordered neutral masses is subsetted to the masses with an adduct in that particular feature. For each sublist, these number neutral masses are also selected for the final annotation.",
      1, 0, null);

  public static final IntegerParameter SIZE_ANG = new IntegerParameter(
      "Annotation max features per clique",
      "After neutral mass selection, if a clique group has a number of monoisotopic features bigger than this parameter,  the annotation group is divided into non-overlapping annotation groups. Each subdivision is annotated independently.",
      20, 1, null);

  public static final ComboParameter POLARITY = new ComboParameter("Adduct polarity",
      "Adduct polarity",
      FXCollections.observableArrayList("positive", "negative"), "positive");

  public static final MZToleranceParameter ANNOTATE_TOL = new MZToleranceParameter(
      "Annotation mass tolerance",
      "Tolerance in mass according to which we consider two or more features compatible with a neutral mass and two or more adducts from Adduct List",
      0, 10);

  public static final DoubleParameter ANNOTATE_FILTER = new DoubleParameter(
      "Annotation duplicate filter",
      "This parameter removes redundant annotations. If two neutral masses in the same annotation group have a relative mass difference smaller than this parameter and the same features and adducts, drop the neutral mass with less adducts",
      LLformatter, 0.0001);

  public static final DoubleParameter ANNOTATE_EMPTY_SCORE = new DoubleParameter(
      "Non-Annotation score",
      "Score given to non annotated features. The value should not be larger than any adduct log frequency (therefore given default value of -6.0, see log10freq of IonizationType)",
      LLformatter, -6.0);

  public static final BooleanParameter ANNOTATE_NORMALIZE = new BooleanParameter(
      "Annotation normalize score",
      "If 'TRUE', the reported score is normalized and scaled. Normalized score goes from 0, when it means that the raw score is close to the minimum score (all features with empty annotations), up to 100, which is the score value of the theoretical maximum annotation (all the adducts of the list with the minimum number of neutral masses).",
      true);


  public CliqueMSParameters() {
    super(new Parameter[]{PEAK_LISTS, FILTER, TOL, ISOTOPES_MAX_CHARGE, ISOTOPES_MAXIMUM_GRADE,
        ISOTOPES_MZ_TOLERANCE, ISOTOPE_MASS_DIFF, ANNOTATE_TOP_MASS, ANNOTATE_TOP_MASS_FEATURE,
        POLARITY, SIZE_ANG, ANNOTATE_TOL, ANNOTATE_FILTER, ANNOTATE_EMPTY_SCORE,
        ANNOTATE_NORMALIZE});
  }

}
