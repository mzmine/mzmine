package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager.MSLevel;
import net.sf.mzmine.parameters.parametertypes.ProcessingComponent;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;

public class HighlightTreeCellRenderer extends DefaultTreeCellRenderer {

  private static Logger logger = Logger.getLogger(HighlightTreeCellRenderer.class.getName());
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private final DPPMSLevelTreeNode[] msLevelNodes;

  public HighlightTreeCellRenderer(DPPMSLevelTreeNode[] msLevelNodes) {
    this.msLevelNodes = msLevelNodes;
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected,
      boolean expanded, boolean leaf, int row, boolean hasFocus) {

    JComponent c = (JComponent) super.getTreeCellRendererComponent(tree, value, isSelected,
        expanded, leaf, row, hasFocus);

    if(!(value instanceof DPPMSLevelTreeNode)) {
      logger.finest("rendering value: " + value.toString() + "not important.");
      setOpaque(false);
      c.setBackground(getBackgroundNonSelectionColor());
      return c;
    }
    logger.finest("renderign value: " + value.toString() + " is mslevel node");
    
    boolean diffMSn =
        DataPointProcessingManager.getInst().getProcessingParameters().isDifferentiateMSn();
    
    c.setOpaque(true);
    c.setBackground(getNodeColor((DPPMSLevelTreeNode)value, diffMSn));
    
    /*if (isSelected) {
      c.setForeground(getTextSelectionColor());
      if (leaf && value.toString().equalsIgnoreCase("red")) {
        // <strong>
        c.setOpaque(true);
        // </strong>
        c.setBackground(Color.RED);
      } else {
        c.setOpaque(false);
        c.setBackground(getBackgroundSelectionColor());
      }
    } else {
      c.setOpaque(false);
      c.setForeground(getTextNonSelectionColor());
      c.setBackground(getBackgroundNonSelectionColor());
    }*/
    return c;
  }
  
  private Color getNodeColor(DPPMSLevelTreeNode node, boolean diffMSn) {
    if(!diffMSn && node.getMSLevel() == MSLevel.MSANY)
      return Color.GREEN;
    if(!diffMSn)
      return Color.RED;
    
    if(diffMSn && node.getMSLevel() == MSLevel.MSANY)
      return Color.RED;
    return Color.GREEN;
  }
}
