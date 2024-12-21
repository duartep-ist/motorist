package pt.ulisboa.tecnico.motorist.common;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

public class SecureDocumentTest {
	private static final SecretKey key = SecureDocument.generateKey();
	private static final JsonObject document = new JsonObject();
	static {
		document.addProperty("pi", Math.PI);
	}

	@Test
	@DisplayName("Encrypting and decrypting does not change the document")
	public void protectAndUnprotect() throws Exception {
		Assertions.assertEquals(
			document.toString(),
			SecureDocument.unprotect(key, SecureDocument.protect(key, document)).toString()
		);
	}

	@Test
	@DisplayName("A protected document is valid")
	public void protectAndCheck() throws Exception {
		Assertions.assertTrue(SecureDocument.check(key, SecureDocument.protect(key, document)));
	}

	@Test
	@DisplayName("A modified protected document is not valid")
	public void protectAndModifyAndCheck() throws Exception {
		byte[] protectedDocument = SecureDocument.protect(key, document);
		protectedDocument[protectedDocument.length-1] ^= 1;
		Assertions.assertFalse(SecureDocument.check(key, protectedDocument));
	}

	@Test
	@DisplayName("A modified protected document can't be unprotected")
	public void protectAndModifyAndUnprotect() throws Exception {
		byte[] protectedDocument = SecureDocument.protect(key, document);
		protectedDocument[protectedDocument.length-1] ^= 1;
		Assertions.assertThrows(SecurityException.class, () -> SecureDocument.unprotect(key, protectedDocument));
	}
}
