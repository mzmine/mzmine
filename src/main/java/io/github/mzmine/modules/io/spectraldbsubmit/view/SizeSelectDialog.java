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

package io.github.mzmine.modules.io.spectraldbsubmit.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import org.jetbrains.annotations.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;

public class SizeSelectDialog extends JDialog {

  private final JPanel contentPanel = new JPanel();
  private JTextField txtWidth;
  private JTextField txtHeight;
  private Dimension dim = null;
  private JLabel lblWrongInput;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      SizeSelectDialog dialog = new SizeSelectDialog();
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   */
  public SizeSelectDialog() {
    setBounds(100, 100, 198, 161);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("", "[][]", "[][][]"));
    {
      JLabel lblWidth = new JLabel("width");
      contentPanel.add(lblWidth, "cell 0 0,alignx trailing");
    }
    {
      txtWidth = new JTextField();
      txtWidth.setText("400");
      contentPanel.add(txtWidth, "cell 1 0,growx");
      txtWidth.setColumns(10);
    }
    {
      JLabel lblHeight = new JLabel("height");
      contentPanel.add(lblHeight, "cell 0 1,alignx trailing");
    }
    {
      txtHeight = new JTextField();
      txtHeight.setText("300");
      contentPanel.add(txtHeight, "cell 1 1,growx");
      txtHeight.setColumns(10);
    }
    {
      lblWrongInput = new JLabel("wrong input");
      lblWrongInput.setFont(new Font("Tahoma", Font.BOLD, 13));
      lblWrongInput.setForeground(new Color(220, 20, 60));
      lblWrongInput.setVisible(false);
      contentPanel.add(lblWrongInput, "cell 0 2 2 1");
    }
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> checkResultsAndFinish());
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
      {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPane.add(cancelButton);
      }
    }
    setModalityType(ModalityType.APPLICATION_MODAL);
    setVisible(true);
    pack();
  }

  private void checkResultsAndFinish() {
    try {
      int w = Integer.parseInt(txtWidth.getText());
      int h = Integer.parseInt(txtHeight.getText());
      dim = new Dimension(w, h);
      setVisible(false);
    } catch (Exception e) {
      lblWrongInput.setVisible(true);
    }
  }

  @Nullable
  public Dimension getResult() {
    return dim;
  }

  public JLabel getLblWrongInput() {
    return lblWrongInput;
  }

  @Nullable
  public static Dimension getSizeInput() {
    SizeSelectDialog d = new SizeSelectDialog();
    return d.getResult();
  }
}
