package reso.examples.gobackn;
import reso.common.Link;
import reso.common.Network;
import reso.ethernet.EthernetAddress;
import reso.ethernet.EthernetFrame;
import reso.ethernet.EthernetInterface;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPRouter;
import reso.scheduler.AbstractScheduler;
import reso.scheduler.Scheduler;
import reso.utilities.NetworkBuilder;

public class Main {
	
	public static final int INITIAL_WINDOW_SIZE = 8;

	public static void main(String[] args) {
		AbstractScheduler scheduler= new Scheduler();
		Network network= new Network(scheduler);
    	try {
    		final EthernetAddress MAC_ADDR1= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x28);
    		final EthernetAddress MAC_ADDR2= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x29);
    		final EthernetAddress MAC_ADDR3= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x2A);
    		final EthernetAddress MAC_ADDR4= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x2B);
    		final IPAddress IP_ADDR1= IPAddress.getByAddress(192, 168, 0, 1);
    		final IPAddress IP_ADDR2= IPAddress.getByAddress(192, 168, 0, 2);
    		final IPAddress IP_ADDR3= IPAddress.getByAddress(192, 168, 1, 1);
    		final IPAddress IP_ADDR4= IPAddress.getByAddress(192, 168, 1, 2);

    		IPHost host1= NetworkBuilder.createHost(network, "H1", IP_ADDR2, MAC_ADDR1);
    		host1.getIPLayer().addRoute(IP_ADDR1, "eth0"); // Route to R1
    		GBNCCProtocol host1proto = new GBNCCProtocol(INITIAL_WINDOW_SIZE, host1, IP_ADDR4, "H1_GBNCC");
    		host1.addApplication(new AppSender(host1proto, host1, IP_ADDR2, "APP1"));

    		IPHost host2= NetworkBuilder.createHost(network,"H2", IP_ADDR4, MAC_ADDR2);
    		host2.getIPLayer().addRoute(IP_ADDR3, "eth0"); // Route to R1
    		GBNCCProtocol host2proto = new GBNCCProtocol(INITIAL_WINDOW_SIZE, host2, IP_ADDR2, "H2_GBNCC");
    		host2.addApplication(new AppSender(host2proto, host2, IP_ADDR4, "APP2"));
    		
    		
    		IPRouter router = NetworkBuilder.createRouter(network, "R1", 
    				new IPAddress[]{IP_ADDR1, IP_ADDR3}, 
    				new EthernetAddress[]{MAC_ADDR3, MAC_ADDR4});
    		
    		//GBNCCProtocol routerProto = new GBNCCProtocol(INITIAL_WINDOW_SIZE, router, "R1_GBNCC");
    		//router.addApplication(routerProto);

    		EthernetInterface h1_eth0= (EthernetInterface) host1.getInterfaceByName("eth0");
    		EthernetInterface h2_eth0= (EthernetInterface) host2.getInterfaceByName("eth0");
    		EthernetInterface r1_eth0= (EthernetInterface) router.getInterfaceByName("eth0");
    		EthernetInterface r1_eth1= (EthernetInterface) router.getInterfaceByName("eth1");
    		
    		// Connect both interfaces h1 to r1 and r1 to h2 with a 5000km long link
    		new Link<EthernetFrame>(h1_eth0, r1_eth0, 5000000, 100000);
    		new Link<EthernetFrame>(r1_eth1, h2_eth0, 5000000, 100000);

    		host1.start();
    		host2.start();
    		
    		scheduler.run();
    	} catch (Exception e) {
    		System.err.println(e.getMessage());
    		e.printStackTrace(System.err);
    	}

	}

}
