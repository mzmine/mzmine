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

package io.github.mzmine.modules.dataprocessing.id_postcolumnreaction;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.Nullable;

public class PostColumnReactionParameters extends SimpleParameterSet {

  public static final StringParameter tpName = new StringParameter(
      "Name of transformation products",
      "Set a name to be used for automated annotation of transformation products (e.g., 'TP').");

  public static final FeatureListsParameter flist = new FeatureListsParameter(
      "Aligned feature list", 1, 1);

  public static final RawDataFilesParameter unreactedRawDataFiles = new RawDataFilesParameter(
      "Unreacted raw data files", 1, Integer.MAX_VALUE);

  // Define parameter set for automated formula prediction!
  public static final OptionalModuleParameter<PostColumnReactionFormulaPredictionParameters> formulaPredictionParameters = new OptionalModuleParameter<>(
      "Predict molecular formulae",
      "Automatic prediction of tranformation product molecular fomulae based on the parent molecular formula",
      new PostColumnReactionFormulaPredictionParameters(), true);

  public static final OptionalParameter<PercentParameter> correlationThreshold = new OptionalParameter<>(
      new PercentParameter("Apply shape correlation threshold",
          "Set a correlation score at which features are considered for transformation product annotation in %",
          0.4), true);

  public PostColumnReactionParameters() {
    super(tpName, flist, unreactedRawDataFiles, formulaPredictionParameters, correlationThreshold);
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true, FxTexts.text(
        "This module identifies and annotates transformation products from post-column reactions. "
            + "It requires correlation grouping (metaCorrelate) to have been run first. "
            + "For each annotated compound, it searches for correlated features that are absent in unreacted control samples. "
            + "These transformation products are then annotated with the parent's name, "
            + "a transformation product name (e.g., TP) and the nominal m/z, yielding e.g., Valsartan_TP_123."
            + "Optionally, the molecular formula of transformation prodcuts can be predicted."));
  }
}
