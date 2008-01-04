package cmview;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import proteinstructure.IntPairSet;
import proteinstructure.Pdb;

import cmview.datasources.Model;

/**
 * JDialog for showing information about the currently loaded contact map(s).
 * @author stehr
 */
public class ContactMapInfoDialog extends JDialog implements ActionListener {

	/*------------------------------ constants ------------------------------*/
	static final long serialVersionUID = 1l;
	
	/*----------------------------- constructors ----------------------------*/
	ContactMapInfoDialog(JFrame f, Model mod, Model mod2, ContactMapPane cmPane) {
		
		// initialize this dialog
		super(f, true);
		this.setTitle("Contact map info");
		this.setResizable(false);
		
		// button pane
		JPanel buttonPane = new JPanel();
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(this);
		buttonPane.add(okButton, BorderLayout.CENTER);
		// sets the default botton of this dialog. hence, whenever the 
		// enter-key is being pressed and released this button is invoked
		this.getRootPane().setDefaultButton(okButton);
		
		// data pane
		int cols = 0;	// will be counted automatically
		int rows = (mod2==null?2:3);
		JPanel dataPane = new JPanel();

		// initialize data
		String seq = mod.getSequence();
		String seq2 = null;
		String s1 = seq.length() <= 10?(seq.length()==0?"Unknown":seq):seq.substring(0,10) + "...";
		String s2 = "";
		int numSelectedContacts = 0;
		if( mod2 == null ) {
			// settings if mod2 is absent
			numSelectedContacts = cmPane.getSelContacts().size();
		} else {
			// settings if mod2 is present
			seq2 = mod2.getSequence();
			s2  = seq2.length() <= 10?(seq2.length()==0?"Unknown":seq2):seq2.substring(0,10) + "...";

			TreeMap<ContactMapPane.ContactSelSet, IntPairSet[]> selMap = cmPane.getSelectedContacts(false);
			IntPairSet union = new IntPairSet();
			union.addAll(selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST]);
			union.addAll(selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.SECOND]);
			union.addAll(selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST]);
			union.addAll(selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.SECOND]);
			union.addAll(selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND]);
			union.addAll(selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.FIRST]);
			
			numSelectedContacts = union.size();
		}
		String pdbCode = null, pdbCode2 = null;
		String chainCode = null, chainCode2 = null;
		String model = null, model2 = null;
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
		
		pdbCode = (mod.getPDBCode()==Pdb.NO_PDB_CODE?"none":mod.getPDBCode());
		chainCode = (mod.getChainCode()==Pdb.NO_CHAIN_CODE?"none":mod.getChainCode());
		model = Integer.toString(mod.getPdbModelNumber());
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
			pdbCode2 = mod2.getPDBCode();
			chainCode2 = mod2.getChainCode();
			model2 = Integer.toString(mod2.getPdbModelNumber());
			contactType2 = mod2.getContactType();
			distCutoff2 = Double.toString(mod2.getDistanceCutoff());
			minSeqSepStr2 = (mod2.getMinSequenceSeparation()<1?"none":Integer.toString(mod2.getMinSequenceSeparation()));
			maxSeqSepStr2 = (mod2.getMaxSequenceSeparation()<1?"none":Integer.toString(mod2.getMaxSequenceSeparation()));
			contactMapSize2 = Integer.toString(mod2.getMatrixSize());
			unobservedResidues2 = Integer.toString(mod2.getNumberOfUnobservedResidues());
			numContacts2 = Integer.toString(mod2.getNumberOfContacts());
			directed2 = (mod2.isDirected()?"Yes":"No");
			secStrucSrc2 = mod2.getSecondaryStructure().getComment();
			commonContactsStr = Integer.toString(cmPane.getCommonContacts(1, 2).size());
		}
		
		// pdb code
		dataPane.add(new JLabel("Pdb code:"));
		dataPane.add(new JLabel(pdbCode));
		if(mod2 != null) {
			dataPane.add(new JLabel(pdbCode2));
		}
		rows++;
		
		// chain code
		dataPane.add(new JLabel("Chain code:"));
		dataPane.add(new JLabel(chainCode));
		if(mod2 != null) {
			dataPane.add(new JLabel(chainCode2));
		}
		rows++;	
		
		// pdb model number
		dataPane.add(new JLabel("Model:"));
		dataPane.add(new JLabel(model));
		if(mod2 != null) {
			dataPane.add(new JLabel(model2));
		}
		rows++;	
		
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
		
		// contact map size
		dataPane.add(new JLabel("Contact map size:"));
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
		
		// number of contacts
		dataPane.add(new JLabel("Number of contacts:"));
		dataPane.add(new JLabel(numContacts));
		if(mod2 != null) {
			dataPane.add(new JLabel(numContacts2));
		}
		rows++;			
		
		// is directed
		if(Start.INCLUDE_GROUP_INTERNALS) {
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
		
		// sequence
		dataPane.add(new JLabel("Sequence:"));
		JLabel seqLabel1 = new JLabel(s1);
		seqLabel1.setToolTipText(seq);
		dataPane.add(seqLabel1);
		if(mod2 != null) {
			JLabel seqLabel2 = new JLabel(s2);
			seqLabel2.setToolTipText(seq2);
			dataPane.add(seqLabel2);
		}
		rows++;
		
		// secondary structure source
		dataPane.add(new JLabel("Secondary structure from:"));
		dataPane.add(new JLabel(secStrucSrc));
		if(mod2 != null) {
			dataPane.add(new JLabel(secStrucSrc2));
		}
		rows++;
		
		// blank line
		dataPane.add(new JLabel(" "));
		dataPane.add(new JLabel(" "));
		if(mod2 != null) {
			dataPane.add(new JLabel(" "));
		}
		rows++;
		
		// number of common contacts
		if(mod2 != null) {
			dataPane.add(new JLabel("Number of common contacts:"));
			dataPane.add(new JLabel(commonContactsStr));
			dataPane.add(new JLabel(" "));		
			rows++;
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
		this.setVisible(false);
		dispose();
	}
}