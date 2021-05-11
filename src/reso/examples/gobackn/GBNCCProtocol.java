package reso.examples.gobackn;
import java.util.ArrayList;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.common.Message;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.ip.IPLayer;

public class GBNCCProtocol extends AbstractApplication implements IPInterfaceListener {
	
	private final IPLayer _ip;
	private final int _windowSize;
	private ArrayList<Message> _window;
	private Receiver _receiver;
	public static final int GBNCC_PROTOCOL = Datagram.allocateProtocolNumber("GBNCC");
	private Message _ack = null;
	private int _expSeqNb = 0;
	
	int _sendBase = 0;
	int _nextSeqNb = 0;
	
	public GBNCCProtocol(int windowSize, Host host, String name){
		super(host, name);
		_ip = ((IPHost)host).getIPLayer();
		_windowSize = windowSize;
		_ip.addListener(GBNCC_PROTOCOL, this);
	}
	
	public void send(String data, IPAddress dst) throws Exception{
		if(_nextSeqNb < _sendBase + _windowSize){
			
			Message packet = new GBNCCMessage(_nextSeqNb, false, data);
			_window.set(_nextSeqNb - _sendBase, packet);
			
			_ip.send(_ip.getInterfaceByName("eth0").getAddress(), dst, GBNCC_PROTOCOL, packet);
			
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
		
		GBNCCMessage pck = (GBNCCMessage)datagram.getPayload();
		
		if(pck._checksum == pck.getChecksum()){ // Not corrupted
			if(pck.isACK()){
				_sendBase = pck._seqNb + 1;
				if(_sendBase == _nextSeqNb){
					stopTimer();
				}else{
					startTimer();
				}
			}else{
				if(_expSeqNb == pck._seqNb){ 
					
					String data = pck._data;
					if(_receiver != null)
						_receiver.Receive(data);
					_ack = new GBNCCMessage(_expSeqNb, true, null);
					_ip.send(IPAddress.ANY, datagram.src, GBNCC_PROTOCOL, _ack);
					_expSeqNb++;
					
				}else if(_ack != null){
					_ip.send(IPAddress.ANY, datagram.src, GBNCC_PROTOCOL, _ack);
				}
			}
		}else{
			System.out.println("CORRUPTED GBNCC Packet");
		}
	}

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	public void setReceiver(Receiver rcv) {
		_receiver = rcv;
	}
}
