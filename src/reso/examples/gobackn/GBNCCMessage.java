package reso.examples.gobackn;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import reso.common.Message;

public class GBNCCMessage implements Message {
	public final int _seqNb;
	public final long _checksum;
	private final int _isACK;
	public final String _data;
	
	public GBNCCMessage(int seqNb, boolean isACK, String data) {
		_seqNb = seqNb;
		_checksum = getChecksum();
		_isACK = isACK ? 1 : 0;
		_data = data;
	}
	
	public long getChecksum(){
		Checksum crc32 = new CRC32();
		crc32.update(_seqNb);
		crc32.update(_isACK);
		if(_data != null)
			crc32.update(_data.getBytes(), 0, _data.getBytes().length);
		return crc32.getValue();
	}
	

	@Override
	public int getByteLength() {
		// SEQNO + ISACK + DATA + CHECKSUM
		return Integer.BYTES + Integer.BYTES + (_data == null ? 0 : _data.getBytes().length) + Long.BYTES;
	}

	public boolean isACK() {
		return _isACK == 1 ? true : false;
	}
}
