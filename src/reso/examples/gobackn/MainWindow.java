package reso.examples.gobackn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import reso.common.AbstractTimer;
import reso.scheduler.AbstractScheduler;

public class MainWindow extends JFrame implements ActionListener {
	
	private AbstractTimer _abst;
	
	public class MainMenu extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public JButton _startButton = new JButton("Start");
		
		public MainMenu(){
			super();
			setLayout(new GridLayout(1, 0));
			addButtons();
			setBorder(new EmptyBorder(10, 10, 10, 10));
		}
		
		public void addButtons(){
			add(_startButton);
			_startButton.setPreferredSize(new Dimension(0, 50));
			_startButton.addActionListener(MainWindow.this);
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final XYSeries _series;
	private final AbstractScheduler _scheduler;
	private final MainMenu _mainMenu = new MainMenu();
	private final AppSender _sender;
	private final AppReceiver _receiver;
	private boolean _started = false;
	
	public MainWindow(AbstractScheduler scheduler, AppSender host1App, AppReceiver host2App){
		super("Projet Réseaux I");
		
		
		_series = new XYSeries("R1");
		_scheduler = scheduler;
		
		_sender = host1App;
		_receiver = host2App;
		
		
		XYSeriesCollection data = new XYSeriesCollection(_series);
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JFreeChart chart = ChartFactory.createXYLineChart(
				"Cwnd size over time",
				"time",
				"size in MSS Packets",
				data,
				PlotOrientation.VERTICAL, 
				false,
				true,
				false
		);
		
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(800, 500));
		add(chartPanel, BorderLayout.CENTER);
		add(_mainMenu, BorderLayout.NORTH);
		
		pack();
		
		
	}
	
	public void addValue(int y){
		_series.add(_scheduler.getCurrentTime(), y);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(_started) return;
		
		_started = true;
		
		
		
		
		if(e.getSource() == _mainMenu._startButton){
			new Thread(new Runnable() {
		        public void run() {
		        	
		        	try {
		    			_receiver.start();
		    			_sender.start();
		    			_abst = new AbstractTimer(_scheduler, 0.01, true) {
							
							@Override
							protected void run() throws Exception {
								addValue(_sender.getProto().getCwnd());
								if(_receiver.isDone()){
									_abst.stop();
								}
							}
						};
						
						_abst.start();
			    		
			            _scheduler.run();
		    		} catch (Exception e1) {
		    			// TODO Auto-generated catch block
		    			e1.printStackTrace();
		    		}
		        	
		        	
		        }
		    }).start();
		}
		
	}
	
	
	
}
