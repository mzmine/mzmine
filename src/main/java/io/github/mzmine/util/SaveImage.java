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

package io.github.mzmine.util;

import io.github.mzmine.main.MZmineCore;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.xmlgraphics.java2d.ps.EPSDocumentGraphics2D;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.JFreeChart;

public class SaveImage implements Runnable {

  public enum FileType {
    EMF, EPS
  };

  private JFreeChart chart;
  private String file;
  private int width;
  private int height;
  private final FileType fileType;

  public SaveImage(JFreeChart c, String f, int w, int h, FileType type) {
    chart = c;
    file = f;
    width = w;
    height = h;
    fileType = type;
  }

  @Override
  public void run() {

    try {

      if (fileType.equals(FileType.EMF)) {
        OutputStream out2 = new java.io.FileOutputStream(file);
        EMFGraphics2D g2d2 = new EMFGraphics2D(out2, new Dimension(width, height));
        g2d2.startExport();
        chart.draw(g2d2, new Rectangle(width, height));
        g2d2.endExport();
        g2d2.closeStream();
        out2.flush();
        out2.close();
      }

      if (fileType.equals(FileType.EPS)) {
        OutputStream out = new java.io.FileOutputStream(file);
        EPSDocumentGraphics2D g2d = new EPSDocumentGraphics2D(false);
        g2d.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());
        g2d.setupDocument(out, width, height);
        chart.draw(g2d, new Rectangle(width, height));
        g2d.finish();
        out.flush();
        out.close();
      }
    } catch (IOException e) {
      MZmineCore.getDesktop().displayErrorMessage("Unable to save image.");
      e.printStackTrace();
    }
  }

}
