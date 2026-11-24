package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleObservation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleTarget;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SimulatedIon;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The adducts and fragmentation rules of the class: a tree with one node per adduct and its rules
 * next to a form for the selected rule. The form only offers combinations that exist and shows the
 * m/z each rule produces for the example lipid.
 */
class LipidRulesViewBuilder {

  private static final String MS1_ONLY = "Nothing, MS1 adduct only";
  private static final String ISSUE_STYLE = "-fx-text-fill: #d9534f;";
  private static final String OK_STYLE = "-fx-text-fill: -fx-accent;";
  private static final String MUTED_STYLE = "-fx-opacity: 0.7;";

  private final CustomLipidClassEditorModel model;
  // the adduct node selected in the tree, used to add fragments to it
  private final ObjectProperty<@Nullable IonizationType> selectedAdduct = new SimpleObjectProperty<>();
  private boolean updatingTree = false;

  LipidRulesViewBuilder(@NotNull CustomLipidClassEditorModel model) {
    this.model = model;
  }

  @NotNull Node build() {
    final Node tree = buildTree();
    final Node form = buildForm();
    HBox.setHgrow(form, Priority.ALWAYS);
    final HBox content = FxLayout.newHBox(Pos.TOP_LEFT, Insets.EMPTY, tree, form);
    final Label hint = FxLabels.newSmallLabel("""
        Add every adduct the class forms. Then add the fragments you expect in MS2 for each \
        adduct. The m/z values are calculated for the example lipid in the preview.""");
    hint.setWrapText(true);
    return FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, hint, content);
  }

  // ---------- tree ----------

  private @NotNull Node buildTree() {
    final TreeItem<Object> root = new TreeItem<>();
    final TreeView<Object> tree = new TreeView<>(root);
    tree.setShowRoot(false);
    tree.setPrefWidth(380);
    tree.setMinWidth(280);
    tree.setPrefHeight(320);
    tree.setCellFactory(_ -> new RuleTreeCell());

    final Runnable rebuild = () -> rebuildTree(tree, root);
    model.getRules().addListener((ListChangeListener<LipidRuleRow>) _ -> rebuild.run());
    model.getSimulatedIons()
        .addListener((MapChangeListener<LipidRuleRow, List<SimulatedIon>>) _ -> tree.refresh());
    model.getPrecursorMzs()
        .addListener((MapChangeListener<IonizationType, Double>) _ -> tree.refresh());
    rebuild.run();

    tree.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
      if (updatingTree || item == null) {
        return;
      }
      if (item.getValue() instanceof LipidRuleRow row) {
        selectedAdduct.set(row.ionizationProperty().get());
        model.selectedRuleProperty().set(row);
      } else if (item.getValue() instanceof IonizationType ion) {
        selectedAdduct.set(ion);
        model.selectedRuleProperty().set(null);
      }
    });
    model.selectedRuleProperty().addListener((_, _, row) -> selectInTree(tree, root, row));

    final MenuButton addAdduct = buildAddAdductMenu();
    final Button addFragment = FxButtons.createButton("Add fragment",
        "Add an expected MS2 fragment to the selected adduct", this::addFragment);
    FxButtons.disableIf(addFragment, selectedAdduct.isNull());
    final Button duplicate = FxButtons.createButton("Duplicate", "Duplicate the selected rule",
        () -> {
          final LipidRuleRow row = model.selectedRuleProperty().get();
          if (row != null) {
            final LipidRuleRow copy = row.copy();
            model.getRules().add(model.getRules().indexOf(row) + 1, copy);
            model.selectedRuleProperty().set(copy);
          }
        });
    FxButtons.disableIf(duplicate, model.selectedRuleProperty().isNull());
    final Button remove = FxButtons.createButton("Remove",
        "Remove the selected rule, or the selected adduct with all its rules", this::remove);
    FxButtons.disableIf(remove,
        model.selectedRuleProperty().isNull().and(selectedAdduct.isNull()));

    VBox.setVgrow(tree, Priority.ALWAYS);
    return FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, tree,
        FxLayout.newFlowPane(addAdduct, addFragment, duplicate, remove));
  }

  /**
   * Groups the rules by adduct in the order of first appearance.
   */
  private void rebuildTree(@NotNull TreeView<Object> tree, @NotNull TreeItem<Object> root) {
    updatingTree = true;
    try {
      final Map<IonizationType, List<LipidRuleRow>> groups = new LinkedHashMap<>();
      for (final LipidRuleRow row : model.getRules()) {
        groups.computeIfAbsent(row.ionizationProperty().get(), _ -> new ArrayList<>()).add(row);
      }
      final List<TreeItem<Object>> adductItems = new ArrayList<>();
      for (final var group : groups.entrySet()) {
        final TreeItem<Object> adductItem = new TreeItem<>(group.getKey());
        adductItem.setExpanded(true);
        for (final LipidRuleRow row : group.getValue()) {
          adductItem.getChildren().add(new TreeItem<>(row));
        }
        adductItems.add(adductItem);
      }
      root.getChildren().setAll(adductItems);
    } finally {
      updatingTree = false;
    }
    selectInTree(tree, root, model.selectedRuleProperty().get());
  }

  private void selectInTree(@NotNull TreeView<Object> tree, @NotNull TreeItem<Object> root,
      @Nullable LipidRuleRow row) {
    if (row == null) {
      return;
    }
    for (final TreeItem<Object> adduct : root.getChildren()) {
      for (final TreeItem<Object> item : adduct.getChildren()) {
        if (item.getValue() == row) {
          if (tree.getSelectionModel().getSelectedItem() != item) {
            tree.getSelectionModel().select(item);
          }
          return;
        }
      }
    }
  }

  private @NotNull MenuButton buildAddAdductMenu() {
    final MenuButton menu = new MenuButton("Add adduct");
    final Menu positive = new Menu("Positive mode");
    final Menu negative = new Menu("Negative mode");
    for (final IonizationType ion : IonizationType.values()) {
      if (ion.getPolarity() != PolarityType.POSITIVE && ion.getPolarity() != PolarityType.NEGATIVE) {
        continue;
      }
      final MenuItem item = new MenuItem(ion.getAdductName());
      item.setOnAction(_ -> addAdduct(ion));
      (ion.getPolarity() == PolarityType.POSITIVE ? positive : negative).getItems().add(item);
    }
    menu.getItems().addAll(positive, negative);
    return menu;
  }

  private void addAdduct(@NotNull IonizationType ion) {
    final LipidRuleRow existing = model.getRules().stream()
        .filter(r -> r.ionizationProperty().get() == ion).findFirst().orElse(null);
    if (existing != null) {
      model.selectedRuleProperty().set(existing);
      return;
    }
    // decision: a new adduct starts as MS1 only, the first fragment replaces that rule
    final LipidRuleRow row = new LipidRuleRow();
    row.ionizationProperty().set(ion);
    row.targetProperty().set(null);
    model.getRules().add(row);
    model.selectedRuleProperty().set(row);
  }

  private void addFragment() {
    final IonizationType ion = selectedAdduct.get();
    if (ion == null) {
      return;
    }
    final List<LipidRuleRow> ofAdduct = model.getRules().stream()
        .filter(r -> r.ionizationProperty().get() == ion).toList();
    if (ofAdduct.size() == 1 && ofAdduct.getFirst().targetProperty().get() == null) {
      ofAdduct.getFirst().targetProperty().set(LipidRuleTarget.HEAD_GROUP);
      model.selectedRuleProperty().set(ofAdduct.getFirst());
      return;
    }
    final LipidRuleRow row = new LipidRuleRow();
    row.ionizationProperty().set(ion);
    final int insertAt = ofAdduct.isEmpty() ? model.getRules().size()
        : model.getRules().indexOf(ofAdduct.getLast()) + 1;
    model.getRules().add(insertAt, row);
    model.selectedRuleProperty().set(row);
  }

  private void remove() {
    final LipidRuleRow row = model.selectedRuleProperty().get();
    if (row != null) {
      model.getRules().remove(row);
      return;
    }
    final IonizationType ion = selectedAdduct.get();
    if (ion == null) {
      return;
    }
    final long count = model.getRules().stream().filter(r -> r.ionizationProperty().get() == ion)
        .count();
    if (count > 1 && !DialogLoggerUtil.showDialogYesNo("Remove adduct?",
        "Remove %s with its %d rules?".formatted(ion.getAdductName(), count))) {
      return;
    }
    model.getRules().removeIf(r -> r.ionizationProperty().get() == ion);
    selectedAdduct.set(null);
  }

  /**
   * Adduct nodes show the precursor m/z of the example lipid, rule nodes their status, sentence
   * and the m/z they produce.
   */
  private class RuleTreeCell extends TreeCell<Object> {

    @Override
    protected void updateItem(Object item, boolean empty) {
      super.updateItem(item, empty);
      setText(null);
      if (empty || item == null) {
        setGraphic(null);
        return;
      }
      if (item instanceof IonizationType ion) {
        final Label name = FxLabels.newBoldLabel(ion.getAdductName());
        final Double mz = model.getPrecursorMzs().get(ion);
        final Label precursor = FxLabels.newLabel(mz == null ? "" : "precursor %.4f".formatted(mz));
        precursor.setStyle(MUTED_STYLE);
        setGraphic(FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, name, precursor));
      } else if (item instanceof LipidRuleRow row) {
        final String issue = row.issueBinding().get();
        final Label status = FxLabels.newLabel(issue == null ? "✓" : "⚠");
        status.setStyle(issue == null ? OK_STYLE : ISSUE_STYLE);
        final Label sentence = FxLabels.newLabel(row.describe());
        final Label mzs = FxLabels.newLabel(formatMzs(model.getSimulatedIons().get(row)));
        mzs.setStyle(MUTED_STYLE);
        setGraphic(FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, status, sentence, mzs));
      }
    }
  }

  private static @NotNull String formatMzs(@Nullable List<SimulatedIon> ions) {
    if (ions == null || ions.isEmpty()) {
      return "";
    }
    final String values = ions.stream().limit(3).map(i -> "%.4f".formatted(i.mz()))
        .collect(Collectors.joining(", "));
    return "→ " + values + (ions.size() > 3 ? ", …" : "");
  }

  // ---------- form ----------

  private @NotNull Node buildForm() {
    final List<Runnable> unbinders = new ArrayList<>();

    final Label title = FxLabels.newBoldTitle("");
    title.setWrapText(true);

    final ComboBox<IonizationType> adduct = new ComboBox<>(
        FXCollections.observableArrayList(IonizationType.values()));
    adduct.setConverter(converter(IonizationType::getAdductName, ""));
    adduct.setTooltip(new Tooltip("Precursor adduct the rule applies to"));

    final ObservableList<LipidRuleTarget> targetItems = FXCollections.observableArrayList();
    targetItems.add(null);
    targetItems.addAll(LipidRuleTarget.values());
    final ComboBox<LipidRuleTarget> target = new ComboBox<>(targetItems);
    target.setConverter(converter(LipidRuleTarget::toString, MS1_ONLY));
    target.setButtonCell(newCell(target.getConverter()));
    target.setCellFactory(_ -> newCell(target.getConverter()));

    // chain modification: atoms added to and removed from the chain
    final StringProperty plusText = new SimpleStringProperty("");
    final StringProperty minusText = new SimpleStringProperty("");
    final TextField plus = FxTextFields.newTextField(plusText,
        "Atoms added to the chain, e.g., H or C3H5O");
    plus.setPromptText("e.g. H");
    final TextField minus = FxTextFields.newTextField(minusText,
        "Atoms removed from the chain, e.g., O or H2O");
    minus.setPromptText("e.g. O");
    final Label plusLabel = FxLabels.newLabel("Plus");
    final Label minusLabel = FxLabels.newLabel("Minus");
    final Label modificationHelp = FxLabels.newSmallLabel("""
        Leave both empty for the plain chain. Fill one or both to change the chain,         e.g., plus H and minus O.""");
    modificationHelp.setWrapText(true);
    for (final Node node : List.of(plus, minus, plusLabel, minusLabel, modificationHelp)) {
      node.managedProperty().bind(node.visibleProperty());
    }

    final StringProperty formulaText = new SimpleStringProperty("");
    final TextField formula = FxTextFields.newTextField(formulaText, null);
    final Label formulaLabel = FxLabels.newLabel("Formula");
    final Label formulaHelp = FxLabels.newSmallLabel("");
    formulaHelp.setWrapText(true);
    for (final Node node : List.of(formula, formulaLabel, formulaHelp)) {
      node.managedProperty().bind(node.visibleProperty());
    }

    final ObservableList<LipidRuleObservation> observationItems = FXCollections.observableArrayList();
    final ComboBox<LipidRuleObservation> observation = new ComboBox<>(observationItems);

    final ComboBox<LipidAnnotationLevel> level = new ComboBox<>(
        FXCollections.observableArrayList(LipidAnnotationLevel.values()));
    final ComboBox<LipidFragmentationRuleRating> rating = new ComboBox<>(
        FXCollections.observableArrayList(LipidFragmentationRuleRating.values()));
    rating.setConverter(converter(r -> r == LipidFragmentationRuleRating.MAJOR
        ? "Major: enough to annotate the class" : "Minor: only supports an annotation", ""));
    final Spinner<Integer> intensity = new Spinner<>(0, 100, 0, 5);
    intensity.setEditable(true);
    intensity.setPrefWidth(90);

    final Label levelHelp = FxLabels.newSmallLabel(
        "Species level confirms the class, e.g., a head group. Molecular species level identifies a chain.");
    levelHelp.setWrapText(true);
    final Label intensityHelp = FxLabels.newSmallLabel(
        "Expected relative intensity 0-100 compared to the other rules of this adduct. 0 ignores it.");
    intensityHelp.setWrapText(true);

    final Label issue = FxLabels.newLabel("");
    issue.setStyle(ISSUE_STYLE);
    issue.setWrapText(true);
    final VBox expectedIons = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);

    final GridPane grid = FxLayout.newGrid2Col(FxLabels.newLabel("Adduct"), adduct,
        FxLabels.newLabel("Fragment contains"), target, plusLabel, plus, minusLabel,
        FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, minus, modificationHelp), formulaLabel, FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, formula,
            formulaHelp), FxLabels.newLabel("Observed as"), observation,
        FxLabels.newLabel("Information"), FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, level,
            levelHelp), FxLabels.newLabel("Rating"), rating,
        FxLabels.newLabel("Rel. intensity"),
        FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, intensity, intensityHelp));

    // labels must not wrap into a narrow column, the input column takes the remaining space
    for (final Node child : grid.getChildren()) {
      if (GridPane.getColumnIndex(child) == null || GridPane.getColumnIndex(child) == 0) {
        if (child instanceof Label label) {
          label.setMinWidth(Region.USE_PREF_SIZE);
          // align with the first line of inputs that have a help text below
          GridPane.setValignment(label, VPos.TOP);
          label.setPadding(new Insets(4, 0, 0, 0));
        }
      }
    }

    // show only the fields and choices that apply to the rule
    final Runnable updateChoices = () -> {
      final LipidRuleRow row = model.selectedRuleProperty().get();
      if (row == null) {
        return;
      }
      final LipidRuleTarget t = row.targetProperty().get();
      observationItems.setAll(t == null ? List.of()
          : LipidFragmentationRuleType.validObservations(t, row.getModification()));
      observation.setValue(row.observationProperty().get());
      observation.setDisable(observationItems.size() <= 1);
      final boolean chain = t != null && t.getChainType() != null;
      for (final Node node : List.of(plus, minus, plusLabel, minusLabel, modificationHelp)) {
        node.setVisible(chain);
      }
      final boolean fragment = t != null;
      level.setDisable(!fragment);
      rating.setDisable(!fragment);
      intensity.setDisable(!fragment);
      final boolean needsFormula = t == LipidRuleTarget.HEAD_GROUP;
      formula.setVisible(needsFormula);
      formulaLabel.setVisible(needsFormula);
      formulaHelp.setVisible(needsFormula);
      formulaLabel.setText(formulaLabel(row));
      formulaHelp.setText(formulaHelp(row));
      formula.setPromptText(formulaPrompt(row));
    };

    final Runnable updateDerived = () -> {
      final LipidRuleRow row = model.selectedRuleProperty().get();
      if (row == null) {
        return;
      }
      title.setText(row.ionizationProperty().get().getAdductName() + "  ·  " + row.describe());
      final String text = row.issueBinding().get();
      issue.setText(text == null ? "" : text);
      expectedIons.getChildren().setAll(expectedIonLabels(row));
    };

    final Runnable bindRow = () -> {
      unbinders.forEach(Runnable::run);
      unbinders.clear();
      final LipidRuleRow row = model.selectedRuleProperty().get();
      if (row == null) {
        return;
      }
      unbinders.add(syncCombo(adduct, row.ionizationProperty(), false));
      unbinders.add(syncCombo(target, row.targetProperty(), true));
      unbinders.add(syncCombo(observation, row.observationProperty(), false));
      unbinders.add(syncCombo(level, row.levelProperty(), false));
      unbinders.add(syncCombo(rating, row.ratingProperty(), false));
      formulaText.bindBidirectional(row.formulaProperty());
      unbinders.add(() -> formulaText.unbindBidirectional(row.formulaProperty()));
      plusText.bindBidirectional(row.plusFormulaProperty());
      unbinders.add(() -> plusText.unbindBidirectional(row.plusFormulaProperty()));
      minusText.bindBidirectional(row.minusFormulaProperty());
      unbinders.add(() -> minusText.unbindBidirectional(row.minusFormulaProperty()));
      final ObjectProperty<Integer> intensityValue = row.relativeIntensityProperty().asObject();
      intensity.getValueFactory().valueProperty().bindBidirectional(intensityValue);
      unbinders.add(
          () -> intensity.getValueFactory().valueProperty().unbindBidirectional(intensityValue));

      final InvalidationListener onChange = _ -> {
        updateChoices.run();
        updateDerived.run();
      };
      final List<Observable> watched = List.of(row.targetProperty(), row.observationProperty(),
          row.formulaProperty(), row.plusFormulaProperty(), row.minusFormulaProperty(),
          row.ionizationProperty());
      for (final Observable property : watched) {
        property.addListener(onChange);
        unbinders.add(() -> property.removeListener(onChange));
      }
      updateChoices.run();
      updateDerived.run();
    };
    model.selectedRuleProperty().addListener((_, _, _) -> bindRow.run());
    model.getSimulatedIons()
        .addListener((MapChangeListener<LipidRuleRow, List<SimulatedIon>>) _ -> updateDerived.run());
    bindRow.run();

    final VBox form = FxLayout.newVBox(Pos.TOP_LEFT, new Insets(0, 0, 0, 10), true, title, grid,
        issue, FxLabels.newBoldLabel("Expected ions"), expectedIons);
    final Label placeholder = FxLabels.newLabel(
        "Select a rule in the tree, or add an adduct to start.");
    final BorderPane wrapper = new BorderPane();
    wrapper.centerProperty().bind(Bindings.when(model.selectedRuleProperty().isNull())
        .then((Node) placeholder).otherwise(form));
    return wrapper;
  }

  private @NotNull List<Node> expectedIonLabels(@NotNull LipidRuleRow row) {
    if (row.targetProperty().get() == null) {
      return List.of(FxLabels.newLabel("No MS2 fragment, the adduct is annotated by MS1 only."));
    }
    if (row.issueBinding().get() != null) {
      return List.of(FxLabels.newLabel("Fix the rule to see its m/z."));
    }
    final List<SimulatedIon> ions = model.getSimulatedIons().get(row);
    if (ions == null) {
      return List.of(FxLabels.newLabel(
          "Complete the backbone formula and chains to calculate the m/z."));
    }
    if (ions.isEmpty()) {
      return List.of(FxLabels.newLabel(
          "No ion for the example lipid. Does the class have this chain type?"));
    }
    final List<Node> labels = new ArrayList<>();
    labels.add(FxLabels.newSmallLabel("For the example lipid " + model.exampleLipidNameProperty()
        .get() + ":"));
    for (final SimulatedIon ion : ions) {
      labels.add(FxLabels.newLabel("m/z %.4f    %s%s".formatted(ion.mz(), ion.ionFormula(),
          ion.chain() == null ? "" : "    " + ion.chain())));
    }
    return labels;
  }

  private static @NotNull String formulaLabel(@NotNull LipidRuleRow row) {
    return row.observationProperty().get() == LipidRuleObservation.NEUTRAL_LOSS ? "Neutral loss"
        : "Fragment ion";
  }

  private static @NotNull String formulaPrompt(@NotNull LipidRuleRow row) {
    return row.observationProperty().get() == LipidRuleObservation.NEUTRAL_LOSS ? "e.g. C2H8NO4P"
        : "e.g. C5H15NO4P+";
  }

  private static @NotNull String formulaHelp(@NotNull LipidRuleRow row) {
    return row.observationProperty().get() == LipidRuleObservation.NEUTRAL_LOSS
        ? "Neutral formula lost from the precursor ion."
        : "Formula of the fragment ion with its charge, e.g., + or -.";
  }

  /**
   * Keeps a combo box and a property in sync without writing null into non-null properties, which
   * happens when the items of a combo box change.
   *
   * @return removes the listeners
   */
  private static <T> @NotNull Runnable syncCombo(@NotNull ComboBox<T> combo,
      @NotNull ObjectProperty<T> property, boolean allowNull) {
    final boolean[] updating = {false};
    final ChangeListener<T> toCombo = (_, _, value) -> {
      updating[0] = true;
      combo.setValue(value);
      updating[0] = false;
    };
    final ChangeListener<T> toProperty = (_, _, value) -> {
      if (!updating[0] && (value != null || allowNull) && !Objects.equals(value, property.get())) {
        property.set(value);
      }
    };
    updating[0] = true;
    combo.setValue(property.get());
    updating[0] = false;
    property.addListener(toCombo);
    combo.valueProperty().addListener(toProperty);
    return () -> {
      property.removeListener(toCombo);
      combo.valueProperty().removeListener(toProperty);
    };
  }

  private static <T> @NotNull StringConverter<T> converter(@NotNull Function<T, String> toString,
      @NotNull String nullText) {
    return new StringConverter<>() {
      @Override
      public String toString(T value) {
        return value == null ? nullText : toString.apply(value);
      }

      @Override
      public T fromString(String string) {
        return null;
      }
    };
  }

  private static <T> @NotNull ListCell<T> newCell(@NotNull StringConverter<T> converter) {
    return new ListCell<>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        // null is a valid choice (MS1 only), so it is shown unless the cell is empty
        setText(empty ? null : converter.toString(item));
      }
    };
  }

  /**
   * @return all adducts used by the rules, in order of first use
   */
  static @NotNull List<IonizationType> adductsOf(@NotNull List<LipidRuleRow> rules) {
    return rules.stream().map(r -> r.ionizationProperty().get()).distinct().toList();
  }
}
