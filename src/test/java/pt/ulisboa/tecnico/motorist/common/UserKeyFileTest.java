package pt.ulisboa.tecnico.motorist.common;

import java.io.File;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test example.
 * TODO: Write tests
 */
public class UserKeyFileTest {
	@Test
	@DisplayName("The loaded key should be equal to the stored key")
	public void storeAndLoad() throws Exception {
		String password = "1234";

		File tempFile = File.createTempFile("motorist-test-", "-key.p12");
		tempFile.deleteOnExit();

		UserKeyFile keyFile = new UserKeyFile(tempFile, password);
		SecretKey originalUserKey = UserKeyFile.generateKey();
		keyFile.storeKey(originalUserKey);
		SecretKey loadedUserKey = keyFile.loadKey();
		Assertions.assertEquals(originalUserKey, loadedUserKey);
	}
}
