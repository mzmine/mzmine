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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;

public class DisableableTreeCellRenderer extends DefaultTreeCellRenderer {

    private static Logger logger = Logger
            .getLogger(DisableableTreeCellRenderer.class.getName());
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DisableableTreeCellRenderer() {
        super();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean isSelected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {

        JComponent c = (JComponent) super.getTreeCellRendererComponent(tree,
                value, isSelected, expanded, leaf, row, hasFocus);

        if (value instanceof DisableableTreeNode)
            c.setEnabled(((DisableableTreeNode) value).isEnabled());

        return c;
    }

    // private Color getNodeColor(DPPMSLevelTreeNode node, boolean diffMSn) {
    // if(!diffMSn && node.getMSLevel() == MSLevel.MSONE)
    // return Color.GREEN;
    // if(!diffMSn && node.getMSLevel() == MSLevel.MSMS)
    // return Color.RED;
    //
    // return Color.GREEN;
    // }
}
