/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.tools.sortpeaklists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.swing.tree.DefaultMutableTreeNode;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.gui.impl.MainWindow;
import io.github.mzmine.gui.impl.projecttree.PeakListTreeModel;
import io.github.mzmine.gui.impl.projecttree.ProjectTree;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * This is a very simple module which reorders feature lists alphabetically
 * 
 */
public class SortPeakListsModule implements MZmineProcessingModule {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Sort feature lists";
    private static final String MODULE_DESCRIPTION = "Sort selected feature lists alphabetically";

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

        List<PeakList> peakLists = Arrays.asList(
                parameters.getParameter(SortPeakListsParameters.peakLists)
                        .getValue().getMatchingPeakLists());

        PeakListTreeModel model = null;
        if (project instanceof MZmineProjectImpl) {
            model = ((MZmineProjectImpl) project).getPeakListTreeModel();
        } else if (MZmineCore.getDesktop() instanceof MainWindow) {
            ProjectTree tree = ((MainWindow) MZmineCore.getDesktop())
                    .getMainPanel().getPeakListTree();
            model = (PeakListTreeModel) tree.getModel();
        }

        if (model == null)
            throw new MSDKRuntimeException(
                    "Cannot find feature list tree model for sorting. Different MZmine project impl?");

        final DefaultMutableTreeNode rootNode = model.getRoot();

        // Get all tree nodes that represent selected feature lists, and remove
        // them from
        final ArrayList<DefaultMutableTreeNode> selectedNodes = new ArrayList<DefaultMutableTreeNode>();

        for (int row = 0; row < rootNode.getChildCount(); row++) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) rootNode
                    .getChildAt(row);
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

        // Sort the feature lists by name
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
        return SortPeakListsParameters.class;
    }

}
