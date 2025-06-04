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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_pubchemsearch.gui;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.CompoundData;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javax.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

public class PubChemResultsModel {

  private final StringProperty formulaToSearch = new SimpleStringProperty();

  private final ObjectProperty<Double> massToSearch = new SimpleObjectProperty<>();

  private final ListProperty<TreeItem<CompoundDBAnnotation>> compounds = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  private final ObjectProperty<FeatureListRow> selectedRow = new SimpleObjectProperty<>();

  private final ObjectProperty<@NotNull IonType> ionType = new SimpleObjectProperty<>(
      new IonType(IonModification.H));

  private final ObjectProperty<@Nullable MZTolerance> mzTolerance = new SimpleObjectProperty<>(
      new MZTolerance(0.003, 5));



  public ObservableList<TreeItem<CompoundDBAnnotation>> getCompounds() {
    return compounds.get();
  }

  public ListProperty<TreeItem<CompoundDBAnnotation>> compoundsProperty() {
    return compounds;
  }

  public FeatureListRow getSelectedRow() {
    return selectedRow.get();
  }

  public ObjectProperty<FeatureListRow> selectedRowProperty() {
    return selectedRow;
  }

  public @NotNull IonType getIonType() {
    return ionType.get();
  }

  public ObjectProperty<@NotNull IonType> ionTypeProperty() {
    return ionType;
  }

  public String getFormulaToSearch() {
    return formulaToSearch.get();
  }

  public StringProperty formulaToSearchProperty() {
    return formulaToSearch;
  }

  public Double getMassToSearch() {
    return massToSearch.get();
  }

  public ObjectProperty<Double> massToSearchProperty() {
    return massToSearch;
  }

  public @Nullable MZTolerance getMzTolerance() {
    return mzTolerance.get();
  }

  public ObjectProperty<@Nullable MZTolerance> mzToleranceProperty() {
    return mzTolerance;
  }
}
