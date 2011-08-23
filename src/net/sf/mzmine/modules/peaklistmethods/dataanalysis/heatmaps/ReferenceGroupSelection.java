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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps;

import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JComboBox;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import org.w3c.dom.Element;

public class ReferenceGroupSelection implements
        UserParameter<ParameterType, JComboBox> {

        private String name, description;
        private ParameterType value, parameterName;      

        public ReferenceGroupSelection() {
                this.name = "Group of reference";
                this.description = "Name of the group that will be used as a reference from the sample parameters";        

        }
        

        public String getDescription() {
                return description;
        }

        public JComboBox createEditingComponent() {
                ArrayList<Object> choicesList = new ArrayList<Object>();
                if (choicesList.isEmpty()) {
                        choicesList.add(ParameterType.NOPARAMETERS);
                }
                Object choices[] = choicesList.toArray();
                JComboBox editor = new JComboBox(choices);
                if (value != null) {
                        editor.setSelectedItem(value);
                }
                return editor;
        }

        public void setValueFromComponent(JComboBox component) {
                value = (ParameterType) component.getSelectedItem();
        }

        public void setValueToComponent(JComboBox component, ParameterType newValue) {
                component.setSelectedItem(newValue);
        }

        public String getName() {
                return name;
        }

        public ParameterType getValue() {
                return value;
        }

        public void setValue(ParameterType newValue) {
                this.value = newValue;
        }

        public boolean checkValue(Collection<String> errorMessages) {
                if (value == null) {
                        errorMessages.add(name + " is not set");
                        return false;
                }
                return true;
        }

        public void loadValueFromXML(Element xmlElement) {
                String elementString = xmlElement.getTextContent();
                if (elementString.length() == 0) {
                        return;
                }
                String attrValue = xmlElement.getAttribute("type");
                if (attrValue.equals("parameter")) {
                        for (UserParameter p : MZmineCore.getCurrentProject().getParameters()) {
                                if (p.getName().equals(elementString)) {
                                        value = new ParameterType(p);
                                        break;
                                }
                        }
                } else {
                        value = new ParameterType(elementString);
                }
        }

        public void saveValueToXML(Element xmlElement) {
                if (value == null) {
                        return;
                }
                if (value.isByParameter()) {
                        xmlElement.setAttribute("type", "parameter");
                        xmlElement.setTextContent(value.getParameter().getName());
                } else {
                        xmlElement.setTextContent(value.toString());
                }
        }

        public Parameter clone() {
                ReferenceGroupSelection copy = new ReferenceGroupSelection();
                copy.setValue(this.getValue());
                return copy;
        }
}
