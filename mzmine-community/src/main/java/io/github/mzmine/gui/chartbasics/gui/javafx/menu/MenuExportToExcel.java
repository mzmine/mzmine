/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.chartbasics.gui.javafx.menu;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.swing.menu.MenuExport;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.XSSFExcelWriterReader;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class MenuExportToExcel extends MenuItem implements MenuExport {

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
