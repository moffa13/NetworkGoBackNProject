package reso.examples.gobackn;

public class RawChunkMessage {
	
	public final boolean _lastMessage;
	public final byte _data[];
	
	public RawChunkMessage(byte data[], boolean lastMessage) {
		_data = data;
		_lastMessage = lastMessage;
	}
}
