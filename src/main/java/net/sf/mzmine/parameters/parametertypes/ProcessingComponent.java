package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.Container;
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
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPModuleCategoryTreeNode;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPModuleTreeNode;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.setup.DPPSetupWindow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;
import weka.classifiers.bayes.net.GUI;

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

  // File chooser
  private final LoadSaveFileChooser chooser;
  private static final String XML_EXTENSION = "xml";

  public ProcessingComponent() {
    super(new BorderLayout());
    setPreferredSize(new Dimension(600, 400));


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

  public ProcessingComponent(DataPointProcessingQueue queue) {
    this();
    setTreeViewProcessingItemsFromQueue(queue);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("BTN_ADD")) {
      addModule();
      sendQueue();
    } else if (e.getActionCommand().equals("BTN_REMOVE")) {
      removeModule();
      sendQueue();
    } else if (e.getActionCommand().equals("BTN_SET_PARAMETERS")) {
      setParameters(getSelectedItem(tvProcessing));
    } else if (e.getActionCommand().equals("BTN_LOAD")) {
      final File file = chooser.getLoadFile(DPPSetupWindow.getInstance().getFrame());

      DataPointProcessingQueue queue = DataPointProcessingQueue.loadFromFile(file);
      setTreeViewProcessingItemsFromQueue(queue);
      sendQueue();
    } else if (e.getActionCommand().equals("BTN_SAVE")) {
      final File file = chooser.getSaveFile(DPPSetupWindow.getInstance().getFrame(), XML_EXTENSION);
      DataPointProcessingQueue queue = getProcessingQueueFromTreeView();
      queue.saveToFile(file);
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
    DefaultMutableTreeNode tiProcessingRoot = new DefaultMutableTreeNode("Processing steps");
    DefaultMutableTreeNode tiAllModulesRoot = new DefaultMutableTreeNode("Modules");

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
          addModule();
        }
      }
    });
  }

  /**
   * Opens the parameter setup dialog of the selected module.
   */
  private void setParameters(DefaultMutableTreeNode _selected) {
    if (_selected == null || !(_selected instanceof DPPModuleTreeNode))
      return;

    DPPModuleTreeNode selected = (DPPModuleTreeNode) _selected;

    MZmineModule module = selected.getModule();
    ParameterSet stepParameters =
        MZmineCore.getConfiguration().getModuleParameters(module.getClass());

    // do i even have to clone here? since, unlike batch mode, this is the only place we use this
    // parameter set.
    // ParameterSet stepParameters = methodParameters.cloneParameterSet();

    if (stepParameters.getParameters().length > 0) {
      ExitCode exitCode = stepParameters.showSetupDialog(null, false);
      if (exitCode != ExitCode.OK)
        return;
    }

    // store the parameters in the tree item
    selected.setParameters(stepParameters);
    sendQueue(); // update the list
  }

  /**
   * Adds the selected module in the tvAllModules to the processing list
   */
  private void addModule() {
    DefaultMutableTreeNode selected = getSelectedItem(tvAllModules);
    if (selected == null)
      return;

    if (selected instanceof DPPModuleTreeNode) {
      addModule((DPPModuleTreeNode) selected.clone());
    } else {
      logger.finest("Cannot add item " + selected.toString() + " to processing list.");
    }
  }

  /**
   * Adds a module in the tvAllModules to the processing list
   */
  private void addModule(DPPModuleTreeNode node) {
    // a module cannot be added twice
    if (treeContains(tvProcessing, node)) {
      logger.finest("Cannot add module " + ((DPPModuleTreeNode) node).getModule().getName()
          + " to processing list twice.");
      return;
    }

    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tvProcessing.getModel().getRoot();
    ((DefaultTreeModel) tvProcessing.getModel()).insertNodeInto(node, root, root.getChildCount());

    logger.finest("Added module " + ((DPPModuleTreeNode) node).getModule().getName()
        + " to processing list.");
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
      ((DefaultTreeModel) tvProcessing.getModel()).removeNodeFromParent(selected);
      logger.finest("Removed module " + ((DPPModuleTreeNode) selected).getModule().getName()
          + " from processing list.");
    } else {
      logger.finest("Cannot remove item " + selected.toString() + " from processing list.");
    }
  }

  /**
   * Creates a DataPointProcessingQueue from the items currently in the tree view.
   * 
   * @return Instance of DataPointProcessingQueue.
   */
  public DataPointProcessingQueue getProcessingQueueFromTreeView() {
    DataPointProcessingQueue list = new DataPointProcessingQueue();

    if (((DefaultMutableTreeNode) tvProcessing.getModel().getRoot()).getChildCount() < 1)
      return list;

    Enumeration<DefaultMutableTreeNode> nodes =
        ((DefaultMutableTreeNode) tvProcessing.getModel().getRoot()).children();
    do {
      DefaultMutableTreeNode item = nodes.nextElement();
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
  private MZmineProcessingStep<DataPointProcessingModule> createProcessingStep(
      DPPModuleTreeNode item) {
    return new MZmineProcessingStepImpl<>(item.getModule(), item.getParameters());
  }

  /**
   * Sends the queue to the DataPointProcessingManager.
   */
  private void sendQueue() {
    if (((DefaultMutableTreeNode) tvProcessing.getModel().getRoot()).getChildCount() < 1)
      return;

    DataPointProcessingQueue queue = getProcessingQueueFromTreeView();
    if (queue.isEmpty())
      logger.info("Processing queue is empty. Sending empty list.");

    DataPointProcessingManager manager = DataPointProcessingManager.getInst();
    manager.clearProcessingSteps();
    manager.setProcessingQueue(queue);
  }

  /**
   * Creates a collection of DPPModuleTreeItem from a queue. Can be used after loading a queue from
   * a file.
   * 
   * @param queue The queue.
   * @return Collection<DPPModuleTreeItem>.
   */
  private Collection<DPPModuleTreeNode> createTreeItemsFromQueue(DataPointProcessingQueue queue) {
    Collection<DPPModuleTreeNode> items = new ArrayList<DPPModuleTreeNode>();

    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tvProcessing.getModel().getRoot();

    for (MZmineProcessingStep<DataPointProcessingModule> step : queue) {
      logger.info("adding module " + step.getModule().getName() + " to the list.");

      root.add(new DPPModuleTreeNode(step.getModule()));
    }

    return items;
  }

  /**
   * Convenience method to publicly set the items of the processing list from the tree view. Used to
   * set the default queue, if set, loaded by the manager's constructor.
   * 
   * @param queue
   */
  public void setTreeViewProcessingItemsFromQueue(DataPointProcessingQueue queue) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tvProcessing.getModel().getRoot();
    root.removeAllChildren();
    Collection<DPPModuleTreeNode> moduleNodes = createTreeItemsFromQueue(queue);
    for (DPPModuleTreeNode node : moduleNodes) {
      addModule(node);
    }
    expandAllNodes(tvProcessing);
  }

  private boolean treeContains(JTree tree, DefaultMutableTreeNode comp) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
    Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode node = e.nextElement();
      if (node.toString().equalsIgnoreCase(comp.toString())) {
        return true;
      }
    }
    return false;
  }

  private DefaultMutableTreeNode getSelectedItem(JTree tree) {
    TreeSelectionModel selectionModel = tree.getSelectionModel();
    if (selectionModel == null)
      return null;
    TreePath path = selectionModel.getSelectionPath();
    if (path == null)
      return null;
    return (DefaultMutableTreeNode) path.getLastPathComponent();
  }

  private void expandAllNodes(JTree tree) {
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }
  }
}
