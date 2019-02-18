package net.sf.mzmine.modules.datapointprocessing.setup;

import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DPPSetupWindow {

  private static Logger logger = Logger.getLogger(DPPSetupWindow.class.getName());

  private FXMLLoader loader;
  private DPPSetupWindowController controller;
  private Parent root;
  private Scene scene;

  private final JFXPanel fxPanel;
  private final JFrame frame;

  private static final DPPSetupWindow inst = new DPPSetupWindow();

  DPPSetupWindow() {
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
    
    frame.setSize(800,  600);
    frame.add(fxPanel);
    fxPanel.setVisible(true);
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
}
