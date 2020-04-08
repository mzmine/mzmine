package io.github.mzmine.testing;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class BiasEstimator
{

	public static String measurementErrorsFile = "PPM_Errors_MZmine3.xlsx";

	public static void main(String[] args) throws Exception
	{
		System.out.println("Bias Estimator");
		Workbook workbook = WorkbookFactory.create(BiasEstimator.class.getResourceAsStream(measurementErrorsFile));
		System.out.println("Found " + workbook.getNumberOfSheets() + " sheets in the file");
		for(Sheet sheet: workbook) {
			System.out.println("Sheet name: " + sheet.getSheetName());
		}

		System.out.println();
		ArrayList<ArrayList<Double>> ppmErrors = new ArrayList<ArrayList<Double>>();
		for(Sheet sheet: workbook)
		{
			ppmErrors.add(getErrors(sheet));
		}

		System.out.println();
		System.out.println("Found error distributions");
		int index = 0;
		for(var errors: ppmErrors)
		{
			System.out.println("Sheet " + (index+1) + ", found " + errors.size() + " errors ppm");
			index++;
		}

		System.out.println();
		System.out.println("Measurement bias estimated for found error distributions");
		double maxRangeLength = 2;
		index = 0;
		for(var errors: ppmErrors)
		{
			System.out.println();
			System.out.println("Distribution " + (index+1) + ":  " + errors.size() + " errors ppm");

			MassMeasurementBiasEstimator meanEstimator = new ArithmeticMeanBiasEstimator(errors);
			double meanEstimate = meanEstimator.getBiasEstimate();
			System.out.printf("Arithmetic mean bias estimate: %f%n", meanEstimate);

			// MassMeasurementBiasEstimator fixedRangeEstimator = new FixedLengthRangeBiasEstimator(errors, maxRangeLength);
			FixedLengthRangeBiasEstimator fixedRangeEstimator = new FixedLengthRangeBiasEstimator(errors, maxRangeLength);
			double fixedRangeEstimate = fixedRangeEstimator.getBiasEstimate();
			System.out.printf("Fixed range length bias estimate, using max range length of %f ppm value: %f%n", 
				maxRangeLength, fixedRangeEstimate);

			HashMap<String, Double> lines = new HashMap<String, Double>();
			lines.put(" ".repeat(40) + "Range smallest value", fixedRangeEstimator.getMostErrorsStartValue());
			lines.put(" ".repeat(40) + "Range biggest value", fixedRangeEstimator.getMostErrorsEndValue());
			lines.put(" ".repeat(140) + "Arithmetic mean", meanEstimate);
			lines.put(" ".repeat(90) + "Range mean", fixedRangeEstimate);

			DistributionPlot.main("ppm errors and measurement bias estimates, distribution " + (index+1),
				fixedRangeEstimator.getErrors(), lines);


			RangeExtenderBiasEstimator rangeExtender = new RangeExtenderBiasEstimator(errors, 1.1, 
				fixedRangeEstimator.getMostErrorsStart(), fixedRangeEstimator.getMostErrorsEnd());
			double stretchedRangeEstimate = rangeExtender.getBiasEstimate();
			System.out.printf("Stretched range bias estimate: %f%n", stretchedRangeEstimate);

			HashMap<String, Double> lines2 = new HashMap<String, Double>();
			lines2.put(" ".repeat(150) + "Range length extender smallest value", rangeExtender.getMostErrorsStartValue());
			lines2.put(" ".repeat(150) + "Range length extender biggest value", rangeExtender.getMostErrorsEndValue());
			lines2.put(" ".repeat(150) + "Range length extender mean", stretchedRangeEstimate);

			DistributionPlot.main("ppm errors and measurement bias estimates stretched, distribution " + (index+1),
				rangeExtender.getErrors(), lines2);

			index++;
		}

	}

	protected static ArrayList<Double> getErrors(Sheet sheet)
	{
		System.out.println();
		System.out.println(sheet.getSheetName());
		DataFormatter dataFormatter = new DataFormatter();
		ArrayList<Double> array = new ArrayList<Double>();

		int index = 0;
		for(Row row: sheet)
		{
			if(index == 0)
			{
				index++;
				continue;
			}
			try
			{
				array.add(row.getCell(3).getNumericCellValue());
				System.out.println("PPM error " + array.get(array.size() - 1));
			}
			catch(Exception e)
			{
				System.out.println("Could not get PPM error");
				System.out.println(e);
			}
			index++;
		}
		return array;
	}
}