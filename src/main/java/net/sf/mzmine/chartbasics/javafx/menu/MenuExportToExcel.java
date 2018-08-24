/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.chartbasics.javafx.menu;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.sf.mzmine.chartbasics.javafx.charts.EChartViewer;
import net.sf.mzmine.chartbasics.menu.MenuExport;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.io.XSSFExcelWriterReader;


public class MenuExportToExcel extends MenuItem implements MenuExport {
  private static final long serialVersionUID = 1L;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private FileChooser fc;

  private XSSFExcelWriterReader excelWriter;
  private EChartViewer chart;

  public MenuExportToExcel(XSSFExcelWriterReader excelWriter, String menuTitle,
      EChartViewer chart) {
    super(menuTitle);
    this.excelWriter = excelWriter;
    this.chart = chart;
    setOnAction(e -> {
      if (fc == null) {
        fc = new FileChooser();
        fc.getExtensionFilters().add(new ExtensionFilter("Microsoft Excel table", "*.xlsx"));
      }
      File f = fc.showSaveDialog(null);
      if (f != null) {
        exportDataToExcel(f);
      }
    });
  }

  public void exportDataToExcel(File f) {
    try {
      logger.info("retrieving data for export to excel");
      Object[][] data = chart.getDataArrayForExport();
      if (data != null) {
        f = FileAndPathUtil.getRealFilePath(f, "xlsx");
        logger.info("Exporting data to excel file: " + f.getAbsolutePath());
        XSSFWorkbook wb = excelWriter.exportDataArrayToFile(f, "xydata", data, false);
        excelWriter.closeWorkbook(wb);
      }
    } catch (InvalidFormatException | IOException e1) {
      logger.log(Level.WARNING, "Cannot export to excel", e1);
    }
  }
}
