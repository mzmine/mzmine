/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ResultTable extends JTable {
  private Hashtable<Long, Image> images;

  public ResultTable() {
    super();
    images = new Hashtable<>(10);
  }


  public void generateIconImage(SiriusCompound compound) {
    Object preview = compound.getPreview();
    if (preview == null)
      return;

    long hash = compound.getPreview().hashCode();
    Image image = compound.generateStructureImage(600, 400);
    if (image != null)
      images.put(hash, image);
  }

//  @Override
//  public String getToolTipText(MouseEvent e) {
//    Point p = e.getPoint();
//    int row = this.rowAtPoint(p);
//    int col = this.columnAtPoint(p);
//
//    JFrame frame = new JFrame();
//    frame.setIconImage(image);
//
//    System.out.println("Hello");
//    return "Sick";
//  }

  @Override
  public Component prepareRenderer(TableCellRenderer renderer,
      int row, int col) {

    Object icon = this.getValueAt(row, 5);
    Component c = super.prepareRenderer(renderer, row, col);

    try {
      Image image = images.get(icon.hashCode());

      if (c instanceof JComponent) {
        JComponent jc = (JComponent) c;
//      String name = getValueAt(row, 0).toString();
        String html = getHtml();
        jc.setToolTipText(html);
      }
    } catch (NullPointerException e) {

    }
    return c;
  }

  private String getHtml() {
    StringBuilder b = new StringBuilder();
    b.append("<html>");
    b.append("<body>");
    b.append("<img src='http://www.topapps.net/wp-content/uploads/2014/03/Google-URL-Shortener-app.png'>");
    b.append("</body>");
    b.append("</html>");
    return b.toString();
  }
}
