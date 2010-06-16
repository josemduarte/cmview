package cmview.gmbp;

public class Geodesic {
	
	// private data
	  static final int    MAX = 8; // max. order of evaluation
	  static final int    MAXITER = 15; // max. number of iterations
	  static final double RHODEG = 180./Math.PI;
	  private Ellipsoid   _ell = Ellipsoid.BESSEL; // ellipsoid in use
	  private double      _h; // parameter of GL
	  private double      _k2[],_k4[]; // vectors of coefficients
	  private int         _n; // order of series expansion
	
	
	  /**
	  compute GL from two different points on GL.
	  @param ell the reference ellipsoid
	  @param phi1rad ellipsoidal latitude (in [rad]) of first point
	  @param phi2rad ellipsoidal latitude (in [rad]) of second point
	  @param dlam12rad difference in ellipsoidal longitudes of the two points
	  (lam2 - lam1 in [rad])
	  */
	  public Geodesic(Ellipsoid ell, double phi1rad, double phi2rad, double dlam12rad){
		  // throws Msg {
	    _ell = ell;
	    _n = MAX;
	    _k2 = new double[order()];
	    _k4 = new double[order()];
	    _h = 0.0;
	    double h_old = 1.E30;
	    double beta1 = _ell.phi2beta(phi1rad);
	    double beta2 = _ell.phi2beta(phi2rad);
	    int iter = 0;
	    while(++iter < MAXITER && Math.abs(h()-h_old) > 1.E-15) {
	      koeff();
	      double v1 = beta2v(beta1);
	      double v2 = beta2v(beta2);
	      double dLam = dlam12rad - h() * (koeff3()*(v2-v1)
	         - 0.5 * Math.sin(2.*v2)*koeff4(v2)+
	           0.5 * Math.sin(2.*v1)*koeff4(v1));
	      h_old = h();
	      _h = 1./Math.sqrt(1.+Math.tan(beta1)*Math.tan(beta1)+
	          (Math.pow(Math.tan(beta2)-Math.tan(beta1)*Math.cos(dLam),2) /
	          Math.sin(dLam)/Math.sin(dLam)));
	    }
	    if(iter == MAXITER)
	    	System.out.println("lat could not converge, check input data");
//	       throw new Msg("lat","could not converge, check input data");
	    koeff();
	  }
	  
	  public int order() { return(_n); }
	  public double h() { return(_h); }
	  public double betamax() { return(Math.acos(h())); }
	  
	  /**
		 converts reduced latitude to latitude v.
		 (maps latitude range [-betamax..+betamax] to [-pi/2..+pi/2])
		 @param beta reduced latitude in radians
		 @return the mapping latitude v in radians
		 */		  
	  public double beta2v(double beta){
          if(beta > betamax() || beta < -betamax())
        	  System.out.println("beta2v "+" invalid beta="+beta+" rad"+" betaMax="+betamax());
//                  throw new Msg("beta2v","invalid beta="+beta+" rad");
          return(Math.asin(Math.sin(beta)/Math.sqrt(1.-h()*h())));
	  }
	  
	  // here the coefficients are computed, private method
	  private void koeff() {
		  double h2 = 1.-h()*h();
	      for(int i=0;i<order();i++) _k2[i] = _k4[i] = 0.0;
	      _k4[order()-1] = 1.0;
	      double p[] = new double[order()+2];
	      double c = 1.0;
	      for(int i=0;i<=order()+1;i++) p[i] = 0.0;
	      if(!_ell.isSphere()) for(int n=1;n<=order();n++) {
	          c *= (2.*n-3.)/2./n*_ell.e2();
	          double c1 = c*(2.*n-1.)/(2.*n+2.)*_ell.e2();
	          _k4[order()-1] += c;
	          double s = 1, t = p[1];
	          p[0] = 1.;
	          double d = 1.0;
  	          if(0.0 != h2) for(int i=1;i<=n;i++) {
	              d *= (1.-2.*i)/2./i*h2;
	              p[i] = s+t; s = t; t = p[i+1];
	              for(int j=0;j<i;j++) {
	            	  _k2[j] += d * p[i] * c;
	                  if(n != order()) _k4[j] += d * p[i] * c1;
	              }
	          } // end for(i)
	      } // end for(n)
	  } // end koeff()
	  
	  private double k24(int ID, double v) {
	      double sin2 = Math.sin(v)*Math.sin(v);
  	      double result = (ID == 2 ? _k2[0] : _k4[0]);
	      double k = 1.0;
	      int nmax = (ID == 2 ? order() : order()-1);
	      for(int n=1;n<nmax;n++) {
	    	  k *= (2.*n)/(2.*n+1.)*sin2;
	          result += k * (ID == 2 ? _k2[n] : _k4[n]);
	      }
	      return(result);
	  }
	  public double koeff1() { 
		  return(_k2[0]+_k4[order()-1]); 
	  }
	  public double koeff2(double v) {
	      return(k24(2,v));
	  }
	  public double koeff3() { 
		  return(_k4[0]+_k4[order()-1]-1.); 
	  }
	  public double koeff4(double v) {
	      return(k24(4,v));
	  }
	  
	  /**
		computes GL-longitude (capital Lambda)
		for a point on GL with latitude phi (in [rad]).
		@param phirad latitude in [rad]
		*/
	  public double Lambda(double phirad){
		  double beta = _ell.phi2beta(phirad);
		  double v = beta2v(beta);
		  return Math.asin(h()/Math.cos(beta)*Math.sin(v)) +
		        h() * v * koeff3() - h()/2. * Math.sin(2.*v)*koeff4(v);
	  }

	  public double arc(double phirad){
	    double v = phi2v(phirad);
	    return(_ell.a()*(v*koeff1() - 0.5*Math.sin(2.*v)*koeff2(v)));
	  }
	  
	  public double phi2v(double phirad){
		    return(beta2v(_ell.phi2beta(phirad)));
	  }
	  
//	/**
//	 * @param args
//	 */
//	  public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		// new test
//		    System.out.println("Test arc");
//		    double phi1rad = 0.5;
//		    double phi2rad = 1.5;
//		    double lam1rad = 0.0;
//		    double lam2rad = 0.5;
//		    double dlam12rad = lam2rad-lam1rad;
//		    Ellipsoid ell = Ellipsoid.BESSEL;
//		    Geodesic gTest = new Geodesic(ell, phi1rad, phi2rad, dlam12rad);
//		    int steps = 10;
//		    double stepSize = (phi2rad-phi1rad)/steps;
//		    for (int i=0; i<=steps; i++){
//		    	double phirad = phi1rad+ (i*stepSize);
//		    	double lamRad = gTest.Lambda(phirad);
//		    	System.out.println(phirad +" -> "+lamRad);
//		    }
//		    System.out.println();
//	  }
}
