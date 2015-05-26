/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

/*
 * Original author: Yann Richet
 */

package net.sf.mzmine.util.R.Rsession;

import org.slf4j.LoggerFactory;

public class Slf4jLogger implements Logger{

	@Override
	public void println(String text, Level level){
		if(Level.OUTPUT.equals(level)){
			LOGGER.debug(text);
		}else if(Level.INFO.equals(level)){
			LOGGER.info(text);
		}else if(Level.WARNING.equals(level)){
			LOGGER.warn(text);
		}else if(Level.ERROR.equals(level)){
			LOGGER.error(text);
		}
	}

	@Override
	public void close() {
		
	}
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Slf4jLogger.class);
	
}