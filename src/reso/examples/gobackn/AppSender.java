package reso.examples.gobackn;
import reso.common.AbstractApplication;
import reso.common.AbstractTimer;
import reso.ip.IPHost;
public class AppSender extends AbstractApplication implements Receiver {
	
    private final GBNCCProtocol _ppl;
    private int _dummy = 0;
    private final AbstractTimer _timer;
    private final MODE _mode;
    
    public enum MODE{
    	RECEIVE,
    	SEND,
    	BOTH
    }
    

	public AppSender(GBNCCProtocol proto, IPHost host, String name, MODE mode) {
		super(host, name);
		_ppl = proto;
		_mode = mode;
		proto.setReceiver(this);
		
		
		_timer = new AbstractTimer(host.getNetwork().getScheduler(), 1000, true) {
			
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
		if(_mode == MODE.SEND || _mode == MODE.BOTH) _timer.start();
	}

	@Override
	public void stop() {
		_timer.stop();	
	}
	
	

}
