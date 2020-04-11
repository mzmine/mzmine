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

package io.github.mzmine.gui.chartbasics.gui.javafx.menu;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.swing.menu.MenuExport;
import io.github.mzmine.util.io.ClipboardWriter;
import javafx.scene.control.MenuItem;

public class MenuExportToClipboard extends MenuItem implements MenuExport {

  private EChartViewer chart;

  public MenuExportToClipboard(String menuTitle, EChartViewer chart) {
    super(menuTitle);
    this.chart = chart;
    setOnAction(e -> exportDataToClipboard());
  }

  public void exportDataToClipboard() {
    Object[][] model = chart.getDataArrayForExport();
    if (model != null)
      ClipboardWriter.writeToClipBoard(model, false);
  }
}
