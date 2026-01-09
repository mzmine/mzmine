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

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.SimpleIonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FeatureUtils;
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
    super("Fragment formula dashboard");
    controller = new FragDashboardController(parameters);
    setContent(controller.buildView());
  }

  public FragDashboardTab(double precursor, @NotNull MassSpectrum fragmentSpectrum) {
    this(precursor, fragmentSpectrum, MassSpectrum.EMPTY, null);
  }

  public FragDashboardTab(double precursor, @NotNull MassSpectrum fragmentSpectrum,
      @NotNull MassSpectrum isotopes, ParameterSet parameters) {
    super("Fragment graph dashboard");
    setSubTitle("m/z " + ConfigService.getGuiFormats().mz(precursor));
    controller = new FragDashboardController(parameters);
    setContent(controller.buildView());
    controller.setInput(precursor, fragmentSpectrum, isotopes);
  }

  /**
   * Creates a new fragment dashboard tab for the row. Uses all row annotations as possible
   * formulae.
   *
   * @param row     The row
   * @param formula if null, defaults to the best annotation of the row as defined by
   *                {@link FeatureUtils#getBestFeatureAnnotation(ModularDataModel)}.
   */
  public FragDashboardTab(@NotNull FeatureListRow row, @Nullable ParameterSet parameters,
      @Nullable IMolecularFormula formula) {
    super("Fragment graph dashboard");
    setSubTitle(FeatureUtils.rowToString(row));
    if (parameters == null) {
      parameters = MZmineCore.getConfiguration().getModuleParameters(FragDashboardModule.class);
    }

    final Scan ms2 = row.getMostIntenseFragmentScan();
    final IsotopePattern bestIsotopePattern = row.getBestIsotopePattern();
    final Double mz = row.getAverageMZ();

    final IonIdentity bestIon = row.getBestIonIdentity();
    if (bestIon != null) {
      parameters.setParameter(FragmentGraphCalcParameters.allowedIons,
          new SimpleIonLibrary("best ion of row " + row.getID(), List.of(bestIon.getIonType())));
    } else {
      parameters.setParameter(FragmentGraphCalcParameters.allowedIons,
          IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_SMALLEST);
    }
    parameters.setParameter(FragmentGraphCalcParameters.polarity,
        TryCatch.npe(() -> row.getBestFeature().getRepresentativeScan().getPolarity(),
            PolarityType.POSITIVE));

    final List<ResultFormula> formulae = ResultFormula.listAllAnnotationIonFormulas(row, true);
    if (formula == null && !formulae.isEmpty()) {
      formula = formulae.getFirst().getFormulaAsObject();
    }

    controller = new FragDashboardController(parameters);
    controller.setInput(mz, ms2, bestIsotopePattern, formula, formulae);
    setContent(controller.buildView());
    controller.rowProperty().set(row);
  }

  public static void addNewTab(@Nullable ParameterSet parameters) {
    MZmineCore.getDesktop().addTab(new FragDashboardTab(parameters));
  }

  /**
   * Creates a new fragment dashboard tab for the row. Uses all row annotations as possible
   * formulae.
   *
   * @param row     The row
   * @param formula if null, defaults to the best annotation of the row as defined by
   *                {@link FeatureUtils#getBestFeatureAnnotation(ModularDataModel)}.
   */
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
