package nl.nikhef.jgridstart.passwordcache;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import org.bouncycastle.openssl.PasswordFinder;

/** Class that caches passwords for a limited time so that the user doesn't
 * need to type in the password everytime.
 * <p>
 * TODO move swing stuff into gui, implement cli as well, auto-select cli/gui
 *      this can be done by implementing this class with callbacks for asking
 *      the user for passwords. Different UIs can then use their own callbacks,
 *      and even the tests can use this to test easily without user intervention.
 * 
 * @author wvengen
 */
public class PasswordCache {
    
    static private Logger logger = Logger.getLogger("nl.nikhef.passwordcache");
    
    /** UI: no questions asked, returns null if password isn't in cache */
    public static final int UI_NONE = 0;
    /** UI: graphical, pops up a dialog when password isn't in cache */
    public static final int UI_GUI = 1;
    /** TODO UI: console, asks for a password on the console */
    public static final int UI_CLI = 2;
    /** user interface backend, one of UI_* */
    protected int ui = UI_GUI;

    
    /** the actual passwords */
    protected HashMap<String, char[]> passwords = null;
    /** list of forget-timers for each password */
    protected HashMap<String, Timer> timers = null;
    /** parent frame for dialogs */
    protected JFrame parent = null;
    /** number of seconds after which passwords are forgotten */
    protected int timeout = 5 * 60;
    /** see {@link #setAlwaysAskForEncrypt} */
    protected boolean alwaysAskForEncrypt = true;
    
    protected PasswordCache() {
	passwords = new HashMap<String, char[]>();
	timers = new HashMap<String, Timer>(); 
    }
    
    /** Reference to singleton instance */
    protected static PasswordCache instance = null;
    /** Retrieve singleton object */
    static public PasswordCache getInstance() {
	if (instance==null) {
	    instance = new PasswordCache();
	}
	return instance;
    }
    
    /** Set the parent for dialogs */
    public void setParent(JFrame parent) {
	this.parent = parent;
    }
    /** Set the timeout after which passwords are forgotten again.
     * <p>
     * Also updates timeout of currently stored passwords.
     * 
     * @param s timeout in number of seconds, or <0 to keep forever
     */
    public void setTimeout(int s) {
	this.timeout = s;
	// update timeout of current passwords
	for (String loc: timers.keySet()) {
	    touch(loc);
	}
    }
    /** Return the current password timeout.
     * @see #setTimeout */
    public int getTimeout() {
	return this.timeout;
    }

    /** Set the user-interface backend.
     * 
     * @param ui One of UI_NONE, UI_GUI and UI_CLI
     */
    public void setUI(int ui) {
	this.ui = ui;
    }
    /** Return the current user-interface backend. */
    public int getUI() {
	return this.ui;
    }
    
    /** Invalidate a cache entry */
    public void invalidate(String loc) {
	// overwrite password for a little security
	if (passwords.containsKey(loc)) {
	    Arrays.fill(passwords.get(loc), '\0');
	    passwords.remove(loc);
	    logger.finest("Password removed for "+loc);
	}
	// remove timer stuff
	if (timers.containsKey(loc)) {
	    timers.get(loc).cancel();
	    timers.remove(loc);
	}
    }
    
    /** Return the password for the specified location to decrypt files
     * <p>
     * Each item has a unique reference to a required password consisting of
     * a location string.
     * 
     * @param msg Message to present to the user when asking for a password.
     *            Actual message is "Enter password to unlock "+msg
     * @param loc Location string
     * @return password
     * @throws PasswordCancelledException 
     */
    public char[] getForDecrypt(String msg, String loc) throws PasswordCancelledException {
	// try if entry exists
	if (passwords.containsKey(loc)) {
	    touch(loc);
	    return passwords.get(loc);
	}
	// else ask from user
	char[] pw = null;
	switch(ui) {
	case UI_GUI:
	    // ask for password
	    // TODO focus is moved from password entry to ok button!!!
	    JOptionPane pane = new JOptionPane();
	    pane.setMessageType(JOptionPane.PLAIN_MESSAGE);
	    pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
	    msg = msg.replace("\n", "<br>");
	    JLabel lbl = new JLabel("<html><body>Enter password to unlock "+msg+"</body></html>");
	    final JPasswordField pass = new JPasswordField(25);
	    pane.setMessage(new Object[] {lbl, pass});
	    JDialog dialog = pane.createDialog(parent, "Enter password");
	    logger.fine("Requesting decryption password for "+loc);
	    dialog.setName("jgridstart-password-entry-decrypt");
	    optionPaneSetFocus(pass);
	    dialog.setVisible(true);
	    if (((Integer)pane.getValue()) != JOptionPane.OK_OPTION) {
		logger.fine("Dencryption password request cancelled for "+loc);
		throw new PasswordCancelledException();
	    }
	    // store password and zero out for a little security
	    pw = pass.getPassword();
	    break;
	case UI_CLI:
	    logger.severe("UI_CLI not implemented");
	    return null;
	case UI_NONE:
	    logger.fine("Decryption password not present for "+loc);
	    return null;
	}
	if (pw!=null) {    
	    set(loc, pw);
	    logger.fine("Decryption password entered for "+loc);
	    return pw;
	}
	logger.fine("Decryption password request cancelled for "+loc);
	return null;
    }
    
    /** Return a new password for the specified location.
     * <p>
     * Also ask for a password and require to enter twice for verification.
     * This is for encrypting data.
     * The password is remembered for the case it is read again.
     *
     * @param msg Description presented to the user when asking for a password.
     * @param loc Location string
     * @return password
     */
    public char[] getForEncrypt(String msg, String loc) throws PasswordCancelledException {
	// always ask for password when writing, unless option set and present
	if (!alwaysAskForEncrypt && passwords.containsKey(loc)) {
	    touch(loc);
	    return passwords.get(loc);
	}
	// TODO focus is moved from password entry to ok button!!!
	final JOptionPane pane = new JOptionPane();
	pane.setMessageType(JOptionPane.PLAIN_MESSAGE);
	pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
	msg = msg.replace("\n", "<br>");
	JLabel lbl1 = new JLabel("<html><body>"+msg+"</body></html>");
	JLabel lbl2 = new JLabel("Please enter the same password and avoid mistakes.");
	JLabel lbl3 = new JLabel("<html><body><i>passwords do not match, try again.</i></body></html>");
	JLabel lbl4 = new JLabel("<html><body><i>password cannot be empty, try again.</i></body></html>");
	final JPasswordField pass1 = new JPasswordField(25);
	final JPasswordField pass2 = new JPasswordField(25);
	lbl3.setVisible(false);
	lbl4.setVisible(false);
	optionPaneSetFocus(pass1);
	do {
	    logger.fine("Requesting encryption password for "+loc);
	    pane.setMessage(new Object[] {lbl1, lbl2, lbl3, lbl4, pass1, pass2});
	    JDialog dialog = pane.createDialog(parent, "Enter password");
	    dialog.setName("jgridstart-password-entry-encrypt");
	    dialog.pack();
	    dialog.setVisible(true);
	    // handle cancel
	    if ( ((Integer)pane.getValue()) != JOptionPane.OK_OPTION ) {
		Arrays.fill(pass1.getPassword(), '\0');
		Arrays.fill(pass2.getPassword(), '\0');
		logger.fine("Encryption password request cancelled for "+loc);
		throw new PasswordCancelledException();
	    }
	    // If password is empty prompt that one, otherwise they didn't match
	    if (pass1.getPassword().length==0)
		lbl4.setVisible(true);
	    else
		lbl3.setVisible(true);
	} while (!Arrays.equals(pass1.getPassword(), pass2.getPassword()) ||
	         pass1.getPassword().length==0); 
	// store password and zero out for a little security
	Arrays.fill(pass2.getPassword(), '\0');
	char[] pw1 = pass1.getPassword();
	set(loc, pw1);
	return pw1;
    }

    /** Set a password for a location.
     * <p>
     * Should not be used normally because the user is the one to give the password.
     * For testing this is convenient. 
     * 
     * @param loc Location string
     * @param pw password to set
     */
    public void set(String loc, char[] pw) {
	passwords.put(loc, pw);
	touch(loc);
	logger.fine("Password set for "+loc);
    }
    
    /** Completely clear the cache so that no passwords are present. */
    public void clear() {
	Set<String> locations = new HashSet<String>(passwords.keySet());
	for (String loc: locations) {
	    invalidate(loc);
	}
    }
    
    /** Reset the forget timeout for a location. Password mustexist already. */
    protected void touch(String loc) {
	if (!passwords.containsKey(loc)) return;
	if (timers.containsKey(loc)) {
	    timers.get(loc).cancel();
	    timers.remove(loc);
	}
	if (timeout>0) new ForgetTask(loc);
    }
    
    /** Set whether or not to always ask a password for encryption. Usually
     * you would want this, but for machine-generated temporary passwords this
     * may not be desired. */
    public boolean setAlwaysAskForEncrypt(boolean alwaysAsk) {
	boolean old = alwaysAskForEncrypt;
	alwaysAskForEncrypt = alwaysAsk;
	return old;
    }
    
    /** Return a {@link PasswordFinder} that asks the user for a password when
     * encrypting a file. The password is retrieved using {@link #getForEncrypt}. 
     * 
     * @param msg Description on what this key is about
     * @param loc Location string
     * @return a new PasswordFinder
     */
    protected CachePasswordFinder getEncryptPasswordFinder(String msg, String loc) {
	return new CachePasswordFinder(true, msg, loc);
    }
    /** Return a {@link PasswordFinder} that returns the cached password, or else
     * asks the user to provide one. The password is retrieved using
     * {@link #getForDecrypt}. 
     * 
     * @param msg Description on what this key is about
     * @param loc Location string
     * @return a new PasswordFinder
     */
    protected CachePasswordFinder getDecryptPasswordFinder(String msg, String loc) {
	return new CachePasswordFinder(false, msg, loc);
    }
    
    /** A {@link PasswordFinder} interface for a password */
    private class CachePasswordFinder implements PasswordFinder {
	private String msg, loc;
	private boolean write;
	@SuppressWarnings("unused") // we don't use it currently
	public boolean wasCancelled = false;
	// TODO implement guess mode (all previous passwords) when no hardware device
	public CachePasswordFinder(boolean write, String msg, String loc) {
	    this.write = write;
	    this.msg = msg;
	    this.loc = loc;
	}
        public char[] getPassword() {
            try {
        	if (write)
        	    return getForEncrypt(msg, loc);
        	else
        	    return getForDecrypt(msg, loc);
            } catch (PasswordCancelledException e) {
        	// store cancel state for exception handling later
        	wasCancelled = true;
        	return null;
            }
        }
    }

    /** Timer task that removes the cached password after timeout */
    protected class ForgetTask extends TimerTask {
	protected String loc;
	public ForgetTask(String loc) {
	    this.loc = loc;
	    update();
	}
	/** update or create new timer task to forget password
	 * after the default timeout */
	public void update() {
	    if (timers.containsKey(loc))
		timers.get(loc).cancel();
	    Timer t = new Timer();
	    t.schedule(this, timeout*1000);
	    timers.put(loc, t);
	    logger.finest("Password timeout reset for "+loc);
	}
	@Override
	public void run() {
	    invalidate(loc);
	}
    }
    
    /** TODO document */
    public static boolean isPasswordWrongException(Throwable e) {
	if (e.getMessage()==null) return false;
	// Since readPEM "throws IOException" the specific information
	// that it might have been a PasswordException is lost :(
	// So now I have to parse the message string ...
	// In addition to this, some weird errors can occur with wrong passwords. These
	// have been found by experience, and if they have high enough a probability to
	// occur when the password is wrong, they're added here.
	return e.getMessage().contains("wrong password") ||
	       e.getMessage().contains("check password") ||
               e.getMessage().contains("java.lang.String cannot be cast to java.lang.Integer");
    }
    /** @see #isPasswordWrongException(Throwable) */
    public static boolean isPasswordWrongException(Exception e) {
	return isPasswordWrongException((Throwable)e);
    }
    /** TODO document */
    public static boolean isPasswordNotSuppliedException(Throwable e) {
	if (e.getMessage()==null) return false;
	// Note: we can get either of these
	return (e.getMessage().contains("No password finder specified") || 
	        e.getMessage().contains("no PasswordFinder specified"));
    }
    /** @see #isPasswordNotSuppliedException(Throwable) */
    public static boolean isPasswordNotSuppliedException(Exception e) {
	return isPasswordNotSuppliedException((Throwable)e);
    }
    /** TODO document */
    public static boolean isPasswordCancelledException(Throwable e) {
	if (e instanceof PasswordCancelledException) return true;
	if (e.getMessage()==null) return false;
	return e.getMessage().contains("Password is null");
    }
    /** @see #isPasswordCancelledException(Throwable) */
    public static boolean isPasswordCancelledException(Exception e) {
	return isPasswordCancelledException((Throwable)e);
    }
    
    /** Helper method to set the focus component of a {@linkplain JOptionPane}.
     * <p>
     * Thank you, <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574">Java bug 5018574</a>.
     */
    protected void optionPaneSetFocus(JComponent c) {
	c.addHierarchyListener(new HierarchyListener() {
	    public void hierarchyChanged(HierarchyEvent e) {
		final Component c = e.getComponent();
		if (c.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
		    Window toplevel = SwingUtilities.getWindowAncestor(c);
		    toplevel.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
			    c.requestFocus();
			}
		    });
		}
	    }
	});    
    }
}
