/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.reporting;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestPoiReport {

  public static void main(String[] args) {
    XWPFDocument document = new XWPFDocument();

    try (FileOutputStream out = new FileOutputStream("C:\\Users\\Steffen\\Documents\\report_figures\\document_with_figure.docx")) {

      // --- 1. Add some introductory text ---
      XWPFParagraph introParagraph = document.createParagraph();
      XWPFRun introRun = introParagraph.createRun();
      introRun.setText("This document demonstrates how to add a figure and a caption using Apache POI.");
      introRun.addBreak(); // Add a line break for spacing

      // --- 2. Add the Image ---
      XWPFParagraph imageParagraph = document.createParagraph();
      imageParagraph.setAlignment(ParagraphAlignment.CENTER); // Center the image

      String imgFile = "C:\\Users\\Steffen\\Documents\\report_figures\\example_image.jpg"; // Make sure this image file exists!
      Path imgPath = Paths.get(imgFile);

      if (!Files.exists(imgPath)) {
        System.err.println("Error: Image file not found at " + imgPath.toAbsolutePath());
        System.err.println("Please place 'example_image.jpg' in your project directory.");
        return; // Exit if image not found
      }

      try (FileInputStream fis = new FileInputStream(imgFile)) {
        XWPFRun imageRun = imageParagraph.createRun();
        // Add the image. Arguments: image stream, image type, image file name, width, height
        // Units.EMU_PER_PIXEL is used to convert pixels to EMUs (English Metric Units)
        // A common practice is to define max width/height for images to control their size in the document.
        int width = 300; // pixels
        int height = 200; // pixels
        imageRun.addPicture(fis, Document.PICTURE_TYPE_JPEG, imgFile,
            Units.toEMU(width), Units.toEMU(height));
      }

      // --- 3. Add the Figure Caption ---
      XWPFParagraph captionParagraph = document.createParagraph();
      captionParagraph.setAlignment(ParagraphAlignment.CENTER); // Center the caption
      XWPFRun captionRun = captionParagraph.createRun();
      captionRun.setText("Figure 1: A beautiful example image generated programmatically.");
      captionRun.setItalic(true);
      captionRun.setFontSize(10);
      captionRun.addBreak(); // Add a line break after the caption for spacing

      // --- 4. Add text that references the figure ---
      XWPFParagraph referenceParagraph = document.createParagraph();
      XWPFRun referenceRun = referenceParagraph.createRun();
      referenceRun.setText("As seen in Figure 1, the image provides a visual representation of our programmatic generation capabilities.");
      referenceRun.addBreak();

      // --- 5. Add a second figure to demonstrate numbering (optional) ---
      XWPFParagraph imageParagraph2 = document.createParagraph();
      imageParagraph2.setAlignment(ParagraphAlignment.CENTER);

      // You would use a different image file here, or reuse the same for demonstration
      // For simplicity, let's reuse the same image and just change the caption
      try (FileInputStream fis2 = new FileInputStream(imgFile)) {
        XWPFRun imageRun2 = imageParagraph2.createRun();
        imageRun2.addPicture(fis2, Document.PICTURE_TYPE_JPEG, imgFile,
            Units.toEMU(250), Units.toEMU(150)); // Slightly smaller image
      }

      XWPFParagraph captionParagraph2 = document.createParagraph();
      captionParagraph2.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun captionRun2 = captionParagraph2.createRun();
      captionRun2.setText("Figure 2: Another instance, possibly with different parameters.");
      captionRun2.setItalic(true);
      captionRun2.setFontSize(10);
      captionRun2.addBreak();

      XWPFParagraph referenceParagraph2 = document.createParagraph();
      XWPFRun referenceRun2 = referenceParagraph2.createRun();
      referenceRun2.setText("Figure 2 further illustrates the flexibility in image insertion.");


      // Write the Document to the file system
      document.write(out);
      System.out.println("document_with_figure.docx written successfully!");

    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
