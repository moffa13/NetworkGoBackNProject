package reso.examples.gobackn;
import java.util.Random;

import reso.common.AbstractApplication;
import reso.ip.IPAddress;
import reso.ip.IPHost;
public class AppSender extends AbstractApplication implements Receiver {
	
    private final IPAddress _dst;
    private final GBNCCProtocol _proto;
    public static int PACKETS_TO_SEND;
    
	public AppSender(IPHost host, IPAddress dst, String name) {
		super(host, name);
		_dst = dst;
		_proto = new GBNCCProtocol(host, _dst, name);
	}
	
	@Override
	public void Receive(byte[] data) {}
	
	public GBNCCProtocol getProto(){
		return _proto;
	}

	@Override
	public void start() throws Exception{
		_proto.start();
		
		Random r = new Random();
		for(int i = 0; i < PACKETS_TO_SEND; i++){
			StringBuilder stb = new StringBuilder();
			stb.append("Hello, ");
			stb.append(r.nextInt());
			System.out.println("Sending dummy data (" + stb.toString() + ")");
			_proto.send(stb.toString().getBytes());
		}	
	}

	@Override
	public void stop() {
		_proto.stop();
	}
	
	

}
