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