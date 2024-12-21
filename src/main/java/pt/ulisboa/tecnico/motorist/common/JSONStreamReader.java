package pt.ulisboa.tecnico.motorist.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JSONStreamReader {
	private BufferedReader bufferedReader;

	public JSONStreamReader(InputStream stream) throws UnsupportedEncodingException {
		this(new InputStreamReader(stream, "UTF-8"));
	}
	public JSONStreamReader(InputStreamReader streamReader) {
		this(new BufferedReader(streamReader));
	}
	public JSONStreamReader(BufferedReader bufferedReader) {
		this.bufferedReader = bufferedReader;
	}

	public JsonObject read() throws IOException {
		String line = this.bufferedReader.readLine();
		if (line == null) {
			throw new IOException("End of stream");
		}
		if (Debug.ENABLED) System.out.println("Received " + line);
		return JsonParser.parseString(line).getAsJsonObject();
	}
}
