/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.util.Date;
import java.util.logging.*;
import javax.swing.*;

class GUIHandler extends Handler {
	private JFrame frame;
	private JTextArea text;
	private StringWriter writer;
	private PrintWriter out;
	
	public GUIHandler(final ConfigurationManager c) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				frame = new JFrame("JayWormNET " + JayWormNet.version);
				text  = new JTextArea();
				text.setVisible(true);
				text.setEditable(false);
			
				// Setting up colors — invalid values (for example, "default") will be ignored
				try {
					text.setBackground(Color.decode(c.backgroundColor));
				} catch(NumberFormatException e) {
				} try {
					text.setForeground(Color.decode(c.foregroundColor));
				} catch(NumberFormatException e) {
				}
				
				JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				
				frame.setSize(800, 512);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(scroll);
				frame.setVisible(true);
				
				writer = new StringWriter();
				out = new PrintWriter(writer);
			}
		});
	}
	
	@Override public void publish(final LogRecord rec) {
		final Formatter fmt = this.getFormatter();
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				out.println(fmt.format(rec));
				text.setText(writer.toString());
			}
		});
	}
	
	@Override public void flush() {
		out.flush();
	}
	
	@Override public void close() {
		out.close();
	}
}

class WNLogFormatter extends Formatter {
	// Filtering for non-printable characters
	protected static String filter(String s) {
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

class SimplifiedWNLogFormatter extends WNLogFormatter {
	@Override public String format(LogRecord rec) {
		return filter("[" + new Date(rec.getMillis()) + " " + rec.getLevel()
				+ "] " + rec.getMessage());
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

		// Resetting log formatter for console output, etc.
		l.setUseParentHandlers(false);
		ConsoleHandler conHandler = new ConsoleHandler();
		conHandler.setFormatter(new WNLogFormatter());
		l.addHandler(conHandler);

		// Checking if graphics available and creating GUI form
		if(!GraphicsEnvironment.isHeadless() && c.enableGUI && !JayWormNet.forceNoGUI) {
			GUIHandler h = new GUIHandler(config);
			h.setFormatter(new SimplifiedWNLogFormatter());
			l.addHandler(h);
		}
		
		if(!c.loggingEnabled) {
			l.setLevel(Level.parse(c.loggingLevel));
			return;
		}
		// Setting up log file
		try {
			FileHandler fileHandler = new FileHandler(c.logFile, true);
			fileHandler.setFormatter(new WNLogFormatter());
			l.addHandler(fileHandler);
		} catch(IOException | SecurityException e) {
			System.err.println("Warning: can't set up file logging. " + e);
		}
		
		l.setLevel(Level.parse(c.loggingLevel));
	}
}
