package reso.examples.gobackn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
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
	private HashMap<Integer, Message> _window;
	private Receiver _receiver;
	public static final int GBNCC_PROTOCOL = Datagram.allocateProtocolNumber("GBNCC");
	public static final int TIMER_RESEND_INTERVAL = 10000;  // 20 might not be good value
	public static final int MSS = 20;  // bytes
	private Message _ack = null;
	private final IPAddress _dst;
	private final RenoCC _cc;
	private LinkedList<Message> _queuedMessages;
	
	// As a receiver
	private int _expSeqNb = 0;
	
	// As a sender
	private int _sendBase = 0;
	private int _nextSeqNb = 0;
	
	private AbstractTimer _resendTimer;
	
	public GBNCCProtocol(Host host, IPAddress dst, String name){
		super(host, name);
		_cc = new RenoCC(this);
		_ip = ((IPHost)host).getIPLayer();
		_queuedMessages = new LinkedList<>();
		_dst = dst;
		_window = new HashMap<>();
		_resendTimer = new AbstractTimer(host.getNetwork().getScheduler(), TIMER_RESEND_INTERVAL, false) {
			
			@Override
			protected void run() throws Exception {
				timeout();				
			}
		};
	}
	
	/**
	 * Called by the congestion controller 
	 * Resizes the window by saving the packets outside of the window's bound and storing them into the queue
	 * @param n
	 * @param oldN
	 * @throws Exception
	 */
	public void reduceWindowSize(int n, int oldN){
		if(_sendBase + n - 1 < _nextSeqNb){ // elements are outside now
			for(int i = _sendBase + n; i < _sendBase + oldN; i++){ // All truncated elems
				Message m = _window.remove(i);
				if(m != null){
					sendData(m); // Send them later
				}
			}
		}
	}
	
	/**
	 * Sends data over ip from eth0 interface to _dst
	 * or store it into the queue if the window is full.
	 * @param data
	 * @throws 
	 */
	private void sendData(byte[] data) throws Exception{
		
		Message packet = new GBNCCMessage(_nextSeqNb, false, data);
		
		if(_nextSeqNb < _sendBase + _cc.getWindowSize()){		
			
			//_window.put(_nextSeqNb - _sendBase, packet);
			_window.put(_nextSeqNb, packet);
			
			sendPacket(packet);
			
			if(_sendBase == _nextSeqNb){
				startTimer();
			}
			
			_nextSeqNb++;
		}else{
			_queuedMessages.add(packet); // Add the packet to the queue and not to the window
		}
	}
	
	private void sendData(Message m) {
		_queuedMessages.add(m);
		trySendPendingPackets();
	}
	
	private void sendPacket(Message m){
		try {
			System.out.println("Sending from " + _ip.getInterfaceByName("eth0").getAddress()
			+ " to " + _dst);
			_ip.send(_ip.getInterfaceByName("eth0").getAddress(), _dst, GBNCC_PROTOCOL, m);
		} catch (Exception e) {
			System.err.println("Can not send Packet, " + e.getMessage());
		}
	}
	
	public boolean canWindowHandle(int MSSBlock){
		return _cc.getWindowSize() + _sendBase - _nextSeqNb >= MSSBlock;
	}
	
	/**
	 * Sends all possible waiting packets in the queue as long as the window can
	 */
	private void trySendPendingPackets(){
		while(canWindowHandle(1) && !_queuedMessages.isEmpty()){ // handle the older packets first as long as the window can take packets and the queue is not empty 
			Message packet = _queuedMessages.poll();
			sendPacket(packet);
		}
	}
	
	
	/**
	 * Sends data over ip and cuts packet in smaller ones if data length is greater than MSS.
	 * @param data
	 * @throws Exception
	 */
	public void send(byte[] data) throws Exception{
		
		trySendPendingPackets();
		
		if(data.length > MSS){ // Data needs to be split because data > MSS
			
			int packetN = (int)Math.ceil(((float)data.length / MSS)); // Number of packet when split in MSS
			
			for(int i = 0; i < packetN; i++){
				 byte[] packet = Arrays.copyOfRange(data, i * MSS, i * MSS + MSS); // 0 to MSS (0 to 19)  then MSS to MSS + MSS (20 to 39)
				 sendData(packet);
			}
			
		}else {
			sendData(data);
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
		
		_cc.timeout();
		
		for(int i = 0; i < _nextSeqNb; i++){
			Message m = _window.get(i);
			sendPacket(m);
		}
		
	}
	
	// todo send last packets from the queue with some sort of timer if no further data is being sent.

	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
		
		GBNCCMessage pck = (GBNCCMessage)datagram.getPayload();
		
		if(pck._checksum == pck.getChecksum()){ // Not corrupted
			if(pck.isACK()){ // ACK Packet is received from receiver
				_cc.receiveACK(pck._seqNb);
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
