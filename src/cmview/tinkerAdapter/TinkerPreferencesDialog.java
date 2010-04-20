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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import owl.core.runners.tinker.TinkerRunner;


/**
 * A Dialog to specify preferences for running tinker with the currently loaded first Model
 * @author Matthias Winkelmann
 *
 */


public class TinkerPreferencesDialog extends JDialog implements ActionListener {

	static final long serialVersionUID = 1l;
	static final String REFINEMENT_LABEL = "Refinement Method";
	static final String NUM_MODELS_LABEL = "Number of Models";
	static final String PARALLELIZATION_LABEL = "Paralellization";
	static final String WINDOW_TITLE = "Start Distgeom Run";
	private JButton runButton, cancelButton;
	private JComboBox refinementCB, parallelCB;
	private JSpinner numModelsSpinner;
	private SpinnerNumberModel numModelsSpinnerModel;
	private TinkerAction callback;
	private JCheckBox gmbpConstraints;
	
	public TinkerPreferencesDialog(JFrame f, TinkerAction cb,boolean hasGMBP) {
		super(f, WINDOW_TITLE, true);
		this.setResizable(false);
		callback = cb;
		runButton = new JButton("Run");
		runButton.addActionListener(this);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		this.getRootPane().setDefaultButton(runButton);
		Object[] refinementOptions = {"Fast (minimization)",  "Slow (annealing)"};
		refinementCB = new JComboBox();
		for (Object o : refinementOptions) {
			refinementCB.addItem(o);

		}
		refinementCB.setSelectedIndex(0);
		refinementCB.setEditable(false);

		Object[] parallelizationOptions = { "None", "Cluster" };
		parallelCB = new JComboBox();
		for (Object o : parallelizationOptions) {
			parallelCB.addItem(o);

		}
		parallelCB.setSelectedIndex(0);
		parallelCB.setEditable(false);

		numModelsSpinnerModel = new SpinnerNumberModel(3, 1, 250, 1);
		numModelsSpinner = new JSpinner(numModelsSpinnerModel);
		gmbpConstraints = new JCheckBox("Apply GMBP Constraints");
		if (hasGMBP) {
			gmbpConstraints.setEnabled(true);
			gmbpConstraints.setSelected(true);
		} else {
			gmbpConstraints.setEnabled(false);
			gmbpConstraints.setSelected(false);
		}
		
		JPanel inputPane = new JPanel();
		JLabel labelAfterRefinement = new JLabel("Refinement Method");
		JLabel labelAfterNumModels = new JLabel("Number of Models");
		JLabel labelAfterParallelization = new JLabel("Parallel Mode");
		inputPane.add(refinementCB);
		inputPane.add(labelAfterRefinement);
		inputPane.add(parallelCB);
		inputPane.add(labelAfterParallelization);
		inputPane.add(numModelsSpinner);
		inputPane.add(labelAfterNumModels);

		GridLayout layout = new GridLayout(3, 3);
		layout.setHgap(5);
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
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(runButton);

		// Put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(selectionPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == runButton) {
			this.go();
		}

		if (e.getSource() == cancelButton) {
			this.setVisible(false);
			dispose();
		}
	}

	private TinkerRunner.REFINEMENT getRefinement() {
	
		switch (refinementCB.getSelectedIndex()) {
		case 0:
			return TinkerRunner.REFINEMENT.MINIMIZATION;
		default:
			return TinkerRunner.REFINEMENT.ANNEALING;
		}
	}
	
	private TinkerRunner.PARALLEL getParallel() {
	
		switch (parallelCB.getSelectedIndex()) {
		case 1:
			return TinkerRunner.PARALLEL.CLUSTER;
		default:
			return TinkerRunner.PARALLEL.NONE;
		}
	}
	
	private boolean getGMBP() {
		return gmbpConstraints.isSelected();
	}
	
	private int getNumModels() {
	
		return numModelsSpinnerModel.getNumber().intValue();
	}
	
	
	private void go() {
		callback.doit(getParallel(),getRefinement(),getNumModels(),getGMBP());
	}
	
	public void createGUI() {
		pack();
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Debugging frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(false);

		try {
			TinkerPreferencesDialog dialog = new TinkerPreferencesDialog(frame,null,false);
			dialog.createGUI();
		} finally {
		}
	}
}
