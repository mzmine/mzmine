package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;

/**
 * Main window tab of the custom lipid class editor. Only one instance exists.
 */
public class CustomLipidClassEditorTab extends SimpleTab {

  public static final String HEADER = "Custom lipid classes";

  private static class Holder {

    private static final CustomLipidClassEditorTab INSTANCE = new CustomLipidClassEditorTab();
  }

  private CustomLipidClassEditorTab() {
    super(HEADER);
    setContent(CustomLipidClassEditorController.getInstance().buildView());
  }

  /**
   * Shows the editor tab and adds it to the main window if needed.
   *
   * @return the tab
   */
  public static CustomLipidClassEditorTab showTab() {
    final CustomLipidClassEditorTab instance = Holder.INSTANCE;
    FxThread.runLater(() -> {
      final var tabPane = instance.getTabPane();
      if (tabPane != null) {
        tabPane.getSelectionModel().select(instance);
      } else {
        MZmineCore.getDesktop().addTab(instance);
      }
    });
    return instance;
  }
}
