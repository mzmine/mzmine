package net.sf.mzmine.main;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import net.sf.mzmine.desktop.Desktop;

public class NewVersionCheck implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public void run(String info) {
    	// Check for updated version
		String currentVersion = "", newestVersion = "", msg = "";
		currentVersion = MZmineCore.getMZmineVersion();
		
		try {
			URL url = new URL("http://mzmine.sourceforge.net/version.txt");
			// Open the stream and put it into BufferedReader
			BufferedReader buffer = new BufferedReader(new InputStreamReader(url.openStream()));

			newestVersion = buffer.readLine();
			buffer.close();
		} catch (Exception e) {
			newestVersion = "0";
		}

		if (newestVersion == "0") {
			if (info == "menu") {
				msg = "An error occured. Please make sure that you are connected to the internet or try again later.";
				logger.info(msg);
				MZmineCore.getDesktop().displayMessage(msg);
			}
		}
		else if (currentVersion == newestVersion || currentVersion == "0.0") {
			if (info == "menu") {
				msg = "No updated version of MZmine is available";
				logger.info(msg);
				MZmineCore.getDesktop().displayMessage(msg);
			}
		}
		else {
			msg = "An updated version is available: MZmine "+newestVersion;
			logger.info(msg);
			if (info == "menu") {
				MZmineCore.getDesktop().displayMessage(msg +"\nPlease download the newest version from: http://mzmine.sourceforge.net");
			}
			else if (info == "desktop") {
				Desktop desktop = MZmineCore.getDesktop();
				desktop.setStatusBarText(msg +". Please download the newest version from: http://mzmine.sourceforge.net", Color.red);
			}
		}
	}

	@Override
	public void run() {
	}
}
