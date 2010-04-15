package cmview;

import java.awt.BorderLayout;
import java.awt.Dimension;
//import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;

import javax.swing.BorderFactory;
//import javax.swing.Box;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ContactStatusBar extends JPanel implements ItemListener, ActionListener, ChangeListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final int DEFAULT_WIDTH = 110;
	protected static final int DEFAULT_HEIGHT = 500;
	
	// settings
	private int width = DEFAULT_WIDTH;						// width of this component, height matches contact map size
	private int groupWidth = width - 20;			// width of information groups within StatusBar
	private int height = DEFAULT_HEIGHT;
	
	// general members
	private ContactView controller; 						// controller which is notified as a response to gui actions
	
	// main panels
	private JPanel groupsPanel;						// panel holding the subgroups
	private AnglePanel anglePanel;					// panel holding the subgroups
	
	// subgroups panels holding gui elements for specific purposes
	private JPanel deltaRadiusGroup;					// radius range
	private JPanel resolutionGroup;					    // resolution 
	private JPanel angleGroup;							// selected angle range
	
	// components for multi model group
	private JSlider radiusSliderLabel;					// to choose a radius-range
	private JRangeSlider radiusSlider;
	private float[] radiusThresholds = new float[] {2.0f, 5.6f, 9.2f, 12.8f};
	private JSlider resolSlider;
	
	
	public ContactStatusBar(ContactView controller) {
		
		this.controller = controller;
		
		// init basic border layout
		this.setLayout(new BorderLayout(0,0));
		groupsPanel = new JPanel();
		angleGroup = new JPanel();
		deltaRadiusGroup = new JPanel();
		resolutionGroup = new JPanel();
		groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));//BoxLayout.PAGE_AXIS
		groupsPanel.setBorder(BorderFactory.createEmptyBorder(2,5,0,5));
		
		groupsPanel.setSize(width, height);
		groupsPanel.setPreferredSize(new Dimension(width, height));
		deltaRadiusGroup.setPreferredSize(new Dimension(groupWidth, height));
		resolutionGroup.setPreferredSize(new Dimension(groupWidth, height));
		
		this.add(groupsPanel,BorderLayout.PAGE_START);
		this.add(angleGroup, BorderLayout.PAGE_END);
		
		this.add(deltaRadiusGroup, BorderLayout.LINE_START); //.PAGE_END
		this.add(resolutionGroup, BorderLayout.LINE_START);
		
		initDeltaRadiusGroup();
		initResolutionGroup();
		initAngleGroup();
		showAngleGroup(true);
	}
	
	/**
	 * Initialize the gui group for changing the radius range.
	 */
	public void initDeltaRadiusGroup() {
		// initialize group
		deltaRadiusGroup = new JPanel();
//		deltaRadiusGroup.setLayout(new BoxLayout(deltaRadiusGroup,BoxLayout.PAGE_AXIS));
		deltaRadiusGroup.setLayout(new BoxLayout(deltaRadiusGroup,BoxLayout.LINE_AXIS));
		String title = "Radius range";
		deltaRadiusGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		deltaRadiusGroup.setVisible(true);
		
		int minSlVal = (int) (this.radiusThresholds[0]*10);
		int maxSlVal = (int) (this.radiusThresholds[this.radiusThresholds.length-1]*10);
		int deltaSlVal = (int) ((this.radiusThresholds[1]-this.radiusThresholds[0])*10);
		
		radiusSlider = new JRangeSlider(minSlVal, maxSlVal, minSlVal, minSlVal+deltaSlVal, JRangeSlider.VERTICAL, -1);
		radiusSlider.setMinExtent(deltaSlVal);
		radiusSlider.setSize(groupWidth/8, HEIGHT);
		radiusSlider.setPreferredSize(new Dimension(groupWidth/6, HEIGHT));
		
		radiusSlider.setEnabled(true);
//		radiusSlider.addChangeListener(this);
		radiusSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
//                m_display.setHighQuality(false);
            }
            public void mouseReleased(MouseEvent e) {
//                m_display.setHighQuality(true);
//                m_display.repaint();
            	System.out.println("RadiusSliderRange= "+ radiusSlider.getLowValue()/10.0f + "-" + radiusSlider.getHighValue()/10.0f);
            	controller.handleChangeRadiusRange(radiusSlider.getLowValue()/10.0f, radiusSlider.getHighValue()/10.0f);
            }
        });
		
		// Label for radiusSlider
		radiusSliderLabel = new JSlider();
		radiusSliderLabel.setMinimum(minSlVal);
		radiusSliderLabel.setMaximum(maxSlVal);
		radiusSliderLabel.setValue(radiusSlider.getMaximum());
		radiusSliderLabel.setMinorTickSpacing(1*10);
		radiusSliderLabel.setMajorTickSpacing((int) ((this.radiusThresholds[1]-this.radiusThresholds[0])*10));
		radiusSliderLabel.setOrientation(JSlider.VERTICAL);		
//		radiusSliderLabel.setInverted(true);		
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(20),new JLabel(" 2.0"));
		labels.put(new Integer(56), new JLabel(" 5.6"));
		labels.put(new Integer(92), new JLabel(" 9.2"));
		labels.put(new Integer(128), new JLabel(" 12.8"));
		radiusSliderLabel.setLabelTable(labels);		
		radiusSliderLabel.setPaintLabels(true);
		radiusSliderLabel.setPaintTicks(true);
//		radiusSliderLabel.setPaintTrack(true);	
		radiusSliderLabel.setSize(groupWidth-(radiusSlider.getWidth()), HEIGHT);
		radiusSliderLabel.setPreferredSize(new Dimension(groupWidth-(radiusSlider.getWidth()), HEIGHT));
				
		// adding components to group		
//	    deltaRadiusGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));
//		deltaRadiusGroup.add(radiusSlider, BorderLayout.WEST, 0);
	    deltaRadiusGroup.add(radiusSlider, BorderLayout.WEST);
//		deltaRadiusGroup.add(Box.createRigidArea(new Dimension(5,0)));
//	    deltaRadiusGroup.add(Box.createRigidArea(new Dimension(groupWidth,2)));
	    deltaRadiusGroup.add(radiusSliderLabel, BorderLayout.WEST);
//		deltaRadiusGroup.add(Box.createRigidArea(new Dimension(5,0)));
//		deltaRadiusGroup.add(radiusSliderLabel, BorderLayout.EAST, 0);
//		deltaRadiusGroup.add(Box.createRigidArea(new Dimension(0,5)));
	    
//	    deltaRadiusGroup.setPreferredSize(new Dimension(groupWidth, deltaRadiusGroup.getHeight()));
//	    groupsPanel.add(deltaRadiusGroup);
	    groupsPanel.add(deltaRadiusGroup, BorderLayout.LINE_START);
	}
	
	/**
	 * Toggles the visibility of the delta radius group on or off.
	 * The delta radius group holds controls for changing 
	 * the radius range of the sphoxel image.
	 * @param show whether to show or hide the delta radius group
	 */
	public void showDeltaRadiusGroup(boolean show) {
		this.deltaRadiusGroup.setVisible(show);
	}
	
	/**
	 * Initialize the gui group for changing the resolution.
	 */
	public void initResolutionGroup() {
		// initialize group
		resolutionGroup = new JPanel();
//		resolutionGroup.setLayout(new BoxLayout(resolutionGroup,BoxLayout.PAGE_AXIS));
		resolutionGroup.setLayout(new BoxLayout(resolutionGroup,BoxLayout.LINE_AXIS));
		String title = "Resolution";
		resolutionGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		resolutionGroup.setVisible(true);
		resolutionGroup.setPreferredSize(new Dimension(groupWidth, height));
		
//		// --- NUMSTEPS----
//		// initialize components
//		resolSlider = new JSlider();
//		resolSlider.setMinimum(4);
//		resolSlider.setMaximum(72);
//		resolSlider.setValue(8);
//		resolSlider.setMinorTickSpacing(4);
//		resolSlider.setMajorTickSpacing(16);
//		resolSlider.setSnapToTicks(true);
//		resolSlider.setExtent(resolSlider.getMajorTickSpacing());
//		resolSlider.setOrientation(JSlider.VERTICAL);
//
////		resolSlider.setInverted(true);		
//		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
//		labels.put(new Integer(4),new JLabel("45¡"));
//		labels.put(new Integer(10),new JLabel("18¡"));
//		labels.put(new Integer(20),new JLabel("9.0¡"));
//		labels.put(new Integer(30),new JLabel("6.0¡"));
//		labels.put(new Integer(40),new JLabel("4.5¡"));
//		labels.put(new Integer(50),new JLabel("3.6¡"));
//		labels.put(new Integer(60),new JLabel("3.0¡"));
////		labels.put(new Integer(9), new JLabel("20¡"));
////		labels.put(new Integer(18), new JLabel("10¡"));
////		labels.put(new Integer(36), new JLabel("5¡"));
//		labels.put(new Integer(72), new JLabel("2.5¡"));
//		resolSlider.setLabelTable(labels);	
		
		
		// --- RESOL----
		// initialize components
		resolSlider = new JSlider();
		resolSlider.setMinimum(2);
		resolSlider.setMaximum(42);
		resolSlider.setValue(42);
		resolSlider.setMinorTickSpacing(4);
		resolSlider.setMajorTickSpacing(8);
		resolSlider.setSnapToTicks(true);
		resolSlider.setExtent(resolSlider.getMajorTickSpacing());
		resolSlider.setOrientation(JSlider.VERTICAL);

//		resolSlider.setInverted(true);		
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(2),new JLabel(" 2¡"));
//		labels.put(new Integer(5),new JLabel("5¡"));
		labels.put(new Integer(10),new JLabel(" 10¡"));
		labels.put(new Integer(18),new JLabel(" 18¡"));
		labels.put(new Integer(26),new JLabel(" 26¡"));
//		labels.put(new Integer(25),new JLabel("25¡"));
		labels.put(new Integer(34),new JLabel(" 34¡"));
//		labels.put(new Integer(35), new JLabel("35¡"));
//		labels.put(new Integer(40), new JLabel("40¡"));
		labels.put(new Integer(42), new JLabel(" 42¡"));
		resolSlider.setLabelTable(labels);	
		 
		resolSlider.setPaintLabels(true);
		resolSlider.setPaintTicks(true);
		resolSlider.setPaintTrack(true);
		
		resolSlider.setEnabled(true);
		resolSlider.addChangeListener(this);
				
		// adding components to group
//	    resolutionGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));
	    resolutionGroup.add(resolSlider, BorderLayout.WEST);
	    resolutionGroup.add(Box.createRigidArea(new Dimension(groupWidth-resolSlider.getWidth(),5)));
	    System.out.println("groupWidth-resolSlider.getWidth()= "+ (groupWidth-resolSlider.getWidth()));
//	    resolutionGroup.setMinimumSize(new Dimension(groupWidth,10));
//	    groupsPanel.add(resolutionGroup);
	    groupsPanel.add(resolutionGroup, BorderLayout.LINE_START);
	}

	/**
	 * Toggles the visibility of the resolution group on or off.
	 * The resolution group holds controls for changing 
	 * the resolution of the sphoxel image.
	 * @param show whether to show or hide the resolution overlay group
	 */
	public void showResolutionGroup(boolean show) {
		this.resolutionGroup.setVisible(show);
	}
	
	/**
	 * Initializes the group for showing coordinates
	 */
	public void initAngleGroup() {
		// init group
//		String title = "Coordinates";
//		coordinatesGroup.setLayout(new BoxLayout(coordinatesGroup,BoxLayout.PAGE_AXIS));
//		coordinatesGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		angleGroup.setVisible(false);
		
//		// init sub-components
//		coordinatesGroup.add(Box.createRigidArea(new Dimension(groupWidth, 20)));
		anglePanel = new AnglePanel();
		
		// add components to group
		angleGroup.add(anglePanel);	
	}

	/**
	 * Toggles the visibility of the coordinates group on or off.
	 * @param show whether to show or hide the group
	 */
	public void showAngleGroup(boolean show) {
		angleGroup.setVisible(show);
	}
	
	/*---------------------------- event listening -------------------------*/
	/**
	 * Handle local item events
	 */
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Handle local button events
	 */
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Handle local change events
	 */
	public void stateChanged(ChangeEvent e) {		
		if(e.getSource() == this.radiusSlider) {
//			controller.handleChangeRadiusRange(this.radiusThresholds[0], (float) (this.radiusSlider.getValue() / 10.0f));
//			System.out.println("RadiusSliderVal= "+ this.radiusSlider.getValue() / 10.0f);
			controller.handleChangeRadiusRange(this.radiusSlider.getLowValue()/10.0f, this.radiusSlider.getHighValue()/10.0f);
			System.out.println("RadiusSliderRange= "+ this.radiusSlider.getLowValue()/10.0f + "-" + this.radiusSlider.getHighValue()/10.0f);
		}
		if(e.getSource() == this.resolSlider) {
			controller.handleChangeResolution(this.resolSlider.getValue());
			System.out.println("ResolutionSliderVal= "+ this.resolSlider.getValue() +"¡");
		}
	}
	
    /*-------------------------- getters and setters -----------------------*/
	
	public AnglePanel getAnglePanel() {
		return this.anglePanel;
	}

}
