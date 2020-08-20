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

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return null;
  }

  @Nonnull
  @Override
  public String getPropertyValue(String property) {
    return null;
  }

  @Nonnull
  @Override
  public Map<String, String> getAllProperties() {
    return null;
  }

  @Nonnull
  @Override
  public Object clone() {
    return null;
  }
}
