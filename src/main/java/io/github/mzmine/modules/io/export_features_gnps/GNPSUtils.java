/*
 * Copyright 2006-2022 The MZmine Development Team
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
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.parser.GnpsJsonParser;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
  public static final String GC_SUBMIT_SITE = "https://gnps-quickstart.ucsd.edu/uploadanalyzegcnetworking";
  public static final String ACCESS_LIBRARY_SPECTRUM = "https://gnps.ucsd.edu/ProteoSAFe/SpectrumCommentServlet?SpectrumID=";
  public static final String ACCESS_USI_SPECTRUM = "https://metabolomics-usi.ucsd.edu/json/?usi1=";
  // Logger.
  private static final Logger logger = Logger.getLogger(GNPSUtils.class.getName());


  /**
   * @param libIDorUSI GNPS library ID
   * @return library spectrum or null
   */
  public static SpectralDBEntry accessLibraryOrUSISpectrum(String libIDorUSI) throws IOException {
    if (isGnpsLibID(libIDorUSI)) {
      return accessLibrarySpectrum(libIDorUSI);
    } else {
      return accessUSISpectrum(libIDorUSI);
    }
  }

  public static boolean isGnpsLibID(String libIDorUSI) {
    return libIDorUSI.toLowerCase().startsWith("ccmslib");
  }

  /**
   * @param libraryID GNPS library ID
   * @return library spectrum or null
   */
  public static SpectralDBEntry accessLibrarySpectrum(String libraryID) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(ACCESS_LIBRARY_SPECTRUM + libraryID);
      logger.info("Retrieving library spectrum " + httpGet.getRequestLine());

      try (CloseableHttpResponse response = client.execute(httpGet)) {
        logger.info("GNPS library response: " + response.getStatusLine());
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
          final String requestResult = EntityUtils.toString(resEntity);
          logger.info("GNPS library response: " + requestResult);

          // all 200s are success
          if (response.getStatusLine().getStatusCode() / 200 == 1) {
            // open job website
            EntityUtils.consume(resEntity);
            // create object from json
            return parseJsonToSpectrum(requestResult);
          }
        }
      }
    }

    return null;
  }

  /**
   * @param usi universal spectrum identifier
   * @return library spectrum or null
   */
  public static SpectralDBEntry accessUSISpectrum(String usi) throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(ACCESS_USI_SPECTRUM + usi);
      logger.info("Retrieving USI spectrum " + httpGet.getRequestLine());

      try (CloseableHttpResponse response = client.execute(httpGet)) {
        logger.info("GNPS library response: " + response.getStatusLine());
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
          final String requestResult = EntityUtils.toString(resEntity);
          logger.info("GNPS library response: " + requestResult);

          // all 200s are success
          if (response.getStatusLine().getStatusCode() / 200 == 1) {
            // open job website
            EntityUtils.consume(resEntity);
            // create object from json
            return parseJsonToSpectrum(requestResult);
          }
        }
      }
    }

    return null;
  }

  @NotNull
  private static SpectralDBEntry parseJsonToSpectrum(String jsonSpec) {
    try (JsonReader reader = Json.createReader(new StringReader(jsonSpec))) {
      JsonObject json = reader.readObject();
      // GNPS has different json return types just try to read the first one which is used for USI
      // then the other that is used for library spectra
      if (json.containsKey("peaks")) {
        // https://metabolomics-usi.ucsd.edu/json/?usi1=mzspec%3AGNPS%3AGNPS-LIBRARY%3Aaccession%3ACCMSLIB00000579622
        JsonArray peaks = json.getJsonArray("peaks");
        DataPoint[] spectrum = GnpsJsonParser.getDataPointsFromJsonArray(peaks);
        final double precursorMz = json.getJsonNumber("precursor_mz").doubleValue();
        final int charge = json.getJsonNumber("precursor_charge").intValue();
        return new SpectralDBEntry(precursorMz, charge, spectrum);
      } else {
        // https://gnps.ucsd.edu/ProteoSAFe/SpectrumCommentServlet?SpectrumID=CCMSLIB00005463737
        // library ID
        final JsonObject info = json.getJsonObject("spectruminfo");
        final String spectrumString = info.getJsonString("peaks_json").getString();
        try (JsonReader specReader = Json.createReader(new StringReader(spectrumString))) {
          final DataPoint[] spectrum = GnpsJsonParser.getDataPointsFromJsonArray(
              specReader.readArray());

          // precursor mz
          final JsonObject annotations = json.getJsonArray("annotations").getJsonObject(0);
          final double precursorMz = Double.parseDouble(
              annotations.getJsonString("Precursor_MZ").getString());
          return new SpectralDBEntry(precursorMz, spectrum);
        }
      }
    }
  }


  /**
   * Submit feature-based molecular networking (FBMN) job to GNPS
   *
   * @param file  base file name to find mgf (file) and csv (file_quant.csv)
   * @param param submission parameters
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
            logger.info("GNPS submit response content length: " + resEntity.getContentLength());

            // open job website
            if (openWebsite) {
              openWebsite(resEntity);
            }
            EntityUtils.consume(resEntity);
          }
        }
      }
    }
    return "";
  }

  /**
   * Open website with GNPS job
   */
  private static void openWebsite(HttpEntity resEntity) {
    if (Desktop.isDesktopSupported()) {
      try {
        JSONObject res = new JSONObject(EntityUtils.toString(resEntity));
        String url = res.getString("url");
        logger.info("Response: " + res);

        if (url != null && !url.isEmpty()) {
          Desktop.getDesktop().browse(new URI(url));
        }

      } catch (ParseException | IOException | URISyntaxException | JSONException e) {
        logger.log(Level.SEVERE, "Error while submitting GNPS job", e);
      }
    }
  }

  /**
   * GNPS-GC-MS workflow: Direct submission
   */
  public static String submitGcJob(File fileName, GnpsGcSubmitParameters param) {
    // TODO Auto-generated method stub
    return null;
  }
}
