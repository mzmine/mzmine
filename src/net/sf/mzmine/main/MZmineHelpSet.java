package net.sf.mzmine.main;

import javax.help.HelpSet;

public class MZmineHelpSet extends HelpSet {

	public MZmineHelpSet() {
		super();
	}
	
	public void addTOCView( MZmineTOCView TOCView){
		this.addView(TOCView);
	}

}
