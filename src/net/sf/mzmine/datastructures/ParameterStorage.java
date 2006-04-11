/*
    Copyright 2005 VTT Biotechnology

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

package net.sf.mzmine.datastructures;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.methods.alignment.AlignmentResultExporterParameters;
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.FastAlignerParameters;
import net.sf.mzmine.methods.alignment.JoinAlignerParameters;
import net.sf.mzmine.methods.alignment.LinearNormalizerParameters;
import net.sf.mzmine.methods.alignment.SimpleGapFillerParameters;
import net.sf.mzmine.methods.alignment.StandardCompoundNormalizerParameters;
import net.sf.mzmine.methods.peakpicking.CentroidPickerParameters;
import net.sf.mzmine.methods.peakpicking.CombinatorialDeisotoperParameters;
import net.sf.mzmine.methods.peakpicking.IncompleteIsotopePatternFilterParameters;
import net.sf.mzmine.methods.peakpicking.LocalPickerParameters;
import net.sf.mzmine.methods.peakpicking.RecursiveThresholdPickerParameters;
import net.sf.mzmine.methods.peakpicking.SimpleDeisotoperParameters;
import net.sf.mzmine.methods.rawdata.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.rawdata.CropFilterParameters;
import net.sf.mzmine.methods.rawdata.MeanFilterParameters;
import net.sf.mzmine.methods.rawdata.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.rawdata.ZoomScanFilterParameters;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotViewParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotViewParameters;

import java.io.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ParameterStorage {


	// GENERAL PARAMETERS
	private GeneralParameters generalParameters;
	public GeneralParameters getGeneralParameters() { return generalParameters; }
	public void setGeneralParameters(GeneralParameters param) { generalParameters = param; }

	private AlignmentResultExporterParameters  alignmentResultExporterParameters;
	public AlignmentResultExporterParameters getAlignmentResultExporterParameters() { return alignmentResultExporterParameters; }
	public void setAlignmentResultExporterParameters(AlignmentResultExporterParameters param) { alignmentResultExporterParameters = param; }


	// BATCH MODE PARAMETERES
	private BatchModeDialogParameters batchModeDialogParameters;
	public BatchModeDialogParameters getBatchModeDialogParameters() { return batchModeDialogParameters; }
	public void setBatchModeDialogParameters(BatchModeDialogParameters param) { batchModeDialogParameters = param; }


	// FILTER PARAMETERS
	private MeanFilterParameters meanFilterParameters;
	public MeanFilterParameters getMeanFilterParameters() { return meanFilterParameters; }
	public void setMeanFilterParameters(MeanFilterParameters param) { meanFilterParameters = param; }

	private SavitzkyGolayFilterParameters savitzkyGolayFilterParameters;
	public SavitzkyGolayFilterParameters getSavitzkyGolayFilterParameters() { return savitzkyGolayFilterParameters; }
	public void setSavitzkyGolayFilterParameters(SavitzkyGolayFilterParameters param) { savitzkyGolayFilterParameters = param; }

	private ChromatographicMedianFilterParameters chromatographicMedianFilterParameters;
	public ChromatographicMedianFilterParameters getChromatographicMedianFilterParameters() { return chromatographicMedianFilterParameters; }
	public void setChromatographicMedianFilterParameters(ChromatographicMedianFilterParameters param) { chromatographicMedianFilterParameters = param; }

	private CropFilterParameters cropFilterParameters;
	public CropFilterParameters getCropFilterParameters() { return cropFilterParameters; }
	public void setCropFilterParameters(CropFilterParameters param) { cropFilterParameters = param; }

	private ZoomScanFilterParameters zoomScanFilterParameters;
	public ZoomScanFilterParameters getZoomScanFilterParameters() { return zoomScanFilterParameters; }
	public void setZoomScanFilterParameters(ZoomScanFilterParameters param) { zoomScanFilterParameters = param; }


	// PEAK PICKER PARAMETERS
	private LocalPickerParameters localPickerParameters;
	public LocalPickerParameters getLocalPickerParameters() { return localPickerParameters; }
	public void setLocalPickerParameters(LocalPickerParameters param) { localPickerParameters = param; }

	private RecursiveThresholdPickerParameters recursiveThresholdPickerParameters;
	public RecursiveThresholdPickerParameters getRecursiveThresholdPickerParameters() { return recursiveThresholdPickerParameters; }
	public void setRecursiveThresholdPickerParameters(RecursiveThresholdPickerParameters param) { recursiveThresholdPickerParameters = param; }

	private CentroidPickerParameters centroidPickerParameters;
	public CentroidPickerParameters getCentroidPickerParameters() { return centroidPickerParameters; }
	public void setCentroidPickerParameters(CentroidPickerParameters param) { centroidPickerParameters = param; }

	private SimpleDeisotoperParameters simpleDeisotoperParameters;
	public SimpleDeisotoperParameters getSimpleDeisotoperParameters() { return simpleDeisotoperParameters; }
	public void setSimpleDeisotoperParameters(SimpleDeisotoperParameters param) { simpleDeisotoperParameters = param; }

	private IncompleteIsotopePatternFilterParameters incompleteIsotopePatternFilterParameters;
	public IncompleteIsotopePatternFilterParameters getIncompleteIsotopePatternFilterParameters() { return incompleteIsotopePatternFilterParameters; }
	public void setIncompleteIsotopePatternFilterParameters(IncompleteIsotopePatternFilterParameters param) { incompleteIsotopePatternFilterParameters = param; }

	private CombinatorialDeisotoperParameters combinatorialDeisotoperParameters;
	public CombinatorialDeisotoperParameters getCombinatorialDeisotoperParameters() { return combinatorialDeisotoperParameters; }
	public void setCombinatorialDeisotoperParameters(CombinatorialDeisotoperParameters param) { combinatorialDeisotoperParameters = param; }


	// ALIGNMENT PARAMETERS
	private JoinAlignerParameters joinAlignerParameters;
	public JoinAlignerParameters getJoinAlignerParameters() { return joinAlignerParameters; }
	public void setJoinAlignerParameters(JoinAlignerParameters param) { joinAlignerParameters = param; }

	private FastAlignerParameters fastAlignerParameters;
	public FastAlignerParameters getFastAlignerParameters() { return fastAlignerParameters; }
	public void setFastAlignerParameters(FastAlignerParameters param) { fastAlignerParameters = param; }

	// ALIGNMENT VISUALIZER PARAMETERS
	private AlignmentResultVisualizerCDAPlotViewParameters alignmentResultVisualizerCDAPlotViewParameters;
	public AlignmentResultVisualizerCDAPlotViewParameters getAlignmentResultVisualizerCDAPlotViewParameters() { return alignmentResultVisualizerCDAPlotViewParameters; }
	public void setAlignmentResultVisualizerCDAPlotViewParameters(AlignmentResultVisualizerCDAPlotViewParameters param) { alignmentResultVisualizerCDAPlotViewParameters = param; }

	private AlignmentResultVisualizerSammonsPlotViewParameters alignmentResultVisualizerSammonsPlotViewParameters;
	public AlignmentResultVisualizerSammonsPlotViewParameters getAlignmentResultVisualizerSammonsPlotViewParameters() { return alignmentResultVisualizerSammonsPlotViewParameters; }
	public void setAlignmentResultVisualizerSammonsPlotViewParameters(AlignmentResultVisualizerSammonsPlotViewParameters param) { alignmentResultVisualizerSammonsPlotViewParameters = param; }

	// ALIGNMENT PROCESSOR PARAMETERS
	private AlignmentResultFilterByGapsParameters alignmentResultFilterByGapsParameters;
	public AlignmentResultFilterByGapsParameters getAlignmentResultFilterByGapsParameters() { return alignmentResultFilterByGapsParameters; }
	public void setAlignmentResultFilterByGapsParameters(AlignmentResultFilterByGapsParameters param) { alignmentResultFilterByGapsParameters = param; }


	// GAP-FILLER PARAMETERS
	private SimpleGapFillerParameters simpleGapFillerParameters;
	public SimpleGapFillerParameters getSimpleGapFillerParameters() { return simpleGapFillerParameters; }
	public void setSimpleGapFillerParameters(SimpleGapFillerParameters param) { simpleGapFillerParameters = param; }


	// NORMALIZER PARAMETERS
	private LinearNormalizerParameters linearNormalizerParameters;
	public LinearNormalizerParameters getLinearNormalizerParameters() { return linearNormalizerParameters; }
	public void setLinearNormalizerParameters(LinearNormalizerParameters param) { linearNormalizerParameters = param; }

	private StandardCompoundNormalizerParameters standardCompoundNormalizerParameters;
	public StandardCompoundNormalizerParameters getStandardCompoundNormalizerParameters() { return standardCompoundNormalizerParameters; }
	public void setStandardCompoundNormalizerParameters(StandardCompoundNormalizerParameters param) { standardCompoundNormalizerParameters = param; }




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


	public boolean readParametesFromFile(File paramFile) {

		if (!(paramFile.exists())) { return false; }

		ParameterStorageXMLReader xmlHandler = new ParameterStorageXMLReader(this);


		// Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {

            // Parse the file
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse( paramFile, xmlHandler);

        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }

        return true;

	}

	public boolean writeParametersToFile(File paramFile) {
		FileWriter paramFileWriter;

		// Open file
		if (paramFile.exists()) { if(!(paramFile.delete())) { return false; } }

		try {
			if (!(paramFile.createNewFile())) { return false; }
		} catch (Exception e) {
			Logger.put("Could not create parameters file " + paramFile + "for writing.");
			Logger.put(e.toString());
			return false;
		}

		try {
			paramFileWriter = new FileWriter(paramFile);
		} catch (Exception e) {
			Logger.put("Could not open parameters file " + paramFile + "for writing.");
			Logger.put(e.toString());
			return false;
		}

		// Write header
		String s;
		try {
			s = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
			paramFileWriter.write(s);
			s = "<MZmineParameters>\n";
			paramFileWriter.write(s);
		} catch (Exception e) {
			Logger.put("Could not write to parameters file " + paramFile + ".");
			Logger.put(e.toString());
			return false;
		}


		// Write parameters
		try {
			s = generalParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = alignmentResultExporterParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = batchModeDialogParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = meanFilterParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = savitzkyGolayFilterParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = chromatographicMedianFilterParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = cropFilterParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = localPickerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = recursiveThresholdPickerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = centroidPickerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = simpleDeisotoperParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = combinatorialDeisotoperParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = incompleteIsotopePatternFilterParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = joinAlignerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = fastAlignerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = alignmentResultVisualizerCDAPlotViewParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = alignmentResultVisualizerSammonsPlotViewParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = alignmentResultFilterByGapsParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = simpleGapFillerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = linearNormalizerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

			s = standardCompoundNormalizerParameters.writeParameterTag();
			paramFileWriter.write("\t" + s + "\n");

		} catch (Exception e) {
			Logger.put("Could not write to parameters file " + paramFile + ".");
			Logger.put(e.toString());
			return false;
		}

		// Write footer
		try {
			s = "</MZmineParameters>\n";
			paramFileWriter.write(s);
		} catch (Exception e) {
			Logger.put("Could not write to parameters file " + paramFile + ".");
			Logger.put(e.toString());
			return false;
		}


		// Close file
		try {
			paramFileWriter.close();
		} catch (Exception e) {
			Logger.put("Could not write to parameters file " + paramFile + ".");
			Logger.put(e.toString());
			return false;
		}


		return true;

	}


}