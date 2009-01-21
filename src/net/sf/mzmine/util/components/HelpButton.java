/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.util.components;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.swing.JButton;

import net.sf.mzmine.desktop.helpsystem.HelpImp;
import net.sf.mzmine.desktop.helpsystem.MZmineHelpSet;
import net.sf.mzmine.main.MZmineCore;

/**
 * This class extends JButton class to implement Help system generated
 * programatically. This class permits to get the help system in a dialog modal
 * window, and assign the HelpSystem Action Listener to this button.
 * 
 * 
 */
public class HelpButton extends JButton {

	/**
	 * This constructor receives as parameter a help ID stored in HelpSet.
	 * 
	 * @param helpID
	 */
	public HelpButton(String helpID) {
		super("Help");
		try {
			
			HelpImp helpImp = MZmineCore.getHelpImp();
			
			if (helpImp == null){
				setVisible(false);
				return;
			}

			MZmineHelpSet hs = helpImp.getHelpSet();
			
			if (hs == null) {
				setEnabled(false);
				return;
			}
			
			HelpBroker hb = hs.createHelpBroker();
			hs.setHomeID(helpID);

			this.addActionListener(new CSH.DisplayHelpFromSource(hb));

		} catch (Exception event) {
			event.printStackTrace();
		}
	}

}
