package cmview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransferFunctionBar extends JPanel implements ActionListener, ItemListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final int DEFAULT_WIDTH = 150;
	protected static final int DEFAULT_HEIGHT = 200;
	
	protected static final int DEFAULT_RED_VAL = 255;
	protected static final int DEFAULT_GREEN_VAL = 255;
	protected static final int DEFAULT_BLUE_VAL = 255;
	protected static final int DEFAULT_ALPHA_VAL = 255;
	
	private final static String[] slopeTypes = new String[] {"Increasing", "Decreasing", "Incr->Decr", "Decr->Incr"};
	
	/*--------------------------- member variables --------------------------*/	
	// settings
	private int width = DEFAULT_WIDTH;						// width of this component, height matches contact map size
	private int height = DEFAULT_HEIGHT-20;
	
//	private boolean includeDeltaRank = false;  // change depending on if database was found
	
	TransferFunctionDialog parentFrame;
	View controller;
	
	// main panels
	private JPanel componentGroups;
	private JPanel componentGroup2;
	private JPanel redCGroup, greenCGroup, blueCGroup, alphaCGroup;
	private JPanel buttonGroup;
	
	private JComboBox redValCB, greenValCB, blueValCB, alphaValCB;
	private String[] inputValTypes;
	private JComboBox redSlopeType, greenSlopeType, blueSlopeType, alphaSlopeType;
	private JCheckBox redSteepB, greenSteepB, blueSteepB, alphaSteepB;
	private JTextField redNField, greenNField, blueNField, alphaNField;
	private JTextField redValField, greenValField, blueValField, alphaValField;
	private JButton updateViewB;
	
//	private ImageIcon 
	private JLabel iconL_red_fct, iconL_green_fct, iconL_blue_fct, iconL_alpha_fct;
	
	private int redValInputType, greenValInputType, blueValInputType, alphaValInputType;
	private final int minNVal = 1;
//	private int redNVal=minNVal, greenNVal=minNVal, blueNVal=minNVal, alphaNVal=minNVal;
	private int defRedVal=DEFAULT_RED_VAL, defGreenVal=DEFAULT_GREEN_VAL, defBlueVal=DEFAULT_BLUE_VAL, defAlphaVal=DEFAULT_ALPHA_VAL;
	private JLabel redFctLabel, greenFctLabel, blueFctLabel, alphaFctLabel;
	
	public TransferFunctionBar(TransferFunctionDialog tfDialog, View view){
		this.parentFrame = tfDialog;
		this.controller = view;
		
		// init basic border layout
		this.setLayout(new BorderLayout(0,0));
		this.componentGroups = new JPanel();
		this.componentGroups.setLayout(new BoxLayout(this.componentGroups, BoxLayout.LINE_AXIS));
		this.componentGroups.setBorder(BorderFactory.createEmptyBorder(2,5,0,5));
		this.componentGroup2 = new JPanel();
		this.componentGroup2.setLayout(new BoxLayout(this.componentGroup2, BoxLayout.LINE_AXIS));
		this.componentGroup2.setBorder(BorderFactory.createEmptyBorder(2,5,0,5));
		
		this.add(this.componentGroups, BorderLayout.PAGE_START);
		this.add(this.componentGroup2, BorderLayout.PAGE_END);
		
		initRedCPanel();
		initGreenCPanel();
		initBlueCPanel();
		initAlphaCPanel();
		initUpdateB();
		
		this.inputValTypes = new String[this.redValCB.getItemCount() - 1];
		for (int i=1; i<this.redValCB.getItemCount(); i++)
			this.inputValTypes[i-1] = this.redValCB.getItemAt(i).toString();
	}
	
	private void initUpdateB() {
		// TODO Auto-generated method stub
		this.buttonGroup = new JPanel();
		this.buttonGroup.setSize(4*width, 20);
		this.buttonGroup.setLayout(new BoxLayout(this.buttonGroup, BoxLayout.LINE_AXIS));
		
		updateViewB = new JButton("Update CMView");
		updateViewB.setEnabled(true);
		updateViewB.addActionListener(this);
		this.buttonGroup.add(updateViewB, BorderLayout.LINE_END);
		this.componentGroup2.add(this.buttonGroup);
	}

	private void initAlphaCPanel() {
		// TODO Auto-generated method stub
		this.alphaCGroup = new JPanel();
		this.alphaCGroup.setSize(width, height);
		this.alphaCGroup.setLayout(new BoxLayout(this.alphaCGroup, BoxLayout.Y_AXIS));
		this.componentGroups.add(this.alphaCGroup);
		
		String title = "Alpha Component";
		this.alphaCGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		this.alphaCGroup.setVisible(true);
		this.alphaCGroup.setMinimumSize(new Dimension(width,height));
		this.alphaCGroup.setPreferredSize(new Dimension(width,height));
		
		this.alphaValCB = initValCB(this.alphaValCB);
		
		JPanel linePanel1 = new JPanel();
		linePanel1.setLayout(new BoxLayout(linePanel1,BoxLayout.LINE_AXIS));
		this.alphaSlopeType = initSlopeTypeCB(this.alphaSlopeType);
		linePanel1.add(this.alphaSlopeType, BorderLayout.LINE_START);

		JPanel linePanel2 = new JPanel();
		linePanel2.setLayout(new BoxLayout(linePanel2,BoxLayout.LINE_AXIS));
		this.alphaSteepB = initCheckBoxButton(this.alphaSteepB,       "Steep             ");
		this.alphaSteepB.setEnabled(false);
		linePanel2.add(this.alphaSteepB, BorderLayout.LINE_START);
		
		
		this.alphaNField = new JTextField(String.valueOf(this.minNVal));
		this.alphaNField.setMaximumSize(new Dimension(30, 22));
		this.alphaNField.setEnabled(false); // set to true as soon as combo box changed
		this.alphaNField.addActionListener(this);
		
		JPanel linePanel3 = new JPanel();
		linePanel3.setLayout(new BoxLayout(linePanel3,BoxLayout.LINE_AXIS));
		String fctName = getFctName(this.alphaSlopeType, this.alphaSteepB, this.alphaNField);
		this.alphaFctLabel = new JLabel(fctName);
		linePanel3.add(this.alphaFctLabel, BorderLayout.LINE_START);
		
		JLabel nValLabel = new JLabel(" n = ");		
		JPanel nLinePanel = new JPanel();
		nLinePanel.setLayout(new BoxLayout(nLinePanel,BoxLayout.LINE_AXIS));
		nLinePanel.add(nValLabel, BorderLayout.LINE_START);
		nLinePanel.add(this.alphaNField, BorderLayout.LINE_END);
		
		JPanel defLinePanel = new JPanel();
		this.alphaValField = new JTextField(String.valueOf(this.defAlphaVal));
		this.alphaValField.setEnabled(true); // set to false as soon as combo box changed
		this.alphaValField.addActionListener(this);
		defLinePanel.setLayout(new BoxLayout(defLinePanel,BoxLayout.LINE_AXIS));
		defLinePanel.add(new JLabel("defV= "), BorderLayout.LINE_START);
		defLinePanel.add(this.alphaValField, BorderLayout.LINE_END);
		
		ImageIcon icon_fct = getFctIcon(this.alphaSlopeType, this.alphaSteepB, this.alphaNField);
		this.iconL_alpha_fct = new JLabel(icon_fct);
		
		JPanel rowPanel1 = new JPanel();
		rowPanel1.setLayout(new BoxLayout(rowPanel1, BoxLayout.PAGE_AXIS));
		rowPanel1.add(nLinePanel, BorderLayout.PAGE_START);
		rowPanel1.add(defLinePanel, BorderLayout.PAGE_END);
		JPanel rowPanel2 = new JPanel();
		rowPanel2.setLayout(new BoxLayout(rowPanel2, BoxLayout.PAGE_AXIS));
		rowPanel2.add(this.iconL_alpha_fct);
		JPanel linePanel4 = new JPanel();
		linePanel4.setLayout(new BoxLayout(linePanel4,BoxLayout.LINE_AXIS));
		linePanel4.add(rowPanel1, BorderLayout.LINE_START);
		linePanel4.add(rowPanel2, BorderLayout.LINE_END);		
		
		this.alphaCGroup.add(this.alphaValCB);
		this.alphaCGroup.add(Box.createRigidArea(new Dimension(0,5)));
		this.alphaCGroup.add(linePanel1); //(this.alphaIncreaseB);
		this.alphaCGroup.add(linePanel2); //(this.alphaSteepB);
		this.alphaCGroup.add(linePanel3); //(fctLabel);
		this.alphaCGroup.add(linePanel4);		
	}



	private void initBlueCPanel() {
		// TODO Auto-generated method stub
		this.blueCGroup = new JPanel();
		this.blueCGroup.setSize(width, height);
		this.blueCGroup.setLayout(new BoxLayout(this.blueCGroup, BoxLayout.Y_AXIS));
		this.componentGroups.add(this.blueCGroup);
		
		String title = "Blue Component";
		this.blueCGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		this.blueCGroup.setVisible(true);
		this.blueCGroup.setMinimumSize(new Dimension(width,height));
		this.blueCGroup.setPreferredSize(new Dimension(width,height));
		
		this.blueValCB = initValCB(this.blueValCB);
		
		JPanel linePanel1 = new JPanel();
		linePanel1.setLayout(new BoxLayout(linePanel1,BoxLayout.LINE_AXIS));
		this.blueSlopeType = initSlopeTypeCB(this.blueSlopeType);
		linePanel1.add(this.blueSlopeType, BorderLayout.LINE_START);

		JPanel linePanel2 = new JPanel();
		linePanel2.setLayout(new BoxLayout(linePanel2,BoxLayout.LINE_AXIS));
		this.blueSteepB = initCheckBoxButton(this.blueSteepB,       "Steep             ");
		this.blueSteepB.setEnabled(false);
		linePanel2.add(this.blueSteepB, BorderLayout.LINE_START);

		this.blueNField = new JTextField(String.valueOf(this.minNVal));
		this.blueNField.setMaximumSize(new Dimension(30, 22));
		this.blueNField.setEnabled(false); // set to true as soon as combo box changed
		this.blueNField.addActionListener(this);
		
		JPanel linePanel3 = new JPanel();
		linePanel3.setLayout(new BoxLayout(linePanel3,BoxLayout.LINE_AXIS));
		String fctName = getFctName(this.blueSlopeType, this.blueSteepB, this.blueNField);
		this.blueFctLabel = new JLabel(fctName);
		linePanel3.add(this.blueFctLabel, BorderLayout.LINE_START);
		
		JLabel nValLabel = new JLabel("n = ");		
		JPanel nLinePanel = new JPanel();
		nLinePanel.setLayout(new BoxLayout(nLinePanel,BoxLayout.LINE_AXIS));
		nLinePanel.add(nValLabel, BorderLayout.LINE_START);
		nLinePanel.add(this.blueNField, BorderLayout.LINE_END);
		
		JPanel defLinePanel = new JPanel();
		this.blueValField = new JTextField(String.valueOf(this.defBlueVal));
		this.blueValField.setEnabled(true); // set to false as soon as combo box changed
		this.blueValField.addActionListener(this);
		defLinePanel.setLayout(new BoxLayout(defLinePanel,BoxLayout.LINE_AXIS));
		defLinePanel.add(new JLabel("defV= "), BorderLayout.LINE_START);
		defLinePanel.add(this.blueValField, BorderLayout.LINE_END);
		
		ImageIcon icon_fct = getFctIcon(this.blueSlopeType, this.blueSteepB, this.blueNField);
		this.iconL_blue_fct = new JLabel(icon_fct);
		
		JPanel rowPanel1 = new JPanel();
		rowPanel1.setLayout(new BoxLayout(rowPanel1, BoxLayout.PAGE_AXIS));
		rowPanel1.add(nLinePanel, BorderLayout.PAGE_START);
		rowPanel1.add(defLinePanel, BorderLayout.PAGE_END);
		JPanel rowPanel2 = new JPanel();
		rowPanel2.setLayout(new BoxLayout(rowPanel2, BoxLayout.PAGE_AXIS));
		rowPanel2.add(this.iconL_blue_fct);
		JPanel linePanel4 = new JPanel();
		linePanel4.setLayout(new BoxLayout(linePanel4,BoxLayout.LINE_AXIS));
		linePanel4.add(rowPanel1, BorderLayout.LINE_START);
		linePanel4.add(rowPanel2, BorderLayout.LINE_END);		
		
		this.blueCGroup.add(this.blueValCB);
		this.blueCGroup.add(Box.createRigidArea(new Dimension(0,5)));
		this.blueCGroup.add(linePanel1); //(this.blueIncreaseB);
		this.blueCGroup.add(linePanel2); //(this.blueSteepB);
		this.blueCGroup.add(linePanel3); //(fctLabel);
		this.blueCGroup.add(linePanel4);	
		
	}



	private void initGreenCPanel() {
		// TODO Auto-generated method stub
		this.greenCGroup = new JPanel();
		this.greenCGroup.setSize(width, height);
		this.greenCGroup.setLayout(new BoxLayout(this.greenCGroup, BoxLayout.Y_AXIS));
		this.componentGroups.add(this.greenCGroup);
		
		String title = "Green Component";
		this.greenCGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		this.greenCGroup.setVisible(true);
		this.greenCGroup.setMinimumSize(new Dimension(width,height));
		this.greenCGroup.setPreferredSize(new Dimension(width,height));
		
		this.greenValCB = initValCB(this.greenValCB);
		
		JPanel linePanel1 = new JPanel();
		linePanel1.setLayout(new BoxLayout(linePanel1,BoxLayout.LINE_AXIS));
		this.greenSlopeType = initSlopeTypeCB(this.greenSlopeType);
		linePanel1.add(this.greenSlopeType, BorderLayout.LINE_START);

		JPanel linePanel2 = new JPanel();
		linePanel2.setLayout(new BoxLayout(linePanel2,BoxLayout.LINE_AXIS));
		this.greenSteepB = initCheckBoxButton(this.greenSteepB,       "Steep             ");
		this.greenSteepB.setEnabled(false);
		linePanel2.add(this.greenSteepB, BorderLayout.LINE_START);

		this.greenNField = new JTextField(String.valueOf(this.minNVal));
		this.greenNField.setMaximumSize(new Dimension(30, 22));
		this.greenNField.setEnabled(false); // set to true as soon as combo box changed
		this.greenNField.addActionListener(this);
		
		JPanel linePanel3 = new JPanel();
		linePanel3.setLayout(new BoxLayout(linePanel3,BoxLayout.LINE_AXIS));
		String fctName = getFctName(this.greenSlopeType, this.greenSteepB, this.greenNField);
		this.greenFctLabel = new JLabel(fctName);
		linePanel3.add(this.greenFctLabel, BorderLayout.LINE_START);
		
		JLabel nValLabel = new JLabel("n = ");		
		JPanel nLinePanel = new JPanel();
		nLinePanel.setLayout(new BoxLayout(nLinePanel,BoxLayout.LINE_AXIS));
		nLinePanel.add(nValLabel, BorderLayout.LINE_START);
		nLinePanel.add(this.greenNField, BorderLayout.LINE_END);
		
		JPanel defLinePanel = new JPanel();
		this.greenValField = new JTextField(String.valueOf(this.defGreenVal));
		this.greenValField.setEnabled(true); // set to false as soon as combo box changed
		this.greenValField.addActionListener(this);
		defLinePanel.setLayout(new BoxLayout(defLinePanel,BoxLayout.LINE_AXIS));
		defLinePanel.add(new JLabel("defV= "), BorderLayout.LINE_START);
		defLinePanel.add(this.greenValField, BorderLayout.LINE_END);
		
		ImageIcon icon_fct = getFctIcon(this.greenSlopeType, this.greenSteepB, this.greenNField);
		this.iconL_green_fct = new JLabel(icon_fct);
		
		JPanel rowPanel1 = new JPanel();
		rowPanel1.setLayout(new BoxLayout(rowPanel1, BoxLayout.PAGE_AXIS));
		rowPanel1.add(nLinePanel, BorderLayout.PAGE_START);
		rowPanel1.add(defLinePanel, BorderLayout.PAGE_END);
		JPanel rowPanel2 = new JPanel();
		rowPanel2.setLayout(new BoxLayout(rowPanel2, BoxLayout.PAGE_AXIS));
		rowPanel2.add(this.iconL_green_fct);
		JPanel linePanel4 = new JPanel();
		linePanel4.setLayout(new BoxLayout(linePanel4,BoxLayout.LINE_AXIS));
		linePanel4.add(rowPanel1, BorderLayout.LINE_START);
		linePanel4.add(rowPanel2, BorderLayout.LINE_END);		
		
		this.greenCGroup.add(this.greenValCB);
		this.greenCGroup.add(Box.createRigidArea(new Dimension(0,5)));
		this.greenCGroup.add(linePanel1); //(this.greenIncreaseB);
		this.greenCGroup.add(linePanel2); //(this.greenSteepB);
		this.greenCGroup.add(linePanel3); //(fctLabel);
		this.greenCGroup.add(linePanel4);		
	}

	private void initRedCPanel() {
		// TODO Auto-generated method stub
		this.redCGroup = new JPanel();
		this.redCGroup.setSize(width, height);
		this.redCGroup.setLayout(new BoxLayout(this.redCGroup, BoxLayout.Y_AXIS));
		this.componentGroups.add(this.redCGroup);
		
		String title = "Red Component";
		this.redCGroup.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		this.redCGroup.setVisible(true);
		this.redCGroup.setMinimumSize(new Dimension(width,height));
		this.redCGroup.setPreferredSize(new Dimension(width,height));
		
		this.redValCB = initValCB(this.redValCB);
		
		JPanel linePanel1 = new JPanel();
		linePanel1.setLayout(new BoxLayout(linePanel1,BoxLayout.LINE_AXIS));
		this.redSlopeType = initSlopeTypeCB(this.redSlopeType);
		linePanel1.add(this.redSlopeType, BorderLayout.LINE_START);

		JPanel linePanel2 = new JPanel();
		linePanel2.setLayout(new BoxLayout(linePanel2,BoxLayout.LINE_AXIS));
		this.redSteepB = initCheckBoxButton(this.redSteepB,       "Steep             ");
		this.redSteepB.setEnabled(false);
		linePanel2.add(this.redSteepB, BorderLayout.LINE_START);

		this.redNField = new JTextField(String.valueOf(this.minNVal));
		this.redNField.setMaximumSize(new Dimension(30, 22));
		this.redNField.setEnabled(false); // set to true as soon as combo box changed
		this.redNField.addActionListener(this);
		
		JPanel linePanel3 = new JPanel();
		linePanel3.setLayout(new BoxLayout(linePanel3,BoxLayout.LINE_AXIS));
		String fctName = getFctName(this.redSlopeType, this.redSteepB, this.redNField);
		this.redFctLabel = new JLabel(fctName);
		linePanel3.add(this.redFctLabel, BorderLayout.LINE_START);
		
		JLabel nValLabel = new JLabel("n = ");		
		JPanel nLinePanel = new JPanel();
		nLinePanel.setLayout(new BoxLayout(nLinePanel,BoxLayout.LINE_AXIS));
		nLinePanel.add(nValLabel, BorderLayout.LINE_START);
		nLinePanel.add(this.redNField, BorderLayout.LINE_END);
		
		JPanel defLinePanel = new JPanel();
		this.redValField = new JTextField(String.valueOf(this.defRedVal));
		this.redValField.setEnabled(true); // set to false as soon as combo box changed
		this.redValField.addActionListener(this);
		defLinePanel.setLayout(new BoxLayout(defLinePanel,BoxLayout.LINE_AXIS));
		defLinePanel.add(new JLabel("defV= "), BorderLayout.LINE_START);
		defLinePanel.add(this.redValField, BorderLayout.LINE_END);
		
		ImageIcon icon_fct = getFctIcon(this.redSlopeType, this.redSteepB, this.redNField);
		this.iconL_red_fct = new JLabel(icon_fct);
		
		JPanel rowPanel1 = new JPanel();
		rowPanel1.setLayout(new BoxLayout(rowPanel1, BoxLayout.PAGE_AXIS));
		rowPanel1.add(nLinePanel, BorderLayout.PAGE_START);
		rowPanel1.add(defLinePanel, BorderLayout.PAGE_END);
		JPanel rowPanel2 = new JPanel();
		rowPanel2.setLayout(new BoxLayout(rowPanel2, BoxLayout.PAGE_AXIS));
		rowPanel2.add(this.iconL_red_fct);
		JPanel linePanel4 = new JPanel();
		linePanel4.setLayout(new BoxLayout(linePanel4,BoxLayout.LINE_AXIS));
		linePanel4.add(rowPanel1, BorderLayout.LINE_START);
		linePanel4.add(rowPanel2, BorderLayout.LINE_END);		
		
		this.redCGroup.add(this.redValCB);
		this.redCGroup.add(Box.createRigidArea(new Dimension(0,5)));
		this.redCGroup.add(linePanel1); //(this.redIncreaseB);
		this.redCGroup.add(linePanel2); //(this.redSteepB);
		this.redCGroup.add(linePanel3); //(fctLabel);
		this.redCGroup.add(linePanel4);	
	}
	
	private String getFctName(JComboBox slopeType, JCheckBox steepB, JTextField nValF){
		String name;
		int nVal = Integer.valueOf(nValF.getText());
		switch (slopeType.getSelectedIndex()){
		case 0: // Increase
			if (!steepB.isSelected() || nVal==1)
				name = "y=x^n";
			else
				name = "y=1-(1-x)^n";
			break;
		case 1: // Decrease
			if (!steepB.isSelected() || nVal==1)
				name = "y=1-x^n";
			else
				name = "y=(1-x)^n";
			break;
		case 2: // incr->decr
			if (!steepB.isSelected() || nVal==1)
				name = "y=(2x)^n y=(2x-2)^n";
			else
				name = "y=1-(2x-1)^n";
			break;
		case 3: // decr->incr
			if (!steepB.isSelected() || nVal==1)
				name = "y=1-(2x)^n y=1-(2x-2)^n";
			else
				name = "y=(2x-1)^n";
			break;
		default:
			name = "";
		}
		
		return name;
	}
	
	private ImageIcon getFctIcon(JComboBox slopeType, JCheckBox steepB, JTextField nValF){
		String image = "";
		int nVal = Integer.valueOf(nValF.getText());
		switch (slopeType.getSelectedIndex()){
		case 0: // Increase
			image += "inc_";
			break;
		case 1: // Decrease
			image += "dec_";
			break;
		case 2: // incr->decr
			image += "inc-dec_";
			break;
		case 3: // decr->incr
			image += "dec-inc_";
			break;
		default:
			image += "";
		}
		if (nVal==1)
			image += "n1";
		else{
			if (steepB.isSelected())
				image += "s_n";
			else
				image += "n";
		}
		image += ".png";
		
		ImageIcon icon_fct = new ImageIcon(this.getClass().getResource(Start.FCT_ICON_DIR + image));
		return icon_fct;		
	}
	
	private double getFctValue(double val, JComboBox slopeType, JCheckBox steepB, JTextField nValF){
		int nVal = Integer.valueOf(nValF.getText());
		double n = (double)nVal;
		switch (slopeType.getSelectedIndex()){
		case 0:
			if (!steepB.isSelected() || nVal==1)
				val = Math.pow(val, n);
			else
				val = 1-Math.pow(1-val,n);
			break;
		case 1:
			if (!steepB.isSelected() || nVal==1)
				val = 1-Math.pow(val, n);
			else
				val = Math.pow(1-val, n);
			break;
		case 2: 
			if (!steepB.isSelected() || nVal==1){
				if (val<=0.5)
					val = Math.pow(2*val, n); 
				else
					val = Math.pow(2*val-2, n);
			}
			else{
				val = 1-Math.pow(2*val-1, n);
			}
			break;
		case 3:
			if (!steepB.isSelected() || nVal==1){
				if (val<=0.5)
					val = 1-Math.pow(2*val, n);
				else
					val = 1-Math.pow(2*val-2, n); 
			}
			else{
				val = Math.pow(2*val-1, n);
			}
			break;
		default:
			val = 0;
		}
		
		return val;
	}


	private JCheckBox initCheckBoxButton(JCheckBox cbButton, String name) {
		// TODO Auto-generated method stub
		cbButton = new JCheckBox(name);
		cbButton.setSelected(true);
		cbButton.addItemListener(this);
		
		return cbButton;
	}
	
	private JComboBox initSlopeTypeCB(JComboBox slopeCB){
		slopeCB = new JComboBox(slopeTypes);
		slopeCB.setSelectedIndex(0);
		slopeCB.setSize(150, 20);
		slopeCB.setMaximumSize(new Dimension(150,20));
		slopeCB.setMinimumSize(new Dimension(150,20));
		slopeCB.addActionListener(this);
		slopeCB.setAlignmentX(CENTER_ALIGNMENT);
		slopeCB.setEnabled(false); // set to true as soon as combo box changed
		return slopeCB;
	}

	private JComboBox initValCB(JComboBox valCB){
		valCB = new JComboBox();
		valCB.addItem((Object)"UseDefaultVal");
		View.BgOverlayType[] viewOptions = {View.BgOverlayType.DENSITY, View.BgOverlayType.DISTANCE}; // View.BgOverlayType.ENERGY
		for (View.BgOverlayType t: viewOptions) {
			valCB.addItem(t.getItem());
		}
		
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			View.BgOverlayType[] viewOptions2 = {View.BgOverlayType.COMMON_NBH};
//			View.BgOverlayType[] viewOptions2 = {View.BgOverlayType.COMMON_NBH, View.BgOverlayType.DELTA_RANK};
			for (View.BgOverlayType t: viewOptions2) {
				valCB.addItem(t.getItem());
			}
			if (this.controller.isDatabaseConnectionAvailable())//(includeDeltaRank)
				valCB.addItem(View.BgOverlayType.DELTA_RANK.getItem());
//			for (ResidueContactScoringFunction f : scoringFunctions) {
//				valCB.addItem(f.getMethodName());
//			}
		}
		
//		valCB.setEditable(true); // this should actually be false, but we want the white background
		valCB.setSize(150, 20);
		valCB.setMaximumSize(new Dimension(150,20));
		valCB.setMinimumSize(new Dimension(150,20));
		valCB.addActionListener(this);
		valCB.setAlignmentX(CENTER_ALIGNMENT);
		return valCB;
	}
	
	/*---------------------------- handling ------------------------------------*/

	@SuppressWarnings("unused")
	private void updateView() {
		// TODO Auto-generated method stub
		// test loop for colour scaling
		String fctName = getFctName(this.redSlopeType, this.redSteepB, this.redNField);
		System.out.println("function: "+fctName);
		for (int i=0; i<=10; i++){
			double val = (double)i/10;
			int redC = getRedComp(val);
			System.out.print(val+"->"+redC+"\t");
		}
		System.out.println();
	}
	

	/*---------------------------- getters and setters -------------------------*/
	
//	public boolean useDeltaRank(){
//		return this.includeDeltaRank;
//	}
		
	public void setValInputType(char colType, String type){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			this.redValCB.setSelectedItem(type); // .getSelectedItem().toString();
		else if (colType=='g')
			this.greenValCB.setSelectedItem(type);
		else if (colType=='b')
			this.blueValCB.setSelectedItem(type);
		else if (colType=='a')
			this.alphaValCB.setSelectedItem(type);
		
	}
	
	public String getValInputType(char colType){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			return this.redValCB.getSelectedItem().toString();
		else if (colType=='g')
			return this.greenValCB.getSelectedItem().toString();
		else if (colType=='b')
			return this.blueValCB.getSelectedItem().toString();
		else if (colType=='a')
			return this.alphaValCB.getSelectedItem().toString();
		
		return "None";
	}
	
	public void setSlopeType(char colType, String type){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			this.redSlopeType.setSelectedItem(type); // .getSelectedItem().toString();
		else if (colType=='g')
			this.greenSlopeType.setSelectedItem(type);
		else if (colType=='b')
			this.blueSlopeType.setSelectedItem(type);
		else if (colType=='a')
			this.alphaSlopeType.setSelectedItem(type);
		
	}
	
	public String getSlopeType(char colType){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			return this.redSlopeType.getSelectedItem().toString();
		else if (colType=='g')
			return this.greenSlopeType.getSelectedItem().toString();
		else if (colType=='b')
			return this.blueSlopeType.getSelectedItem().toString();
		else if (colType=='a')
			return this.alphaSlopeType.getSelectedItem().toString();
		
		return "none";
	}

	public String[] getInputValTypes(){
		return this.inputValTypes;
	}
	
	public void setSteep(char colType, boolean steep){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			this.redSteepB.setSelected(steep); // .getSelectedItem().toString();
		else if (colType=='g')
			this.greenSteepB.setSelected(steep);
		else if (colType=='b')
			this.blueSteepB.setSelected(steep);
		else if (colType=='a')
			this.alphaSteepB.setSelected(steep);
		
	}
	
	public boolean getSteep(char colType){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			return this.redSteepB.isSelected();
		else if (colType=='g')
			return this.greenSteepB.isSelected();
		else if (colType=='b')
			return this.blueSteepB.isSelected();
		else if (colType=='a')
			return this.alphaSteepB.isSelected();
		
		return false;
	}
	
	public void setNVal(char colType, int val){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			this.redNField.setText(String.valueOf(val));
		else if (colType=='g')
			this.greenNField.setText(String.valueOf(val));
		else if (colType=='b')
			this.blueNField.setText(String.valueOf(val));
		else if (colType=='a')
			this.alphaNField.setText(String.valueOf(val));
		
	}
	
	public int getNVal(char colType){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			return Integer.valueOf(this.redNField.getText());
		else if (colType=='g')
			return Integer.valueOf(this.greenNField.getText());
		else if (colType=='b')
			return Integer.valueOf(this.blueNField.getText());
		else if (colType=='a')
			return Integer.valueOf(this.alphaNField.getText());
		
		return -1;
	}
	
	public void setDefVal(char colType, int val){
		colType = Character.toLowerCase(colType);
		if (colType=='r'){
			this.redValField.setText(String.valueOf(val));
			this.defRedVal = val;
		}
		else if (colType=='g'){
			this.greenValField.setText(String.valueOf(val));
			this.defGreenVal = val;
		}
		else if (colType=='b'){
			this.blueValField.setText(String.valueOf(val));
			this.defBlueVal = val;
		}
		else if (colType=='a'){
			this.alphaValField.setText(String.valueOf(val));
			this.defAlphaVal = val;	
		}
	}
	
	public int getDefVal(char colType){
		colType = Character.toLowerCase(colType);
		if (colType=='r')
			return this.defRedVal;
		else if (colType=='g')
			return this.defGreenVal;
		else if (colType=='b')
			return this.defBlueVal;
		else if (colType=='a')
			return this.defAlphaVal;
		
		return -1;
	}
	
	public Color getColor(double[] values){
		int redComp = this.defRedVal; //DEFAULT_RED_VAL;
		int greenComp = this.defGreenVal; // DEFAULT_GREEN_VAL;
		int blueComp = this.defBlueVal; // DEFAULT_BLUE_VAL;
		int alpha = this.defAlphaVal; //DEFAULT_ALPHA_VAL;
		
		if (this.redValInputType != 0){
			double val = values[this.redValInputType-1];
//			System.out.println("val="+val+"  for type="+inputValTypes[this.redValInputType]);
			val = getFctValue(val, redSlopeType, redSteepB, redNField);
			if (val>=0 && val<=1)
				redComp = (int) Math.round(val*255);
		}
		if (this.greenValInputType != 0){
			double val = values[this.greenValInputType-1];
			val = getFctValue(val, greenSlopeType, greenSteepB, greenNField);
			if (val>=0 && val<=1)
				greenComp = (int) Math.round(val*255);
		}
		if (this.blueValInputType != 0){
			double val = values[this.blueValInputType-1];
//			System.out.println("val="+val+"  for type="+inputValTypes[this.blueValInputType]);
			val = getFctValue(val, blueSlopeType, blueSteepB, blueNField);
			if (val>=0 && val<=1)
				blueComp = (int) Math.round(val*255);
		}
		if (this.alphaValInputType != 0){
			double val = values[this.alphaValInputType-1];
//			System.out.println("val="+val+"  for type="+inputValTypes[this.alphaValInputType]);
			val = getFctValue(val, alphaSlopeType, alphaSteepB, alphaNField);
			if (val>=0 && val<=1)
				alpha = (int) Math.round(val*255);
		}
						
		Color col = new Color(redComp, greenComp, blueComp, alpha);
		return col;
	}
	
	
	private int getRedComp(double val){
		int redCVal = DEFAULT_RED_VAL;
		if (this.redValCB.getSelectedIndex() != 0){
			double redVal = getFctValue(val, redSlopeType, redSteepB, redNField);
			if (redVal>=0 && redVal<=1)
				redCVal = (int) Math.round(redVal*255);
			else
				System.out.println("Invalid value range");
		}
		return redCVal;
	}


	/*---------------------------- event listening -------------------------*/

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == this.redValCB){
			this.redValInputType = this.redValCB.getSelectedIndex();
			if (this.redValCB.getSelectedIndex() == 0){
//				this.redIncreaseB.setEnabled(false);
				this.redSlopeType.setEnabled(false);
				this.redSteepB.setEnabled(false);
				this.redNField.setEnabled(false);
				this.redValField.setEnabled(true);
			}
			else {
//				this.redIncreaseB.setEnabled(true);
				this.redSlopeType.setEnabled(true);
				this.redSteepB.setEnabled(true);
				this.redNField.setEnabled(true);
				this.redValField.setEnabled(false);
			}
		}
		if (e.getSource() == this.alphaValCB){
			this.alphaValInputType = this.alphaValCB.getSelectedIndex();
			if (this.alphaValCB.getSelectedIndex() == 0){
//				this.alphaIncreaseB.setEnabled(false);
				this.alphaSlopeType.setEnabled(false);
				this.alphaSteepB.setEnabled(false);
				this.alphaNField.setEnabled(false);
				this.alphaValField.setEnabled(true);
			}
			else {
//				this.alphaIncreaseB.setEnabled(true);
				this.alphaSlopeType.setEnabled(true);
				this.alphaSteepB.setEnabled(true);
				this.alphaNField.setEnabled(true);
				this.alphaValField.setEnabled(false);
			}
		}
		if (e.getSource() == this.greenValCB){
			this.greenValInputType = this.greenValCB.getSelectedIndex();
			if (this.greenValCB.getSelectedIndex() == 0){
//				this.greenIncreaseB.setEnabled(false);
				this.greenSlopeType.setEnabled(false);
				this.greenSteepB.setEnabled(false);
				this.greenNField.setEnabled(false);
				this.greenValField.setEnabled(true);
			}
			else {
//				this.greenIncreaseB.setEnabled(true);
				this.greenSlopeType.setEnabled(true);
				this.greenSteepB.setEnabled(true);
				this.greenNField.setEnabled(true);
				this.greenValField.setEnabled(false);
			}
		}
		if (e.getSource() == this.blueValCB){
			this.blueValInputType = this.blueValCB.getSelectedIndex();
			if (this.blueValCB.getSelectedIndex() == 0){
//				this.blueIncreaseB.setEnabled(false);
				this.blueSlopeType.setEnabled(false);
				this.blueSteepB.setEnabled(false);
				this.blueNField.setEnabled(false);
				this.blueValField.setEnabled(true);
			}
			else {
//				this.blueIncreaseB.setEnabled(true);
				this.blueSlopeType.setEnabled(true);
				this.blueSteepB.setEnabled(true);
				this.blueNField.setEnabled(true);
				this.blueValField.setEnabled(false);
			}
		}
		
		if (e.getSource() == this.redSlopeType || e.getSource() == this.redNField){
			if (Integer.valueOf(this.redNField.getText()) < this.minNVal)
				this.redNField.setText(String.valueOf(this.minNVal));
			
			String fctName = getFctName(this.redSlopeType, this.redSteepB, this.redNField);
			this.redFctLabel.setText(fctName);	
			ImageIcon icon_fct = getFctIcon(this.redSlopeType, this.redSteepB, this.redNField);
			this.iconL_red_fct.setIcon(icon_fct);				
		}
		if (e.getSource() == this.greenSlopeType || e.getSource() == this.greenNField){
			if (Integer.valueOf(this.greenNField.getText()) < this.minNVal)
				this.greenNField.setText(String.valueOf(this.minNVal));
			
			String fctName = getFctName(this.greenSlopeType, this.greenSteepB, this.greenNField);
			this.greenFctLabel.setText(fctName);	
			ImageIcon icon_fct = getFctIcon(this.greenSlopeType, this.greenSteepB, this.greenNField);
			this.iconL_green_fct.setIcon(icon_fct);					
		}
		if (e.getSource() == this.blueSlopeType || e.getSource() == this.blueNField){			
			if (Integer.valueOf(this.blueNField.getText()) < this.minNVal)
				this.blueNField.setText(String.valueOf(this.minNVal));
			
			String fctName = getFctName(this.blueSlopeType, this.blueSteepB, this.blueNField);
			this.blueFctLabel.setText(fctName);		
			ImageIcon icon_fct = getFctIcon(this.blueSlopeType, this.blueSteepB, this.blueNField);
			this.iconL_blue_fct.setIcon(icon_fct);	
		}
		if (e.getSource() == this.alphaSlopeType || e.getSource() == this.alphaNField){
			if (Integer.valueOf(this.alphaNField.getText()) < this.minNVal)
				this.alphaNField.setText(String.valueOf(this.minNVal));
			
			String fctName = getFctName(this.alphaSlopeType, this.alphaSteepB, this.alphaNField);
			this.alphaFctLabel.setText(fctName);				
			ImageIcon icon_fct = getFctIcon(this.alphaSlopeType, this.alphaSteepB, this.alphaNField);
			this.iconL_alpha_fct.setIcon(icon_fct);			
		}
		
		if (e.getSource() == this.redValField)
			this.defRedVal = Integer.valueOf(this.redValField.getText());
		if (e.getSource() == this.greenValField)
			this.defGreenVal = Integer.valueOf(this.greenValField.getText());
		if (e.getSource() == this.blueValField)
			this.defBlueVal = Integer.valueOf(this.blueValField.getText());
		if (e.getSource() == this.alphaValField)
			this.defAlphaVal = Integer.valueOf(this.alphaValField.getText());
				
		if (e.getSource() == this.updateViewB){
//			updateView();
			this.controller.handleShowTFbasedMap(true);
		}
		
	}


	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		if (e.getItemSelectable() == this.redSteepB){
			String fctName = getFctName(this.redSlopeType, this.redSteepB, this.redNField);
			this.redFctLabel.setText(fctName);	
			ImageIcon icon_fct = getFctIcon(this.redSlopeType, this.redSteepB, this.redNField);
			this.iconL_red_fct.setIcon(icon_fct);		
		}
		if (e.getItemSelectable() == this.greenSteepB){
			String fctName = getFctName(this.greenSlopeType, this.greenSteepB, this.greenNField);
			this.greenFctLabel.setText(fctName);	
			ImageIcon icon_fct = getFctIcon(this.greenSlopeType, this.greenSteepB, this.greenNField);
			this.iconL_green_fct.setIcon(icon_fct);		
		}
		if (e.getItemSelectable() == this.blueSteepB){
			String fctName = getFctName(this.blueSlopeType, this.blueSteepB, this.blueNField);
			this.blueFctLabel.setText(fctName);	
			ImageIcon icon_fct = getFctIcon(this.blueSlopeType, this.blueSteepB, this.blueNField);
			this.iconL_blue_fct.setIcon(icon_fct);		
		}
		if (e.getItemSelectable() == this.alphaSteepB){
			String fctName = getFctName(this.alphaSlopeType, this.alphaSteepB, this.alphaNField);
			this.alphaFctLabel.setText(fctName);			
			ImageIcon icon_fct = getFctIcon(this.alphaSlopeType, this.alphaSteepB, this.alphaNField);
			this.iconL_alpha_fct.setIcon(icon_fct);
		}
	}

}
