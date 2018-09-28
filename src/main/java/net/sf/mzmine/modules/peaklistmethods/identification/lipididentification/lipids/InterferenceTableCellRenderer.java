package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.sf.mzmine.util.components.ColorCircle;

public class InterferenceTableCellRenderer extends DefaultTableCellRenderer {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  ArrayList<ColorCircle> circle = new ArrayList<ColorCircle>();

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {

    return circle.get(row);

  }

}
