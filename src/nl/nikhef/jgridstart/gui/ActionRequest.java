package nl.nikhef.jgridstart.gui;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.jdesktop.swingworker.SwingWorker;

import nl.nikhef.jgridstart.CertificatePair;
import nl.nikhef.jgridstart.CertificateSelection;
import nl.nikhef.jgridstart.CertificateStore;
import nl.nikhef.jgridstart.gui.util.BareBonesActionLaunch;
import nl.nikhef.jgridstart.gui.util.TemplateWizard;

public class ActionRequest extends AbstractAction {
    
    static private Logger logger = Logger.getLogger("nl.nikhef.jgridstart.gui");
    protected JFrame parent = null;
    protected CertificateStore store = null;
    protected CertificateSelection selection = null;
    
    public ActionRequest(JFrame parent, CertificateStore store, CertificateSelection selection) {
	super();
	this.parent = parent;
	this.store = store;
	this.selection = selection;
	putValue(NAME, "Request new...");
	putValue(MNEMONIC_KEY, new Integer('R'));
	BareBonesActionLaunch.addAction("request", this);
    }
    
    public void actionPerformed(ActionEvent e) {
	logger.finer("Action: "+getValue(NAME));
	TemplateWizard dlg = new RequestWizard();
	Properties p = new Properties();
	p.setProperty("surname", "Klaassen");
	p.setProperty("givenname", "Piet");
	dlg.setData(p);
	dlg.setVisible(true);
    }
    
    /** Wizard that asks the user for information and generates the certificate */
    protected class RequestWizard extends TemplateWizard implements TemplateWizard.PageListener {
	
	/** the resulting CertificatePair, or null if not yet set */
	protected CertificatePair cert = null;
	/** working thread */
	protected SwingWorker<Void, String> worker = null;
	
	public RequestWizard() {
	    super(parent);
	    pages.add(getClass().getResource("certificate_request_01.html"));
	    pages.add(getClass().getResource("certificate_request_02.html"));
	    pages.add(getClass().getResource("certificate_request_03.html"));
	    setHandler(this);
	}
	
	/** called when page in wizard was changed */
	public void pageChanged(TemplateWizard w, int page) {
	    // stop worker on page change when needed
	    if (worker!=null) {
		worker.cancel(true);
		worker = null;
	    }
	    if (page==2) {
		// on page two we need to execute the things
		worker = new GenerateWorker(w);
		worker.execute();
		// go next only when all actions are finished
		nextAction.setEnabled(false);
	    }
	    if (page==3) {
		// quit wizard
		setVisible(false);
		dispose();
		// and select the newly created certificate in the main window
		int index = store.indexOf(cert);
		selection.setSelection(index);
	    }
	}

	/** worker thread for generation of a certificate */
	protected class GenerateWorker extends SwingWorker<Void, String> {
	    /** gui element to refresh on update; don't use in worker thread! */
	    protected TemplateWizard w;
	    protected Properties p;

	    public GenerateWorker(TemplateWizard w) {
		super();
		this.w = w;
		// use clone to avoid synchronisation issues; not really tested though
		p = (Properties)w.data().clone();
	    }

	    /** worker thread that generates the certificate, etc. */
	    @Override
	    protected Void doInBackground() throws Exception {
		try {
		    // Generate a keypair and certificate signing request
		    // TODO make this configurable
		    // TODO demo/tutorial DN
		    if (cert==null) {
			p.setProperty("subject",
			    "O=dutchgrid, O=users, " +
			    "O="+p.getProperty("org")+", " +
			    "CN="+p.getProperty("givenname")+
			          " "+p.getProperty("surname"));
			cert = store.generateRequest(p);
			// now that request has been generated, lock fields
			// used for that since they are in the request now
			publish("lock.org");
			publish("lock.givenname");
			publish("lock.surname");
			// update gui
			publish("state.keypair");
			publish("state.gencsr");
		    }
		    // TODO only upload if not yet done
		    cert.uploadRequest();
		    publish("state.submitcsr");
		    cert.downloadCertificate();
		    publish("state.approved");
		    publish("state.finished");
		} catch (Exception e) {
		    // TODO handle er
		    e.printStackTrace();
		    throw(e);
		}
		return null;
	    }

	    /** process publish() event from worker thread. This updates the
	     * gui in the sense that a property is added to the TemplateWizard
	     * and it is refreshed. This gives a template the opportunity to
	     * change the display based on a property (e.g. a checkbox) */
	    protected void process(List<String> keys) {
		// update content pane
		for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
		    String key = it.next();
		    w.data().setProperty(key, "true");
		    if (key.equals("state.finished"))
			nextAction.setEnabled(true);
		}
		w.refresh();
	    }
	}
    }
}
