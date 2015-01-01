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

package net.sf.mzmine.modules.visualization.threed;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;

import visad.TextControl;
import visad.VisADException;
import visad.util.TextControlWidget;

/**
 * A dialog that holds a VisAD TextControlWidget.
 *
 * @author $Author$
 * @version $Revision$
 */
public class TextControlDialog extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Label Font";

    /**
     * Create the dialog.
     *
     * @param owner
     *            parent window.
     * @param control
     *            the text control.
     * @throws VisADException
     *             if there are VisAD problems.
     * @throws RemoteException
     *             if there are VisAD problems.
     */
    public TextControlDialog(final Window owner, final TextControl control)
	    throws VisADException, RemoteException {

	super(owner, TITLE, ModalityType.APPLICATION_MODAL);

	// Layout dialog.
	final Container content = getContentPane();
	content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
	content.add(new TextControlWidget(control), Component.CENTER_ALIGNMENT);
	content.add(createDoneButton());
	pack();
    }

    /**
     * A button to close the dialog.
     *
     * @return the button.
     */
    private JButton createDoneButton() {

	final JButton button = new JButton("Done");
	button.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
		setVisible(false);
	    }
	});
	button.setAlignmentX(Component.CENTER_ALIGNMENT);
	return button;
    }

    @Override
    public void setVisible(final boolean visible) {
	if (visible) {
	    setLocationRelativeTo(getOwner());
	}
	super.setVisible(visible);
    }
}
