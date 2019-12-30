/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.mzhistogram.chart;

import javax.swing.SwingUtilities;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;;

public class HistogramDialog extends Stage {

  private final Scene mainScene;
  private final BorderPane mainPane;

  protected HistogramPanel histo;

  /**
   * Create the dialog. Bin width is automatically chosen
   */
  public HistogramDialog(String title, String xLabel, HistogramData data) {
    this(title, xLabel, data, 0);
  }

  /**
   *
   * @param title
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public HistogramDialog(String title, String xLabel, HistogramData data, double binWidth) {
    // this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setTitle(title);

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    // setBounds(100, 100, 1000, 800);
    // getContentPane().setLayout(new BorderLayout());
    histo = new HistogramPanel(xLabel, data, binWidth);

    SwingNode swingNode = new SwingNode();
    mainPane.setCenter(swingNode);
    SwingUtilities.invokeLater(() -> {
      swingNode.setContent(histo);
    });

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);
  }

  public HistogramPanel getHistoPanel() {
    return histo;
  }
}
