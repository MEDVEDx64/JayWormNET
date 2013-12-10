/*/ Part of FryWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.frywnet;

import java.io.*;
import java.util.Date;
import java.util.logging.*;

class WNLogFormatter extends Formatter {
	// Filtering for non-printable characters
	private static String filter(String s) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < s.length(); i++) {
			String sym = s.charAt(i) == '\n'? "\\n":
					s.charAt(i) == '\t'? "\\t":
					s.charAt(i) == '\r'? "\\r":
					s.charAt(i) == '\b'? "\\b":
					s.charAt(i) == '\f'? "\\f": null;
			if(sym == null) {
				buffer.ensureCapacity(buffer.length()+2);
				buffer.append(s.charAt(i));
			} else {
				buffer.ensureCapacity(buffer.length()+4);
				buffer.append(sym);
			}
		}
		
		return buffer.toString().replaceAll("\\p{C}", "?");
	}
	
	@Override public String format(LogRecord rec) {
		return filter("[" + new Date(rec.getMillis()) + " " + rec.getLevel()
				+ "] " + (rec.getSourceClassName().contains("IRC") || rec.getSourceClassName().contains("User")?
						"IRC: ": rec.getSourceClassName().contains("HTTP")? "HTTP: ": "") + rec.getMessage()) + "\n";
	}
}

// Logging service
public class WNLogger {
	static boolean running = false;
	static ConfigurationManager c = null;
	public static Logger l = null;
	
	public static void start(ConfigurationManager config) {
		if(running) return;
		c = config;
		l = Logger.getLogger("wnl");
		l.setLevel(Level.parse(c.loggingLevel));
		
		// Resetting log formatter for console output, etc.
		l.setUseParentHandlers(false);
		ConsoleHandler conHandler = new ConsoleHandler();
		conHandler.setFormatter(new WNLogFormatter());
		l.addHandler(conHandler);
		
		if(!c.loggingEnabled) return;
		// Setting up log file
		try {
			FileHandler fileHandler = new FileHandler(c.logFile, true);
			fileHandler.setFormatter(new WNLogFormatter());
			l.addHandler(fileHandler);
		} catch(IOException | SecurityException e) {
			System.err.println("Warning: can't set up file logging. " + e);
		}
	}
}
