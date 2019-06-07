package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import javax.swing.tree.DefaultMutableTreeNode;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;

/**
 * Stores {@link DataPointProcessingModule}s and their parameters in a tree item. All MZmineModules
 * implementing DataPointProcessingModule are automatically added in
 * {@link DPPSetupWindowController}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPModuleTreeNode extends DefaultMutableTreeNode {
  private DataPointProcessingModule module;
  private ModuleSubCategory subCat;
  private ParameterSet parameters;
  private boolean dialogShowing;

  /**
   * avoid usage of this constructor, this is only used to set up the all modules tree view.
   * usually it is beneficial to set the parameters at creation.
   * 
   * @param module
   */
  public DPPModuleTreeNode(DataPointProcessingModule module) {
    this(module, MZmineCore.getConfiguration().getModuleParameters(module.getClass()));
  }

  public DPPModuleTreeNode(DataPointProcessingModule module, ParameterSet parameters) {
    super(module.getName());
    setModule(module);
    setSubCat(module.getModuleSubCategory());
    setParameters(parameters);
    setDialogShowing(false);
  }

  public DataPointProcessingModule getModule() {
    return module;
  }

  private void setModule(DataPointProcessingModule module) {
    this.module = module;
  }

  public ModuleSubCategory getSubCat() {
    return subCat;
  }

  private void setSubCat(ModuleSubCategory subCat) {
    this.subCat = subCat;
  }

  public ParameterSet getParameters() {
    return parameters;
  }

  public void setParameters(ParameterSet parameters) {
    this.parameters = parameters;
  }

  public boolean isDialogShowing() {
    return dialogShowing;
  }

  public void setDialogShowing(boolean dialogShowing) {
    this.dialogShowing = dialogShowing;
  }

  @Override
  public DPPModuleTreeNode clone() {
    return new DPPModuleTreeNode(module, getParameters().cloneParameterSet());
  }
}
