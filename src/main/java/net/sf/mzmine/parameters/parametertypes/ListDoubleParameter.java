package net.sf.mzmine.parameters.parametertypes;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Element;

import net.sf.mzmine.parameters.UserParameter;


//import org.w3c.dom.Element;

//import net.sf.mzmine.parameters.UserParameter;


public class ListDoubleParameter implements UserParameter <List <Double>, ListDoubleComponent>

{	
		private final String name, description;
	    private final boolean valueRequired;
	    
	    private List <Double> value;
	    
	    public ListDoubleParameter (String name, String description,
	            boolean valueRequired, List <Double> defaultValue)
	    {
	        this.name = name;
	        this.description = description;
	        this.valueRequired = valueRequired;
	        this.value = defaultValue;
	    }
	    
	    @Override
	    public String getName() {return name;}
	    
	    @Override
	    public String getDescription() {return description;}
	    
	    public boolean isValueRequired() {return valueRequired;}
	    
	    @Override
	    public ListDoubleComponent createEditingComponent() {
	        return new ListDoubleComponent();
	    }
	    
	    @Override
	    public List <Double> getValue() {return value;}
	    
	    @Override 
	    public void setValue (List <Double> value) {
	        this.value = value;
	    }
	    
	    @Override
	    public ListDoubleParameter cloneParameter() {
	        ListDoubleParameter copy = new ListDoubleParameter(
	                name, description, valueRequired, value);
	        copy.setValue(value);
	        return copy;
	    }
	    
	    @Override
	    public void setValueFromComponent(ListDoubleComponent component) {
	        value = component.getValue();
	    }

	    @Override
	    public void setValueToComponent(ListDoubleComponent component,
	            List <Double> newValue) {
	        component.setValue(newValue);
	    }
	    
	    @Override
	    public void loadValueFromXML(Element xmlElement)
	    {

	    	String values = xmlElement.getTextContent().replaceAll("\\s","");
        	String[] strValues = values.split(",");
        	double[] doubleValues = new double[strValues.length];
        	for(int i = 0; i < strValues.length; i++) {
        	    try {
        	       doubleValues[i] = Double.parseDouble(strValues[i]);
        	    } catch (NumberFormatException nfe) {
        	       // The string does not contain a parsable integer.
        	    }
        	} 
        	Double[] doubleArray = ArrayUtils.toObject(doubleValues);
        	List<Double> ranges = Arrays.asList(doubleArray);
	        value = ranges;
	    }
	    
	    @Override
	    public void saveValueToXML (Element xmlElement)
	    {
	        if (value == null) return;
	        
	        String[] strValues = new String[value.size()];
	    	
	    	for(int i = 0; i < value.size(); i++) {
	    		strValues[i] = Double.toString(value.get(i));
	    	}
	    	String text = String.join(",", strValues);
	        
	        
	        xmlElement.setTextContent(text);
	    }
	    
	    @Override
	    public boolean checkValue(Collection <String> errorMessages)
	    {
	        if (valueRequired && value == null) {
	            errorMessages.add(name + " is not set properly");
	            return false;
	        }
	        
	        return true;
	    }
	
	
	
	
	
	
}
