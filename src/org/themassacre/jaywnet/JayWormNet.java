/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

// Main class
public class JayWormNet {
	public static HTTPServer http = null;
	public static IRCServer irc = null;
	
	static ConfigurationManager config;
	public static final String version = "alpha-0.10";
	public static boolean forceNoGUI = false;

	static void printHelp() {
		System.err.println("JayWormNET — Java WormNET server\n\nList of available command-line options:\n"
				+ "\t--nogui\t\t- never use GUI\n\t--version\t- show JayWormNET version\n\t--help\t\t"
				+ "- show this message\n\nYou may want to edit or create the 'wnet.cfg' file for fine tuning.");
	}
	
	public static void main(String[] args) {
		// Command-line arguments
		if(args.length > 0) {
			if(args[0].equals("--nogui"))
				forceNoGUI = true;
			else if(args[0].equals("--help")) {
				printHelp();
				System.exit(0);
			} else if(args[0].equals("--version")) {
				System.err.println("JayWormNET " + version);
				System.exit(0);
			}
		}
		
		// Creating configuration storage
		config = new ConfigurationManager("wnet.cfg"); // file name here
		
		// Initializing logging
		WNLogger.start(config);
		WNLogger.l.info("JayWormNET " + version);
		WNLogger.l.info("Server hostname is '" + config.serverHost + "'");

		// Starting servers
		if(config.HTTPPort > 0)		http = new HTTPServer(config);
		if(config.IRCPort > 0)		irc = new IRCServer(config);
	}

}
