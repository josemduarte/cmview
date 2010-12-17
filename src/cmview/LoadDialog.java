package cmview;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.Document;

import owl.core.structure.AAinfo;
import owl.core.structure.Pdb;
import owl.core.util.actionTools.Getter;
import owl.core.util.actionTools.GetterError;



/**
 * A dialog to load a contact map. This dialog is used by several load commands
 * and displays different input fields depending on the given parameters.
 * The action to be performed when ok is pressed can be passed as a LoadAction instance.
 */
public class LoadDialog extends JDialog implements ActionListener, PopupMenuListener, DocumentListener, ItemListener {  

	/*------------------------------ constants ------------------------------*/
	
	// class variables
	static final long serialVersionUID = 1l;
	private static final String COMBO_FILL_VALUE = ""; 
	private static final int NUMBER_COMBO_FILL_ITEMS = 8;
	
	/*--------------------------- member variables --------------------------*/
	
	/**
	 * Maps some field-identifiers to default values. Make use of the following keys
	 * <ul>
	 * <li>ct -> referring to the previous contact type</li>
	 * <li>dist -> ... contact distance threshold</li>
	 * <li>minss -> ... minimal sequence separation</li>
	 * <li>maxss -> ... maximal sequence separation</li>
	 * <li>model -> ... model index</li>
	 * </ul> 
	 */
	private static TreeMap<String,String> field2defValue = new TreeMap<String, String>();
	private static final String LABEL_AFTER_COMBO_BOX = "click to load";
	
	
	// instance variables
	private JButton loadButton, cancelButton, fileChooserButton;
	private JTextField selectFileName, selectGraphId, selectAc, selectDist, selectMinSeqSep, selectMaxSeqSep, selectDb;
	private JLabel labelAfterCc,labelAfterModel;
	private JCheckBox loadAllModelsCheckBox;
	private JTextArea seqArea;
	/** loads the model from the source */
	private LoadAction loadAction;
	/** implements the retrieval of all available chain identifiers for the given source */
	private Getter ccGetter;
	private Getter modelsGetter;
	private JFrame parentFrame;
	// combo boxes for the contact type, the chain code and the model
	private JComboBox comboCt, comboCc, comboModel;
	/** enabled if all present chain codes have been determined */
	private boolean determinedAllCc = false;
	private boolean determinedAllModels = false;
	private String prevPdbName;
	private Object[] prevComboCcItems;
	private Object[] prevComboModelItems;
	private String prevLabelAfterCc;
	private String prevLabelAfterModel;
	
	/*------------------------- static functions ----------------------------*/
	
	/**
	 * The values for some fields of the preceding load dialog are kept in 
	 * memory. If you do not want to use them you have to invoke this function 
	 * before constructing the next dialog to reset the values.
	 * @see #LoadDialog(JFrame, String, LoadAction, String, String, String, String, String, String, String, String, String)
	 */
	public void resetDefFieldValues() {
		field2defValue.clear();
	}

	// 
	// helper function for filling combo boxes
	private Object makeObj(final String item)  {
		return new Object() { public String toString() { return item; } };
	}

	/*----------------------------- constructors ----------------------------*/
	
	/**
	 * Constructs a new load dialog with the given text values for the input 
	 * fields. Some value might not be set properly as the values of the 
	 * preceding dialog are still kept in memory. Resetting these default 
	 * values by invoking function {@link #resetDefFieldValues()} yields the 
	 * expected behavior of the new dialog. The shape of the resulting load 
	 * dialog depends on the values being set. Whenever you initialise a value 
	 * to <code>null</code> the corresponding field in the load dialog is 
	 * missing! Please note that <code>showFileName</code> and 
	 * <code>showAc</code> are mutually exclusive!
	 * @param f  parent frame
	 * @param title  title of the load dialog
	 * @param a  action to perform when user clicks the OK button
	 * @param showFileName  filename, initialises text of the filename field
	 * @param showAc  pdb accession code, ... pdb code field
	 * @param showModel NMR model serial
	 *  Only allowed values: <code>""</code> or <code>null</code> 
	 * @param showLoadAllModels check box to load all models (weighted ensemble graph)
	 *  Only allowed values: <code>""</code> (on) or <code>null</code> (off)
	 * @param showCc  chain code 
	 *  Only allowed values: <code>""</code> or <code>null</code>  
	 * @param showCt  contact type, sets this contact type to the selected one 
	 *  (deprecated!)
	 * @param showDist  contact distance threshold, initializes text of the 
	 *  distance field 
	 * @param showMinSeqSep  minimal sequence separation, ... min. seq. sep. 
	 *  field
	 * @param showMaxSeqSep maximal sequence separation, ... max. seq. sep. 
	 *  field
	 * @param showDb  name of the database to be used, ... database field
	 * @param showGraphId  name of the graph to be used, ... graph id field
	 * @param showPasteSeq text area for pasting sequence
	 */
	public LoadDialog(JFrame f, String title, LoadAction a,
			String showFileName, String showAc, String showModel, String showLoadAllModels, String showCc,
			String showCt, String showDist, String showMinSeqSep, String showMaxSeqSep, String showDb, String showGraphId, String showPasteSeq) 
	throws LoadDialogConstructionError {
	
		super(f, title, true);
		
		// do some checkings
		if( showFileName != null && showAc != null ) {
			throw new LoadDialogConstructionError("The arguments showFileName and showAc are mutually exclusive!");
		}
		
		this.loadAction = a;
		this.parentFrame = f;

		this.setResizable(false);
		//setLocation(300,200); // apparently has no effect
		
		loadButton = new JButton("Ok");
		cancelButton = new JButton("Cancel");
		fileChooserButton = new JButton("Browse");
		loadButton.addActionListener(this);
		cancelButton.addActionListener(this);
		fileChooserButton.addActionListener(this);
		
		// sets the default button of this dialog. hence, whenever the 
		// enter-key is being pressed and released this button is invoked
		this.getRootPane().setDefaultButton(loadButton);
	
		// construct all the text fields and components
		selectFileName = new JTextField();
		selectGraphId = new JTextField();
		selectAc = new JTextField();
		selectDist = new JTextField();
		selectMinSeqSep = new JTextField();
		selectMaxSeqSep = new JTextField();		
		selectDb = new JTextField();

		loadAllModelsCheckBox = new JCheckBox("all models");
		loadAllModelsCheckBox.addItemListener(this);
				
		// register all insertion/deletion/update events from these fields.
		// this allows as to reset the comboCc (s.b.) whenever any changes were 
		// made in these field
		selectFileName.getDocument().addDocumentListener(this);
		selectAc.getDocument().addDocumentListener(this);
		
		// get present contact types from file and put their identifiers as 
		// items into the combo box
		Set<String> contactTypes = AAinfo.getAllContactTypes();
		comboCt = new JComboBox();
		Object o;
		for(String ct:contactTypes) {
			o = makeObj(ct);
			comboCt.addItem(o);
			if(ct.equals(showCt)) comboCt.setSelectedItem(o);
		}
		comboCt.setEditable(false);
		
		// construct combo box for the present chain codes. this load-dialog 
		// functions as popup-event handler of events fired by this combo box
		comboCc = new JComboBox();
		//comboModel.setMaximumRowCount(10);
		comboCc.addPopupMenuListener(this);
		
		comboModel = new JComboBox();
		//comboModel.setMaximumRowCount(10);
		comboModel.addPopupMenuListener(this);
		
		//JPanel labelPane = new JPanel();
		JPanel inputPane = new JPanel();
		JLabel labelFileName = new JLabel("Filename:");
		JLabel labelGraphId = new JLabel("Graph Id:");
		JLabel labelAc = new JLabel("PDB Code:");
		JLabel labelAfterAc = new JLabel("e.g. 7adh");
		JLabel labelCc = new JLabel("Chain code:");
		labelAfterCc = new JLabel(LABEL_AFTER_COMBO_BOX);
		JLabel labelCt = new JLabel("Contact type:");
		JLabel labelDist = new JLabel("Distance cutoff:");
		JLabel labelAfterDist = new JLabel("e.g. 8.0");
		JLabel labelMinSeqSep = new JLabel("Min. Seq. Sep.:");
		JLabel labelMaxSeqSep = new JLabel("Max. Seq. Sep.:");		
		JLabel labelAfterMinSeqSep = new JLabel("e.g. 1");
		JLabel labelAfterMaxSeqSep = new JLabel("e.g. 50");		
		JLabel labelDb = new JLabel("Database:");
		JLabel labelModel = new JLabel("Model:");
		labelAfterModel = new JLabel(LABEL_AFTER_COMBO_BOX);
		JLabel labelPasteSeq = new JLabel("Or paste sequence:");
		JLabel dummyLabel = new JLabel();
		JLabel dummyLabel2 = new JLabel();
		
		int fields = 0;
		if(showFileName != null) {
			inputPane.add(labelFileName);
			inputPane.add(selectFileName);
			inputPane.add(fileChooserButton);
			selectFileName.setText(showFileName);
			fields++;
		}
		if(showAc != null) {
			inputPane.add(labelAc);
			inputPane.add(selectAc);
			inputPane.add(labelAfterAc);
			//inputPane.add(Box.createHorizontalGlue());
			selectAc.setText(showAc);
			fields++;
		}
		if(showCc != null) {
			inputPane.add(labelCc);
			inputPane.add(comboCc);
			inputPane.add(labelAfterCc);
			// TODO the following is a cheap workaround for java bug 4743225 
			// (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4743225)
			// the bug is not solved but they give a number of more complicated workarounds there, 
			// this solution is much simpler and for our purposes looks good enough to me 
			for (int i=1;i<=NUMBER_COMBO_FILL_ITEMS;i++) comboCc.addItem(makeObj(COMBO_FILL_VALUE));
			fields++;
		}
		if(showModel != null) {
			inputPane.add(labelModel);
			inputPane.add(comboModel);
			if(Start.USE_EXPERIMENTAL_FEATURES && Start.SHOW_WEIGHTED_CONTACTS && showLoadAllModels != null) {
				inputPane.add(loadAllModelsCheckBox);
			} else {
				inputPane.add(labelAfterModel);
			}
			
			// set text-field to previous value if there is such thing in 
			// field2defValues.
			if( field2defValue.containsKey("model") ) {
				// NOTE: this assumes that field2defValue yields a valid 
				// model index
				comboModel.addItem(field2defValue.get("model"));				
			} else {
				comboModel.addItem(makeObj(showModel));
				// TODO the following is a cheap workaround for java bug 4743225 
				// (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4743225)
				// the bug is not solved but they give a number of more complicated workarounds there, 
				// this solution is much simpler and for our purposes looks good enough to me 
				for (int i=1;i<=NUMBER_COMBO_FILL_ITEMS-1;i++) comboModel.addItem(makeObj(COMBO_FILL_VALUE));
			}

			fields++;
		}
		if(showCt != null) {
			inputPane.add(labelCt);
			inputPane.add(comboCt);
			inputPane.add(Box.createHorizontalGlue());
			
			// set text-field to previous value if there is such thing in 
			// field2defValues.
			if( field2defValue.containsKey("ct") ) {
				// NOTE: this assumes that field2defValue yields a valid 
				// contact type
				comboCt.setSelectedItem(field2defValue.get("ct"));				
			}		

			fields++;
		}
		if(showDist != null) {
			inputPane.add(labelDist);
			inputPane.add(selectDist);
			inputPane.add(labelAfterDist);
			//inputPane.add(Box.createHorizontalGlue());
			
			// set text-field to previous value if there is such thing in 
			// field2defValues.
			if( field2defValue.containsKey("dist") ) {
				// NOTE: this assumes that field2defValue yields a valid 
				// contact type
				selectDist.setText(field2defValue.get("dist"));				
			} else {
				selectDist.setText(showDist);
			}
			
			fields++;
		}
		if(showMinSeqSep != null) {
			inputPane.add(labelMinSeqSep);
			inputPane.add(selectMinSeqSep);
			inputPane.add(labelAfterMinSeqSep);

			// set text-field to previous value if there is such thing in 
			// field2defValues.
			if( field2defValue.containsKey("minss") ) {
				// NOTE: this assumes that field2defValue yields a valid 
				// contact type
				selectMinSeqSep.setText(field2defValue.get("minss"));				
			} else {
				selectMinSeqSep.setText(showMinSeqSep);
			}

			fields++;
		}
		if(showMaxSeqSep != null) {
			inputPane.add(labelMaxSeqSep);
			inputPane.add(selectMaxSeqSep);
			inputPane.add(labelAfterMaxSeqSep);
			//inputPane.add(Box.createHorizontalGlue());

			// set text-field to previous value if there is such thing in 
			// field2defValues.
			if( field2defValue.containsKey("maxss") ) {
				// NOTE: this assumes that field2defValue yields a valid 
				// contact type
				selectMaxSeqSep.setText(field2defValue.get("maxss"));				
			} else {
				selectMaxSeqSep.setText(showMaxSeqSep);
			}
			
			fields++;
		}
		if(showDb != null) {
			inputPane.add(labelDb);
			inputPane.add(selectDb);
			//inputPane.add(labelAfterDb);
			inputPane.add(Box.createHorizontalGlue());
			selectDb.setText(showDb);
			fields++;
		}
		if(showGraphId != null) {
			inputPane.add(labelGraphId);
			inputPane.add(selectGraphId);
			inputPane.add(Box.createHorizontalGlue());
			//inputPane.add(labelAfterGraphId);
			selectGraphId.setText(showGraphId);
			fields++;
		}
		if(showPasteSeq != null) {
			inputPane.add(labelPasteSeq);
			inputPane.add(dummyLabel);
			inputPane.add(dummyLabel2);			
			fields++;
		}
		GridLayout layout = new GridLayout(fields,3);
		layout.setHgap(5);
		inputPane.setLayout(layout);
		
		// Setting up pane for pasting in a sequence (only shown if showPasteSeq != null)
		seqArea = new JTextArea(6, 42);
		seqArea.setFont(new Font(Font.DIALOG,Font.PLAIN,12));
		seqArea.setLineWrap(true);
		JScrollPane seqPane = new JScrollPane(seqArea);
		
		// Lay out the label and scroll pane from top to bottom.
		JPanel selectionPane = new JPanel();
		selectionPane.setLayout(new BoxLayout(selectionPane, BoxLayout.PAGE_AXIS));

		//selectionPane.add(labelPane);
		selectionPane.add(inputPane);
		if(showPasteSeq != null) selectionPane.add(seqPane);
		selectionPane.add(Box.createRigidArea(new Dimension(0,5)));
		selectionPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(loadButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		// Put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(selectionPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);	
	}
	
	/** main function to initialize and show GUI -> now moved to constructor */
	public void createGUI() {
		showIt();
	}
	
	public void showIt() {
		pack();
		setLocationRelativeTo(getParent());
		setVisible(true);		
	}

	/** return the currently selected accession code */
	public String getSelectedFileName() {
		String selectedFileName = selectFileName.getText();
		return selectedFileName;
	}	
	
	/** return the currently selected accession code */
	public String getSelectedAc() {
		String selectedAc = selectAc.getText();
		return selectedAc;
	}
	
	/**
	 * Gets the selected model serial.
	 * @return selected model serial, 1 if input box is empty or -1 if input box is not shown.
	 * @throws LoadDialogInputError if the input box contains a non-integer value.
	 */
	public int getSelectedModel() throws LoadDialogInputError {
		if (!comboModel.isShowing()) return -1; // this happens when loading from graph/caspRR file
		int selectedModelSerial = 1;
		String selectedText = comboModel.getSelectedItem().toString();
		if(selectedText.length() > 0) {
			try {
				selectedModelSerial = Integer.valueOf(selectedText);
			} catch(NumberFormatException e) {
				throw new LoadDialogInputError("Could not parse value for model serial.");
			}
		}
		return selectedModelSerial;
	}
	
	/**
	 * Returns the state of the 'load all models' checkbox.
	 * @return true if checkbox is selected, false otherwise
	 */
	public boolean isSelectedLoadAllModels() {
		return loadAllModelsCheckBox.isSelected();
	}

	/**
	 * Gets the currently selected chaincode
	 * @return selected chain code, null if inout box is not shown.
	 * @throws LoadDialogInputError if there's no item currently selected in input box  
	 */
	public String getSelectedCc() throws LoadDialogInputError {
		if (!comboCc.isShowing()) return null; // this happens when loading from graph/caspRR file
		Object item = comboCc.getSelectedItem();
		
		if(item == null || item.toString().length() == 0) {
			throw new LoadDialogInputError("<html>" +
					"Chain code is missing. Click on the drop-down menu to see valid chain codes. " +
					"</html>");
		}
		
		String selectedCc = item.toString();
		if(selectedCc.length() == 0) selectedCc = Pdb.NULL_CHAIN_CODE;
		return selectedCc;
	}

	/** 
	 * Returns the currently selected contact type 
	 * @throws LoadDialogInputError if the input box contains a non-valid contact type 
	 */
	public String getSelectedCt() throws LoadDialogInputError {
		String selectedCt = comboCt.getSelectedItem().toString();
		if(!AAinfo.isValidContactType(selectedCt)) {
			//System.err.println("The contact type " + selectedCt + " is invalid.");
			throw new LoadDialogInputError("The contact type " + selectedCt + " is invalid.");
		}
		return selectedCt;
	}
	
	/** 
	 * Returns the currently selected distance cutoff 
	 * @throws LoadDialogInputError if the input box doesn't contain a numeric value.
	 */
	public double getSelectedDist() throws LoadDialogInputError {
		double selectedDist = 0.0;
		String selectedText = selectDist.getText();
		if(selectedText.length() > 0) {
			try {
				selectedDist = Double.valueOf(selectDist.getText());
			} catch(NumberFormatException e) {
				//System.err.println("Could not parse value for distance cutoff.");
				throw new LoadDialogInputError("Could not parse value for distance cutoff.");
			}
		}
		return selectedDist;
	}
	
	/** 
	 * Returns the currently selected min seq sep 
	 * @throws LoadDialogInputError if the input box contains a non-integer value.
	 */
	public int getSelectedMinSeqSep() throws LoadDialogInputError {
		int selectedMinSeqSep = -1;
		String selectedText = selectMinSeqSep.getText();
		if(selectedText.length() > 0) {
			try {
				selectedMinSeqSep = Integer.valueOf(selectMinSeqSep.getText());
			} catch(NumberFormatException e) {
				//System.err.println("Could not parse value for min sequence separation.");
				throw new LoadDialogInputError("Could not parse value for min sequence separation.");
			}
		}
		return selectedMinSeqSep;
	}
	
	/** 
	 * Returns the currently selected max seq sep 
	 * @throws LoadDialogInputError if the input box contains a non-integer value.
	 */
	public int getSelectedMaxSeqSep() throws LoadDialogInputError {
		int selectedMaxSeqSep = -1;
		String selectedText = selectMaxSeqSep.getText();
		if(selectedText.length() > 0) {
			try {
				selectedMaxSeqSep = Integer.valueOf(selectMaxSeqSep.getText());
			} catch(NumberFormatException e) {
				//System.err.println("Could not parse value for max sequence separation.");
				throw new LoadDialogInputError("Could not parse value for max sequence separation.");
			}
		}
		return selectedMaxSeqSep;
	}

	/** 
	 * Returns the currently selected database name 
	 */
	public String getSelectedDb() {
		String selectedDb = selectDb.getText();
		return selectedDb;
	}
	
	/** 
	 * Returns the currently selected graph id 
	 * @throws LoadDialogInputError if the input box contains a non-integer value.
	 */
	public int getSelectedGraphId() throws LoadDialogInputError {
		int selectedGraphId = -1;
		String selectedText = selectGraphId.getText();
		if(selectedText.length() > 0) {
			try {
				selectedGraphId = Integer.valueOf(selectGraphId.getText());
			} catch(NumberFormatException e) {
				//System.err.println("Could not parse value for graph id.");
				throw new LoadDialogInputError("Could not parse value for graph id.");
			}
		}
		return selectedGraphId;
	}
	
	/**
	 * Returns the contents of the sequence text area
	 * @return
	 */
	public String getPastedSequence() {
		return seqArea.getText();
	}
	
	/** action listener method for buttons */
	public void actionPerformed (ActionEvent e) {
		
		/* load button */
		if (e.getSource()== loadButton ){
			this.go();	
		}

		
		if (e.getSource() == cancelButton) {
			this.setVisible(false);
			dispose();
		}
		
		if (e.getSource() == fileChooserButton) {
			JFileChooser fileChooser = Start.getFileChooser(); // use global file chooser to remember previous path
			int ret = fileChooser.showOpenDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = fileChooser.getSelectedFile();
				String path = chosenFile.getPath();
				this.selectFileName.setText(path);
			}
		}
	}
	
	/**
	 * 	listener method for check box
	 */
	public void itemStateChanged(ItemEvent e) {
		if(e.getItemSelectable() == loadAllModelsCheckBox) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				comboModel.setEnabled(false);
			} else {
				comboModel.setEnabled(true);
			}
		}
	}
	
	public Getter getChainCodesGetter() {
		return ccGetter;
	}
	
	public void setChainCodeGetter(Getter getter) {
		this.ccGetter = getter;
	}
	
	public Getter getModelsGetter() {
		return modelsGetter;
	}
	
	public void setModelsGetter(Getter getter) {
		this.modelsGetter = getter;
	}
	
	
	public void popupMenuCanceled(PopupMenuEvent e) {
	}

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		if( e.getSource() == comboCc ) {
			handleComboCcEvent("visible");
		} else if( e.getSource() == comboModel ) {
			handleComboModelEvents("visible");
		} 
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	}
	
	/**
	 * Handles update event for some documents (the actual text-area of a
	 * text fields).
	 *
	 * @param e  a document event
	 */
	public void changedUpdate(DocumentEvent e) {
		 // TODO: At what situation is an update-event being fired?
		// nothing to do so far!
	}

	/**
	 * Handles insertion event for some documents (the actual text-area of a
	 * text fields).
	 * @param e  a document event
	 */
	public void insertUpdate(DocumentEvent e) {
		Document d = e.getDocument();
		if( d == selectFileName.getDocument() ) {
			handleSelectFileNameEvents(e);
		} else if ( d == selectAc.getDocument() ){
			handleSelectAcEvents(e);
		} 
	}

	/**
	 * Handles removal event for some documents (the actual text-area of a 
	 * text field).
	 * @param e  a document event
	 */
	public void removeUpdate(DocumentEvent e) {
		Document d = e.getDocument();
		if( d == selectFileName.getDocument() ) {
			handleSelectFileNameEvents(e);
		} else if ( d == selectAc.getDocument() ){
			handleSelectAcEvents(e);
		} 
	}
	
	/**
	 * Deletes the chain codes of the chain code combo box and backs them up in 
	 * {@link #prevComboCcItems}.
	 */
	private void clearComboCc() {
		// disables the flag to indicate that the set of all present chain 
		// codes for the next pdb code/filename have to be taken  
		// from the source when the user presses the arrow of comboCc next time
		determinedAllCc = false;
		
		// delete all items of the combo box for the chain codes (but keep them 
		// in mind!)
		prevComboCcItems = new Object[comboCc.getItemCount()];
		for( int i = 0; i < prevComboCcItems.length; ++i ) {
			prevComboCcItems[i] = comboCc.getItemAt(i);
		}
		comboCc.removeAllItems();
		for (int i=1;i<=NUMBER_COMBO_FILL_ITEMS;i++) comboCc.addItem(makeObj(COMBO_FILL_VALUE));
		comboCc.setEditable(false);
	}
	
	/**
	 * Resets the chain codes of the chain code combo box with the backed-up 
	 * values.
	 */
	private void resetComboCc() {
		if( prevComboCcItems != null ) {
			// enables the flag to indicate that one has not to retrieve the chain 
			// codes again as the previous pdb code/ filename has been reset
			determinedAllCc = true;

			// reset the item of the combo box for the chain codes
			comboCc.removeAllItems();
			for( int i = 0; i < prevComboCcItems.length; ++i ) {
				comboCc.addItem(prevComboCcItems[i]);
			}
			labelAfterCc.setText(prevLabelAfterCc);
		}
	}
	
	private void clearComboModel() {
		// disables the flag to indicate that the set of all model indices 
		// for the next pdb code/filename have to be taken from the source 
		// when the user presses the arrow of comboModel next time
		determinedAllModels = false;
		
		// delete all items of the combo box for the chain codes (but keep them 
		// in mind!)
		prevComboModelItems = new Object[comboModel.getItemCount()];
		for( int i = 0; i < prevComboModelItems.length; ++i ) {
			prevComboModelItems[i] = comboModel.getItemAt(i);
		}
		comboModel.removeAllItems();
		comboModel.addItem(makeObj("1"));
		for (int i=1;i<=NUMBER_COMBO_FILL_ITEMS-1;i++) comboModel.addItem(makeObj(COMBO_FILL_VALUE));
		comboModel.setEditable(false);
		prevLabelAfterModel = labelAfterModel.getText();
		labelAfterModel.setText(LABEL_AFTER_COMBO_BOX);
	}

	private void resetComboModel() {
		if( prevComboModelItems != null ) {
			// enables the flag to indicate that one has not to retrieve the model
			// identifiers again as the previous pdb code/ filename has been reset
			determinedAllModels = true;

			// reset the item of the combo box for the chain codes
			comboModel.removeAllItems();
			for( int i = 0; i < prevComboModelItems.length; ++i ) {
				comboModel.addItem(prevComboModelItems[i]);
			}
			labelAfterModel.setText(prevLabelAfterModel);
		}
	}
	
	private void handleSourceNameChanges(DocumentEvent e) {
		if ( e.getType() == DocumentEvent.EventType.INSERT || e.getType() ==  DocumentEvent.EventType.REMOVE ) {
			// NOTE: make sure that you apply all changes for this event also to function 
			//       removeUpdate(DocumentEvent) !!!
			if (prevPdbName != null	&& (prevPdbName.equals(getSelectedFileName()) || prevPdbName.equals(getSelectedAc()))) {
				resetComboCc();
				resetComboModel();
			} else {
				if (determinedAllCc == true) {
					clearComboCc();
				}
				if (determinedAllModels == true) {
					clearComboModel();
				}
			
				labelAfterCc.setText(LABEL_AFTER_COMBO_BOX);
				labelAfterModel.setText(LABEL_AFTER_COMBO_BOX);
			}
		} // else: nothing else to do so far!
	}
	
	private void handleSelectAcEvents(DocumentEvent e) {
		handleSourceNameChanges(e);
	}
	
	private void handleSelectFileNameEvents(DocumentEvent e) {
		handleSourceNameChanges(e);
	}

	private void handleComboCcEvent(String state) {
		if(state.equals("visible")) {
			if( ccGetter != null && determinedAllCc == false) {
				String[] allCc = null;
				try {
					setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					allCc = (String[]) ccGetter.get();

					if( allCc == null ) {
						if( !"".equals(getSelectedAc()) || !"".equals(getSelectedFileName()) ) {
							// source either contains no chain or whatever. we are 
							// catching that fact by putting a message in the chain 
							// combo box field
							comboCc.setEditable(true);
							comboCc.addItem(makeObj("ERROR"));

							labelAfterCc.setText("check pdb source!");
							determinedAllCc = true;
							backupPdbName();
						}
					} else {
						// copy everything from the array to the combo-box
						comboCc.removeAllItems();
						for( int i = 0; i < allCc.length; ++i ) {
							comboCc.addItem(makeObj(allCc[i]));						
						}
						determinedAllCc = true;
						backupPdbName();
					}
				} catch (GetterError e) {
					System.err.println(e.getMessage());
					labelAfterCc.setText("check pdb source!");
				}
				setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				
			} else if( comboCc.getItemCount() == 0 ) {
				comboCc.setEditable(true);
				comboCc.addItem(makeObj("ERROR"));
				determinedAllCc = true;
				backupPdbName();
			}
		} // else: nothing to do so far!
	}
	
	private void handleComboModelEvents(String state) {
		if( state.equals("visible") ) {
			if (modelsGetter != null && determinedAllModels == false ) {
				try {
					setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					Integer[] allModels = (Integer[]) modelsGetter.get();
					if( allModels == null ) {
						comboModel.removeAllItems();
						comboModel.addItem(makeObj("1"));
					} else {
						comboModel.removeAllItems();
						for (int i = 0; i < allModels.length; ++i) {
							comboModel.addItem(makeObj(allModels[i].toString()));
						}
						//comboModel.setMaximumRowCount(comboModel.getItemCount());	// doesn't seem to work
						determinedAllModels = true;
						backupPdbName();
					}
				} catch (GetterError e) {
					System.err.println(e.getMessage());
					labelAfterModel.setText("check pdb source!");
				}
				setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				
			} else if ( determinedAllModels == false ){
				// set 1 as default model identifier and make the combo box 
				// editable
				comboModel.removeAllItems();
				comboModel.addItem(makeObj("1"));
				comboModel.setEditable(true);
				
				determinedAllModels = true;
				backupPdbName();
			}
			
		} // else: nothing to do so far!
	}

	private void backupPdbName() {
		String emptyString = "";
		if (!emptyString.equals(getSelectedFileName()) && emptyString.equals(getSelectedAc())) {
			// backup filename
			prevPdbName = getSelectedFileName();
		} else if (emptyString.equals(getSelectedFileName()) && !emptyString.equals(getSelectedAc())) {
			// backup pdb accession code
			prevPdbName = getSelectedAc();
		} else {
			System.err.println("Error: asking for filename and pdb accession code but neither is present!");
		}
	}

	/** test user input and if it looks fine, perform load and dispose the dialog */
	private void go() {
		try {
			String f = getSelectedFileName();
			String ac = getSelectedAc();
			int modelSerial = getSelectedModel();
			boolean loadAllModels = isSelectedLoadAllModels();
			String cc = getSelectedCc();
			String ct = getSelectedCt();
			double dist = getSelectedDist();
			int minss = getSelectedMinSeqSep();
			int maxss = getSelectedMaxSeqSep();
			String db = getSelectedDb();
			int gid = getSelectedGraphId();
			String seq = getPastedSequence();
			
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

			this.loadAction.doit((Object) parentFrame, f, ac, modelSerial, loadAllModels, cc, ct, dist, minss, maxss, db, gid, seq);
			
			// write default values to remember them for the next load-dialog 
			// (e.g. for loading the second structure)
			field2defValue.put("ct", ct);
			if(dist  >  0.0) field2defValue.put("dist", new Double(dist).toString());
			if(minss >= 0)   field2defValue.put("minss", new Integer(minss).toString());
			if(maxss >= 0)   field2defValue.put("maxss", new Integer(maxss).toString());
			
			this.setVisible(false);
			dispose();
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
		} catch (LoadDialogInputError e) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			JOptionPane.showMessageDialog(this, e.getMessage(), "Input error", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Debugging frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(false);

		try {
			LoadDialog dialog = new LoadDialog(frame, "Test dialog", new LoadAction(false) {
				public void doit (Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {					
					System.out.println("Filename:\t" + f);
					System.out.println("PDB code:\t" + ac);
					System.out.println("Model serial:\t" + modelSerial);
					System.out.println("All models:\t" + loadAllModels);
					System.out.println("Chain code:\t" + cc);
					System.out.println("Contact type:\t" + ct);
					System.out.println("Dist. cutoff:\t" + dist);
					System.out.println("Min. Seq. Sep.:\t" + minss);
					System.out.println("Max. Seq. Sep.:\t" + maxss);	        		
					System.out.println("Database:\t" + db);
					System.out.println("Graph Id:\t" + gid);
					System.out.println("Sequence:\t" + seq);
				};
			}, "filename", null, "2", "", "B", "Ca", "8.0", "0", "20", "pdbase", "1", "");
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					System.exit(0);
				}
				public void windowClosed(WindowEvent event) {
					System.exit(0);
				}
			});
			dialog.createGUI();
		} catch (LoadDialogConstructionError e) {
			System.err.println(e.getMessage());
		}
	}
}
