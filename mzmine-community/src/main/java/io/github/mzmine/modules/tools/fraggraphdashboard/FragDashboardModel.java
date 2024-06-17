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

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FragDashboardModel {

  /**
   * Controls the fragment signals to be matched to. Bound bidirectionally to a text field to
   * manually edit the input.
   */
  private final ObjectProperty<MassSpectrum> spectrum = new SimpleObjectProperty<>();

  /**
   * Controls the isotope signals to be matched to. Bound bidirectionally to a text field to
   * manually edit the input.
   */
  private final ObjectProperty<MassSpectrum> isotopePattern = new SimpleObjectProperty<>();

  /**
   * The measured precursor m/z (accurate mass).
   */
  private final DoubleProperty precursorMz = new SimpleDoubleProperty();

  /**
   * currently not used.
   */
  private final ObjectProperty<IonType> ionType = new SimpleObjectProperty<>(
      new IonType(IonModification.H));
  private final ListProperty<IonType> ionTypes = new SimpleListProperty<>(
      FXCollections.observableArrayList(new IonType(IonModification.H),
          new IonType(IonModification.H_NEG)));

  /**
   * A list of possible precursor formulae for the given precursor m/z. Displayed in a table in the
   * view.
   */
  private final ListProperty<ResultFormula> precursorFormulae = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  /**
   * The currently selected precursor formula.
   */
  private final ObjectProperty<@Nullable IMolecularFormula> precursorFormula = new SimpleObjectProperty<>();

  private final BooleanProperty allowGraphRecalculation = new SimpleBooleanProperty(false);
  private final ListProperty<SignalFormulaeModel> selectedNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> selectedEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SignalFormulaeModel> allNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> allEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  /**
   * If the frag dashboard was opened from a feature list row, the row is stored here.
   */
  private final ObjectProperty<@Nullable FeatureListRow> row = new SimpleObjectProperty<>(null);

  public FragDashboardModel() {
  }

  public MassSpectrum getIsotopePattern() {
    return isotopePattern.get();
  }

  public void setIsotopePattern(MassSpectrum isotopePattern) {
    this.isotopePattern.set(isotopePattern);
  }

  public ObjectProperty<MassSpectrum> isotopePatternProperty() {
    return isotopePattern;
  }

  public double getPrecursorMz() {
    return precursorMz.get();
  }

  public void setPrecursorMz(double precursorMz) {
    this.precursorMz.set(precursorMz);
  }

  public DoubleProperty precursorMzProperty() {
    return precursorMz;
  }

//  public IonType getIonType() {
//    return ionType.get();
//  }

//  public void setIonType(IonType ionType) {
//    this.ionType.set(ionType);
//  }

//  public ObjectProperty<IonType> ionTypeProperty() {
//    return ionType;
//  }

  public ObservableList<IonType> getIonTypes() {
    return ionTypes.get();
  }

  public void setIonTypes(ObservableList<IonType> ionTypes) {
    this.ionTypes.set(ionTypes);
  }

  public ListProperty<IonType> ionTypesProperty() {
    return ionTypes;
  }

  public ObservableList<ResultFormula> getPrecursorFormulae() {
    return precursorFormulae.get();
  }

  public void setPrecursorFormulae(ObservableList<ResultFormula> precursorFormulae) {
    this.precursorFormulae.set(precursorFormulae);
  }

  public ListProperty<ResultFormula> precursorFormulaeProperty() {
    return precursorFormulae;
  }

  @Nullable
  public IMolecularFormula getPrecursorFormula() {
    return precursorFormula.get();
  }

  public void setPrecursorFormula(@Nullable IMolecularFormula precursorFormula) {
    this.precursorFormula.set(precursorFormula);
  }

  public ObjectProperty<@Nullable IMolecularFormula> precursorFormulaProperty() {
    return precursorFormula;
  }

  public MassSpectrum getSpectrum() {
    return spectrum.get();
  }

  public void setSpectrum(MassSpectrum spectrum) {
    this.spectrum.set(spectrum);
  }

  public ObjectProperty<MassSpectrum> spectrumProperty() {
    return spectrum;
  }

  public ObservableList<SignalFormulaeModel> getSelectedNodes() {
    return selectedNodes.get();
  }

  public void setSelectedNodes(ObservableList<SignalFormulaeModel> selectedNodes) {
    this.selectedNodes.set(selectedNodes);
  }

  public ListProperty<SignalFormulaeModel> selectedNodesProperty() {
    return selectedNodes;
  }

  public ObservableList<SignalFormulaeModel> getAllNodes() {
    return allNodes.get();
  }

  public void setAllNodes(ObservableList<SignalFormulaeModel> allNodes) {
    this.allNodes.set(allNodes);
  }

  public ListProperty<SignalFormulaeModel> allNodesProperty() {
    return allNodes;
  }

  public ObservableList<SubFormulaEdge> getSelectedEdges() {
    return selectedEdges.get();
  }

  public void setSelectedEdges(ObservableList<SubFormulaEdge> selectedEdges) {
    this.selectedEdges.set(selectedEdges);
  }

  public ListProperty<SubFormulaEdge> selectedEdgesProperty() {
    return selectedEdges;
  }

  public ObservableList<SubFormulaEdge> getAllEdges() {
    return allEdges.get();
  }

  public void setAllEdges(ObservableList<SubFormulaEdge> allEdges) {
    this.allEdges.set(allEdges);
  }

  public ListProperty<SubFormulaEdge> allEdgesProperty() {
    return allEdges;
  }

  public boolean getAllowGraphRecalculation() {
    return allowGraphRecalculation.get();
  }

  public void setAllowGraphRecalculation(boolean allowGraphRecalculation) {
    this.allowGraphRecalculation.set(allowGraphRecalculation);
  }

  public BooleanProperty allowGraphRecalculationProperty() {
    return allowGraphRecalculation;
  }

  public @Nullable FeatureListRow getRow() {
    return row.get();
  }

  public void setRow(@Nullable FeatureListRow row) {
    this.row.set(row);
  }

  public ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return row;
  }
}
