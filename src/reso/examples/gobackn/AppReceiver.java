package reso.examples.gobackn;
import reso.common.AbstractApplication;
import reso.ip.IPAddress;
import reso.ip.IPHost;
public class AppReceiver extends AbstractApplication implements Receiver {
	
    private final GBNCCProtocol _ppl;
    private int _expPacketNumber;
    private int _packetNumber;
    
	public AppReceiver(IPHost host, IPAddress dst, String name, int expPacketNumber) {
		super(host, name);
		_expPacketNumber = expPacketNumber;
		_ppl = new GBNCCProtocol(host, dst, name);
		_ppl.setReceiver(this);
	}
	
	@Override
	public void Receive(byte[] data) {
		_packetNumber++;
		System.out.println("[" + (int)(getHost().getNetwork().getScheduler().getCurrentTime() * 1000) + "ms] " + "Received data (" + new String(data) + ")");
		if(_packetNumber == _expPacketNumber){
			System.out.println("SUCCESS ! All packets have successfully been received.");
		}
		
	}

	@Override
	public void start() throws Exception{
		_ppl.start();
		_packetNumber = 0;
	}

	@Override
	public void stop() {
		_ppl.stop();
	}

	public boolean isDone() {
		return _packetNumber == _expPacketNumber;
	}
	
	

}
