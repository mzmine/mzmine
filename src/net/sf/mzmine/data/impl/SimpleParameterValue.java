package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;

public class SimpleParameterValue implements ParameterValue {

	private Object value;
	
	/**
	 * Initializes object with given value, and checks that it is acceptable value for the parameter
	 *
	 */
	
	
	/**
	 * Initializes object with given value, and checks that it is acceptable value for the parameter.
	 * 
	 * @param parameter	Parameter
	 * @param value	Value for the parameter
	 * @throws SimpleParameterValueInvalidValueException if value is not acceptable for the parameter
	 */
	public SimpleParameterValue(Parameter parameter, Object value) throws SimpleParameterValueInvalidValueException {

		// Check for correct type
		switch (parameter.getType()) {
		case INTEGER:
			try { Integer test = (Integer)value; }
			catch (ClassCastException e) { throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be Integer."); }			 
			break;
		case DOUBLE:
			try { Double test = (Double)value; }
			catch (ClassCastException e) { throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be Double."); }			 
			break;
		case STRING:
			try { String test = (String)value; }
			catch (ClassCastException e) { throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be String."); }			 
			break;
		case BOOLEAN:
			try { Boolean test = (Boolean)value; }
			catch (ClassCastException e) { throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be Boolean."); }			 
			break;		
		}
		
		// Check for possible values
		Object[] possibleValues = parameter.getPossibleValues();
		boolean foundMatch = true;
		if (possibleValues!=null) {
			foundMatch = false;
			for (Object possibleValue : possibleValues) {
				if (possibleValue==value) {
					foundMatch = true;
					break;
				}
			}
		}
		if (!foundMatch) throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " is not one of the possible parameter values.");
		
		// Check for min & max
		if ( 	(parameter.getType()==Parameter.ParameterType.INTEGER) ) {
			if (parameter.getMinimumValue()!=null) {
				Integer minValue = (Integer)parameter.getMinimumValue();
				if (minValue>(Integer)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be greater-than or equal to " + minValue + ".");
			}
			if (parameter.getMaximumValue()!=null) {
				Integer maxValue = (Integer)parameter.getMaximumValue();
				if (maxValue<(Integer)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be less-than or equal to " + maxValue + ".");
			}
		}
		if ( 	(parameter.getType()==Parameter.ParameterType.DOUBLE) ) {
			if (parameter.getMinimumValue()!=null) {
				Double minValue = (Double)parameter.getMinimumValue();
				if (minValue>(Double)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be greater-than or equal to " + minValue + ".");
			}
			if (parameter.getMaximumValue()!=null) {
				Double maxValue = (Double)parameter.getMaximumValue();
				if (maxValue<(Double)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be less-than or equal to " + maxValue + ".");
			}
		}		
		
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}

}
