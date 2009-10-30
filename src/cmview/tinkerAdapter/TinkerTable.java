package cmview.tinkerAdapter;

import java.awt.BorderLayout;

import javax.swing.table.TableModel;

import javax.swing.*;

/**
 * A window with a table to select the second model to load from a list 
 * of Tinker reconstruction results
 * @author matt
 *
 */

public class TinkerTable extends JFrame {

	static final long serialVersionUID = 1l;
	JScrollPane scrollPane;
	JTable table;

	

	public TinkerTable() {

		
		TableModel model = new TinkerTableModel();
		table = new JTable(model);
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		
		scrollPane = new JScrollPane(table);
		this.setLayout(new BorderLayout());
		this.add(scrollPane);

	}

	public void showIt() {
		pack();
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

	public static void main(String[] args) {

		TinkerTable a = new TinkerTable();
		a.showIt();
	}

}
