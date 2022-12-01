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

package io.github.mzmine.util.swing;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.w3c.dom.DOMImplementation;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.parser.UnsupportedFormatException;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

/**
 * Export swing components to pdf, emf, eps
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SwingExportUtil {
  private static final Logger logger = Logger.getLogger(SwingExportUtil.class.getName());

  /**
   * 
   * @param panel
   * @param path
   * @param fileName
   * @param format without . (ALL, PDF, EMF, EPS, SVG). ALL = export all at once
   */
  public static void writeToGraphics(JComponent panel, File file, String format)
      throws IOException, DocumentException, UnsupportedFormatException {
    writeToGraphics(panel, file.getParentFile(), file.getName(), format);
  }

  public static void writeToGraphics(JComponent panel, File path, String fileName, String format)
      throws IOException, DocumentException, UnsupportedFormatException {
    switch (format.toLowerCase()) {
      case "all":
        writeToPDF(panel, path, fileName);
        writeToEMF(panel, path, fileName);
        writeToEPS(panel, path, fileName);
        writeToSVG(panel, path, fileName);
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
      case "svg":
        writeToSVG(panel, path, fileName);
        break;
      default:
        throw new UnsupportedFormatException("Format is not supported for image export: " + format);
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
    logger.info(
        () -> MessageFormat.format("Exporting panel to PDF file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));
    Document document = new Document(new Rectangle(width, height));
    PdfWriter writer = null;
    try {
      writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
      document.open();
      PdfContentByte contentByte = writer.getDirectContent();
      PdfTemplate template = contentByte.createTemplate(width, height);
      Graphics2D g2 = new PdfGraphics2D(contentByte, width, height, new DefaultFontMapper());
      panel.print(g2);
      g2.dispose();
      contentByte.addTemplate(template, 0, 0);
      document.close();
      writer.close();
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
    logger.info(
        () -> MessageFormat.format("Exporting panel to EPS file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));
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
    logger.info(
        () -> MessageFormat.format("Exporting panel to EMF file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));

    VectorGraphics g = new EMFGraphics2D(fileName, new Dimension(width, height));
    g.startExport();
    panel.print(g);
    g.endExport();
  }

  public static void writeToSVG(JComponent panel, File fileName) throws IOException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getWidth();
    logger.info(
        () -> MessageFormat.format("Exporting panel to SVG file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));

    // Get a DOMImplementation
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    svgGenerator.setSVGCanvasSize(new Dimension(width, height));
    panel.print(svgGenerator);

    boolean useCSS = true; // we want to use CSS style attribute

    try (Writer out = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8")) {
      svgGenerator.stream(out, useCSS);
    }
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

  /**
   * Writes swing to SVG (scalable vector graphics)
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws IOException
   */
  public static void writeToSVG(JComponent panel, File path, String fileName) throws IOException {
    writeToSVG(panel, FileAndPathUtil.getRealFilePath(path, fileName, "svg"));
  }
}
