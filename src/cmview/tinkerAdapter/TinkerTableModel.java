package cmview.tinkerAdapter;

import javax.swing.table.AbstractTableModel;

import tinker.TinkerRunner;

/**
 * The table model for TinkerTable.
 * @author Matthias Winkelmann
 *
 */

public class TinkerTableModel extends AbstractTableModel {

	
	
	private String[] columnNames = { "Structure #", "Constraints violated", "RMSD" };

	private TinkerRunner run;

	public TinkerTableModel(TinkerRunner run) {
		this.run = run;
	}
	
	private static final long serialVersionUID = 1L;

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getRowCount() {
		return run.getLastNumberOfModels();
	}

	@Override
	public Number getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0: 
			return rowIndex+1;
		case 1:
			return run.getBoundViols(rowIndex+1);
		}
		return 0;
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
