/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.molstructure;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

public class Structure2DComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    private AtomContainerRenderer renderer;
    private IAtomContainer molecule;

    public Structure2DComponent(String structure) throws CDKException,
	    IOException {

	// Load the structure
	StringReader reader = new StringReader(structure);

	MDLReader molReader = new MDLReader(reader);

	/*
	 * ChemModel chemModel = new ChemModel(); chemModel = (ChemModel)
	 * molReader.read(chemModel);
	 */
	molecule = new AtomContainer();
	molReader.read(molecule);
	molReader.close();

	StructureDiagramGenerator sdg = new StructureDiagramGenerator();
	sdg.setMolecule(molecule);
	sdg.generateCoordinates();
	molecule = sdg.getMolecule();

	// generators make the image elements
	List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
	generators.add(new BasicSceneGenerator());
	generators.add(new BasicBondGenerator());
	generators.add(new BasicAtomGenerator());

	// the renderer needs to have a toolkit-specific font manager
	renderer = new AtomContainerRenderer(generators, new AWTFontManager());

    }

    @Override
    protected void paintComponent(Graphics g) {

	Graphics2D g2 = (Graphics2D) g;
	g2.setColor(Color.WHITE);
	g2.fillRect(0, 0, getWidth(), getHeight());

	final Rectangle drawArea = new Rectangle(getWidth(), getHeight());
	renderer.setup(molecule, drawArea);
	renderer.paint(molecule, new AWTDrawVisitor(g2));
    }

}
