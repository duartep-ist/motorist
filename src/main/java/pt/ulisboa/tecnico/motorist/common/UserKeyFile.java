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
	private char[] password;

	public UserKeyFile(File file, String password) {
		this.file = file;
		this.password = password.toCharArray();
	}

	public static SecretKey generateKey() {
		try {
			return KeyGenerator.getInstance("AES").generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
	}

	public void storeKey(SecretKey key) throws IOException {
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

	public SecretKey loadKey() throws IOException, UnrecoverableEntryException, WrongPasswordException {
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
		} catch (IOException e) {
			if (e.getMessage() != null && e.getMessage().contains("UnrecoverableKeyException")) {
				throw new WrongPasswordException(e);
			} else {
				throw e;
			}
		}
	}

	public class WrongPasswordException extends Exception {
		private WrongPasswordException(Throwable cause) {
			super("Can't decrypt key file due to wrong password or corrupted file", cause);
		}
	}
}
