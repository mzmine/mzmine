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

package io.github.mzmine.gui.chartbasics.graphicsexport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import javax.swing.JMenuItem;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.util.Args;
import org.w3c.dom.DOMImplementation;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import io.github.mzmine.gui.chartbasics.ChartLogics;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters.FixedSize;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxColorUtil;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

/**
 * Graphics export of JFreeCharts to different vector and pixel graphics formats.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartExportUtil {
  // ######################################################################################
  // VECTORS: PDF uses ITextpdf lib

  /**
   * Add export dialog to popup menu of a chartpanel
   *
   * @param cp
   */
  public static void addExportDialogToMenu(final ChartPanel cp) {
    JMenuItem exportGraphics = new JMenuItem("Export graphics...");
    exportGraphics.addActionListener(e -> {
      
      GraphicsExportParameters parameters = (GraphicsExportParameters) MZmineCore
          .getConfiguration().getModuleParameters(GraphicsExportModule.class);
      
      MZmineCore.getModuleInstance(GraphicsExportModule.class)
          .openDialog(cp.getChart(), parameters);
    });
    // add to menu
    cp.getPopupMenu().add(exportGraphics);
  }

  /**
   * takes Only Width in account
   *
   * @param chart
   * @param sett
   * @throws Exception
   */
  public static void writeChartToImage(ChartPanel chart, GraphicsExportParameters sett)
      throws Exception {
    boolean repaint = false;
    FixedSize fixed = sett.getFixedSize();

    double oldW = sett.getWidthPixel();
    double oldH = sett.getHeightPixel();

    // Size only by width?
    if (sett.isUseOnlyWidth()) {
      // fixed size for chart or plot
      if (fixed.equals(FixedSize.Chart)) {
        sett.setHeightPixel(ChartLogics.calcHeightToWidth(chart, oldW, false));
      } else {
        // fixed plot width
        sett.setPixelSize(ChartLogics.calcSizeForPlotWidth(chart, oldW));
      }
    } else if (fixed.equals(FixedSize.Plot)) {
      // fixed plot size - width and height are given
      sett.setPixelSize(ChartLogics.calcSizeForPlotSize(chart, oldW, oldH));
    }

    Dimension size = sett.getPixelSize();
    // resize
    chart.setPreferredSize(size);
    chart.setMaximumSize(size);
    chart.setMinimumSize(size);
    // repaint
    if (repaint) {
      chart.revalidate();
      chart.repaint();
    }
    writeChartToImage(chart.getChart(), sett, chart.getChartRenderingInfo());
    // reset size
    sett.setPixelSize(oldW, oldH);
  }
  
  /**
   * takes Only Width in account
   *
   * @param chart
   * @param sett
   * @throws Exception
   */
  public static void writeChartToImageFX(EChartViewer chart, GraphicsExportParameters sett)
      throws Exception {
    boolean repaint = false;
    FixedSize fixed = sett.getFixedSize();

    double oldW = sett.getWidthPixel();
    double oldH = sett.getHeightPixel();

    // Size only by width?
    if (sett.isUseOnlyWidth()) {
      // fixed size for chart or plot
      if (fixed.equals(FixedSize.Chart)) {
        sett.setHeightPixel(ChartLogicsFX.calcHeightToWidth(chart, oldW/*, false*/));
      } else {
        // fixed plot width
        sett.setPixelSize(ChartLogicsFX.calcSizeForPlotWidth(chart, oldW));
      }
    } else if (fixed.equals(FixedSize.Plot)) {
      // fixed plot size - width and height are given
      sett.setPixelSize(ChartLogicsFX.calcSizeForPlotSize(chart, oldW, oldH));
    }

    Dimension size = sett.getPixelSize();
    // resize
    chart.setPrefSize(size.getWidth(), size.getHeight());
    chart.setMaxSize(size.getWidth(), size.getHeight());
    chart.setMinSize(size.getWidth(), size.getHeight());
    // repaint TODO:
//    if (repaint) {
//      chart.revalidate();
//      chart.repaint();
//    }
    writeChartToImage(chart.getChart(), sett, chart.getRenderingInfo());
    // reset size
    sett.setPixelSize(oldW, oldH);
  }

  /**
   * This method is used to save all image formats. it uses the specific methods for each file
   * format
   *
   * @param chart
   * @param sett
   * @param info
   */
  private static void writeChartToImage(JFreeChart chart, GraphicsExportParameters sett,
      ChartRenderingInfo info) throws Exception {
    // Background color
    Paint saved = chart.getBackgroundPaint();
//    Color awtColor = FxColorUtil.fxColorToAWT(sett.getColorWithAlpha());
//    chart.setBackgroundPaint(awtColor);
//    chart.setBackgroundImageAlpha((float) sett.getTransparency());
//    if (chart.getLegend() != null)
//      chart.getLegend().setBackgroundPaint(awtColor);
    // legends and stuff
//    for (int i = 0; i < chart.getSubtitleCount(); i++)
//      if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
//        ((PaintScaleLegend) chart.getSubtitle(i)).setBackgroundPaint(awtColor);

    // apply bg
//    chart.getPlot().setBackgroundPaint(awtColor);

    // create folder
    File f = sett.getFullpath();
    if (!f.exists()) {
      if (f.getParentFile() != null) {
        f.getParentFile().mkdirs();
      }
      // f.createNewFile();
    }

    Dimension size = sett.getPixelSize();
    // Format
    switch (sett.getFormat()) {
      case "PDF":
        writeChartToPDF(chart, size.width, size.height, f);
        break;
      case "PNG":
        writeChartToPNG(chart, info, size.width, size.height, f, (int) sett.getDPI());
        break;
      case "JPG":
        writeChartToJPEG(chart, info, size.width, size.height, f, (int) sett.getDPI());
        break;
      case "EPS":
        writeChartToEPS(chart, size.width, size.height, f);
        break;
      case "SVG":
        writeChartToSVG(chart, size.width, size.height, f);
        break;
      case "EMF":
        writeChartToEMF(chart, size.width, size.height, f);
        break;
    }
    //
    chart.setBackgroundPaint(saved);
    chart.setBackgroundImageAlpha(255);
    if (chart.getLegend() != null)
      chart.getLegend().setBackgroundPaint(saved);
    // legends and stuff
    for (int i = 0; i < chart.getSubtitleCount(); i++)
      if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
        ((PaintScaleLegend) chart.getSubtitle(i)).setBackgroundPaint(saved);

    // apply bg
    chart.getPlot().setBackgroundPaint(saved);
  }

  /**
   * This method saves a chart as a PDF with given dimensions
   *
   * @param chart
   * @param width
   * @param height
   * @param fileName is a full path
   */
  public static void writeChartToPDF(JFreeChart chart, int width, int height, File fileName)
      throws Exception {
    PdfWriter writer = null;

    Document document = new Document(new Rectangle(width, height));

    try {
      writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
      document.open();
      PdfContentByte contentByte = writer.getDirectContent();
      PdfTemplate template = contentByte.createTemplate(width, height);
      Graphics2D graphics2d = template.createGraphics(width, height, new DefaultFontMapper());
      Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);

      chart.draw(graphics2d, rectangle2d);

      graphics2d.dispose();
      contentByte.addTemplate(template, 0, 0);

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      document.close();
    }
  }

  public static void writeChartToPDF(JFreeChart chart, int width, int height, File path,
      String fileName) throws Exception {
    writeChartToPDF(chart, width, height, FileAndPathUtil.getRealFilePath(path, fileName, ".pdf"));
  }

  // ######################################################################################
  // PIXELS: JPG PNG
  public static void writeChartToPNG(JFreeChart chart, ChartRenderingInfo info, int width,
      int height, File fileName) throws IOException {
    ChartUtils.saveChartAsPNG(fileName, chart, width, height, info);
  }

  public static void writeChartToPNG(JFreeChart chart, ChartRenderingInfo info, int width,
      int height, File fileName, int resolution) throws IOException {
    if (resolution == 72)
      writeChartToPNG(chart, info, width, height, fileName);
    else {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
      try {
        BufferedImage image = paintScaledChartToBufferedImage(chart, info, out, width, height,
            resolution, BufferedImage.TYPE_INT_ARGB);
        out.write(ChartUtils.encodeAsPNG(image));
      } finally {
        out.close();
      }
    }
  }

  public static void writeChartToJPEG(JFreeChart chart, int width, int height, File fileName)
      throws IOException {
    ChartUtils.saveChartAsJPEG(fileName, chart, width, height);
  }

  public static void writeChartToJPEG(JFreeChart chart, ChartRenderingInfo info, int width,
      int height, File fileName, int resolution) throws IOException {
    // Background color
    Paint saved = chart.getBackgroundPaint();
    if (((Color) saved).getAlpha() == 0) {
      chart.setBackgroundPaint(Color.WHITE);
      chart.setBackgroundImageAlpha(255);
      if (chart.getLegend() != null)
        chart.getLegend().setBackgroundPaint(Color.WHITE);
      // legends and stuff
      for (int i = 0; i < chart.getSubtitleCount(); i++)
        if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
          ((PaintScaleLegend) chart.getSubtitle(i)).setBackgroundPaint(Color.WHITE);

      // apply bg
      chart.getPlot().setBackgroundPaint(Color.WHITE);
    }
    //
    if (resolution == 72)
      writeChartToJPEG(chart, width, height, fileName);
    else {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
      try {
        BufferedImage image = paintScaledChartToBufferedImage(chart, info, out, width, height,
            resolution, BufferedImage.TYPE_INT_RGB);
        EncoderUtil.writeBufferedImage(image, ImageFormat.JPEG, out, 1.f);
      } finally {
        out.close();
      }
    }
  }

  /**
   * Paints a chart with scaling options
   *
   * @param chart
   * @param info
   * @param out
   * @param width
   * @param height
   * @param resolution
   * @return BufferedImage of a given chart with scaling to resolution
   * @throws IOException
   */
  private static BufferedImage paintScaledChartToBufferedImage(JFreeChart chart,
      ChartRenderingInfo info, OutputStream out, int width, int height, int resolution,
      int bufferedIType) throws IOException {
    Args.nullNotPermitted(out, "out");
    Args.nullNotPermitted(chart, "chart");

    double scaleX = resolution / 72.0;
    double scaleY = resolution / 72.0;

    double desiredWidth = width * scaleX;
    double desiredHeight = height * scaleY;
    double defaultWidth = width;
    double defaultHeight = height;
    boolean scale = false;

    // get desired width and height from somewhere then...
    if ((scaleX != 1) || (scaleY != 1)) {
      scale = true;
    }

    BufferedImage image = new BufferedImage((int) desiredWidth, (int) desiredHeight, bufferedIType);
    Graphics2D g2 = image.createGraphics();

    if (scale) {
      AffineTransform saved = g2.getTransform();
      g2.transform(AffineTransform.getScaleInstance(scaleX, scaleY));
      chart.draw(g2, new Rectangle2D.Double(0, 0, defaultWidth, defaultHeight), info);
      g2.setTransform(saved);
      g2.dispose();
    } else {
      chart.draw(g2, new Rectangle2D.Double(0, 0, defaultWidth, defaultHeight), info);
    }
    return image;
  }

  // ######################################################################################
  // VECTORS: EPS uses EpsGraphics2D
  // SVG uses BATIK lib
  public static void writeChartToSVG(JFreeChart chart, int width, int height, File name)
      throws Exception {
    // Get a DOMImplementation
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    svgGenerator.setSVGCanvasSize(new Dimension(width, height));
    chart.draw(svgGenerator, new Rectangle2D.Double(0, 0, width, height));

    boolean useCSS = true; // we want to use CSS style attribute

    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(name), "UTF-8");
      svgGenerator.stream(out, useCSS);
    } catch (UnsupportedEncodingException | FileNotFoundException e) {
      e.printStackTrace();
      throw e;
    } catch (SVGGraphics2DIOException e) {
      e.printStackTrace();
      throw e;
    } finally {
      try {
        out.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        throw e;
      }
    }
  }

  public static void writeChartToEPS(JFreeChart chart, int width, int height, File name)
      throws IOException {
    EpsGraphics g;
    try {
      g = new EpsGraphics("EpsTools Drawable Export", new FileOutputStream(name), 0, 0, width,
          height, ColorMode.COLOR_RGB);
      Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);
      chart.draw(g, rectangle2d);
      g.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw e;
    }
  }

  public static void writeChartToEMF(JFreeChart chart, int width, int height, File name)
      throws IOException {
    try {
      VectorGraphics g = new EMFGraphics2D(name, new Dimension(width, height));
      g.startExport();
      Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);
      chart.draw(g, rectangle2d);
      g.endExport();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw e;
    }
  }

}
