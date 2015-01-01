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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Adducts parameter.
 *
 * @author $Author$
 * @version $Revision$
 */
public class AdductsParameter extends MultiChoiceParameter<AdductType> {

    // Logger.
    private static final Logger LOG = Logger.getLogger(AdductsParameter.class
	    .getName());

    // XML tags.
    private static final String ADDUCTS_TAG = "adduct";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String MASS_ATTRIBUTE = "mass_difference";
    private static final String SELECTED_ATTRIBUTE = "selected";

    /**
     * Create the parameter.
     *
     * @param name
     *            name of the parameter.
     * @param description
     *            description of the parameter.
     */
    public AdductsParameter(final String name, final String description) {

	super(name, description, AdductType.getDefaultValues());
    }

    @Override
    public MultiChoiceComponent createEditingComponent() {

	return new AdductsComponent(getChoices());
    }

    @Override
    public void setValueFromComponent(final MultiChoiceComponent component) {

	super.setValueFromComponent(component);
	setChoices((AdductType[]) component.getChoices());
    }

    @Override
    public void loadValueFromXML(final Element xmlElement) {

	// Get the XML tag.
	final NodeList items = xmlElement.getElementsByTagName(ADDUCTS_TAG);
	final int length = items.getLength();

	// Start with current choices and empty selections.
	final ArrayList<AdductType> newChoices = new ArrayList<AdductType>(
		Arrays.asList(getChoices()));
	final ArrayList<AdductType> selections = new ArrayList<AdductType>(
		length);

	// Process each adduct.
	for (int i = 0; i < length; i++) {

	    final Node item = items.item(i);

	    // Get attributes.
	    final NamedNodeMap attributes = item.getAttributes();
	    final Node nameNode = attributes.getNamedItem(NAME_ATTRIBUTE);
	    final Node massNode = attributes.getNamedItem(MASS_ATTRIBUTE);
	    final Node selectedNode = attributes
		    .getNamedItem(SELECTED_ATTRIBUTE);

	    // Valid attributes?
	    if (nameNode != null && massNode != null) {

		try {
		    // Create new adduct.
		    final AdductType adduct = new AdductType(
			    nameNode.getNodeValue(),
			    Double.parseDouble(massNode.getNodeValue()));

		    // A new choice?
		    if (!newChoices.contains(adduct)) {

			newChoices.add(adduct);
		    }

		    // Selected?
		    if (!selections.contains(adduct)
			    && selectedNode != null
			    && Boolean
				    .parseBoolean(selectedNode.getNodeValue())) {

			selections.add(adduct);
		    }
		} catch (NumberFormatException ex) {

		    // Ignore.
		    LOG.warning("Illegal mass difference attribute in "
			    + item.getNodeValue());
		}
	    }
	}

	// Set choices and selections (value).
	setChoices(newChoices.toArray(new AdductType[newChoices.size()]));
	setValue(selections.toArray(new AdductType[selections.size()]));
    }

    @Override
    public void saveValueToXML(final Element xmlElement) {

	// Get choices and selections.
	final AdductType[] choices = getChoices();
	final AdductType[] value = getValue();
	final List<AdductType> selections = Arrays
		.asList(value == null ? new AdductType[] {} : value);

	if (choices != null) {

	    final Document parent = xmlElement.getOwnerDocument();
	    for (final AdductType item : choices) {

		final Element element = parent.createElement(ADDUCTS_TAG);
		element.setAttribute(NAME_ATTRIBUTE, item.getName());
		element.setAttribute(MASS_ATTRIBUTE,
			Double.toString(item.getMassDifference()));
		element.setAttribute(SELECTED_ATTRIBUTE,
			Boolean.toString(selections.contains(item)));
		xmlElement.appendChild(element);
	    }
	}
    }

    @Override
    public AdductsParameter cloneParameter() {

	final AdductsParameter copy = new AdductsParameter(getName(),
		getDescription());
	copy.setChoices(getChoices());
	copy.setValue(getValue());
	return copy;
    }
}
