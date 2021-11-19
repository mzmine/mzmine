/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes;

/**
 * Flags all remove original feature list parameters for batch optimization
 */
public class RemoveOriginalSourcesParameter extends BooleanParameter {

  public RemoveOriginalSourcesParameter() {
    this("Remove original feature lists",
        "After processing, remove all original feature lists to clear memory", true);
  }

  public RemoveOriginalSourcesParameter(String name, String description) {
    this(name, description, true);
  }

  public RemoveOriginalSourcesParameter(String name, String description,
      Boolean defaultValue) {
    super(name, description, defaultValue);
  }

}
