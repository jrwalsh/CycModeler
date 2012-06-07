package cycModeler;

import edu.iastate.javacyco.JavacycConnection;

/**
 * Main class for the CycModeler class.
 * 
 * @author Jesse Walsh
 *
 */

public class Main {
	static public String connectionStringLocal =  "jrwalsh.student.iastate.edu";
	static public String connectionStringEcoServer =  "ecoserver.vrac.iastate.edu";
	static public String connectionStringTHTServer =  "tht.vrac.iastate.edu";
	static public String organismStringK12 =  "ECOLI"; //Built-in K12 model
	static public String organismStringCBIRC =  "CBIRC"; //CBiRC E. coli model
	static public int defaultPort =  4444;
	
	static private JavacycConnection conn = null;
	
	static {
		/**
	     * The following static block is needed in order to load the
	     * the libSBML Java module when the application starts.
	     */
	    String varname;
	    String shlibname;
	
	    if (System.getProperty("mrj.version") != null) {
	      varname = "DYLD_LIBRARY_PATH";    // We're on a Mac.
	      shlibname = "libsbmlj.jnilib and/or libsbml.dylib";
	    }
	    else {
	      varname = "LD_LIBRARY_PATH";      // We're not on a Mac.
	      shlibname = "libsbmlj.so and/or libsbml.so";
	    }
	
	    try {
	      System.loadLibrary("sbmlj");
	      // For extra safety, check that the jar file is in the classpath.
	      Class.forName("org.sbml.libsbml.libsbml");
	    }
	    catch (UnsatisfiedLinkError e) {
	      System.err.println("Error encountered while attempting to load libSBML:");
	      e.printStackTrace();
	      System.err.println("Please check the value of your " + varname +
				 " environment variable and/or" +
	                         " your 'java.library.path' system property" +
	                         " (depending on which one you are using) to" +
	                         " make sure it lists all the directories needed to" +
	                         " find the " + shlibname + " library file and the" +
	                         " libraries it depends upon (e.g., the XML parser).");
	      System.exit(1);
	    }
	    catch (ClassNotFoundException e) {
	      e.printStackTrace();
	      System.err.println("Error: unable to load the file libsbmlj.jar." +
	                         " It is likely your -classpath option and/or" +
	                         " CLASSPATH environment variable do not" +
	                         " include the path to the file libsbmlj.jar.");
	      System.exit(1);
	    }
	    catch (SecurityException e) {
	      System.err.println("Error encountered while attempting to load libSBML:");
	      e.printStackTrace();
	      System.err.println("Could not load the libSBML library files due to a"+
	                         " security exception.\n");
	      System.exit(1);
	    }
	}
	
	/**
	 * Main method for the CycModeler class.  This method initializes a connection object and calls the run() method.
	 * 
	 * @param args Not used
	 */
	public static void main(String[] args) {
		if(args.length<1) {
			System.out.println("Usage: Main CONFIGFILE");
			System.exit(0);
		}
		String configFile = args[0];
		
		Long start = System.currentTimeMillis();
		conn = new JavacycConnection(connectionStringLocal,defaultPort);
		conn.selectOrganism(organismStringK12);
		run(configFile);
		Long stop = System.currentTimeMillis();
		Long runtime = (stop - start) / 1000;
		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	/**
	 * This method initializes a CycModeler object and calls its methods.
	 */
	public static void run(String configFile) {
		CycModeler modeler = new CycModeler(conn, configFile);
		modeler.createGenomeScaleModelFromEcoCyc();
	}
}
