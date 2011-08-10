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
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 *
 *
 */
public class ParameterSelection implements
        UserParameter<ParameterType, JComboBox> {

        private String name, description;
        private ParameterType value;

        public ParameterSelection() {
                this.name = "Sample parameter";
                this.description = "One sample parameter has to be selected to be used in the heat map. They can be defined in \"Project -> Set sample parameters\"";
        }

        /**
         * @see net.sf.mzmine.data.Parameter#getName()
         */
        @Override
        public String getName() {
                return name;
        }

        /**
         * @see net.sf.mzmine.data.Parameter#getDescription()
         */
        @Override
        public String getDescription() {
                return description;
        }

        @Override
        public JComboBox createEditingComponent() {
                ArrayList<Object> choicesList = new ArrayList<Object>();

                for (UserParameter p : MZmineCore.getCurrentProject().getParameters()) {
                        choicesList.add(new ParameterType(p));
                }

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

        @Override
        public ParameterType getValue() {
                return value;
        }

        @Override
        public void setValue(ParameterType value) {
                this.value = value;
        }

        @Override
        public ParameterSelection clone() {
                ParameterSelection copy = new ParameterSelection();
                copy.setValue(this.getValue());
                return copy;
        }

        @Override
        public void setValueFromComponent(JComboBox component) {
                value = (ParameterType) component.getSelectedItem();
        }

        @Override
        public void setValueToComponent(JComboBox component, ParameterType newValue) {
                component.setSelectedItem(newValue);
        }

        @Override
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

        @Override
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

        @Override
        public boolean checkValue(Collection<String> errorMessages) {
                // TODO Auto-generated method stub
                return true;
        }
}
