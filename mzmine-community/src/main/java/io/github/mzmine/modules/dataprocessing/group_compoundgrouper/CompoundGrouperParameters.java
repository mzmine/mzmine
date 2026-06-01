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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import org.jetbrains.annotations.NotNull;

public class CompoundGrouperParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final ModuleOptionsEnumComboParameter<CompoundComponentizerType> COMPONENTIZER = new ModuleOptionsEnumComboParameter<>(
      "Strategy",
      "Strategy for grouping FeatureListRows into CompoundRows. Each strategy exposes its own "
          + "parameters (tolerances, density thresholds, etc.).",
      new CompoundComponentizerType[]{CompoundComponentizerType.SimpleSeeder},
      CompoundComponentizerType.SimpleSeeder);

  public static final ModuleOptionsEnumComboParameter<CompoundRepresentativeSelectorOption> REPRESENTATIVE_SELECTOR = new ModuleOptionsEnumComboParameter<>(
      "Representative row",
      "Strategy for picking the representative FeatureListRow of each CompoundRow. "
          + "'Annotated first' prefers (1) the highest-intensity row with a compound / spectral "
          + "library annotation, (2) then the highest-intensity row carrying an IonIdentity, "
          + "(3) and finally the highest-intensity row. 'Preferred IonType' favors clean "
          + "single-adduct forms in a tier order (M+H / M-H first), with an intensity tiebreak "
          + "and an ultimate fallback to the highest-intensity row.",
      CompoundRepresentativeSelectorOption.PREFER_ANNOTATED);

  public CompoundGrouperParameters() {
    super(FEATURE_LISTS, COMPONENTIZER, REPRESENTATIVE_SELECTOR);
  }

  /**
   * Set all parameter values explicitly on {@code param}.
   */
  public static void setAll(@NotNull final ParameterSet param,
      @NotNull final FeatureListsSelection flists,
      @NotNull final CompoundComponentizerType componentizer,
      @NotNull final ParameterSet componentizerParameters,
      @NotNull final CompoundRepresentativeSelectorOption representativeSelector) {
    param.setParameter(FEATURE_LISTS, flists);
    param.getParameter(COMPONENTIZER).setValue(componentizer, componentizerParameters);
    param.getParameter(REPRESENTATIVE_SELECTOR).setValue(representativeSelector);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
