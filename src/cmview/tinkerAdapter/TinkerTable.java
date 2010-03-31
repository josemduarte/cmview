package cmview.tinkerAdapter;

import java.awt.BorderLayout;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.*;

import owl.core.runners.tinker.TinkerRunner;

import cmview.View;


/**
 * A window with a table to select the second model to load from a list 
 * of Tinker reconstruction results
 * @author Matthias Winkelmann
 *
 */

public class TinkerTable extends JFrame {

	static final long serialVersionUID = 1l;
	private JScrollPane scrollPane;
	private JTable table;
	private int lastSelectedStructure = -1;
	private TinkerTableModel tableModel;
	private View view;
	private TinkerRunner run;
	private class TinkerTableSelectionListener implements ListSelectionListener {
		
		TinkerTable table1;
		
		public TinkerTableSelectionListener(TinkerTable tab) {
			table1= tab;
		}
		
		public void valueChanged(ListSelectionEvent e) {
		
			table1.selectionChanged(e.getFirstIndex(),e.getLastIndex());
			
		}
		
		
	}

	public TinkerTable(TinkerRunner run, TinkerRunAction action, View view) {

		this.view = view;
		this.run = run;
		tableModel = new TinkerTableModel(run);
		table = new JTable(tableModel);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		table.getSelectionModel().addListSelectionListener(new TinkerTableSelectionListener(this));
		scrollPane = new JScrollPane(table);
		this.setLayout(new BorderLayout());
		this.add(scrollPane);
		this.showIt();

	}

	public void showIt() {
		pack();
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

	
	
	public void selectionChanged(int firstChangedRow, int secondChangedRow) {
		// the event always fires twice, so we cache the last value and do nothing if
		// the selection didn't effectively change
		//int firstStructure = table.convertRowIndexToModel(firstChangedRow)+1;
		//int secondStructure = table.convertRowIndexToModel(secondChangedRow)+1;
		//int last = lastSelectedStructure;
		//int newStruct = firstStructure;
		//if (firstStructure == lastSelectedStructure) {
		//	newStruct = secondStructure;
		//}
		
		//lastSelectedStructure = newStruct;
		
		// row order might have changed. Get selected structure id
		
		int sel = table.getSelectedRow();
		if (sel == -1) {
			return;
		}
		
		sel = table.convertRowIndexToModel(sel)+1;
		if (sel == lastSelectedStructure) {
			System.out.println(sel+"ignored");
			return;
		}
		lastSelectedStructure= sel;
		
		view.doLoadSecondModelFromPdbFile(run.getOutPdbFile(sel).getAbsolutePath());
		System.out.println("Now selected:"+sel);
	}
	
	public static void main(String[] args) {
		//Number[][] test = { { 1, 15, 3.4 }, { 2, 3, 1.4 }, { 3, 12, 0.4 },
		//		{ 4, 7, 3.3 }, { 5, 4, 6.6 }, { 6, 7, 5.1 }, { 7, 12, 4.9 },
		//		{ 8, 6, 2.0 } };
		
		
		//TinkerTable a = new TinkerTable(test);
		//a.showIt();
	}

}
