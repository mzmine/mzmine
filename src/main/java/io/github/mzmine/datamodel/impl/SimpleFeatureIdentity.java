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

import io.github.mzmine.datamodel.FeatureIdentity;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;

/**
 * Simple FeatureIdentity implementation;
 */
public class SimpleFeatureIdentity implements FeatureIdentity {

  private Hashtable<String, String> properties;

  /**
   * This constructor is protected so only derived classes can use it. Other modules using this
   * class should always set the name by default.
   */
  protected SimpleFeatureIdentity() {

    this("Unknown name");
  }

  public SimpleFeatureIdentity(final String name) {

    // Check name.
    if (name == null) {
      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties = new Hashtable<String, String>();

    properties.put(PROPERTY_NAME, name);

  }

  public SimpleFeatureIdentity(final String name, final String formula, final String method,
      final String id, final String url) {

    // Check name
    if (name == null) {
      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties = new Hashtable<String, String>();

    properties.put(PROPERTY_NAME, name);

    if (formula != null) {
      properties.put(PROPERTY_FORMULA, formula);
    }
    if (method != null) {
      properties.put(PROPERTY_METHOD, method);
    }
    if (id != null) {
      properties.put(PROPERTY_ID, id);
    }
    if (url != null) {
      properties.put(PROPERTY_URL, url);
    }
  }

  public SimpleFeatureIdentity(final Hashtable<String, String> prop) {

    // Check for name .
    if (prop.get(PROPERTY_NAME) == null) {

      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties = prop;
  }

  @Override
  public @NotNull String getName() {

    return properties.get(PROPERTY_NAME);
  }

  public String toString() {

    return getName();
  }

  @Override
  public @NotNull String getDescription() {

    final StringBuilder description = new StringBuilder();
    for (final Entry<String, String> entry : properties.entrySet()) {

      if (description.length() > 0) {
        description.append('\n');
      }
      description.append(entry.getKey());
      description.append(": ");
      description.append(entry.getValue());
    }

    return description.toString();
  }

  @Override
  public @NotNull Map<String, String> getAllProperties() {

    return new Hashtable<String, String>(properties);
  }

  @Override
  // Removed @NotNull in front because the function may return null
  public String getPropertyValue(final String property) {
    return properties.get(property);
  }

  public void setPropertyValue(final String property, final String value) {

    // Check name.
    if (property.equals(PROPERTY_NAME) && value == null) {

      throw new IllegalArgumentException("Identity properties must contain name");
    }

    properties.put(property, value);
  }

  /**
   * Copy the identity.
   *
   * @return the new copy.
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized @NotNull Object clone() {
    return new SimpleFeatureIdentity((Hashtable<String, String>) properties.clone());
  }
}
