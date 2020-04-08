package io.github.mzmine.testing;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

public class BiasEstimator
{

	public static String measurementErrorsFile = "PPM_Errors_MZmine3.xlsx";

	public static void main(String[] args) throws Exception
	{
		System.out.println("Bias Estimator");
		// URL path = BiasEstimator.class.getResource(measurementErrorsFile);
		// URL path = ClassLoader.getSystemResource(measurementErrorsFile);
		// ClassLoader.getResourceAsStream(measurementErrorsFile);
		Workbook workbook = WorkbookFactory.create(BiasEstimator.class.getResourceAsStream(measurementErrorsFile));
		// URL path = BiasEstimator.class.getClassLoader().getResource(measurementErrorsFile);
		// Workbook workbook = WorkbookFactory.create(new File(measurementErrorsFile));
		// Workbook workbook = WorkbookFactory.create(new File(path.getFile()));
		// File file = new File(path.getFile());
		// File file = new File(measurementErrorsFile);
		// File file = new File(path.toURI());
		// System.out.println(path.toURI());
		// File file = new File(path.toURI());
		// File file = new File(path.getFile());
		// File file = Paths.get(path.toURI()).toFile();
		// System.out.println(file.exists());
		System.out.println("Found " + workbook.getNumberOfSheets() + " sheets in the file");
		for(Sheet sheet: workbook) {
			System.out.println("Sheet name: " + sheet.getSheetName());
		}

		// ArrayList<Float[]> ppmErrors = new ArrayList<Float[]>();
		// ArrayList<ArrayList<Float>> ppmErrors = new ArrayList<ArrayList<Float>>();
		ArrayList<ArrayList<Double>> ppmErrors = new ArrayList<ArrayList<Double>>();
		for(Sheet sheet: workbook)
		{
			ppmErrors.add(getErrors(sheet));
		}

		System.out.println("Found error distributions");
		int index = 0;
		for(var errors: ppmErrors)
		{
			System.out.println("Sheet " + (index+1) + ", found " + errors.size() + " errors ppm");
			index++;
		}

		System.out.println();
		System.out.println("Arithemtic mean bias estimates for given distributions");
		index = 0;
		for(var errors: ppmErrors)
		{
			MassMeasurementBiasEstimator meanEstimator = new ArithmeticMeanBiasEstimator(errors);
			double estimate = meanEstimator.getBiasEstimate();
			System.out.printf("Distribution %d: bias estimate %f%n", index+1, estimate);
			index++;
		}

		double maxRangeLength = 2;
		System.out.println();
		System.out.println("Fixed length estimates for given distributions");
		System.out.println("Using max range length of " + maxRangeLength + " ppm value");
		index = 0;
		for(var errors: ppmErrors)
		{
			System.out.println();
			MassMeasurementBiasEstimator fixedRangeEstimator = new FixedLengthRangeBiasEstimator(errors, maxRangeLength);
			double estimate = fixedRangeEstimator.getBiasEstimate();
			System.out.printf("Distribution %d: bias estimate %f%n", index+1, estimate);
			index++;
		}

		// DistributionPlot.main();
		// System.out.println("test");
	}

	// protected static Float[] getErrors(Sheet sheet)
	// protected static ArrayList<Float> getErrors(Sheet sheet)
	protected static ArrayList<Double> getErrors(Sheet sheet)
	{
		System.out.println();
		System.out.println(sheet.getSheetName());
		DataFormatter dataFormatter = new DataFormatter();
		ArrayList<Double> array = new ArrayList<Double>();

		int index = 0;
		for(Row row: sheet)
		{
			// for(Cell cell: row)
			// {
			// 	System.out.println(dataFormatter.formatCellValue(cell));			
			// }
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
		// return new Float[10];
		return array;
	}
}