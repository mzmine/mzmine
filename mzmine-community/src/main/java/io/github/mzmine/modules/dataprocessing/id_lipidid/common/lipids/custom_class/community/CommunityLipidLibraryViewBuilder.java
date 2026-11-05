package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community.CommunityLipidLibraryModel.SourceFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Table of the community lipid library with search, details and import.
 */
class CommunityLipidLibraryViewBuilder extends FxViewBuilder<CommunityLipidLibraryModel> {

  CommunityLipidLibraryViewBuilder(@NotNull CommunityLipidLibraryModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final TableView<LipidLibraryEntry> table = buildTable();
    final SplitPane split = new SplitPane(table, buildDetails());
    split.setDividerPositions(0.65);

    final BorderPane root = FxLayout.newBorderPane(split);
    root.setTop(buildToolbar());
    root.setBottom(buildActionBar());
    return root;
  }

  private @NotNull Node buildToolbar() {
    final TextField search = FxTextFields.newTextField(model.searchProperty(),
        "Search name, abbreviation, formula, author or tag");
    search.setPromptText("Search the community library…");
    HBox.setHgrow(search, Priority.ALWAYS);
    final var source = FxComboBox.createComboBox("Which classes to show",
        Arrays.asList(SourceFilter.values()), model.sourceFilterProperty());
    final Button reload = FxButtons.createButton("Reload", "Download the catalog again",
        () -> run(model.reloadActionProperty().get()));
    FxButtons.disableIf(reload, model.loadingProperty());
    final Hyperlink website = FxLabels.newHyperlink(
        () -> MZmineCore.getDesktop().openWebPage(LipidLibraryClient.WEBSITE_URL),
        "Open website");
    return FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, search, source,
        reload, website);
  }

  private @NotNull TableView<LipidLibraryEntry> buildTable() {
    final FilteredList<LipidLibraryEntry> filtered = new FilteredList<>(model.getEntries());
    filtered.predicateProperty().bind(Bindings.createObjectBinding(() -> {
      final String query = model.searchProperty().get().strip().toLowerCase();
      final SourceFilter source = model.sourceFilterProperty().get();
      return (LipidLibraryEntry e) -> switch (source) {
        case ALL -> true;
        case COMMUNITY -> e.isCommunity();
        case BUILTIN -> !e.isCommunity();
      } && (query.isEmpty() || Arrays.stream(query.split("\\s+"))
          .allMatch(e.searchText()::contains));
    }, model.searchProperty(), model.sourceFilterProperty()));

    final TableView<LipidLibraryEntry> table = new TableView<>();
    final SortedList<LipidLibraryEntry> sorted = new SortedList<>(filtered);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setItems(sorted);
    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    final Button load = FxButtons.createButton("Load the community library",
        () -> run(model.reloadActionProperty().get()));
    final Label placeholderText = FxLabels.newLabel(Bindings.createStringBinding(() -> {
      if (model.loadingProperty().get()) {
        return "Loading…";
      }
      return model.loadedProperty().get() ? "No lipid class matches your search."
          : "The community library is loaded from the internet.";
    }, model.loadingProperty(), model.loadedProperty()));
    load.visibleProperty().bind(model.loadedProperty().not().and(model.loadingProperty().not()));
    table.setPlaceholder(FxLayout.newVBox(Pos.CENTER, Insets.EMPTY, placeholderText, load));

    table.getColumns().addAll(List.of(column("Abbr.", 70, LipidLibraryEntry::abbr),
        column("Name", 220, LipidLibraryEntry::name),
        column("Main class", 150, e -> mainClassLabel(e.mainClass())),
        column("Mode", 60, e -> polarities(e.polarities())),
        column("Authors", 140, LipidLibraryEntry::authorNames),
        column("Source", 90, e -> e.isCommunity() ? "Community" : "Built-in")));

    table.getSelectionModel().getSelectedItems()
        .addListener((ListChangeListener<LipidLibraryEntry>) _ -> model.getSelected()
            .setAll(table.getSelectionModel().getSelectedItems()));
    return table;
  }

  private static @NotNull TableColumn<LipidLibraryEntry, String> column(@NotNull String title,
      double prefWidth, @NotNull Function<LipidLibraryEntry, String> value) {
    final TableColumn<LipidLibraryEntry, String> column = new TableColumn<>(title);
    column.setCellValueFactory(
        cd -> new ReadOnlyStringWrapper(Objects.requireNonNullElse(value.apply(cd.getValue()), "")));
    column.setPrefWidth(prefWidth);
    return column;
  }

  private static @NotNull String polarities(@Nullable List<String> polarities) {
    if (polarities == null) {
      return "";
    }
    return String.join(" ", polarities.stream()
        .map(p -> "POSITIVE".equals(p) ? "+" : "NEGATIVE".equals(p) ? "−" : p).toList());
  }

  /**
   * @param mainClass enum name of a main class
   * @return the display name of the main class, or a readable form of unknown names
   */
  private static @NotNull String mainClassLabel(@Nullable String mainClass) {
    if (mainClass == null) {
      return "";
    }
    try {
      return LipidMainClasses.valueOf(mainClass).getName();
    } catch (IllegalArgumentException e) {
      return humanize(mainClass);
    }
  }

  private static @NotNull String humanize(@Nullable String enumName) {
    if (enumName == null || enumName.isEmpty()) {
      return "";
    }
    final String s = enumName.toLowerCase().replace('_', ' ');
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private @NotNull Node buildDetails() {
    final VBox details = FxLayout.newVBox(Pos.TOP_LEFT, FxLayout.DEFAULT_PADDING_INSETS, true);
    final Runnable update = () -> {
      if (model.getSelected().size() != 1) {
        final int n = model.getSelected().size();
        details.getChildren().setAll(FxLabels.newLabel(n == 0 ? "Select a lipid class to see details."
            : n + " lipid classes selected."));
        return;
      }
      details.getChildren().setAll(detailsOf(model.getSelected().getFirst()));
    };
    model.getSelected().addListener((ListChangeListener<LipidLibraryEntry>) _ -> update.run());
    update.run();
    return details;
  }

  private @NotNull List<Node> detailsOf(@NotNull LipidLibraryEntry e) {
    final Label title = FxLabels.newBoldTitle(e.abbr() + "  " + e.name());
    title.setWrapText(true);
    final Label description = FxLabels.newLabel(Objects.requireNonNullElse(e.description(), ""));
    description.setWrapText(true);
    final Label authors = FxLabels.newLabel("Authors: " + e.authorNames());
    authors.setWrapText(true);
    final Label formula = FxLabels.newLabel(
        "Backbone formula: " + Objects.requireNonNullElse(e.backBoneFormula(), ""));
    final Label chains = FxLabels.newLabel("Chains: " + (e.chainTypes() == null ? ""
        : String.join(", ", e.chainTypes().stream().map(CommunityLipidLibraryViewBuilder::humanize)
            .toList())));
    chains.setWrapText(true);

    final Label rights;
    final Hyperlink license;
    if (e.isCommunity()) {
      rights = FxLabels.newLabel(
          "© " + Objects.requireNonNullElse(e.rightsHolder(), "the authors") + ". Free to use in "
              + "mzmine for your analyses. Redistribution needs permission.");
      license = FxLabels.newHyperlink(
          () -> MZmineCore.getDesktop().openWebPage(LipidLibraryClient.TERMS_URL), "Terms of use");
    } else {
      rights = FxLabels.newLabel("Part of mzmine, MIT license.");
      license = null;
    }
    rights.setWrapText(true);

    final Hyperlink web = FxLabels.newHyperlink(
        () -> MZmineCore.getDesktop().openWebPage(LipidLibraryClient.websiteUrl(e)),
        "Show rules on the website");

    final Label version = e.requiresNewerMzmine() ? FxLabels.newLabel(
        "Requires mzmine " + e.minMzmineVersion() + " or newer.") : null;
    if (version != null) {
      version.setStyle("-fx-text-fill: #e0a400;");
    }
    final Label instrument = e.instrument() == null || e.instrument().isBlank() ? null
        : FxLabels.newLabel("Tested on: " + e.instrument());

    return Stream.of(title, version, description, formula, chains, authors,
        instrument, web, rights, license).filter(Objects::nonNull).map(n -> (Node) n).toList();
  }

  private @NotNull Node buildActionBar() {
    final Button importButton = FxButtons.createButton("Add to my library",
        "Download the selected classes and save them in your local library",
        () -> run(model.importActionProperty().get()));
    FxButtons.disableIf(importButton,
        Bindings.isEmpty(model.getSelected()).or(model.loadingProperty()));
    final Label status = FxLabels.newLabel(model.statusProperty());
    status.setWrapText(true);
    final Hyperlink terms = FxLabels.newHyperlink(
        () -> MZmineCore.getDesktop().openWebPage(LipidLibraryClient.TERMS_URL), "Terms of use");
    return FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, importButton,
        status, terms);
  }

  private static void run(@Nullable Runnable action) {
    if (action != null) {
      action.run();
    }
  }
}
