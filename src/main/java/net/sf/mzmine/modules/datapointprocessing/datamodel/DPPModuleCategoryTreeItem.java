package net.sf.mzmine.modules.datapointprocessing.datamodel;

import javafx.scene.control.TreeItem;

public class DPPModuleCategoryTreeItem extends TreeItem<String> {
  private ModuleSubCategory category;
  
  public DPPModuleCategoryTreeItem(ModuleSubCategory category){
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
