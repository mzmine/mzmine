/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.export_gnps;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import io.github.mzmine.modules.io.export_gnps.fbmn.GnpsFbmnSubmitParameters;
import io.github.mzmine.modules.io.export_gnps.gc.GnpsGcSubmitParameters;
import io.github.mzmine.util.files.FileAndPathUtil;

/**
 * Class to submit GNPS feature based molecular networking jobs directly
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GNPSUtils {

  // Logger.
  private static final Logger logger = Logger.getLogger(GNPSUtils.class.getName());

  public static final String FBMN_SUBMIT_SITE =
      "https://gnps-quickstart.ucsd.edu/uploadanalyzefeaturenetworking";
  public static final String GC_SUBMIT_SITE =
      "https://gnps-quickstart.ucsd.edu/uploadanalyzegcnetworking";

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

    }
    File meta = !useMeta ? null
        : param.getParameter(GnpsFbmnSubmitParameters.META_FILE).getEmbeddedParameter()
            .getValue();

    return submitFbmnJob(mgf, quan, meta, null, title, email, username, password, presets,
        openWebsite);
  }

  /**
   * Submit feature-based molecular networking (FBMN) job to GNPS
   */
  public static String submitFbmnJob(@Nonnull File mgf, @Nonnull File quan, @Nullable File meta,
      @Nullable File[] additionalEdges,
      String title, String email, String username, String password, String presets,
      boolean openWebsite) throws IOException {
    // NEEDED files
    if (mgf.exists() && quan.exists() && !presets.isEmpty()) {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      try {
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
        CloseableHttpResponse response = httpclient.execute(httppost);
        try {
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
        } finally {
          response.close();
        }
      } finally {
        httpclient.close();
      }
    }
    return "";
  }

  /**
   * Open website with GNPS job
   *
   * @param resEntity
   */
  private static void openWebsite(HttpEntity resEntity) {
    if (Desktop.isDesktopSupported()) {
      try {
        JSONObject res = new JSONObject(EntityUtils.toString(resEntity));
        String url = res.getString("url");
        logger.info("Response: " + res.toString());

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
