package reso.examples.gobackn;
import java.util.ArrayList;

import reso.common.Message;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.ip.IPLayer;

public class GBNSender implements IPInterfaceListener {
	
	private final IPLayer _ip;
	private final IPAddress _dst;
	private final int _windowSize;
	private ArrayList<Message> _window;
	public static final int GBN_PROTOCOL = Datagram.allocateProtocolNumber("GBN");
	
	int _sendBase = 0;
	int _nextSeqNb = 0;
	
	public GBNSender(int windowSize, IPLayer ip, IPAddress dst){
		_ip = ip;
		_dst = dst;
		_windowSize = windowSize;
		_ip.addListener(GBNReceiver.ACK_PROTO, this);
	}
	
	public void send(String data) throws Exception{
		if(_nextSeqNb < _sendBase + _windowSize){
			
			Message packet = new GBNPacket(_nextSeqNb, data);
			_window.set(_nextSeqNb - _sendBase, packet);
			
			_ip.send(_ip.getInterfaceByName("eth0").getAddress(), _dst, GBN_PROTOCOL, packet);
			
			if(_sendBase == _nextSeqNb){
				startTimer();
			}
			
			_nextSeqNb++;
		}else{
			
		}
	}

	private void startTimer() {
		// TODO Auto-generated method stub
		
	}
	
	private void stopTimer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
		ACKPacket pck = (ACKPacket)datagram.getPayload();
		if(pck._checksum == pck.getChecksum()){ // Not corrupted
			_sendBase = pck._expSeqNb + 1;
			if(_sendBase == _nextSeqNb){
				stopTimer();
			}else{
				startTimer();
			}
		}else{
			System.out.println("CORRUPTED ACK PACKET");
		}
	}
}
