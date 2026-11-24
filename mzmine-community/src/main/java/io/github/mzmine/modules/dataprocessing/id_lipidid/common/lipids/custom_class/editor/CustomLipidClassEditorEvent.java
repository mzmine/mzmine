package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * User actions in the custom lipid class editor, sent by the view and handled by the controller.
 */
sealed interface CustomLipidClassEditorEvent {

  record NewClass() implements CustomLipidClassEditorEvent {

  }

  /**
   * @param template a built-in or saved class to start from
   */
  record FromTemplate(@NotNull ILipidClass template) implements CustomLipidClassEditorEvent {

  }

  /**
   * @param lipidClass a saved class to open
   */
  record Open(@NotNull CustomLipidClass lipidClass) implements CustomLipidClassEditorEvent {

  }

  record Duplicate() implements CustomLipidClassEditorEvent {

  }

  record Save() implements CustomLipidClassEditorEvent {

  }

  record Revert() implements CustomLipidClassEditorEvent {

  }

  /**
   * @param classes saved classes to delete
   */
  record Delete(@NotNull List<CustomLipidClass> classes) implements CustomLipidClassEditorEvent {

  }

  /**
   * @param file JSON file to import
   */
  record Import(@NotNull Path file) implements CustomLipidClassEditorEvent {

  }

  /**
   * @param file    target JSON file
   * @param classes classes to export
   */
  record Export(@NotNull Path file, @NotNull List<CustomLipidClass> classes) implements
      CustomLipidClassEditorEvent {

  }

  /**
   * Tests the class against the pasted spectrum.
   */
  record TestSpectrum() implements CustomLipidClassEditorEvent {

  }
}
