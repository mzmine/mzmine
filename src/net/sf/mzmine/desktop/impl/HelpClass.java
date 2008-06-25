package net.sf.mzmine.desktop.impl;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;

import net.sf.mzmine.desktop.MZmineMenu;

public class HelpClass {

	private HelpBroker hb;
	
	private HelpSet hs;

	private ActionListener helpListener;
	
	public void addMenuItem(MainMenu menu) {

		try {
			
			File urlAddress = new File(System.getProperty("user.dir")
					+ File.separator + "help" + File.separator + "help.hs");

			URL url = urlAddress.toURI().toURL();
			hs = new HelpSet(null, url);
			hb = hs.createHelpBroker();
			helpListener = new CSH.DisplayHelpFromSource(hb);

			menu.addMenuItem(MZmineMenu.HELPSYSTEM, "Help Contents",
					"Help system contents", KeyEvent.VK_C, helpListener, null);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ActionListener getHelpListener() {
		return helpListener;
	}

	public HelpBroker getHelpBroker() {
		return hb;
	}

	public HelpSet getHelpSet() {
		return hs;
	}

}
