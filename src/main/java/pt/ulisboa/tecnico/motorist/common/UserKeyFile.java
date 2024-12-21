package pt.ulisboa.tecnico.motorist.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class UserKeyFile {
	private File file;

	public UserKeyFile(File file) {
		this.file = file;
	}

	public static SecretKey generateKey() {
		try {
			return KeyGenerator.getInstance("AES").generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
	}

	public boolean exists() {
		return this.file.exists();
	}

	public void storeKey(SecretKey key, String passwordString) throws IOException {
		char[] password = passwordString.toCharArray();
		try {
			// Initialize an empty KeyStore
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(null, password);

			// Store the key in the KeyStore
			KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(key);
			KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(password);
			keyStore.setEntry("userKey", entry, protectionParameter);

			// Store the KeyStore in the file
			try (FileOutputStream stream = new FileOutputStream(file)) {
				keyStore.store(stream, password);
			}
		} catch (KeyStoreException e) {
			throw new Error(e);
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		} catch (CertificateException e) {
			throw new Error(e);
		}
	}

	/**
	 * Loads the user key from the file
	 * @param passwordString
	 * @return The user key or `null` if the password is wrong
	 * @throws IOException
	 */
	public SecretKey loadKey(String passwordString) throws IOException {
		char[] password = passwordString.toCharArray();
		try {
			// Load the KeyStore from the file
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			try (FileInputStream fis = new FileInputStream(file)) {
				keyStore.load(fis, password);
			}

			// Load the key from the KeyStore
			KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(password);
			return ((KeyStore.SecretKeyEntry) keyStore.getEntry("userKey", protectionParameter)).getSecretKey();
		} catch (KeyStoreException e) {
			throw new Error(e);
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		} catch (CertificateException e) {
			throw new Error(e);
		} catch (UnrecoverableEntryException e) {
			return null;
		} catch (IOException e) {
			if (e.getMessage() != null && e.getMessage().contains("UnrecoverableKeyException")) {
				return null;
			} else {
				throw e;
			}
		}
	}
}
