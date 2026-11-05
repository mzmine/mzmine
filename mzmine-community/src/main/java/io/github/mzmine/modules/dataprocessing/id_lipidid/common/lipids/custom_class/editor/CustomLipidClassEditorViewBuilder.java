package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import javafx.scene.Group;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import javafx.collections.MapChangeListener;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation.SimulatedIon;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleTarget;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassJson;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassPreviewPane;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Delete;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Duplicate;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Export;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.FromTemplate;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Import;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.NewClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Open;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Revert;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Save;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.Share;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library.CustomLipidClassPreset;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.FormulaUtils;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the custom lipid class editor: local library on the left, the class form and rule table
 * in the center and a live preview on the right. The community library is a second tab.
 */
class CustomLipidClassEditorViewBuilder extends FxViewBuilder<CustomLipidClassEditorModel> {

  private static final String MS1_ONLY = "MS1 adduct only";
  private final Supplier<Region> communityViewSupplier;
  private final Runnable onCommunityShown;
  private final BooleanProperty libraryExpanded = new SimpleBooleanProperty(true);
  // guards against loops between list selection and model selection
  private boolean updatingSelection = false;

  CustomLipidClassEditorViewBuilder(@NotNull CustomLipidClassEditorModel model,
      @NotNull Supplier<Region> communityViewSupplier, @NotNull Runnable onCommunityShown) {
    super(model);
    this.communityViewSupplier = communityViewSupplier;
    this.onCommunityShown = onCommunityShown;
  }

  @Override
  public Region build() {
    final SplitPane editorAndPreview = new SplitPane(buildEditor(), buildPreview());
    editorAndPreview.setDividerPositions(0.6);
    // the library is a side panel that collapses to a narrow strip to give the editor space
    final Region libraryPane = buildLibraryPane();
    libraryPane.setPrefWidth(300);
    libraryPane.setMinWidth(240);
    final Node collapsedStrip = buildCollapsedLibraryStrip();
    final BorderPane main = new BorderPane(editorAndPreview);
    main.leftProperty().bind(
        Bindings.when(libraryExpanded).then((Node) libraryPane).otherwise(collapsedStrip));

    final Tab myClasses = new Tab("My lipid classes", main);
    final Tab community = new Tab("Community library", communityViewSupplier.get());
    // decision: load the catalog from the internet only when the user looks at it
    community.setOnSelectionChanged(_ -> {
      if (community.isSelected()) {
        onCommunityShown.run();
      }
    });
    final TabPane tabs = new TabPane(myClasses, community);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    return tabs;
  }

  // ---------- library ----------

  private @NotNull Region buildLibraryPane() {
    final FilteredList<CustomLipidClassPreset> filtered = new FilteredList<>(
        model.getLibraryPresets());
    filtered.predicateProperty().bind(Bindings.createObjectBinding(() -> {
      final String query = model.librarySearchProperty().get().strip().toLowerCase();
      return query.isEmpty() ? _ -> true
          : (CustomLipidClassPreset p) -> p.name().toLowerCase().contains(query);
    }, model.librarySearchProperty()));

    final ListView<CustomLipidClassPreset> list = new ListView<>(
        filtered.sorted((a, b) -> a.name().compareToIgnoreCase(b.name())));
    list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    list.setPlaceholder(FxLabels.newLabel("""
        No saved lipid classes yet.
        Create a new class, start from a built-in
        template or import classes from the
        community library."""));
    list.setCellFactory(_ -> new ListCell<>() {
      @Override
      protected void updateItem(CustomLipidClassPreset item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          return;
        }
        final CustomLipidClass c = item.lipidClass();
        final Label abbr = FxLabels.newBoldLabel(c.getAbbr());
        final Label name = FxLabels.newLabel(c.getName());
        final Label category = FxLabels.newSmallLabel(
            c.getMainClass() == null ? "" : c.getMainClass().getName());
        setText(null);
        setGraphic(FxLayout.newVBox(Insets.EMPTY, FxLayout.newHBox(Insets.EMPTY, abbr, name),
            category));
      }
    });

    // list selection -> open class
    list.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
      if (updatingSelection || selected == null
          || list.getSelectionModel().getSelectedItems().size() != 1) {
        return;
      }
      model.selectedPresetProperty().set(selected);
      model.sendEvent(new Open(selected.lipidClass()));
    });
    // model selection -> list selection, e.g., after saving or when a change was not discarded
    model.selectedPresetProperty().addListener((_, _, preset) -> {
      updatingSelection = true;
      try {
        if (preset == null) {
          list.getSelectionModel().clearSelection();
        } else {
          list.getSelectionModel().clearSelection();
          list.getSelectionModel().select(preset);
        }
      } finally {
        updatingSelection = false;
      }
    });

    final TextField search = FxTextFields.newTextField(model.librarySearchProperty(),
        "Search your lipid classes");
    search.setPromptText("Search…");
    search.setMaxWidth(Double.MAX_VALUE);
    list.setMaxWidth(Double.MAX_VALUE);

    final Button newButton = FxButtons.createButton("New", "Create an empty lipid class",
        () -> model.sendEvent(new NewClass()));
    final MenuButton templates = buildTemplateMenu();
    final Button importButton = FxButtons.createButton("Import…",
        "Import lipid classes from a JSON file into your library",
        () -> chooseImportFile(list));
    final Button exportButton = FxButtons.createButton("Export…",
        "Export the selected lipid classes to a JSON file", () -> chooseExportFile(list));
    FxButtons.disableIf(exportButton,
        Bindings.isEmpty(list.getSelectionModel().getSelectedItems()));
    final Button deleteButton = FxButtons.createButton("Delete",
        "Delete the selected lipid classes from your library", () -> model.sendEvent(
            new Delete(selectedClasses(list))));
    FxButtons.disableIf(deleteButton,
        Bindings.isEmpty(list.getSelectionModel().getSelectedItems()));

    final Label hint = FxLabels.newSmallLabel("""
        Saved classes can be added to the lipid annotation module \
        under Custom lipid classes → From library.""");
    hint.setWrapText(true);

    VBox.setVgrow(list, Priority.ALWAYS);
    return FxLayout.newVBox(Pos.TOP_LEFT, FxLayout.DEFAULT_PADDING_INSETS, true,
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, FxLabels.newBoldTitle("My lipid classes"),
            FxLayout.newHVFillSpacer(), FxButtons.createButton("◀",
                "Hide the library to give the editor more space",
                () -> libraryExpanded.set(false))), search, list,
        FxLayout.newFlowPane(newButton, templates, importButton, exportButton, deleteButton),
        hint);
  }

  /**
   * Narrow strip shown instead of the library when it is hidden.
   */
  private @NotNull Node buildCollapsedLibraryStrip() {
    final Button show = FxButtons.createButton("▶", "Show your lipid class library",
        () -> libraryExpanded.set(true));
    final Label title = FxLabels.newLabel(Bindings.createStringBinding(
        () -> "My lipid classes (%d)".formatted(model.getLibraryPresets().size()),
        model.getLibraryPresets()));
    title.setRotate(-90);
    // a group makes the layout use the rotated bounds
    final Group rotated = new Group(title);
    final VBox strip = FxLayout.newVBox(Pos.TOP_CENTER, FxLayout.DEFAULT_PADDING_INSETS, show,
        rotated);
    strip.setOnMouseClicked(_ -> libraryExpanded.set(true));
    strip.setStyle("-fx-cursor: hand;");
    return strip;
  }

  private static @NotNull List<CustomLipidClass> selectedClasses(
      @NotNull ListView<CustomLipidClassPreset> list) {
    return list.getSelectionModel().getSelectedItems().stream()
        .map(CustomLipidClassPreset::lipidClass).toList();
  }

  private @NotNull MenuButton buildTemplateMenu() {
    final MenuButton templates = new MenuButton("From template");
    templates.setTooltip(new Tooltip("Start a new class from a built-in mzmine lipid class"));
    for (final LipidCategories category : LipidCategories.values()) {
      final List<LipidClasses> classes = Arrays.stream(LipidClasses.values())
          .filter(c -> c.getCoreClass() == category).toList();
      if (classes.isEmpty()) {
        continue;
      }
      final Menu menu = new Menu(category.getName());
      for (final LipidClasses lipidClass : classes) {
        final MenuItem item = new MenuItem(lipidClass.getAbbr() + "  " + lipidClass.getName());
        item.setOnAction(_ -> model.sendEvent(new FromTemplate(lipidClass)));
        menu.getItems().add(item);
      }
      templates.getItems().add(menu);
    }
    return templates;
  }

  private void chooseImportFile(@NotNull Node owner) {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Import lipid classes");
    chooser.getExtensionFilters()
        .add(new ExtensionFilter("Lipid classes (JSON)", CustomLipidClassJson.FILE_EXTENSION));
    final File file = chooser.showOpenDialog(owner.getScene().getWindow());
    if (file != null) {
      model.sendEvent(new Import(file.toPath()));
    }
  }

  private void chooseExportFile(@NotNull ListView<CustomLipidClassPreset> list) {
    final List<CustomLipidClass> classes = selectedClasses(list);
    if (classes.isEmpty()) {
      return;
    }
    final FileChooser chooser = new FileChooser();
    chooser.setTitle("Export lipid classes");
    chooser.getExtensionFilters()
        .add(new ExtensionFilter("Lipid classes (JSON)", CustomLipidClassJson.FILE_EXTENSION));
    chooser.setInitialFileName(classes.size() == 1 ? classes.getFirst().getAbbr() + ".json"
        : "lipid_classes.json");
    final File file = chooser.showSaveDialog(list.getScene().getWindow());
    if (file != null) {
      model.sendEvent(new Export(file.toPath(), classes));
    }
  }

  // ---------- editor ----------

  private @NotNull Region buildEditor() {
    final VBox form = FxLayout.newVBox(Pos.TOP_LEFT, FxLayout.DEFAULT_PADDING_INSETS, true,
        buildHeader(),
        newSection("1  Class", buildClassForm()), newSection("2  Chains", buildChainEditor()),
        newSection("3  Adducts and fragmentation rules",
            new LipidRulesViewBuilder(model).build()),
        buildMessages());
    final ScrollPane scroll = new ScrollPane(form);
    scroll.setFitToWidth(true);

    final BorderPane editor = FxLayout.newBorderPane(scroll);
    editor.setBottom(buildActionBar());

    final Label placeholder = FxLabels.newLabel("""
        Select a lipid class in your library, create a new one
        or start from a built-in template.""");
    final Button newButton = FxButtons.createButton("New lipid class",
        () -> model.sendEvent(new NewClass()));
    final VBox empty = FxLayout.newVBox(Pos.CENTER, FxLayout.DEFAULT_PADDING_INSETS, placeholder,
        newButton, buildTemplateMenu());

    final BorderPane wrapper = new BorderPane();
    wrapper.centerProperty().bind(
        Bindings.when(model.editingProperty()).then((Node) editor).otherwise(empty));
    return wrapper;
  }

  private @NotNull Node buildHeader() {
    final Label title = FxLabels.newBoldTitle(Bindings.createStringBinding(() -> {
      final String abbr = model.abbrProperty().get();
      final String name = model.nameProperty().get();
      final String text = (abbr + " " + name).strip();
      return text.isEmpty() ? "New lipid class" : text;
    }, model.abbrProperty(), model.nameProperty()));
    final Label state = FxLabels.newItalicLabel(Bindings.createStringBinding(() -> {
      if (model.originalProperty().get() == null) {
        return "not saved yet";
      }
      return model.modifiedProperty().get() ? "unsaved changes" : "saved in your library";
    }, model.originalProperty(), model.modifiedProperty()));
    return FxLayout.newHBox(Pos.BASELINE_LEFT, Insets.EMPTY, title, state);
  }

  private static @NotNull TitledPane newSection(@NotNull String title, @NotNull Node content) {
    final TitledPane pane = new TitledPane(title, content);
    pane.setCollapsible(true);
    pane.setMaxWidth(Double.MAX_VALUE);
    return pane;
  }

  private @NotNull Node buildClassForm() {
    final TextField name = FxTextFields.newTextField(model.nameProperty(),
        "Full name of the lipid class, e.g., Diacylglycerophosphocholines");
    final TextField abbr = FxTextFields.newTextField(model.abbrProperty(),
        "Abbreviation used in annotations, e.g., PC");

    final ComboBox<LipidCategories> category = FxComboBox.createComboBox(
        "LIPID MAPS category of the class", Arrays.asList(LipidCategories.values()),
        model.categoryProperty());

    // main classes of the selected category first, so the choice stays short
    final ObservableList<LipidMainClasses> mainClasses = FXCollections.observableArrayList();
    final Runnable updateMainClasses = () -> {
      final LipidCategories selected = model.categoryProperty().get();
      mainClasses.setAll(Arrays.stream(LipidMainClasses.values())
          .filter(m -> selected == null || m.getLipidCategory() == selected).toList());
      final LipidMainClasses current = model.mainClassProperty().get();
      if (current != null && !mainClasses.contains(current)) {
        mainClasses.add(current);
      }
    };
    updateMainClasses.run();
    model.categoryProperty().addListener((_, _, _) -> updateMainClasses.run());
    model.mainClassProperty().addListener((_, _, _) -> updateMainClasses.run());
    final ComboBox<LipidMainClasses> mainClass = FxComboBox.createComboBox(
        "Main class within the category", mainClasses, model.mainClassProperty());

    final TextField backbone = FxTextFields.newTextField(model.backboneFormulaProperty(),
        "Formula of the lipid without its chains, e.g., C8H20NO6P for PC");
    final Label backboneMass = FxLabels.newLabel(
        Bindings.createStringBinding(() -> formatMass(model.backboneFormulaProperty().get()),
            model.backboneFormulaProperty()));

    return FxLayout.newGrid2Col(FxLabels.newLabel("Name"), name,
        FxLabels.newLabel("Abbreviation"), abbr, FxLabels.newLabel("Category"), category,
        FxLabels.newLabel("Main class"), mainClass, FxLabels.newLabel("Backbone formula"),
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, backbone, deriveBackbone(backbone),
            backboneMass));
  }

  private @NotNull Button deriveBackbone(@NotNull Node owner) {
    final Button derive = FxButtons.createButton("Derive…",
        "Calculate the backbone formula from one known lipid of the class", () -> {
          if (model.getChains().isEmpty()) {
            DialogLoggerUtil.showMessageDialog("Add chains first",
                "Add the chains of the class in section 2, then derive the backbone.");
            return;
          }
          BackboneDeriveDialog.show(owner.getScene().getWindow(),
                  model.getChains().toArray(LipidChainType[]::new))
              .ifPresent(model.backboneFormulaProperty()::set);
        });
    return derive;
  }

  private static @NotNull String formatMass(@Nullable String formula) {
    if (formula == null || formula.isBlank()) {
      return "";
    }
    final var parsed = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula.strip());
    return parsed == null ? "cannot read formula"
        : "%.4f Da".formatted(FormulaUtils.getMonoisotopicMass(parsed));
  }

  private @NotNull Node buildChainEditor() {
    final ListView<LipidChainType> chains = new ListView<>(model.getChains());
    chains.setMaxWidth(420);
    chains.setPrefHeight(110);
    chains.setPlaceholder(FxLabels.newLabel("No chains, add the chains of the class below"));
    chains.setCellFactory(_ -> new ListCell<>() {
      @Override
      protected void updateItem(LipidChainType item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          return;
        }
        final LipidRuleTarget target = LipidRuleTarget.ofChainType(item);
        setText("#%d  %s".formatted(getIndex() + 1, target == null ? item : target));
      }
    });

    final ObjectProperty<LipidChainType> toAdd = new SimpleObjectProperty<>(
        LipidChainType.ACYL_CHAIN);
    final List<LipidChainType> addable = Arrays.stream(LipidChainType.values())
        .filter(c -> c != LipidChainType.TWO_ACYL_CHAINS_COMBINED).toList();
    final ComboBox<LipidChainType> chainType = FxComboBox.createComboBox("Chain type to add",
        addable, toAdd);
    chainType.setConverter(new StringConverter<>() {
      @Override
      public String toString(LipidChainType c) {
        final LipidRuleTarget target = c == null ? null : LipidRuleTarget.ofChainType(c);
        return target == null ? String.valueOf(c) : target.toString();
      }

      @Override
      public LipidChainType fromString(String string) {
        return null;
      }
    });

    final Button add = FxButtons.createButton("Add chain", () -> {
      if (toAdd.get() != null) {
        model.getChains().add(toAdd.get());
      }
    });
    final Button remove = FxButtons.createButton("Remove", "Remove the selected chain", () -> {
      final int index = chains.getSelectionModel().getSelectedIndex();
      if (index >= 0) {
        model.getChains().remove(index);
      }
    });
    final Button up = FxButtons.createButton("↑", "Move the selected chain up",
        () -> moveChain(chains, -1));
    final Button down = FxButtons.createButton("↓", "Move the selected chain down",
        () -> moveChain(chains, 1));
    final Label hint = FxLabels.newSmallLabel(
        "The order defines the order in annotations, e.g., the sphingoid base first.");
    return FxLayout.newVBox(Insets.EMPTY, chains,
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, chainType, add, remove, up, down), hint);
  }

  private void moveChain(@NotNull ListView<LipidChainType> chains, int direction) {
    final int index = chains.getSelectionModel().getSelectedIndex();
    final int target = index + direction;
    if (index < 0 || target < 0 || target >= model.getChains().size()) {
      return;
    }
    final LipidChainType chain = model.getChains().remove(index);
    model.getChains().add(target, chain);
    chains.getSelectionModel().select(target);
  }

  private @NotNull Node buildMessages() {
    final VBox issues = new VBox(2);
    final VBox warnings = new VBox(2);
    final Runnable update = () -> {
      issues.getChildren().setAll(model.getIssues().stream()
          .map(i -> styled(FxLabels.newLabel("✗ " + i), "-fx-text-fill: #d9534f;")).toList());
      warnings.getChildren().setAll(model.getWarnings().stream()
          .map(w -> styled(FxLabels.newLabel("⚠ " + w), "-fx-text-fill: #e0a400;")).toList());
    };
    model.getIssues().addListener((ListChangeListener<String>) _ -> update.run());
    model.getWarnings().addListener((ListChangeListener<String>) _ -> update.run());
    update.run();
    return FxLayout.newVBox(Insets.EMPTY, issues, warnings);
  }

  private static @NotNull Label styled(@NotNull Label label, @NotNull String style) {
    label.setStyle(style);
    label.setWrapText(true);
    return label;
  }

  private @NotNull Node buildActionBar() {
    final Button save = FxButtons.createButton("Save to library",
        "Save the class to your local lipid class library", () -> model.sendEvent(new Save()));
    FxButtons.disableIf(save, model.validClassProperty().isNull());
    final Button revert = FxButtons.createButton("Revert", "Discard unsaved changes",
        () -> model.sendEvent(new Revert()));
    FxButtons.disableIf(revert, model.modifiedProperty().not());
    final Button duplicate = FxButtons.createButton("Save as copy…",
        "Continue editing a copy of this class", () -> model.sendEvent(new Duplicate()));
    final Button share = FxButtons.createButton("Share with community…",
        "Submit this class to the mzmine custom lipid library", () -> model.sendEvent(new Share()));
    FxButtons.disableIf(share, model.validClassProperty().isNull());

    final Label issueCount = FxLabels.newLabel(Bindings.createStringBinding(() -> {
      final int n = model.getIssues().size();
      return n == 0 ? "" : n == 1 ? "1 issue to fix" : n + " issues to fix";
    }, model.getIssues()));
    issueCount.setStyle("-fx-text-fill: #d9534f;");
    return FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, save, revert,
        duplicate, share, issueCount);
  }

  // ---------- preview ----------

  private @NotNull Region buildPreview() {
    final CustomLipidClassPreviewPane preview = new CustomLipidClassPreviewPane();
    preview.setOnExampleChanged(model.exampleProperty()::set);
    // decision: debounce so typing a formula does not rebuild the spectra on every key stroke
    final PauseTransition delay = new PauseTransition(Duration.millis(400));
    delay.setOnFinished(_ -> {
      preview.setLipidClass(model.validClassProperty().get());
      highlightSelectedRule(preview);
    });
    model.validClassProperty().addListener((_, _, _) -> delay.playFromStart());
    model.selectedRuleProperty().addListener((_, _, _) -> highlightSelectedRule(preview));
    model.getSimulatedIons().addListener(
        (MapChangeListener<LipidRuleRow, List<SimulatedIon>>) _ -> highlightSelectedRule(preview));

    final Label empty = FxLabels.newLabel(new SimpleStringProperty(
        "The preview shows example annotations and simulated MS2 spectra once the class has no issues. "
            + "Change the example chains to see other m/z values. The selected rule is marked in orange."));
    empty.setWrapText(true);
    final BorderPane previewPane = FxLayout.newBorderPane(preview);
    previewPane.setTop(FxLayout.newVBox(FxLayout.DEFAULT_PADDING_INSETS, empty));

    final Tab previewTab = new Tab("Preview", previewPane);
    final Tab testTab = new Tab("Test with spectrum", new SpectrumTestViewBuilder(model).build());
    final TabPane tabs = new TabPane(previewTab, testTab);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    return tabs;
  }

  private void highlightSelectedRule(@NotNull CustomLipidClassPreviewPane preview) {
    final LipidRuleRow row = model.selectedRuleProperty().get();
    final List<SimulatedIon> ions = row == null ? null : model.getSimulatedIons().get(row);
    if (row == null || ions == null) {
      preview.highlight(null, List.of());
      return;
    }
    preview.highlight(row.ionizationProperty().get(), ions.stream().map(SimulatedIon::mz).toList());
  }
}
