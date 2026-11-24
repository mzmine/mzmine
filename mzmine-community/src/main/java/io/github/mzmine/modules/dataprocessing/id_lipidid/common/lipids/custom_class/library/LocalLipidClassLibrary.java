package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import java.util.List;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * The custom lipid classes the user saved locally. They are available in the custom lipid class
 * editor and can be added to the lipid annotation module. Stored in the mzmine presets directory.
 */
public final class LocalLipidClassLibrary {

  private final CustomLipidClassPresetStore store = new CustomLipidClassPresetStore();
  private boolean loaded = false;

  private static class Holder {

    private static final LocalLipidClassLibrary INSTANCE = new LocalLipidClassLibrary();
  }

  private LocalLipidClassLibrary() {
  }

  /**
   * @return the singleton, loads the classes from disk on first access
   */
  public static @NotNull LocalLipidClassLibrary getInstance() {
    final LocalLipidClassLibrary instance = Holder.INSTANCE;
    instance.ensureLoaded();
    return instance;
  }

  private synchronized void ensureLoaded() {
    if (!loaded) {
      loaded = true;
      store.loadAllPresetsOrDefaults();
    }
  }

  /**
   * @return the live list of saved classes, changes on the JavaFX thread
   */
  public @NotNull ObservableList<CustomLipidClassPreset> getPresets() {
    return store.getCurrentPresets();
  }

  /**
   * @return a copy of all saved classes
   */
  public @NotNull List<CustomLipidClass> getClasses() {
    return List.copyOf(store.getCurrentPresets()).stream().map(CustomLipidClassPreset::lipidClass)
        .toList();
  }

  /**
   * Saves the class and replaces a saved class with the same abbreviation and name.
   *
   * @param lipidClass the class to save
   */
  public void save(@NotNull CustomLipidClass lipidClass) {
    store.addAndSavePreset(new CustomLipidClassPreset(lipidClass), true);
  }

  /**
   * Replaces a saved class, e.g., after renaming it in the editor.
   *
   * @param old     the class before editing, removed from the library
   * @param updated the class after editing
   */
  public void replace(@NotNull CustomLipidClass old, @NotNull CustomLipidClass updated) {
    final CustomLipidClassPreset oldPreset = new CustomLipidClassPreset(old);
    final CustomLipidClassPreset newPreset = new CustomLipidClassPreset(updated);
    if (!oldPreset.equalsIgnoreCaseName(newPreset)) {
      store.removePresetsWithName(oldPreset);
    }
    store.addAndSavePreset(newPreset, true);
  }

  /**
   * @param lipidClass the class to remove from the library and disk
   */
  public void delete(@NotNull CustomLipidClass lipidClass) {
    store.removePresetsWithName(new CustomLipidClassPreset(lipidClass));
  }

  /**
   * @param abbr abbreviation of a class
   * @param name name of a class
   * @return true if a class with the same abbreviation and name is saved
   */
  public boolean contains(@NotNull String abbr, @NotNull String name) {
    final CustomLipidClassPreset probe = new CustomLipidClassPreset(
        new CustomLipidClass(name, abbr, null, null, "", null, null));
    return store.getCurrentPresets().stream().anyMatch(p -> p.equalsIgnoreCaseName(probe));
  }
}
