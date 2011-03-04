/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.dialogs.ExitCode;

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

	private Parameter parameters[];

	public SimpleParameterSet(Parameter parameters[]) {
		// Create a copy rather than reference
		this.parameters = new Parameter[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			this.parameters[i] = parameters[i].clone();
		}
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public void loadValuesFromXML(Element xmlElement) {
		NodeList list = xmlElement.getElementsByTagName(parameterElement);
		for (int i = 0; i < list.getLength(); i++) {
			Element nextElement = (Element) list.item(i);
			String paramName = nextElement.getAttribute(nameAttribute);
			for (Parameter param : parameters) {
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
		for (Parameter param : parameters) {
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
			if (!(parameters[i] instanceof UserParameter))
				continue;
			UserParameter up = (UserParameter) parameters[i];
			s.append(up.getName());
			s.append(": ");
			s.append(up.getValue());
			if (i < parameters.length - 1)
				s.append(", ");
		}
		return s.toString();
	}

	/**
	 * Make a deep copy
	 */
	public ParameterSet clone() {
		Parameter newParameters[] = new Parameter[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			newParameters[i] = parameters[i].clone();
		}
		SimpleParameterSet copy = new SimpleParameterSet(newParameters);
		return copy;
	}

	@SuppressWarnings("unchecked")
	public <T extends Parameter> T getParameter(T parameter) {
		for (Parameter p : parameters) {
			if (p.getName().equals(parameter.getName()))
				return (T) p;
		}
		throw new IllegalArgumentException("Parameter " + parameter.getName()
				+ " does not exist");
	}

	@Override
	public ExitCode showSetupDialog() {
		ParameterSetupDialog dialog = new ParameterSetupDialog(this);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	@Override
	public ExitCode showSetupDialog(Map<UserParameter, Object> autoValues) {
		ParameterSetupDialog dialog = new ParameterSetupDialog(this, autoValues);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

}
