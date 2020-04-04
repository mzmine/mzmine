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

package io.github.mzmine.util;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.xmlgraphics.java2d.ps.EPSDocumentGraphics2D;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.JFreeChart;
import io.github.mzmine.main.MZmineCore;

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
