/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.sortfeaturelists;

import io.github.mzmine.datamodel.features.FeatureList;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * This is a very simple module which reorders feature lists alphabetically
 * 
 */
public class SortFeatureListsModule implements MZmineProcessingModule {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String MODULE_NAME = "Sort feature lists";
  private static final String MODULE_DESCRIPTION = "Sort selected feature lists alphabetically";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    List<FeatureList> featureLists = Arrays.asList(parameters
        .getParameter(SortFeatureListsParameters.featureLists).getValue().getMatchingFeatureLists());
    /*
     * PeakListTreeModel model = null; if (project instanceof MZmineProjectImpl) { model =
     * ((MZmineProjectImpl) project).getPeakListTreeModel(); } else if (MZmineCore.getDesktop()
     * instanceof MainWindow) { ProjectTree tree = ((MainWindow) MZmineCore.getDesktop())
     * .getMainPanel().getPeakListTree(); model = (PeakListTreeModel) tree.getModel(); }
     * 
     * if (model == null) throw new MSDKRuntimeException(
     * "Cannot find feature list tree model for sorting. Different MZmine project impl?");
     * 
     * final DefaultMutableTreeNode rootNode = model.getRoot();
     * 
     * // Get all tree nodes that represent selected feature lists, and remove // them from final
     * ArrayList<DefaultMutableTreeNode> selectedNodes = new ArrayList<DefaultMutableTreeNode>();
     * 
     * for (int row = 0; row < rootNode.getChildCount(); row++) { DefaultMutableTreeNode
     * selectedNode = (DefaultMutableTreeNode) rootNode .getChildAt(row); Object selectedObject =
     * selectedNode.getUserObject(); if (featureLists.contains(selectedObject)) {
     * selectedNodes.add(selectedNode); } }
     * 
     * // Get the index of the first selected item final ArrayList<Integer> positions = new
     * ArrayList<Integer>(); for (DefaultMutableTreeNode node : selectedNodes) { int nodeIndex =
     * rootNode.getIndex(node); if (nodeIndex != -1) positions.add(nodeIndex); } if
     * (positions.isEmpty()) return ExitCode.ERROR; int insertPosition = Collections.min(positions);
     * 
     * // Sort the feature lists by name Collections.sort(selectedNodes, new
     * Comparator<DefaultMutableTreeNode>() {
     * 
     * @Override public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) { return
     * o1.getUserObject().toString() .compareTo(o2.getUserObject().toString()); } });
     * 
     * // Reorder the nodes in the tree model for (DefaultMutableTreeNode node : selectedNodes) {
     * model.removeNodeFromParent(node); model.insertNodeInto(node, rootNode, insertPosition);
     * insertPosition++; }
     */
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELIST;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SortFeatureListsParameters.class;
  }

}
