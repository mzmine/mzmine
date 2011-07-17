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

import java.util.Collection;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Batch queue parameter
 */
public class BatchQueueParameter implements
		UserParameter<BatchQueue, BatchSetupComponent> {

	private static final String BATCHSTEP_ELEMENT = "batchstep";
	private static final String METHOD_ELEMENT = "method";

	private BatchQueue value;

	@Override
	public String getName() {
		return "Batch queue";
	}

	@Override
	public String getDescription() {
		return "Please add and configure individual batch steps";
	}

	@Override
	public BatchSetupComponent createEditingComponent() {
		return new BatchSetupComponent();
	}

	@Override
	public BatchQueue getValue() {
		return value;
	}

	@Override
	public void setValue(BatchQueue newValue) {
		this.value = newValue;
	}

	@Override
	public void setValueFromComponent(BatchSetupComponent component) {
		this.value = component.getValue();
	}

	@Override
	public void setValueToComponent(BatchSetupComponent component,
			BatchQueue newValue) {
		component.setValue(newValue);
	}

	public BatchQueueParameter clone() {
		BatchQueueParameter copy = new BatchQueueParameter();
		copy.setValue(value.clone());
		return copy;
	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		boolean allParamsOK = true;
		if (value == null) {
			errorMessages.add(getName() + " is not set");
			return false;
		}
		for (BatchStepWrapper batchStep : value) {
			MZmineProcessingModule module = batchStep.getMethod();
			ParameterSet params = batchStep.getParameters();
			if (params == null) {
				errorMessages.add("Parameters for " + module + " are not set");
			} else {
				for (Parameter<?> p : params.getParameters()) {
					// Ignore the raw data files and peak lists parameters
					if ((p instanceof RawDataFilesParameter)
							|| (p instanceof PeakListsParameter))
						continue;
					boolean pOK = p.checkValue(errorMessages);
					if (!pOK)
						allParamsOK = false;
				}
			}
		}
		return allParamsOK;
	}

	@Override
	public void loadValueFromXML(Element xmlElement) {

		BatchQueue newQueue = new BatchQueue();

		MZmineModule allModules[] = MZmineCore.getAllModules();

		NodeList nodes = xmlElement.getElementsByTagName(BATCHSTEP_ELEMENT);
		for (int i = 0; i < nodes.getLength(); i++) {

			Element stepElement = (Element) nodes.item(i);
			String methodName = stepElement.getAttribute(METHOD_ELEMENT);

			for (MZmineModule module : allModules) {
				if ((module instanceof MZmineProcessingModule)
						&& (module.getClass().getName().equals(methodName))) {
					MZmineProcessingModule method = (MZmineProcessingModule) module;
					ParameterSet parameters = module.getParameterSet().clone();
					parameters.loadValuesFromXML(stepElement);
					BatchStepWrapper step = new BatchStepWrapper(method,
							parameters);
					newQueue.add(step);
					break;
				}
			}

		}

		this.value = newQueue;
	}

	@Override
	public void saveValueToXML(Element xmlElement) {
		if (value == null)
			return;

		Document parentDocument = xmlElement.getOwnerDocument();

		for (int i = 0; i < value.size(); i++) {
			BatchStepWrapper step = value.get(i);

			Element stepElement = parentDocument
					.createElement(BATCHSTEP_ELEMENT);
			String methodName = step.getMethod().getClass().getName();
			stepElement.setAttribute(METHOD_ELEMENT, methodName);
			ParameterSet stepParameters = step.getParameters();
			stepParameters.saveValuesToXML(stepElement);

		}

	}

}