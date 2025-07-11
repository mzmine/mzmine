/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata2GroupsSelection;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata2GroupsSelectionParameter;
import org.jetbrains.annotations.NotNull;

public class FoldChangeSignificanceRowFilterParameters extends SimpleParameterSet {

  public static final ComboParameter<SignificanceTests> test = new ComboParameter<>(
      "Significance test", "Test to compare two groups", SignificanceTests.univariateValues(),
      SignificanceTests.WELCHS_T_TEST);

  public static final Metadata2GroupsSelectionParameter grouping = new Metadata2GroupsSelectionParameter(
      "Select the metadata column and two groups to calculate the fold-change and significance for filtering.");

  public static final DoubleParameter maxPValue = new DoubleParameter("Maximum p-value",
      "Maximum p-value when comparing group A/group B (default 0.05)",
      ConfigService.getGuiFormats().scoreFormat(), 0.05, 0d, 1d);

  public static final DoubleParameter minFoldChangeFactor = new DoubleParameter(
      "log2(fold-change) threshold", """
      The log2(FC) threshold (as log2(group A/group B)), e.g., 1 and -1 correspond to FC 2 and 0.5, respectively.
      The threshold may be applied to two sides (up/down regulation) using absolute values or a single side using signed values like -1 for everything downregulated.""",
      ConfigService.getGuiFormats().scoreFormat(), 1d);

  public static final ComboParameter<FoldChangeFilterSides> foldChangeSideOption = new ComboParameter<>(
      "Fold-change filter", """
      Fold-change filter may be applied by absolute or signed value so looking at both sides or just a single side, respectively.""",
      FoldChangeFilterSides.values(), FoldChangeFilterSides.ABS_TWO_SIDED);


  public FoldChangeSignificanceRowFilterParameters() {
    super(grouping, test, maxPValue, minFoldChangeFactor, foldChangeSideOption);
  }

  public FoldChangeSignificanceRowFilter createFilter(FeaturesDataTable dataTable) {
    final Metadata2GroupsSelection group = this.getValue(grouping);
    final double maxP = this.getValue(maxPValue);
    final double minFC = this.getValue(minFoldChangeFactor);
    final FoldChangeFilterSides fcSideOption = this.getValue(foldChangeSideOption);
    final SignificanceTests significanceTest = this.getValue(test);

    return new FoldChangeSignificanceRowFilter(dataTable, group, maxP, minFC, fcSideOption,
        significanceTest);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
