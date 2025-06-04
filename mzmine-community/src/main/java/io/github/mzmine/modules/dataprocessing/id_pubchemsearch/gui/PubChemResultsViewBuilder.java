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

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.PubChemIdType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MsMsScoreType;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.TreeTableColumns;
import io.github.mzmine.javafx.components.factories.TreeTableColumns.ColumnAlignment;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.molstructure.StructureTableCell;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceComponent;
import io.github.mzmine.util.Comparators;
import io.github.mzmine.util.components.FormulaTextField;
import io.github.mzmine.util.javafx.IonTypeTextField;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class PubChemResultsViewBuilder extends FxViewBuilder<PubChemResultsModel> {

  final Consumer<List<CompoundDBAnnotation>> onAddAnnotationsPressed;
  final Runnable onClosePressed;
  final Runnable onFormulaSearchPressed;
  final Runnable onMassSearchPressed;

  protected PubChemResultsViewBuilder(PubChemResultsModel model,
      Consumer<List<CompoundDBAnnotation>> onAddAnnotationsPressed, Runnable onClosePressed,
      Runnable onFormulaSearchPressed, Runnable onMassSearchPressed) {
    super(model);
    this.onAddAnnotationsPressed = onAddAnnotationsPressed;
    this.onClosePressed = onClosePressed;
    this.onFormulaSearchPressed = onFormulaSearchPressed;
    this.onMassSearchPressed = onMassSearchPressed;
  }

  @Override
  public Region build() {

    final BorderPane borderPane = new BorderPane();

    final HBox formulaBox = createFormulaSearchBox();
    final Region massBox = createMassSearchBox();

    borderPane.setTop(FxLayout.newHBox(formulaBox, new Separator(Orientation.VERTICAL), massBox));

    final TreeTableView<CompoundDBAnnotation> treeTableView = new TreeTableView<>();
    treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

    // dummy for the root item
    final TreeItem<CompoundDBAnnotation> root = new TreeItem<>(new SimpleCompoundDBAnnotation());

    treeTableView.setRoot(root);
    treeTableView.setShowRoot(false);

    model.compoundsProperty().bindBidirectional(new SimpleListProperty<>(root.getChildren()));

    createAndAddColumns(treeTableView);
    borderPane.setCenter(treeTableView);

    final Button btnAccept = FxButtons.createButton("Accept selected", null,
        FxIconUtil.getFontIcon(FxIcons.ADD), _ -> onAddAnnotationsPressed.accept(
            treeTableView.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue)
                .toList()));
    final Button btnClose = FxButtons.createButton("Close", null, FxIconUtil.getFontIcon(FxIcons.X),
        _ -> onClosePressed.run());
    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().addAll(btnAccept, btnClose);
    borderPane.setBottom(buttonBar);

    return borderPane;
  }

  private @NotNull HBox createFormulaSearchBox() {
    final FormulaTextField formulaField = new FormulaTextField();
    formulaField.textProperty().bindBidirectional(model.formulaToSearchProperty());
    final Button btnSearchFormula = FxButtons.createButton("Search formula", null,
        FxIconUtil.getFontIcon(FxIcons.SEARCH), _ -> onFormulaSearchPressed.run());
    btnSearchFormula.disableProperty().bind(
        Bindings.createBooleanBinding(() -> formulaField.formulaProperty().get() == null,
            formulaField.formulaProperty()));
    final Label label = FxLabels.newLabelNoWrap("Formula:");
    final HBox formulaBox = FxLayout.newHBox(label, formulaField, btnSearchFormula);
    return formulaBox;
  }

  private @NotNull Region createMassSearchBox() {

    final ToggleGroup grp = new ToggleGroup();
    final RadioButton neutralMass = new RadioButton("Neutral mass");
    neutralMass.setToggleGroup(grp);
    final DoubleComponent neutralMassField = new DoubleComponent(100, 0d, Double.MAX_VALUE,
        ConfigService.getGuiFormats().mzFormat(), null);
    neutralMassField.setPrefWidth(100);
    neutralMassField.valueProperty().bindBidirectional(model.massToSearchProperty());
    neutralMassField.disableProperty().bind(neutralMass.selectedProperty().not());
    neutralMass.setSelected(true);

    final RadioButton accurateMass = new RadioButton("m/z");
    accurateMass.setToggleGroup(grp);
    final DoubleComponent accurateMassField = new DoubleComponent(100, 0d, Double.MAX_VALUE,
        ConfigService.getGuiFormats().mzFormat(), null);
    accurateMassField.setPrefWidth(100);
    final IonTypeTextField ionTypeField = new IonTypeTextField();
    ionTypeField.getTextField().setPrefWidth(80);
    ionTypeField.ionTypeProperty().bindBidirectional(model.ionTypeProperty());
    accurateMassField.disableProperty().bind(accurateMass.selectedProperty().not());
    ionTypeField.disableProperty().bind(accurateMass.selectedProperty().not());

    // the neutral mass field is bound to the model, so update the neutral mass in this case
    accurateMassField.valueProperty().addListener((_, _, accMass) -> {
      if (accMass != null && ionTypeField.getIonType() != null) {
        neutralMassField.valueProperty().setValue(ionTypeField.getIonType().getMass(accMass));
      }
    });
    ionTypeField.ionTypeProperty().addListener((__, _, newType) -> {
      if (newType != null && accurateMassField.valueProperty().getValue() != null) {
        neutralMassField.valueProperty()
            .setValue(newType.getMass(accurateMassField.valueProperty().getValue()));
      }
    });

    // if the neutral mass changes, also update the accurate mass
    neutralMassField.valueProperty().addListener((_, _, neutral) -> {
      if (neutral != null && ionTypeField.getIonType() != null) {
        accurateMassField.valueProperty().setValue(ionTypeField.getIonType().getMZ(neutral));
      }
    });

    final Button searchMass = FxButtons.createButton("Search mass", null,
        FxIconUtil.getFontIcon(FxIcons.SEARCH), _ -> onMassSearchPressed.run());
    searchMass.disableProperty().bind(
        Bindings.createBooleanBinding(() -> model.massToSearchProperty().get() == null,
            model.massToSearchProperty()));

    final HBox neutralSearchBox = FxLayout.newHBox(neutralMass, neutralMassField);
    final HBox accurateSearchBox = FxLayout.newHBox(accurateMass, accurateMassField, ionTypeField);

    final Label tolLabel = FxLabels.newLabelNoWrap("Tolerance:");
    final MZToleranceComponent mzTol = new MZToleranceComponent();
    mzTol.setPrefWidth(150);
    mzTol.setMaxWidth(150);
    mzTol.valueProperty().bindBidirectional(model.mzToleranceProperty());
    final HBox toleranceBox = FxLayout.newHBox(Insets.EMPTY, tolLabel, mzTol);

    final Region massBox = FxLayout.newHBox(Insets.EMPTY, neutralSearchBox,
        new Separator(Orientation.VERTICAL), accurateSearchBox, toleranceBox, searchMass);

    return massBox;
  }

  private static void createAndAddColumns(TreeTableView<CompoundDBAnnotation> treeTableView) {
    final NumberFormats formats = ConfigService.getGuiFormats();

    final TreeTableColumn<CompoundDBAnnotation, String> nameColumn = TreeTableColumns.createColumn(
        "Name", a -> new ReadOnlyStringWrapper(a.getCompoundName()));
    treeTableView.getColumns().add(nameColumn);

    final TreeTableColumn<CompoundDBAnnotation, String> formulaColumn = TreeTableColumns.createColumn(
        "Formula", p -> new ReadOnlyStringWrapper(p.getFormula()));
    treeTableView.getColumns().add(formulaColumn);

    final TreeTableColumn<CompoundDBAnnotation, String> inchiColumn = TreeTableColumns.createColumn(
        "InChI", p -> new ReadOnlyStringWrapper(p.getInChI()));
    treeTableView.getColumns().add(inchiColumn);

    final TreeTableColumn<CompoundDBAnnotation, String> inchiKeyColumn = TreeTableColumns.createColumn(
        "InChIKey", p -> new ReadOnlyStringWrapper(p.getInChIKey()));
    treeTableView.getColumns().add(inchiKeyColumn);

    final TreeTableColumn<CompoundDBAnnotation, String> smilesColumn = TreeTableColumns.createColumn(
        "SMILES", p -> new ReadOnlyStringWrapper(p.getSmiles()));
    treeTableView.getColumns().add(smilesColumn);

    final TreeTableColumn<CompoundDBAnnotation, String> cidColumn = TreeTableColumns.createColumn(
        "PubChemCID", p -> new ReadOnlyObjectWrapper<>(p.get(PubChemIdType.class)));
    treeTableView.getColumns().add(cidColumn);

    final TreeTableColumn<CompoundDBAnnotation, Integer> chargeColumn = TreeTableColumns.createColumn(
        "Charge", 0, 0, ColumnAlignment.RIGHT, Comparator.comparingInt(o -> o != null ? o : 0),
        p -> new ReadOnlyObjectWrapper<>(p.get(ChargeType.class)));
    treeTableView.getColumns().add(chargeColumn);

    final TreeTableColumn<CompoundDBAnnotation, Double> massColum = TreeTableColumns.createColumn(
        "Neutral mass", 0, 0, formats.mzFormat(), ColumnAlignment.RIGHT,
        Comparators.COMPARE_ABS_DOUBLE,
        p -> new ReadOnlyObjectWrapper<>(p.get(NeutralMassType.class)));
    treeTableView.getColumns().add(massColum);

    final TreeTableColumn<CompoundDBAnnotation, Object> structureColumn = TreeTableColumns.createColumn(
        "Structure", annotation -> new ReadOnlyObjectWrapper<>(annotation.getStructure()));
    structureColumn.setCellFactory(_ -> new StructureTableCell<>());
    treeTableView.getColumns().add(structureColumn);

    final TreeTableColumn<CompoundDBAnnotation, Double> mzDiffColumn = TreeTableColumns.createColumn(
        "Δm/z (abs.)", 0, 0, formats.mzFormat(), ColumnAlignment.RIGHT,
        Comparators.COMPARE_ABS_DOUBLE,
        a -> new ReadOnlyObjectWrapper<>(a.get(MzAbsoluteDifferenceType.class)));
    treeTableView.getColumns().add(mzDiffColumn);

    final TreeTableColumn<CompoundDBAnnotation, Float> mzPpmDiffColumn = TreeTableColumns.createColumn(
        "Δm/z (ppm)", 0, 0, formats.mzFormat(), ColumnAlignment.RIGHT,
        Comparators.COMPARE_ABS_FLOAT,
        a -> new ReadOnlyObjectWrapper<>(a.get(MzPpmDifferenceType.class)));
    treeTableView.getColumns().add(mzPpmDiffColumn);

    final TreeTableColumn<CompoundDBAnnotation, Float> isotopeScoreColumn = TreeTableColumns.createColumn(
        "Isotope score", 0, 0, formats.scoreFormat(), ColumnAlignment.RIGHT,
        Comparators.COMPARE_ABS_FLOAT,
        a -> new ReadOnlyObjectWrapper<>(a.get(IsotopePatternScoreType.class)));
    treeTableView.getColumns().add(isotopeScoreColumn);

    final TreeTableColumn<CompoundDBAnnotation, Float> ms2ScoreColumn = TreeTableColumns.createColumn(
        "MS2 score", 0, 0, formats.scoreFormat(), ColumnAlignment.RIGHT,
        Comparators.COMPARE_ABS_FLOAT,
        a -> new ReadOnlyObjectWrapper<>(a.get(MsMsScoreType.class)));
    treeTableView.getColumns().add(ms2ScoreColumn);
  }
}
