package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;

public class HighlightTreeCellRenderer extends DefaultTreeCellRenderer {

  private static Logger logger = Logger.getLogger(HighlightTreeCellRenderer.class.getName());
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JCheckBox cb;
  public HighlightTreeCellRenderer(JCheckBox cb) {
    super();
    this.cb = cb;
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected,
      boolean expanded, boolean leaf, int row, boolean hasFocus) {

    JComponent c = (JComponent) super.getTreeCellRendererComponent(tree, value, isSelected,
        expanded, leaf, row, hasFocus);

    if(!(value instanceof DPPMSLevelTreeNode)) {
//      logger.finest("rendering value: " + value.toString() + "not important.");
      setOpaque(false);
      c.setBackground(getBackgroundNonSelectionColor());
      return c;
    }
//    logger.finest("renderign value: " + value.toString() + " is mslevel node");
    
    boolean diffMSn = cb.isSelected();
    
    c.setOpaque(true);
    c.setBackground(getNodeColor((DPPMSLevelTreeNode)value, diffMSn));
    
    return c;
  }
  
  private Color getNodeColor(DPPMSLevelTreeNode node, boolean diffMSn) {
    if(!diffMSn && node.getMSLevel() == MSLevel.MSONE)
      return Color.GREEN;
    if(!diffMSn && node.getMSLevel() == MSLevel.MSMS)
      return Color.RED;
    
    return Color.GREEN;
  }
}
