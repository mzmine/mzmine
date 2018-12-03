/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.rawdatamethods.merge;

import java.awt.Window;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboComponent;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.util.ExitCode;

public class RawFileMergeParameters extends SimpleParameterSet {

  public enum MODE {
    MERGE_SELECTED, MERGE_PATTERN;
    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public enum POSITION {
    BEFORE_FIRST, AFTER_LAST;
    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ComboParameter<MODE> mode = new ComboParameter<MODE>("Mode",
      "Merge all selected to one or all file that have a matching suffix or prefixs", MODE.values(),
      MODE.MERGE_PATTERN);

  public static final ComboParameter<POSITION> position =
      new ComboParameter<POSITION>("Grouping identifier position",
          "Define position of the identifier to use for grouping (e.g., a number after the last _)",
          POSITION.values(), POSITION.AFTER_LAST);

  public static final StringParameter posMarker =
      new StringParameter("Position marker", "e.g., the last _ or any string", "_");


  public static final OptionalParameter<StringParameter> MS2_marker =
      new OptionalParameter<>(new StringParameter("MS2 marker",
          "Raw data files that contain this marker in their name will only be used as a source for MS2 scans.",
          ""));


  public static final StringParameter suffix =
      new StringParameter("Suffix to new name", "The suffix to describe the new file", "_merged");

  public RawFileMergeParameters() {
    super(new Parameter[] {dataFiles, mode, position, posMarker, MS2_marker, suffix});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    //
    ((ComboComponent) dialog.getComponentForParameter(mode)).addItemListener(e -> {
      boolean pattern = (e.getItem().equals(MODE.MERGE_PATTERN));
      dialog.getComponentForParameter(position).setVisible(pattern);
      dialog.getComponentForParameter(posMarker).setVisible(pattern);
    });


    dialog.setVisible(true);
    return dialog.getExitCode();
  }

}
