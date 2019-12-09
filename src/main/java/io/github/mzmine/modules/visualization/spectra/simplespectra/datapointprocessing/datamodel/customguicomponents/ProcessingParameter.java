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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import java.util.Collection;
import org.w3c.dom.Element;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPParameterValueWrapper;
import io.github.mzmine.parameters.UserParameter;

public class ProcessingParameter implements
        UserParameter<DPPParameterValueWrapper, ProcessingComponent> {

    private String name;
    private String description;
    private DPPParameterValueWrapper value;

    public ProcessingParameter(String name, String description) {
        this.name = name;
        this.description = description;
        this.value = new DPPParameterValueWrapper();
        // if(queue == null)
        // this.value =
        // DataPointProcessingManager.getInst().getProcessingQueue();
        // else
        // this.value = queue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DPPParameterValueWrapper getValue() {
        return value;
    }

    @Override
    public void setValue(DPPParameterValueWrapper newValue) {
        this.value = newValue;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if (value == null) {
            errorMessages.add("Queue has not been set up. (null)");
            return false;
        }
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        this.value.loadfromXML(xmlElement);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value != null)
            value.saveValueToXML(xmlElement);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ProcessingComponent createEditingComponent() {
        ProcessingComponent comp = new ProcessingComponent();
        return comp;
    }

    @Override
    public void setValueFromComponent(ProcessingComponent component) {
        this.value = component.getValueFromComponent();
        DataPointProcessingManager.getInst().updateParameters();
    }

    @Override
    public void setValueToComponent(ProcessingComponent component,
            DPPParameterValueWrapper newValue) {
        component.setValueFromValueWrapper(newValue);
    }

    @Override
    public UserParameter<DPPParameterValueWrapper, ProcessingComponent> cloneParameter() {
        ProcessingParameter copy = new ProcessingParameter(name, description);
        copy.setValue(this.value.clone());
        return copy;
    }

}
