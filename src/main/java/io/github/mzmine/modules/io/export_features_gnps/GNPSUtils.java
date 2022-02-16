/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_features_gnps;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnSubmitParameters;
import io.github.mzmine.modules.io.export_features_gnps.gc.GnpsGcSubmitParameters;
import io.github.mzmine.modules.io.export_features_gnps.masst.MasstDatabase;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to submit GNPS feature based molecular networking jobs directly
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GNPSUtils {

  public static final String FBMN_SUBMIT_SITE = "https://gnps-quickstart.ucsd.edu/uploadanalyzefeaturenetworking";
  public static final String MASST_SUBMIT_URL = "https://masst.ucsd.edu/submit";
  public static final String GC_SUBMIT_SITE = "https://gnps-quickstart.ucsd.edu/uploadanalyzegcnetworking";
  // Logger.
  private static final Logger logger = Logger.getLogger(GNPSUtils.class.getName());

  /**
   * Submit feature-based molecular networking (FBMN) job to GNPS
   *
   * @param file  base file name to find mgf (file) and csv (file_quant.csv)
   * @param param submission parameters
   * @return
   */
  public static String submitFbmnJob(File file, GnpsFbmnSubmitParameters param) throws IOException {
    // optional
    boolean useMeta = param.getParameter(GnpsFbmnSubmitParameters.META_FILE).getValue();
    boolean openWebsite = param.getParameter(GnpsFbmnSubmitParameters.OPEN_WEBSITE).getValue();
    String presets = param.getParameter(GnpsFbmnSubmitParameters.PRESETS).getValue().toString();
    boolean exportIIN = param.getParameter(GnpsFbmnSubmitParameters.EXPORT_ION_IDENTITY_NETWORKS)
        .getValue();
    String title = param.getParameter(GnpsFbmnSubmitParameters.JOB_TITLE).getValue();
    String email = param.getParameter(GnpsFbmnSubmitParameters.EMAIL).getValue();
    String username = param.getParameter(GnpsFbmnSubmitParameters.USER).getValue();
    String password = param.getParameter(GnpsFbmnSubmitParameters.PASSWORD).getValue();
    //
    File folder = file.getParentFile();
    String name = file.getName();
    // all file paths
    File mgf = FileAndPathUtil.getRealFilePath(folder, name, "mgf");
    File quan = FileAndPathUtil.getRealFilePath(folder, name + "_quant", "csv");

    // NEEDED files
    if (mgf.exists() && quan.exists()) {
      File meta = !useMeta ? null
          : param.getParameter(GnpsFbmnSubmitParameters.META_FILE).getEmbeddedParameter()
              .getValue();
      File iinEdges = !exportIIN ? null
          : FileAndPathUtil.getRealFilePath(folder, name + "_edges_msannotation", "csv");

      return submitFbmnJob(mgf, quan, meta, new File[]{iinEdges}, title, email, username, password,
          presets, openWebsite);
    } else {
      return "";
    }
  }

  /**
   * Submit feature-based molecular networking (FBMN) job to GNPS
   */
  public static String submitFbmnJob(@NotNull File mgf, @NotNull File quan, @Nullable File meta,
      @Nullable File[] additionalEdges, String title, String email, String username,
      String password, String presets, boolean openWebsite) throws IOException {
    // NEEDED files
    if (mgf.exists() && quan.exists() && !presets.isEmpty()) {
      try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
        MultipartEntity entity = new MultipartEntity();

        // ######################################################
        // NEEDED
        // tool, presets, quant table, mgf
        entity.addPart("featuretool", new StringBody("MZMINE2"));
        entity.addPart("description", new StringBody(title == null ? "" : title));
        entity.addPart("networkingpreset", new StringBody(presets));
        entity.addPart("featurequantification", new FileBody(quan));
        entity.addPart("featurems2", new FileBody(mgf));

        // ######################################################
        // OPTIONAL
        // email, meta data, additional edges
        entity.addPart("email", new StringBody(email));
        entity.addPart("username", new StringBody(username));
        entity.addPart("password", new StringBody(password));
        if (meta != null && meta.exists()) {
          entity.addPart("samplemetadata", new FileBody(meta));
        }

        // add additional edges
        if (additionalEdges != null) {
          for (File edge : additionalEdges) {
            if (edge != null && edge.exists()) {
              entity.addPart("additionalpairs", new FileBody(edge));
            }
          }
        }

        HttpPost httppost = new HttpPost(FBMN_SUBMIT_SITE);
        httppost.setEntity(entity);

        logger.info("Submitting GNPS job " + httppost.getRequestLine());
        try (CloseableHttpResponse response = httpclient.execute(httppost)) {
          logger.info("GNPS submit response status: " + response.getStatusLine());
          HttpEntity resEntity = response.getEntity();
          if (resEntity != null) {
            final String requestResult = EntityUtils.toString(resEntity);
            logger.info("GNPS FBMN/IIMN response: " + requestResult);

            // open job website
            if (openWebsite) {
              openWebsite(requestResult);
            }
            EntityUtils.consume(resEntity);
          }
        }
      }
    }
    return "";
  }


  public static boolean submitMASSTJob(DataPoint[] data, double precursorMZ, MasstDatabase database,
      double minCosine, double parentMzTol, double fragmentMzTol, int minMatchedSignals,
      boolean searchAnalogs, boolean openWebsite) throws IOException {
    return submitMASSTJob("MZmine3 MASST submission", data, precursorMZ, database, minCosine,
        parentMzTol, fragmentMzTol, minMatchedSignals, searchAnalogs, "", "", "", openWebsite);
  }

  /**
   * Submit a masst search on GNPS agianst public datasets. Single spectrum against all public
   * data.
   *
   * @param description       job description
   * @param data              the actual spectral data points
   * @param precursorMZ       the searched precursor m/z
   * @param database          the repositories to search in
   * @param minCosine         minimum cosine similarity score
   * @param parentMzTol       parent/precursor m/z tolerance
   * @param fragmentMzTol     fragment m/z tolerance
   * @param minMatchedSignals minimum number of matched signals
   * @param searchAnalogs     search analog compounds by precursor m/z shift
   * @param email             optional email
   * @param username          optional username
   * @param password          optional password
   * @param openWebsite       open the website if successfull
   * @return true if the request was successful
   * @throws IOException
   */
  public static boolean submitMASSTJob(String description, DataPoint[] data, double precursorMZ,
      MasstDatabase database, double minCosine, double parentMzTol, double fragmentMzTol,
      int minMatchedSignals, boolean searchAnalogs, String email, String username, String password,
      boolean openWebsite) throws IOException {
    if (data.length < minMatchedSignals) {
      logger.warning(() -> String.format(
          "Cannot MASST search when spectrum contains less data points (%d) than minimum matched signals (%d).",
          data.length, minMatchedSignals));
      return false;
    }
    if (precursorMZ <= 1) {
      logger.warning("Cannot MASST search with this precursor m/z=" + precursorMZ);
      return false;
    }

    final String data_tab_sep = Arrays.stream(data).map(d -> d.getMZ() + "\t" + d.getIntensity())
        .collect(Collectors.joining("\n"));

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(MASST_SUBMIT_URL);

      List<NameValuePair> params = new ArrayList<>();
      if ("MZMINE_TEST_SUBMISSION_ADD_TEST_PART".equals(description)) {
        // only do test submission
        params.add(new BasicNameValuePair("test", ""));
      }
      params.add(new BasicNameValuePair("peaks", data_tab_sep));
      params.add(new BasicNameValuePair("precursormz", "" + precursorMZ));
      params.add(new BasicNameValuePair("pmtolerance", "" + parentMzTol));
      params.add(new BasicNameValuePair("fragmenttolerance", "" + fragmentMzTol));
      params.add(new BasicNameValuePair("cosinescore", "" + minCosine));
      params.add(new BasicNameValuePair("matchedpeaks", "" + minMatchedSignals));
      params.add(new BasicNameValuePair("email", "" + email));
      params.add(new BasicNameValuePair("login", "" + username));
      params.add(new BasicNameValuePair("password", "" + password));
      params.add(new BasicNameValuePair("description", description));
      params.add(new BasicNameValuePair("database", database.getGnpsValue()));
      params.add(new BasicNameValuePair("analogsearch", searchAnalogs ? "Yes" : "No"));
      httpPost.setEntity(new UrlEncodedFormEntity(params));

      logger.info("Submitting GNPS job " + httpPost.getRequestLine());

      try (CloseableHttpResponse response = client.execute(httpPost)) {
        logger.info("GNPS submit response status: " + response.getStatusLine());
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
          final String requestResult = EntityUtils.toString(resEntity);
          logger.info("GNPS MASST response: " + requestResult);

          // all 200s are success
          if (response.getStatusLine().getStatusCode() / 200 == 1) {
            // open job website
            if (openWebsite) {
              openWebsite(requestResult);
            }
            EntityUtils.consume(resEntity);
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Open website with GNPS job
   */
  private static void openWebsite(String requestResult) {
    if (Desktop.isDesktopSupported()) {
      String url = "";
      try {
        JSONObject res = new JSONObject(requestResult);
        url = res.getString("url");
        logger.info("Response: " + res);

      } catch (ParseException | JSONException e) {
        // try reading the link from text - maybe type was plain text
        // parse from html response link, e.g.:
        // <a href="https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=theTaskID">
        url = StringUtils.substringBetween(requestResult, "<a href=\"", "\">");

        if (url == null || url.isBlank()) {
          logger.log(Level.SEVERE,
              "Error while submitting GNPS job, cannot read response URL as json or text", e);
        }
      }

      if (url != null && !url.isBlank()) {
        try {
          Desktop.getDesktop().browse(new URI(url));
        } catch (ParseException | IOException | URISyntaxException e) {
          logger.log(Level.WARNING, "Cannot open browser for URL: " + url, e);
        }
      }
    }
  }

  /**
   * GNPS-GC-MS workflow: Direct submission
   *
   * @param fileName
   * @param param
   * @return
   */
  public static String submitGcJob(File fileName, GnpsGcSubmitParameters param) {
    // TODO Auto-generated method stub
    return null;
  }
}
