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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class represents the About dialog box.
 * 
 */
public class AboutDialog extends JDialog implements ActionListener {

    // Contents of the dialog, this string is available (in editable format) in
    // file AboutText.html
    private String contentString = "<html> <center> <h1>MZmine</h1> <h3>Version 0.xx</h3> <p> Copyright (c) 2005-2007 MZmine Development Team </p> </center> <p> Please use <a href=\"https://sourceforge.net/forum/?group_id=139835\">forums</a>  at Sourceforge.net to contact the MZmine development team. </p> <p> Contributors (order a-z): <ul> <li>Jarkko Miettinen (VTT Biotechnology) <li>Matej Ore&#353;i&#269; (VTT Biotechnology) <li>Mikko Katajamaa (Turku Centre for Biotechnology) <li>Tom&aacute;&#353; Pluskal (Okinawa Institute of Science and Technology) </ul> </p> <br> <h3>License information for MZmine</h3> <p> This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version. </p> <p> This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. </p> <p> You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. </p> <br> <h3>Acknowledgements</h3> <p>MZmine uses some pieces of third-party code. We are thankful to the authors for their code.</p> <ul> <li>Base64.java by Robert Harder <li>Chemistry Development Kit <li>Danby Package for probability and statistics by Charles S. Stanton	 <li>dom4j XML framework <li>ExampleFileFilter.java, TableMap.java, TableSorter.java by Sun Microsystems, Inc.	 <li>Jaxen <li>JFreeChart <li>NetCDF for Java library by Unidata Community <li>Swing Desktop extensions by Guy Davis <li>VisAD <li>Code for mzXML file format support is based on <i>JRAP</i> by SASHIMI-project </ul> </p> <br> <h3>Copyright and license information for ExampleFileFilter.java, TableMap.java, TableSorter.java</h3> <p>Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.</p> <p> Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: <ul> <li>Redistribution of source code must retain the above copyright notice, this list of conditions and the following disclaimer. <li>Redistribution in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. </ul> </p> <p> Neither the name of Sun Microsystems, Inc. or the names of contributors may be used to endorse or promote products derived from this software without specific prior written permission. </p> <p> This software is provided \"AS IS,\" without a warranty of any kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. (\"SUN\") AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. </p> <p> You acknowledge that this software is not designed, licensed or intended for use in the design, construction, operation or maintenance of any nuclear facility. </p> </body> </html>";

    /**
     * Creates new form AboutDialog
     */
    public AboutDialog(Desktop desktop) {

        super(desktop.getMainFrame(), "About MZmine", true);

        JPanel buttonPanel = new JPanel();
        GUIUtils.addButtonInPanel(buttonPanel, "OK!", this);
        add(buttonPanel, BorderLayout.SOUTH);

        JEditorPane textPane = new JEditorPane();
        textPane.setBorder(new MatteBorder(new Insets(0, 10, 10, 10),
                Color.white));
        textPane.setContentType("text/html");
        textPane.setText(contentString);
        textPane.setEditable(false);
        textPane.setCaretPosition(0);

        JScrollPane textScroll = new JScrollPane();
        textScroll.setViewportView(textPane);
        add(textScroll, BorderLayout.CENTER);

        pack();
        setBounds(0, 0, 600, 300);
        setLocationRelativeTo(desktop.getMainFrame());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        dispose();
    }

}
