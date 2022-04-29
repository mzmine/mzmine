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

package io.github.mzmine.project.parameterssetup;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import java.util.stream.Stream;

public class ProjectMetadataParameters extends SimpleParameterSet {

  public static final StringParameter title = new StringParameter("Title",
      "Title of the new parameter", "", true, true);

  public enum AvailableTypes {
    TEXT, DOUBLE, DATETIME
  }

  public static final ComboParameter<String> valueType = new ComboParameter<String>("Type",
      "Type of the new parameter",
      Stream.of(AvailableTypes.values()).map(Enum::toString).toArray(String[]::new),
      Stream.of(AvailableTypes.values()).map(Enum::toString).toArray(String[]::new)[0]);

  public ProjectMetadataParameters() {
    super(new Parameter[]{title, valueType});
  }
}
