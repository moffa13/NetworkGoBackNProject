package reso.examples.gobackn;
import java.util.ArrayList;

import reso.common.AbstractApplication;
import reso.common.AbstractTimer;
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
	public static final int TIMER_RESEND_INTERVAL = 20;  // 20 might not be good value
	private Message _ack = null;
	private final IPAddress _dst;
	
	// As a receiver
	private int _expSeqNb = 0;
	
	// As a sender
	private int _sendBase = 0;
	private int _nextSeqNb = 0;
	
	private AbstractTimer _resendTimer;
	
	public GBNCCProtocol(int windowSize, Host host, IPAddress dst, String name){
		super(host, name);
		_ip = ((IPHost)host).getIPLayer();
		_dst = dst;
		_windowSize = windowSize;
		_resendTimer = new AbstractTimer(host.getNetwork().getScheduler(), TIMER_RESEND_INTERVAL, false) {
			
			@Override
			protected void run() throws Exception {
				timeout();				
			}
		};
	}
	
	public void send(byte[] data) throws Exception{
		if(_nextSeqNb < _sendBase + _windowSize){
			
			Message packet = new GBNCCMessage(_nextSeqNb, false, data);
			_window.set(_nextSeqNb - _sendBase, packet);
			
			_ip.send(_ip.getInterfaceByName("eth0").getAddress(), _dst, GBNCC_PROTOCOL, packet);
			
			if(_sendBase == _nextSeqNb){
				startTimer();
			}
			
			_nextSeqNb++;
		}else{
			//// ?? BLOCK APP
		}
	}

	private void startTimer() {
		if(_resendTimer.isRunning()) stopTimer();
		_resendTimer.start();		
	}
	
	private void stopTimer() {
		_resendTimer.stop();
	}
	
	private void timeout() {
		
		startTimer();
		
		for(Message m : _window){
			try {
				_ip.send(_ip.getInterfaceByName("eth0").getAddress(), _dst, GBNCC_PROTOCOL, m);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
		
		GBNCCMessage pck = (GBNCCMessage)datagram.getPayload();
		
		if(pck._checksum == pck.getChecksum()){ // Not corrupted
			if(pck.isACK()){ // ACK Packet is received from receiver
				_sendBase = pck._seqNb + 1;
				if(_sendBase == _nextSeqNb){
					stopTimer();
				}else{
					startTimer();
				}
			}else{ // Data is received from the sender (we are receiver in that case)
				if(_expSeqNb == pck._seqNb){ 
					
					byte[] data = pck._data;
					if(_receiver != null)
						_receiver.Receive(data);
					_ack = new GBNCCMessage(_expSeqNb, true, null);
					_ip.send(IPAddress.ANY, datagram.src, GBNCC_PROTOCOL, _ack);
					_expSeqNb++;
					
				}else {
					trySendACK();
				}
			}
		}else{
			System.out.println("CORRUPTED GBNCC Packet");
			trySendACK();
		}
	}
	
	public void trySendACK() throws Exception{
		if(_ack != null){
			_ip.send(IPAddress.ANY, _dst, GBNCC_PROTOCOL, _ack);
		}
	}

	@Override
	public void start() throws Exception {
		_ip.addListener(GBNCC_PROTOCOL, this);
	}

	@Override
	public void stop() {
		stopTimer();
		_ip.removeListener(GBNCC_PROTOCOL, this);
		_window.clear();
		_expSeqNb = 0;
		_sendBase = 0;
		_nextSeqNb = 0;		
		
	}

	public void setReceiver(Receiver rcv) {
		_receiver = rcv;
	}
}
