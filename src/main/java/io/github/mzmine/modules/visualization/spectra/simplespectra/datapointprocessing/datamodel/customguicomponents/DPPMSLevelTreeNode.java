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

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;

/**
 * Stores MSLevel in a tree item. Used to organize the tree view automatically.
 * Every {@link MSLevel} is automatically added in {@link ProcessingComponent}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPMSLevelTreeNode extends DisableableTreeNode {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private MSLevel mslevel;

    public DPPMSLevelTreeNode(MSLevel mslevel) {
        super(mslevel.toString());
        setMSLevel(mslevel);
    }

    public MSLevel getMSLevel() {
        return mslevel;
    }

    private void setMSLevel(MSLevel mslevel) {
        this.mslevel = mslevel;
    }
}
