package cmview.tinkerAdapter;

import javax.swing.table.AbstractTableModel;

/**
 * The table model for TinkerTable.
 * @author Matthias Winkelmann
 *
 */

public class TinkerTableModel extends AbstractTableModel {

	
	private String[] columnNames = { "Structure #", "Constraints violated", "RMSD" };

	private Number[][] data = { { 1, 15, 3.4 }, { 2, 3, 1.4 }, { 3, 12, 0.4 },
			{ 4, 7, 3.3 }, { 5, 4, 6.6 }, { 6, 7, 5.1 }, { 7, 12, 4.9 },
			{ 8, 6, 2.0 } };

	
	
	private static final long serialVersionUID = 1L;

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data[rowIndex][columnIndex];
	}
	
	@Override
	public String getColumnName(int c) {
		return columnNames[c];
	}
	
	@Override
	public Class<?> getColumnClass(int c) {
		if (c < 2) {
			return Integer.class;
		}
		
		return Float.class;
	
    }

}
