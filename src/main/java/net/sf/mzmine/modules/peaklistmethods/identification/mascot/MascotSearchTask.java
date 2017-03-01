/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import java.util.logging.Logger;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class MascotSearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int finishedRows, totalRows;

    private PeakList pp;

    /**
     * private static int MIN_MSMS_LEVEL = 2; private MascotParameters
     * parameters; private String submisionString; private File tmpFile; private
     * String details = null; private static final String MGFIDENT="RowIdx";
     * 
     * 
     * The default constructor
     * 
     * @param parameters
     *            the search parameter
     * @param peakList
     *            the peak list
     * 
     *            public MascotSearchTask(ParameterSet parameters, PeakList
     *            peakList) {
     * 
     *            this.pp = peakList; this.parameters = (MascotParameters)
     *            parameters; }
     * 
     *            /** Create an mgf file from the given scan. The title has the
     *            following format: Row <RowID> (rt=<retention time>)
     *
     * @param writer
     *            the writer
     * @param rawFile
     *            the raw file
     * @param scan
     *            the scan
     * 
     *            public void writeMgf(PrintWriter writer, RawDataFile rawFile,
     *            Scan scan, Integer query) { if (scan.getMSLevel() <
     *            MIN_MSMS_LEVEL) { logger.warning("Scan " +
     *            scan.getScanNumber() +
     *            " is not a MS/MS scan."+scan.getMSLevel()); return; }
     *            writer.println(); writer.println("BEGIN IONS");
     *            writer.println("TITLE="+MGFIDENT+" " + query.toString() +
     *            " (scan="+scan.getScanNumber()+"rt=" + scan.getRetentionTime()
     *            + ")");
     * 
     *            writer.println("PEPMASS=" + scan.getPrecursorMZ()); if
     *            (scan.getRetentionTime() > 0) { writer.println("RTINSECONDS="
     *            + scan.getRetentionTime()); } if (scan.getPrecursorCharge() >
     *            0) { writer.println("CHARGE=" + scan.getPrecursorCharge() +
     *            "+"); } if (scan.getPrecursorCharge() < 0) {
     *            writer.println("CHARGE=" + scan.getPrecursorCharge() + "-"); }
     * 
     *            DataPoint[] dps = scan.getDataPoints();
     * 
     *            if (!scan.isCentroided()) { /*LocalMaxMassDetector detector =
     *            new LocalMaxMassDetector(); //ParameterSet params =
     *            detector.getParameterSet(); DataPoint[] peaks = null; //
     *            detector.getMassValues(scan, params); for (int i = 0; i <
     *            peaks.length; i++) { writer.println(peaks[i].getMZ() + "\t" +
     *            peaks[i].getIntensity()); } } else { for (int k = 0; k <
     *            dps.length; k++) { writer.println(dps[k].getMZ() + "\t" +
     *            dps[k].getIntensity()); } }
     * 
     *            writer.println("END IONS"); }
     * 
     *            /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (totalRows == 0)
	    return 0;
	return ((double) finishedRows) / totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "MS/MS identification of " + pp.getName();

    }

    /**
     * @see java.lang.Runnable#run()
     */
    /*
     * (non-Javadoc)
     * 
     * @see net.sf.mzmine.taskcontrol.Task#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);
	totalRows = 100;
	/*
	 * try { logger.info("Create temporary MGF file"); tmpFile =
	 * File.createTempFile("mascot", "mascot"); PrintWriter mgfWriter = new
	 * PrintWriter(tmpFile); for (int i = 0; i < pp.getRows().length; i++) {
	 * // Check if we are not canceled if ( isCanceled( )) return;
	 * PeakListRow row = pp.getRows()[i]; //TODO search for the peak with
	 * the best fragmentation not the most intense peak Scan msmsScan =
	 * row.getBestPeak().getDataFile().getScan(
	 * row.getBestPeak().getMostIntenseFragmentScanNumber()); if (msmsScan
	 * == null) continue; writeMgf(mgfWriter,
	 * row.getBestPeak().getDataFile(), msmsScan,i); mgfWriter.flush(); }
	 * mgfWriter.close(); mgfWriter = null;
	 * logger.info("Submit temporary MGF to Server"); submisionString =
	 * parameters.getSubmissionString(tmpFile, 0);
	 * 
	 * String boundery = parameters.getBoundery(); URL mascotSubmitUrl = new
	 * URL(parameters.getMascotSubmitUrlString()); URLConnection
	 * mascotConnection = mascotSubmitUrl.openConnection(); // We'll doth
	 * I/O and we don't want cached content.
	 * mascotConnection.setDoOutput(true);
	 * mascotConnection.setDoInput(true);
	 * mascotConnection.setUseCaches(false); // Specify the content type and
	 * cache-control.
	 * 
	 * mascotConnection.setRequestProperty("Content-Type",
	 * " multipart/form-data; boundary=" + boundery);
	 * mascotConnection.setRequestProperty("Cache-Control", " no-cache"); //
	 * Write the POST information. OutputStream serverOutput =
	 * mascotConnection.getOutputStream(); PrintWriter mascotSubmitWriter =
	 * new PrintWriter(new OutputStreamWriter(serverOutput));
	 * mascotSubmitWriter.print(submisionString);
	 * mascotSubmitWriter.flush(); // Retrieve the result.
	 * 
	 * if (!tmpFile.delete()) { tmpFile.deleteOnExit(); }
	 * 
	 * InputStream serverResponseStream = mascotConnection.getInputStream();
	 * BufferedReader responseReader = new BufferedReader(new
	 * InputStreamReader(serverResponseStream)); String line = null;
	 * StringBuffer buffer = new StringBuffer(""); String mascotXmlResponse
	 * = null; int startIndex, stopIndex;
	 * 
	 * while ((line = responseReader.readLine()) != null) { if ( isCanceled(
	 * )) return; // check for Mascot error messages; if
	 * (line.matches(".*M\\d\\d\\d\\d\\d]<BR>")) { errorMessage =
	 * line.replaceAll("<BR>", ""); } int outputHelper = 0; try { if
	 * ((outputHelper = line.indexOf("%")) > 0) { String temp =
	 * line.substring(outputHelper - 3, outputHelper + 1); while
	 * (temp.startsWith(".")) { temp = temp.substring(1); }
	 * 
	 * finishedRows = new Integer(temp.substring(0, temp
	 * .indexOf("%"))).intValue(); } } catch
	 * (StringIndexOutOfBoundsException e) { // e.printStackTrace(); //
	 * System.err.println(line); } catch (NumberFormatException ee) { //
	 * ignore that }
	 * 
	 * 
	 * String detect = "<A HREF=\""; int offset = detect.length(); if
	 * ((startIndex = line.indexOf(detect)) >= 0) { // <A //
	 * HREF="../cgi/master_results.pl?file=../data/20100601/F021799.dat">
	 * buffer.append(line); stopIndex = line.indexOf("\">");
	 * mascotXmlResponse = line.substring(startIndex + offset, stopIndex); }
	 * } serverResponseStream.close(); serverResponseStream = null;
	 * serverOutput.close(); serverOutput = null;
	 * 
	 * if (mascotXmlResponse == null) { logger.info(buffer.toString());
	 * setStatus( TaskStatus.ERROR ); return; }
	 * 
	 * String lDate = null; String lDatfileFilename = null; String detect =
	 * "../cgi/master_results.pl?file=../data/"; int offset =
	 * detect.length(); if ((startIndex = mascotXmlResponse.indexOf(detect))
	 * >= 0) { detect = "data/"; startIndex =
	 * mascotXmlResponse.indexOf(detect); offset = detect.length(); String
	 * [] temp = mascotXmlResponse.substring(startIndex +
	 * offset).split("/"); lDate = temp[0]; lDatfileFilename = temp[1];
	 * 
	 * } else{ logger.info(buffer.toString()); setStatus( TaskStatus.ERROR
	 * ); return; }
	 * 
	 * 
	 * MascotDatfile mdf = null; String resultString =
	 * parameters.getMascotInstallUrlString()
	 * +"x-cgi/ms-status.exe?Autorefresh=false&Show=RESULTFILE&DateDir=" +
	 * lDate + "&ResJob=" + lDatfileFilename; logger.info(resultString); try
	 * { URL lDatfileLocation = new URL(resultString); URLConnection
	 * lURLConnection = lDatfileLocation.openConnection(); BufferedReader br
	 * = new BufferedReader(new
	 * InputStreamReader(lURLConnection.getInputStream())); mdf = new
	 * MascotDatfile(br);
	 * 
	 * } catch(MalformedURLException e) { e.printStackTrace(); }
	 * catch(IOException ioe) { ioe.printStackTrace(); }
	 * 
	 * assert mdf != null;
	 * 
	 * QueryToPeptideMap queryPeptideMap = mdf.getQueryToPeptideMap(); int
	 * numberOfQueries = mdf.getNumberOfQueries();
	 * 
	 * for (int i = 1; i <= numberOfQueries; i++) { PeptideHit pepHit =
	 * queryPeptideMap.getPeptideHitOfOneQuery(i); if (pepHit != null){
	 * Query q = mdf.getQuery(i); String title = q.getTitle(); String[]
	 * tokens = title.split(" "); int rowId = Integer.parseInt(tokens[1]);
	 * MascotPeakIdentity mpid = new MascotPeakIdentity(pepHit);
	 * pp.getRows()[rowId].addPeakIdentity(mpid, true); } }
	 * 
	 * 
	 * 
	 * 
	 * } catch (Exception e) { logger.info(e.getMessage());
	 * e.printStackTrace(); setStatus( TaskStatus.ERROR ); return; }
	 */
	setStatus(TaskStatus.FINISHED);
	logger.info("Finished peaks search");
    }

}
