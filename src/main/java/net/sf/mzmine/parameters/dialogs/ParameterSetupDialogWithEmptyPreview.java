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

package net.sf.mzmine.parameters.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.sf.mzmine.parameters.ParameterSet;


/**
 * You can extend this class to create your own setup dialog containing a preview. All you need to
 * do is extend this, call super.addDialogComponents(); in the extention's addDialogComponents();
 * and then add your preview (e.g. a chart) to pnlPreview via the .add-method to
 * BorderLayout.CENTER. If you need extra buttons you can add them to pnlPreviewButtons, which is a
 * panel south of the preview and formatted by a flow layout.
 * 
 * Please note the mainPanel of ParameterSetupDialog is not the main panel anymore, but newMainPanel
 * is. The old mainPanel is moved inside a JScrollPane to allow users with low resolution to set the
 * parameters properly.
 * 
 * TODO: COPY actionPerformed METHOD TO YOUR SUBCLASS AND UNCOMMENT THE LINES TO SHOW PREVIEW DEFINE
 * cmpPreview AS WELL. THIS IS PRECODED FOR A CHECKBOX!
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ParameterSetupDialogWithEmptyPreview extends ParameterSetupDialog {

  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // panels
  protected JScrollPane pnScroll; // this will contain the parameter panel
  protected JPanel pnlPreview; // this will contain the preview and navigation panels
  protected JPanel pnlPreviewButtons; // this will contain the navigation buttons for the preview
  protected JPanel newMainPanel; // this will be the new main panel
  protected JPanel pnlParameters; // this will contain all parameters of the module (the main panel
  // will be inserted here)

  public ParameterSetupDialogWithEmptyPreview(Window parent, boolean valueCheckRequired,
      ParameterSet parameters) {
    super(parent, valueCheckRequired, parameters);
  }

  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();

    // initialize panels
    pnlPreview = new JPanel(new BorderLayout());
    pnlPreviewButtons = new JPanel(new FlowLayout());
    pnlParameters = new JPanel(new BorderLayout());
    newMainPanel = new JPanel(new BorderLayout());
    pnScroll = new JScrollPane();
    pnScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);


    // reorganize panels
    getContentPane().remove(mainPanel);
    mainPanel.remove(super.pnlButtons);
    pnScroll.setViewportView(mainPanel);
    mainPanel.setMinimumSize(new Dimension(400, 400));
    pnlParameters.add(pnScroll, BorderLayout.CENTER);
    pnlParameters.add(super.pnlButtons, BorderLayout.SOUTH);
    pnlPreview.add(pnlPreviewButtons, BorderLayout.SOUTH);
    newMainPanel.add(pnlParameters, BorderLayout.WEST);
    newMainPanel.add(pnlPreview, BorderLayout.CENTER);
    getContentPane().add(newMainPanel, BorderLayout.CENTER);

    pnlPreview.setVisible(false);

    // later add your preview via pnlPreview.add(YOUR_PANEL, BorderLayout.CENTER);
    // and your buttons to control the preview via pnlPreviewButtons.add(YOUR_BUTTON);
    updateMinimumSize();
    pack();
  }

  // TODO: COPY THIS METHOD TO YOUR SUBCLASS AND UNCOMMENT THE LINES TO SHOW PREVIEW
  // DEFINE cmpPreview AS WELL. THIS IS PRECODED FOR A CHECKBOX!
  @Override
  public void actionPerformed(ActionEvent ae) {
    super.actionPerformed(ae);
    /*
     * if (ae.getSource() == cmpPreview) { logger.info(ae.getSource().toString());
     * 
     * if (cmpPreview.isSelected()) { 
     *   newMainPanel.add(pnlPreview, BorderLayout.CENTER);
     *   pnlPreview.setVisible(true); 
     *   updateMinimumSize(); pack(); 
     * } 
     * else {
     *   newMainPanel.remove(pnlPreview); pnlPreview.setVisible(false); updateMinimumSize(); pack(); 
     * }
     */
  }
}
