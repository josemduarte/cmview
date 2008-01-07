package cmview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import proteinstructure.Alignment;

/**
 * A JDialog to show a single sequence or an alignment of sequences in a scrollpane.
 * @author stehr
 */
public class SequenceViewDialog extends JDialog implements ActionListener {
	
	/*------------------------------ constants ------------------------------*/
	static final long serialVersionUID = 1l;
	static final int INITIAL_WINDOW_WIDTH = 700;	// TODO: Move this to constants in Start
	
	/*--------------------------- member variables --------------------------*/
	JEditorPane seqField = new JEditorPane();
	ContactMapPane cmPane;	// only needed for node selections
	int[] seqOffsets;	// stores the index of the beginning of each sequence in the JEditorPane
	
	/*----------------------------- constructors ----------------------------*/
	
	/**
	 * Create a new sequence view dialog from an alignment. Currently only tested for alignments with exactly two sequences.
	 */
	SequenceViewDialog(JFrame f, Alignment alignment, ContactMapPane cmPane) {
		super(f, true);
		// pack sequences into array
		String[] names = new String[alignment.getNumberOfSequences()];
		String[] sequences = new String[alignment.getNumberOfSequences()];
		int i = 0;
		for(String tag:alignment.getTags()) {
			names[i] = tag;
			sequences[i] = alignment.getAlignedSequence(tag);
			i++;
		}
		init(names, sequences);
		this.cmPane = cmPane;
	}
	
	/**
	 * Create a new sequence view dialog from a single sequence.
	 * @param f
	 * @param strucName
	 * @param sequence
	 * TODO: Merge with above, taking an alignment with a single sequence.
	 */
	SequenceViewDialog(JFrame f, String strucName, String sequence, ContactMapPane cmPane) {
		super(f, true);
		String[] names = {strucName};
		String[] sequences = {sequence};	
		init(names, sequences);
		this.cmPane = cmPane;
	}	
	
	/*---------------------------- private methods --------------------------*/
	
	/**
	 * Create the dialog showing the given sequences and sequence (i.e. structure) names.
	 */
	private void init(String[] names, String[] sequences) {
		this.setTitle("Sequence view");
		
		// preferences
		String htmlFont = "Monospaced";
		
		// set sequence offsets
		this.seqOffsets = new int[sequences.length];
		int idx = 0;
		for (int j = 0; j < sequences.length; j++) {
			idx += names[j].length();
			seqOffsets[j] = idx;
			idx += sequences[j].length();
		}
		
		// create text to be rendered
		String header = "<html><table>";
		String body = "";
		for (int i = 0; i < sequences.length; i++) {
			body += String.format("<tr><td><font face=%s>%s<font></td><td><font face=%s>%s</font></td></tr>", htmlFont, names[i], htmlFont, sequences[i]);
		}
		String footer = "</table></html>";

		// create a temporary JEditorPane to find out the preferred size after html rendering 
		String text = header + body + footer;
		JEditorPane testWidth = new JEditorPane("text/html", text);
		Dimension layoutSize = testWidth.getPreferredSize();
		
		// now create the real JEditorPane, setting the width of the table to the right values
		header = String.format("<html><table width=%d>", layoutSize.width);
		text = header + body + footer;
		seqField.setContentType("text/html");
		seqField.setText(text);
		seqField.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(seqField);
		scrollPane.setBorder(BorderFactory.createLineBorder(Color.gray));
		
		// init button pane
		JPanel buttonPane = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.addActionListener(this);
		buttonPane.add(okButton);
		this.getRootPane().setDefaultButton(okButton);

		// put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		this.pack();
		
		// resize window - TODO: give proper preferred size values to the contained components to avoid later resizing
		this.setSize(INITIAL_WINDOW_WIDTH, 100 + 20 * sequences.length); // TODO: this is a quick workaround for the release version
		seqField.setCaretPosition(0); // this is necessary to scroll the view back to the left of the sequence
	}
	
	/*-------------------------- implemented methods ------------------------*/
	
	public void actionPerformed(ActionEvent arg0) {
		// fetch position of selection and send to viewer (experimental)
		if(Start.INCLUDE_GROUP_INTERNALS) {
			int start = seqField.getSelectionStart();
			int end = seqField.getSelectionEnd();
			int startSeq = 0, endSeq = 0, startPos = 0, endPos = 0;	// all counting from 1..n
			for (int i = 0; i < seqOffsets.length; i++) {
				if(start > seqOffsets[i]) {
					startSeq = i+1;
					startPos = start - seqOffsets[i] - 1;
				}
				if(end > seqOffsets[i]) {
					endSeq = i+1;
					endPos = end - seqOffsets[i] - 2;
				}
			}
			if(startSeq == endSeq && startSeq == 1) {
				System.out.println("Selecting nodes " + startPos + "-" + endPos);
				for (int i = startPos; i <= endPos; i++) {
					cmPane.selectNodeNbh(i);
				}
				cmPane.repaint();
			}
		}
		this.setVisible(false);
		dispose();
	}
	
}
