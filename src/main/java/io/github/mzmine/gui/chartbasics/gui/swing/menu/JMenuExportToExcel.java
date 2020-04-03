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
