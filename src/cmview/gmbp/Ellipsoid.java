package cmview.gmbp;

public class Ellipsoid {
	
	  private String _name; // name of ellipsoid
	  private double _a,_f; // semimajor axis and flattening
	  
	  /**
	  shortcuts for pre-defined ellipsoids of revolution.
	  */
	  public static enum ELL { BESSEL, HAYFORD, WGS84 }
	  private static final String classname = "Ellipsoid";
	  
	  /** parameters of Bessel ellipsoid */
	  static final public Ellipsoid BESSEL = new Ellipsoid();

	  /** parameters of Hayford ellipsoid */
	  static final public Ellipsoid HAYFORD = new Ellipsoid(ELL.HAYFORD);

	  /** parameters of WGS84 ellipsoid */
	  static final public Ellipsoid WGS84 = new Ellipsoid(ELL.WGS84);
	  
	  // ------ constructors --------
	  /**
	  default constructor - initializes the Bessel ellipsoid.
	  */
	  public Ellipsoid() {
	          this(ELL.BESSEL);
	  }
	  
	  private Ellipsoid(ELL id) {
	        try {
	                switch(id) {
	                        case HAYFORD:
	                        	init("Hayford",6378388.0,1./297.0); break;
	                        case WGS84:	                                                                
	                        	init("WGS84",6378137.0,1/298.257222); break;
	                        default:        
	                        	init("Bessel",6377397.155,1./299.15281285);
	                }
	        }
	        catch(Exception ex) {}
	  }
	  
	  /** */
	  private void init(String n, double a, double f) {
	  		//throws Msg {
		  String r = classname+".init";
		  _name = n;
		  _a = a;
		  _f = f;
		  if(null == n || n.equals(""))
			  System.out.println(r+" name must not be empty");
//	                  throw new Msg(r,"name must not be empty");
	      if(Math.abs(a-6365.E3)>5.E3)
	    	  System.out.println(r+" "+String.format("invalid semimajor axis a= %.3f m",a));
//	                  throw new Msg(r,String.format("invalid semimajor axis a= %.3f m",a));
	      if(!isSphere() && (e2() < 0.006 || e2() > 0.008))
	    	  System.out.println(r+" invalid f; 1/f="+1./f());
//	            throw new Msg(r,"invalid f; 1/f="+1./f());
	  }
	  
	  /** @return true if ellipsoid is a sphere */
	  public boolean isSphere() { return(0.0 == f()); }
	  
	  /** @return the square of the first numerical eccentricity */
	  public double e2() { return(f()*(2.-f())); }

	  /** @return the first numerical eccentricity */
	  public double e() { return(Math.sqrt(e2())); }
	  
	  /** @return the flattening of the current ellipsoid */
	  public double f()  { return(_f); }
	  
	  /** @return the semimajor axis (in meters) of the current ellipsoid */
	  public double a()  { return(_a); }
	  
	  /** @return the name of the current ellipsoid */
	  public String name() { return(_name); }
	  
	  /**
	  @param phirad the ellipsoidal latitude in radians
	  @return the reduced latitude in radians
	  */
	  public double phi2beta(double phirad) {
	      return(Math.atan(Math.tan(phirad)*(1.-f())));
	  }

}
