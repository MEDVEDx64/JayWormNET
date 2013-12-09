/*/ Part of FryWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.frywnet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import org.themassacre.util.*;

class Game {
	public String		name, password, loc, hosterNickname, hosterAddress, channel;
	public int			created, gameID;
}

class HTTPServerException extends Exception {
	private static final long serialVersionUID = 185903561694370915L;
	
	public HTTPServerException() {
		super();
	}
	
	public HTTPServerException(String mesg) {
		super(mesg);
	}
}

// HTTP server listening thread
class HTTPServerListener extends Thread {
	Socket socket;
	boolean useSnooperFix = false;
	
	public HTTPServerListener(Socket s) {
		this.socket = s;
	}
	
	public void run() {
		// Message which will be sent back to server
		String serveMessage = createResponse(200, "");
		
		// Lines, received from client
		ArrayList<String> received = new ArrayList<String>();
		
		// Additional parameters (specified in URL)
		//ArrayList<String[]> params = new ArrayList<String[]>();
		Map<String, String> params = new HashMap<String, String>();
		
		// Client socket I/O streams
		BufferedReader in;
		OutputStream out = null;
		
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = socket.getOutputStream();
			
			// Reading from client
			while(true) {
				String buffer = in.readLine();
				if(buffer == null)
					break;
				if(buffer.length() == 0)
					break;
				received.add(buffer);
			}

			if(received.size() == 0) {
				WNLogger.l.warning(socket.getInetAddress().toString().substring(1) + ": connection error.");
				Thread.sleep(100);
				socket.close();
				return;
			}
			
			// Reconstructing message from array into a single buffer, only needed for "finest" logging
			if(HTTPServer.config.loggingEnabled && Level.parse(HTTPServer.config.loggingLevel).intValue()
					<= Level.FINEST.intValue()) {
				StringBuffer b = new StringBuffer();
				for(int i = 0; i < received.size(); i++) {
					String line = received.get(i);
					b.ensureCapacity(b.length() + line.length() + 4);
					b.append(line + "\r\n");
				}
				WNLogger.l.finest("Input message: " + b.toString());
			}

			// Validating request type
			if(!received.get(0).startsWith("GET")) {
				serveMessage = createResponse(500, "Only GET requests are supported");
				throw new HTTPServerException("Bad request from client");
			}
			
			// Parsing URL
			URL url = new URL(received.get(0).split(" +")[1]);
			String fileName = url.getFile();
			// Finalizing
			fileName = fileName.contains("?")? fileName.substring(0, fileName.indexOf("?")): fileName.substring(0);
			fileName = fileName.contains("/")? fileName.split("/+")[fileName.split("/+").length-1]: fileName;
			String query = url.getQuery();
			
			// Creating parameters table
			if(query != null) {
				String[] list = query.split("&");
				for(int i = 0; i < list.length; i++) {
					if(list[i] == null) continue;
					String[] row = list[i].split("=");
					if(row.length == 2)
						params.put(row[0], row[1]);
				}
			}
			
			// HTTP response body
			String body = "<NOTHING>";			
			if(fileName.equalsIgnoreCase("Login.asp")) {
				body = "<CONNECT " + HTTPServer.config.serverHost + ">";
				// Also, sending MOTD here
				if(HTTPServer.config.httpShowMOTD)
					body = body + "<MOTD>" + HTTPServer.MOTD + "</MOTD>";
			}
			else if(fileName.equalsIgnoreCase("RequestChannelScheme.asp")) {
				useSnooperFix = true;
				int chanIndex = Channel.indexOf(IRCServer.channels, params.get("Channel"));
				if(chanIndex == -1) // if channel does not exist
					body = "<SCHEME=Pf,Be>";
				else
					body = "<SCHEME=" + IRCServer.channels[chanIndex].scheme + ">";
			}
			else if(fileName.equalsIgnoreCase("Game.asp")) {
				if(params.get("Cmd").equals("Create")) {

					// Registering a new game
					Game game = new Game();
					String name = params.get("Name");
					game.name = name.length() > 29? name.substring(0, 29): name;
					game.password = params.get("Pwd");
					game.loc = params.get("Loc");
					game.gameID = HTTPServer.games.size()+1;
					game.channel = params.get("Chan");
					game.created = (int)(new Date().getTime()/1000);

					game.hosterAddress = HTTPServer.config.forceHosterIP?
							socket.getInetAddress().toString().substring(1): params.get("HostIP");
					game.hosterNickname = params.get("Nick");
					
					// Finally pushing it into the list
					HTTPServer.games.add(game);
					
					// IRC game hosting announcement
					if(HTTPServer.config.announceGameHosting)
						IRCServer.broadcast(":" + HTTPServer.config.serverHost + " NOTICE #"
								+ game.channel +  " :" + game.hosterNickname + " hosting a game: " + game.name, game.channel);
					WNLogger.l.info("<#" + game.channel + "> " + game.hosterNickname + " hosting a game: " + game.name);
					headers = headers + "\r\nSetGameId: : " + game.gameID;
					body = "<NOTHING>";
					
				}
				
				else if(params.get("Cmd").equals("Close")) {
					String gameName = "";
					int gID = Integer.decode(params.get("GameID"));
					for(int i = 0; i < HTTPServer.games.size(); i++) {
						boolean found = false;
						// Looking for a game requested to be closed
						if(HTTPServer.games.get(i).gameID == gID) {
							gameName = HTTPServer.games.get(i).name;
							HTTPServer.games.remove(i);
							found = true;
							break;
						}
						
						if(!found) {
							throw new Exception("Trying to close a non-existant game with id " + gID);
						}
						
						IRCServer.broadcast(":" + HTTPServer.config.serverHost + " NOTICE #"
								+ params.get("Channel") + " :" + gameName + ": game has closed.", params.get("Channel"));
						WNLogger.l.info("<#" + params.get("Channel") + "> " + gameName + ": game closed");
					}
				}
				
				else if(params.get("Cmd").equals("Failed"))
					body = "<NOTHING>";
				else throw new Exception("Unknown command (" + params.get("Cmd") + ")");
			}
			
			else if(fileName.equalsIgnoreCase("GameList.asp")) {
				HTTPServer.cleanUpGames();
				body = /*body +*/"<GAMELISTSTART>\r\n";
				for(int i = 0; i < HTTPServer.games.size(); i++) {
					Game g = HTTPServer.games.get(i);
					if(params.get("Channel").equals(g.channel))
						body = body + "<GAME " + g.name + " " + g.hosterNickname + " "
								+ g.hosterAddress + " " + g.loc + " 1 " + (g.password == null? "0 ": "1 ") +
								+ g.gameID + " 0><BR>\r\n";
				}
				body = body + "<GAMELISTEND>";
			} else if(fileName.equalsIgnoreCase("UpdatePlayerInfo.asp")) {
				// nothing
			} else
				throw new Exception("Invalid file specified: " + fileName);
			
			serveMessage = createResponse(200, body);

		} catch(HTTPServerException eHTTP) {
		} catch(MalformedURLException eUrl) {
			serveMessage = createResponse(500, "Invalid URL");
			WNLogger.l.warning("Invalid URL in HTTP request: " + received.get(0).split(" +")[1]);
		}
		catch(Exception e) {
			serveMessage = createResponse(500, "Internal server error");
			WNLogger.l.warning(e.toString());
			
		} finally {
			// Sending response back to client
			try {
				if(out != null) {
					out.write(serveMessage.getBytes());
					WNLogger.l.finest("Output message: " + serveMessage);
				}
				Thread.sleep(100);
				socket.close();
			} catch(Exception eAnother) {
				eAnother.printStackTrace();
			}
		}
	}
	
	String createResponse(int code, String mesg) {
		return "HTTP/1.0 " + code + (code < 300? " OK": " ERROR")
				+ "\r\n" + headers + (mesg.length() == 0? "": ("\r\nContent-Length: " + (mesg.length())))
				+ "\r\nConnection: close" + (mesg.length() == 0? "": "\r\n\r\n")
				+ mesg + ((useSnooperFix && HTTPServer.config.enableWheatSnooperSchemeFix)? "\n": "\r\n");
	}
	
	final String headersXPoweredBy = "X-Powered-By: FryWormNET-" + FryWormNet.version;
	String headers = headersXPoweredBy;
}

public class HTTPServer extends Thread {
	public static ArrayList<Game> games = new ArrayList<Game>();
	public static ConfigurationManager config;
	public static String MOTD = "";
	
	void readMOTD() {
		try(BufferedReader in = new BufferedReader(new InputStreamReader(
				StreamUtils.getResourceAsStream(config.httpMOTDFileName, this)))) {
			while(true) {
				String line = in.readLine();
				if(line == null)
					break;
				MOTD += line;
			}
		} catch(Exception e) {
			WNLogger.l.warning("Can't read HTTP MOTD file: " + e);
			MOTD = "[MOTD file has failed to read]";
		}
	}
	
	public static void cleanUpGames() {
		for(int i = 0; i < games.size(); i++) {
			// Killing 'too old' games
			if(((int)(new Date().getTime()/1000) - games.get(i).created) > config.gameLifeTime) {
				games.remove(i);
				break;
			}
		}
	}
	
	// HTTP server main thread
	@Override public void run() {
		if(config.httpShowMOTD) readMOTD();
		try(ServerSocket srvSocket = new ServerSocket(config.HTTPPort)) {
			while(true) {
				try {
					Socket socket = srvSocket.accept();
					WNLogger.l.info("Incoming connection (" + socket.getRemoteSocketAddress()
							.toString().substring(1)+ ")");
					(new HTTPServerListener(socket)).start();
				} catch(Exception e) {
					e.printStackTrace();
					WNLogger.l.severe("Unable to accept connection and create server thread: " + e);
					try {
						Thread.sleep(1000);
					} catch(Exception eAnother) {
						eAnother.printStackTrace();
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			WNLogger.l.severe("Can't create HTTP server: " + e);
			WNLogger.l.severe("Exiting now with error status.");
			System.exit(-1);
		}
	}
	
	public HTTPServer(ConfigurationManager c) {
		config = c;
		this.start();
		WNLogger.l.info("Starting HTTP server, listening on port " + config.HTTPPort);
	}
	
}
