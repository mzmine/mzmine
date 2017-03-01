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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.parameters.parametertypes;

import javax.swing.JButton;

import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AddAdductsAction;
import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.DefaultAdductsAction;
import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.ExportAdductsAction;
import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.ImportAdductsAction;

/**
 * A component for selecting adducts.
 *
 */
public class AdductsComponent extends MultiChoiceComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create the component.
     *
     * @param choices
     *            the adduct choices.
     */
    public AdductsComponent(AdductType[] choices) {

	super(choices);
	addButton(new JButton(new AddAdductsAction()));
	addButton(new JButton(new ImportAdductsAction()));
	addButton(new JButton(new ExportAdductsAction()));
	addButton(new JButton(new DefaultAdductsAction()));
    }
}
