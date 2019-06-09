package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager.MSLevel;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPParameterValueWrapper;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents.DPPMSLevelTreeNode;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents.DPPModuleCategoryTreeNode;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents.DPPModuleTreeNode;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents.HighlightTreeCellRenderer;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;

public class ProcessingComponent extends JPanel implements ActionListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(ProcessingComponent.class.getName());

  private JTree tvProcessing;
  private JTree tvAllModules;
  private final JSplitPane split;
  private final JPanel buttonPanel;
  private final JCheckBox cbDiffMSn;

  // File chooser
  private final LoadSaveFileChooser chooser;
  private static final String XML_EXTENSION = "xml";
  
  DefaultMutableTreeNode tiProcessingRoot;
  DefaultMutableTreeNode tiAllModulesRoot;
  
  DPPMSLevelTreeNode[] msLevelNodes;
  DPPMSLevelTreeNode tiLastTarget;
  
  public ProcessingComponent() {
    super(new BorderLayout());
    
    setPreferredSize(new Dimension(600, 400));

    cbDiffMSn = GUIUtils.addCheckbox(this, "Use different settings for MS^1 and MS^n", this, "CBX_DIFFMSN",
        "If enabled, MS^1 and MS^n processing will use different parameters. The currently used settings are highlighted in green.");
    add(cbDiffMSn, BorderLayout.NORTH);

    setupTreeViews();
    split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tvProcessing, tvAllModules);
    initTreeListeners();
    add(split, BorderLayout.CENTER);
    split.setDividerLocation(300);

    buttonPanel = new JPanel(new FlowLayout());
    GUIUtils.addButton(buttonPanel, "Add", null, this, "BTN_ADD");
    GUIUtils.addButton(buttonPanel, "Remove", null, this, "BTN_REMOVE");
    GUIUtils.addButton(buttonPanel, "Set parameters", null, this, "BTN_SET_PARAMETERS");
    GUIUtils.addButton(buttonPanel, "Load", null, this, "BTN_LOAD");
    GUIUtils.addButton(buttonPanel, "Save", null, this, "BTN_SAVE");
    // GUIUtils.addButton(buttonPanel, "Set Default...", null, this, "BTN_SET_DEFAULT");
    add(buttonPanel, BorderLayout.SOUTH);

    chooser = new LoadSaveFileChooser("Select Processing Queue File");
    chooser.addChoosableFileFilter(new FileNameExtensionFilter("XML files", XML_EXTENSION));

    super.repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("BTN_ADD")) {
      addSelectedModule();
      sendValueWrapper();
    } else if (e.getActionCommand().equals("BTN_REMOVE")) {
      removeModule();
      sendValueWrapper();
    } else if (e.getActionCommand().equals("BTN_SET_PARAMETERS")) {
      DefaultMutableTreeNode item = getSelectedItem(tvProcessing);
      if (item != null)
        setParameters(item);
    } else if (e.getActionCommand().equals("BTN_LOAD")) {
      final File file = chooser.getLoadFile(this);
      if (file != null) {
        DPPParameterValueWrapper value = new DPPParameterValueWrapper();
        value.loadFromFile(file);
        setValueFromValueWrapper(value);
        sendValueWrapper();
      }
    } else if (e.getActionCommand().equals("BTN_SAVE")) {
      final File file = chooser.getSaveFile(this, XML_EXTENSION);
      if (file != null) {
        DPPParameterValueWrapper value = getValueFromComponent();
        value.saveToFile(file);
      }
    } else if(e.getActionCommand().equals("CBX_DIFFMSN")) {
      ((DefaultTreeModel)tvProcessing.getModel()).reload();
      expandAllNodes(tvProcessing);
      sendValueWrapper();
    }
    // else if(e.getActionCommand().equals("BTN_SET_DEFAULT")) {
    // final File file = chooser.getLoadFile(DPPSetupWindow.getInstance().getFrame());
    // if (file != null) {
    // DataPointProcessingManager.getInst().getParameters()
    // .getParameter(DataPointProcessingParameters.defaultDPPQueue).setValue(file);
    // logger.finest("Set default processing queue to: " + file.getAbsolutePath());
    // }
  }

  private void setupTreeViews() {
    tiProcessingRoot = new DefaultMutableTreeNode("Processing queues");
    msLevelNodes = new DPPMSLevelTreeNode[MSLevel.values().length];
    for(MSLevel mslevel : MSLevel.values()) {
      msLevelNodes[mslevel.ordinal()] = new DPPMSLevelTreeNode(mslevel);
      tiProcessingRoot.add(msLevelNodes[mslevel.ordinal()]);
    }
    
    tiAllModulesRoot = new DefaultMutableTreeNode("Modules");

    // create category items dynamically, if a new category is added later on.
    DPPModuleCategoryTreeNode[] moduleCategories =
        new DPPModuleCategoryTreeNode[ModuleSubCategory.values().length];
    for (int i = 0; i < moduleCategories.length; i++) {
      moduleCategories[i] = new DPPModuleCategoryTreeNode(ModuleSubCategory.values()[i]);
      tiAllModulesRoot.add(moduleCategories[i]);
    }

    // add modules to their module category items
    Collection<MZmineModule> moduleList = MZmineCore.getAllModules();
    for (MZmineModule module : moduleList) {
      if (module instanceof DataPointProcessingModule) {
        DataPointProcessingModule dppm = (DataPointProcessingModule) module;
        // only add modules that have applicable ms levels
        // add each module as a child of the module category items
        for (DPPModuleCategoryTreeNode catItem : moduleCategories) {
          if (dppm.getModuleSubCategory().equals(catItem.getCategory())) {
            catItem.add(new DPPModuleTreeNode(dppm));
          }
        }
      }
    }

    // add the categories to the root item
    tvProcessing = new JTree(tiProcessingRoot);
    tvAllModules = new JTree(tiAllModulesRoot);

    tvProcessing.setCellRenderer(new HighlightTreeCellRenderer(msLevelNodes));
    
    tvAllModules.setRootVisible(true);
    tvProcessing.setRootVisible(true);
    expandAllNodes(tvAllModules);
  }

  private void initTreeListeners() {
    tvProcessing.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (e.getClickCount() == 2) {
          setParameters(getSelectedItem(tvProcessing));
        }
      }
    });

    tvAllModules.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (e.getClickCount() == 2) {
          addSelectedModule();
        }
      }
    });
  }

  /**
   * Opens the parameter setup dialog of the selected module.
   */
  private void setParameters(@Nonnull DefaultMutableTreeNode _selected) {
    if (_selected == null || !(_selected instanceof DPPModuleTreeNode))
      return;

    DPPModuleTreeNode selected = (DPPModuleTreeNode) _selected;

    ParameterSet stepParameters = selected.getParameters();

    if (stepParameters.getParameters().length > 0 && !selected.isDialogShowing()) {
      selected.setDialogShowing(true);
      ExitCode exitCode = stepParameters.showSetupDialog(null, true);
      if (exitCode == ExitCode.OK) {
        // store the parameters in the tree item
        selected.setDialogShowing(false);
        selected.setParameters(stepParameters);
        sendValueWrapper(); // update the list
      }
    }

  }

  /**
   * Adds the selected module in the tvAllModules to the processing list
   */
  private void addSelectedModule() {
    DefaultMutableTreeNode selected = getSelectedItem(tvAllModules);
    if (selected == null)
      return;

    if (selected instanceof DPPModuleTreeNode) {
      DPPModuleTreeNode node = (DPPModuleTreeNode) selected.clone();
      addModule(node);
    } else {
      logger.finest("Cannot add item " + selected.toString() + " to " + this.getName() + ".");
    }
  }

  /**
   * Adds a module in the tvAllModules to the processing list
   */
  private void addModule(@Nonnull DPPModuleTreeNode node) {
    // a module cannot be added twice
    DPPMSLevelTreeNode target = getTargetNode();
    if(target == null)
      return;
    addModule(node, target);
  }
  
  /**
   * Adds a module in the tvAllModules to the processing list
   */
  private void addModule(@Nonnull DPPModuleTreeNode node, @Nonnull DPPMSLevelTreeNode target) {
    
    if(target == null)
      return;
    
    if (nodeContains(target, node)) {
      logger.finest("Cannot add module " + ((DPPModuleTreeNode) node).getModule().getName()
          + " to processing list twice.");
      return;
    }
    if (!moduleFitsMSLevel(node.getModule(), target)) {
      logger.warning("The use of module \"" + node.getModule().getName() + "\" ("
          + node.getModule().getApplicableMSLevel()
          + ") is not recommended for processing scans of MS-level \"" + target.getMSLevel().toString()
          + "\". This might lead to unexpected results.");
    }

    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tvProcessing.getModel().getRoot();
    ((DefaultTreeModel) tvProcessing.getModel()).insertNodeInto(node, target, target.getChildCount());

    logger.finest("Added module " + node.getModule().getName()
        + " to " + target.getMSLevel().toString() + " processing list.");
    expandAllNodes(tvProcessing);
  }

  /**
   * Removes the selected module in the tvProcessingList from the list
   */
  private void removeModule() {
    DefaultMutableTreeNode selected = getSelectedItem(tvProcessing);
    if (selected == null)
      return;

    if (selected instanceof DPPModuleTreeNode) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selected.getParent();
      parent.remove(selected);
      // selected.removeFromParent();
      logger.finest("Removed module " + ((DPPModuleTreeNode) selected).getModule().getName()
          + " from processing list.");
    } else {
      logger.finest("Cannot remove item " + selected.toString() + " from processing list.");
    }
    ((DefaultTreeModel) tvProcessing.getModel()).reload();
    expandAllNodes(tvProcessing);
  }

  public @Nonnull DPPParameterValueWrapper getValueFromComponent() {
    DPPParameterValueWrapper value = new DPPParameterValueWrapper();
    Boolean val = Boolean.valueOf(cbDiffMSn.isSelected());
    value.setDifferentiateMSn(val);
    
    for(MSLevel mslevel : MSLevel.values())
      value.setQueue(mslevel, getProcessingQueueFromNode(getNodeByMSLevel(mslevel)));

    return value;
  }
  
  /**
   * Creates DataPointProcessingQueues from the items currently in the tree view.
   * 
   * @return Instance of DataPointProcessingQueue.
   */
  public @Nonnull DataPointProcessingQueue getProcessingQueueFromNode(DPPMSLevelTreeNode parentNode) {
    DataPointProcessingQueue list = new DataPointProcessingQueue();

    if (parentNode.getChildCount() < 1)
      return list;

    Enumeration<?> nodes = parentNode.children();
    
    do {
      DefaultMutableTreeNode item = (DefaultMutableTreeNode) nodes.nextElement();
      if (!(item instanceof DPPModuleTreeNode))
        continue;
      DPPModuleTreeNode moduleitem = (DPPModuleTreeNode) item;
      list.add(createProcessingStep(moduleitem));
    } while (nodes.hasMoreElements());

    return list;
  }

  /**
   * Creates a MZmineProcessingStep<DataPointProcessingModule> from an DPPModuleTreeItem.
   * 
   * @param item Tree item.
   * @return Instance of MZmineProcessingStep<DataPointProcessingModule>.
   */
  private @Nonnull MZmineProcessingStep<DataPointProcessingModule> createProcessingStep(
      @Nonnull DPPModuleTreeNode item) {
    return new MZmineProcessingStepImpl<>(item.getModule(), item.getParameters());
  }

  /**
   * Sends the queues to the DataPointProcessingManager.
   */
  private void sendValueWrapper() {
    // if (((DefaultMutableTreeNode) tvProcessing.getModel().getRoot()).getChildCount() < 1)
    // return;

    List<String> errorMessage = new ArrayList<String>();
    DPPParameterValueWrapper value = getValueFromComponent();
    if (!value.checkValue(errorMessage))
      logger.info(errorMessage.toString());

    DataPointProcessingManager manager = DataPointProcessingManager.getInst();
//    manager.clearProcessingSteps();
    manager.setProcessingParameters(value);
  }

  /**
   * Creates a collection of DPPModuleTreeItem from a queue. Can be used after loading a queue from
   * a file.
   * 
   * @param queue The queue.
   * @return Collection<DPPModuleTreeItem>.
   */
  private @Nonnull Collection<DPPModuleTreeNode> createTreeItemsFromQueue(
      @Nullable DataPointProcessingQueue queue) {
    Collection<DPPModuleTreeNode> items = new ArrayList<DPPModuleTreeNode>();

    if (queue == null)
      return items;

    for (MZmineProcessingStep<DataPointProcessingModule> step : queue) {
      items.add(new DPPModuleTreeNode(step.getModule(), step.getParameterSet()));
    }

    return items;
  }

  /**
   * Convenience method to publicly set the items of the processing list from the tree view. Used to
   * set the default queue, if set, loaded by the manager's constructor.
   * 
   * @param queue
   */
  public void setTreeViewProcessingItemsFromQueue(@Nullable DataPointProcessingQueue queue, MSLevel level) {
    logger.info("Loading queue into tvProcessing...");
    DPPMSLevelTreeNode targetNode = getNodeByMSLevel(level);
    
    targetNode.removeAllChildren();
    Collection<DPPModuleTreeNode> moduleNodes = createTreeItemsFromQueue(queue);
    for (DPPModuleTreeNode node : moduleNodes) {
      addModule(node, targetNode);
    }
    ((DefaultTreeModel) tvProcessing.getModel()).reload();
    expandAllNodes(tvProcessing);
  }
  
  /**
   * Sets the values of the component.
   * @param valueWrapper
   */
  public void setValueFromValueWrapper(DPPParameterValueWrapper valueWrapper) {
    for(MSLevel mslevel : MSLevel.values())
      setTreeViewProcessingItemsFromQueue(valueWrapper.getQueue(mslevel), mslevel);
    
    cbDiffMSn.setSelected(valueWrapper.isDifferentiateMSn());
  }

  /**
   * Convenience method to check if the module's ms level is applicable for this component.
   * 
   * @param module
   * @return
   */
  public static boolean moduleFitsMSLevel(DataPointProcessingModule module, DPPMSLevelTreeNode target) {
    if (module.getApplicableMSLevel() == MSLevel.MSANY)
      return true;
    if (module.getApplicableMSLevel() == target.getMSLevel())
      return true;
    return false;
  }

  private boolean nodeContains(@Nonnull DefaultMutableTreeNode node, @Nonnull DefaultMutableTreeNode comp) {
    Enumeration<?> e = node.depthFirstEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
      if (n.toString().equalsIgnoreCase(comp.toString())) {
        return true;
      }
    }
    return false;
  }

  private @Nullable DefaultMutableTreeNode getSelectedItem(@Nonnull JTree tree) {
    TreeSelectionModel selectionModel = tree.getSelectionModel();
    if (selectionModel == null)
      return null;
    TreePath path = selectionModel.getSelectionPath();
    if (path == null)
      return null;
    return (DefaultMutableTreeNode) path.getLastPathComponent();
  }

  private void expandAllNodes(@Nonnull JTree tree) {
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }
  }
  
  private @Nonnull DPPMSLevelTreeNode getTargetNode() {
    DefaultMutableTreeNode n = getSelectedItem(tvProcessing);
    
    if(n instanceof DPPModuleTreeNode || n == tiProcessingRoot)
      return tiLastTarget;
    
    if(n instanceof DPPMSLevelTreeNode)
      tiLastTarget = (DPPMSLevelTreeNode) n;
    if(tiLastTarget == null)
      tiLastTarget = msLevelNodes[0];
    
    return tiLastTarget;
  }
  
  private @Nonnull DPPMSLevelTreeNode getNodeByMSLevel(MSLevel mslevel){
    return msLevelNodes[mslevel.ordinal()];
  }
}
