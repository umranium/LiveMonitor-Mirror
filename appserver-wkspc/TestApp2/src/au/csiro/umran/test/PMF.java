package au.csiro.umran.test;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import au.csiro.umran.test.model.ViewingSession;

public class PMF {
	
    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PMF() {}

    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }
    
    public static ViewingSession getSession(PersistenceManager persistenceManager) {
    	
        ViewingSession session = null;
        
        try {
        	session = persistenceManager.getObjectById(ViewingSession.class, ViewingSession.DEF_SESSION_KEY);
        } catch (Exception e) {
        	//	ignore
        }
        
        if (session==null) {
        	session = new ViewingSession();
        	session = persistenceManager.makePersistent(session);
        }
        
        return session;
    }

}
