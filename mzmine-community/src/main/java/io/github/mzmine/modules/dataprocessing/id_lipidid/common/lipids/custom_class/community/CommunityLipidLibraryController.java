package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library.LocalLipidClassLibrary;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Browses the community lipid library and imports classes into the local library.
 */
public class CommunityLipidLibraryController extends FxController<CommunityLipidLibraryModel> {

  private static final Logger logger = Logger.getLogger(
      CommunityLipidLibraryController.class.getName());
  private final CommunityLipidLibraryViewBuilder viewBuilder;
  private final Consumer<List<CustomLipidClass>> onImported;

  /**
   * @param onImported called on the GUI thread with the classes that were added to the library
   */
  public CommunityLipidLibraryController(@NotNull Consumer<List<CustomLipidClass>> onImported) {
    super(new CommunityLipidLibraryModel());
    this.onImported = onImported;
    viewBuilder = new CommunityLipidLibraryViewBuilder(model);
    model.reloadActionProperty().set(this::reload);
    model.importActionProperty().set(this::importSelected);
  }

  @Override
  protected @NotNull FxViewBuilder<CommunityLipidLibraryModel> getViewBuilder() {
    return viewBuilder;
  }

  /**
   * Downloads the catalog unless it was loaded or is loading already, e.g., when the community tab
   * is shown.
   */
  public void reloadIfNeeded() {
    if (!model.loadedProperty().get() && !model.loadingProperty().get()) {
      reload();
    }
  }

  /**
   * Downloads the catalog of the community library.
   */
  public void reload() {
    model.loadingProperty().set(true);
    model.statusProperty().set("Loading the community library…");
    onTaskThread(new FxUpdateTask<>("Load community lipid library", model) {
      private List<LipidLibraryEntry> entries = List.of();
      private String error = null;

      @Override
      protected void process() {
        try {
          entries = LipidLibraryClient.fetchCatalog().classes();
        } catch (Exception e) {
          logger.log(Level.WARNING, "Cannot load the community lipid library", e);
          error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
      }

      @Override
      protected void updateGuiModel() {
        model.loadingProperty().set(false);
        if (error != null) {
          model.statusProperty().set(
              "Cannot load the community library. Check your internet connection or try again later. ("
                  + error + ")");
          return;
        }
        model.getEntries().setAll(entries);
        model.loadedProperty().set(true);
        model.statusProperty().set(entries.size() + " lipid classes in the library");
      }

      @Override
      public String getTaskDescription() {
        return "Loading the community lipid library";
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }
    });
  }

  /**
   * Downloads the selected classes and saves them to the local library.
   */
  private void importSelected() {
    final List<LipidLibraryEntry> selected = List.copyOf(model.getSelected());
    if (selected.isEmpty()) {
      return;
    }
    if (selected.stream().anyMatch(LipidLibraryEntry::requiresNewerMzmine)
        && !DialogLoggerUtil.showDialogYesNo("Newer mzmine needed",
        "Some classes use rules that need a newer mzmine version and may not load correctly. Import anyway?")) {
      return;
    }
    model.loadingProperty().set(true);
    model.statusProperty().set("Downloading %d lipid classes…".formatted(selected.size()));
    onTaskThread(new FxUpdateTask<>("Import community lipid classes", model) {
      private final List<CustomLipidClass> classes = new ArrayList<>();
      private final List<String> failed = new ArrayList<>();

      @Override
      protected void process() {
        for (final LipidLibraryEntry entry : selected) {
          try {
            classes.addAll(LipidLibraryClient.fetchClasses(entry));
          } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot download lipid class " + entry.id(), e);
            failed.add(entry.abbr() + " " + entry.name());
          }
        }
      }

      @Override
      protected void updateGuiModel() {
        model.loadingProperty().set(false);
        final LocalLipidClassLibrary library = LocalLipidClassLibrary.getInstance();
        classes.forEach(library::save);
        model.statusProperty().set(
            "Added %d lipid classes to your library.".formatted(classes.size()) + (failed.isEmpty()
                ? "" : " Failed: " + String.join(", ", failed)));
        if (!classes.isEmpty()) {
          onImported.accept(classes);
        }
      }

      @Override
      public String getTaskDescription() {
        return "Downloading community lipid classes";
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }
    });
  }
}
