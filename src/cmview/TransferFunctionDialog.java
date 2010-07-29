package cmview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class TransferFunctionDialog extends JFrame implements ActionListener{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	HashMap<JPopupMenu,JMenu> popupMenu2Parent;
	// indices of the all main menus in the frame's menu bar
	TreeMap<String, Integer> menu2idx;
	
	//Menu items
	JMenuItem mmLoad, mmSave;
	// menu item labels 
	private static final String LABEL_FILE_LOAD = "Load TFFct";
	private static final String LABEL_FILE_SAVE = "Save TFFct";
	
	ContactMapPane cmPane;
	View view;
	
	TransferFunctionBar tfFct;
	StatusBar stat;
	JPanel tffP;
	
//	private class MyDispatcher implements KeyEventDispatcher {
//		private TransferFunctionBar tfFct;
//		private MyDispatcher(TransferFunctionBar tfFct){
//			this.tfFct = tfFct;
//		}
//	    public boolean dispatchKeyEvent(KeyEvent e) {
//	    	// -- forward key events --
//	        if (e.getID() == KeyEvent.KEY_PRESSED) {
//	        	this.tfFct.keyPressed(e);
//	        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
//	        	this.tfFct.keyReleased(e);
//	        } else if (e.getID() == KeyEvent.KEY_TYPED) {
//	        	this.tfFct.keyTyped(e);
//	        }
//	        return false;
//	    }
//	}
	
	TransferFunctionDialog(View view, ContactMapPane cmPane){
		super("TransferFunction Selection");
		this.view = view;
		this.cmPane = cmPane;
		this.tfFct = new TransferFunctionBar(this, this.view);
		stat = new StatusBar(view);
		
//		setMinimumSize(new Dimension(4*TransferFunctionBar.DEFAULT_WIDTH, TransferFunctionBar.DEFAULT_HEIGHT));
		setPreferredSize(new Dimension(10+4*TransferFunctionBar.DEFAULT_WIDTH, 20+40+TransferFunctionBar.DEFAULT_HEIGHT));
		initFrame();
		setVisible(true);							// show GUI	

//		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
//	    manager.addKeyEventDispatcher(new MyDispatcher(this.tfFct));
		
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
	}
	
	/**
	 * Adds a menu to the menubar of this View object. Moreover, some mappings are done:
	 * <ul>
	 * <li>mapping from menu identifier (string value obtained by <code>menu.getText()</code>)</li>
	 * <li>mapping from the menu's JPopupMenu to the menu</li>
	 * </ul> 
	 * @param menu  the menu to be added 
	 */
	public void addToJMenuBar(JMenu menu) {
		getJMenuBar().add(menu);
		menu2idx.put(menu.getText(), getJMenuBar().getMenuCount()-1);
		popupMenu2Parent.put(menu.getPopupMenu(),menu);
	}
	

	public TransferFunctionBar getTransfFctBar(){
		return this.tfFct;
	}
	
	/*---------------------------- Event handling --------------------------*/

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource() == mmLoad) {
			handleLoad();
		}	
		if(e.getSource() == mmSave) {
			handleSave();
		}	
		
	}
	
	/*---------------------------- private methods --------------------------*/

	/**
	 * Sets up and returns a new menu item with the given icon and label, adds it to the given JMenu and
	 * registers 'this' as the action listener.
	 * @param label the text of the item in the menu
	 * @param icon an optional icon to be shown in the menu
	 * @param menu the JMenu this item will be added to, or null if the item should be invisible
	 */
	private JMenuItem makeMenuItem(String label, Icon icon, JMenu menu) {
		JMenuItem newItem = new JMenuItem(label, icon);
		newItem.addActionListener(this);
		if(menu != null) menu.add(newItem);
		return newItem;
	}

	private void initFrame() {
		// TODO Auto-generated method stub
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);
		
		// MENU
		JMenuBar menuBar;
		JMenu menu;
		menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);
		menu2idx = new TreeMap<String, Integer>();
		popupMenu2Parent = new HashMap<JPopupMenu, JMenu>();
		// File menu
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		mmLoad = makeMenuItem(LABEL_FILE_LOAD, null, menu);		
		mmSave = makeMenuItem(LABEL_FILE_SAVE, null, menu);		
		addToJMenuBar(menu);
		
		
		this.tffP = new JPanel(new BorderLayout()); 		// panel holding the transferFunctionPanel
		this.tffP.add(this.tfFct, BorderLayout.WEST);
//		this.getContentPane().add(tffP, BorderLayout.CENTER); // and get rid of this line
		this.getContentPane().add(this.stat, BorderLayout.CENTER);
		this.getContentPane().add(this.tfFct,BorderLayout.WEST);
//		this.add(this.tffP);
//		this.add(this.stat, BorderLayout.WEST);
		
		pack();
	}
	
	private void handleSave() {
		// TODO Auto-generated method stub
		System.out.println("Save TFFct");
		
		int ret = Start.getFileChooser().showSaveDialog(this);
		if(ret == JFileChooser.APPROVE_OPTION) {
			File chosenFile = Start.getFileChooser().getSelectedFile();
			if (confirmOverwrite(chosenFile)) {
				Writer output = null;
			    try {
					output = new BufferedWriter(new FileWriter(chosenFile)); //(new FileWriter(file));
				    // red component
					output.write("red\t"+this.tfFct.getValInputType('r')+"\t"+this.tfFct.getSlopeType('r')+"\t"
							+this.tfFct.getSteep('r')+"\t"+this.tfFct.getNVal('r')+"\t"+this.tfFct.getDefVal('r')+"\n");
					// green component
					output.write("green\t"+this.tfFct.getValInputType('g')+"\t"+this.tfFct.getSlopeType('g')+"\t"
							+this.tfFct.getSteep('g')+"\t"+this.tfFct.getNVal('g')+"\t"+this.tfFct.getDefVal('g')+"\n");
					// blue component
					output.write("blue\t"+this.tfFct.getValInputType('b')+"\t"+this.tfFct.getSlopeType('b')+"\t"
							+this.tfFct.getSteep('b')+"\t"+this.tfFct.getNVal('b')+"\t"+this.tfFct.getDefVal('b')+"\n");
					// alpha component
					output.write("alpha\t"+this.tfFct.getValInputType('a')+"\t"+this.tfFct.getSlopeType('a')+"\t"
							+this.tfFct.getSteep('a')+"\t"+this.tfFct.getNVal('a')+"\t"+this.tfFct.getDefVal('a'));
//					output.write("any text \n");
//				    output.write("\t hello");
				    output.close();
					System.out.println("File " + chosenFile.getPath() + " saved.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("Error while trying to write transfer function to txt file " + chosenFile.getPath());			
					e.printStackTrace();
				}
			}
		}
	}

	private void handleLoad() {
		// TODO Auto-generated method stub
		String filename = openTXTFile();
		System.out.print("Load TFFct: "+filename);
		
		File file = new File(filename);
		BufferedReader reader = null;
		
		try	{
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while ((line=reader.readLine())!=null){
				String lineArray[] = line.split("\t");
				String colC = lineArray[0];
				String inputT = lineArray[1];
				String slopeT = lineArray[2];
				boolean steep = Boolean.valueOf(lineArray[3]);
				int n = Integer.valueOf(lineArray[4]);
				int def = Integer.valueOf(lineArray[5]);
				
				char col = colC.charAt(0); // = ' ';
				if (colC=="red"){
					col = 'r';
				}
				else if (colC=="green"){
					col = 'g';
				}
				else if (colC=="blue"){
					col = 'b';
				}
				else if (colC=="alpha"){
					col = 'a';
				}
				this.tfFct.setValInputType(col, inputT);
				this.tfFct.setSlopeType(col, slopeT);
				this.tfFct.setSteep(col, steep);
				this.tfFct.setNVal(col, n);
				this.tfFct.setDefVal(col, def);
				System.out.print("successful \n");
			}
		}
		catch (FileNotFoundException e){
			System.out.print("failed \n");
			e.printStackTrace();
		}
		catch (IOException e){
			System.out.print("failed \n");
			e.printStackTrace();
		}
		
	}
	
	private String openTXTFile() {
		String sFileName = null;
		final JFileChooser chooser = new JFileChooser("Verzeichnis wählen");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileFilter filter = new FileNameExtensionFilter("TXT file", "txt");
        chooser.setFileFilter(filter);

        chooser.setVisible(true);
        final int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File inputVerzFile = chooser.getSelectedFile();
            sFileName = inputVerzFile.getPath();
        }
//        System.out.println("Abbruch");
        chooser.setVisible(false); 
        return sFileName;
	}
	
	/**
	 * Checks for existence of given file and displays a confirm dialog
	 * @param file
	 * @return true if file does not exist or user clicks on Yes, false if file
	 *  exists and user clicks on No
	 */
	private boolean confirmOverwrite(File file) {
		if(file.exists()) {
			String message = "File " + file.getAbsolutePath() + " already exists. Overwrite?";
			int ret = JOptionPane.showConfirmDialog(this,message, "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(ret != JOptionPane.YES_OPTION) {
				return false;
			} else {
				return true;
			}
		} 
		return true;
	}

}
