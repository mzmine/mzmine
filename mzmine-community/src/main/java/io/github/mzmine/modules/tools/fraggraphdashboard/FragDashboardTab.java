/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.TryCatch;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FragDashboardTab extends SimpleTab {

  private final FragDashboardController controller;

  public FragDashboardTab() {
    this(null);
  }

  public FragDashboardTab(@Nullable ParameterSet parameters) {
    super("Fragment graph dashboard");
    controller = new FragDashboardController(parameters);
    setContent(controller.buildView());
  }

  public FragDashboardTab(double precursor, @NotNull MassSpectrum fragmentSpectrum) {
    this(precursor, fragmentSpectrum, MassSpectrum.EMPTY, null);
  }

  public FragDashboardTab(double precursor, @NotNull MassSpectrum fragmentSpectrum,
      @NotNull MassSpectrum isotopes, ParameterSet parameters) {
    super("Fragment graph dashboard");
    controller = new FragDashboardController(parameters);
    setContent(controller.buildView());
    controller.setInput(precursor, fragmentSpectrum, isotopes);
  }

  public FragDashboardTab(@NotNull FeatureListRow row, @Nullable ParameterSet parameters,
      @Nullable IMolecularFormula formula) {
    super("Fragment graph dashboard");
    if (parameters == null) {
      parameters = MZmineCore.getConfiguration().getModuleParameters(FragDashboardModule.class);
    }

    final Scan ms2 = row.getMostIntenseFragmentScan();
    final IsotopePattern bestIsotopePattern = row.getBestIsotopePattern();
    final Double mz = row.getAverageMZ();

    if (row.getBestIonIdentity() != null && row.getBestIonIdentity().getIonType() != null) {
      parameters.setParameter(FragmentGraphCalcParameters.adducts,
          List.of(row.getBestIonIdentity().getIonType().getAdduct()));
    }
    parameters.setParameter(FragmentGraphCalcParameters.polarity,
        TryCatch.npe(() -> row.getBestFeature().getRepresentativeScan().getPolarity(),
            PolarityType.POSITIVE));

    controller = new FragDashboardController(parameters);
    controller.setInput(mz, ms2, bestIsotopePattern, formula, ResultFormula.forAllAnnotations(row));
    setContent(controller.buildView());
  }

  public static void addNewTab(@Nullable ParameterSet parameters) {
    MZmineCore.getDesktop().addTab(new FragDashboardTab(parameters));
  }

  public static void addNewTab(@Nullable ParameterSet parameters, @NotNull FeatureListRow row,
      @Nullable IMolecularFormula formula) {
    MZmineCore.getDesktop().addTab(new FragDashboardTab(row, parameters, formula));
  }

  public static void addNewTab(double precursor, @NotNull MassSpectrum fragmentSpectrum,
      @NotNull MassSpectrum isotopes, @Nullable ParameterSet parameters) {
    MZmineCore.getDesktop()
        .addTab(new FragDashboardTab(precursor, fragmentSpectrum, isotopes, parameters));
  }
}
