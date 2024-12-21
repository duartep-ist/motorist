package pt.ulisboa.tecnico.motorist.common;

import java.io.File;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserKeyFileTest {
	@Test
	@DisplayName("The loaded key should be equal to the stored key")
	public void storeAndLoad() throws Exception {
		String password = "1234";

		File tempFile = File.createTempFile("motorist-test-", "-key.p12");
		tempFile.deleteOnExit();

		UserKeyFile keyFile = new UserKeyFile(tempFile);
		SecretKey originalUserKey = UserKeyFile.generateKey();
		keyFile.storeKey(originalUserKey, password);
		SecretKey loadedUserKey = keyFile.loadKey(password);
		Assertions.assertEquals(originalUserKey, loadedUserKey);
	}
}
