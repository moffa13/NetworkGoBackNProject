package reso.examples.gobackn;

import reso.common.Message;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.ip.IPLayer;

public class GBNReceiver implements IPInterfaceListener {
	
	private final Receiver _receiver;
	private final IPLayer _ip;
	public static final int ACK_PROTO = Datagram.allocateProtocolNumber("ACK");
	private Message _ack = null;
	
	private int _expSeqNb = 0;
	
	
	public GBNReceiver(IPLayer ip, AppReceiver receiver) {
		_receiver = receiver;
		_ip = ip;
		ip.addListener(GBNSender.GBN_PROTOCOL, this);
	}

	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
		GBNPacket pck = (GBNPacket)datagram.getPayload();
		if(pck._checksum == pck.getChecksum() && _expSeqNb == pck._nextSeqNb){ // Not corrupted
		
			String data = pck._data;
			_receiver.Receive(data);
			_ack = new ACKPacket(_expSeqNb);
			_ip.send(IPAddress.ANY, datagram.src, ACK_PROTO, _ack);
			_expSeqNb++;
			
		}else if(_ack != null){
			_ip.send(IPAddress.ANY, datagram.src, ACK_PROTO, _ack);
		}
		
		if(pck._checksum != pck.getChecksum()){
			System.out.println("CORRUPTED PACKET");
		}
	}

}
