package reso.examples.gobackn;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import reso.common.Message;

public class GBNCCMessage implements Message {
	public final int _seqNb;
	public final long _checksum;
	private final int _isACK;
	public final byte[] _data;
	
	public GBNCCMessage(int seqNb, boolean isACK, byte[] data) {
		_seqNb = seqNb;
		_isACK = isACK ? 1 : 0;
		_data = data;
		_checksum = getChecksum();
	}
	
	public long getChecksum(){
		Checksum crc32 = new CRC32();
		crc32.update(_seqNb);
		crc32.update(_isACK);
		if(_data != null)
			crc32.update(_data, 0, _data.length);
		return crc32.getValue();
	}
	

	@Override
	public int getByteLength() {
		// SEQNO + ISACK + DATA + CHECKSUM
		return Integer.BYTES + Integer.BYTES + (_data == null ? 0 : _data.length) + Long.BYTES;
	}

	public boolean isACK() {
		return _isACK == 1 ? true : false;
	}
}
