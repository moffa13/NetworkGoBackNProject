package reso.examples.gobackn;
import reso.common.AbstractApplication;
import reso.ip.IPHost;
import reso.ip.IPAddress;
import reso.ip.IPLayer;
public class AppSender extends AbstractApplication {
	
	private final IPLayer _ip;
    private final IPAddress _dst;
    private final GBNSender _ppl;
    private boolean started = false;
    

	public AppSender(IPHost host, IPAddress dst, String name) {
		super(host, name);
		_ip = host.getIPLayer();
		_dst = dst;
		_ppl = new GBNSender(8, _ip, _dst);
	}

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
				
	}
	
	

}
