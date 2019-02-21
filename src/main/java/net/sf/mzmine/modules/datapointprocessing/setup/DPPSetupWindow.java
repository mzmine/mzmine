/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

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

public class DPPSetupWindow {

  private static Logger logger = Logger.getLogger(DPPSetupWindow.class.getName());
  private static final String MODULE_NAME = "Data point processing setup Window";

  private FXMLLoader loader;
  private DPPSetupWindowController controller;
  private Parent root;
  private Scene scene;

  private final JFXPanel fxPanel;
  private final JFrame frame;

  private static final DPPSetupWindow inst = new DPPSetupWindow();

  public DPPSetupWindow() {

    frame = new JFrame("Data point processing method selection");
    frame.setSize(600, 400);
    frame.setLocationRelativeTo(null);
    
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

    frame.add(fxPanel);
    
    fxPanel.setVisible(true);

    // add menu item manually, else we'd need a module just to add this item which would be
    // unnecessary. Additionally we dont have a parameter set class.
    addMenuItem();

    logger.finest("DPPSetupWindow intialized.");
  }
  
  public void show() {
    fxPanel.setVisible(true);
    frame.setVisible(true);
  }

  public void hide() {
    fxPanel.setVisible(true);
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

  public JFrame getFrame() {
    return frame;
  }

}
