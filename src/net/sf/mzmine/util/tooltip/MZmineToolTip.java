package net.sf.mzmine.util.tooltip;

import javax.swing.JToolTip;

public class MZmineToolTip extends JToolTip {
	  public MZmineToolTip() {
		    setUI(new MZmineToolTipUI());
		  }
}
