/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.numberformat;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * NumberFormat extension to provide both number-style and date-style
 * formatting, with XML import/export of format definition. This class is
 * synchronized, so it can be used by multiple threads.
 */
public class RTFormatter extends NumberFormat implements Cloneable {

	private RTFormatterType embeddedNumberFormatterType;
	private Format embeddedFormatter;

	public RTFormatter(RTFormatterType type, String pattern) {
		setFormat(type, pattern);
	}

	public void setFormat(RTFormatterType type, String pattern) {
		
		assert type != null;
		
		this.embeddedNumberFormatterType = type;
		
		switch (type) {
		case Time:

			// We want to avoid the 12-hour format ('h') and use 24-hour format
			// starting with 0 instead ('H')
			pattern = pattern.replace('h', 'H');

			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			// important for handling low values, otherwise in different Time
			// zones we may get to negative numbers
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
			embeddedFormatter = sdf;

			break;
		case NumberInMin:
		case NumberInSec:
			embeddedFormatter = new DecimalFormat(pattern);
			break;
		}
	}

	public RTFormatterType getType() {
		return embeddedNumberFormatterType;
	}

	public String getPattern() {
		switch (embeddedNumberFormatterType) {
		case Time:
			return ((SimpleDateFormat) embeddedFormatter).toPattern();
		case NumberInMin:
		case NumberInSec:
			return ((DecimalFormat) embeddedFormatter).toPattern();
		}
		return null;
	}

	/**
	 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public synchronized StringBuffer format(double arg0, StringBuffer arg1,
			FieldPosition arg2) {

		// conversion to msec
		if (embeddedNumberFormatterType == RTFormatterType.Time)
			arg0 *= 1000;

		// conversion to min
		if (embeddedNumberFormatterType == RTFormatterType.NumberInMin)
			arg0 /= 60;

		return embeddedFormatter.format(arg0, arg1, arg2);
	}

	/**
	 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public synchronized StringBuffer format(long arg0, StringBuffer arg1,
			FieldPosition arg2) {

		// conversion to msec
		if (embeddedNumberFormatterType == RTFormatterType.Time)
			arg0 *= 1000;

		// conversion to min
		if (embeddedNumberFormatterType == RTFormatterType.NumberInMin)
			arg0 /= 60;

		return embeddedFormatter.format(arg0, arg1, arg2);
	}

	/**
	 * @see java.text.NumberFormat#parse(java.lang.String,
	 *      java.text.ParsePosition)
	 */
	public synchronized Number parse(String str, ParsePosition pos) {
		try {
			switch (embeddedNumberFormatterType) {
			case Time:
				SimpleDateFormat sdf = (SimpleDateFormat) embeddedFormatter;
				double result = ((sdf.parse(str, pos).getTime()) / 1000.0);
				return result;
			case NumberInMin:
				DecimalFormat df = (DecimalFormat) embeddedFormatter;
				result = df.parse(str, pos).doubleValue() * 60;
				return result;
			case NumberInSec:
				df = (DecimalFormat) embeddedFormatter;
				return df.parse(str, pos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @see java.text.NumberFormat#clone()
	 */
	public RTFormatter clone() {
		return new RTFormatter(embeddedNumberFormatterType, getPattern());
	}

}
