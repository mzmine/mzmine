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

package net.sf.mzmine.util;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;


import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


import net.sf.mzmine.methods.alignment.AlignmentResultExporterParameters;
import net.sf.mzmine.methods.alignment.fast.FastAlignerParameters;
import net.sf.mzmine.methods.alignment.filterbygaps.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.methods.deisotoping.combinatorial.CombinatorialDeisotoperParameters;
import net.sf.mzmine.methods.deisotoping.incompletefilter.IncompleteIsotopePatternFilterParameters;
import net.sf.mzmine.methods.deisotoping.simple.SimpleDeisotoperParameters;
import net.sf.mzmine.methods.filtering.chromatographicmedian.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.filtering.crop.CropFilterParameters;
import net.sf.mzmine.methods.filtering.mean.MeanFilterParameters;
import net.sf.mzmine.methods.filtering.savitzkygolay.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.filtering.zoomscan.ZoomScanFilterParameters;
import net.sf.mzmine.methods.gapfilling.simple.SimpleGapFillerParameters;
import net.sf.mzmine.methods.normalization.linear.LinearNormalizerParameters;
import net.sf.mzmine.methods.normalization.standardcompound.StandardCompoundNormalizerParameters;
import net.sf.mzmine.methods.peakpicking.centroid.CentroidPickerParameters;
import net.sf.mzmine.methods.peakpicking.local.LocalPickerParameters;
import net.sf.mzmine.methods.peakpicking.recursivethreshold.RecursiveThresholdPickerParameters;
import net.sf.mzmine.userinterface.dialogs.BatchModeDialogParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotViewParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotViewParameters;






public class ParameterStorage {


	// GENERAL PARAMETERS
	private GeneralParameters generalParameters;
	public GeneralParameters getGeneralParameters() { return generalParameters; }

	private AlignmentResultExporterParameters  alignmentResultExporterParameters;
	public AlignmentResultExporterParameters getAlignmentResultExporterParameters() { return alignmentResultExporterParameters; }


	// BATCH MODE PARAMETERES
	private BatchModeDialogParameters batchModeDialogParameters;
	public BatchModeDialogParameters getBatchModeDialogParameters() { return batchModeDialogParameters; }


	// FILTER PARAMETERS
	private MeanFilterParameters meanFilterParameters;
	public MeanFilterParameters getMeanFilterParameters() { return meanFilterParameters; }

	private SavitzkyGolayFilterParameters savitzkyGolayFilterParameters;
	public SavitzkyGolayFilterParameters getSavitzkyGolayFilterParameters() { return savitzkyGolayFilterParameters; }

	private ChromatographicMedianFilterParameters chromatographicMedianFilterParameters;
	public ChromatographicMedianFilterParameters getChromatographicMedianFilterParameters() { return chromatographicMedianFilterParameters; }

	private CropFilterParameters cropFilterParameters;
	public CropFilterParameters getCropFilterParameters() { return cropFilterParameters; }

	private ZoomScanFilterParameters zoomScanFilterParameters;
	public ZoomScanFilterParameters getZoomScanFilterParameters() { return zoomScanFilterParameters; }


	// PEAK PICKER PARAMETERS
	private LocalPickerParameters localPickerParameters;
	public LocalPickerParameters getLocalPickerParameters() { return localPickerParameters; }

	private RecursiveThresholdPickerParameters recursiveThresholdPickerParameters;
	public RecursiveThresholdPickerParameters getRecursiveThresholdPickerParameters() { return recursiveThresholdPickerParameters; }

	private CentroidPickerParameters centroidPickerParameters;
	public CentroidPickerParameters getCentroidPickerParameters() { return centroidPickerParameters; }

	private SimpleDeisotoperParameters simpleDeisotoperParameters;
	public SimpleDeisotoperParameters getSimpleDeisotoperParameters() { return simpleDeisotoperParameters; }

	private IncompleteIsotopePatternFilterParameters incompleteIsotopePatternFilterParameters;
	public IncompleteIsotopePatternFilterParameters getIncompleteIsotopePatternFilterParameters() { return incompleteIsotopePatternFilterParameters; }

	private CombinatorialDeisotoperParameters combinatorialDeisotoperParameters;
	public CombinatorialDeisotoperParameters getCombinatorialDeisotoperParameters() { return combinatorialDeisotoperParameters; }


	// ALIGNMENT PARAMETERS
	private JoinAlignerParameters joinAlignerParameters;
	public JoinAlignerParameters getJoinAlignerParameters() { return joinAlignerParameters; }

	private FastAlignerParameters fastAlignerParameters;
	public FastAlignerParameters getFastAlignerParameters() { return fastAlignerParameters; }


	// ALIGNMENT VISUALIZER PARAMETERS
	private AlignmentResultVisualizerCDAPlotViewParameters alignmentResultVisualizerCDAPlotViewParameters;
	public AlignmentResultVisualizerCDAPlotViewParameters getAlignmentResultVisualizerCDAPlotViewParameters() { return alignmentResultVisualizerCDAPlotViewParameters; }

	private AlignmentResultVisualizerSammonsPlotViewParameters alignmentResultVisualizerSammonsPlotViewParameters;
	public AlignmentResultVisualizerSammonsPlotViewParameters getAlignmentResultVisualizerSammonsPlotViewParameters() { return alignmentResultVisualizerSammonsPlotViewParameters; }


	// ALIGNMENT PROCESSOR PARAMETERS
	private AlignmentResultFilterByGapsParameters alignmentResultFilterByGapsParameters;
	public AlignmentResultFilterByGapsParameters getAlignmentResultFilterByGapsParameters() { return alignmentResultFilterByGapsParameters; }


	// GAP-FILLER PARAMETERS
	private SimpleGapFillerParameters simpleGapFillerParameters;
	public SimpleGapFillerParameters getSimpleGapFillerParameters() { return simpleGapFillerParameters; }


	// NORMALIZER PARAMETERS
	private LinearNormalizerParameters linearNormalizerParameters;
	public LinearNormalizerParameters getLinearNormalizerParameters() { return linearNormalizerParameters; }

	private StandardCompoundNormalizerParameters standardCompoundNormalizerParameters;
	public StandardCompoundNormalizerParameters getStandardCompoundNormalizerParameters() { return standardCompoundNormalizerParameters; }



	/**
	 * Constructor: Initializes all parameters with their default values
	 */
	public ParameterStorage() {

		generalParameters = new GeneralParameters();
		alignmentResultExporterParameters = new AlignmentResultExporterParameters();
		batchModeDialogParameters = new BatchModeDialogParameters();
		meanFilterParameters = new MeanFilterParameters();
		savitzkyGolayFilterParameters = new SavitzkyGolayFilterParameters();
		chromatographicMedianFilterParameters = new ChromatographicMedianFilterParameters();
		cropFilterParameters = new CropFilterParameters();
		zoomScanFilterParameters = new ZoomScanFilterParameters();
		localPickerParameters = new LocalPickerParameters();
		recursiveThresholdPickerParameters = new RecursiveThresholdPickerParameters();
		centroidPickerParameters = new CentroidPickerParameters();
		simpleDeisotoperParameters = new SimpleDeisotoperParameters();
		combinatorialDeisotoperParameters = new CombinatorialDeisotoperParameters();
		incompleteIsotopePatternFilterParameters = new IncompleteIsotopePatternFilterParameters();
		joinAlignerParameters = new JoinAlignerParameters();
		fastAlignerParameters = new FastAlignerParameters();
		alignmentResultVisualizerCDAPlotViewParameters = new AlignmentResultVisualizerCDAPlotViewParameters();
		alignmentResultVisualizerSammonsPlotViewParameters = new AlignmentResultVisualizerSammonsPlotViewParameters();
		alignmentResultFilterByGapsParameters = new AlignmentResultFilterByGapsParameters();
		simpleGapFillerParameters = new SimpleGapFillerParameters();
		linearNormalizerParameters = new LinearNormalizerParameters();
		standardCompoundNormalizerParameters = new StandardCompoundNormalizerParameters();

	}


	/**
	 * Reads all parameter settings from a file
	 * @param	paramFile	Parameter settings file
	 */
	public void readParameters(File paramFile) throws IOException {


		// Read XML file to a DOM document
		DocumentBuilder docBuilder;
		Document doc;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = docBuilder.parse(paramFile);
		}
		catch (ParserConfigurationException e) { throw new IOException(e.toString()); }
		catch (SAXException e) { throw new IOException(e.toString()); }

		// Let parameter objects fetch their values from document
		NodeList n = doc.getElementsByTagName("RawDataFilters");
		Element filtersParameters = (Element)(n.item(0));
		meanFilterParameters.readFromXML(filtersParameters);
		chromatographicMedianFilterParameters.readFromXML(filtersParameters);
		savitzkyGolayFilterParameters.readFromXML(filtersParameters);
		zoomScanFilterParameters.readFromXML(filtersParameters);


	}

	/**
	 * Writes all parameter values to file
	 * @param	paramFile	Parameter settings file
	 */
	public void writeParameters(File paramFile) throws IOException {
		FileWriter paramFileWriter;

		// Create new DOM document
		DocumentBuilder docBuilder;
		Document doc;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = docBuilder.newDocument();
		}
		catch (ParserConfigurationException e) { throw new IOException(e.toString()); }

		Element rootElement = doc.createElement("MZmineParameters");
		doc.appendChild(rootElement);

		// Ask parameter object to add their elements to the document
		// Raw data filters
		Element filtersParameters = doc.createElement("RawDataFilters");
		rootElement.appendChild(filtersParameters);
		filtersParameters.appendChild(meanFilterParameters.addToXML(doc));
		filtersParameters.appendChild(chromatographicMedianFilterParameters.addToXML(doc));
		filtersParameters.appendChild(savitzkyGolayFilterParameters.addToXML(doc));
		filtersParameters.appendChild(zoomScanFilterParameters.addToXML(doc));

	    // Write a DOM document to a file
        try {
            Source source = new DOMSource(doc);
            FileOutputStream ostream = new FileOutputStream(paramFile);
            Result result = new StreamResult(ostream);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
            ostream.close();
        } catch (TransformerConfigurationException e) {
			throw new IOException(e.toString());
        } catch (TransformerException e) {
			throw new IOException(e.toString());
        }

    }

}