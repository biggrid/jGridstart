package nl.nikhef.jgridstart.passwordcache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import nl.nikhef.jgridstart.osutils.FileUtils;

import org.bouncycastle.openssl.PasswordFinder;

/** PEM file writer that works directory on files, integrated with {@link PasswordCache}. */
public class PEMWriter extends org.bouncycastle.openssl.PEMWriter {
    
    private File f = null;
    
    /** Create PEMWriter for a file */
    public PEMWriter(File out) throws IOException {
	super(new FileWriter(out));
	f = out;
    }
    
    /** Create PEMWriter and specify provider */
    public PEMWriter(File out, String provider) throws IOException {
	super(new FileWriter(out), provider);
	f = out;
    }
    
    /** Write object encrypted to PEM encrypted.
     * <p>
     * This sets the file permissions to user-accessible only when
     * the supplied object contains a private key. For the rest it
     * just calls its parent,
     * {@link org.bouncycastle.openssl.PEMWriter#writeObject}.
     * <p>
     * 
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object obj, String algorithm, char[] password, SecureRandom random) throws IOException {
	// Close for others, keep writable for us until we are done
	FileUtils.chmod(f, true, true, false, true);
	super.writeObject(obj, algorithm, password, random);
    }

    /** Write object to PEM encrypted */
    public void writeObject(Object obj, PasswordFinder pwf) throws NoSuchAlgorithmException, IOException {
	writeObject(obj, "DESEDE", pwf);
    }
    
    /** Write object to PEM encrypted specifying algorithm */
    public void writeObject(Object obj, String algorithm, PasswordFinder pwf) throws NoSuchAlgorithmException, IOException {
	// Best to call SecureRandom() instead of getInstance, such that we can
	// get the best available algorithm, e.g. NativePRNG corresponding to
	// /dev/(u)random on Linux, when available, instead of built-in
	// SHA1PRNG).
	final SecureRandom random = new SecureRandom();
	// Call nextBytes to force seeding it
	random.nextBytes(new byte[1]);
	char[] pw = pwf.getPassword();
	if (pw==null) throw new PasswordCancelledException();
	writeObject(obj, algorithm, pw, random);
    }
    
    /** Write object to PEM encrypted using {@link PasswordCache} */
    public void writeObject(Object obj, String msg) throws NoSuchAlgorithmException, IOException, PasswordCancelledException {
	PasswordFinder pwf = PasswordCache.getInstance().getEncryptPasswordFinder(msg, f.getCanonicalPath());
	writeObject(obj, pwf);
    }

    /** Write object to PEM encrypted using {@link PasswordCache} specifying algorithm */
    public void writeObject(Object obj, String algorithm, String msg) throws NoSuchAlgorithmException, IOException {
	PasswordFinder pwf = PasswordCache.getInstance().getEncryptPasswordFinder(msg, f.getCanonicalPath());
	writeObject(obj, algorithm, pwf);
    }
    
    /** Write single object to PEM */
    public static void writeObject(File f, Object obj) throws IOException {
	PEMWriter w = new PEMWriter(f);
	try {
	    w.writeObject(obj);
	} finally {
	    w.close();
	}
    }

    /** Write single object to PEM specifying provider */
    public static void writeObject(File f, String provider, Object obj) throws IOException {
	PEMWriter w = new PEMWriter(f, provider);
	try {
	    w.writeObject(obj);
	} finally {
	    w.close();
	}
    }
    
    /** Write single object to PEM encrypted using {@link PasswordCache} */
    public static void writeObject(File f, Object obj, String msg) throws NoSuchAlgorithmException, IOException, PasswordCancelledException {
	PEMWriter w = new PEMWriter(f);
	try {
	    w.writeObject(obj, msg);
	} finally {
	    w.close();
	    FileUtils.chmod(f, true, false, false, true);
	}
    }
    
    /** Wrote single object to PEM encrypted with specified password.
     * <p>
     * Stores password in {@link PasswordCache} as well.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    public static void writeObject(File f, Object obj, final char[] pw) throws NoSuchAlgorithmException, IOException {
	PEMWriter w = new PEMWriter(f);
	PasswordCache.getInstance().set(f.getCanonicalPath(), pw);
	try {
	    w.writeObject(obj, new PasswordFinder() {
		public char[] getPassword() { return pw; }
	    });
	} finally {
	    w.close();
	    FileUtils.chmod(f, true, false, false, true);
	}
    }

    // TODO finish variants
}
