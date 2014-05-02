package nl.nikhef.jgridstart.gui;

import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.UIManager;

import nl.nikhef.jgridstart.gui.util.ErrorMessage;
import nl.nikhef.jgridstart.logging.LogHelper;
import nl.nikhef.jgridstart.logging.LogWindowHandler;
import nl.nikhef.jgridstart.util.GeneralUtils;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

/** Graphical user-interface main program */
public class Main {

    // setup logging
    // First need to create a new AppContext(). This is necessary since 1.7.0_25
    // See https://grid.sara.nl/issues/view.php?id=4121 and
    // http://bugs.java.com/view_bug.do?bug_id=8017776
    // We implement the workaround mentioned in
    // http://stackoverflow.com/questions/17275259/nullpointerexception-in-invokelater-while-running-through-java-webstart
    static {
	if(sun.awt.AppContext.getAppContext() == null){
	    SunToolkit.createNewAppContext();
	}
	LogHelper.setupLogging(false);
    }
    static private Logger logger = Logger.getLogger("nl.nikhef.jgridstart.gui");

    /** graphical user-interface entry point */
    public static void main(String[] args) {
	// we want to receive log messages in the logging window
	//   the logging window is for the graphical user-interface only, so it
	//   is added as a handler here, as an extra gui-feature
	Logger.getLogger("").addHandler(LogWindowHandler.getInstance());
	// log information about this computer
	LogHelper.logEnvironment();
	
	// load system properties if not yet set, not fatal if it fails
	try {
	    GeneralUtils.loadConfig();
	} catch (IOException e) {
	    logger.warning("Could not load configuration:"+e);
	    ErrorMessage.logException(e);
	}
	// Schedule a job for the event-dispatching thread:
	// creating and showing this application's GUI.
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		try {
		    createAndShowGUI();
		} catch(Throwable e) {
		    ErrorMessage.internal(null, e);
		}
	    }
	});
    }

    private static void createAndShowGUI() {
	// use system look and feel for known-good OSes only
	if (System.getProperty("os.name").startsWith("Win") ||
		System.getProperty("os.name").startsWith("Mac")) {
	    try {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch(Exception e) { }
	}
	
	JGSFrame frame = new JGSFrame();
	frame.setDefaultCloseOperation(JGSFrame.EXIT_ON_CLOSE);

	frame.pack();
	frame.setVisible(true);
    }

}
