/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.io.csvexport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class IdentityItemsParameter extends MultiChoiceParameter<String> {

	public static final String ALL_IDENTITIES = "All identity elements";

	public IdentityItemsParameter() {
		super("Export identity elements", "Selection of identities to export",
				new String[] { ALL_IDENTITIES });
	}

	@Override
	public MultiChoiceComponent createEditingComponent() {

		HashSet<String> elements = new HashSet<String>();

		elements.add(ALL_IDENTITIES);

		for (PeakList peakList : MZmineCore.getCurrentProject().getPeakLists()) {
			for (PeakListRow peakListRow : peakList.getRows()) {

				PeakIdentity peakIdentity = peakListRow
						.getPreferredPeakIdentity();
				if (peakIdentity != null) {

					Map<String, String> properties = peakIdentity
							.getAllProperties();
					Iterator<String> subItr = properties.keySet().iterator();

					while (subItr.hasNext()) {

						String propertyName = subItr.next();
						if (!elements.contains(propertyName)) {
							elements.add(propertyName);
						}

					}

				}
			}
		}

		String identityElements[] = elements.toArray(new String[0]);
		Arrays.sort(identityElements);

		setChoices(identityElements);

		return super.createEditingComponent();
	}

	@Override
	public IdentityItemsParameter cloneParameter() {
		IdentityItemsParameter copy = new IdentityItemsParameter();
		copy.setChoices(this.getChoices());
		copy.setValue(this.getValue());
		return copy;
	}

}
