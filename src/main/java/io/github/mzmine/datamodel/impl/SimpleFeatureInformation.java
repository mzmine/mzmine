/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.FeatureInformation;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author aleksandrsmirnov
 */
public class SimpleFeatureInformation implements FeatureInformation {

  private final Map<String, String> properties;

  // ------------------------------------------------------------------------
  // ----- Constructors -----------------------------------------------------
  // ------------------------------------------------------------------------

  public SimpleFeatureInformation() {
    properties = new HashMap<>();
  }

  public SimpleFeatureInformation(String propertyName, String propertyValue) {
    this();
    properties.put(propertyName, propertyValue);
  }

  public SimpleFeatureInformation(Map<String, String> properties) {
    this.properties = properties;
  }

  // ------------------------------------------------------------------------
  // ----- Methods ----------------------------------------------------------
  // ------------------------------------------------------------------------

  public void addProperty(String name, String value) {
    properties.put(name, value);
  }

  public void addProperty(Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  // ------------------------------------------------------------------------
  // ----- Properties -------------------------------------------------------
  // ------------------------------------------------------------------------

  @Override
  @NotNull
  public String getPropertyValue(String propertyName) {
    return properties.get(propertyName);
  }

  @Override
  @NotNull
  public String getPropertyValue(String propertyName, String defaultValue) {
    String value = properties.get(propertyName);
    if (value == null)
      value = defaultValue;
    return value;
  }

  @Override
  @NotNull
  public Map<String, String> getAllProperties() {
    return properties;
  }

  @Override
  @NotNull
  public synchronized SimpleFeatureInformation clone() {
    return new SimpleFeatureInformation(new HashMap<>(properties));
  }
}
