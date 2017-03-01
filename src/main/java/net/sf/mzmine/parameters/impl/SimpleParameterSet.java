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

package net.sf.mzmine.parameters.impl;

import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.ExitCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple storage for the parameters. A typical MZmine module will inherit this
 * class and define the parameters for the constructor.
 */
public class SimpleParameterSet implements ParameterSet {

    private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

    private static final String parameterElement = "parameter";
    private static final String nameAttribute = "name";

    private Parameter<?> parameters[];

    public SimpleParameterSet() {
	this.parameters = new Parameter<?>[0];
    }

    public SimpleParameterSet(Parameter<?> parameters[]) {
	this.parameters = parameters;
    }

    public Parameter<?>[] getParameters() {
	return parameters;
    }

    public void loadValuesFromXML(Element xmlElement) {
	NodeList list = xmlElement.getElementsByTagName(parameterElement);
	for (int i = 0; i < list.getLength(); i++) {
	    Element nextElement = (Element) list.item(i);
	    String paramName = nextElement.getAttribute(nameAttribute);
	    for (Parameter<?> param : parameters) {
		if (param.getName().equals(paramName)) {
		    try {
			param.loadValueFromXML(nextElement);
		    } catch (Exception e) {
			logger.log(Level.WARNING,
				"Error while loading parameter values for "
					+ param.getName(), e);
		    }
		}
	    }
	}
    }

    public void saveValuesToXML(Element xmlElement) {
	Document parentDocument = xmlElement.getOwnerDocument();
	for (Parameter<?> param : parameters) {
	    Element paramElement = parentDocument
		    .createElement(parameterElement);
	    paramElement.setAttribute(nameAttribute, param.getName());
	    xmlElement.appendChild(paramElement);
	    param.saveValueToXML(paramElement);
	}
    }

    /**
     * Represent method's parameters and their values in human-readable format
     */
    public String toString() {

	StringBuilder s = new StringBuilder();
	for (int i = 0; i < parameters.length; i++) {

	    Parameter<?> param = parameters[i];
	    Object value = param.getValue();

	    if (value == null)
		continue;

	    s.append(param.getName());
	    s.append(": ");
	    if (value.getClass().isArray()) {
		s.append(Arrays.toString((Object[]) value));
	    } else {
		s.append(value.toString());
	    }
	    if (i < parameters.length - 1)
		s.append(", ");
	}
	return s.toString();
    }

    /**
     * Make a deep copy
     */
    public ParameterSet cloneParameterSet() {

	// Make a deep copy of the parameters
	Parameter<?> newParameters[] = new Parameter[parameters.length];
	for (int i = 0; i < parameters.length; i++) {
	    newParameters[i] = parameters[i].cloneParameter();
	}

	try {
	    // Do not make a new instance of SimpleParameterSet, but instead
	    // clone the runtime class of this instance - runtime type may be
	    // inherited class. This is important in order to keep the proper
	    // behavior of showSetupDialog(xxx) method for cloned classes

	    SimpleParameterSet newSet = this.getClass().newInstance();
	    newSet.parameters = newParameters;
	    return newSet;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    @SuppressWarnings("unchecked")
    public <T extends Parameter<?>> T getParameter(T parameter) {
	for (Parameter<?> p : parameters) {
	    if (p.getName().equals(parameter.getName()))
		return (T) p;
	}
	throw new IllegalArgumentException("Parameter " + parameter.getName()
		+ " does not exist");
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	if ((parameters == null) || (parameters.length == 0))
	    return ExitCode.OK;
	ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
		valueCheckRequired, this);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }

    @Override
    public boolean checkParameterValues(Collection<String> errorMessages) {
	boolean allParametersOK = true;
	for (Parameter<?> p : parameters) {
	    boolean pOK = p.checkValue(errorMessages);
	    if (!pOK)
		allParametersOK = false;
	}
	return allParametersOK;
    }

}
