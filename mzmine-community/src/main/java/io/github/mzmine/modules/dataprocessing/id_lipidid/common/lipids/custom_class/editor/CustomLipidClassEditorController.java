package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import javafx.util.Duration;
import javafx.collections.ListChangeListener;
import javafx.animation.PauseTransition;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorInteractor.TestOutcome;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.CustomLipidClassEditorEvent.TestSpectrum;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassJson;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community.CommunityLipidLibraryController;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community.LipidLibraryClient;
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
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library.LocalLipidClassLibrary;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.InvalidationListener;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.jetbrains.annotations.NotNull;

/**
 * Editor to build, save, import, export and share custom lipid classes. Saved classes go to the
 * local lipid class library and can be added to the lipid annotation module.
 */
public class CustomLipidClassEditorController extends FxController<CustomLipidClassEditorModel> {

  private static final Logger logger = Logger.getLogger(
      CustomLipidClassEditorController.class.getName());

  private final CustomLipidClassEditorInteractor interactor;
  private final CustomLipidClassEditorViewBuilder viewBuilder;
  private final CommunityLipidLibraryController communityController;
  private final PauseTransition simulationDelay = new PauseTransition(Duration.millis(250));

  private static class Holder {

    private static final CustomLipidClassEditorController INSTANCE = new CustomLipidClassEditorController();
  }

  private CustomLipidClassEditorController() {
    super(new CustomLipidClassEditorModel(LocalLipidClassLibrary.getInstance().getPresets()));
    interactor = new CustomLipidClassEditorInteractor(model, LocalLipidClassLibrary.getInstance());
    communityController = new CommunityLipidLibraryController(
        imported -> model.getLibraryPresets().stream()
            .filter(p -> imported.stream().anyMatch(c -> c.getAbbr().equals(p.lipidClass().getAbbr())
                && c.getName().equals(p.lipidClass().getName()))).findFirst()
            .ifPresent(p -> model.selectedPresetProperty().set(p)));
    viewBuilder = new CustomLipidClassEditorViewBuilder(model, communityController::buildView,
        communityController::reloadIfNeeded);
    model.setEventHandler(this::handleEvent);

    // validate on every change of the class in the editor
    final InvalidationListener validate = _ -> {
      if (model.editingProperty().get()) {
        interactor.updateModel();
      }
    };
    model.nameProperty().addListener(validate);
    model.abbrProperty().addListener(validate);
    model.categoryProperty().addListener(validate);
    model.mainClassProperty().addListener(validate);
    model.backboneFormulaProperty().addListener(validate);
    model.getChains().addListener(validate);
    model.getRules().addListener(validate);

    // m/z of each rule for the example lipid, debounced while typing
    simulationDelay.setOnFinished(_ -> interactor.simulate());
    final InvalidationListener simulate = _ -> {
      if (model.editingProperty().get()) {
        simulationDelay.playFromStart();
      }
    };
    model.abbrProperty().addListener(simulate);
    model.backboneFormulaProperty().addListener(simulate);
    model.getChains().addListener(simulate);
    model.getRules().addListener(simulate);
    model.exampleProperty().addListener(simulate);
    model.editingProperty().addListener(simulate);

    // keep the rule form on an existing rule
    model.getRules().addListener((ListChangeListener<LipidRuleRow>) _ -> {
      final LipidRuleRow selected = model.selectedRuleProperty().get();
      if (selected != null && !model.getRules().contains(selected)) {
        model.selectedRuleProperty().set(model.getRules().isEmpty() ? null
            : model.getRules().getFirst());
      }
    });
  }

  /**
   * @return the editor shared by the editor tab and other parts of mzmine
   */
  public static @NotNull CustomLipidClassEditorController getInstance() {
    return Holder.INSTANCE;
  }

  @Override
  protected @NotNull FxViewBuilder<CustomLipidClassEditorModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  protected FxInteractor<CustomLipidClassEditorModel> getInteractor() {
    return interactor;
  }

  /**
   * Opens a class in the editor as a new unsaved class, e.g., a class from a module parameter.
   *
   * @param lipidClass the class to edit
   */
  public void editAsNew(@NotNull ILipidClass lipidClass) {
    onGuiThread(() -> {
      if (confirmDiscardChanges()) {
        interactor.load(lipidClass, false);
      }
    });
  }

  private void handleEvent(@NotNull CustomLipidClassEditorEvent event) {
    switch (event) {
      case NewClass _ -> {
        if (confirmDiscardChanges()) {
          model.selectedPresetProperty().set(null);
          interactor.newClass();
        }
      }
      case FromTemplate(ILipidClass template) -> {
        if (confirmDiscardChanges()) {
          model.selectedPresetProperty().set(null);
          interactor.load(template, false);
        }
      }
      case Open(CustomLipidClass lipidClass) -> {
        if (isOpen(lipidClass)) {
          return;
        }
        if (confirmDiscardChanges()) {
          interactor.load(lipidClass, true);
        } else {
          // keep the list selection on the class in the editor
          selectOriginalInList();
        }
      }
      case Duplicate _ -> interactor.duplicate();
      case Save _ -> save();
      case Revert _ -> interactor.revert();
      case Delete(List<CustomLipidClass> classes) -> delete(classes);
      case Import(var file) -> {
        try {
          final List<CustomLipidClass> classes = CustomLipidClassJson.read(file);
          final long existing = interactor.countExisting(classes);
          if (existing > 0 && !DialogLoggerUtil.showDialogYesNo("Replace lipid classes?",
              "%d of %d classes in the file already exist in your library. Replace them?".formatted(
                  existing, classes.size()))) {
            return;
          }
          interactor.addToLibrary(classes);
        } catch (IOException | RuntimeException e) {
          logger.log(Level.WARNING, "Cannot import lipid classes from " + file, e);
          DialogLoggerUtil.showErrorDialog("Import failed",
              "Cannot read lipid classes from %s:\n%s".formatted(file, e.getMessage()));
        }
      }
      case Export(var file, var classes) -> {
        try {
          interactor.exportFile(file, classes);
        } catch (IOException e) {
          logger.log(Level.WARNING, "Cannot export lipid classes to " + file, e);
          DialogLoggerUtil.showErrorDialog("Export failed", e.getMessage());
        }
      }
      case Share _ -> share();
      case TestSpectrum _ -> testSpectrum();
    }
  }

  private void testSpectrum() {
    final CustomLipidClass lipidClass = model.validClassProperty().get();
    final var ionization = model.testIonizationProperty().get();
    final String precursor = model.testPrecursorMzProperty().get();
    final String peaks = model.testPeaksProperty().get();
    final double ms1 = model.testMs1PpmProperty().get();
    final double ms2 = model.testMs2PpmProperty().get();
    model.testStatusProperty().set("Testing…");
    onTaskThread(new FxUpdateTask<>("Test lipid class with spectrum", model) {
      private TestOutcome outcome;

      @Override
      protected void process() {
        try {
          outcome = interactor.testSpectrum(lipidClass, ionization, precursor, peaks, ms1, ms2);
        } catch (RuntimeException e) {
          logger.log(Level.WARNING, "Cannot test lipid class with spectrum", e);
          outcome = TestOutcome.error("The test failed: " + e.getMessage());
        }
      }

      @Override
      protected void updateGuiModel() {
        model.getTestResults().setAll(outcome.results());
        model.testStatusProperty().set(outcome.message());
      }

      @Override
      public String getTaskDescription() {
        return "Testing lipid class with spectrum";
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }
    });
  }

  private boolean isOpen(@NotNull CustomLipidClass lipidClass) {
    final CustomLipidClass original = model.originalProperty().get();
    return original != null && original.getAbbr().equals(lipidClass.getAbbr())
        && original.getName().equals(lipidClass.getName());
  }

  private void selectOriginalInList() {
    final CustomLipidClass original = model.originalProperty().get();
    model.selectedPresetProperty().set(original == null ? null
        : model.getLibraryPresets().stream().filter(p -> isOpen(p.lipidClass())).findFirst()
            .orElse(null));
  }

  /**
   * @return true if there are no unsaved changes or the user agrees to discard them
   */
  private boolean confirmDiscardChanges() {
    if (!model.editingProperty().get() || !model.modifiedProperty().get()) {
      return true;
    }
    return DialogLoggerUtil.showDialogYesNo("Discard changes?",
        "The lipid class %s has unsaved changes. Discard them?".formatted(
            model.abbrProperty().get()));
  }

  private void save() {
    if (model.validClassProperty().get() == null) {
      DialogLoggerUtil.showWarningDialog("Cannot save lipid class",
          "Fix these issues first:\n" + String.join("\n", model.getIssues()));
      return;
    }
    if (interactor.wouldOverwriteOtherClass() && !DialogLoggerUtil.showDialogYesNo(
        "Replace lipid class?",
        "Your library already has a class %s %s. Replace it?".formatted(model.abbrProperty().get(),
            model.nameProperty().get()))) {
      return;
    }
    interactor.save();
    selectOriginalInList();
  }

  private void delete(@NotNull List<CustomLipidClass> classes) {
    if (classes.isEmpty()) {
      return;
    }
    final String names = classes.size() == 1 ? classes.getFirst().getAbbr() + " "
        + classes.getFirst().getName() : classes.size() + " lipid classes";
    if (!DialogLoggerUtil.showDialogYesNo("Delete lipid classes?",
        "Delete %s from your library? This cannot be undone.".formatted(names))) {
      return;
    }
    classes.forEach(interactor::delete);
  }

  /**
   * Copies the class JSON to the clipboard and opens the submission form of the community library.
   */
  private void share() {
    final CustomLipidClass lipidClass = model.validClassProperty().get();
    if (lipidClass == null) {
      DialogLoggerUtil.showWarningDialog("Cannot share lipid class",
          "Fix these issues first:\n" + String.join("\n", model.getIssues()));
      return;
    }
    final boolean accepted = DialogLoggerUtil.showDialogYesNo(AlertType.CONFIRMATION,
        "Share with the community", """
            Share %s %s in the mzmine custom lipid library?

            1. The class is copied to your clipboard.
            2. The submission form opens in your browser (requires a free GitHub account).
            3. Paste the class into the form, describe it and submit.

            The class is checked automatically and reviewed before it is published. By submitting, \
            you grant mzio GmbH exclusive rights to the class. You keep the right to use it for \
            your own research and are named as its author. Details: %s""".formatted(
            lipidClass.getAbbr(), lipidClass.getName(),
            LipidLibraryClient.CONTRIBUTOR_AGREEMENT_URL));
    if (!accepted) {
      return;
    }
    final ClipboardContent content = new ClipboardContent();
    content.putString(CustomLipidClassJson.toJson(List.of(lipidClass)));
    Clipboard.getSystemClipboard().setContent(content);
    MZmineCore.getDesktop().openWebPage(LipidLibraryClient.submissionUrl(lipidClass));
  }
}
