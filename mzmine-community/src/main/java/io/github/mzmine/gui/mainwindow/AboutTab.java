/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.MZmineCore;
import io.mzio.links.MzioMZmineLinks;
import java.awt.Desktop;
import java.net.URI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AboutTab extends SimpleTab {

  public AboutTab() {
    super("About mzmine");
    setContent(createContent());
  }

  private Node createContent() {
    VBox contentBox = new VBox(10);
    contentBox.setAlignment(Pos.TOP_CENTER);
    contentBox.setPadding(new Insets(10));

    Node mzmineImage = FxIconUtil.resizeImage("icons/introductiontab/logos_mzio_mzmine.png", 350,
        200);
    contentBox.getChildren().add(mzmineImage);

    // Software Name
    contentBox.getChildren().add(createDescriptionLabel("Software Name"));
    Label softwareName = new Label("mzmine");
    contentBox.getChildren().add(softwareName);

    // Version Information
    contentBox.getChildren().add(createDescriptionLabel("Version Information"));
    Label versionInfo = new Label(MZmineCore.getMZmineVersion().toString());
    contentBox.getChildren().add(versionInfo);

    // Copyright Notice
    contentBox.getChildren().add(createDescriptionLabel("Copyright Notice"));
    Label copyrightNotice = new Label("©2024 by mzio GmbH and development team");
    contentBox.getChildren().add(copyrightNotice);

    // License Information
    contentBox.getChildren().add(createDescriptionLabel("License Information"));
    Label licenseInfo = new Label(
        "Parts of this software are distributed under the terms of the MIT license.");
    contentBox.getChildren().add(licenseInfo);

    // Contact Information
    contentBox.getChildren().add(createDescriptionLabel("Contact Information"));
    Label contactInfo = createHyperlinkLabel(MzioMZmineLinks.CONTACT.getUrl());
    contentBox.getChildren().add(contactInfo);

    // Terms and conditions
    //TODO add to mzio links
    contentBox.getChildren().add(createDescriptionLabel("Terms and Conditions"));
    Label legalInfo = createHyperlinkLabel(
        "https://mzio.io/general-terms-and-conditions/"); // Assuming you're updating this
    contentBox.getChildren().add(legalInfo);

    // Privacy Policy
    contentBox.getChildren().add(createDescriptionLabel("Privacy Policy"));
    Label privacyPolicy = createHyperlinkLabel(MzioMZmineLinks.PRIVACY_POLICY.getUrl());
    contentBox.getChildren().add(privacyPolicy);

    // Company Information
    contentBox.getChildren().add(createDescriptionLabel("Company Information"));
    Label companyInfo = new Label("mzio GmbH\nAltenwall 26\n28195 Bremen, Germany\n");
    companyInfo.setAlignment(Pos.CENTER);
    companyInfo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    contentBox.getChildren().add(companyInfo);

    // Third-party Libraries
    contentBox.getChildren().add(createDescriptionLabel("Third-party Libraries"));
    Label thirdPartyAttributions = new Label(
        "MSFileReader file reading tool. Copyright © 2009 - 2014 by Thermo Fisher Scientific, Inc. All rights reserved.\n"
            + "MassLynxRaw library. Copyright © 2014 by Waters, Inc.\n"
            + "TDF Software Development Kit, Bruker Daltonics GmbH & Co. KG");
    thirdPartyAttributions.setAlignment(Pos.CENTER);
    thirdPartyAttributions.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    contentBox.getChildren().add(thirdPartyAttributions);

    ScrollPane scrollPane = new ScrollPane(contentBox);
    scrollPane.setFitToWidth(true);
    scrollPane.setPadding(new Insets(20)); // Add padding to the ScrollPane

    return scrollPane;
  }

  private Label createDescriptionLabel(String description) {
    Label label = new Label(description);
    label.setFont(Font.font(null, FontWeight.BOLD, 14));
    return label;
  }

  private Label createHyperlinkLabel(String url) {
    Label hyperlink = new Label(url);
    hyperlink.setStyle("-fx-text-fill: blue; -fx-underline: true;");
    hyperlink.setCursor(javafx.scene.Cursor.HAND);
    hyperlink.setOnMouseClicked(e -> {
      try {
        Desktop.getDesktop().browse(new URI(url));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    });
    return hyperlink;
  }


}
