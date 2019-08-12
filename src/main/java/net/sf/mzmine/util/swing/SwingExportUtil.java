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

package net.sf.mzmine.util.swing;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JComponent;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Export swing components to pdf, emf, eps
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SwingExportUtil {

  /**
   * 
   * @param panel
   * @param path
   * @param fileName
   * @param format without . (ALL, PDF, EMF, EPS). ALL = export all at once
   */
  public static void writeToGraphics(JComponent panel, File file, String format)
      throws IOException, DocumentException {
    writeToGraphics(panel, file.getParentFile(), file.getName(), format);
  }

  public static void writeToGraphics(JComponent panel, File path, String fileName, String format)
      throws IOException, DocumentException {
    switch (format.toLowerCase()) {
      case "all":
        writeToPDF(panel, path, fileName);
        writeToEMF(panel, path, fileName);
        writeToEPS(panel, path, fileName);
        break;
      case "pdf":
        writeToPDF(panel, path, fileName);
        break;
      case "emf":
        writeToEMF(panel, path, fileName);
        writeToEPS(panel, path, fileName);
        break;
      case "eps":
        writeToEPS(panel, path, fileName);
        break;
    }
  }

  /**
   * Writes swing to pdf
   * 
   * @param panel
   * @param fileName
   * @throws DocumentException
   * @throws Exception
   */
  public static void writeToPDF(JComponent panel, File fileName)
      throws IOException, DocumentException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getHeight();
    Document document = new Document(new Rectangle(width, height));
    try {
      PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
      document.open();
      PdfContentByte contentByte = writer.getDirectContent();
      PdfTemplate template = contentByte.createTemplate(width, height);
      Graphics2D g2 = template.createGraphics(width, height, new DefaultFontMapper());
      panel.print(g2);
      g2.dispose();
      contentByte.addTemplate(template, 0, 0);
    } finally {
      if (document.isOpen()) {
        document.close();
      }
    }
  }

  /**
   * Writes swing to EPS
   * 
   * @param panel
   * @param fileName
   * @throws Exception
   */
  public static void writeToEPS(JComponent panel, File fileName) throws IOException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getWidth();
    EpsGraphics g;
    g = new EpsGraphics("EpsTools Drawable Export", new FileOutputStream(fileName), 0, 0, width,
        height, ColorMode.COLOR_RGB);
    panel.print(g);
    g.close();
  }

  /**
   * Writes swing to EMF
   * 
   * @param panel
   * @param fileName
   * @throws Exception
   */
  public static void writeToEMF(JComponent panel, File fileName) throws IOException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getWidth();

    VectorGraphics g = new EMFGraphics2D(fileName, new Dimension(width, height));
    g.startExport();
    panel.print(g);
    g.endExport();
  }

  /**
   * Writes swing to pdf
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws Exception
   */
  public static void writeToPDF(JComponent panel, File path, String fileName)
      throws IOException, DocumentException {
    writeToPDF(panel, FileAndPathUtil.getRealFilePath(path, fileName, "pdf"));
  }

  /**
   * Writes swing to EPS
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws Exception
   */
  public static void writeToEPS(JComponent panel, File path, String fileName) throws IOException {
    writeToEPS(panel, FileAndPathUtil.getRealFilePath(path, fileName, "eps"));
  }

  /**
   * Writes swing to EMF
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws Exception
   */
  public static void writeToEMF(JComponent panel, File path, String fileName) throws IOException {
    writeToEMF(panel, FileAndPathUtil.getRealFilePath(path, fileName, "emf"));
  }
}
