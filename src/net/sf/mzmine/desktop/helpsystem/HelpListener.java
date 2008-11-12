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
