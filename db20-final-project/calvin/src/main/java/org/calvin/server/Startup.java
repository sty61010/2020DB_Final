package org.calvin.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.calvin.groupcomm.GroupClient;
import org.calvin.groupcomm.GroupServer;
import org.vanilladb.core.server.VanillaDb;

public class Startup {
	private static Logger logger = Logger.getLogger(GroupServer.class.getName());

	public static void main(String[] args) {
		
		int nodeID = Integer.parseInt(args[0]);
		
		if (logger.isLoggable(Level.INFO))
			logger.info("Initializing Calvin...");
		
		VanillaDb.init("BenchDB");
//		Calvin.init(nodeID);
	}
	
public static void init(int nodeID) {
		
//		int nodeID = nodeID;
		
		if (logger.isLoggable(Level.INFO))
			logger.info("Initializing Calvin...");
		
//		Calvin.init(nodeID);
	}
}