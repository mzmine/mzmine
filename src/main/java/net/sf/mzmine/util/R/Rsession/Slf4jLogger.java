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