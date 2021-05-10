package reso.examples.gobackn;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import reso.common.Message;

public class GBNPacket implements Message {
	
	public final int _nextSeqNb;
	public final String _data;
	public final long _checksum;
	
	public GBNPacket(int nextSeqNb, String data) {
		_nextSeqNb = nextSeqNb;
		_data = data;
		_checksum = getChecksum();
	}
	
	public long getChecksum(){
		Checksum crc32 = new CRC32();
		crc32.update(_nextSeqNb);
		crc32.update(_data.getBytes(), 0, _data.getBytes().length);
		return crc32.getValue();
	}
	

	@Override
	public int getByteLength() {
		return Integer.BYTES + _data.getBytes().length + Long.BYTES;
	}

}
