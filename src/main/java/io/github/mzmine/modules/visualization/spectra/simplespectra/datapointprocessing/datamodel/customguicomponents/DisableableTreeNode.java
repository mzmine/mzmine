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

import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;

public class DisableableTreeNode extends DefaultMutableTreeNode {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private boolean enabled;

    public DisableableTreeNode(String string) {
        super(string);
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables/Disables this node and all children nodes
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        Enumeration<?> e = this.children();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            if (n instanceof DisableableTreeNode) {
                ((DisableableTreeNode) n).setEnabled(isEnabled());
            }
        }
    }
}
