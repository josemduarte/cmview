package cmview.sadpAdapter;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cmview.toolUtils.PreferencesDialog;

public class SADPPreferencesDialog extends PreferencesDialog implements DocumentListener,ItemListener {

	static final long serialVersionUID = 1l;

	static final String WINDOW_TITLE = "SADP Preferences";

	// stuff connected to the preferences taken as an input (default pref.) and those to be output (custom)
	private Properties                defaultProps;
	private Properties                customProps;
	private TreeMap<String, String>   comments;
	private TreeMap<String, Object>   types;
	private TreeMap<String, Object[]> domains;

	// the fields for the tabs
	private TreeMap<String, JTextField> textFields;
	private TreeMap<String, JLabel>     domainLabels;
	private TreeMap<String, Boolean>    textFieldChanged;
	
	// slider for basic preferences
	private JSlider basicPrefSlider;

	// the panes for the tabs
	private JPanel basicPane;
	private JPanel advancedPane;

	// maps from decision name to whether the decision has been made
	// if a decision depends on the state of a component it is recommended to 
	// take the variable name of the respective component as key to the 
	// decision
	private TreeMap<String, Boolean> decisions;

	private JCheckBox advCBinputErrorMessage;

	/**
	 * Constructs preference dialog for the SADP alignment algorithm.
	 * @param f             the parent frame
	 * @param defaultProps  holds the default properties
	 * @param comments      comments for the parameters to be shown as tool tips
	 * @param types         holds the proper type for each parameter. the actual type is determined via the <tt> getClass().cast() </tt> functionality of class Object. Types must not be arrays.
	 * @param domains       holds the valid domains for each parameter. the parameter type is again determined via the passed tree-map types.
	 * */
	public SADPPreferencesDialog( Frame f, Properties defaultProps, TreeMap<String, String> comments, TreeMap<String, Object> types, TreeMap<String,Object[]> domains ) {
		super(f,WINDOW_TITLE);
		this.defaultProps = defaultProps;
		this.comments     = comments;
		this.types        = types;
		this.domains      = domains;
		customProps       = new Properties(defaultProps); // this one is to be returned
		textFields        = new TreeMap<String, JTextField>();
		domainLabels      = new TreeMap<String, JLabel>();
		textFieldChanged  = new TreeMap<String, Boolean>();
		decisions         = new TreeMap<String, Boolean>();
		advCBinputErrorMessage = new JCheckBox("Do not show this message again!");
		advCBinputErrorMessage.setName("advCBinputErrorMessage");
		advCBinputErrorMessage.addItemListener(this);
		addPreferences();
	}

	/**
	 * Creates and adds tabbed-panes for the BASIC and the ADVANCED preferences
	 * to the preference dialog. 
	 */
	protected void addPreferences() {
		makeBasicTab();
		makeAdvancedTab();
	}

	/**
	 *  Adds the tabbed-pane for the BASIC preferences to the dialog.
	 */
	protected void makeBasicTab() {
		// TODO: implement some intelligent slider which somehow tries to adapt the settings with respect to the recent slider position
		basicPane        = new JPanel();
		basicPrefSlider  = new JSlider();
		JLabel  infoText = new JLabel("Move the slider to affect the running time and alignment quality.");
		infoText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// make the slider's labels
		Hashtable<Integer,JLabel> sliderLabels = new Hashtable<Integer, JLabel>();
		sliderLabels.put(0, new JLabel("fast & sloppy"));
		sliderLabels.put(2, new JLabel("DEFAULT"));
		sliderLabels.put(4, new JLabel("slow & thorough"));

		// put labels onto the slider an adjust slider dimension (min/max) according to the number of labels
		basicPrefSlider.setMinimum(0);
		basicPrefSlider.setMaximum(4);
		basicPrefSlider.setValue(2);
		basicPrefSlider.setMinorTickSpacing(1);
		basicPrefSlider.setMajorTickSpacing(1); // enable tick
		basicPrefSlider.setSnapToTicks(true);
		basicPrefSlider.setPaintTicks(true);
		basicPrefSlider.setLabelTable(sliderLabels); // ... and set labels
		basicPrefSlider.setPaintLabels(true);
		basicPrefSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		basicPrefSlider.addChangeListener(this);

		// putting everything together
		basicPane.setLayout(new GridLayout(2,1));
		basicPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		basicPane.add(infoText);
		basicPane.add(basicPrefSlider);

		addTab("Basic", basicPane);
	}

	/**
	 *  Adds the tabbed-pane for the ADVANCED preferences to the dialog.
	 */
	protected void makeAdvancedTab() {
		advancedPane = new JPanel();
		TreeMap<String,JLabel> paramLabels  = new TreeMap<String, JLabel>();
		int lines = 1; // initialize by two to consider: header line

		// add header line
		advancedPane.add(new JLabel("Parameters"));
		advancedPane.add(new JLabel("Values"));
		advancedPane.add(new JLabel("Restricted Domains"));	

		// create text fields along with their respective labels
		for( Enumeration<?> opts = defaultProps.propertyNames(); opts.hasMoreElements(); ) {

			String     opt    = (String) opts.nextElement();
			JLabel     label  = new JLabel(opt+":");
			JTextField field  = new JTextField(customProps.getProperty(opt));
			JLabel     domain = new JLabel(makeDomainString(opt));

			field.getDocument().addDocumentListener(this);

			if( comments.containsKey(opt) ) {
				// adds optional comment as a tool tip to the current parameter
				label.setToolTipText(comments.get(opt));
			}

			// add field and label to the respective maps
			textFields.put(opt,field);
			textFieldChanged.put(opt, false);
			paramLabels.put(opt,label);
			domainLabels.put(opt,domain);
			++lines;
		}

		// place labels and field on advancedPane
		for( Iterator<String> it = textFields.keySet().iterator(); it.hasNext(); ) {
			String opt = it.next();
			advancedPane.add(paramLabels.get(opt));
			advancedPane.add(textFields.get(opt));
			advancedPane.add(domainLabels.get(opt));
		}

		// add fields and labels to advancedPane in a nice layout
		GridLayout dataGrid = new GridLayout(lines,3);
		dataGrid.setHgap(5);
		advancedPane.setLayout(dataGrid);	

		// add advancedPane as a new tabbed-pane to the dialog
		addTab("Advanced",advancedPane);

		decisions.put(advCBinputErrorMessage.getName(), false);
	}

	protected String makeDomainString( String opt ) {

		char open,close;

		if( isNumber(types.get(opt)) ) {
			if( domains.get(opt).length == 2 ) {
				open  = '[';
				close = ']';
			} else {
				open  = '{';
				close = '}';
			}
		} else {
			// so we are not dealing with numbers but with characters, which used to be enclosed in curly braces
			open  = '{';
			close = '}';
		}

		// make string
		StringBuffer sb = new StringBuffer();
		sb.append(open);
		for( int i=0; i<domains.get(opt).length; ++i ) {
			sb.append(domains.get(opt)[i]);
			if( i<domains.get(opt).length-1) {
				sb.append(",");
			}
		}
		sb.append(close);

		return sb.toString();
	}

	public void insertUpdate(DocumentEvent d) {
		checkTextFieldChanged(d);
	}

	public void removeUpdate(DocumentEvent d) {
		checkTextFieldChanged(d);
	}

	public void changedUpdate(DocumentEvent d) {
		checkTextFieldChanged(d);
	}

	protected void checkTextFieldChanged(DocumentEvent d) {
		for( String f:textFields.keySet() ) {
			if( d.getDocument() == textFields.get(f).getDocument() ) {
				if( !getApplyButton().isEnabled() ) {
					getApplyButton().setEnabled(true);
				}
				if( textFieldChanged.get(f) == false ) {
					textFieldChanged.put(f, true);
				}
				return;
			}
		}
	}
	
	public void stateChanged(ChangeEvent c) {
		if( c.getSource() == basicPrefSlider && !getApplyButton().isVisible() ) {
			getApplyButton().setVisible(true);
		} else {
			super.stateChanged(c);
		}
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		String decisionKey = null;

		if( source == advCBinputErrorMessage ) {
			decisionKey = advCBinputErrorMessage.getName();
		}

		if( decisionKey != null ) {
			decisions.put(decisionKey,e.getStateChange()==ItemEvent.SELECTED);
		}
	}

	protected boolean hasAnythingChanged() {
		for( boolean b : textFieldChanged.values() ) {
			if( b == true ) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Updates customized parameters and invokes pop-up error messages if any 
	 * domain constraint has been violated.
	 * @return true if the updating procedure passed without any detected input
	 *         error, false if an error has been detected or if nothing has 
	 *         actually changed. 
	 */
	protected boolean updateCustomParamsFromAdvanced() {

		// return if there is no modified text field
		if( !hasAnythingChanged() ) {
			return false;
		}

		boolean valid = false;
		boolean anythingInvalid = false;

		for( String opt : textFields.keySet() ) {

			if( textFieldChanged.get(opt) == false ) {
				continue;
			}

			String val = textFields.get(opt).getText();

			//System.out.println(opt+":"+val+" (default:"+defaultProps.getProperty(opt)+")");

			valid = false;

			try {

				if( domains.get(opt).length != 2 || !isNumber(types.get(opt)) ) {
					// domains != 2 denote non interval domains, we therefore have
					// to check equality for each single element in the domain
					if( isDouble(types.get(opt)) ) {
						// as double values may be present in different kind of 
						// formattings we have to compare the double values instead
						// of the text-field's string (which could possibly be the
						// cheaper comparison)
						for( Object elem:domains.get(opt) ) {
							if( (Double) elem == new Double(val) ) {
								valid = true;
								break;
							}			
						}
					} else {
						for( Object elem:domains.get(opt) ) {
							if( elem == val ) {
								valid = true;
								break;
							}
						}
					}
				} else {
					// domains == 2 denote interval-like domains, we therefore have
					// to check whether the set value is in the valid value range
					if( isInteger(types.get(opt)) ) {
						Integer i = new Integer(val);
						if( (Integer) domains.get(opt)[0] <= i && (Integer) domains.get(opt)[1] >= i ) {
							valid = true;
						}
					} else if( isDouble(types.get(opt) ) ) {
						Double i = new Double(val);
						if( (Double) domains.get(opt)[0] <= i && (Double) domains.get(opt)[1] >= i ) {
							valid = true;
						}		    
					} else {
						// TODO: throw any kind of type not found exception or whatever!
					}
				}
			} catch (NumberFormatException e) {
				// val seems to be invalid
			}

			if( !valid ) {
//				// print error message
//				String errorMessage = "Customized value of parameter "+opt+" does not fit its values domain: '"+val+"' <=>"+domainLabels.get(opt).getText()+"!";
//				JOptionPane.showMessageDialog(this, errorMessage, "Input error", JOptionPane.INFORMATION_MESSAGE);
//				// reset malformed text-field
//				textFields.get(opt).setText(defaultProps.getProperty(opt));
//				// HINT: we stop here and return further below as the setText 
//				// command triggers a resetting of textFieldChanged for the 
//				// current 'opt' to true which we do not want
				textFields.get(opt).setBackground(Color.red);
				anythingInvalid = true;
			} else {
				if( textFields.get(opt).getBackground() == Color.red ) {
					textFields.get(opt).setBackground(Color.white);
				}
				customProps.setProperty(opt, val);
				// set text-field-changed blinker to false as we have either 
				// validated the new and/or old value as valid or invalid in which 
				// case we have reset it to the default value which is supposed to 
				// be valid and hence turn the changed state of field 'opt' to 
				// unchanged
				textFieldChanged.put(opt, false);
			}



//			if( !valid) {
//			// return with false indicating that something went wrong
////			return false;
//			}
		}

		if( anythingInvalid ) {
			if( decisions.get(advCBinputErrorMessage.getName()) == false ) { 
				Object[] textElements = new Object[2];
				textElements[0] = "Violated domains have been detected indicated by red shaded textfields!";
				textElements[1] = advCBinputErrorMessage; 
				JOptionPane.showMessageDialog(this, textElements, "Input error", JOptionPane.INFORMATION_MESSAGE);
			}
			return false;
		} else {		
			// update process completed without any error
			return true;
		}
	}

	protected boolean updateCustomParamsFromBasic() {
		
		@SuppressWarnings("unused")
		int state = basicPrefSlider.getValue();
		
		return true;
	}

	/**
	 * Function is called when the OK button has been pressed.
	 * Triggers saving of recently made changes and disposes the preferences 
	 * dialog.
	 */
	@Override
	public void ok() {
		System.out.println("ACTION: pressed OK-button");
		if( apply(getTabbedPane().getSelectedIndex()) ) {
			dispose();			
		}
	}

	/**
	 * Function is called when the Cancel button has been pressed.
	 * The preferences dialog will be disposed.
	 * Note that function {@link #getCustomizedParams()} will return the 
	 * default settings.
	 */
	@Override
	public void cancel() {
		System.out.println("ACTION: pressed Cancel-button");
		// parameters to be output are supposed to be the same as the default parameters
		customProps = defaultProps;
		dispose();
	}

	/**
	 * Function is called when the Apply button has been pressed.
	 * The internal representation of the user defined settings will be 
	 * updated.
	 * @param tabIdx  apply modification made in the tabbed-pane having this 
	 *                 index
	 */
	public boolean apply(int tabIdx) {
		System.out.println("ACTION: pressed Apply-button");
		if( getTabbedPane().getComponentAt(tabIdx) == basicPane ) {
			if( updateCustomParamsFromBasic() ) {
				getApplyButton().setEnabled(false);
			} else {
				return false;
			}
		} else if ( getTabbedPane().getComponentAt(tabIdx) == advancedPane ) {
			if( updateCustomParamsFromAdvanced() ) {
				getApplyButton().setEnabled(false);
			} else {
				return false;
			}
		} else {
			System.err.println("Error: Unrecognized component index "+tabIdx+"!");
			return false;
		}
		return true;
	}

	public void reset(int i) {
		if( i == 0 ) {
			resetBasic();
		} else if( i == 1 ) {
			resetAdvanced();
		} else {
			System.err.println("Error: Cannot reset tab "+i+" as this index exceeds the maximal tab-index!");
		}
	}

	/**
	 * Resets setting made in the tabbed pane for the basic preferences.
	 */
	protected void resetBasic() {
		// TODO: to be implemented ... tja???
	}

	/**
	 * Resets all text-fields with the values in the default properties.
	 */
	protected void resetAdvanced() {
		JTextField tmp;
		for( String opt : textFields.keySet() ) {
			tmp = textFields.get(opt);
			tmp.setText(defaultProps.getProperty(opt));
			tmp.setBackground(Color.white);
			textFieldChanged.put(opt,false);

		}		
	}

	/**
	 * Gets customized parameter settings
	 * @return the customized settings
	 */
	public Properties getCustomizedParams() {
		return customProps;
	}

	/**
	 * Checks if the given object can be casted to an instance of type <code>Number</code>.
	 * @param obj
	 * @return false if casting fails, else true
	 */
	private boolean isNumber(Object obj) {
		try {
		    	@SuppressWarnings("unused")
			Number n = (Number) obj;
			return true;
		} catch ( ClassCastException e ) {
			return false;
		}
	}

	private boolean isInteger( Object obj ) {
		try {
			@SuppressWarnings("unused")
			Number n = (Integer) obj;
			return true;
		} catch( ClassCastException e ) {
			return false;
		}
	}

	private boolean isDouble( Object obj ) {
		try {
			@SuppressWarnings("unused")
			Number n = (Double) obj;
			return true;
		} catch( ClassCastException e ) {
			return false;
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Properties                defaultProps = SADPPreferencesRetriever.getDefaultParams();
		TreeMap<String, String>   comments     = SADPPreferencesRetriever.getComments();
		TreeMap<String, Object>   types        = SADPPreferencesRetriever.getTypes();	
		TreeMap<String, Object[]> domains      = SADPPreferencesRetriever.getDomains();

		JFrame frame = new JFrame();
		SADPPreferencesDialog diag = new SADPPreferencesDialog(frame,defaultProps,comments,types,domains);
		diag.createGUI();
		Properties customProps = diag.getCustomizedParams();
		System.out.println("BLA");
		customProps.list(System.out);
	}

}
