/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.data.impl;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.data.Parameter.ParameterType;

import org.dom4j.Element;

/**
 */
public class SimpleParameterSet implements StorableParameterSet {

    public static final String PARAMETER_ELEMENT_NAME = "parameter";
    public static final String PARAMETER_NAME_ATTRIBUTE = "name";
    public static final String PARAMETER_TYPE_ATTRIBUTE = "type";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // sorted parameters
    private Vector<Parameter> parameters;

    // parameter -> value
    private Hashtable<Parameter, Object> values;

    /**
     * Checks if project contains current value for some of the parameters, and
     * initializes this object using those values if present.
     * 
     */
    public SimpleParameterSet() {
        // Initialize hashtable for storing values for parameters
        parameters = new Vector<Parameter>();
        values = new Hashtable<Parameter, Object>();

    }

    /**
     * Checks if project contains current value for some of the parameters, and
     * initializes this object using those values if present.
     * 
     */
    public SimpleParameterSet(Parameter[] initParameters) {
        this();
        for (Parameter p : initParameters) {
            parameters.add(p);
        }
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#getAllParameters()
     */
    public Parameter[] getParameters() {
        return parameters.toArray(new Parameter[0]);
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#getParameter(java.lang.String)
     */
    public Parameter getParameter(String name) {
        Iterator<Parameter> it = parameters.iterator();
        while (it.hasNext()) {
            Parameter p = it.next();
            if (p.getName().equals(name))
                return p;
        }
        return null;
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#addParameter(net.sf.mzmine.data.Parameter)
     */
    public void addParameter(Parameter parameter) {

        Iterator<Parameter> params = parameters.iterator();

        while (params.hasNext()) {

            Parameter p = params.next();
            if (p.getName().equals(parameter.getName())) {
                logger.warning("Parameter set already contains parameter called "
                        + parameter.getName());
                return;
            }
        }

        parameters.add(parameter);

    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#removeParameter(net.sf.mzmine.data.Parameter)
     */
    public void removeParameter(Parameter parameter) {
        parameters.remove(parameter);
        values.remove(parameter);

    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#getParameterValue(net.sf.mzmine.data.Parameter)
     */
    public Object getParameterValue(Parameter parameter) {
        Object value = values.get(parameter);
        if (value == null)
            value = parameter.getDefaultValue();
        return value;
    }

    /**
     */
    public void setParameterValue(Parameter parameter, Object value)
            throws IllegalArgumentException {

        if (!parameters.contains(parameter))
            throw (new IllegalArgumentException("Unknown parameter " + parameter));

        Object possibleValues[] = parameter.getPossibleValues();
        if (possibleValues != null) {
            for (Object possibleValue : possibleValues) {
                if (possibleValue.equals(value)) {
                    values.put(parameter, possibleValue);
                    return;
                }
            }
            // value not found
            throw (new IllegalArgumentException("Invalid value " + value + " for parameter " + parameter));

        }

        switch (parameter.getType()) {
        case BOOLEAN:
            if (!(value instanceof Boolean))
                throw (new IllegalArgumentException("Value type mismatch"));
            break;
        case INTEGER:
            if (!(value instanceof Integer))
                throw (new IllegalArgumentException("Value type mismatch"));
            Integer minIValue = (Integer) parameter.getMinimumValue();
            if ((minIValue != null) && (minIValue.compareTo((Integer) value) > 0))
                throw (new IllegalArgumentException("Minimum value of parameter " + parameter + " is "
                        + minIValue));
            Integer maxIValue = (Integer) parameter.getMaximumValue();
            if ((maxIValue != null) && (maxIValue.compareTo((Integer) value) < 0))
                throw (new IllegalArgumentException("Maximum value of parameter " + parameter + "  is "
                        + maxIValue));

            break;

        case FLOAT:
            if (!(value instanceof Float))
                throw (new IllegalArgumentException("Value type mismatch"));
            Float minDValue = (Float) parameter.getMinimumValue();
            if ((minDValue != null) && (minDValue.compareTo((Float) value) > 0))
                throw (new IllegalArgumentException("Minimum value of parameter " + parameter + "  is "
                        + minDValue));
            Float maxDValue = (Float) parameter.getMaximumValue();
            if ((maxDValue != null) && (maxDValue.compareTo((Float) value) < 0))
                throw (new IllegalArgumentException("Maximum value of parameter " + parameter + "  is "
                        + maxDValue));
            break;
        case STRING:
            if (!(value instanceof String))
                throw (new IllegalArgumentException("Value type mismatch"));
            break;
        }

        values.put(parameter, value);
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#removeParameterValue(net.sf.mzmine.data.Parameter)
     */
    public void removeParameterValue(Parameter parameter) {
        values.remove(parameter);
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#exportValuesToXML(org.w3c.dom.Element)
     */
    public void exportValuesToXML(Element element) {

        Iterator<Parameter> params = parameters.iterator();

        while (params.hasNext()) {

            Parameter p = params.next();

            Element newElement = element.addElement(PARAMETER_ELEMENT_NAME);

            newElement.addAttribute(PARAMETER_NAME_ATTRIBUTE, p.getName());
            newElement.addAttribute(PARAMETER_TYPE_ATTRIBUTE,
                    p.getType().toString());

            Object value = getParameterValue(p);
            if (value != null) {
                newElement.addText(value.toString());
            }

        }

    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#importValuesFromXML(org.w3c.dom.Element)
     */
    public void importValuesFromXML(Element element) {

        Iterator paramIter = element.elementIterator(PARAMETER_ELEMENT_NAME);

        while (paramIter.hasNext()) {
            Element paramElem = (Element) paramIter.next();
            Parameter param = getParameter(paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE));

            if (param != null) {

                ParameterType paramType = ParameterType.valueOf(paramElem.attributeValue(PARAMETER_TYPE_ATTRIBUTE));
                String valueText = paramElem.getText();
                if ((valueText == null) || (valueText.length() == 0))
                    continue;
                Object value = null;
                switch (paramType) {
                case BOOLEAN:
                    value = Boolean.parseBoolean(valueText);
                    break;
                case INTEGER:
                    value = Integer.parseInt(valueText);
                    break;
                case FLOAT:
                    value = Float.parseFloat(valueText);
                    break;
                case STRING:
                    value = valueText;
                    break;
                }

                setParameterValue(param, value);
            }

        }

    }

    public SimpleParameterSet clone() {
        Parameter params[] = getParameters();
        SimpleParameterSet newSet = new SimpleParameterSet(params);
        for (Parameter p : params) {
            Object v = values.get(p);
            if (v != null)
                newSet.setParameterValue(p, v);
        }
        return newSet;
    }

    /**
     * Represent method's parameters and their values in human-readable format
     */
    public String toString() {
        String s = "";
        for (Parameter p : getParameters()) {
            s = s.concat(p.getName() + ": " + values.get(p) + ", ");
        }
        return s;
    }

}
