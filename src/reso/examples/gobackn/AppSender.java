package reso.examples.gobackn;
import java.util.Random;

import reso.common.AbstractApplication;
import reso.ip.IPAddress;
import reso.ip.IPHost;
public class AppSender extends AbstractApplication implements Receiver {
	
    private final int _nbPackets;
    private final IPAddress _dst;
    
	public AppSender(IPHost host, IPAddress dst, String name, int nbPackets) {
		super(host, name);
		_nbPackets = nbPackets;	
		_dst = dst;
	}
	
	@Override
	public void Receive(byte[] data) {}

	@Override
	public void start() throws Exception{
		GBNCCProtocol ppl = new GBNCCProtocol(host, _dst, name);
		ppl.start();
		
		Random r = new Random();
		for(int i = 0; i < _nbPackets; i++){
			StringBuilder stb = new StringBuilder();
			stb.append("Hello, ");
			stb.append(r.nextInt());
			System.out.println("Sending dummy data (" + stb.toString() + ")");
			ppl.send(stb.toString().getBytes());
		}	
	}

	@Override
	public void stop() {}
	
	

}
