package org.calvin.server;

import org.calvin.groupcomm.GroupServer;
import org.calvin.metadata.PartionMgr;
import org.calvin.scheduler.CalvinScheduler;
import org.calvin.storedprocedures.CalvinStoredProcFactory;
import org.calvin.storedprocedures.CalvinStoredProcedure;
import org.vanilladb.core.server.VanillaDb;

import java.util.logging.Logger;

import org.calvin.cache.CacheMgr;

public class Calvin extends VanillaDb{
	
	private static Logger logger = Logger.getLogger(GroupServer.class.getName());
	
	public static GroupServer server;
	public static CacheMgr cache;
	public static PartionMgr partionMgr;
	public static CalvinScheduler scheduler;
	
	public static void init(String dbname, int nodeID, CalvinStoredProcFactory factory, PartionMgr parMgr) {
		server = new GroupServer(nodeID);
		VanillaDb.init(dbname);
		cache = new CacheMgr();
		partionMgr = parMgr;
		scheduler = new CalvinScheduler(factory);
	    
	}
	
	
	
}