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

package io.github.mzmine.gui.framework.strokechooser;

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
