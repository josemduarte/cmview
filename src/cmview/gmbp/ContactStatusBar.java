package cmview.gmbp;

import java.awt.BorderLayout;
import java.awt.Dimension;
//import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;

import javax.swing.BorderFactory;
//import javax.swing.Box;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class ContactStatusBar extends JPanel implements ItemListener, ActionListener, ChangeListener, KeyListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final int DEFAULT_WIDTH = 120;
	protected static final int DEFAULT_HEIGHT = 600;
	public static final float[] radiusThresholds = new float[] {2.0f, 5.6f, 9.2f, 12.8f};
	protected static final String[] colorStrings = {"BlueRed", "HotCold", "RGB"};
	protected final static int BLUERED = 0;
	protected final static int HOTCOLD = 1;
	protected final static int RGB = 2;
//	private final static String cylProjString = "Cylindrical";
//	private final static String pseudoCylProjString = "Kavrayskiy";
	private final static String[] projStrings = new String[] {"Cylindrical", "Kavrayskiy", "Azimuthal"};
	
	/*--------------------------- member variables --------------------------*/	
	// settings
	private int width = DEFAULT_WIDTH;						// width of this component, height matches contact map size
	private int groupWidth = width -5; //- 20;			// width of information groups within StatusBar
	private int height = DEFAULT_HEIGHT;
	
	// general members
	private ContactView controller; 						// controller which is notified as a response to gui actions
	
	// main panels
	private JPanel angleGroup;							// selected angle range
	private JPanel sphoxelGroup;						// panel holding the subgroups
	
	// subgroups panels holding gui elements for specific purposes
	private JPanel deltaRadiusPanel;					
	private JPanel deltaRadiusSliderPanel;
	private JPanel deltaRadiusButtonPanel;
	private JPanel resolutionPanel;					    
	private AnglePanel anglePanel;					
	private JPanel sstypePanel;
	private JPanel outliersPanel;
	private JPanel projectionPanel;
	private JPanel nbhsPanel;
	private NBHSselPanel nbhsSelPanel;
	
	// components for multi model group
	private JSlider radiusSliderLabel;					// to choose a radius-range
	private JRangeSlider radiusSlider;
	private JCheckBox radiusButton;
	private final int deltaSlVal = (int) ((radiusThresholds[1]-radiusThresholds[0])*10);
	private JSlider resolSlider;
	private JCheckBox diffSSTypeButton;
	private JCheckBox remOutliersButton;
	private JTextField minRatioField;
	private JTextField maxRatioField;
	private JComboBox colorCBox;
	private JButton colorScale;
	private JRadioButton cylProjRadioButton;
	private JRadioButton pseudoCylProjRadioButton;
	private JRadioButton azimuthProjRadioButton;
	private ButtonGroup projButtonGroup; 
	private JButton nbhsButton;
	private JTextField maxNumTraces;
	
	private boolean radiusRangesFixed = true;
	private boolean diffSSType = true;
	private boolean removeOutliers = true;
	private double minAllowedRatio = ContactPane.defaultMinAllowedRat;
	private double maxAllowedRatio = ContactPane.defaultMaxAllowedRat;
	private int chosenColourScale = BLUERED;
	private int chosenProjection = 0;
	private int maxNumNBHStraces = 100;
	
	private final int minSlVal = (int) (radiusThresholds[0]*10);
	private final int maxSlVal = (int) (radiusThresholds[radiusThresholds.length-1]*10);			
	
	
	public ContactStatusBar(ContactView controller) {

//		addKeyListener(this);
		
		this.controller = controller;
		
		// init basic border layout
		this.setLayout(new BorderLayout(0,0));
		sphoxelGroup = new JPanel();
		angleGroup = new JPanel();
		
		deltaRadiusPanel = new JPanel();
		resolutionPanel = new JPanel();
		sstypePanel = new JPanel();
		outliersPanel = new JPanel();
		projectionPanel = new JPanel();
		nbhsPanel = new JPanel();
		
		sphoxelGroup.setLayout(new BoxLayout(sphoxelGroup, BoxLayout.Y_AXIS));//BoxLayout.PAGE_AXIS
		sphoxelGroup.setBorder(BorderFactory.createEmptyBorder(2,5,0,5));
		
		sphoxelGroup.setSize(width, height);
		sphoxelGroup.setPreferredSize(new Dimension(width, height));
//		deltaRadiusPanel.setPreferredSize(new Dimension(groupWidth, height));
//		resolutionPanel.setPreferredSize(new Dimension(groupWidth, height));
//		sstypePanel.setPreferredSize(new Dimension(groupWidth, 50));
		
		this.add(sphoxelGroup,BorderLayout.PAGE_START);
		this.add(angleGroup, BorderLayout.PAGE_END);
		
//		this.add(deltaRadiusGroup, BorderLayout.LINE_START); //.PAGE_END
//		this.add(resolutionGroup, BorderLayout.LINE_START);
		
		this.removeOutliers = this.controller.cPane.isRemoveOutliers();
		
		initDeltaRadiusPanel();
//		initResolutionPanel();
		initSSTypePanel();
		initDrawPropPanel();
		initProjPropPanel();
		initNBHSPanel();
		initAngleGroup();
		showAngleGroup(true);
	}
	
	/**
	 * Initialize the gui group for changing the radius range.
	 */
	public void initDeltaRadiusPanel() {
		// initialize group
//		deltaRadiusPanel = new JPanel();
		deltaRadiusPanel.setLayout(new BoxLayout(deltaRadiusPanel,BoxLayout.PAGE_AXIS));
		String title = "Radius";
		deltaRadiusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		deltaRadiusPanel.setVisible(true);
		deltaRadiusPanel.setPreferredSize(new Dimension(groupWidth, height));
		deltaRadiusPanel.setMinimumSize(new Dimension(groupWidth,80));
		
		deltaRadiusSliderPanel = new JPanel();
		deltaRadiusSliderPanel.setLayout(new BoxLayout(deltaRadiusSliderPanel,BoxLayout.LINE_AXIS));
		
		deltaRadiusButtonPanel = new JPanel();
		deltaRadiusButtonPanel.setLayout(new BoxLayout(deltaRadiusButtonPanel,BoxLayout.LINE_AXIS));
		
		radiusButton = new JCheckBox("FixedRanges");
		radiusButton.setSelected(true);
		radiusButton.addItemListener(this);
		
//		System.out.println("deltaSliderValue= "+this.deltaSlVal);
		
		radiusSlider = new JRangeSlider(minSlVal, maxSlVal, minSlVal, minSlVal+this.deltaSlVal, JRangeSlider.VERTICAL, -1);
		radiusSlider.setMinExtent(this.deltaSlVal);
		radiusSlider.setSize(groupWidth/8, HEIGHT);
		radiusSlider.setPreferredSize(new Dimension(groupWidth/6, HEIGHT));
		
		radiusSlider.setEnabled(true);
		radiusSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
//                m_display.setHighQuality(false);
            }
            public void mouseReleased(MouseEvent e) {
    			updateRadiusValues();
            }
        });
		
		// Label for radiusSlider
		radiusSliderLabel = new JSlider();
		radiusSliderLabel.setMinimum(minSlVal);
		radiusSliderLabel.setMaximum(maxSlVal);
		radiusSliderLabel.setValue(radiusSlider.getHighValue());
		radiusSliderLabel.setMinorTickSpacing(1*10);
		radiusSliderLabel.setMajorTickSpacing(this.deltaSlVal);
		radiusSliderLabel.setExtent(this.deltaSlVal);
		radiusSliderLabel.setSnapToTicks(true);
		radiusSliderLabel.setOrientation(JSlider.VERTICAL);		
//		radiusSliderLabel.setInverted(true);		
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(20),new JLabel(" 2.0"));
		labels.put(new Integer(56), new JLabel(" 5.6"));
		labels.put(new Integer(92), new JLabel(" 9.2"));
		labels.put(new Integer(128), new JLabel(" 12.8"));
		radiusSliderLabel.setLabelTable(labels);		
		radiusSliderLabel.setPaintLabels(true);
		radiusSliderLabel.setPaintTicks(false);
//		radiusSliderLabel.setPaintTrack(true);	
		radiusSliderLabel.setSize(groupWidth-(radiusSlider.getWidth()), HEIGHT);
		radiusSliderLabel.setPreferredSize(new Dimension(groupWidth-(radiusSlider.getWidth()), HEIGHT));
		
//		radiusSliderLabel.setEnabled(true);
//		radiusSliderLabel.addChangeListener(this);
		radiusSliderLabel.setEnabled(false);
								
		// adding components to group	
		deltaRadiusSliderPanel.add(radiusSlider, BorderLayout.LINE_START);
	    deltaRadiusSliderPanel.add(radiusSliderLabel, BorderLayout.LINE_START);	    
	    deltaRadiusButtonPanel.add(radiusButton, BorderLayout.LINE_START);
		
		deltaRadiusPanel.add(deltaRadiusButtonPanel);
		deltaRadiusPanel.add(deltaRadiusSliderPanel);
	    
	    sphoxelGroup.add(deltaRadiusPanel);
	}
	
	/**
	 * Toggles the visibility of the delta radius group on or off.
	 * The delta radius group holds controls for changing 
	 * the radius range of the sphoxel image.
	 * @param show whether to show or hide the delta radius group
	 */
	public void showDeltaRadiusPanel(boolean show) {
		this.deltaRadiusPanel.setVisible(show);
	}
	
	/**
	 * Initialize the gui group for changing the resolution.
	 */
	public void initResolutionPanel() {
		// initialize group
//		resolutionPanel = new JPanel();
//		resolutionPanel.setLayout(new BoxLayout(resolutionPanel,BoxLayout.PAGE_AXIS));
		resolutionPanel.setLayout(new BoxLayout(resolutionPanel,BoxLayout.LINE_AXIS));
		String title = "Resolution";
		resolutionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		resolutionPanel.setVisible(true);
		resolutionPanel.setPreferredSize(new Dimension(groupWidth, height));
		resolutionPanel.setMinimumSize(new Dimension(groupWidth,100));
		
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
		
//		resolSlider.setEnabled(true);
		resolSlider.setEnabled(false);
		resolSlider.addChangeListener(this);
				
		// adding components to group
//	    resolutionPanel.add(Box.createRigidArea(new Dimension(groupWidth,5)));
	    resolutionPanel.add(resolSlider, BorderLayout.WEST);
	    resolutionPanel.add(Box.createRigidArea(new Dimension(groupWidth-resolSlider.getWidth(),5)));
	    System.out.println("groupWidth-resolSlider.getWidth()= "+ (groupWidth-resolSlider.getWidth()));
	    
	    sphoxelGroup.add(resolutionPanel);
//	    sphoxelGroup.add(resolutionPanel, BorderLayout.LINE_START);
	}

	/**
	 * Toggles the visibility of the resolution group on or off.
	 * The resolution group holds controls for changing 
	 * the resolution of the sphoxel image.
	 * @param show whether to show or hide the resolution overlay group
	 */
	public void showResolutionPanel(boolean show) {
		this.resolutionPanel.setVisible(show);
	}

	/**
	 * Initialize the gui group for switching flag for diffSSType on and of.
	 */
	public void initSSTypePanel(){
		sstypePanel.setLayout(new BoxLayout(sstypePanel,BoxLayout.LINE_AXIS));
		String title = "SSType";
		sstypePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		sstypePanel.setVisible(true);
		sstypePanel.setMinimumSize(new Dimension(groupWidth,50));
		sstypePanel.setPreferredSize(new Dimension(groupWidth,50));
		
		diffSSTypeButton = new JCheckBox("DiffSSType    ");
		diffSSTypeButton.setSelected(true);
		diffSSTypeButton.addItemListener(this);
		
		sstypePanel.add(diffSSTypeButton, BorderLayout.LINE_START);
		sphoxelGroup.add(sstypePanel);
	}
	/**
	 * Toggles the visibility of the sstype panel on or off.
	 * The sstype panel holds controls for switching 
	 * differentiation of i_ssType on or of.
	 * @param show whether to show or hide the sstype panel
	 */
	public void showSSTypePanel(boolean show){
		this.sstypePanel.setVisible(show);
	}

	/**
	 * Initialize the gui group for switching removal of outliers in sphoxel image on and of.
	 */
	public void initDrawPropPanel(){
		outliersPanel.setLayout(new BoxLayout(outliersPanel,BoxLayout.PAGE_AXIS));
		String title = "Outliers";
		outliersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		outliersPanel.setVisible(true);		
		outliersPanel.setMinimumSize(new Dimension(groupWidth,170));
		outliersPanel.setPreferredSize(new Dimension(groupWidth,170));
		
		JPanel buttonLinePanel = new JPanel();
		buttonLinePanel.setLayout(new BoxLayout(buttonLinePanel,BoxLayout.LINE_AXIS));
		JPanel minLinePanel = new JPanel();
		minLinePanel.setLayout(new BoxLayout(minLinePanel,BoxLayout.LINE_AXIS));
		JPanel maxLinePanel = new JPanel();
		maxLinePanel.setLayout(new BoxLayout(maxLinePanel,BoxLayout.LINE_AXIS));
		JPanel colorPanel = new JPanel();
		colorPanel.setLayout(new BoxLayout(colorPanel,BoxLayout.LINE_AXIS));
		JPanel scaleButtonPanel = new JPanel();
		scaleButtonPanel.setLayout(new BoxLayout(scaleButtonPanel, BoxLayout.LINE_AXIS));
		
		remOutliersButton = new JCheckBox("Remove        ");
		remOutliersButton.setSelected(this.removeOutliers);
		remOutliersButton.addItemListener(this);
		
		minRatioField = new JTextField(String.valueOf(this.minAllowedRatio));
		maxRatioField = new JTextField(String.valueOf(this.maxAllowedRatio));
		minRatioField.setEnabled(this.removeOutliers);
		maxRatioField.setEnabled(this.removeOutliers);
		minRatioField.addActionListener(this);
		maxRatioField.addActionListener(this);
		JLabel minLabel = new JLabel("min=  ");
		JLabel maxLabel = new JLabel("max= ");		

		colorCBox = new JComboBox(colorStrings);
		colorCBox.setSelectedItem(chosenColourScale);
		colorCBox.addActionListener(this);
		
		colorScale = new JButton("ShowScale");
		colorScale.setEnabled(true);
		colorScale.addActionListener(this);
		
		buttonLinePanel.add(remOutliersButton, BorderLayout.LINE_START);
		minLinePanel.add(minLabel, BorderLayout.LINE_START);
		minLinePanel.add(minRatioField, BorderLayout.LINE_END);
		maxLinePanel.add(maxLabel, BorderLayout.LINE_START);
		maxLinePanel.add(maxRatioField, BorderLayout.LINE_END);
		colorPanel.add(colorCBox, BorderLayout.LINE_START);
		scaleButtonPanel.add(colorScale, BorderLayout.LINE_START);
		outliersPanel.add(buttonLinePanel);
		outliersPanel.add(minLinePanel);
		outliersPanel.add(maxLinePanel);
		outliersPanel.add(colorPanel);
		outliersPanel.add(scaleButtonPanel);
//		outliersPanel.add(remOutliersButton);
//		outliersPanel.add(minRatioField);
//		outliersPanel.add(maxRatioField);
		sphoxelGroup.add(outliersPanel);	
		
	}
	/**
	 * Toggles the visibility of the drawProp panel on or off.
	 * @param show whether to show or hide the draw prop panel
	 */
	public void showDrawPropPanel(boolean show){
		this.outliersPanel.setVisible(show);
	}
	
	/**
	 * Initialize the gui group changing the map projection type.
	 */
	public void initProjPropPanel(){
//		projectionPanel
		projectionPanel.setLayout(new BoxLayout(projectionPanel,BoxLayout.PAGE_AXIS));
		String title = "Projection";
		projectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		projectionPanel.setVisible(true);		
		projectionPanel.setMinimumSize(new Dimension(groupWidth,100));
		projectionPanel.setPreferredSize(new Dimension(groupWidth,100));
		
		//Create the radio buttons.
		cylProjRadioButton = new JRadioButton(projStrings[0]+"   ");
		cylProjRadioButton.setActionCommand(projStrings[0]);
		pseudoCylProjRadioButton = new JRadioButton(projStrings[1]+"   ");
		pseudoCylProjRadioButton.setActionCommand(projStrings[1]);
		azimuthProjRadioButton = new JRadioButton(projStrings[2]+"  ");
		azimuthProjRadioButton.setActionCommand(projStrings[2]);
		if (this.controller.cPane.defaultProjType == this.controller.cPane.cylindricalMapProj){
			cylProjRadioButton.setSelected(true);
			this.chosenProjection = this.controller.cPane.cylindricalMapProj;
		}
		else if (this.controller.cPane.defaultProjType == this.controller.cPane.kavrayskiyMapProj){
			pseudoCylProjRadioButton.setSelected(true);
			this.chosenProjection = this.controller.cPane.kavrayskiyMapProj;
		}
		else if (this.controller.cPane.defaultProjType == this.controller.cPane.azimuthalMapProj){
			azimuthProjRadioButton.setSelected(true);
			this.chosenProjection = this.controller.cPane.azimuthalMapProj;
		}
		
		//Group the radio buttons.
        ButtonGroup projButtonGroup = new ButtonGroup();
        projButtonGroup.add(cylProjRadioButton);
        projButtonGroup.add(pseudoCylProjRadioButton);
        projButtonGroup.add(azimuthProjRadioButton);
        
        //Register a listener for the radio buttons.
        cylProjRadioButton.addActionListener(this);
        pseudoCylProjRadioButton.addActionListener(this);
        azimuthProjRadioButton.addActionListener(this);

        JPanel radioB1 = new JPanel();
        radioB1.setLayout(new BoxLayout(radioB1,BoxLayout.LINE_AXIS));
        JPanel radioB2 = new JPanel();
        radioB2.setLayout(new BoxLayout(radioB2,BoxLayout.LINE_AXIS));
        JPanel radioB3 = new JPanel();
        radioB3.setLayout(new BoxLayout(radioB3,BoxLayout.LINE_AXIS));
		
        radioB1.add(cylProjRadioButton);
        radioB2.add(pseudoCylProjRadioButton);
        radioB3.add(azimuthProjRadioButton);
        projectionPanel.add(radioB1);
        projectionPanel.add(radioB2);
        projectionPanel.add(radioB3);
		
		sphoxelGroup.add(projectionPanel);		
	}
	/**
	 * Toggles the visibility of the projection panel on or off.
	 * @param show whether to show or hide the draw prop panel
	 */
	public void showProjPropPanel(boolean show){
		this.projectionPanel.setVisible(show);
	}
	

	
	/**
	 * Initializes the group for showing coordinates
	 */
	public void initNBHSPanel() {		
		JPanel nbhsLinePanel = new JPanel();
		nbhsLinePanel.setLayout(new BoxLayout(nbhsLinePanel,BoxLayout.LINE_AXIS));
		JPanel maxLinePanel = new JPanel();
		maxLinePanel.setLayout(new BoxLayout(maxLinePanel,BoxLayout.LINE_AXIS));	
		JPanel buttonLinePanel = new JPanel();
		buttonLinePanel.setLayout(new BoxLayout(buttonLinePanel,BoxLayout.LINE_AXIS));
		
		nbhsSelPanel = new NBHSselPanel(this.controller.cPane.getNbhString());
				
		maxNumTraces = new JTextField(String.valueOf(this.maxNumNBHStraces));
		maxNumTraces.setEnabled(true);
		maxNumTraces.addActionListener(this);
		JLabel maxLabel = new JLabel("max#=  ");
	
		nbhsButton = new JButton("Traces");
		nbhsButton.setEnabled(true);
		nbhsButton.setPreferredSize(new Dimension(nbhsSelPanel.getPreferredSize().width, nbhsButton.getSize().height));
		nbhsButton.addActionListener(this);
		
		nbhsLinePanel.add(nbhsSelPanel, BorderLayout.LINE_START);
		maxLinePanel.add(maxLabel, BorderLayout.LINE_START);
		maxLinePanel.add(maxNumTraces, BorderLayout.LINE_END);
		buttonLinePanel.add(nbhsButton, BorderLayout.LINE_START);

		// init panel
		nbhsPanel.setLayout(new BoxLayout(nbhsPanel,BoxLayout.PAGE_AXIS));  // BoxLayout.Y_AXIS));
		String title = "NBHString";
		nbhsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		nbhsPanel.setVisible(true);	
		nbhsPanel.setMinimumSize(new Dimension(groupWidth, nbhsSelPanel.getPreferredSize().height + 60 + 30));
		nbhsPanel.setPreferredSize(new Dimension(groupWidth, nbhsSelPanel.getPreferredSize().height + 60 + 30));
		
		nbhsPanel.add(nbhsLinePanel, BorderLayout.PAGE_START);
//		nbhsPanel.add(maxLinePanel);
		nbhsPanel.add(buttonLinePanel, BorderLayout.PAGE_END);
		
//		nbhsPanel.add(nbhsSelPanel, BorderLayout.LINE_START);
//		nbhsPanel.add(maxLinePanel);
//		nbhsPanel.add(nbhsButton, BorderLayout.LINE_START);
//		nbhsPanel.add(nbhsSelPanel);
//		nbhsPanel.add(maxLinePanel);
//		nbhsPanel.add(nbhsButton);
//		nbhsPanel.add(anglePanel);
		sphoxelGroup.add(nbhsPanel);
	}

	/**
	 * Toggles the visibility of the coordinates group on or off.
	 * @param show whether to show or hide the group
	 */
	public void showNBHSPanel(boolean show) {
		angleGroup.setVisible(show);
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
	

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		System.out.println("keyReleased3");
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		System.out.println("keyReleased1");
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		System.out.println("keyReleased2");
		
	}

	/**
	 * Handle local item events
	 */
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		if (e.getItemSelectable() == radiusButton){
			if (e.getStateChange() == ItemEvent.SELECTED)
				this.radiusRangesFixed = true;
			else // e.getStateChange() == ItemEvent.DESELECTED
				this.radiusRangesFixed = false;
//			this.radiusSliderLabel.setEnabled(radiusRangesFixed);
			this.resolSlider.setEnabled(!this.radiusRangesFixed);
//			this.resolSlider.setVisible(!this.radiusRangesFixed);
			if (this.radiusRangesFixed)
				updateRadiusValues();
			System.out.println("radiusRangesFixed ="+this.radiusRangesFixed);
			controller.handleChangeRadiusRangesFixed(this.radiusRangesFixed);
		}
		if (e.getItemSelectable() == diffSSTypeButton){
			if (e.getStateChange() == ItemEvent.SELECTED)
				this.diffSSType = true;
			else // e.getStateChange() == ItemEvent.DESELECTED
				this.diffSSType = false;
			controller.handleChangeDiffSSType(this.diffSSType);
		}
		if (e.getItemSelectable() == remOutliersButton){
			if (e.getStateChange() == ItemEvent.SELECTED)
				this.removeOutliers = true;
			else
				this.removeOutliers = false;
			minRatioField.setEnabled(this.removeOutliers);
			maxRatioField.setEnabled(this.removeOutliers);
			controller.handleChangeRemOutliers(this.removeOutliers);	
		}
	}

	/**
	 * Handle local button events
	 */
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == this.colorCBox){
//			this.chosenColourScale = colorStrings
			this.chosenColourScale = this.colorCBox.getSelectedIndex();
			controller.handleChangeColourScale(this.chosenColourScale);
//			controller.handleChangeShowColourScale(this.chosenColourScale);
			System.out.println(this.colorCBox.getSelectedItem()+"  "+this.colorCBox.getSelectedIndex());
		}
		if (e.getSource()==this.minRatioField || e.getSource()==this.maxRatioField){
			this.minAllowedRatio = Double.valueOf(minRatioField.getText());
			this.maxAllowedRatio = Double.valueOf(maxRatioField.getText());
			controller.handleChangeOutlierThresholds(this.minAllowedRatio, this.maxAllowedRatio);
			System.out.println("Thresholds: "+this.minAllowedRatio+" : "+this.maxAllowedRatio);			
		}
		if (e.getSource()==this.cylProjRadioButton || e.getSource()==this.pseudoCylProjRadioButton || e.getSource()==this.azimuthProjRadioButton){
			if (e.getActionCommand()==projStrings[0])
				chosenProjection = this.controller.cPane.cylindricalMapProj; // ContactPane.cylindricalMapProj;
			else if (e.getActionCommand()==projStrings[1])
				chosenProjection = this.controller.cPane.kavrayskiyMapProj; //ContactPane.kavrayskiyMapProj;
			else if (e.getActionCommand()==projStrings[2])
				chosenProjection = this.controller.cPane.azimuthalMapProj;
			controller.handleChangeProjectionType(chosenProjection);
//			System.out.println(e.getActionCommand());
//			System.out.println(e.getSource());
		}
		if (e.getSource() == this.colorScale){
			controller.handleChangeShowColourScale(this.chosenColourScale);			
		}

		if (e.getSource() == this.nbhsButton){
			this.maxNumNBHStraces = Integer.valueOf(this.maxNumTraces.getText());
			System.out.println("MaxNumLines= "+this.maxNumNBHStraces);
			controller.handleChangeTracesParam(this.nbhsSelPanel.getActNbhString(), this.maxNumNBHStraces); //(this.nbhsSelPanel.getActNbhString());			
		}
	}
	
	private void updateRadiusValues(){
		int min = this.radiusSlider.getLowValue();
		int max = this.radiusSlider.getHighValue();
		int minVal = min;
		int maxVal = max;
		if (this.radiusRangesFixed){
			int fac = (int) Math.round((double)(min/this.deltaSlVal));		
			minVal = this.minSlVal + (fac*this.deltaSlVal);
			fac = (int) Math.round( (double)(max-this.minSlVal)/(double)(this.deltaSlVal));
			maxVal = this.minSlVal + (fac*this.deltaSlVal);	
			if (maxVal==minVal) { 
				if (maxVal<this.maxSlVal)
					maxVal += this.deltaSlVal;
				else
					minVal -= this.deltaSlVal;
			}				
		}

		this.radiusSlider.setLowValue(minVal);
		this.radiusSlider.setHighValue(maxVal);
		this.radiusSliderLabel.setValue(maxVal);

//		controller.handleChangeRadiusRange(radiusThresholds[0], (float) (this.radiusSlider.getValue() / 10.0f));
//		System.out.println("RadiusSliderVal= "+ this.radiusSlider.getValue() / 10.0f);
		controller.handleChangeRadiusRange((float)(this.radiusSlider.getLowValue())/10.0f, (float)(this.radiusSlider.getHighValue())/10.0f);
		System.out.println("RadiusSliderRange= "+ (float)(this.radiusSlider.getLowValue())/10.0f + "-" + (float)(this.radiusSlider.getHighValue())/10.0f);
	}
	
	/**
	 * Handle local change events
	 */
	public void stateChanged(ChangeEvent e) {		
		if(e.getSource() == this.radiusSlider) {
			updateRadiusValues();
			System.out.println("in stateChanged");
		}
		if(e.getSource() == this.radiusSliderLabel){
//			float max = (float)(this.radiusSliderLabel.getValue())/10.0f;
//			if (this.radiusRangesFixed){
//				if (max < radiusThresholds[1])
//					max = radiusThresholds[1];
//				float min = max-((float)(this.deltaSlVal)/10.0f); // = this.delatRadiusThresholds[0];
//				this.radiusSlider.setLowValue((int) (min*10.0f));
//				this.radiusSlider.setHighValue((int) (max*10.0f));				
//			}
//			else {
//				int diff = this.radiusSlider.getHighValue()-this.radiusSlider.getLowValue();
//				this.radiusSlider.setHighValue((int) (max*10.0f));
//				this.radiusSlider.setLowValue((int)(max*10.0f)-diff);				
//			}
//			controller.handleChangeRadiusRange((float)(this.radiusSlider.getLowValue())/10.0f, (float)(this.radiusSlider.getHighValue())/10.0f);
//			System.out.println("RadiusSliderRange= "+ (float)(this.radiusSlider.getLowValue())/10.0f + "-" + (float)(this.radiusSlider.getHighValue())/10.0f);
		}
		if(e.getSource() == this.resolSlider) {
			if (!this.radiusRangesFixed){
				controller.handleChangeResolution(this.resolSlider.getValue());
				System.out.println("ResolutionSliderVal= "+ this.resolSlider.getValue() +"¡");				
			}
		}
	}
	
    /*-------------------------- getters and setters -----------------------*/
	
	public AnglePanel getAnglePanel() {
		return this.anglePanel;
	}
	
	public NBHSselPanel getNBHSPanel(){
		return this.nbhsSelPanel;
	}
	
	public JSlider getResolSlider(){
		return this.resolSlider;
	}

	public void setProjButtonGroup(ButtonGroup projButtonGroup) {
		this.projButtonGroup = projButtonGroup;
	}

	public ButtonGroup getProjButtonGroup() {
		return projButtonGroup;
	}

}
