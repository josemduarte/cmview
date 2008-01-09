package cmview;

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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import proteinstructure.Alignment;

import cmview.datasources.Model;

/**
 * JDialog for showing information about the currently loaded contact map(s).
 * @author stehr
 */
public class ContactMapInfoDialog extends JDialog implements ActionListener {

	/*------------------------------ constants ------------------------------*/
	static final long serialVersionUID = 1l;
	
	/*--------------------------- member variables --------------------------*/
	JButton okButton;
	JButton showSeqButton;
	JFrame parent;
	Model mod, mod2;
	Alignment alignment;
	ContactMapPane cmPane;
	
	/*----------------------------- constructors ----------------------------*/
	ContactMapInfoDialog(JFrame f, Model mod, Model mod2, Alignment alignment, ContactMapPane cmPane) {
		
		// initialize this dialog
		super(f, true);
		this.setTitle("Contact map info");
		this.setResizable(false);
		
		// set member variables
		this.parent = f;
		this.mod = mod;
		this.mod2 = mod2;
		this.alignment = alignment;
		this.cmPane = cmPane;
		
		// button pane
		JPanel buttonPane = new JPanel();
		okButton = new JButton("OK");
		showSeqButton = new JButton("Show sequence");
		if(mod2 != null) {
			showSeqButton.setText("Show alignment");
		}
		okButton.addActionListener(this);
		showSeqButton.addActionListener(this);
		
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(showSeqButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(okButton);

		// sets the default botton of this dialog. hence, whenever the 
		// enter-key is being pressed and released this button is invoked
		this.getRootPane().setDefaultButton(okButton);
		
		// data pane
		int rows = (mod2==null?2:3);
		int cols = 0;	// will be counted automatically
		int showSeqLen = 15;	// how many residues to show in main window
		
		JPanel dataPane = new JPanel();

		// initialize data
		String seq = mod.getSequence();
		String seq2 = null;
		String s1 = seq.length() <= showSeqLen?(seq.length()==0?"Unknown":seq):seq.substring(0,showSeqLen) + "...";
		String s2 = "";
		int numSelectedContacts = cmPane.getSelContacts().size();
		if( mod2 != null ) {
			// settings if mod2 is present
			seq2 = mod2.getSequence();
			s2  = seq2.length() <= showSeqLen?(seq2.length()==0?"Unknown":seq2):seq2.substring(0,showSeqLen) + "...";
		}

//		String pdbCode = null, pdbCode2 = null;
//		String chainCode = null, chainCode2 = null;
//		String model = null, model2 = null;
		String contactType = null, contactType2 = null;
		String distCutoff = null, distCutoff2 = null;
		String minSeqSepStr = null, maxSeqSepStr = null;
		String minSeqSepStr2 = null, maxSeqSepStr2 = null;
		String contactMapSize = null, contactMapSize2 = null;
		String unobservedResidues = null, unobservedResidues2 = null;
		String numContacts = null, numContacts2 = null;
		String directed = null, directed2 = null;
		String secStrucSrc = null, secStrucSrc2 = null;
		String commonContactsStr = null, selectedContactsStr = null;
		String uniqueContactsStr1 = null, uniqueContactsStr2 = null;
		
//		pdbCode = (mod.getPDBCode()==Pdb.NO_PDB_CODE?"none":mod.getPDBCode());
//		chainCode = (mod.getChainCode()==Pdb.NO_CHAIN_CODE?"none":mod.getChainCode());
//		model = Integer.toString(mod.getPdbModelNumber());
		contactType = mod.getContactType();
		distCutoff = Double.toString(mod.getDistanceCutoff());
		minSeqSepStr = (mod.getMinSequenceSeparation()<1?"none":Integer.toString(mod.getMinSequenceSeparation()));
		maxSeqSepStr = (mod.getMaxSequenceSeparation()<1?"none":Integer.toString(mod.getMaxSequenceSeparation()));
		contactMapSize = Integer.toString(mod.getMatrixSize());
		unobservedResidues = Integer.toString(mod.getNumberOfUnobservedResidues());
		numContacts = Integer.toString(mod.getNumberOfContacts());
		directed = (mod.isDirected()?"Yes":"No");
		secStrucSrc = mod.getSecondaryStructure().getComment();
		selectedContactsStr = Integer.toString(numSelectedContacts);
		if(mod2 != null) {
//			pdbCode2 = (mod2.getPDBCode()==Pdb.NO_PDB_CODE?"none":mod2.getPDBCode());
//			chainCode2 = (mod2.getChainCode()==Pdb.NO_CHAIN_CODE?"none":mod2.getChainCode());
//			model2 = Integer.toString(mod2.getPdbModelNumber());
			contactType2 = mod2.getContactType();
			distCutoff2 = Double.toString(mod2.getDistanceCutoff());
			minSeqSepStr2 = (mod2.getMinSequenceSeparation()<1?"none":Integer.toString(mod2.getMinSequenceSeparation()));
			maxSeqSepStr2 = (mod2.getMaxSequenceSeparation()<1?"none":Integer.toString(mod2.getMaxSequenceSeparation()));
			contactMapSize2 = Integer.toString(mod2.getMatrixSize());
			unobservedResidues2 = Integer.toString(mod2.getNumberOfUnobservedResidues());
			numContacts2 = Integer.toString(mod2.getNumberOfContacts());
			directed2 = (mod2.isDirected()?"Yes":"No");
			secStrucSrc2 = mod2.getSecondaryStructure().getComment();
			int commonContacts = cmPane.getCommonContacts(1, 2).size();
			commonContactsStr = Integer.toString(commonContacts);
			uniqueContactsStr1 = Integer.toString(mod.getNumberOfContacts() - commonContacts);
			uniqueContactsStr2 = Integer.toString(mod2.getNumberOfContacts() - commonContacts);
		}
		
		// contact map ids
		dataPane.add(new JLabel("Name:"));
		dataPane.add(new JLabel(mod.getLoadedGraphID()));
		if(mod2 != null) {
			dataPane.add(new JLabel(mod2.getLoadedGraphID()));
		}
		rows++;
		
		// blank line
		dataPane.add(new JLabel(" "));
		dataPane.add(new JLabel(" "));
		if(mod2 != null) {
			dataPane.add(new JLabel(" "));
		}
		rows++;	
		
//		// pdb code
//		dataPane.add(new JLabel("Pdb code:"));
//		dataPane.add(new JLabel(pdbCode));
//		if(mod2 != null) {
//			dataPane.add(new JLabel(pdbCode2));
//		}
//		rows++;
//		
//		// chain code
//		dataPane.add(new JLabel("Chain code:"));
//		dataPane.add(new JLabel(chainCode));
//		if(mod2 != null) {
//			dataPane.add(new JLabel(chainCode2));
//		}
//		rows++;	
//		
//		// pdb model number
//		dataPane.add(new JLabel("Model:"));
//		dataPane.add(new JLabel(model));
//		if(mod2 != null) {
//			dataPane.add(new JLabel(model2));
//		}
//		rows++;	
		
		// contact type
		dataPane.add(new JLabel("Contact type:"));
		dataPane.add(new JLabel(contactType));
		if(mod2 != null) {
			dataPane.add(new JLabel(contactType2));
		}
		rows++;
		
		// distance cutoff
		dataPane.add(new JLabel("Distance cutoff:"));
		dataPane.add(new JLabel(distCutoff));
		if(mod2 != null) {
			dataPane.add(new JLabel(distCutoff2));
		}
		rows++;
		
		// minimum sequence separation
		dataPane.add(new JLabel("Min Seq Sep:"));
		dataPane.add(new JLabel(minSeqSepStr));
		if(mod2 != null) {
			dataPane.add(new JLabel(minSeqSepStr2));
		}
		rows++;
		
		// maximum sequence separation
		dataPane.add(new JLabel("Max Seq Sep:"));
		dataPane.add(new JLabel(maxSeqSepStr));
		if(mod2 != null) {
			dataPane.add(new JLabel(maxSeqSepStr2));
		}
		rows++;	
		
		// blank line
		dataPane.add(new JLabel(" "));
		dataPane.add(new JLabel(" "));
		if(mod2 != null) {
			dataPane.add(new JLabel(" "));
		}
		rows++;	
		
		// sequence
		dataPane.add(new JLabel("Sequence:"));
		JLabel seqLabel1 = new JLabel(s1);
		String toolTipText = formatSequenceForHtml(seq);
		seqLabel1.setToolTipText(toolTipText);
		dataPane.add(seqLabel1);
		if(mod2 != null) {
			JLabel seqLabel2 = new JLabel(s2);
			toolTipText = formatSequenceForHtml(seq2);
			seqLabel2.setToolTipText(toolTipText);
			dataPane.add(seqLabel2);
		}
		rows++;
		
		// sequence length (= contact map size)
		dataPane.add(new JLabel("Sequence length:"));
		dataPane.add(new JLabel(contactMapSize));
		if(mod2 != null) {
			dataPane.add(new JLabel(contactMapSize2));
		}
		rows++;
				
		// unobserved residues
		dataPane.add(new JLabel("Unobserved residues:"));
		dataPane.add(new JLabel(unobservedResidues));
		if(mod2 != null) {
			dataPane.add(new JLabel(unobservedResidues2));
		}
		rows++;	
				
		// secondary structure source
		dataPane.add(new JLabel("Secondary structure source:"));
		dataPane.add(new JLabel(secStrucSrc));
		if(mod2 != null) {
			dataPane.add(new JLabel(secStrucSrc2));
		}
		rows++;

		if(Start.INCLUDE_GROUP_INTERNALS) {

			// blank line
			dataPane.add(new JLabel(" "));
			dataPane.add(new JLabel(" "));
			if(mod2 != null) {
				dataPane.add(new JLabel(" "));
			}
			rows++;

			// is directed

			dataPane.add(new JLabel("Directed:"));
			dataPane.add(new JLabel(directed));
			if(mod2 != null) {
				dataPane.add(new JLabel(directed2));
			}
			rows++;
		}
		
		// blank line
		dataPane.add(new JLabel(" "));
		dataPane.add(new JLabel(" "));
		if(mod2 != null) {
			dataPane.add(new JLabel(" "));
		}
		rows++;
		
		// number of contacts
		dataPane.add(new JLabel("Number of contacts:"));
		dataPane.add(new JLabel(numContacts));
		if(mod2 != null) {
			dataPane.add(new JLabel(numContacts2));
		}
		rows++;
		
		if(mod2 != null) {
		
			// number of unique contacts
			if(mod2 != null) {
				dataPane.add(new JLabel("Number of unique contacts:"));
				dataPane.add(new JLabel(uniqueContactsStr1));
				dataPane.add(new JLabel(uniqueContactsStr2));			
				rows++;
			}		

			// number of common contacts
			if(mod2 != null) {
				dataPane.add(new JLabel("Number of common contacts:"));
				dataPane.add(new JLabel(commonContactsStr));
				dataPane.add(new JLabel(commonContactsStr));		
				rows++;
			}
			
		}
		
		// number of selected contacts
		dataPane.add(new JLabel("Number of selected contacts:"));
		dataPane.add(new JLabel(selectedContactsStr));
		if(mod2 != null) {
			dataPane.add(new JLabel(" "));
		}		
		rows++;
		
		// add data to data pane
		dataPane.setBorder(BorderFactory.createEmptyBorder(15,15,0,15));
		GridLayout dataLayout = new GridLayout(rows, cols);
		dataLayout.setHgap(10);
		dataLayout.setVgap(5);
		dataPane.setLayout(dataLayout);
		
		// Put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(dataPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		this.pack();
	}

	/*-------------------------- implemented methods ------------------------*/
	
	public void actionPerformed(ActionEvent arg0) {
		
		if(arg0.getSource() == okButton) {
			this.setVisible(false);
			dispose();			
		}
		
		if(arg0.getSource() == showSeqButton) {
			this.setVisible(false);
			dispose();
			
			JDialog seqDialog = null;
			if(alignment == null) {
				seqDialog = new SequenceViewDialog(parent, mod.getLoadedGraphID(), mod.getSequence(), cmPane);
			} else {
				seqDialog = new SequenceViewDialog(parent, alignment, cmPane);
			}
			seqDialog.setLocationRelativeTo(parent);
			seqDialog.setVisible(true);			
		}
	}
	
	/*---------------------------- private methods --------------------------*/
	
	private String formatSequenceForHtml(String seq) {
		int charsPerRow = 100;	// format sequence with this number of characters per row
		String html = "<html>Full sequence:<br>";
		int i = 0;
		while(i + charsPerRow < seq.length()) {
			html += String.format("%s<br>", seq.substring(i, i+charsPerRow));
			i += charsPerRow;
		}
		html += seq.substring(i, seq.length()) + "<html>";
		return html;
	}
}
