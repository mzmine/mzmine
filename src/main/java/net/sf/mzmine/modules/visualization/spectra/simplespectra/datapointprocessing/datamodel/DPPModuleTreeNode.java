package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import javax.swing.tree.DefaultMutableTreeNode;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.setup.DPPSetupWindowController;
import net.sf.mzmine.parameters.ParameterSet;

/**
 * Stores {@link DataPointProcessingModule}s and their parameters in a tree item. All MZmineModules implementing
 * DataPointProcessingModule are automatically added in {@link DPPSetupWindowController}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPModuleTreeNode extends DefaultMutableTreeNode {
  private DataPointProcessingModule module;
  private ModuleSubCategory subCat;
  private ParameterSet parameters;

  public DPPModuleTreeNode(DataPointProcessingModule module) {
    super(module.getName());
    setModule(module);
    setSubCat(module.getModuleSubCategory());
    setParameters(MZmineCore.getConfiguration().getModuleParameters(module.getClass()));
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
  
  @Override
  public DPPModuleTreeNode clone() {
    return new DPPModuleTreeNode(module);
  }
}
