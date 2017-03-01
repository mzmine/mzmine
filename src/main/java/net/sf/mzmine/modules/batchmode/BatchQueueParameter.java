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

package net.sf.mzmine.modules.batchmode;

import java.util.Collection;

import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

import org.w3c.dom.Element;

/**
 * Batch queue parameter.
 */
public class BatchQueueParameter implements
	UserParameter<BatchQueue, BatchSetupComponent> {

    private BatchQueue value;

    /**
     * Create the parameter.
     */
    public BatchQueueParameter() {
	value = null;
    }

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
    public void setValue(final BatchQueue newValue) {
	value = newValue;
    }

    @Override
    public void setValueFromComponent(final BatchSetupComponent component) {
	setValue(component.getValue());
    }

    @Override
    public void setValueToComponent(final BatchSetupComponent component,
	    final BatchQueue newValue) {
	component.setValue(newValue);
    }

    @Override
    public BatchQueueParameter cloneParameter() {
	final BatchQueueParameter copy = new BatchQueueParameter();
	copy.setValue(value.clone());
	return copy;
    }

    @Override
    public boolean checkValue(final Collection<String> errorMessages) {

	boolean allParamsOK = true;
	if (value == null) {

	    // Parameters not set.
	    errorMessages.add(getName() + " is not set");
	    allParamsOK = false;

	} else {

	    // Check each step.
	    for (final MZmineProcessingStep<?> batchStep : value) {

		// Check step's parameters.
		final ParameterSet params = batchStep.getParameterSet();
		if (params != null) {
		    for (final Parameter<?> parameter : params.getParameters()) {

			// Ignore the raw data files and peak lists parameters
			if (!(parameter instanceof RawDataFilesParameter)
				&& !(parameter instanceof PeakListsParameter)
				&& !parameter.checkValue(errorMessages)) {
			    allParamsOK = false;
			}
		    }
		}
	    }
	}

	return allParamsOK;
    }

    @Override
    public void loadValueFromXML(final Element xmlElement) {
	value = BatchQueue.loadFromXml(xmlElement);
    }

    @Override
    public void saveValueToXML(final Element xmlElement) {
	if (value != null) {
	    value.saveToXml(xmlElement);
	}
    }
}