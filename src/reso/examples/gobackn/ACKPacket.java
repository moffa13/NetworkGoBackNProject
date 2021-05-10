package reso.examples.gobackn;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import reso.common.Message;

public class ACKPacket implements Message {
	public final int _expSeqNb;
	public final long _checksum;
	
	public ACKPacket(int expSeqNb) {
		_expSeqNb = expSeqNb;
		_checksum = getChecksum();
	}
	
	public long getChecksum(){
		Checksum crc32 = new CRC32();
		crc32.update(_expSeqNb);
		return crc32.getValue();
	}
	

	@Override
	public int getByteLength() {
		return Integer.BYTES;
	}
}
