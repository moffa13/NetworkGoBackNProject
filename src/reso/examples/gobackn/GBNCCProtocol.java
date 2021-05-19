package reso.examples.gobackn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import com.google.common.primitives.Bytes;

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
	private HashMap<Integer, GBNCCMessage> _window;
	private Receiver _receiver;
	public static final int GBNCC_PROTOCOL = Datagram.allocateProtocolNumber("GBNCC");
	public static final int TIMER_RESEND_INTERVAL = 2;
	public static final double PACKET_DROP_PERCENTAGE = 0.01;
	public static final int MSS = 10;  // bytes
	private GBNCCMessage _ack = null;
	private final IPAddress _dst;
	private final RenoCC _cc;
	private LinkedList<RawChunkMessage> _queuedMessages;
	private ArrayList<Byte> _receivedBytes;
	
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
		_receivedBytes = new ArrayList<>();
		_resendTimer = new AbstractTimer(host.getNetwork().getScheduler(), TIMER_RESEND_INTERVAL, false) {
			
			@Override
			protected void run() throws Exception {
				timeout();				
			}
		};
	}
	
	private void sendData(byte[] data) throws Exception{
		sendData(data, true);
	}
	
	/**
	 * Sends data over ip from eth0 interface to _dst
	 * or store it into the queue if the window is full.
	 * @param data
	 * @throws 
	 */
	private void sendData(byte[] data, boolean lastMessage) throws Exception{
		
		if(_nextSeqNb < _sendBase + _cc.getWindowSize()){		
			
			GBNCCMessage packet = new GBNCCMessage(_nextSeqNb, false, data, lastMessage);
			
			System.out.println("SENDING " + _nextSeqNb);
			
			
			//_window.put(_nextSeqNb - _sendBase, packet);
			_window.put(_nextSeqNb, packet);
			
			
			
			sendPacket(packet);
			
			if(_sendBase == _nextSeqNb){
				startTimer();
			}
			
			_nextSeqNb++;
			
		}else{
			_queuedMessages.add(new RawChunkMessage(data, lastMessage)); // Add the packet to the queue and not to the window
		}
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
			RawChunkMessage packet = _queuedMessages.poll();
			try {
				sendData(packet._data, packet._lastMessage);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
				 boolean lastMessage = i == packetN - 1;
				 sendData(packet, lastMessage);
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
		
		_cc.timeout();
		
		startTimer();
		
		int min = Math.min(_nextSeqNb, _sendBase + _cc.getWindowSize());
				
		for(int i = _sendBase; i < min; i++){
			Message m = _window.get(i);
			sendPacket(m);
		}
		
	}

	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
		
		
		
		double d = Math.random();
		// Simulate packet drop
		if(d < PACKET_DROP_PERCENTAGE)
			return;
		System.out.println("RECV");
		
		GBNCCMessage pck = (GBNCCMessage)datagram.getPayload();
		
		if(pck._checksum == pck.getChecksum()){ // Not corrupted
			if(pck.isACK()){ // ACK Packet is received from receiver
				
				System.out.println(pck._seqNb + " ACK'ed");

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
					
					addBytesToReceiveList(data);
					
					if(_receiver != null && pck.isLastMessage()) {
						sendtoReceiver();
					}
						
					_ack = new GBNCCMessage(_expSeqNb, true, null, true);
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
		
		trySendPendingPackets();
	}
	
	private void sendtoReceiver() {
		_receiver.Receive(Bytes.toArray(_receivedBytes));
		_receivedBytes.clear();
		
	}

	private void addBytesToReceiveList(byte[] data) {
		_receivedBytes.addAll(Bytes.asList(data));
		
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
