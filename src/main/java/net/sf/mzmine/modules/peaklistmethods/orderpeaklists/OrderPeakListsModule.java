/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.orderpeaklists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.desktop.impl.projecttree.PeakListTreeModel;
import net.sf.mzmine.desktop.impl.projecttree.ProjectTree;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * This is a very simple module which reorders peak lists alphabetically
 * 
 */
public class OrderPeakListsModule implements MZmineProcessingModule {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Order peak lists";
    private static final String MODULE_DESCRIPTION = "Order selected peak lists alphabetically";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        List<PeakList> peakLists = Arrays.asList(parameters
                .getParameter(OrderPeakListsParameters.peakLists).getValue()
                .getMatchingPeakLists());

        ProjectTree tree = ((MainWindow) MZmineCore.getDesktop())
                .getMainPanel().getPeakListTree();
        final PeakListTreeModel model = (PeakListTreeModel) tree.getModel();
        final DefaultMutableTreeNode rootNode = model.getRoot();

        // Get all tree nodes that represent selected peak lists, and remove
        // them from
        final ArrayList<DefaultMutableTreeNode> selectedNodes = new ArrayList<DefaultMutableTreeNode>();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path
                    .getLastPathComponent();
            Object selectedObject = selectedNode.getUserObject();
            if (peakLists.contains(selectedObject)) {
                selectedNodes.add(selectedNode);
            }
        }

        // Get the index of the first selected item
        final ArrayList<Integer> positions = new ArrayList<Integer>();
        for (DefaultMutableTreeNode node : selectedNodes) {
            int nodeIndex = rootNode.getIndex(node);
            if (nodeIndex != -1)
                positions.add(nodeIndex);
        }
        if (positions.isEmpty())
            return ExitCode.ERROR;
        int insertPosition = Collections.min(positions);

        // Sort the peak lists by name
        Collections.sort(selectedNodes,
                new Comparator<DefaultMutableTreeNode>() {
                    @Override
                    public int compare(DefaultMutableTreeNode o1,
                            DefaultMutableTreeNode o2) {
                        return o1.getUserObject().toString()
                                .compareTo(o2.getUserObject().toString());
                    }
                });

        // Reorder the nodes in the tree model
        for (DefaultMutableTreeNode node : selectedNodes) {
            model.removeNodeFromParent(node);
            model.insertNodeInto(node, rootNode, insertPosition);
            insertPosition++;
        }

        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLIST;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return OrderPeakListsParameters.class;
    }

}
