/*
    Copyright 2005-2006 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.userinterface.dialogs;

import javax.swing.JDialog;




/**
 * This class represents the About dialog box.
 *
 * @version	30 March 2006
 */
public class AboutDialog extends JDialog {

	// GUI elements
	private javax.swing.JButton jButton1;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JScrollPane textScroll;

	// Contents of the dialog, this string is available (in editable format) in file AboutText.html
	private String contentString = "<html> <center> <h1>MZmine</h1> <h3>Version 0.60</h3> <p> Software written and designed by<br> Mikko Katajamaa (Turku Centre for Biotechnology)<br> Jarkko Miettinen (VTT Biotechnology)<br> Matej Ore&#353;i&#269; (VTT Biotechnology) </p> <p> Please send questions and comments to <a href=\"mailto:mzmine@btk.fi\">mzmine@btk.fi</a>. </p> </center> <br> <h3>Copyright and license information for MZmine</h3> <p> Copyright (c) 2005 VTT Biotechnology. </p> <p> This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version. </p> <p> This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. </p> <p> You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. </p> <br> <h3>Acknowledgements</h3> <p>MZmine uses some pieces of third-party code. We are thankful to the authors for their code.</p> <ul> <li><i>NetCDF for Java library</i> by Unidata Community (GNU Lesser General Public License) <li><i>Danby Package for probability and statistics</i> by Charles S. Stanton <li><i>ExampleFileFilter.java, TableMap.java, TableSorter.java</i> by Sun Microsystems, Inc. (license below) <li><i>Base64.java</i> by Robert Harder (public domain) <li>Code for mzXML file format support is based on <i>JRAP</i> by SASHIMI-project (GNU General Public License) </ul> </p> <br> <h3>Copyright and license information for ExampleFileFilter.java, TableMap.java, TableSorter.java</h3> <p>Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.</p> <p> Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: <ul> <li>Redistribution of source code must retain the above copyright notice, this list of conditions and the following disclaimer. <li>Redistribution in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. </ul> </p> <p> Neither the name of Sun Microsystems, Inc. or the names of contributors may be used to endorse or promote products derived from this software without specific prior written permission. </p> <p> This software is provided \"AS IS,\" without a warranty of any kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. (\"SUN\") AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. </p> <p> You acknowledge that this software is not designed, licensed or intended for use in the design, construction, operation or maintenance of any nuclear facility. </p> </body> </html>";


    /**
     * Creates new form AboutDialog
     */
    public AboutDialog() {
        initComponents();
        setBounds(0,0, 600, 300);
    }


    /**
     * Initializes the components of the dialog
     */
    private void initComponents() {
        jButton1 = new javax.swing.JButton();
        jEditorPane1 = new javax.swing.JEditorPane();
        textScroll = new javax.swing.JScrollPane();


        setTitle("About MZmine");
        jButton1.setText("OK!");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        getContentPane().add(jButton1, java.awt.BorderLayout.SOUTH);

        jEditorPane1.setBorder(new javax.swing.border.MatteBorder(new java.awt.Insets(0, 10, 10, 10), new java.awt.Color(255,255,255)));
        jEditorPane1.setContentType("text/html");
        jEditorPane1.setText(contentString);
        jEditorPane1.setEditable(false);

		jEditorPane1.setCaretPosition(0);
		textScroll.setViewportView(jEditorPane1);

        getContentPane().add(textScroll, java.awt.BorderLayout.CENTER);

        pack();

    }

	/**
	 * This method is called by to OK button's action listener when button is clicked
	 */
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }


}
