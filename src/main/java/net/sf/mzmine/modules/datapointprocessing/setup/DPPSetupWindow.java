package net.sf.mzmine.modules.datapointprocessing.setup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import net.sf.mzmine.desktop.impl.MainMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;

public class DPPSetupWindow  {

  private static Logger logger = Logger.getLogger(DPPSetupWindow.class.getName());

  private FXMLLoader loader;
  private DPPSetupWindowController controller;
  private Parent root;
  private Scene scene;

  private final JFXPanel fxPanel;
  private final JFrame frame;

  private static final DPPSetupWindow inst = new DPPSetupWindow();

  public DPPSetupWindow() {
    frame = new JFrame("Data point processing method selection");

    fxPanel = new JFXPanel();

    loader = new FXMLLoader(getClass().getResource("DPPSetupWindow.fxml"));
    setController(loader.getController());
    try {
      root = loader.load();
    } catch (IOException e) {
      logger.warning("Failed to initialize DPPSetupWindow.");
      e.printStackTrace();
      return;
    }
    scene = new Scene(root);
    fxPanel.setScene(scene);

    frame.setSize(800, 600);
    frame.add(fxPanel);
    fxPanel.setVisible(true);

    // add menu item manually, else we'd need a module just to add this item which would be
    // unnecessary.
    addMenuItem();

    logger.finest("DPPSetupWindow intialized.");
  }

  public void show() {
    frame.setVisible(true);
  }

  public void hide() {
    frame.setVisible(false);
  }

  public static DPPSetupWindow getInstance() {
    return inst;
  }

  public DPPSetupWindowController getController() {
    return controller;
  }

  private void setController(DPPSetupWindowController controller) {
    this.controller = controller;
  }

  /**
   * Adds a menu item to the main menu.
   */
  private void addMenuItem() {
    JMenuBar menu = MZmineCore.getDesktop().getMainWindow().getJMenuBar();
    if (menu instanceof MainMenu) {
      JMenuItem item = new JMenuItem("Spectra processing");
      item.setToolTipText("Set up instant spectra processing methods.");
      item.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          DPPSetupWindow.getInstance().show();
        }
      });
      ((MainMenu) menu).addMenuItem(MZmineModuleCategory.TOOLS, item);
    }
  }

}
