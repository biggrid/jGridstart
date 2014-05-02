package nl.nikhef.jgridstart.gui;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;

import nl.nikhef.jgridstart.gui.util.URLLauncherCertificate;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/** Show "About" dialog */
public class ActionAbout extends AbstractAction {

    static private Logger logger = Logger.getLogger("nl.nikhef.jgridstart.gui");

    public ActionAbout(JFrame parent) {
	super();
	putValue(NAME, "About...");
	putValue(MNEMONIC_KEY, new Integer('A'));
	URLLauncherCertificate.addAction("about", this);
    }

    public void actionPerformed(ActionEvent e) {
	logger.finer("Action: "+getValue(NAME));
		
	final JEditorPane editorPane = new JEditorPane();
	editorPane.setContentType("text/html");
	editorPane.setEditable(false);
	editorPane.setOpaque(false);
	editorPane.setText("<html><h1>jGridstart</h1>" +
		"jGridstart version: <b>" +
		System.getProperty("jgridstart.version") +
		" (rev " +
		System.getProperty("jgridstart.revision") +
		")</b><br>" +
		"Java runtime environment version: <b>" +
		System.getProperty("java.version") +
		"</b><br><br>" +
		"<i>JGridstart is an open source project,<br>" +
		"licensed under the Apache 2.0 license.<br>" +
		"It was developed at Nikhef as part of<br>" +
		"the BiG Grid project.</i>" +
		"<br>" +
		"<h3><a href=\"http://jgridstart.nikhef.nl/\">http://jgridstart.nikhef.nl/</a></h3></html>");

	editorPane.addHyperlinkListener(
	    new HyperlinkListener() {
		public void hyperlinkUpdate(HyperlinkEvent e) {
		    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			URLLauncherCertificate.openURL( e.getURL() );
		    }
		}
	    });

	JOptionPane.showMessageDialog(null,
				      editorPane,
				      "About jGridstart",
				      JOptionPane.INFORMATION_MESSAGE);
    }
}
