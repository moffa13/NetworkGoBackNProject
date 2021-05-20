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
	
	public enum SENDER{
		SENDER,
		RECEIVER,
		BOTH
	}
	
	private final IPLayer _ip;
	private HashMap<Integer, GBNCCMessage> _window;
	private Receiver _receiver;
	public static final int GBNCC_PROTOCOL = Datagram.allocateProtocolNumber("GBNCC");
	public static final double TIMER_RESEND_INTERVAL = 0.8;
	public static final double PACKET_DROP_PERCENTAGE = 0.01;
	public static final int MAX_PACKET_DROPS = 10;
	public static final int MSS = 10;  // bytes
	private GBNCCMessage _ack = null;
	private final IPAddress _dst;
	private final RenoCC _cc;
	private LinkedList<RawChunkMessage> _queuedMessages;
	private ArrayList<Byte> _receivedBytes;
	private int _currentDrops = 0;
	
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
	}
	
	private void sendData(byte[] data) throws Exception{
		sendData(data, true);
	}
	
	/**
	 * Sends data over ip
	 * or store it into the queue if the window is full.
	 * @param data
	 * @throws 
	 */
	private void sendData(byte[] data, boolean lastMessage) throws Exception{
		
		if(_nextSeqNb < _sendBase + _cc.getWindowSize()){		
			
			GBNCCMessage packet = new GBNCCMessage(_nextSeqNb, false, data, lastMessage);

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
	
	/**
	 * Sends a packet over ip (already containing a sequence number)
	 * @param m
	 */
	private void sendPacket(Message m){
		try {
			log(false, SENDER.SENDER, "Sending packet with seqNb = " + ((GBNCCMessage)m)._seqNb);
			_ip.send(_ip.getInterfaceByName("eth0").getAddress(), _dst, GBNCC_PROTOCOL, m);
		} catch (Exception e) {
			log(true, SENDER.BOTH, "Can not send Packet, " + e.getMessage());
		}
	}
	
	/**
	 * Check if the window contains at least MSSBlock packets of MSS size free.
	 * This is used to check if a packet can be added to the window.
	 * @param MSSBlock
	 * @return
	 */
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
				log(false, SENDER.SENDER, "Trying to send queued message " + _sendBase + " " + _cc.getWindowSize());
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
				if(i == packetN - 1) {
					int remaining = data.length - i * MSS;
					byte[] packet = Arrays.copyOfRange(data, i * MSS, i * MSS + remaining); // 0 to MSS (0 to 19)  then MSS to MSS + MSS (20 to 39)
					sendData(packet, true);
				}else {
					
					byte[] packet = Arrays.copyOfRange(data, i * MSS, i * MSS + MSS); // 0 to MSS (0 to 19)  then MSS to MSS + MSS (20 to 39)
					sendData(packet, false);
				}
				
			}
			
		}else {
			sendData(data);
		}
		
	}

	public void startTimer() {
		log(false, SENDER.BOTH, "START TIMER");
		stopTimer();
		_resendTimer = new AbstractTimer(host.getNetwork().getScheduler(), TIMER_RESEND_INTERVAL, true) {
			
			@Override
			protected void run() throws Exception {
				timeout(true);
			}
		};
		_resendTimer.start();
	}
	
	public void stopTimer() {
		if(_resendTimer != null) _resendTimer.stop();
	}
	
	/**
	 * 
	 * @param realTimeout true if the timeout function is triggered by a real timeout, false if it is called for a fast retransmit.
	 */
	public void timeout(boolean realTimeout) {
			
		
		int min = getLastWindowIndex();
		
		log(false, SENDER.SENDER, "Going to resend from " + _sendBase + " to " + (min - 1));
				
		for(int i = _sendBase; i < min; i++){
			Message m = _window.get(i);
			sendPacket(m);
		}
		
		if(realTimeout){
			log(true, SENDER.SENDER, "Timeout detected..");
			_cc.timeout();
		}
		
	}
	
	public int getLastWindowIndex(){
		// As the window can be shrinked, always check if _nextSeqNb is not outside,
		// If yes, resend only all the packets in the window and not outside
		return Math.min(_nextSeqNb, _sendBase + _cc.getWindowSize());
	}
	
	/**
	 * Logs the specified message
	 * @param error Is it an error message ?
	 * @param sender Whether it is being logged from the sender or the receiver
	 * @param message
	 */
	public void log(boolean error, SENDER sender, String message){
				
		StringBuilder stb = new StringBuilder();
		
		stb.append("[");
		stb.append((int)(getHost().getNetwork().getScheduler().getCurrentTime() * 1000));
		stb.append("ms] ");
		
		if(error)
			stb.append("[ERROR] ");
		else
			stb.append("[INFO] ");
		
		if(sender == SENDER.SENDER){
			stb.append("SENDER : ");
		}else if(sender == SENDER.RECEIVER){
			stb.append("RECEIVER : ");
		}
		
		stb.append(message);
	
		System.out.println(stb.toString());
		
	}

	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram) throws Exception {
		
		GBNCCMessage pck = (GBNCCMessage)datagram.getPayload();
		
		// Simulate packet drop
		double d = Math.random();
		if(_currentDrops < MAX_PACKET_DROPS && d < PACKET_DROP_PERCENTAGE){
			log(true, SENDER.BOTH, "Faking a packet drop ... (seqNb=" + pck._seqNb + ", isAck=" + pck.isACK() + ")");
			_currentDrops++;
			return;
		}
		
		// Not corrupted
		if(pck._checksum == pck.getChecksum()){ 
			if(pck.isACK()){ // ACK Packet is received from receiver
				
				log(false, SENDER.SENDER, pck._seqNb + " ACK'ed");

				_cc.receiveACK(pck._seqNb);
				_window.remove(pck._seqNb); // Remove the received packet from the window
				_sendBase = pck._seqNb + 1;

				// Up to date
				if(_sendBase == _nextSeqNb){
					stopTimer();
				}else{
					startTimer();
				}
				
				trySendPendingPackets();
				
				
			}else{ // Data is received from the sender (we are receiver in that case)
				
				if(_expSeqNb == pck._seqNb){ 
					
					log(false, SENDER.RECEIVER, "Packet received in correct order");
					
					byte[] data = pck._data;
					
					addBytesToReceiveList(data);
					
					// We have received the last chunked message or a single one
					if(_receiver != null && pck.isLastMessage()) {
						sendtoReceiver();
					}
						
					_ack = new GBNCCMessage(_expSeqNb, true, null, true);
					_expSeqNb++;
					
				}else {
					log(true, SENDER.RECEIVER, "Received wrong packet (seqNb=" + pck._seqNb + ", expectedSeqNb=" + _expSeqNb + ")");
				}
				
				trySendACK(false, SENDER.RECEIVER);
			}
		}else{
			log(true, SENDER.BOTH, "CORRUPTED GBNCC Packet");
			trySendACK(true, SENDER.BOTH);
		}
		
	}
	
	private void sendtoReceiver() {
		_receiver.Receive(Bytes.toArray(_receivedBytes));
		_receivedBytes.clear();
		
	}

	private void addBytesToReceiveList(byte[] data) {
		_receivedBytes.addAll(Bytes.asList(data));
		
	}

	public void trySendACK(boolean error, SENDER sender) throws Exception{
		if(_ack != null){
			log(error, sender, "Sending ACK with seqNb = " + _ack._seqNb);
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
		_queuedMessages.clear();
		_receivedBytes.clear();
		_currentDrops = 0;
		_expSeqNb = 0;
		_sendBase = 0;
		_nextSeqNb = 0;		
		_ack = null;
		_cc.reset();
	}

	public void setReceiver(Receiver rcv) {
		_receiver = rcv;
	}

	public int getCwnd() {
		return _cc.getWindowSize();
	}
}
