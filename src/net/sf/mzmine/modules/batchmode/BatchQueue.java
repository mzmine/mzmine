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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.batchmode;

import java.util.Map;
import java.util.Vector;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.dialogs.ExitCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Batch steps queue
 */
public class BatchQueue extends Vector<BatchStepWrapper> implements
		ParameterSet {

	public static final String BATCHSTEP_ELEMENT = "batchstep";
	public static final String METHOD_ELEMENT = "method";

	public void saveValuesToXML(Element xmlElement) {

		Document parentDocument = xmlElement.getOwnerDocument();

		for (int i = 0; i < size(); i++) {
			BatchStepWrapper step = get(i);

			Element stepElement = parentDocument
					.createElement(BATCHSTEP_ELEMENT);
			String methodName = step.getMethod().getClass().getName();
			stepElement.setAttribute(METHOD_ELEMENT, methodName);
			ParameterSet stepParameters = step.getParameters();
			stepParameters.saveValuesToXML(stepElement);

		}

	}

	public void loadValuesFromXML(Element xmlElement) {

		// erase current steps
		clear();

		MZmineModule allModules[] = MZmineCore.getAllModules();

		NodeList nodes = xmlElement.getElementsByTagName(BATCHSTEP_ELEMENT);
		for (int i = 0; i < nodes.getLength(); i++) {

			Element stepElement = (Element) nodes.item(i);
			String methodName = stepElement.getAttribute(METHOD_ELEMENT);

			for (MZmineModule module : allModules) {
				if ((module instanceof BatchStep)
						&& (module.getClass().getName().equals(methodName))) {
					BatchStep method = (BatchStep) module;
					ParameterSet parameters = module.getParameterSet().clone();
						parameters.loadValuesFromXML(stepElement);
					BatchStepWrapper step = new BatchStepWrapper(method,
							parameters);
					add(step);
					break;
				}
			}

		}

	}

	public BatchQueue clone() {

		BatchQueue clonedQueue = new BatchQueue();
		for (int i = 0; i < size(); i++) {
			BatchStepWrapper step = get(i);
			BatchStepWrapper clonedStep = new BatchStepWrapper(
					step.getMethod(), step.getParameters().clone());
			clonedQueue.add(clonedStep);
		}
		return clonedQueue;
	}

	@Override
	public UserParameter[] getParameters() {
		return null;
	}

	@Override
	public <T extends Parameter> T getParameter(T parameter) {
		return null;
	}

	@Override
	public ExitCode showSetupDialog() {
		BatchModeDialog dialog = new BatchModeDialog(this);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	@Override
	public ExitCode showSetupDialog(Map<UserParameter, Object> autoValues) {
		return null;
	}


}