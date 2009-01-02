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

package net.sf.mzmine.desktop.helpsystem;

import java.awt.event.ActionEvent;

import javax.help.CSH;
import javax.help.HelpBroker;

public class HelpListener extends CSH.DisplayHelpFromSource{

	private HelpBroker hb;
	
	public HelpListener(HelpBroker hb) {
		super(hb);
		this.hb = hb;
	}

	public void actionPerformed(ActionEvent e) {
		hb.getHelpSet().setHomeID("net/sf/mzmine/desktop/helpsystem/AboutText.html");
		super.actionPerformed(e);
	}

}
