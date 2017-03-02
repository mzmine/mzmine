/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.datamodel;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * This interface is used to keep extra information about peaks
 * 
 * @author aleksandrsmirnov
 */
public interface PeakInformation extends Cloneable {
    
    /**
     * Returns the value of a property
     * 
     * @param property name
     * @return
     */
    
    @Nonnull
    String getPropertyValue(String property);
    
    @Nonnull
    String getPropertyValue(String property, String defaultValue);
    
    /**
     * Returns all the properties in the form of a map <key, value>
     * @return 
     */
    
    @Nonnull
    Map <String, String> getAllProperties();
    
    /**
     * Returns a copy of PeakInformation object
     * @return 
     */
    
    @Nonnull
    public Object clone();
}
