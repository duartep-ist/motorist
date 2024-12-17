package pt.ulisboa.tecnico.motorist.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

public class JSONStreamWriter {
	private OutputStreamWriter writer;

	public JSONStreamWriter(OutputStream stream) throws UnsupportedEncodingException {
		this(new OutputStreamWriter(stream, "UTF-8"));
	}
	public JSONStreamWriter(OutputStreamWriter writer) {
		this.writer = writer;
	}

	public void write(JSONObject object) throws IOException {
		this.writer.write(object.toString() + "\n");
		this.writer.flush();
	}
}
