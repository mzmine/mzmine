/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;

/**
 * File open dialog
 */
public class ProjectOpenDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private JFileChooser fileChooser;
    private File projectDir;
    public enum Status{
    	Processing,Finished,Canceled,Error;
    }
    private Status status;
    
    public ProjectOpenDialog(String path) {

        super(MZmineCore.getDesktop().getMainFrame(), "Please select a project Directory to open..." ,
                true);
        
        logger.finest("Displaying project open dialog");
        status = Status.Processing;
        
        fileChooser = new JFileChooser();
        if (path != null){
        	fileChooser.setCurrentDirectory(new File(path));
        }
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.addActionListener(this);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        add(fileChooser, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        // check if user clicked "Open"
 
        if (command.equals("ApproveSelection")) {

            File selectedDir = fileChooser.getSelectedFile();
            try{
            	MZmineCore.getIOController().openProject(selectedDir);
            	status = Status.Finished;
            }catch(Throwable e){
            	JOptionPane.showMessageDialog(
            			this,
            			"Could not open project dir",
            			"Project opening error",
            			JOptionPane.ERROR_MESSAGE);
            	logger.fine("Could not open project file."+e.getMessage());
            	status = Status.Error;
            }
        }else{
        	status = Status.Canceled;
        }
        // discard this dialog
        dispose();
    }
    
    public String getCurrentDirectory(){
    	return this.projectDir.toString();
    }
    public Status getStatus(){
    	return status;
    }
}
