/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.PeakIdentity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * modified peakIdentity to contain multiple values for a single key
 */
public class TabularPeakIdentity implements PeakIdentity {

  private String name;
  private Hashtable<String, List<String>> properties;


  protected TabularPeakIdentity(){
    this("Unknown Name");
  }

  public TabularPeakIdentity(String name){
    this.name = name;
    properties = new Hashtable<>();
  }

  public boolean addProperty(String property, String value){
    if(properties.containsKey(property)){
      properties.get(property).add(value);
      return false;
    }
    else{
      List l = new ArrayList();
      l.add(value);
      properties.put(property,l);
      return true;
    }
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nonnull
  @Override
  public String getDescription() {
    if(properties.size() == 0)
      return "";
    final StringBuilder description = new StringBuilder();
    description.append("<table><tr>");
    Integer size = null;
    description.append("<th>S. No.</th>");
    for(String property : properties.keySet()){
      description.append("<th>");
      description.append(property);
      description.append("</th>");
      size = properties.get(property).size();
    }

    description.append("</tr>");
    for(int i=0 ; i<size ; i++){
      description.append("<tr>");
      description.append("<td>");
      description.append(i+1);
      description.append("</td>");
      for(String property : properties.keySet()){
        description.append("<td>");
        description.append(properties.get(property).get(i));
        description.append("</td>");
      }
      description.append("</tr>");
    }
    description.append("</tr>");
    return description.toString();
  }

  /**
   * As multiple values are possible, this method gives all values, seperated by space
   * @param property
   * @return String containing all the values seperated by space
   */
  @Nonnull
  @Override
  public String getPropertyValue(String property) {

    final StringBuilder condensedPropertyValues = new StringBuilder();
    for(String value : properties.get(property))
      condensedPropertyValues.append(value+" ");
    return condensedPropertyValues.toString();
  }

  @Nonnull
  @Override
  public Map<String, String> getAllProperties() {
    Map<String,String> allProperty = new HashMap<>();
    for(String property : properties.keySet()){
      String condensedProperty = this.getPropertyValue(property);
      allProperty.put(property,condensedProperty);
    }
    return allProperty;
  }

  /**
   * Copy the identity.
   *
   * @return the new copy.
   */

  @Override
  public synchronized   @Nonnull Object clone() {
    TabularPeakIdentity temp = new TabularPeakIdentity(this.name);
    temp.properties = (Hashtable<String, List<String>>) this.properties.clone();
    return temp;
  }
}
