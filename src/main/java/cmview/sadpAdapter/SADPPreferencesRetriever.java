package cmview.sadpAdapter;

import java.util.Properties;
import java.util.TreeMap;

import owl.sadp.SADP;



/**
 * Retrieves the default parameter settings, the comments to the parameters, 
 * the respective parameter types and the domains for each parameter of class SADP.
 * */
public class SADPPreferencesRetriever {

	protected static Properties                defaultParams;
	protected static TreeMap<String, Object>   defaultParams2;
	protected static TreeMap<String, String>   comments;
	protected static TreeMap<String, Object>   types;
	protected static TreeMap<String, Object[]> domains;
	protected static boolean                   initialized  = false;
	protected static boolean                   initialized2 = false;

	public static TreeMap<String,String> getComments() {
		init();
		return new TreeMap<String, String>(comments);
	}

	public static Properties getDefaultParams() {
		init();
		return new Properties(defaultParams);
	}

	public static TreeMap<String, Object> getTypes() {
		init();
		return new TreeMap<String, Object>(types);
	}

	public static TreeMap<String, Object[]> getDomains() {
		init();
		return new TreeMap<String, Object[]>(domains);
	}

	public static TreeMap<String, String[]> getDifferentPresettings(int howMany) {
		init2();
		
		TreeMap<String,String[]> presettings = new TreeMap<String,String[]>();
		
		for( String opt : defaultParams2.keySet() ) {
			//String[] presetForOpt = new String[howMany];
			if( domains.get(opt).length == 2 ) {
				Class<?> optsClass = defaultParams2.get(opt).getClass();
				@SuppressWarnings("unused")
				Object domainSize;
				if( optsClass == Integer.class ) {
					domainSize = (Integer) domains.get(opt)[1] - (Integer) domains.get(opt)[0]; 
				} else if ( optsClass == Double.class ) {
					domainSize =  (Double) domains.get(opt)[1] - (Double) domains.get(opt)[0];
				}
			}
		}
		
		return presettings;
	}
	
	private static void init() {
		if( initialized == true ) {
			return;
		}
		// create defaultParams
		SADP sadp = new SADP();
		defaultParams = new Properties();
//		defaultParams.setProperty("eps0",sadp.getEps0().toString());
//		defaultParams.setProperty("eps1",sadp.getEps1().toString());
		defaultParams.setProperty("b0",sadp.getB0().toString());
		defaultParams.setProperty("bf",sadp.getBf().toString());
		defaultParams.setProperty("br",sadp.getBr().toString());
		defaultParams.setProperty("I0",sadp.getI0().toString());
		defaultParams.setProperty("I1",sadp.getI1().toString());

		// create comments
		comments = new TreeMap<String, String>();
//		comments.put("eps0","");
//		comments.put("eps1","");
		comments.put("b0","Initial value of annealing parameter b = 1/T, where T is the temperatur.");
		comments.put("bf","Final value of annealing parameter b = 1/T, where T is the temperatur.");
		comments.put("br","Factor by which the anealing parameter b = 1/T is increased, where T is the temperatur");
		comments.put("I0","Number of iterations of assignment loop.");
		comments.put("I1","Number of iterations of Sinkhorn loop.");

		// create types mapping
		types = new TreeMap<String, Object>();
		Double  d = new Double(0.0);
		Integer i = new Integer(0);
//		types.put("eps0", (Object) d );
//		types.put("eps1", (Object) d );
		types.put("b0",   (Object) d );
		types.put("bf",   (Object) d );
		types.put("br",   (Object) d );
		types.put("I0",   (Object) i );
		types.put("I1",   (Object) i );
		
		// create domains (as suggested in class aglappe.sadp.SADP)
		domains = new TreeMap<String, Object[]>();
		Double[]  domainB0 = new Double[2];
		domainB0[0] = 0.000001;
		domainB0[1] = 2.0;
		Double[]  domainBf = new Double[2];
		domainBf[0] = 5.000001;
		domainBf[1] = 20.0;
		Double[]  domainBr = new Double[2];
		domainBr[0] = 1.075;
		domainBr[1] = 3.0;
		Integer[] domainI0 = new Integer[2];
		domainI0[0] = 1;
		domainI0[1] = 10;
		Integer[] domainI1 = new Integer[2];
		domainI1[0] = 1;
		domainI1[1] = 30;
//		domains.put("eps0", (Object[])  domainEps0 );
//		domains.put("eps1", (Object[])  domainEps1 );
		domains.put("b0",   (Object[])  domainB0   );
		domains.put("bf",   (Object[])  domainBf   );
		domains.put("br",   (Object[])  domainBr   );
		domains.put("I0",   (Object[])  domainI0   );
		domains.put("I1",   (Object[])  domainI1   );	

		initialized = true;
	}

	private static void init2() {
		if( initialized2 ) {
			return;
		}
		
		// create default parameters
		SADP sadp = new SADP();	
		defaultParams2 = new TreeMap<String, Object>();
		defaultParams2.put("b0",sadp.getB0());
		defaultParams2.put("bf",sadp.getBf());
		defaultParams2.put("br",sadp.getBr());
		defaultParams2.put("I0",sadp.getI0());
		defaultParams2.put("I1",sadp.getI1());
		
		// create comments
		comments = new TreeMap<String, String>();
		comments.put("b0","Initial value of annealing parameter b = 1/T, where T is the temperatur.");
		comments.put("bf","Final value of annealing parameter b = 1/T, where T is the temperatur.");
		comments.put("br","Factor by which the anealing parameter b = 1/T is increased, where T is the temperatur");
		comments.put("I0","Number of iterations of assignment loop.");
		comments.put("I1","Number of iterations of Sinkhorn loop.");
		
		// create domains (as suggested in class aglappe.sadp.SADP)
		domains = new TreeMap<String, Object[]>();
		Double[]  domainB0 = new Double[2];
		domainB0[0] = 0.000001;
		domainB0[1] = 2.0;
		Double[]  domainBf = new Double[2];
		domainBf[0] = 5.000001;
		domainBf[1] = 20.0;
		Double[]  domainBr = new Double[2];
		domainBr[0] = 1.075;
		domainBr[1] = 3.0;
		Integer[] domainI0 = new Integer[2];
		domainI0[0] = 1;
		domainI0[1] = 10;
		Integer[] domainI1 = new Integer[2];
		domainI1[0] = 1;
		domainI1[1] = 30;
		domains.put("b0",(Object[]) domainB0);
		domains.put("bf",(Object[]) domainBf);
		domains.put("br",(Object[]) domainBr);
		domains.put("I0",(Object[]) domainI0);
		domains.put("I1",(Object[]) domainI1);	

		initialized2 = true;
	}		
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO: hm...
	}

}
