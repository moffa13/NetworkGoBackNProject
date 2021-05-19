package reso.examples.gobackn;

import java.awt.Dimension;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import reso.scheduler.AbstractScheduler;

public class MainWindow extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final XYSeries _series;
	private final AbstractScheduler _scheduler;

	
	public MainWindow(AbstractScheduler scheduler){
		super("Projet Réseaux I");
		
		
		_series = new XYSeries("R1");
		_scheduler = scheduler;
		
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
		setContentPane(chartPanel);
		
		pack();
		
		
	}
	
	public void addValue(int y){
		_series.add(_scheduler.getCurrentTime(), y);
	}
	
	
	
}
