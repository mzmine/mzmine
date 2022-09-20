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
package io.github.mzmine.main;

import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.taskcontrol.Task;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

public class GoogleAnalyticsTracker {

  public static final GoogleAnalyticsTracker GAT = new GoogleAnalyticsTracker();
  private static final Logger logger = Logger.getLogger(GoogleAnalyticsTracker.class.getName());
  // Parameters
  private static final String trackingUrl = "http://www.google-analytics.com/__utm.gif";
  private static final String trackingCode = "UA-63013892-4"; // Google Analytics Tracking Code
  private final String systemLocale; // Language
  private final boolean sendGUIinfo;
  private final Random random;
  private final String userAgent; // User Agent name
  private String hostName = "localhost"; // Host name
  private Dimension screenSize; // Screen Size

  private GoogleAnalyticsTracker() {
    // Parameters
    Desktop desktop = MZmineCore.getDesktop();
    // Only if not in "headless" mode
    this.sendGUIinfo = !(desktop instanceof HeadLessDesktop);
    if (this.sendGUIinfo) {
      screenSize = Toolkit.getDefaultToolkit().getScreenSize();

      // Find screen size for multiple screen setup
      GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice[] devices = g.getScreenDevices();
      if (devices.length > 1) {
        int totalWidth = 0;
        for (GraphicsDevice device : devices) {
          totalWidth += device.getDisplayMode().getWidth();
        }
        screenSize = new Dimension(totalWidth, (int) screenSize.getHeight());
      }
    }
    systemLocale = Locale.getDefault().toString().replace("_", "-");
    random = new Random(System.currentTimeMillis());

    if (hostName.equals("localhost")) {
      try {
        hostName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        // silent
      }
    }

    // userAgent = "Java/" + System.getProperty("java.version");
    // Operating System
    String os = System.getProperty("os.arch");
    if (os == null || os.length() < 1) {
      userAgent = "UNKNOWN";
    } else {

      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
        userAgent = "Mozilla/5.0 (Windows NT " + System.getProperty("os.version") + ")";
      } else if (System.getProperty("os.name").toLowerCase().contains("macintosh")) {
        userAgent = "Mozilla/5.0 (Mozilla/5.0 (Macintosh)";
      } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
        userAgent = "Mozilla/5.0 (Macintosh; Intel " + System.getProperty("os.name") + " "
            + System.getProperty("os.version").replace(".", "_") + ")";
      } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
        userAgent = "Mozilla/5.0 (Mozilla/5.0 (Linux)";
      } else {
        userAgent =
            "Mozilla/5.0 (" + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + ")";
      }
    }
  }

  public static void track(final String title, final String url) {
    Thread gatThread = new Thread(() -> GAT.send(title, url));
    gatThread.setName("GAT MZmine");
    gatThread.setPriority(Thread.MIN_PRIORITY);
    gatThread.start();
  }

  public static void trackTaskRun(Task task) {
    trackClass("Run task", task);
  }

  public static void trackClass(final String title, Object obj) {
    String className = obj.getClass().getName();
    track(title, "/JAVA/" + className);
  }

  public static void trackModule(MZmineRunnableModule module) {
    track("Module run: " + module.getName(), "/JAVA/" + module.getName());
  }


  public void send(String pageTitle, String pageUrl) {
    pageUrl = pageUrl.replaceAll(" ", "_");

    // Only send data if sendStatistics variable is not set to 0
    Boolean sendStatistics = MZmineCore.getConfiguration().getSendStatistics();

    if (sendStatistics) {
      String documentTitle = "Java: MZmine " + MZmineCore.getMZmineVersion() + " - " + pageTitle;
      int cookie = random.nextInt();
      int randomValue = random.nextInt(2147483647) - 1;
      long now = new Date().getTime();

      StringBuilder url = new StringBuilder(trackingUrl);
      url.append("?utmwv=1"); // Analytics version
      url.append("&utmn=").append(random.nextInt()); // Random int
      url.append("&utmcs=UTF-8"); // Encoding
      if (this.sendGUIinfo) {
        url.append(
            "&utmsr=" + (int) screenSize.getWidth() + "x" + (int) screenSize.getHeight()); // Screen
        // size
      }
      url.append("&utmul=" + systemLocale); // User language
      url.append("&utmje=1"); // Java Enabled
      url.append("&utmcr=1"); // Carriage return
      url.append("&utmdt=" + URLEncoder.encode(documentTitle, StandardCharsets.UTF_8)); // Document
      // title
      url.append("&utmhn=" + hostName);// Hostname
      url.append("&utmp=" + URLEncoder.encode(pageUrl, StandardCharsets.UTF_8));// Document url
      url.append("&utmac=" + trackingCode);// Google Analytics account
      url.append(
          "&utmcc=__utma%3D'" + cookie + "." + randomValue + "." + now + "." + now + "." + now
              + ".2%3B%2B__utmb%3D" + cookie + "%3B%2B__utmc%3D" + cookie + "%3B%2B__utmz%3D"
              + cookie + "." + now
              + ".2.2.utmccn%3D(direct)%7Cutmcsr%3D(direct)%7Cutmcmd%3D(none)%3B%2B__utmv%3D"
              + cookie);

      try {

        URI uri = new URI(url.toString());
//        logger.info("Sending stats " + uri);

        HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();

        HttpRequest request = HttpRequest.newBuilder().uri(uri).headers("User-agent", userAgent)
            .GET().build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

//        logger.info(response.body() + "  CODE:" + response.statusCode());

      } catch (Exception e) {
        // silent
//        logger.log(Level.WARNING, "Error during connection. " + e.getMessage(), e);
      }
    }
  }

}
