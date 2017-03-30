/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.mzmine.datamodel.impl;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.PeakInformation;

/**
 *
 * @author aleksandrsmirnov
 */
public class SimplePeakInformation implements PeakInformation {
    
    private final Map <String, String> properties;
    
    // ------------------------------------------------------------------------
    // ----- Constructors -----------------------------------------------------
    // ------------------------------------------------------------------------
    
    public SimplePeakInformation() {
        properties = new HashMap <> ();
    }
    
    public SimplePeakInformation(String propertyName, String propertyValue) {
        this();
        properties.put(propertyName, propertyValue);
    }
    
    public SimplePeakInformation(Map <String, String> properties) {
        this.properties = properties;
    }
    
    // ------------------------------------------------------------------------
    // ----- Methods ----------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public void addProperty(String name, String value) {
        properties.put(name, value);
    }
    
    public void addProperty(Map <String, String> properties) {
        this.properties.putAll(properties);
    }
    
    // ------------------------------------------------------------------------
    // ----- Properties -------------------------------------------------------
    // ------------------------------------------------------------------------
    
    @Override @Nonnull
    public String getPropertyValue(String propertyName) {
        return properties.get(propertyName);
    }
    
    @Override @Nonnull
    public String getPropertyValue(String propertyName, String defaultValue) {
        String value = properties.get(propertyName);
        if (value == null) value = defaultValue;
        return value;
    }
    
    @Override @Nonnull
    public Map <String, String> getAllProperties() {
        return properties;
    }
    
    @Override @Nonnull
    public synchronized SimplePeakInformation clone() {
        return new SimplePeakInformation(new HashMap <> (properties));
    }
}
