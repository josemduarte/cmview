package cmview.tinkerAdapter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import javax.swing.JPanel;

import cmview.Start;

import tinker.TinkerRunner;


/**
 * A dialog giving feedback to the user while Tinker is doing its thing.
 * @author Matthias Winkelmann
 */

public class TinkerProgressDialog extends JDialog implements ActionListener {

	static final long serialVersionUID = 1l;
	static final String CREATE_PROTEIN_LABEL = "Creating unfolded protein";
	static final String CHOOSE_BEST_LABEL = "Selecting best structure";
	static final String LOADING_LABEL = "Loading as second structure";
	
	static final String WINDOW_TITLE = "Running Tinker...";
	
	TinkerRunAction runner;
	JButton cancelButton;
	JLabel labelProtein, labelDistgeom, labelChooseStructure, labelLoading;
	ImageIcon iconOpen, iconProgress, iconDone, iconWait;
	int structures;
	public TinkerProgressDialog(JFrame f, TinkerRunAction action, int structs) {
		
		super(f, WINDOW_TITLE, true);
		runner = action;
		this.setResizable(false);
		this.setModal(true);
		structures = structs;
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);

		

		JPanel inputPane = new JPanel();
		
		iconDone = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "tick.png"));
		iconProgress = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cog.png"));
		iconOpen = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "lightning.png"));
		iconWait = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cup.png"));
		labelProtein = new JLabel(CREATE_PROTEIN_LABEL,iconOpen,JLabel.LEFT);
		
		// We use the longest possible label here (and overwrite the text before showing it) to get the right size for the dialog
		
		labelDistgeom = new JLabel("Generating Structures ("+structures+"/"+structures+" done)",iconOpen,JLabel.LEFT);
		labelChooseStructure = new JLabel(CHOOSE_BEST_LABEL,iconOpen,JLabel.LEFT);
		labelLoading = new JLabel(LOADING_LABEL,iconOpen,JLabel.LEFT);
		labelProtein.setEnabled(false);
		labelDistgeom.setEnabled(false);
		labelChooseStructure.setEnabled(false);
		labelLoading.setEnabled(false);
		inputPane.add(labelProtein);
		inputPane.add(labelDistgeom);
		inputPane.add(labelChooseStructure);
		inputPane.add(labelLoading);

		GridLayout layout = new GridLayout(5,2);
		layout.setHgap(5);
		layout.setVgap(3);
		inputPane.setLayout(layout);

		JPanel selectionPane = new JPanel();
		selectionPane.setLayout(new BoxLayout(selectionPane,
				BoxLayout.LINE_AXIS));

		// selectionPane.add(labelPane);
		selectionPane.add(inputPane);
		selectionPane.add(Box.createRigidArea(new Dimension(0, 5)));
		selectionPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(cancelButton);

		// Put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(selectionPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == cancelButton) {
			this.setVisible(false);
			runner.cancel();
			dispose();
		}
	}
	
	public void createGUI() {
		pack();
		setLocationRelativeTo(getParent());
		labelDistgeom.setText("Generating Structures");
		setVisible(true);
	}
	
	public void setState(TinkerRunner.STATE s) {
		
		switch(s) {
		case PROTEIN:
			labelProtein.setEnabled(true);
			labelProtein.setIcon(iconProgress);
			break;
		case STRUCTURES:
			labelProtein.setIcon(iconDone);
			filesDone(0);
			labelDistgeom.setEnabled(true);
			labelDistgeom.setIcon(iconWait);
			break;
		case SELECTION:
			labelDistgeom.setIcon(iconDone);
			labelChooseStructure.setEnabled(true);
			labelChooseStructure.setIcon(iconProgress);
			break;
		case LOADING:
			labelChooseStructure.setIcon(iconDone);
			labelLoading.setEnabled(true);
			labelLoading.setIcon(iconProgress);
		}
		this.repaint();
		
	}
	
	public static void main(String[] args) throws InterruptedException {
		JFrame frame = new JFrame();
		frame.setTitle("Debugging frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(false);
		TinkerProgressDialog dialog;
		try {
			dialog = new TinkerProgressDialog(frame,null,2);
			dialog.createGUI();
			
		} finally {
		}

		
		
	}

	public void filesDone(int done) {
		labelDistgeom.setText("Generating Structures ("+done+"/"+structures+" done)");
		
	}

	
	
	
}
