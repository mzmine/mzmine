package net.sf.mzmine.modules.datapointprocessing.setup;

import javafx.scene.control.TreeItem;
import net.sf.mzmine.modules.datapointprocessing.ModuleSubCategory;

public class DPPModuleCategoryTreeItem extends TreeItem<String> {
  private ModuleSubCategory category;
  
  DPPModuleCategoryTreeItem(ModuleSubCategory category){
    super(category.toString());
    setCategory(category);
  }

  public ModuleSubCategory getCategory() {
    return category;
  }

  private void setCategory(ModuleSubCategory category) {
    this.category = category;
  }
}
