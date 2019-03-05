package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Stores module categories in a tree item. Used to organize the tree view automatically. Every
 * {@link ModuleSubCategory} is automatically added in {@link DPPSetupWindowController}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPModuleCategoryTreeNode extends DefaultMutableTreeNode {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private ModuleSubCategory category;

  public DPPModuleCategoryTreeNode(ModuleSubCategory category) {
    super(category.getName());
    setCategory(category);
  }

  public ModuleSubCategory getCategory() {
    return category;
  }

  private void setCategory(ModuleSubCategory category) {
    this.category = category;
  }
}
