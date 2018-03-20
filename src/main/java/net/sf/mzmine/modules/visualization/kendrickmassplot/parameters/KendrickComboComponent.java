package net.sf.mzmine.modules.visualization.kendrickmassplot.parameters;

import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class KendrickComboComponent<ValueType> extends JPanel {
  private static final long serialVersionUID = 1L;

  private final JComboBox<ValueType> comboBox;
  private ActionListener l;

  public KendrickComboComponent(ValueType choices[], ActionListener l) {
    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
    comboBox = new JComboBox<ValueType>(choices);
    comboBox.addActionListener(l);
    add(comboBox);
  }

  @Override
  public void setToolTipText(String toolTip) {
    comboBox.setToolTipText(toolTip);
  }

  public Object getSelectedItem() {
    return comboBox.getSelectedItem();
  }

  public int getSelectedIndex() {
    return comboBox.getSelectedIndex();
  }

  public void setSelectedItem(ValueType newValue) {
    comboBox.setSelectedItem(newValue);
  }

  public void setModel(ComboBoxModel<ValueType> model) {
    comboBox.setModel(model);
  }
}
