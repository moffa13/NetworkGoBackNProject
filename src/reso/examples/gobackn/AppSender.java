package reso.examples.gobackn;
import reso.common.AbstractApplication;
import reso.common.AbstractTimer;
import reso.ip.IPHost;
import reso.ip.IPAddress;
public class AppSender extends AbstractApplication implements Receiver {
	
    private final GBNCCProtocol _ppl;
    private int _dummy = 0;
    private final AbstractTimer _timer;
    

	public AppSender(GBNCCProtocol proto, IPHost host, IPAddress dst, String name) {
		super(host, name);
		_ppl = proto;
		proto.setReceiver(this);
		_timer = new AbstractTimer(host.getNetwork().getScheduler(), 1, true) {
			
			@Override
			protected void run() throws Exception {
				System.out.println("Sending dummy");
				StringBuilder stb = new StringBuilder();
				stb.append("Hello ");
				stb.append(_dummy++);
				
				_ppl.send(stb.toString().getBytes());
			
			}
		};
	}
	
	@Override
	public void Receive(byte[] data) {
		System.out.println("Received data (" + new String(data) + ")");
	}

	@Override
	public void start() throws Exception {
		_timer.start();
	}

	@Override
	public void stop() {
		_timer.stop();	
	}
	
	

}
