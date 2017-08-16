/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.projectmethods.projectsave;

import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.util.ExitCode;

public class ProjectSaveParameters extends SimpleParameterSet {

    private static final FileFilter filters[] = new FileFilter[] { new FileNameExtensionFilter(
	    "MZmine projects", "mzmine") };

    public static final FileNameParameter projectFile = new FileNameParameter(
	    "Project file", "File name of project to be saved");

    public ProjectSaveParameters() {
	super(new Parameter[] { projectFile });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	File currentProjectFile = MZmineCore.getProjectManager()
		.getCurrentProject().getProjectFile();

	if ((currentProjectFile != null) && (currentProjectFile.canWrite())) {
	    getParameter(projectFile).setValue(currentProjectFile);
	    return ExitCode.OK;
	}

	JFileChooser chooser = new JFileChooser();

	for (FileFilter filter : filters)
	    chooser.addChoosableFileFilter(filter);
	chooser.setFileFilter(filters[0]);

	chooser.setMultiSelectionEnabled(false);

	int returnVal = chooser.showSaveDialog(parent);
	if (returnVal != JFileChooser.APPROVE_OPTION)
	    return ExitCode.CANCEL;

	File selectedFile = chooser.getSelectedFile();

	if (!selectedFile.getName().endsWith(".mzmine")) {
	    selectedFile = new File(selectedFile.getPath() + ".mzmine");
	}

	if (selectedFile.exists()) {
	    int selectedValue = JOptionPane.showConfirmDialog(MZmineCore
		    .getDesktop().getMainWindow(), selectedFile.getName()
		    + " already exists, overwrite ?", "Question...",
		    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
	    if (selectedValue != JOptionPane.YES_OPTION)
		return ExitCode.CANCEL;
	}

	getParameter(projectFile).setValue(selectedFile);

	return ExitCode.OK;

    }
}
