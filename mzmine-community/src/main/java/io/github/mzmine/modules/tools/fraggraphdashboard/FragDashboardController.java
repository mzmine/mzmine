/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.mvci.FragmentGraphController;
import io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable.SpectrumPlotTableController;
import io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable.SpectrumPlotTableViewBuilder.Layout;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FragDashboardController extends FxController<FragDashboardModel> {

  private static final Logger logger = Logger.getLogger(FragDashboardController.class.getName());

  private final ParameterSet parameters;
  private final FragDashboardBuilder fragDashboardBuilder;
  private final FragmentGraphController fragmentGraphController;
  private final SpectrumPlotTableController isotopeController;
  private final SpectrumPlotTableController ms2Controller;

  public FragDashboardController() {
    this(null);
  }

  public FragDashboardController(@Nullable ParameterSet parameters) {
    super(new FragDashboardModel());
    this.parameters = requireNonNullElse(parameters,
        ConfigService.getConfiguration().getModuleParameters(FragDashboardModule.class));
    fragmentGraphController = new FragmentGraphController(parameters);

//    model.precursorFormulaProperty()
//        .bindBidirectional(fragmentGraphController.precursorFormulaProperty());
//    model.spectrumProperty().bindBidirectional(fragmentGraphController.spectrumProperty());
    model.allEdgesProperty().bindContentBidirectional(fragmentGraphController.allEdgesProperty());
    model.allNodesProperty().bindContentBidirectional(fragmentGraphController.allNodesProperty());
    model.selectedEdgesProperty()
        .bindContentBidirectional(fragmentGraphController.selectedEdgesProperty());
    model.selectedNodesProperty()
        .bindContentBidirectional(fragmentGraphController.selectedNodesProperty());
    fragmentGraphController.measuredPrecursorMzProperty() // regular binding so we take control of the property with this controller.
        .bind(model.precursorMzProperty().map(Number::doubleValue));

    ms2Controller = new SpectrumPlotTableController(Layout.HORIZONTAL);
    isotopeController = new SpectrumPlotTableController(Layout.HORIZONTAL);

    model.spectrumProperty().bindBidirectional(ms2Controller.spectrumProperty());
    model.isotopePatternProperty().bindBidirectional(isotopeController.spectrumProperty());

    fragDashboardBuilder = new FragDashboardBuilder(model, fragmentGraphController.buildView(),
        ms2Controller.buildView(), isotopeController.buildView(), this::updateFragmentGraph,
        this::startFormulaCalculation, this::saveToRowAction, parameters);

    initSelectedEdgeToSpectrumListener();
  }

  @Override
  protected @NotNull FxViewBuilder<FragDashboardModel> getViewBuilder() {
    return fragDashboardBuilder;
  }

  public void updateFragmentGraph() {
    fragmentGraphController.precursorFormulaProperty().set(model.getPrecursorFormula());
    fragmentGraphController.spectrumProperty().set(model.getSpectrum());
  }

  public void startFormulaCalculation() {
    onTaskThreadDelayed(new FragGraphPrecursorFormulaTask(model, parameters), new Duration(200));
  }

  public void setInput(double precursorMz, @NotNull MassSpectrum ms2Spectrum,
      @Nullable MassSpectrum isotopePattern) {
    model.setPrecursorMz(precursorMz);
    model.setSpectrum(ms2Spectrum);
    model.setIsotopePattern(isotopePattern != null ? isotopePattern : MassSpectrum.EMPTY);
  }

  public void setInput(double precursorMz, @NotNull MassSpectrum ms2Spectrum,
      @Nullable MassSpectrum isotopePattern, @Nullable IMolecularFormula formula) {
    if (ms2Spectrum instanceof Scan s) {
      if (s.getMassList() == null) {
        throw new MissingMassListException(s);
      }
      ms2Spectrum = s.getMassList();
    }
    setInput(precursorMz, ms2Spectrum, isotopePattern);
    model.setPrecursorFormula(formula);
  }

  public void setInput(double precursorMz, @NotNull MassSpectrum ms2Spectrum,
      @Nullable MassSpectrum isotopePattern, @Nullable IMolecularFormula formula,
      @Nullable List<ResultFormula> formulae) {
    setInput(precursorMz, ms2Spectrum, isotopePattern, formula);
    if (formula != null) {
      updateFragmentGraph();
    }

    if (formulae != null) {
      model.precursorFormulaeProperty().setAll(formulae);
    }
  }

  private void initSelectedEdgeToSpectrumListener() {
    model.selectedEdgesProperty().addListener((ListChangeListener<SubFormulaEdge>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          for (SubFormulaEdge edge : change.getAddedSubList()) {
            ms2Controller.addDomainMarker(
                Range.closed(edge.smaller().getPeakWithFormulae().peak().getMZ(),
                    edge.larger().getPeakWithFormulae().peak().getMZ()));
          }
        }
        if (change.wasRemoved()) {
          for (SubFormulaEdge edge : change.getRemoved()) {
            ms2Controller.removeDomainMarker(
                Range.closed(edge.smaller().getPeakWithFormulae().peak().getMZ(),
                    edge.larger().getPeakWithFormulae().peak().getMZ()));
          }
        }
      }
    });
  }

  /**
   * Saves the selected formula to a row as a ResultFormula.
   */
  public void saveToRowAction() {
    if (model.getRow() != null && model.getPrecursorFormula() != null) {
      try {

        IMolecularFormula chargedFormula = (IMolecularFormula) model.getPrecursorFormula().clone();

        final SimpleIsotopePattern measuredPattern = new SimpleIsotopePattern(
            ScanUtils.extractDataPoints(model.getIsotopePattern()), chargedFormula.getCharge(),
            IsotopePatternStatus.DETECTED, "");

        final Map<DataPoint, String> annotations = model.allNodesProperty().stream().collect(
            Collectors.toMap(sfm -> sfm.getPeakWithFormulae().peak(),
                sfm -> MolecularFormulaManipulator.getString(
                    sfm.getSelectedFormulaWithMz().formula())));

        final ResultFormula formula = new ResultFormula(chargedFormula, measuredPattern,
            model.getSpectrum(), model.getPrecursorMz(),
            parameters.getValue(FragmentGraphCalcParameters.ms2Tolerance), annotations);

        final FeatureList featureList = model.getRow().getFeatureList();
        if (featureList != null) {
          featureList.addRowType(DataTypes.get(FormulaListType.class));
        }
        model.getRow().addFormula(formula, true);

      } catch (CloneNotSupportedException e) {
        logger.log(Level.WARNING, "Failed to clone molecular formula.", e);
      }
    }
  }

  public ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return model.rowProperty();
  }
}
