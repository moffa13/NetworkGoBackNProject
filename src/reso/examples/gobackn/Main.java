package reso.examples.gobackn;

import reso.common.Network;
import reso.ethernet.EthernetAddress;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPRouter;
import reso.scheduler.AbstractScheduler;
import reso.scheduler.Scheduler;
import reso.utilities.NetworkBuilder;

public class Main {

	public static void main(String[] args) {
		
		
		
		AbstractScheduler scheduler= new Scheduler();
		Network network= new Network(scheduler);
		
    	try {
    		final EthernetAddress MAC_ADDR1= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x28);
    		final EthernetAddress MAC_ADDR2= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x29);
    		final EthernetAddress MAC_ADDR3= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x2A);
    		final EthernetAddress MAC_ADDR4= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x2B);
    		final IPAddress IP_ADDR1= IPAddress.getByAddress(192, 168, 0, 2);
    		final IPAddress IP_ADDR2= IPAddress.getByAddress(192, 168, 1, 2);
    		final IPAddress IP_ADDR3= IPAddress.getByAddress(192, 168, 0, 1);
    		final IPAddress IP_ADDR4= IPAddress.getByAddress(192, 168, 1, 1);
    		
    		
    		/**
    		 * Here are the parameters you can modify to change the simulation's behaviour.
    		 */
    		AppSender.PACKETS_TO_SEND = 2000;
    		GBNCCProtocol.PACKET_DROP_PERCENTAGE = 0.01;
    		GBNCCProtocol.TRANSMISSION_RATE_BYTE_PER_SEC = 40;
    		GBNCCProtocol.MAX_PACKET_DROPS = 10;
    		GBNCCProtocol.MSS = 10;  // bytes
    		GBNCCProtocol.TIMER_RESEND_INTERVAL = 0.8;

    		// Make host1 sending infos to host2
    		IPHost host1= NetworkBuilder.createHost(network, "H1", IP_ADDR1, MAC_ADDR1);
    		AppSender host1App = new AppSender(host1, IP_ADDR2, "APP1");
    		host1.addApplication(host1App);

    		IPHost host2= NetworkBuilder.createHost(network,"H2", IP_ADDR2, MAC_ADDR2);
    		AppReceiver host2App = new AppReceiver(host2, IP_ADDR1, "APP2", AppSender.PACKETS_TO_SEND);
    		host2.addApplication(host2App);
    		
    		MainWindow w = new MainWindow(scheduler, host1App, host2App);
    		w.setVisible(true);
    		
    		
    		IPRouter router = NetworkBuilder.createRouter(network, "R1", 
    				new IPAddress[]{IP_ADDR3, IP_ADDR4}, 
    				new EthernetAddress[]{MAC_ADDR3, MAC_ADDR4});

    		NetworkBuilder.createLink(host1, "eth0", router, "eth0", 5000000, 100000);
    		NetworkBuilder.createLink(router, "eth1", host2, "eth0", 5000000, 100000);
    		
			host1.getIPLayer().addRoute(IP_ADDR2, IP_ADDR3);
			host1.getIPLayer().addRoute(IP_ADDR3, "eth0");
			
			host2.getIPLayer().addRoute(IP_ADDR1, IP_ADDR4);
			host2.getIPLayer().addRoute(IP_ADDR4, "eth0");
			
			router.getIPLayer().addRoute(IP_ADDR1, "eth0");
			router.getIPLayer().addRoute(IP_ADDR2, "eth1");
			

			router.start();
    	
    	} catch (Exception e) {
    		System.err.println(e.getMessage());
    		e.printStackTrace(System.err);
    	}

	}

}
