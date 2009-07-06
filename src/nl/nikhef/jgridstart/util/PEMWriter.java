package nl.nikhef.jgridstart.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

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
	FileUtils.chmod(f, true, true, false, true);
	super.writeObject(obj, algorithm, password, random);
    }

    /** Write object to PEM encrypted */
    public void writeObject(Object obj, PasswordFinder pwf) throws NoSuchAlgorithmException, IOException {
	writeObject(obj, "DESEDE", pwf);
    }
    
    /** Write object to PEM encrypted specifying algorithm */
    public void writeObject(Object obj, String algorithm, PasswordFinder pwf) throws NoSuchAlgorithmException, IOException {
	final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
	writeObject(obj, algorithm, pwf.getPassword(), random);
    }
    
    /** Write object to PEM encrypted using {@link PasswordCache} */
    public void writeObject(Object obj, String msg) throws NoSuchAlgorithmException, IOException {
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
	w.writeObject(obj);
	w.close();
    }

    /** Write single object to PEM specifying provider */
    public static void writeObject(File f, String provider, Object obj) throws IOException {
	PEMWriter w = new PEMWriter(f, provider);
	w.writeObject(obj);
	w.close();
    }
    
    /** Write single object to PEM encrypted using {@link PasswordCache} */
    public static void writeObject(File f, Object obj, String msg) throws NoSuchAlgorithmException, IOException {
	PEMWriter w = new PEMWriter(f);
	w.writeObject(obj, msg);
	w.close();
    }

    // TODO finish variants
}