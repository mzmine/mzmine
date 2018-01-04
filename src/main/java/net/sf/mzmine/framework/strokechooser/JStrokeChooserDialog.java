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
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;

public class JStrokeChooserDialog extends JDialog {

  private final JPanel contentPanel = new JPanel();
  private JPanel pnPreview;
  private JTextField txtWidth;
  private JTextField txtArray;
  private JTextField txtPhase;
  private JTextField txtMiterLimit;
  private JComboBox comboCap;
  private JComboBox comboJoin;

  private ArrayList<ApplyListener> listener;

  private Border errorBorder = BorderFactory.createLineBorder(Color.RED, 2);
  private Border stdBorder;

  private SettingsBasicStroke result;
  private JStrokeChooser strokeChooserPanel;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      JStrokeChooserDialog dialog = new JStrokeChooserDialog(new SettingsBasicStroke(1.5f));
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   */
  public JStrokeChooserDialog(SettingsBasicStroke stroke) {
    result = stroke;

    ItemListener itemListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        SettingsBasicStroke s = getStrokeChooserPanel().getStroke();
        // all values
        int join = getJoinSelection();
        int cap = getCapSelection();

        s.setJoin(join);
        s.setCap(cap);
        // create new
        getStrokeChooserPanel().setStroke(s);
        getStrokeChooserPanel().repaint();
      }
    };


    setBounds(100, 100, 450, 300);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));
    {
      JPanel panel = new JPanel();
      contentPanel.add(panel, BorderLayout.CENTER);
      panel.setLayout(new MigLayout("", "[][][][]", "[][][][][][][][grow]"));
      {
        strokeChooserPanel = new JStrokeChooser(result);
        strokeChooserPanel.getButton().setPreferredSize(new Dimension(33, 20));
        strokeChooserPanel.getButton().setMinimumSize(new Dimension(33, 20));
        strokeChooserPanel.setButtonActive(false);
        panel.add(strokeChooserPanel, "cell 0 0 4 1,grow");
      }
      {
        JLabel lblWidth = new JLabel("width");
        panel.add(lblWidth, "cell 0 1,alignx trailing");
      }
      {
        txtWidth = new JTextField();
        stdBorder = txtWidth.getBorder();
        txtWidth.getDocument().putProperty("owner", txtWidth);
        txtWidth.setText("1.5");
        txtWidth.setToolTipText("Line width as floating point number");
        panel.add(txtWidth, "cell 1 1,alignx left");
        txtWidth.setColumns(4);
      }
      {
        JButton btnDots = new JButton("dots");
        btnDots.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String w = getTxtWidth().getText();
            getTxtArray().setText(w + " " + w);
          }
        });
        panel.add(btnDots, "cell 2 1");
      }
      {
        JLabel lblDashArray = new JLabel("dash array");
        panel.add(lblDashArray, "cell 0 2,alignx trailing");
      }
      {
        txtArray = new JTextField();
        txtArray.getDocument().putProperty("owner", txtArray);
        txtArray
            .setToolTipText("An array of dash, space, dash, space separated by a space character");
        txtArray.setText("7 3.5 1.5 3.5");
        txtArray.setColumns(16);
        panel.add(txtArray, "cell 1 2,growx");
      }
      {
        JButton btnDash = new JButton("dash");
        btnDash.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            getTxtArray().setText("7 3.5");
          }
        });
        panel.add(btnDash, "cell 2 2");
      }
      {
        JButton btnDashdot = new JButton("dash-dot");
        btnDashdot.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            getTxtArray().setText("7 3.5 " + getTxtWidth().getText() + " 3.5");
          }
        });
        panel.add(btnDashdot, "cell 3 2");
      }
      {
        JLabel lblCap = new JLabel("cap");
        panel.add(lblCap, "cell 0 3,alignx trailing");
      }
      {
        comboCap = new JComboBox();
        comboCap.setModel(new DefaultComboBoxModel(new String[] {"Round", "Butt", "Square"}));
        panel.add(comboCap, "cell 1 3,growx");
        comboCap.addItemListener(itemListener);
      }
      {
        JLabel lblJoin = new JLabel("join");
        panel.add(lblJoin, "cell 0 4,alignx trailing");
      }
      {
        comboJoin = new JComboBox();
        comboJoin.setModel(new DefaultComboBoxModel(new String[] {"Round", "Miter", "Bevel"}));
        panel.add(comboJoin, "cell 1 4,growx");
        comboJoin.addItemListener(itemListener);
      }
      {
        JLabel lblPhase = new JLabel("phase");
        panel.add(lblPhase, "cell 0 5,alignx trailing");
      }
      {
        txtPhase = new JTextField();
        txtPhase.getDocument().putProperty("owner", txtPhase);
        txtPhase.setText("0");
        txtPhase.setToolTipText("The phase is an offset which shifts the pattern start");
        txtPhase.setColumns(4);
        panel.add(txtPhase, "cell 1 5,alignx left");
      }
      {
        JLabel lblMiterLimit = new JLabel("miter limit");
        panel.add(lblMiterLimit, "cell 0 6,alignx trailing");
      }
      {
        txtMiterLimit = new JTextField();
        txtMiterLimit.getDocument().putProperty("owner", txtMiterLimit);
        txtMiterLimit.setText("2");
        txtMiterLimit.setToolTipText("Limit of miter joins");
        panel.add(txtMiterLimit, "cell 1 6,alignx left");
        txtMiterLimit.setColumns(4);
      }
      {
        pnPreview = new JPanel() {
          @Override
          public void paint(Graphics g) {
            super.paint(g);
            Dimension d = getSize();
            Graphics2D g2 = (Graphics2D) g;

            int dist = 3;
            int x = dist;
            int y = dist;
            int w = d.width / 3 - x * 2;
            int h = d.height - y - 2;

            g2.setStroke(getStrokeChooserPanel().getStroke().getStroke());
            // draw rect
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, w, h);

            x += w + dist;
            // draw circle
            g2.setColor(Color.BLUE);
            g2.drawOval(x, y, w, h);
            x += w + dist;
            // draw star
            g2.setColor(Color.RED);
            double r = Math.min(w / 4.0, h / 4.0) - 5;
            g2.draw(createDefaultStar(r, x + r * 2 + 10, y + r * 2 + 10));

          }

          private Shape createDefaultStar(double radius, double centerX, double centerY) {
            return createStar(centerX, centerY, radius, radius * 2.63, 5, Math.toRadians(-18));
          }

          private Shape createStar(double centerX, double centerY, double innerRadius,
              double outerRadius, int numRays, double startAngleRad) {
            Path2D path = new Path2D.Double();
            double deltaAngleRad = Math.PI / numRays;
            for (int i = 0; i < numRays * 2; i++) {
              double angleRad = startAngleRad + i * deltaAngleRad;
              double ca = Math.cos(angleRad);
              double sa = Math.sin(angleRad);
              double relX = ca;
              double relY = sa;
              if ((i & 1) == 0) {
                relX *= outerRadius;
                relY *= outerRadius;
              } else {
                relX *= innerRadius;
                relY *= innerRadius;
              }
              if (i == 0) {
                path.moveTo(centerX + relX, centerY + relY);
              } else {
                path.lineTo(centerX + relX, centerY + relY);
              }
            }
            path.closePath();
            return path;
          }
        };
        pnPreview.setPreferredSize(new Dimension(10, 150));
        panel.add(pnPreview, "cell 0 7 4 1,grow");
      }
      pack();
    }
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            //
            settingsToStroke();
            if (listener != null)
              for (ApplyListener l : listener)
                l.apply(result);
            setVisible(false);
          }
        });
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
      {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
        });
        {
          JButton btnApply = new JButton("Apply");
          btnApply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              //
              settingsToStroke();
              if (listener != null)
                for (ApplyListener l : listener)
                  l.apply(result);
            }
          });
          buttonPane.add(btnApply);
        }
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
      }
    }

    // delayed document listener
    DelayedDocumentListener ddl = new DelayedDocumentListener() {
      @Override
      public void documentChanged(DocumentEvent e) {
        JTextField txt = (JTextField) e.getDocument().getProperty("owner");
        try {
          float f = Float.valueOf(txt.getText());

          SettingsBasicStroke s = getStrokeChooserPanel().getStroke();
          //
          if (txt.equals(getTxtWidth())) {
            s.setLineWidth(f);
          }
          if (txt.equals(getTxtMiterLimit())) {
            s.setMiterlimit(f);
          }
          if (txt.equals(getTxtPhase())) {
            s.setDashphase(f);
          }

          // create new
          getStrokeChooserPanel().setStroke(s);
          getStrokeChooserPanel().repaint();
          pnPreview.repaint();

          // reset to standard border
          txt.setBorder(stdBorder);
        } catch (Exception ex) {
          // error
          txt.setBorder(errorBorder);
        }
      }
    };

    getTxtMiterLimit().getDocument().addDocumentListener(ddl);
    getTxtPhase().getDocument().addDocumentListener(ddl);
    getTxtWidth().getDocument().addDocumentListener(ddl);

    DelayedDocumentListener ddl2 = new DelayedDocumentListener() {
      @Override
      public void documentChanged(DocumentEvent e) {
        JTextField txt = (JTextField) e.getDocument().getProperty("owner");
        try {
          String[] split = getTxtArray().getText().split(" ");
          float[] array = new float[split.length];
          for (int i = 0; i < split.length; i++)
            array[i] = Float.valueOf(split[i]);

          SettingsBasicStroke s = getStrokeChooserPanel().getStroke();
          // all values
          s.setDashArray(array);

          // create new
          getStrokeChooserPanel().setStroke(s);
          getStrokeChooserPanel().repaint();
          pnPreview.repaint();

          // reset to standard border
          txt.setBorder(stdBorder);
        } catch (Exception ex) {
          // error
          txt.setBorder(errorBorder);
        }
      }
    };
    getTxtArray().getDocument().addDocumentListener(ddl2);
  }

  protected void settingsToStroke() {
    try {
      String[] split = getTxtArray().getText().split(" ");
      float[] array = new float[split.length];
      for (int i = 0; i < split.length; i++)
        array[i] = Float.valueOf(split[i]);

      // all values
      float w = Float.valueOf(getTxtWidth().getText());
      float miterLimit = Float.valueOf(getTxtMiterLimit().getText());
      float phase = Float.valueOf(getTxtPhase().getText());
      int join = getJoinSelection();
      int cap = getCapSelection();

      SettingsBasicStroke s = new SettingsBasicStroke(w, cap, join, miterLimit, array, phase);
      // create new
      getStrokeChooserPanel().setStroke(s);
      getStrokeChooserPanel().repaint();
      pnPreview.repaint();
      result = s;
    } catch (Exception ex) {
    }
  }

  public int getJoinSelection() {
    String s = getComboJoin().getSelectedItem().toString();
    if (s.equals("Bevel"))
      return BasicStroke.JOIN_BEVEL;
    else if (s.equals("Miter"))
      return BasicStroke.JOIN_MITER;
    else
      return BasicStroke.JOIN_ROUND;
  }

  public int getCapSelection() {
    String s = getComboCap().getSelectedItem().toString();
    if (s.equals("Butt"))
      return BasicStroke.CAP_BUTT;
    else if (s.equals("Round"))
      return BasicStroke.CAP_ROUND;
    else
      return BasicStroke.CAP_SQUARE;
  }

  public void addApplyListener(ApplyListener l) {
    if (listener == null)
      listener = new ArrayList<ApplyListener>(1);
    listener.add(l);
  }

  public void clearApplyListener() {
    listener.clear();
  }

  public ArrayList<ApplyListener> getApplyListener() {
    return listener;
  }

  public JTextField getTxtPhase() {
    return txtPhase;
  }

  public JTextField getTxtMiterLimit() {
    return txtMiterLimit;
  }

  public JTextField getTxtArray() {
    return txtArray;
  }

  public JTextField getTxtWidth() {
    return txtWidth;
  }

  public JComboBox getComboCap() {
    return comboCap;
  }

  public JComboBox getComboJoin() {
    return comboJoin;
  }

  public JStrokeChooser getStrokeChooserPanel() {
    return strokeChooserPanel;
  }
}
