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

package io.github.mzmine.modules.visualization.featurelisttable;

import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JToolBar;

import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.swing.IconUtil;

class PeakListTableToolBar extends JToolBar {

    private static final long serialVersionUID = 1L;
    private static final Icon propertiesIcon = IconUtil
            .loadIconFromResources("icons/propertiesicon.png");
    private static final Icon widthIcon = IconUtil
            .loadIconFromResources("icons/widthicon.png");
    private static final Icon printIcon = IconUtil
            .loadIconFromResources("icons/printicon.png");

    PeakListTableToolBar(PeakListTableWindow masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));

        GUIUtils.addButton(this, null, propertiesIcon, masterFrame,
                "PROPERTIES", "Set table properties");
        GUIUtils.addButton(this, null, widthIcon, masterFrame,
                "AUTOCOLUMNWIDTH", "Set auto column width");
        addSeparator();
        GUIUtils.addButton(this, null, printIcon, masterFrame, "PRINT",
                "Print");

    }

}
