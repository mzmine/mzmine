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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.AdductsComponent;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * An action to add custom adducts.
 *
 */
public class AddAdductsAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create the action.
     */
    public AddAdductsAction() {

	super("Add...");
	putValue(SHORT_DESCRIPTION, "Add a custom adduct to the set of choices");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

	// Parent component.
	final AdductsComponent parent = (AdductsComponent) SwingUtilities
		.getAncestorOfClass(AdductsComponent.class,
			(Component) e.getSource());

	if (parent != null) {

	    // Show dialog.
	    final ParameterSet parameters = new AddAdductParameters();
	    if (parameters.showSetupDialog(MZmineCore.getDesktop()
		    .getMainWindow(), true) == ExitCode.OK) {

		// Create new adduct.
		final AdductType adduct = new AdductType(parameters
			.getParameter(AddAdductParameters.NAME).getValue(),
			parameters.getParameter(
				AddAdductParameters.MASS_DIFFERENCE).getValue());

		// Add to list of choices (if not already present).
		final Collection<AdductType> choices = new ArrayList<AdductType>(
			Arrays.asList((AdductType[]) parent.getChoices()));
		if (!choices.contains(adduct)) {

		    choices.add(adduct);
		    parent.setChoices(choices.toArray(new AdductType[choices
			    .size()]));
		}
	    }
	}
    }

    /**
     * Represents an adduct.
     */
    private static class AddAdductParameters extends SimpleParameterSet {

	// Adduct name.
	private static final StringParameter NAME = new StringParameter("Name",
		"A name to identify the new adduct");

	// Adduct mass difference.
	private static final DoubleParameter MASS_DIFFERENCE = new DoubleParameter(
		"Mass difference", "Mass difference for the new adduct",
		MZmineCore.getConfiguration().getMZFormat());

	private AddAdductParameters() {
	    super(new Parameter[] { NAME, MASS_DIFFERENCE });
	}
    }
}
