package cmview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import owl.core.structure.scoring.DRToThree;
import owl.core.structure.scoring.ResidueContactScoringFunction;

import cmview.datasources.Model;

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
public class StatusBar extends JPanel implements ItemListener, ActionListener, ChangeListener {
	
	private static final long serialVersionUID = 1L;
	
	// settings
	private int width = 182;						// width of this component, height matches contact map size
	private int groupWidth = width - 20;			// width of information groups within StatusBar
	
	
	// general members
	private View controller; 						// controller which is notified as a response to gui actions
	
	// main panels
	private JPanel groupsPanel;						// panel holding the subgroups
	private CoordinatesPanel coordinatesPanel;		// panel showing coordinates
	
	// subgroups panels holding gui elements for specific purposes
	private JPanel overlaysGroup;					// background overlays
	private JPanel deltaRankGroup;					// controls for delta rank mode
	private JPanel multiModelGroup; 				// discretization slider
	private JPanel coordinatesGroup;				// TODO: coordinates
//	private JPanel compareGroup;					// TODO: controls for compare mode
//	private JPanel filterGroup;						// TODO: contact filters (sequence separation, distance, ...)
		
	// components for delta rank group
	private JLabel deltaRankLable;					// delta rank display (in delta rank mode)
	private JButton addBestDRContactButton;
	private JButton removeWorstDRContactButton;
	
	// components for overlay group
	private JComboBox firstViewCB, secondViewCB;			// drop down lists in overlaysGroup
	
	// components for multi model group
	private HistogramPanel histogramPanel;					// showing the histogram of contact weights
	private JCheckBox discretizeCheckBox;					// to switch discretization slider on/off
	private JSlider discretizationSlider;					// to choose a cutoff for discretization
	private JCheckBox l10checkBox;							// to choose length/10 contacts for discretization	
	private JCheckBox l5checkBox;							// to choose length/5 contacts for discretization
	private JButton discretizeButton;						// to permanently apply discretization
	
	private ResidueContactScoringFunction[] scoringFunctions = new ResidueContactScoringFunction[1];
	
	private ScoringFunctionController topScoringFunctionController;
	private ScoringFunctionController bottomScoringFunctionController;
	
	/**
	 * Initializes the status bar
	 * @param controller the controller which is notified on gui actions in the status bar
	 */
	public StatusBar(View controller) {
		
		this.controller = controller;
		
		scoringFunctions[0] = new DRToThree();
		
		// init basic border layout
		this.setLayout(new BorderLayout(0,0));
		groupsPanel = new JPanel();
		coordinatesGroup = new JPanel();
		groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.PAGE_AXIS));
		groupsPanel.setBorder(BorderFactory.createEmptyBorder(2,5,0,5));
		this.add(groupsPanel,BorderLayout.PAGE_START);
		this.add(coordinatesGroup, BorderLayout.PAGE_END);
		initCoordinatesGroup();
		initOverlayGroup();
		initDeltaRankGroup();
		initMultiModelGroup();	
	}
	
	public ResidueContactScoringFunction getScoringFunctionWithName(String name) {
		for (ResidueContactScoringFunction f : scoringFunctions) {
			if (name == f.getMethodName()) {
				return f;
			}
		}
		return null;
	}
	

	public int getGroupWidth() {
		return this.groupWidth;
	}

	public ResidueContactScoringFunction[] getScoringFunctions(){
		return this.scoringFunctions;
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
		overlaysGroup.setVisible(false);
		
		// init sub-components
		firstViewCB = new JComboBox();
		secondViewCB = new JComboBox();
		firstViewCB.addItem((Object)"Top-Right BG");
		secondViewCB.addItem((Object)"Bottom-Left BG");
		View.BgOverlayType[] viewOptions = {View.BgOverlayType.DENSITY, View.BgOverlayType.DISTANCE}; // View.BgOverlayType.ENERGY
		for (View.BgOverlayType t: viewOptions) {
			firstViewCB.addItem(t.getItem());
			secondViewCB.addItem(t.getItem());
		}
		
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			View.BgOverlayType[] viewOptions2 = {View.BgOverlayType.COMMON_NBH}; //, View.BgOverlayType.DELTA_RANK};
			for (View.BgOverlayType t: viewOptions2) {
				firstViewCB.addItem(t.getItem());
				secondViewCB.addItem(t.getItem());
			}
			if (this.controller.isDatabaseConnectionAvailable()){
				firstViewCB.addItem(View.BgOverlayType.DELTA_RANK.getItem());
				secondViewCB.addItem(View.BgOverlayType.DELTA_RANK.getItem());
				
				for (ResidueContactScoringFunction f : scoringFunctions) {
					firstViewCB.addItem(f.getMethodName());
					secondViewCB.addItem(f.getMethodName());
				}
			}
		}
		
		// add option for variable transferFunction
		secondViewCB.addItem(View.BgOverlayType.TF_FUNC.getItem());
		
		firstViewCB.setEditable(true); // this should actually be false, but we want the white background
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
		groupsPanel.add(overlaysGroup);
		groupsPanel.add(Box.createVerticalGlue());	 // to show delta rank label (added later) just below this group
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
//		String title = "Coordinates";
//		coordinatesGroup.setLayout(new BoxLayout(coordinatesGroup,BoxLayout.PAGE_AXIS));
//		coordinatesGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		coordinatesGroup.setVisible(false);
		
//		// init sub-components
//		coordinatesGroup.add(Box.createRigidArea(new Dimension(groupWidth, 20)));
		coordinatesPanel = new CoordinatesPanel();
		
		// add components to group
		coordinatesGroup.add(coordinatesPanel);	
	}

	/**
	 * Toggles the visibility of the coordinates group on or off.
	 * @param show whether to show or hide the group
	 */
	public void showCoordinatesGroup(boolean show) {
		coordinatesGroup.setVisible(show);
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
		
		// initialize group
		multiModelGroup = new JPanel();
		multiModelGroup.setLayout(new BoxLayout(multiModelGroup,BoxLayout.PAGE_AXIS));
		String title = "Contact weights";
		multiModelGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
	    multiModelGroup.setVisible(false);
		
		// initialize components
		histogramPanel = new HistogramPanel();
		histogramPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		discretizationSlider = new JSlider();
		discretizationSlider.setMinorTickSpacing(10);
		discretizationSlider.setMajorTickSpacing(50);
		discretizationSlider.setMaximum(100);
		discretizationSlider.setPaintTicks(true);
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(1),new JLabel("0"));
		labels.put(new Integer(100), new JLabel("1"));
		discretizationSlider.setLabelTable(labels);
		discretizationSlider.setPaintLabels(true);
		discretizationSlider.setEnabled(false);
		discretizationSlider.addChangeListener(this);
		discretizeCheckBox = new JCheckBox("discretize");
		discretizeCheckBox.setSelected(false);
		
		discretizeCheckBox.setAlignmentX(CENTER_ALIGNMENT);
		discretizeCheckBox.addItemListener(this);	// events are handled by this class because the effects are strictly local
		discretizeButton = new JButton("Apply permanently");
		discretizeButton.setAlignmentX(CENTER_ALIGNMENT);
		discretizeButton.setEnabled(false);
		discretizeButton.addActionListener(this);
		// place two check boxes horizontally
		l10checkBox = new JCheckBox("len/10");
		l10checkBox.setEnabled(false);
		l10checkBox.addItemListener(this);
		l5checkBox = new JCheckBox("len/5");
		l5checkBox.setEnabled(false);
		l5checkBox.addItemListener(this);
		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel,BoxLayout.LINE_AXIS));
		checkBoxPanel.add(l10checkBox);
		checkBoxPanel.add(l5checkBox);
		checkBoxPanel.setAlignmentX(CENTER_ALIGNMENT);

		// adding components to group
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));
	    multiModelGroup.add(discretizeCheckBox);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));
		multiModelGroup.add(histogramPanel);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));    
	    multiModelGroup.add(discretizationSlider);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));    	    
	    multiModelGroup.add(checkBoxPanel);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,10)));
	    multiModelGroup.add(discretizeButton);
	    multiModelGroup.add(Box.createRigidArea(new Dimension(groupWidth,10)));
	    groupsPanel.add(multiModelGroup);
	}
	
	/**
	 * Toggles the visibility of the overlays group on or off.
	 * @param show whether to show or hide the overlay group
	 * @param mod the model for which a histogram will be shown
	 */
	public void showMultiModelGroup(boolean show, Model mod) {
		if(Start.USE_EXPERIMENTAL_FEATURES && Start.SHOW_WEIGHTED_CONTACTS) {
			if(show) calculateHistogram(mod);
			this.multiModelGroup.setVisible(show);
		}
	}
	
	
	/**
	 * initializes controls for a generic residue scoring function background
	 */
	
	public void initResidueScoringFunctionGroup(ResidueContactScoringFunction f,boolean bottom) {
		if (bottom) {
			if(this.bottomScoringFunctionController != null) {
				groupsPanel.remove(bottomScoringFunctionController);
			}
			this.bottomScoringFunctionController = new ScoringFunctionController(f,this);
			groupsPanel.add(bottomScoringFunctionController);
			bottomScoringFunctionController.setVisible(true);
		} else {
			if (topScoringFunctionController!= null) {
				groupsPanel.remove(topScoringFunctionController);
			}
			this.topScoringFunctionController = new ScoringFunctionController(f,this);
			groupsPanel.add(topScoringFunctionController);
			topScoringFunctionController.setVisible(true);
		}
		
	}
	
	/**
	 * Initialize controls for delta rank mode
	 */
	public void initDeltaRankGroup() {
		
		// init group
		deltaRankGroup = new JPanel();
		deltaRankGroup.setLayout(new BoxLayout(deltaRankGroup,BoxLayout.PAGE_AXIS));
		String title = "Delta Rank";
		deltaRankGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		
		// init gui components
		deltaRankLable = new JLabel(" ");
		//deltaRankLable.setBounds(5, 5, 100, 20);
		deltaRankLable.setAlignmentX(CENTER_ALIGNMENT);
		addBestDRContactButton = new JButton("add best");
		addBestDRContactButton.addActionListener(this);
		addBestDRContactButton.setMaximumSize(new Dimension(120,50));
		addBestDRContactButton.setAlignmentX(CENTER_ALIGNMENT);
		removeWorstDRContactButton = new JButton("remove worst");
		removeWorstDRContactButton.setMaximumSize(new Dimension(120,50));
		removeWorstDRContactButton.setAlignmentX(CENTER_ALIGNMENT);
		removeWorstDRContactButton.addActionListener(this);
		// add components to group
		deltaRankGroup.add(Box.createRigidArea(new Dimension(groupWidth,5)));	// defines component width
		deltaRankGroup.add(deltaRankLable);
		deltaRankGroup.add(Box.createRigidArea(new Dimension(0,10)));
		deltaRankGroup.add(addBestDRContactButton);
		deltaRankGroup.add(Box.createRigidArea(new Dimension(0,5)));
		deltaRankGroup.add(removeWorstDRContactButton);
		deltaRankGroup.add(Box.createRigidArea(new Dimension(0,5)));
		
		// add group to StatusBar
		deltaRankGroup.setVisible(false);
		groupsPanel.add(deltaRankGroup);
	}
	
	/**
	 * Toggles the visibility of the overlays group on or off.
	 * @param show whether to show or hide the overlay group
	 */
	public void showDeltaRankGroup(boolean show) {
		this.deltaRankGroup.setVisible(show);
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

		// discretize check box (switch discretization on/off)
		if(e.getItemSelectable() == discretizeCheckBox) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				discretizationSlider.setEnabled(false);
				discretizeButton.setEnabled(false);
				l5checkBox.setEnabled(false);
				l10checkBox.setEnabled(false);
				controller.handleSwitchDiscretizeOff();
			} else {
				discretizationSlider.setEnabled(true);
				discretizeButton.setEnabled(true);
				l5checkBox.setEnabled(true);
				l10checkBox.setEnabled(true);
				if(l5checkBox.isSelected()) {
					controller.handleSwitchDiscretizeOn(5);
				} else
				if(l10checkBox.isSelected()) {
					controller.handleSwitchDiscretizeOn(10);
				} else {
					controller.handleSwitchDiscretizeOn(discretizationSlider.getValue() / 100.0);
				}
			}
		}
		
		// l/5 check box
		if(e.getItemSelectable() == l5checkBox) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				if(!l10checkBox.isSelected()) {
					discretizationSlider.setEnabled(true);
					controller.handleChangeDiscretization(discretizationSlider.getValue() / 100.0);
				}
			} else {
				discretizationSlider.setEnabled(false);
				l10checkBox.setSelected(false);
				controller.handleChangeDiscretization(5);
			}
		}
		
		// l/10 check box
		if(e.getItemSelectable() == l10checkBox) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				if(!l5checkBox.isSelected()) {
					discretizationSlider.setEnabled(true);
					controller.handleChangeDiscretization(discretizationSlider.getValue() / 100.0);
				}
			} else {
				discretizationSlider.setEnabled(false);
				l5checkBox.setSelected(false);
				controller.handleChangeDiscretization(10);
			}
		}
	}
	
	/**
	 * Handle local button events
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == discretizeButton) {
			//System.out.println("Discretize button was pressed. Threshold = " + discretizationSlider.getValue());
			if(l5checkBox.isSelected()) {
				controller.handleApplyDiscretizationPermanently(5);
			} else if(l10checkBox.isSelected()) {
				controller.handleApplyDiscretizationPermanently(10);
			}
			controller.handleApplyDiscretizationPermanently(discretizationSlider.getValue() / 100.0);
		}
		if(e.getSource() == firstViewCB) {
			controller.handleBgOverlayChange(false,firstViewCB.getSelectedItem());
		}
		if(e.getSource() == secondViewCB) {
			controller.handleBgOverlayChange(true,secondViewCB.getSelectedItem());
		}
		if(e.getSource() == addBestDRContactButton) {
			controller.handleAddBestDR();
		}
		if(e.getSource() == removeWorstDRContactButton) {
			controller.handleDeleteWorstDR();
		}
	}
	
	/**
	 * Handle local change events
	 */
	public void stateChanged(ChangeEvent e) {
		
		if(e.getSource() == discretizationSlider) {
			controller.handleChangeDiscretization(discretizationSlider.getValue() / 100.0);
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
	
	/**
	 * Enable the option to choose difference map as a background overlay.
	 * This is being called when compare mode is switched on. Currently, it
	 * is not possible to exit compare mode (other than closing the window)
	 * so there is no need for a disableDifferenceMapOverlay() method.
	 */
	public void enableDifferenceMapOverlay() {
		firstViewCB.addItem(View.BgOverlayType.DIFF_DIST.getItem());
		secondViewCB.addItem(View.BgOverlayType.DIFF_DIST.getItem());			
	}
	
	/**
	 * Calculates the histogram for the graph in the given model.
	 * @param mod the model containing the graph for which the histogram will be calculated
	 */
	public void calculateHistogram(Model mod) {
		this.histogramPanel.calculateHistogram(mod);
	}

	public void updateScoringFunctions() {
		if(topScoringFunctionController != null) {
			topScoringFunctionController.updateScores();
		}
		if(bottomScoringFunctionController != null) {
			bottomScoringFunctionController.updateScores();
		}
		
	}
	
	
	
}
