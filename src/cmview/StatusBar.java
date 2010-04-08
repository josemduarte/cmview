package cmview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;


/**
 * Status bar component to the right of the contact map. The status bar holds several
 * JPanels which show information and controls depending on the type of data currently
 * displayed in the main view (single contact map, two contact maps, fuzzy map, delta rank).
 * For information components (e.g. the coordinate display) , the data to be shown is
 * passed to setter methods. For interactive components (e.g. the overlay dropdown lists)
 * the actions are handled by the view object whose reference is passed in the constructor. 
 * 
 * The groups currently shown are (from top to bottom):
 * - drop down menus for choosing overlays
 * - delta rank score (if in delta rank mode)
 * - sequence coordinates of current mouse position (using CoordinatesPanel)
 * 
 * Planned groups for future versions:
 * - delta rank information
 * - information and controls for compare mode
 * - controls for fuzzy contact maps (discretization slider)
 * - evtl. controls for contact maps which have 3D information (e.g. send to PyMol)
 * 
 * The display of sequent coordinates depends on the mode and some flags. The modes are
 * - single contact map : show i and j sequence coordinates of current mouse position
 * - single contact map, diagonal selection mode : show additionally the sequence separation 
 * - compare mode : show i and j coordinates of both contact maps side-by-side (plus titles)
 * - residue ruler mode : if mouse is over residue ruler, indicated by iNum="" or jNum="", show only i or j coordinate
 * Additionally there are some flags to toggle extra information:
 * - hasSecondaryStructure : display secondary structure type of current i and j
 * - writePDBResNum : in addition to canonic sequence coordinates show PDB residue numbers below
 * - showAliAndSeqPos : in addition to sequence coordinates show alingment coordinates (only in compare mode)
 * @author stehr
 *
 */
public class StatusBar extends JPanel implements ItemListener, ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// settings
	private int width = 182;						// width of this component, height matches contact map size
	private int groupWidth = width - 20;			// width of information groups within StatusBar
	
	// general members
	private View controller; 						// controller which is notified as a response to gui actions
	
	// subcomponents
	private JPanel groupsPane;						// panel holding the subgroups
	private CoordinatesPanel coordinatesPanel;		// panel showing coordinates
	
	private JPanel overlaysGroup;					// background overlays
	private JPanel coordinatesGroup;				// TODO: coordinates
	private JPanel multiModelGroup; 				// TODO: discretization slider
//	private JPanel compareGroup;					// TODO: controls for compare mode
//	private JPanel filterGroup;						// TODO: contact filters (sequence separation, distance, ...)
		
	// data rank label
	private JLabel deltaRankLable;					// delta rank display (in delta rank mode)
	private JButton addBestDRContactButton;
	private JButton removeWorstDRContactButton;
	
	// gui components in subgroups
	private JComboBox firstViewCB, secondViewCB;			// drop down lists in overlaysGroup
	private JCheckBox discretizeCheckBox;					// to switch discretization slider on/off
	private JSlider discretizationSlider;					// to choose a cutoff for discretization
	private JButton discretizeButton;						// to permanently apply discretization
	
	/**
	 * Initializes the status bar
	 * @param controller the controller which is notified on gui actions in the status bar
	 */
	public StatusBar(View controller) {
		//this.setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
		//this.add(Box.createRigidArea(new Dimension(width, 2)));
		this.controller = controller;
		
		// init basic border layout
		this.setLayout(new BorderLayout(0,0));
		coordinatesPanel = new CoordinatesPanel();
		groupsPane = new JPanel();
		groupsPane.setLayout(new BoxLayout(groupsPane, BoxLayout.PAGE_AXIS));
		//groupsPane.add(Box.createRigidArea(new Dimension(width, 2)));
		groupsPane.setBorder(BorderFactory.createEmptyBorder(2,5,0,5));
		this.add(groupsPane,BorderLayout.PAGE_START);
		this.add(coordinatesPanel, BorderLayout.PAGE_END);
		this.addBestDRContactButton = new JButton("add max DR");
		this.removeWorstDRContactButton = new JButton("remove min DR");
		
	}
	
	/**
	 * Initialize the group of controls for showing background overlays (distance map, contact density etc.)
	 * TODO: Add overlay of difference map in compare mode.
	 */
	public void initOverlayGroup() {
		// init group
		overlaysGroup = new JPanel();
		String title = "Overlays";
		overlaysGroup.setLayout(new BoxLayout(overlaysGroup,BoxLayout.PAGE_AXIS));
		overlaysGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		
		// init sub-components
		firstViewCB = new JComboBox();
		secondViewCB = new JComboBox();
		firstViewCB.addItem((Object)"Top-Right BG");
		secondViewCB.addItem((Object)"Bottom-Left BG");
		Object[] viewOptions = {"Common Nbhd", "Contact Density", "Distance","Delta Rank"};
		for (Object o : viewOptions) {
			firstViewCB.addItem(o);
			secondViewCB.addItem(o);
		}
		firstViewCB.setEditable(true);
		secondViewCB.setEditable(true);
		firstViewCB.setSize(150, 20);
		secondViewCB.setSize(150, 20);
		firstViewCB.setMaximumSize(new Dimension(150,20));
		secondViewCB.setMaximumSize(new Dimension(150,20));
		secondViewCB.setMinimumSize(new Dimension(150,20));
		firstViewCB.setMinimumSize(new Dimension(150,20));
		firstViewCB.addActionListener(this);
		firstViewCB.setAlignmentX(CENTER_ALIGNMENT);
		secondViewCB.setAlignmentX(CENTER_ALIGNMENT);
		secondViewCB.addActionListener(this);

		// add sub-components to group
		overlaysGroup.add(Box.createRigidArea(new Dimension(groupWidth,10)));
		overlaysGroup.add(secondViewCB);
		overlaysGroup.add(Box.createRigidArea(new Dimension(0,5)));
		overlaysGroup.add(firstViewCB);
		overlaysGroup.add(Box.createRigidArea(new Dimension(0,5)));
		
		// add group to StatusBar
		groupsPane.add(overlaysGroup);
		groupsPane.add(Box.createVerticalGlue());	 // to show delta rank label (added later) just below this group
	}
	
	/**
	 * Toggles the visibility of the overlays group on or off.
	 * The overlays group holds controls for selecting background
	 * overlays for the lower/upper half of the contact map.
	 * @param show whether to show or hide the overlay group
	 */
	public void showOverlayGroup(boolean show) {
		this.overlaysGroup.setVisible(show);
	}
	
	/**
	 * Initializes the group for showing coordinates
	 */
	public void initCoordinatesGroup() {
		// init group
		coordinatesGroup = new JPanel();
		String title = "Coordinates";
		coordinatesGroup.setLayout(new BoxLayout(coordinatesGroup,BoxLayout.PAGE_AXIS));
		coordinatesGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		
		// init sub-components
		coordinatesGroup.add(Box.createRigidArea(new Dimension(groupWidth, 20)));
		
		// add group to StatusBar
		groupsPane.add(coordinatesGroup);
		//this.add(Box.createVerticalGlue());	 // to show next component just below this group		
	}
	
	/**
	 * Initialize the gui group for multi-model or fuzzy contact maps.
	 */
	public void initMultiModelGroup() {
		// First function to implement is a slider for discretization.
		// In the first iteration, multi model structures are simply converted
		// to fuzzy contact maps. The Model needs to be updated to hold both
		// the original fuzzy map and the (currently active) discretized one.
		// Any edits made to the contact map (such as contact deletion or creation)
		// will force a discretization and the current discretization will be fixed.
		// (Show a warning message that discretization will be fixed).
		// MultiModels/Fuzzy maps can come from: NMR ensembles, MD trajectories,
		// distance map, fuzzy contact map file.
		multiModelGroup = new JPanel();
		multiModelGroup.setLayout(new BoxLayout(multiModelGroup,BoxLayout.PAGE_AXIS));
		String title = "Discretization";
		multiModelGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		discretizationSlider = new JSlider();
		discretizationSlider.setMinorTickSpacing(10);
		discretizationSlider.setMajorTickSpacing(50);
		discretizationSlider.setMaximum(100);
		discretizationSlider.setPaintTicks(true);
		discretizationSlider.setEnabled(false);
		discretizeCheckBox = new JCheckBox("discretize");
		discretizeCheckBox.setSelected(false);
		discretizeCheckBox.setAlignmentX(CENTER_ALIGNMENT);
		discretizeCheckBox.addItemListener(this);	// events are handled by this class because the effects are strictly local
		discretizeButton = new JButton("Apply permanently");
		discretizeButton.setAlignmentX(CENTER_ALIGNMENT);
		discretizeButton.setEnabled(false);
		discretizeButton.addActionListener(this);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));
	    multiModelGroup.add(discretizeCheckBox);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));    
	    multiModelGroup.add(discretizationSlider);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));
	    multiModelGroup.add(discretizeButton);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,10)));
	    //this.add(Box.createVerticalGlue());
	    groupsPane.add(multiModelGroup);
	    //this.add(Box.createVerticalGlue());
	}
	
	public void showMultiModelGroup(boolean show) {
		this.multiModelGroup.setVisible(show);
	}
	
	/**
	 * Initialize the label for showing the delta rank score
	 */
	public void initDeltaRankLable() {
		this.add(Box.createRigidArea(new Dimension(150,200)));
		deltaRankLable = new JLabel();
		deltaRankLable.setBounds(5, 5, 100, 20);
		groupsPane.add(deltaRankLable);
		//this.add(deltaRankLable,2);
	}
	
	private void initDeltaRankStrategyButtons() {
		addBestDRContactButton = new JButton("add best");
		removeWorstDRContactButton = new JButton("remove worst");
	}
	
	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width,160);
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width,getHeight());
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width,super.getMaximumSize().height);
	}

	

	/*---------------------------- event listening -------------------------*/
	
	/**
	 * Handle local item events
	 */
	public void itemStateChanged(ItemEvent e) {
		if(e.getItemSelectable() == discretizeCheckBox) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				discretizationSlider.setEnabled(false);
				discretizeButton.setEnabled(false);
			} else {
				discretizationSlider.setEnabled(true);
				discretizeButton.setEnabled(true);
			}
		}
	}
	
	/**
	 * Handle local button events
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == discretizeButton) {
			System.out.println("Discretize button was pressed. Threshold = " + discretizationSlider.getValue());
		}
		if(e.getSource() == firstViewCB) {
			controller.handleBgOverlayChange(false,firstViewCB.getSelectedIndex());
		}
		if(e.getSource() == secondViewCB) {
			controller.handleBgOverlayChange(true,secondViewCB.getSelectedIndex());
		}
	}
	
	/*-------------------------- getters and setters -----------------------*/
	
	public CoordinatesPanel getCoordinatesPanel() {
		return this.coordinatesPanel;
	}
	
	public void setDeltaRank(float f) {
		{
		deltaRankLable.setText("\u0394" + "rank: "+f);
		}
	}
	
}
