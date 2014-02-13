package cmview.gmbp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
//import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;
import java.util.ArrayList;
//import java.util.Dictionary;
//import java.util.Enumeration;
//import java.util.Hashtable;
import java.util.Iterator;

import javax.accessibility.Accessible;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
//import javax.swing.JLabel;
//import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
//import javax.swing.plaf.UIResource;

/**
 * <p>Implements a Swing-based Range slider, which allows the user to enter a 
 * range (minimum and maximum) value.</p>
 */
public class JRangeSlider extends JComponent implements MouseListener, MouseMotionListener, KeyListener, SwingConstants, Accessible {   
	
	private static final long serialVersionUID = 1L;
	final public static int VERTICAL = 0;
	final public static int HORIZONTAL = 1;
	final public static int LEFTRIGHT_TOPBOTTOM = 0;
	final public static int RIGHTLEFT_BOTTOMTOP = 1;
	
	final public static int PREFERRED_BREADTH = 16;
	final public static int PREFERRED_LENGTH = 300;
	final protected static int ARROW_SZ = 16;
	final protected static int ARROW_WIDTH = 8;
	final protected static int ARROW_HEIGHT = 4;
	
	protected BoundedRangeModel model;
	protected int orientation;
	protected int direction;
	protected boolean empty;
	protected int increment = 1;
	protected int minExtent = 0; // min extent, in pixels
	
	protected ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();
	protected ChangeEvent changeEvent = null;
	protected ChangeListener lstnr;
	
	protected Color thumbColor = new Color(150,180,220);
	
//	// by Corinna
//	/**
//     * The number of values between the major tick marks -- the 
//     * larger marks that break up the minor tick marks.
//     */
//    protected int majorTickSpacing;
//
//    /**
//     * The number of values between the minor tick marks -- the 
//     * smaller marks that occur between the major tick marks.
//     * @see #setMinorTickSpacing
//     */
//    protected int minorTickSpacing;
//
//    /**
//     * If true, the knob (and the data value it represents) 
//     * resolve to the closest tick mark next to where the user
//     * positioned the knob.  The default is false.
//     * @see #setSnapToTicks
//     */
//    protected boolean snapToTicks = false;
//    
//    /**
//     * If true, the knob (and the data value it represents) 
//     * resolve to the closest slider value next to where the user
//     * positioned the knob.
//     */
//    boolean snapToValue = true;
//    
//	/**
//     * {@code Dictionary} of what labels to draw at which values
//     */
//    private Dictionary labelTable;
//    
//    private boolean paintLabels = false;
//    private boolean paintTicks = false;
//	//end by Corinna
	
	// ------------------------------------------------------------------------
	
	/** 
	 * Create a new range slider. 
	 *
	 * @param minimum - the minimum value of the range.
	 * @param maximum - the maximum value of the range.
	 * @param lowValue - the current low value shown by the range slider's bar.
	 * @param highValue - the current high value shown by the range slider's bar.
	 * @param orientation - construct a horizontal or vertical slider?
	 */
	public JRangeSlider(int minimum, int maximum, int lowValue, int highValue, int orientation) {
	    this(new DefaultBoundedRangeModel(lowValue, highValue - lowValue, minimum, maximum),
	            orientation,LEFTRIGHT_TOPBOTTOM);
	}
	
	/** 
	 * Create a new range slider. 
	 *
	 * @param minimum - the minimum value of the range.
	 * @param maximum - the maximum value of the range.
	 * @param lowValue - the current low value shown by the range slider's bar.
	 * @param highValue - the current high value shown by the range slider's bar.
	 * @param orientation - construct a horizontal or vertical slider?
	 * @param direction - Is the slider left-to-right/top-to-bottom or right-to-left/bottom-to-top
	 */
	public JRangeSlider(int minimum, int maximum, int lowValue, int highValue, int orientation, int direction) {
	    this(new DefaultBoundedRangeModel(lowValue, highValue - lowValue, minimum, maximum), 
	            orientation, direction);
	}
	
	/** 
	 * Create a new range slider. 
	 *
	 * @param model - a BoundedRangeModel specifying the slider's range
	 * @param orientation - construct a horizontal or vertical slider?
	 * @param direction - Is the slider left-to-right/top-to-bottom or right-to-left/bottom-to-top
	 */
	public JRangeSlider(BoundedRangeModel model, int orientation, int direction) {
	    super.setFocusable(true);
	    this.model = model;
	    this.orientation = orientation;     
	    this.direction = direction;
	    
	    setForeground(Color.LIGHT_GRAY);
	    
	    this.lstnr = createListener();
	    model.addChangeListener(lstnr);
	    
	    addMouseListener(this);
	    addMouseMotionListener(this);
	    addKeyListener(this);
	}
	
	/**
	 * Create a listener to relay change events from the bounded range model.
	 * @return a ChangeListener to relay events from the range model
	 */
	protected ChangeListener createListener() {
	    return new RangeSliderChangeListener();
	}
	
	/**
	 * Listener that fires a change event when it receives  change event from
	 * the slider list model.
	 */
	protected class RangeSliderChangeListener implements ChangeListener {
	    public void stateChanged(ChangeEvent e) {
	        fireChangeEvent();
	    }
	}
	
	/** 
	 * Returns the current "low" value shown by the range slider's bar. The low
	 * value meets the constraint minimum <= lowValue <= highValue <= maximum. 
	 */
	public int getLowValue() {
	    return model.getValue();
	}
	
	/** 
	 * Sets the low value shown by this range slider. This causes the range slider to be
	 * repainted and a ChangeEvent to be fired.
	 * @param lowValue the low value to use
	 */
	public void setLowValue(int lowValue) {
	    int e = (model.getValue()-lowValue)+model.getExtent();
	    model.setRangeProperties(lowValue, e,
	        model.getMinimum(), model.getMaximum(), false);
	    model.setValue(lowValue);
	}
	
	/** 
	 * Returns the current "high" value shown by the range slider's bar. The high
	 * value meets the constraint minimum <= lowValue <= highValue <= maximum. 
	 */
	public int getHighValue() {
	    return model.getValue()+model.getExtent();
	}
	
	/** 
	 * Sets the high value shown by this range slider. This causes the range slider to be
	 * repainted and a ChangeEvent to be fired.
	 * @param highValue the high value to use
	 */
	public void setHighValue(int highValue) {
	    model.setExtent(highValue-model.getValue());
	}
	
	/**
	 * Set the slider range span.
	 * @param lowValue the low value of the slider range
	 * @param highValue the high value of the slider range
	 */
	public void setRange(int lowValue, int highValue) {
	    model.setRangeProperties(lowValue, highValue-lowValue,
	            model.getMinimum(), model.getMaximum(), false);
	}
	
	/**
	 * Gets the minimum possible value for either the low value or the high value.
	 * @return the minimum possible range value
	 */
	public int getMinimum() {
	    return model.getMinimum();
	}
	
	/**
	 * Sets the minimum possible value for either the low value or the high value.
	 * @param minimum the minimum possible range value
	 */
	public void setMinimum(int minimum) {
	    model.setMinimum(minimum);
	}
	
	/**
	 * Gets the maximum possible value for either the low value or the high value.
	 * @return the maximum possible range value
	 */
	public int getMaximum() {
	    return model.getMaximum();
	}
	
	/**
	 * Sets the maximum possible value for either the low value or the high value.
	 * @param maximum the maximum possible range value
	 */
	public void setMaximum(int maximum) {
	    model.setMaximum(maximum);
	}
	
	/**
	 * Sets the minimum extent (difference between low and high values).
	 * This method <strong>does not</strong> change the current state of the
	 * model, but can affect all subsequent interaction.
	 * @param minExtent the minimum extent allowed in subsequent interaction
	 */
	public void setMinExtent(int minExtent) {
	    this.minExtent = minExtent;
	}
	
	/**
	 * Sets whether this slider is empty.
	 * @param empty true if set to empty, false otherwise
	 */
	public void setEmpty(boolean empty) {
	    this.empty = empty;
	    repaint();
	}
	
	/**
	 * Get the slider thumb color. This is the part of the slider between
	 * the range resize buttons.
	 * @return the slider thumb color
	 */
	public Color getThumbColor() {
	    return thumbColor;
	}
	
	/**
	 * Set the slider thumb color. This is the part of the slider between
	 * the range resize buttons.
	 * @param thumbColor the slider thumb color
	 */
	public void setThumbColor(Color thumbColor) {
	    this.thumbColor = thumbColor;
	}
	
	/**
	 * Get the BoundedRangeModel backing this slider.
	 * @return the slider's range model
	 */
	public BoundedRangeModel getModel() {
	    return model;
	}
	
	/**
	 * Set the BoundedRangeModel backing this slider.
	 * @param brm the slider range model to use
	 */
	public void setModel(BoundedRangeModel brm) {
	    model.removeChangeListener(lstnr);
	    model = brm;
	    model.addChangeListener(lstnr);
	    repaint();
	}
	
	/** 
	 * Registers a listener for ChangeEvents.
	 * @param cl the ChangeListener to add
	 */
	public void addChangeListener(ChangeListener cl) {
	    if ( !listeners.contains(cl) )
	        listeners.add(cl);
	}
	
	/** 
	 * Removes a listener for ChangeEvents.
	 * @param cl the ChangeListener to remove
	 */
	public void removeChangeListener(ChangeListener cl) {
	    listeners.remove(cl);
	}
	
	/**
	 * Fire a change event to all listeners.
	 */
	protected void fireChangeEvent() {
	    repaint();
	    if ( changeEvent == null )
	        changeEvent = new ChangeEvent(this);
	    Iterator<ChangeListener> iter = listeners.iterator();
	    while ( iter.hasNext() )
	        ((ChangeListener)iter.next()).stateChanged(changeEvent);
	}
	
	/**
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize() {
	    if (orientation == VERTICAL) {
	        return new Dimension(PREFERRED_BREADTH, PREFERRED_LENGTH);
	    }
	    else {
	        return new Dimension(PREFERRED_LENGTH, PREFERRED_BREADTH);
	    }
	}
	
	
//	//---------- by Corinna
//    /**
//     * This method returns the major tick spacing.  The number that is returned
//     * represents the distance, measured in values, between each major tick mark.
//     * If you have a slider with a range from 0 to 50 and the major tick spacing
//     * is set to 10, you will get major ticks next to the following values:
//     * 0, 10, 20, 30, 40, 50.
//     *
//     * @return the number of values between major ticks
//     * @see #setMajorTickSpacing
//     */
//    public int getMajorTickSpacing() { 
//        return majorTickSpacing; 
//    }
//
//
//    /**
//     * This method sets the major tick spacing.  The number that is passed in
//     * represents the distance, measured in values, between each major tick mark.
//     * If you have a slider with a range from 0 to 50 and the major tick spacing
//     * is set to 10, you will get major ticks next to the following values:
//     * 0, 10, 20, 30, 40, 50.
//     * <p>
//     * In order for major ticks to be painted, {@code setPaintTicks} must be
//     * set to {@code true}.
//     * <p>
//     * This method will also set up a label table for you.
//     * If there is not already a label table, and the major tick spacing is
//     * {@code > 0}, and {@code getPaintLabels} returns
//     * {@code true}, a standard label table will be generated (by calling
//     * {@code createStandardLabels}) with labels at the major tick marks.
//     * For the example above, you would get text labels: "0",
//     * "10", "20", "30", "40", "50".
//     * The label table is then set on the slider by calling
//     * {@code setLabelTable}.
//     *
//     * @param  n  new value for the {@code majorTickSpacing} property
//     * @see #getMajorTickSpacing
//     * @see #setPaintTicks
//     * @see #setLabelTable
//     * @see #createStandardLabels(int)
//     * @beaninfo
//     *        bound: true
//     *    attribute: visualUpdate true
//     *  description: Sets the number of values between major tick marks.
//     * 
//     */
//    public void setMajorTickSpacing(int n) {
//        int oldValue = majorTickSpacing;
//        majorTickSpacing = n;
//        if ( labelTable == null && getMajorTickSpacing() > 0 && getPaintLabels() ) {
//            setLabelTable( createStandardLabels( getMajorTickSpacing() ) );
//        }
//        firePropertyChange("majorTickSpacing", oldValue, majorTickSpacing);
//        if (majorTickSpacing != oldValue && getPaintTicks()) {
//            repaint();
//        }
//    }
//
//
//
//    /**
//     * This method returns the minor tick spacing.  The number that is returned
//     * represents the distance, measured in values, between each minor tick mark.
//     * If you have a slider with a range from 0 to 50 and the minor tick spacing
//     * is set to 10, you will get minor ticks next to the following values:
//     * 0, 10, 20, 30, 40, 50.
//     *
//     * @return the number of values between minor ticks
//     * @see #getMinorTickSpacing
//     */
//    public int getMinorTickSpacing() { 
//        return minorTickSpacing; 
//    }
//
//
//    /**
//     * This method sets the minor tick spacing.  The number that is passed in
//     * represents the distance, measured in values, between each minor tick mark.
//     * If you have a slider with a range from 0 to 50 and the minor tick spacing
//     * is set to 10, you will get minor ticks next to the following values:
//     * 0, 10, 20, 30, 40, 50.
//     * <p>
//     * In order for minor ticks to be painted, {@code setPaintTicks} must be
//     * set to {@code true}.
//     *
//     * @param  n  new value for the {@code minorTickSpacing} property
//     * @see #getMinorTickSpacing
//     * @see #setPaintTicks
//     * @beaninfo
//     *        bound: true
//     *    attribute: visualUpdate true
//     *  description: Sets the number of values between minor tick marks.
//     */
//    public void setMinorTickSpacing(int n) { 
//        int oldValue = minorTickSpacing;
//        minorTickSpacing = n; 
//        firePropertyChange("minorTickSpacing", oldValue, minorTickSpacing);
//        if (minorTickSpacing != oldValue && getPaintTicks()) {
//            repaint();
//        }
//    }
//
//
//    /**
//     * Returns true if the knob (and the data value it represents) 
//     * resolve to the closest tick mark next to where the user
//     * positioned the knob.
//     *
//     * @return true if the value snaps to the nearest tick mark, else false
//     * @see #setSnapToTicks
//     */
//    public boolean getSnapToTicks() { 
//        return snapToTicks; 
//    }
//
//
//    /**
//     * Returns true if the knob (and the data value it represents) 
//     * resolve to the closest slider value next to where the user
//     * positioned the knob.
//     *
//     * @return true if the value snaps to the nearest slider value, else false
//     * @see #setSnapToValue
//     */
//    boolean getSnapToValue() { 
//        return snapToValue; 
//    }
//    
//    /**
//     * Tells if labels are to be painted.
//     * @return true if labels are painted, else false
//     * @see #setPaintLabels
//     */
//    public boolean getPaintLabels() { 
//        return paintLabels; 
//    }
//    
//	/**
//     * {@inheritDoc}
//     *
//     * @since 1.6
//     */
//    public void setFont(Font font) {
//        super.setFont(font);
//        updateLabelSizes();
//    }
//    
//	/**
//     * Returns the dictionary of what labels to draw at which values.
//     *
//     * @return the <code>Dictionary</code> containing labels and
//     *    where to draw them
//     */
//    public Dictionary getLabelTable() {
///*
//        if ( labelTable == null && getMajorTickSpacing() > 0 ) {
//            setLabelTable( createStandardLabels( getMajorTickSpacing() ) );
//        }
//*/
//        return labelTable;
//    }
//
//	/**
//     * Used to specify what label will be drawn at any given value.
//     * The key-value pairs are of this format:
//     * <code>{ Integer value, java.swing.JComponent label }</code>.
//     * <p>
//     * An easy way to generate a standard table of value labels is by using the
//     * {@code createStandardLabels} method.
//     * <p>
//     * Once the labels have been set, this method calls {@link #updateLabelUIs}.
//     * Note that the labels are only painted if the {@code paintLabels}
//     * property is {@code true}.
//     *
//     * @param labels new {@code Dictionary} of labels, or {@code null} to
//     *        remove all labels
//     * @see #createStandardLabels(int)
//     * @see #getLabelTable
//     * @see #setPaintLabels
//     * @beaninfo
//     *       hidden: true
//     *        bound: true
//     *    attribute: visualUpdate true
//     *  description: Specifies what labels will be drawn for any given value.
//     */
//    public void setLabelTable( Dictionary labels ) {
//        Dictionary oldTable = labelTable;
//        labelTable = labels;
//        updateLabelUIs();
//        firePropertyChange("labelTable", oldTable, labelTable );
//        if (labels != oldTable) {
//            revalidate();
//            repaint();
//        }
//    }
//    
//    /**
//     * Updates the UIs for the labels in the label table by calling
//     * {@code updateUI} on each label.  The UIs are updated from
//     * the current look and feel.  The labels are also set to their
//     * preferred size.
//     *
//     * @see #setLabelTable
//     * @see JComponent#updateUI
//     */
//    protected void updateLabelUIs() {
//        if ( getLabelTable() == null ) {
//            return;
//        }
//        Enumeration labels = getLabelTable().keys();
//        while ( labels.hasMoreElements() ) {
//            Object value = getLabelTable().get( labels.nextElement() );
//            if ( value instanceof JComponent ) {
//                JComponent component = (JComponent)value;
//                component.updateUI();
//                component.setSize( component.getPreferredSize()  );
//            }
//        }
//    }
//    private void updateLabelSizes() {
//        Dictionary labelTable = getLabelTable();
//        if (labelTable != null) {
//            Enumeration labels = labelTable.elements();
//            while (labels.hasMoreElements()) {
//                Object value = labels.nextElement();
//                if (value instanceof JComponent) {
//                    JComponent component = (JComponent)value;
//                    component.setSize(component.getPreferredSize());
//                }
//            }
//        }
//    }
//    
//    /**
//     * Determines whether labels are painted on the slider.
//     * <p>
//     * This method will also set up a label table for you.
//     * If there is not already a label table, and the major tick spacing is
//     * {@code > 0},
//     * a standard label table will be generated (by calling
//     * {@code createStandardLabels}) with labels at the major tick marks.
//     * The label table is then set on the slider by calling
//     * {@code setLabelTable}.
//     * <p>
//     * By default, this property is {@code false}.
//     *
//     * @param  b  whether or not to paint labels
//     * @see #getPaintLabels
//     * @see #getLabelTable
//     * @see #createStandardLabels(int)
//     * @beaninfo
//     *        bound: true
//     *    attribute: visualUpdate true
//     *  description: If true labels are painted on the slider.
//     */
//    public void setPaintLabels(boolean b) {
//        boolean oldValue = paintLabels;
//        paintLabels = b;
////        if ( labelTable == null && getMajorTickSpacing() > 0 ) {
////            setLabelTable( createStandardLabels( getMajorTickSpacing() ) );
////        }
//        firePropertyChange("paintLabels", oldValue, paintLabels);
//        if (paintLabels != oldValue) {
//            revalidate();
//            repaint();
//        }
//    } 
//    
//    /**
//     * Tells if tick marks are to be painted.
//     * @return true if tick marks are painted, else false
//     * @see #setPaintTicks
//     */
//    public boolean getPaintTicks() { 
//        return paintTicks; 
//    }
//
//
//    /**
//     * Determines whether tick marks are painted on the slider.
//     * By default, this property is {@code false}.
//     *
//     * @param  b  whether or not tick marks should be painted
//     * @see #getPaintTicks
//     * @beaninfo
//     *        bound: true
//     *    attribute: visualUpdate true
//     *  description: If true tick marks are painted on the slider.
//     */
//    public void setPaintTicks(boolean b) { 
//        boolean oldValue = paintTicks;
//        paintTicks = b;
//        firePropertyChange("paintTicks", oldValue, paintTicks);
//        if (paintTicks != oldValue) {
//            revalidate();
//            repaint();
//        }
//    }
//    
//    /**
//     * Creates a {@code Hashtable} of numerical text labels, starting at the
//     * slider minimum, and using the increment specified.
//     * For example, if you call <code>createStandardLabels( 10 )</code>
//     * and the slider minimum is zero,
//     * then labels will be created for the values 0, 10, 20, 30, and so on.
//     * <p>
//     * For the labels to be drawn on the slider, the returned {@code Hashtable}
//     * must be passed into {@code setLabelTable}, and {@code setPaintLabels}
//     * must be set to {@code true}.
//     * <p>
//     * For further details on the makeup of the returned {@code Hashtable}, see
//     * the {@code setLabelTable} documentation.
//     *
//     * @param  increment  distance between labels in the generated hashtable
//     * @return a new {@code Hashtable} of labels
//     * @see #setLabelTable
//     * @see #setPaintLabels
//     * @throws IllegalArgumentException if {@code increment} is less than or
//     *		equal to zero
//     */
//    public Hashtable createStandardLabels( int increment ) {
//        return createStandardLabels( increment, getMinimum() );
//    }
//    
//    /**
//     * Creates a {@code Hashtable} of numerical text labels, starting at the
//     * starting point specified, and using the increment specified.
//     * For example, if you call
//     * <code>createStandardLabels( 10, 2 )</code>,
//     * then labels will be created for the values 2, 12, 22, 32, and so on.
//     * <p>
//     * For the labels to be drawn on the slider, the returned {@code Hashtable}
//     * must be passed into {@code setLabelTable}, and {@code setPaintLabels}
//     * must be set to {@code true}.
//     * <p>
//     * For further details on the makeup of the returned {@code Hashtable}, see
//     * the {@code setLabelTable} documentation.
//     *
//     * @param  increment  distance between labels in the generated hashtable
//     * @param  start      value at which the labels will begin
//     * @return a new {@code Hashtable} of labels
//     * @see #setLabelTable
//     * @see #setPaintLabels
//     * @exception IllegalArgumentException if {@code start} is
//     * 		out of range, or if {@code increment} is less than or equal
//     *		to zero
//     */
//    public Hashtable createStandardLabels( int increment, int start ) {
//        if ( start > getMaximum() || start < getMinimum() ) {
//            throw new IllegalArgumentException( "Slider label start point out of range." );
//        }
//
//        if ( increment <= 0 ) {
//            throw new IllegalArgumentException( "Label incremement must be > 0" );
//        }
//
//        class SmartHashtable extends Hashtable implements PropertyChangeListener {
//            int increment = 0;
//            int start = 0;
//            boolean startAtMin = false;
//
//            class LabelUIResource extends JLabel implements UIResource {
//                public LabelUIResource( String text, int alignment ) {
//                    super( text, alignment );
//                    setName("Slider.label");
//                }
//
//		public Font getFont() {
//		    Font font = super.getFont();
//		    if (font != null && !(font instanceof UIResource)) {
//			return font;
//		    }
//		    return JRangeSlider.this.getFont();
//		}
//
//		public Color getForeground() {
//		    Color fg = super.getForeground();
//		    if (fg != null && !(fg instanceof UIResource)) {
//			return fg;
//		    }
//		    if (!(JRangeSlider.this.getForeground() instanceof UIResource)) {
//			return JRangeSlider.this.getForeground();
//		    }
//		    return fg;
//		}
//            }
//
//            public SmartHashtable( int increment, int start ) {
//                super();
//                this.increment = increment;
//                this.start = start;
//                startAtMin = start == getMinimum();
//                createLabels();
//            }
//
//            public void propertyChange( PropertyChangeEvent e ) {
//                if ( e.getPropertyName().equals( "minimum" ) && startAtMin ) {
//                    start = getMinimum();
//                }
//
//                if ( e.getPropertyName().equals( "minimum" ) ||
//                     e.getPropertyName().equals( "maximum" ) ) {
//
//                    Enumeration keys = getLabelTable().keys();
//                    Object key = null;
//                    Hashtable hashtable = new Hashtable();
//
//                    // Save the labels that were added by the developer
//                    while ( keys.hasMoreElements() ) {
//                        key = keys.nextElement();
//                        Object value = getLabelTable().get( key );
//                        if ( !(value instanceof LabelUIResource) ) {
//                            hashtable.put( key, value );
//                        }
//                    }
//
//                    clear();
//                    createLabels();
//
//                    // Add the saved labels
//                    keys = hashtable.keys();
//                    while ( keys.hasMoreElements() ) {
//                        key = keys.nextElement();
//                        put( key, hashtable.get( key ) );
//                    }
//
//                    ((JSlider)e.getSource()).setLabelTable( this );
//                }
//            }
//
//            void createLabels() {
//                for ( int labelIndex = start; labelIndex <= getMaximum(); labelIndex += increment ) {
//                    put( new Integer( labelIndex ), new LabelUIResource( ""+labelIndex, JLabel.CENTER ) );
//                }
//            }
//        }
//
//        SmartHashtable table = new SmartHashtable( increment, start );
//
//        if ( getLabelTable() != null && (getLabelTable() instanceof PropertyChangeListener) ) {
//            removePropertyChangeListener( (PropertyChangeListener)getLabelTable() );
//        }
//
//        addPropertyChangeListener( table );
//
//        return table;
//    }
//	// end by Corinna
	
	// ------------------------------------------------------------------------
	// Rendering
	
	/**
	 * Override this method to perform custom painting of the slider trough.
	 * @param g a Graphics2D context for rendering
	 * @param width the width of the slider trough
	 * @param height the height of the slider trough
	 */
	protected void customPaint(Graphics2D g, int width, int height) {
	    // does nothing in this class
	    // subclasses can override to perform custom painting
	}
	
	/**
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {        
	    Rectangle bounds = getBounds();
	    int width = (int)bounds.getWidth() - 1;
	    int height = (int)bounds.getHeight() - 1;
	
	    int min = toScreen(getLowValue());
	    int max = toScreen(getHighValue());
	
	    // Paint the full slider if the slider is marked as empty
	    if (empty) {
	        if (direction == LEFTRIGHT_TOPBOTTOM) {
	            min = ARROW_SZ;
	            max = (orientation == VERTICAL) ? height-ARROW_SZ : width-ARROW_SZ;
	        }
	        else {
	            min = (orientation == VERTICAL) ? height-ARROW_SZ : width-ARROW_SZ;
	            max = ARROW_SZ;                 
	        }
	    }
	
	    Graphics2D g2 = (Graphics2D)g;
	    g2.setColor(getBackground());
	    g2.fillRect(0, 0, width, height);
	    g2.setColor(getForeground());
	    g2.drawRect(0, 0, width, height);
	
	    customPaint(g2, width, height);
	    
	    // Draw arrow and thumb backgrounds
	    g2.setStroke(new BasicStroke(1));
	    if (orientation == VERTICAL) {  
	        if (direction == LEFTRIGHT_TOPBOTTOM) {
	            g2.setColor(getForeground());
	            g2.fillRect(0, min - ARROW_SZ, width, ARROW_SZ-1);
	            paint3DRectLighting(g2,0,min-ARROW_SZ,width,ARROW_SZ-1);
	        
	            if ( thumbColor != null ) {
	                g2.setColor(thumbColor);
	                g2.fillRect(0, min, width, max - min-1);
	                paint3DRectLighting(g2,0,min,width,max-min-1);
	            }
	            
	            g2.setColor(getForeground());
	            g2.fillRect(0, max, width, ARROW_SZ-1);
	            paint3DRectLighting(g2,0,max,width,ARROW_SZ-1);
	        
	            // Draw arrows          
	            g2.setColor(Color.black);
	            paintArrow(g2, (width-ARROW_WIDTH) / 2.0, min - ARROW_SZ + (ARROW_SZ-ARROW_HEIGHT) / 2.0, ARROW_WIDTH, ARROW_HEIGHT, true);
	            paintArrow(g2, (width-ARROW_WIDTH) / 2.0, max + (ARROW_SZ-ARROW_HEIGHT) / 2.0, ARROW_WIDTH, ARROW_HEIGHT, false);
	        }
	        else {
	            g2.setColor(getForeground());
	            g2.fillRect(0, min, width, ARROW_SZ-1);
	            paint3DRectLighting(g2,0,min,width,ARROW_SZ-1);
	        
	            if ( thumbColor != null ) {
	                g2.setColor(thumbColor);
	                g2.fillRect(0, max, width, min-max-1);
	                paint3DRectLighting(g2,0,max,width,min-max-1);
	            }
	        
	            g2.setColor(getForeground());
	            g2.fillRect(0, max-ARROW_SZ, width, ARROW_SZ-1);
	            paint3DRectLighting(g2,0,max-ARROW_SZ,width,ARROW_SZ-1);
	        
	            // Draw arrows          
	            g2.setColor(Color.black);
	            paintArrow(g2, (width-ARROW_WIDTH) / 2.0, min + (ARROW_SZ-ARROW_HEIGHT) / 2.0, ARROW_WIDTH, ARROW_HEIGHT, false);
	            paintArrow(g2, (width-ARROW_WIDTH) / 2.0, max - ARROW_SZ + (ARROW_SZ-ARROW_HEIGHT) / 2.0, ARROW_WIDTH, ARROW_HEIGHT, true);             
	        }
	    }
	    else {
	        if (direction == LEFTRIGHT_TOPBOTTOM) {
	            g2.setColor(getForeground());
	            g2.fillRect(min - ARROW_SZ, 0, ARROW_SZ-1, height);
	            paint3DRectLighting(g2,min-ARROW_SZ,0,ARROW_SZ-1,height);
	        
	            if ( thumbColor != null ) {
	                g2.setColor(thumbColor);
	                g2.fillRect(min, 0, max - min - 1, height);
	                paint3DRectLighting(g2,min,0,max-min-1,height);
	            }
	
	            g2.setColor(getForeground());
	            g2.fillRect(max, 0, ARROW_SZ-1, height);
	            paint3DRectLighting(g2,max,0,ARROW_SZ-1,height);
	        
	            // Draw arrows          
	            g2.setColor(Color.black);
	            paintArrow(g2, min - ARROW_SZ + (ARROW_SZ-ARROW_HEIGHT) / 2.0, (height-ARROW_WIDTH) / 2.0, ARROW_HEIGHT, ARROW_WIDTH, true);
	            paintArrow(g2, max + (ARROW_SZ-ARROW_HEIGHT) / 2.0, (height-ARROW_WIDTH) / 2.0, ARROW_HEIGHT, ARROW_WIDTH, false);
	        }
	        else {
	            g2.setColor(getForeground());
	            g2.fillRect(min, 0, ARROW_SZ - 1, height);
	            paint3DRectLighting(g2,min,0,ARROW_SZ-1,height);
	            
	            if ( thumbColor != null ) {
	                g2.setColor(thumbColor);
	                g2.fillRect(max, 0, min - max - 1, height);
	                paint3DRectLighting(g2,max,0,min-max-1,height); 
	            }
	            
	            g2.setColor(getForeground());
	            g2.fillRect(max-ARROW_SZ, 0, ARROW_SZ-1, height);
	            paint3DRectLighting(g2,max-ARROW_SZ,0,ARROW_SZ-1,height); 
	        
	            // Draw arrows          
	            g2.setColor(Color.black);
	            paintArrow(g2, min + (ARROW_SZ-ARROW_HEIGHT) / 2.0, (height-ARROW_WIDTH) / 2.0, ARROW_HEIGHT, ARROW_WIDTH, true);
	            paintArrow(g2, max - ARROW_SZ + (ARROW_SZ-ARROW_HEIGHT) / 2.0, (height-ARROW_WIDTH) / 2.0, ARROW_HEIGHT, ARROW_WIDTH, false);                   
	        }
	    }               
	}
	
	/**
	 * This draws an arrow as a series of lines within the specified box.
	 * The last boolean specifies whether the point should be at the 
	 * right/bottom or left/top. 
	 */
	protected void paintArrow(Graphics2D g2, double x, double y, int w, int h,
	                          boolean topDown)
	{
	    int intX = (int)(x+0.5);
	    int intY = (int)(y+0.5);
	    
	    if (orientation == VERTICAL) {
	        if (w % 2 == 0) {
	            w = w - 1;
	        }
	        
	        if (topDown) {
	            for(int i=0; i<(w/2+1); i++) {
	                g2.drawLine(intX+i,intY+i,intX+w-i-1,intY+i);
	            }
	        }
	        else {
	            for(int i=0; i<(w/2+1); i++) {
	                g2.drawLine(intX+w/2-i,intY+i,intX+w-w/2+i-1,intY+i);
	            }               
	        }
	    }
	    else {
	        if (h % 2 == 0) {
	            h = h - 1;
	        }
	                    
	        if (topDown) {
	            for(int i=0; i<(h/2+1); i++) {
	                g2.drawLine(intX+i,intY+i,intX+i,intY+h-i-1);
	            }
	        }
	        else {
	            for(int i=0; i<(h/2+1); i++) {
	                g2.drawLine(intX+i,intY+h/2-i,intX+i,intY+h-h/2+i-1);
	            }               
	        }           
	    }
	}
	
	/**
	 * Adds Windows2K type 3D lighting effects
	 */
	protected void paint3DRectLighting(Graphics2D g2, int x, int y,
	                                   int width, int height)
	{
	    g2.setColor(Color.white);
	    g2.drawLine(x+1,y+1,x+1,y+height-1);
	    g2.drawLine(x+1,y+1,x+width-1,y+1);
	    g2.setColor(Color.gray);
	    g2.drawLine(x+1,y+height-1,x+width-1,y+height-1);
	    g2.drawLine(x+width-1,y+1,x+width-1,y+height-1);
	    g2.setColor(Color.darkGray);
	    g2.drawLine(x,y+height,x+width,y+height);
	    g2.drawLine(x+width,y,x+width,y+height);        
	}
	
	/**
	 * Converts from screen coordinates to a range value.
	 */
	protected int toLocal(int xOrY) {
	    Dimension sz = getSize();
	    int min = getMinimum();
	    double scale;
	    if (orientation == VERTICAL) {
	        scale = (sz.height - (2 * ARROW_SZ)) / (double) (getMaximum() - min);           
	    }
	    else {
	        scale = (sz.width - (2 * ARROW_SZ)) / (double) (getMaximum() - min);
	    }
	
	    if (direction == LEFTRIGHT_TOPBOTTOM) {
	        return (int) (((xOrY - ARROW_SZ) / scale) + min + 0.5);         
	    }
	    else {
	        if (orientation == VERTICAL) {
	            return (int) ((sz.height - xOrY - ARROW_SZ) / scale + min + 0.5);
	        }
	        else {
	            return (int) ((sz.width - xOrY - ARROW_SZ) / scale + min + 0.5);
	        }
	    }
	}
	
	/**
	 * Converts from a range value to screen coordinates.
	 */
	protected int toScreen(int xOrY) {
	    Dimension sz = getSize();
	    int min = getMinimum();
	    double scale;
	    if (orientation == VERTICAL) {
	        scale = (sz.height - (2 * ARROW_SZ)) / (double) (getMaximum() - min);           
	    }
	    else {
	        scale = (sz.width - (2 * ARROW_SZ)) / (double) (getMaximum() - min);
	    }
	
	    // If the direction is left/right_top/bottom then we subtract the min and multiply times scale
	    // Otherwise, we have to invert the number by subtracting the value from the height
	    if (direction == LEFTRIGHT_TOPBOTTOM) {
	        return (int)(ARROW_SZ + ((xOrY - min) * scale) + 0.5);
	    }
	    else {
	        if (orientation == VERTICAL) {
	            return (int)(sz.height-(xOrY - min) * scale - ARROW_SZ + 0.5);
	        }
	        else {
	            return (int)(sz.width-(xOrY - min) * scale - ARROW_SZ + 0.5);
	        }
	    }
	}
	
	/**
	 * Converts from a range value to screen coordinates.
	 */
	protected double toScreenDouble(int xOrY) {
	    Dimension sz = getSize();
	    int min = getMinimum();
	    double scale;
	    if (orientation == VERTICAL) {
	        scale = (sz.height - (2 * ARROW_SZ)) / (double) (getMaximum()+1 - min);         
	    }
	    else {
	        scale = (sz.width - (2 * ARROW_SZ)) / (double) (getMaximum()+1 - min);
	    }
	
	    // If the direction is left/right_top/bottom then we subtract the min and multiply times scale
	    // Otherwise, we have to invert the number by subtracting the value from the height
	    if (direction == LEFTRIGHT_TOPBOTTOM) {
	        return ARROW_SZ + ((xOrY - min) * scale);
	    }
	    else {
	        if (orientation == VERTICAL) {
	            return sz.height-(xOrY - min) * scale - ARROW_SZ;
	        }
	        else {
	            return sz.width-(xOrY - min) * scale - ARROW_SZ;
	        }
	    }
	}
	
	
	// ------------------------------------------------------------------------
	// Event Handling
	
	static final int PICK_NONE = 0;
	static final int PICK_LEFT_OR_TOP = 1;
	static final int PICK_THUMB = 2;
	static final int PICK_RIGHT_OR_BOTTOM = 3;
	int pick;
	int pickOffsetLow;
	int pickOffsetHigh;
	int mouse;
	
	private int pickHandle(int xOrY) {
	    int min = toScreen(getLowValue());
	    int max = toScreen(getHighValue());
	    int pick = PICK_NONE;
	    
	    if (direction == LEFTRIGHT_TOPBOTTOM) {
	        if ((xOrY > (min - ARROW_SZ)) && (xOrY < min)) {
	            pick = PICK_LEFT_OR_TOP;
	        } else if ((xOrY >= min) && (xOrY <= max)) {
	            pick = PICK_THUMB;
	        } else if ((xOrY > max) && (xOrY < (max + ARROW_SZ))) {
	            pick = PICK_RIGHT_OR_BOTTOM;
	        }
	    }
	    else {
	        if ((xOrY > min) && (xOrY < (min + ARROW_SZ))) {
	            pick = PICK_LEFT_OR_TOP;
	        } else if ((xOrY <= min) && (xOrY >= max)) {
	            pick = PICK_THUMB;
	        } else if ((xOrY > (max - ARROW_SZ) && (xOrY < max))) {
	            pick = PICK_RIGHT_OR_BOTTOM;
	        }           
	    }
	    
	    return pick;
	}
	
	private void offset(int dxOrDy) {
	    model.setValue(model.getValue()+dxOrDy);
	}
	
	/**
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {        
	    if (orientation == VERTICAL) {
	        pick = pickHandle(e.getY());
	        pickOffsetLow = e.getY() - toScreen(getLowValue());
	        pickOffsetHigh = e.getY() - toScreen(getHighValue());
	        mouse = e.getY();
	    }
	    else {
	        pick = pickHandle(e.getX());
	        pickOffsetLow = e.getX() - toScreen(getLowValue());
	        pickOffsetHigh = e.getX() - toScreen(getHighValue());
	        mouse = e.getX();           
	    }
	    repaint();
	}
	
	/**
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {
	    requestFocus();
	    int value = (orientation == VERTICAL) ? e.getY() : e.getX();
	    
	    int minimum = getMinimum();
	    int maximum = getMaximum();
	    int lowValue = getLowValue();
	    int highValue = getHighValue();
	    
	    switch (pick) {
	        case PICK_LEFT_OR_TOP:
	            int low = toLocal(value-pickOffsetLow);
	        
	            if (low < minimum) {
	                low = minimum;
	            }
	            if (low > maximum - minExtent) {
	                low = maximum - minExtent;
	            }
	            if (low > highValue-minExtent) {
	                setRange(low, low + minExtent);
	            }
	            else
	                setLowValue(low);
	            break;
	
	        case PICK_RIGHT_OR_BOTTOM:
	            int high = toLocal(value-pickOffsetHigh);
	            
	            if (high < minimum + minExtent) {
	                high = minimum + minExtent;
	            }
	            if (high > maximum) {
	                high = maximum;
	            }
	            if (high < lowValue+minExtent) {
	                setRange(high - minExtent, high);
	            }
	            else
	                setHighValue(high);
	            break;
	
	        case PICK_THUMB:
	            int dxOrDy = toLocal(value - pickOffsetLow) - lowValue;
	            if ((dxOrDy < 0) && ((lowValue + dxOrDy) < minimum)) {
	                dxOrDy = minimum - lowValue;
	            }
	            if ((dxOrDy > 0) && ((highValue + dxOrDy) > maximum)) {
	                dxOrDy = maximum - highValue;
	            }
	            if (dxOrDy != 0) {
	                offset(dxOrDy);
	            }
	            break;
	    }
	}
	
	/**
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
	    pick = PICK_NONE;
	    repaint();
	}
	
	/**
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
	    if (orientation == VERTICAL) {
	        switch (pickHandle(e.getY())) {
	            case PICK_LEFT_OR_TOP:
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	            case PICK_RIGHT_OR_BOTTOM:
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	            case PICK_THUMB:
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	            case PICK_NONE :
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	        }
	    }
	    else {
	        switch (pickHandle(e.getX())) {
	            case PICK_LEFT_OR_TOP:
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	            case PICK_RIGHT_OR_BOTTOM:
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	            case PICK_THUMB:
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	            case PICK_NONE :
	                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                break;
	        }           
	    }
	}
	
	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
	}
	/**
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {
	}
	/**
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
	}
	
	private void grow(int increment) {
	    model.setRangeProperties(model.getValue()-increment,
	        model.getExtent()+2*increment, 
	        model.getMinimum(), model.getMaximum(), false);
	}
	
	/**
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
	    int kc = e.getKeyCode();
	    boolean v = (orientation == VERTICAL);
	    boolean d = (kc == KeyEvent.VK_DOWN);
	    boolean u = (kc == KeyEvent.VK_UP);
	    boolean l = (kc == KeyEvent.VK_LEFT);
	    boolean r = (kc == KeyEvent.VK_RIGHT);
	    
	    int minimum = getMinimum();
	    int maximum = getMaximum();
	    int lowValue = getLowValue();
	    int highValue = getHighValue();
	    
	    if ( v&&r || !v&&u ) {
	        if ( lowValue-increment >= minimum &&
	             highValue+increment <= maximum ) {
	            grow(increment);
	        }
	    } else if ( v&&l || !v&&d ) { 
	        if ( highValue-lowValue >= 2*increment ) {
	            grow(-1*increment);
	        }
	    } else if ( v&&d || !v&&l ) {
	        if ( lowValue-increment >= minimum ) {
	            offset(-increment);
	        }
	    } else if ( v&&u || !v&&r ) {
	        if ( highValue+increment <= maximum ) {
	            offset(increment);
	        }
	    }
	}
	
	/**
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
	}
	/**
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
	}

} // end of class JRangeSlider

