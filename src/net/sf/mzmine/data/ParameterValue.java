package net.sf.mzmine.data;

/**
 * ParameterValue interface represent a value of a single parameter
 */
public interface ParameterValue {

	/**
	 * @return Parameter value as Object 
	 */
	public Object getValue();
	
	/**
	 * @return Parameter value as Integer or null if it is not possible to cast value to Integer
	 */
	public Integer getIntegerValue();
	
	/**
	 * @return Parameter value as Double or null if it is not possible to cast value to Double
	 */
	public Double getDoubleValue();
	
	/**
	 * @return Parameter value as String or null if it is not possible to cast value to String
	 */
	public String getStringValue();
	
	/**
	 * @return Parameter value as Boolean or null if it is not possible to cast value to Boolean
	 */
	public Boolean getBooleanValue();
	
}
