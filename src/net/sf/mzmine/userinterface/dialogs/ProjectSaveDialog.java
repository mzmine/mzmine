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

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import net.sf.mzmine.main.MZmineCore;
/**
 * File open dialog
 */
public class ProjectSaveDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private JFileChooser fileChooser;
    private boolean with_raw_file=false;
    
    public ProjectSaveDialog(String path) {

        super(MZmineCore.getDesktop().getMainFrame(), "Saving project...",
                true);
        
        logger.finest("Displaying project save dialog");
        
        fileChooser = new JFileChooser();
        if (path != null) fileChooser.setCurrentDirectory(new File(path));
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.addActionListener(this);
        
        //fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        add(fileChooser, BorderLayout.CENTER);
         
        JPanel panel=new JPanel();
        
        JRadioButton button_wo_rawFile = new JRadioButton("Not include raw data files");
        button_wo_rawFile.addActionListener(this);
        button_wo_rawFile.setActionCommand("WITHOUT_RAWFILE");
        button_wo_rawFile.setSelected(true);
        button_wo_rawFile.setEnabled(false);
        
        JRadioButton button_w_rawFile = new JRadioButton("Include raw data files");
        button_w_rawFile.addActionListener(this);
        button_w_rawFile.setActionCommand("WITH_RAWFILE"); 
        button_w_rawFile.setEnabled(false);
        
        ButtonGroup buttonGroup=new ButtonGroup();
        
        buttonGroup.add(button_wo_rawFile);
        buttonGroup.add(button_w_rawFile);
        panel.add(button_wo_rawFile,BorderLayout.NORTH);
        panel.add(button_w_rawFile,BorderLayout.SOUTH);
        
        add(panel,BorderLayout.SOUTH);
        
        pack();

        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        // check if user clicked "Open"
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {

            File selectedFile =new File( fileChooser.getSelectedFile().toString()+".mzm");
            
            try{
            	MZmineCore.getIOController().saveProject(selectedFile);
            }catch(IOException e){
            	logger.fine("Could not save project file."+e.getMessage());
            }
            dispose();
        }else if (command.equals("WITHOUT_RAWFILE")){
        	this.with_raw_file=true;
        }else if (command.equals("WITH_RAWFILE")){
        	this.with_raw_file=false;
        }
        	
        	
        // discard this dialog
        
    }
    
    public String getCurrentDirectory() {
        return fileChooser.getCurrentDirectory().toString();
    }

}
