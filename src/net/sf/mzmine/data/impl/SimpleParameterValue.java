package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;

public class SimpleParameterValue implements ParameterValue {

	private Object value;

	/**
	 * Initializes parameter value without checks
	 */
	public SimpleParameterValue(Object value) {
		this.value = value;
	}
	
	/**
	 * Initializes object with given value, and checks that it is acceptable value for the parameter.
	 * 
	 * @param parameter	Parameter, if null then checks for valid value are skipped
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
				Integer minValue = parameter.getMinimumValue().getIntegerValue();
				if (minValue>(Integer)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be greater-than or equal to " + minValue + ".");
			}
			if (parameter.getMaximumValue()!=null) {
				Integer maxValue = parameter.getMaximumValue().getIntegerValue();
				if (maxValue<(Integer)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be less-than or equal to " + maxValue + ".");
			}
		}
		if ( 	(parameter.getType()==Parameter.ParameterType.DOUBLE) ) {
			if (parameter.getMinimumValue()!=null) {
				Double minValue = parameter.getMinimumValue().getDoubleValue();
				if (minValue>(Double)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be greater-than or equal to " + minValue + ".");
			}
			if (parameter.getMaximumValue()!=null) {
				Double maxValue = parameter.getMaximumValue().getDoubleValue();
				if (maxValue<(Double)value)
					throw new SimpleParameterValueInvalidValueException("Value for parameter " + parameter.getName() + " must be less-than or equal to " + maxValue + ".");
			}
		}		
	
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}

	public Boolean getBooleanValue() {
		Boolean castedValue = null;
		try { castedValue = (Boolean)value;} catch (Exception e) {}
		return castedValue;
	}

	public Double getDoubleValue() {
		Double castedValue = null;
		try { castedValue = (Double)value;}	catch (Exception e) {}
		return castedValue;
	}

	public Integer getIntegerValue() {
		Integer castedValue = null;
		try { castedValue = (Integer)value;} catch (Exception e) {}
		return castedValue;
	}

	public String getStringValue() {
		String castedValue = null;
		try { castedValue = (String)value;} catch (Exception e) {}
		return castedValue;
	}

	public String toString() {
		return value.toString();
	}
}
