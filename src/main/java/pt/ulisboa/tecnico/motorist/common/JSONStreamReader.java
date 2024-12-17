package pt.ulisboa.tecnico.motorist.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

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

	public JSONObject read() throws IOException, JSONException {
		return new JSONObject(this.bufferedReader.readLine());
	}
}
