package com.angelo.carsim;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialRange;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.ValueDataset;

public class Display extends JFrame{

	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	
	private static final long serialVersionUID = 1L;

	public static JSlider brakeSlider = new JSlider();	
	public static JSlider throttleSlider = new JSlider();
	public static JSlider gearSlider = new JSlider();
	
	public static boolean absLightOn = false;
	public static double absTimer;
	
	private static Image absTexture;
	private static AffineTransform absImageTransform; 
	
	public static boolean tcsLightOn = false;
	public static double tcsTimer;
	
	private static Image tcsTexture;
	private static AffineTransform tcsImageTransform; 
	
	private static int framesCount;
	private static int timePast;
	private static long framesTimer;
	private static int framesCountAvg;
	
	
	
	public Display() {
		super("2D Car Sim");
		
		absTexture = TextureLoader.getTexture("abs.png");
		
		absTexture = absTexture.getScaledInstance(60, 48, Image.SCALE_DEFAULT);
		
		absImageTransform = AffineTransform.getTranslateInstance(25,0);
		//absImageTransform.scale(0.05, 0.05);
		
		tcsTexture = TextureLoader.getTexture("tcs.png");
	
		tcsTexture = tcsTexture.getScaledInstance(50, 50, Image.SCALE_DEFAULT);
		
		tcsImageTransform = AffineTransform.getTranslateInstance(100,0);
		//tcsImageTransform.scale(0.1, 0.1);
		
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.X_AXIS));
		
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;

			public void paintComponent(Graphics g){
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				AffineTransform saveAT = g2.getTransform();
				
				Main.road.render((Graphics2D)g2.create());
				Main.car.render((Graphics2D)g2.create());		
				
				//Restore transform
				g2.setTransform(saveAT);
		
				g2.setColor(Color.BLACK);
				g2.drawString("FPS: "+framesCountAvg, 5, 15);		
			}
		};
		
		panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		
		this.add(panel);
		this.add(createUIPanel());
		this.addKeyListener(new Keyboard());
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}
	
	public void updateDisplay(){
		calculateFPS();
		
		this.repaint();
	}

	public static void calculateFPS(){
		long now = System.currentTimeMillis();
		
		framesCount++;
		
		if (now - framesTimer > 1000) {
			framesTimer = now;
			framesCountAvg = framesCount;
			framesCount = 0;
		}	
	}
	
	public static JPanel createUIPanel() {
		JPanel localJPanel = new JPanel();
		localJPanel.setLayout(new BoxLayout(localJPanel, BoxLayout.Y_AXIS));
		
		localJPanel.add(createDialUIPanel());
		localJPanel.add(createSymbolsPanel());
		localJPanel.add(createSlidersPanel());
		
		return localJPanel;
	}
	
	public static JPanel createDialUIPanel() {
		JPanel localJPanel = new JPanel(new GridLayout(1, 2));
		localJPanel.add(new SpeedOMeterPanel());
		localJPanel.add(new TachometerPanel());
		
		return localJPanel;
	}
	
	public static JPanel createSymbolsPanel() {
		JPanel localJPanel = new JPanel() {
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				
			    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);		    
				
				if(absTimer > 1.0/6.0) {
					absLightOn = !absLightOn;
		
					absTimer = 0;
				}
				
				if(!Main.car.absActive) {
					absLightOn = false;
				}

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, absLightOn ? 1.0f : 0.1f));
				
				g2.drawImage(absTexture, absImageTransform, null);
				
				if(tcsTimer > 1.0/6.0) {
					tcsLightOn = !tcsLightOn;
		
					tcsTimer = 0;
				}
				
				if(!Main.car.tcsActive) {
					tcsLightOn = false;
				}

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, tcsLightOn ? 1.0f : 0.1f));
				
				g2.drawImage(tcsTexture, tcsImageTransform, null);
			}
			
		};
		
		localJPanel.setPreferredSize(new Dimension(400,50));
		
		return localJPanel;
	}
	
	public static JPanel createSlidersPanel() {
		JPanel localJPanel = new JPanel(new GridLayout(1, 1));
		localJPanel.add(new PedalsPanel());
		
		return localJPanel;
	}
	
	static class PedalsPanel extends JPanel implements ChangeListener {
		public PedalsPanel() {
			super();	
			
			this.setLayout(new GridLayout(4, 2));
				
			add(new JLabel("Brake", JLabel.CENTER));
				
			add(new JLabel("Gear", JLabel.CENTER));
			
			brakeSlider.setMaximum(100);
			brakeSlider.setPaintTicks(true);
			brakeSlider.setMajorTickSpacing(50);
			brakeSlider.setMinorTickSpacing(25);	
			
			Hashtable<Integer, JComponent> brakeLabelTable = new Hashtable<Integer, JComponent>();
			
			for(int i = 0; i <= 100; i+= 50) {
				brakeLabelTable.put(i, new JLabel(String.valueOf(i)+"%") );
			}
			
			brakeSlider.setLabelTable(brakeLabelTable);
			brakeSlider.setPaintLabels(true);	
			brakeSlider.setValue(0);	
			add(brakeSlider);
			
			gearSlider.setMaximum(5);
			gearSlider.setMinimum(1);
			gearSlider.setPaintTicks(true);
			gearSlider.setMajorTickSpacing(1);	
			gearSlider.setSnapToTicks(true);
			gearSlider.setPaintLabels(true);	
			gearSlider.setValue(1);	
			add(gearSlider);

			add(new JLabel("Throttle", JLabel.CENTER));
			
			add(new JLabel(""));
			
			throttleSlider.setMaximum(100);
			throttleSlider.setPaintTicks(true);
			throttleSlider.setMajorTickSpacing(50);
			throttleSlider.setMinorTickSpacing(25);
			
			Hashtable<Integer, JComponent> throttleLabelTable = new Hashtable<Integer, JComponent>();
			
			for(int i = 0; i <= 100; i+= 50) {
				throttleLabelTable.put(i, new JLabel(String.valueOf(i)+"%") );
			}
			
			throttleSlider.setLabelTable(throttleLabelTable);
			throttleSlider.setPaintLabels(true);
			throttleSlider.setValue(0);	
			add(throttleSlider);
			
			add(new JLabel(""));
		}

		@Override
		public void stateChanged(ChangeEvent e) {
		}
	}
	
	public static DefaultValueDataset kmh = new DefaultValueDataset(0.0D);
	public static DefaultValueDataset rpm = new DefaultValueDataset(0.8D);

	static class SpeedOMeterPanel extends JPanel implements ChangeListener {

		public SpeedOMeterPanel() {
			super();
			JFreeChart localJFreeChart = createStandardDialChart("Speedometer", "KM/H", kmh, 0, 200.0D, 20.0D, 3);
			DialPlot localDialPlot = (DialPlot) localJFreeChart.getPlot();

			DialBackground localDialBackground = new DialBackground(new Color(30, 30, 30));
			localDialPlot.setBackground(localDialBackground);
			localDialPlot.removePointer(0);
			localDialPlot.addPointer(new DialPointer.Pointer());
			ChartPanel localChartPanel = new ChartPanel(localJFreeChart);
			localChartPanel.setPreferredSize(new Dimension(300, 300));
			add(localChartPanel);
		}

		public static JFreeChart createStandardDialChart(String paramString1, String paramString2,
				ValueDataset paramValueDataset, double paramDouble1, double paramDouble2, double paramDouble3,
				int paramInt) {
			DialPlot localDialPlot = new DialPlot();
			localDialPlot.setDataset(paramValueDataset);
			localDialPlot.setDialFrame(new StandardDialFrame());
			localDialPlot.setBackground(new DialBackground());
			DialTextAnnotation localDialTextAnnotation = new DialTextAnnotation(paramString2);
			localDialTextAnnotation.setFont(new Font("Dialog", 1, 14));
			localDialTextAnnotation.setPaint(new Color(255, 255, 255));
			localDialTextAnnotation.setRadius(0.7D);

			localDialPlot.addLayer(localDialTextAnnotation);
			DialValueIndicator localDialValueIndicator = new DialValueIndicator(0);
			localDialPlot.addLayer(localDialValueIndicator);
			StandardDialScale localStandardDialScale = new StandardDialScale(paramDouble1, paramDouble2, -120.0D,
					-300.0D, 10.0D, 4);
			localStandardDialScale.setMajorTickIncrement(paramDouble3);
			localStandardDialScale.setMinorTickCount(paramInt);
			localStandardDialScale.setTickRadius(0.88D);
			localStandardDialScale.setTickLabelOffset(0.15D);
			localStandardDialScale.setTickLabelFont(new Font("Dialog", 0, 14));
			localStandardDialScale.setTickLabelPaint(new Color(255, 255, 255));
			localStandardDialScale.setMajorTickPaint(new Color(255, 255, 255));
			localStandardDialScale.setMinorTickPaint(new Color(255, 255, 255));
			localDialPlot.addScale(0, localStandardDialScale);
			localDialPlot.addPointer(new DialPointer.Pin());
			DialCap localDialCap = new DialCap();
			localDialPlot.setCap(localDialCap);
			return new JFreeChart(paramString1, localDialPlot);
		}

		public void stateChanged(ChangeEvent paramChangeEvent) {

		}
	}

	static class TachometerPanel extends JPanel implements ChangeListener {

		public TachometerPanel() {
			super();
			JFreeChart localJFreeChart = createStandardDialChart("Tachometer", "RPM x1000", rpm, 0, 8, 1, 9);
			DialPlot localDialPlot = (DialPlot) localJFreeChart.getPlot();
			StandardDialRange localStandardDialRange1 = new StandardDialRange(6.5D, 8D, Color.red);
			localStandardDialRange1.setInnerRadius(0.4D);
			localStandardDialRange1.setOuterRadius(0.55D);
			localDialPlot.addLayer(localStandardDialRange1);
			DialBackground localDialBackground = new DialBackground(new Color(30, 30, 30));

			localDialPlot.setBackground(localDialBackground);
			localDialPlot.removePointer(0);
			localDialPlot.addPointer(new DialPointer.Pointer());
			ChartPanel localChartPanel = new ChartPanel(localJFreeChart);
			localChartPanel.setPreferredSize(new Dimension(300, 300));
			add(localChartPanel);
		}

		public static JFreeChart createStandardDialChart(String paramString1, String paramString2,
				ValueDataset paramValueDataset, double paramDouble1, double paramDouble2, double paramDouble3,
				int paramInt) {
			DialPlot localDialPlot = new DialPlot();
			localDialPlot.setDataset(paramValueDataset);
			localDialPlot.setDialFrame(new StandardDialFrame());
			localDialPlot.setBackground(new DialBackground());
			DialTextAnnotation localDialTextAnnotation = new DialTextAnnotation(paramString2);
			localDialTextAnnotation.setFont(new Font("Dialog", 1, 14));
			localDialTextAnnotation.setPaint(new Color(255, 255, 255));
			localDialTextAnnotation.setRadius(0.7D);
			localDialPlot.addLayer(localDialTextAnnotation);
			DialValueIndicator localDialValueIndicator = new DialValueIndicator(0);
			localDialPlot.addLayer(localDialValueIndicator);
			StandardDialScale localStandardDialScale = new StandardDialScale(paramDouble1, paramDouble2, -120.0D,
					-300.0D, 10.0D, 4);
			localStandardDialScale.setMajorTickIncrement(paramDouble3);
			localStandardDialScale.setMinorTickCount(paramInt);
			localStandardDialScale.setTickRadius(0.88D);
			localStandardDialScale.setTickLabelOffset(0.15D);
			localStandardDialScale.setTickLabelFont(new Font("Dialog", 0, 14));
			localStandardDialScale.setTickLabelPaint(new Color(255, 255, 255));
			localStandardDialScale.setMajorTickPaint(new Color(255, 255, 255));
			localStandardDialScale.setMinorTickPaint(new Color(255, 255, 255));
			localDialPlot.addScale(0, localStandardDialScale);
			localDialPlot.addPointer(new DialPointer.Pin());
			DialCap localDialCap = new DialCap();
			localDialPlot.setCap(localDialCap);
			return new JFreeChart(paramString1, localDialPlot);
		}

		public void stateChanged(ChangeEvent paramChangeEvent) {

		}
	}
	
}
