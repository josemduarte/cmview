package cmview;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import proteinstructure.AA;

/**
 * A dialog to load a contact map. This dialog is used by several load commands
 * and displays different input fields depending on the given parameters.
 * The action to be performed when ok is pressed can be passed as a LoadAction instance.
 */
public class LoadDialog extends JDialog implements ActionListener {

	static final long serialVersionUID = 1l;
	
	private JButton loadButton, cancelButton, fileChooserButton;
	private JTextField selectFileName, selectGraphId, selectAc, selectCc, selectDist, selectMinSeqSep, selectMaxSeqSep, selectDb;
	private LoadAction loadAction;
	private JFrame parentFrame;
	private JComboBox comboCt;
	
	// helper function for filling combo boxes
	private Object makeObj(final String item)  {
		return new Object() { public String toString() { return item; } };
	}

	/** construct a new start object with the given title */
	public LoadDialog(JFrame f, String title, LoadAction a,
			String showFileName, String showAc, String showCc, String showCt,
			String showDist, String showMinSeqSep, String showMaxSeqSep, String showDb, String showGraphId) {
		super(f, title, true);
		this.loadAction = a;
		this.parentFrame = f;

		this.setResizable(false);
		setLocation(300,200);
		
		loadButton = new JButton("Ok");
		cancelButton = new JButton("Cancel");
		fileChooserButton = new JButton("Browse");
		loadButton.addActionListener(this);
		cancelButton.addActionListener(this);
		fileChooserButton.addActionListener(this);
		
		selectFileName = new JTextField();
		selectGraphId = new JTextField();
		selectAc = new JTextField();
		selectCc = new JTextField();
		//selectCt = new JTextField();
		selectDist = new JTextField();
		selectMinSeqSep = new JTextField();
		selectMaxSeqSep = new JTextField();		
		selectDb = new JTextField();
		
		String[] contactTypes = AA.contactTypes();
		comboCt = new JComboBox();
		Object o;
		for(String ct:contactTypes) {
			o = makeObj(ct);
			comboCt.addItem(o);
			if(ct.equals(showCt)) comboCt.setSelectedItem(o);
		}
		comboCt.setEditable(true);

		//JPanel labelPane = new JPanel();
		JPanel inputPane = new JPanel();
		JLabel labelFileName = new JLabel("Filename:");
		JLabel labelGraphId = new JLabel("Graph Id:");
		JLabel labelAc = new JLabel("PDB Code:");
		JLabel labelAfterAc = new JLabel("e.g. 7adh");
		JLabel labelCc = new JLabel("Chain code:");
		JLabel labelAfterCc = new JLabel("e.g. A");
		JLabel labelCt = new JLabel("Contact type:");
		JLabel labelDist = new JLabel("Distance cutoff:");
		JLabel labelAfterDist = new JLabel("e.g. 8.0");
		JLabel labelMinSeqSep = new JLabel("Min. Seq. Sep.:");
		JLabel labelMaxSeqSep = new JLabel("Max. Seq. Sep.:");		
		JLabel labelAfterMinSeqSep = new JLabel("e.g. 0");
		JLabel labelAfterMaxSeqSep = new JLabel("e.g. 50");		
		JLabel labelDb = new JLabel("Database:");
		
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
			inputPane.add(selectCc);
			inputPane.add(labelAfterCc);
			//inputPane.add(Box.createHorizontalGlue());
			selectCc.setText(showCc);
			fields++;
		}
		if(showCt != null) {
			inputPane.add(labelCt);
			//inputPane.add(selectCt);
			inputPane.add(comboCt);
			//inputPane.add(labelAfterCt);
			inputPane.add(Box.createHorizontalGlue());
			//selectCt.setText(showCt);
			fields++;
		}
		if(showDist != null) {
			inputPane.add(labelDist);
			inputPane.add(selectDist);
			inputPane.add(labelAfterDist);
			//inputPane.add(Box.createHorizontalGlue());
			selectDist.setText(showDist);
			fields++;
		}
		if(showMinSeqSep != null) {
			inputPane.add(labelMinSeqSep);
			inputPane.add(selectMinSeqSep);
			inputPane.add(labelAfterMinSeqSep);
			//inputPane.add(Box.createHorizontalGlue());
			selectMinSeqSep.setText(showMinSeqSep);
			fields++;
		}
		if(showMaxSeqSep != null) {
			inputPane.add(labelMaxSeqSep);
			inputPane.add(selectMaxSeqSep);
			inputPane.add(labelAfterMaxSeqSep);
			//inputPane.add(Box.createHorizontalGlue());
			selectMaxSeqSep.setText(showMaxSeqSep);
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
		GridLayout layout = new GridLayout(fields,3);
		layout.setHgap(5);
		inputPane.setLayout(layout);
		
		// Lay out the label and scroll pane from top to bottom.
		JPanel selectionPane = new JPanel();
		selectionPane.setLayout(new BoxLayout(selectionPane, BoxLayout.LINE_AXIS));

		//selectionPane.add(labelPane);
		selectionPane.add(inputPane);
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

	/** return the currently selected chain code */
	public String getSelectedCc() {
		String selectedCc = selectCc.getText();
		if(selectedCc.length() == 0) selectedCc = Start.NULL_CHAIN_CODE;
		return selectedCc;
	}

	/** return the currently selected contact type 
	 * @throws LoadDialogInputError */
	public String getSelectedCt() throws LoadDialogInputError {
		String selectedCt = comboCt.getSelectedItem().toString();
		if(!AA.isValidCT(selectedCt)) {
			//System.err.println("The contact type " + selectedCt + " is invalid.");
			throw new LoadDialogInputError("The contact type " + selectedCt + " is invalid.");
		}
		return selectedCt;
	}
	
	/** return the currently selected distance cutoff 
	 * @throws LoadDialogInputError */
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
	
	/** return the currently selected min seq sep 
	 * @throws LoadDialogInputError */
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
	
	/** return the currently selected max seq sep 
	 * @throws LoadDialogInputError */
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

	/** return the currently selected database name */
	public String getSelectedDb() {
		String selectedDb = selectDb.getText();
		return selectedDb;
	}
	
	/** return the currently selected graph id 
	 * @throws LoadDialogInputError */
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
	
	/** action listener for load button */
	public void actionPerformed (ActionEvent e) {
		
		/* load button */
		if (e.getSource()== loadButton){
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

	/** test user input and if it looks fine, perform load and dispose the dialog */
	private void go() {
		try {
			String f = getSelectedFileName();
			String ac = getSelectedAc();
			String cc = getSelectedCc();
			String ct = getSelectedCt();
			double dist = getSelectedDist();
			int minss = getSelectedMinSeqSep();
			int maxss = getSelectedMaxSeqSep();
			String db = getSelectedDb();
			int gid = getSelectedGraphId();

			this.loadAction.doit((Object) parentFrame, f, ac, cc, ct, dist, minss, maxss, db, gid);
			this.setVisible(false);
			dispose();
		} catch (LoadDialogInputError e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Input error", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Debugging frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(false);

	        LoadDialog dialog = new LoadDialog(frame, "Test dialog", new LoadAction(false) {
	        	public void doit (Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
	        		System.out.println("You clicked the Ok button");
	        		System.out.println("Filename:\t" + f);
	        		System.out.println("PDB code:\t" + ac);
	        		System.out.println("Chain code:\t" + cc);
	        		System.out.println("Contact type:\t" + ac);
	        		System.out.println("Dist. cutoff:\t" + dist);
	        		System.out.println("Min. Seq. Sep.:\t" + minss);
	        		System.out.println("Max. Seq. Sep.:\t" + maxss);	        		
	        		System.out.println("Database:\t" + db);
	        		System.out.println("Graph Id:\t" + gid);
	        	};
	        }, "filename", "1tdr", "B", "Ca", "8.0", "0", "20", "pdbase", "1");
		dialog.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent event) {
		        System.exit(0);
		    }
		    public void windowClosed(WindowEvent event) {
		        System.exit(0);
		    }
		});
		dialog.createGUI();		
	}

}
