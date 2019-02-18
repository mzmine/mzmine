package net.sf.mzmine.modules.datapointprocessing.setup;

import java.io.IOException;
import java.util.logging.Logger;
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
  
  
  private static final DPPSetupWindow inst = new DPPSetupWindow();
  
  DPPSetupWindow(){
    loader = new FXMLLoader(getClass().getResource("DPPSetupWindow.fxml"));
    setController(loader.getController());
    try {
      root = loader.load();
    } catch (IOException e) {
      logger.warning("Failed to initialize DPPSetupWindow.");
      e.printStackTrace();
      return;
    }
    scene = new Scene(root, 800, 600);
    logger.finest("DPPSetupWindow intialized.");
  }
  
  public void show(Stage stage) {
    stage.setScene(scene);
    stage.show();
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
