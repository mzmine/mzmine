/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MirrorScanWindowFXML extends Stage {

  private MirrorScanWindowController controller;

  public MirrorScanWindowFXML() {
    super();
    setTitle("Spectral mirror plots");

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("MirrorScanWindow.fxml"));
    try {
      Pane mainPane = loader.load();
      controller = loader.getController();
      this.setScene(new Scene(mainPane));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public MirrorScanWindowController getController() {
    return controller;
  }
}
