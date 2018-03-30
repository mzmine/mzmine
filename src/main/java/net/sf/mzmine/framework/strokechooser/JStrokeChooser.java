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

package net.sf.mzmine.framework.strokechooser;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * opens a {@link JStrokeChooserDialog}
 * 
 * @author r_schm33
 *
 */
public class JStrokeChooser extends JPanel {
  private JButton button;

  private SettingsBasicStroke stroke;

  private ArrayList<ApplyListener> listener;


  public JStrokeChooser() {
    this(new SettingsBasicStroke(1.5f));
  }

  public JStrokeChooser(BasicStroke s) {
    this(new SettingsBasicStroke(s));
  }

  public JStrokeChooser(SettingsBasicStroke str) {
    super();
    this.stroke = str;

    setLayout(new BorderLayout(0, 0));

    button = new JButton() {
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        Dimension d = this.getSize();
        // g.setColor(this.getBackground());
        // g.fillRect(0, 0, d.width, d.height);

        g.setColor(Color.BLACK);
        Graphics2D g2 = (Graphics2D) g;

        if (stroke != null) {
          g2.setStroke(stroke.getStroke());
          float w = stroke.getLineWidth();
          g2.drawLine(2, (int) (d.height - w) / 2, d.width - 2, (int) (d.height - w) / 2);
        }
      }
    };
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // open dialog
        JStrokeChooserDialog d = new JStrokeChooserDialog(stroke);
        d.addApplyListener(new ApplyListener() {
          @Override
          public void apply(Object o) {
            if (o != null) {
              SettingsBasicStroke s = (SettingsBasicStroke) o;
              setStroke(s);
              button.repaint();
            }
          }
        });
        d.setVisible(true);
      }
    });
    add(button, BorderLayout.CENTER);
  }


  public void addStrokeListener(ApplyListener l) {
    if (listener == null)
      listener = new ArrayList<ApplyListener>(1);
    listener.add(l);
  }

  public void clearStrokeListener() {
    listener.clear();
  }

  public ArrayList<ApplyListener> getStrokeListener() {
    return listener;
  }

  public JButton getButton() {
    return button;
  }

  public SettingsBasicStroke getStroke() {
    return stroke;
  }

  public void setButtonActive(boolean b) {
    button.setEnabled(b);
  }

  public void setStroke(SettingsBasicStroke s) {
    stroke = s;
    fireStrokeChangedEvent();
  }

  private void fireStrokeChangedEvent() {
    if (listener != null) {
      for (ApplyListener l : listener)
        l.apply(stroke);
    }
  }

  public void setStroke(BasicStroke newValue) {
    if (stroke == null)
      stroke = new SettingsBasicStroke(newValue);
    else
      stroke.setStroke(newValue);
  }


}
