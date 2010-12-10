package cmview;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import owl.core.structure.scoring.ResidueContactScoringFunction;

class ScoringFunctionController extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private ResidueContactScoringFunction f;
	private StatusBar statusBar;
	private JButton addBestButton, removeWorstButton;
	private JLabel rfLabel;
	
	public ScoringFunctionController(ResidueContactScoringFunction f, StatusBar sb) {
		this.f = f;
		statusBar = sb;
		this.setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
		String title = f.getMethodName();
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), title));
		rfLabel= new JLabel("Score: "+f.getOverallScore());
		dataChanged();
		addBestButton = new JButton("add best");
		addBestButton.setMaximumSize(new Dimension(120,50));
		addBestButton.setAlignmentX(CENTER_ALIGNMENT);
		
		removeWorstButton = new JButton("remove worst");
		removeWorstButton.setMaximumSize(new Dimension(120,50));
		removeWorstButton.setAlignmentX(CENTER_ALIGNMENT);
		
		addBestButton.addActionListener(this);
		removeWorstButton.addActionListener(this);

		this.add(Box.createRigidArea(new Dimension(statusBar.getGroupWidth(),10)));	// defines component width
		this.add(rfLabel);
		this.add(Box.createRigidArea(new Dimension(0,10)));
		this.add(addBestButton);
		this.add(Box.createRigidArea(new Dimension(0,5)));
		this.add(removeWorstButton);
		this.add(Box.createRigidArea(new Dimension(0,5)));
		this.setVisible(false);
	}

	
	public ResidueContactScoringFunction getScoringFunction() {
		return this.f;
	}

	public void dataChanged() {
		rfLabel.setText("Score: "+f.getOverallScore());
	}
	
	public void updateScores() {
		dataChanged();
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == addBestButton) {
			
		} else if (e.getSource() == removeWorstButton) {
			
		}
		
	}
}