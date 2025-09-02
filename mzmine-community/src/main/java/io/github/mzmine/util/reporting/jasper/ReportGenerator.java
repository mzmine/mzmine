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

package io.github.mzmine.util.reporting.jasper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportGenerator {

    public static void main(String[] args) throws JRException, FileNotFoundException {
        // 1. Compile your JRXML templates
        // Assuming you have MainReport.jrxml, SummarySubreport.jrxml, FeatureDetailSubreport.jrxml
        InputStream mainReportStream = new FileInputStream(new File("C:\\Users\\Steffen\\JaspersoftWorkspace\\MyReports\\testreport.jrxml"));
        JasperReport jasperMainReport = JasperCompileManager.compileReport(mainReportStream);

//        InputStream summarySubreportStream = ReportGenerator.class.getResourceAsStream("/reports/SummarySubreport.jrxml");
//        JasperReport jasperSummarySubreport = JasperCompileManager.compileReport(summarySubreportStream);

//        InputStream featureDetailSubreportStream = ReportGenerator.class.getResourceAsStream("/reports/FeatureDetailSubreport.jrxml");
//        JasperReport jasperFeatureDetailSubreport = JasperCompileManager.compileReport(featureDetailSubreportStream);

        // 2. Prepare your data (JavaBeans)

        /*// Summary Data
        List<FeatureSummary> summaryData = new ArrayList<>();
        summaryData.add(new FeatureSummary(100.123, 5.2, "Compound A"));
        summaryData.add(new FeatureSummary(201.456, 12.8, "Compound B"));
        // ... populate more summary features

        // Detailed Data (each containing rasterized SVG as InputStream)
        List<FeatureDetail> detailedData = new ArrayList<>();
        // For demonstration, let's create some dummy details with generated images
        for (int i = 0; i < 3; i++) {
            FeatureDetail detail = new FeatureDetail(100.0 + i, 5.0 + i, "Feature " + (char)('A'+i));
            // Simulate SVG to PNG conversion (as discussed in previous answer)
            try {
                String simulatedSvg = "<svg width=\"300\" height=\"150\">" +
                                      "<circle cx=\"150\" cy=\"75\" r=\"50\" fill=\"lightgreen\"/>" +
                                      "<text x=\"10\" y=\"20\">Details for " + detail.getId() + "</text>" +
                                      "</svg>";
                detail.generateChromatogramImageFromSvg(simulatedSvg);
            } catch (Exception e) {
                e.printStackTrace(); // Handle error gracefully
            }
            detailedData.add(detail);
        }*/

        // 3. Set up parameters and data sources for the main report
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("META_COMPANY", "mzio GmbH");
        parameters.put("META_TITLE", "Example report");
        parameters.put("META_LAB_DESCRIPTION", """
            Dr. Steffen Heuckeroth
            CTO, PhD. Analytical Chemistry
            A street 1337
            481XX Münster
            Germany""");
        parameters.put("META_ORDER_NUMBER", "123SSDBJKUAH213");
        parameters.put("META_CUSTOMER_NAME", "Customer Name");
        parameters.put("META_CUSTOMER_DEPARTMENT", "Customer Department");
        parameters.put("META_CUSTOMER_ADDRESS", "Customer Road 537, A town, XXXXXX");
        parameters.put("META_CUSTOMER_PROJECT", "Research Project X15");
        parameters.put("META_ORDER_REQUEST_DATE", "15.03.2025");
        parameters.put("META_ORDER_FINISHED_DATE", "15.04.2025");
        parameters.put("META_ORDER_SAMPLEIDS", "1524835, 1535483, 16832185");
        parameters.put("META_FREE_TEXT_FIELD", """
            Sehr geehrte Frau Mustermann,
            die Probe(n) wurde(n) auftragsgemäß in Anlehnung an DIN EN ISO/IEC 9001 untersucht. Die
            Analysenergebnisse beziehen sich ausschließlich auf das zur Verfügung gestellte Probenmaterial zum
            Zeitpunkt der Analyse. Die Proben können bei Bedarf abgeholt werden. Ansonsten müssen die Proben
            aus platztechnischen Gründen leider nach 14 Tagen entsorgt werden.""");
        parameters.put("META_ORDER_DESC", "Identification of Metabolites in X");
        parameters.put("META_LOGO_PATH", "D:\\OneDrive - mzio GmbH\\mzio\\design\\logos\\svg\\logo_mzio.svg");

        parameters.put("LAB_ADDRESS", "Altenwall 26, 28195 Bremen");


        // Pass compiled subreports as parameters so the main report can reference them
//        parameters.put("SUMMARY_SUBREPORT", jasperSummarySubreport);
//        parameters.put("FEATURE_DETAIL_SUBREPORT", jasperFeatureDetailSubreport);

        // Data source for the Summary (if passed to a subreport/table component)
//        parameters.put("SUMMARY_DATA_SOURCE", new JRBeanCollectionDataSource(summaryData));

        // Data source for the Detailed Features (if passed to a subreport/list component)
//        parameters.put("DETAILED_FEATURES_DATA_SOURCE", new JRBeanCollectionDataSource(detailedData));


        // Main report's data source (can be empty if it's just a container)
        // If your main report iterates over some master object, provide that here.
        // For this example, let's assume the main report has no direct data source,
        // but uses parameters to pass data to subreports.
        JREmptyDataSource mainReportDataSource = new JREmptyDataSource();


        // 4. Fill the report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperMainReport, parameters, mainReportDataSource);

        // 5. View/Export the report
        JasperViewer.viewReport(jasperPrint, false);
    }
}
