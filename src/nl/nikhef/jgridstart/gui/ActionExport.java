package nl.nikhef.jgridstart.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import nl.nikhef.jgridstart.CertificatePair;
import nl.nikhef.jgridstart.CertificateSelection;
import nl.nikhef.jgridstart.gui.util.URLLauncher;
import nl.nikhef.jgridstart.gui.util.ErrorMessage;
import nl.nikhef.jgridstart.util.PasswordCache.PasswordCancelledException;

/** Export selected certificate to PKCS#12/PEM file */
public class ActionExport extends CertificateAction {
    
    public ActionExport(JFrame parent, CertificateSelection s) {
	super(parent, s);
	putValue(NAME, "Export...");
	putValue(MNEMONIC_KEY, new Integer('E'));
	putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control E"));
	URLLauncher.addAction("export", this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
	logger.finer("Action: "+getValue(NAME));
	JFileChooser chooser = new CertificateFileChooser(false);
	chooser.setDialogTitle("Export the currently selected certificate");
	chooser.setApproveButtonText("Export");
	chooser.setApproveButtonMnemonic('E');
	int result = chooser.showDialog(findWindow(e.getSource()), null);
	if (result == JFileChooser.APPROVE_OPTION) {
	    doExport(e, chooser.getSelectedFile());
	}
    }
    
    /** Export the current certificate to a file
     * 
     * @param f File to export to
     */
    public void doExport(ActionEvent e, File f) {
	CertificatePair cert = getCertificatePair();
	logger.info("Exporting certificate "+cert+" to: "+f);
	try {
	    cert.exportTo(f);
	} catch (PasswordCancelledException e1) {
	    // do nothing
	} catch (Exception e1) {
	    logger.severe("Error exporting certificate "+f+": "+e1);
	    ErrorMessage.error(findWindow(e.getSource()), "Export failed", e1);
	}
    }
}
