package net.sf.mzmine.modules.visualization.kendrickmassplot.parameters;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class CustomKendrickMassComponent extends JPanel {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final JTextField textField;

  public CustomKendrickMassComponent(int inputsize) {
    textField = new JTextField(inputsize);
    add(textField);
  }

  public void setText(String text) {
    textField.setText(text);
  }

  public String getText() {
    return textField.getText();
  }

  @Override
  public void setToolTipText(String toolTip) {
    textField.setToolTipText(toolTip);
  }

  public void setEnabled() {
    textField.setEnabled(true);
  }

  public void setDisabled() {
    textField.setEnabled(false);
  }

}
