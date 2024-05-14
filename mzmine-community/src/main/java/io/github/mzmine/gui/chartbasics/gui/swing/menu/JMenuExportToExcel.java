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

package io.github.mzmine.gui.chartbasics.gui.swing.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.files.FileTypeFilter;
import io.github.mzmine.util.io.XSSFExcelWriterReader;

public class JMenuExportToExcel extends JMenuItem implements MenuExport {
  private static final long serialVersionUID = 1L;

  private XSSFExcelWriterReader excelWriter;
  private EChartPanel chart;

  public JMenuExportToExcel(XSSFExcelWriterReader excelWriter, String menuTitle,
      EChartPanel chart) {
    super(menuTitle);
    this.excelWriter = excelWriter;
    this.chart = chart;
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser c = new JFileChooser();
        c.addChoosableFileFilter(new FileTypeFilter("xlsx", "Excel workbook"));
        if (c.showSaveDialog(chart) == JFileChooser.APPROVE_OPTION) {
          exportDataToExcel(c.getSelectedFile());
        }
      }
    });
  }

  public void exportDataToExcel(File f) {
    try {
      Object[][] data = chart.getDataArrayForExport();
      if (data != null) {
        f = FileAndPathUtil.getRealFilePath(f, "xlsx");
        XSSFWorkbook wb = excelWriter.exportDataArrayToFile(f, "xydata", data, false);
        excelWriter.closeWorkbook(wb);
      }
    } catch (InvalidFormatException | IOException e1) {
      e1.printStackTrace();
    }
  }
}
