package cycModeler;

import java.util.ArrayList;

import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.SBMLWriter;

import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

public class Test {
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
	
	public static void main(String[] args) {
		System.out.println("TESTING MODE");
		Long start = System.currentTimeMillis();
		conn = new JavacycConnection(connectionStringTHTServer,defaultPort);
		conn.selectOrganism(organismStringK12);
		test();
		Long stop = System.currentTimeMillis();
		Long runtime = (stop - start) / 1000;
		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	public static void test() {
		Diffusion diffusion = new Diffusion(conn);
		diffusion.getSmallMetabolites();
//		sbmlInteralFunctionTests(200);
//		sbmlInteralFunctionTests(250);
//		sbmlInteralFunctionTests(300);
	}
	
	// Testing
	public static void sbmlInteralFunctionTests(int mode) {
		CycModeler modeler = new CycModeler(conn);
		switch (mode) {
			case 40: {
				// Check behavior of the isReactionBalanced function
				ArrayList<String> reacs = new ArrayList<String>();
				ArrayList<String> prods =  new ArrayList<String>();
				reacs.add("GLC");
				reacs.add("GAP");
				prods.add("GAP");
				prods.add("GLC");
				System.out.println(modeler.isReactionBalanced(reacs, prods));
			} break;
			case 60: {
				// Check if a pathway contains a general term
				modeler.isGeneralizedReaction("MALATE-DEHYDROGENASE-ACCEPTOR-RXN");
				modeler.isGeneralizedReaction("SUCCINATE-DEHYDROGENASE-UBIQUINONE-RXN");
			} break;
			case 80: {
				// Look for conditions in comments
				try {
					ArrayList<Reaction> allRxns = Reaction.all(conn);
					for (Reaction r : allRxns) {
						if (r.getComment() != null && r.getComment().toLowerCase().contains("aerobic")) {
							System.out.println(r.getLocalID());
							System.out.println(r.getComment());
						}
					}
				} catch (PtoolsErrorException e) {
					e.printStackTrace();
				}
			} break;
			case 200: {
				modeler.createGenomeScaleModelFromEcoCyc();
			} break;
			case 210: {
				try {
					System.out.println(modeler.prepareGenericReaction(modeler.loadReaction("ABC-56-RXN")).size());
//					System.out.println(instantiateGenericReaction(loadReaction("RXN-11319")).size());
//					System.out.println(instantiateGenericReaction(loadReaction("RXN0-1842")).size());
//					System.out.println(instantiateGenericReaction(loadReaction("RXN0-3381")).size());
//					System.out.println(instantiateGenericReaction(loadReaction("RXN0-4261")).size());
//					System.out.println(instantiateGenericReaction(loadReaction("RXN0-4581")).size());
//					System.out.println(instantiateGenericReaction(loadReaction("RXN0-5128")).size());
				} catch (PtoolsErrorException e) {
					e.printStackTrace();
				}
			} break;
			case 220: {
				try {
					ArrayList<ReactionInstance> rxns = new ArrayList<ReactionInstance>();
					ReactionInstance rxn = new ReactionInstance(null, modeler.loadReaction("PGLUCISOM-RXN"), "NamedReaction", false, new ArrayList<MetaboliteInstance>(), new ArrayList<MetaboliteInstance>());
					rxn.reactants.add(new MetaboliteInstance(modeler.loadFrame("GLC-6-P"), modeler.defaultCompartment, 1, modeler.getChemicalFormula(modeler.loadFrame("GLC-6-P"))));
					rxn.products.add(new MetaboliteInstance(modeler.loadFrame("FRUCTOSE-6P"), modeler.defaultCompartment, 1, modeler.getChemicalFormula(modeler.loadFrame("FRUCTOSE-6P"))));
					rxns.add(rxn);
					SBMLDocument doc = modeler.createBlankSBMLDocument("Testing", 2, 1);
					doc = modeler.generateSBMLModel(doc, rxns);
					SBMLWriter writer = new SBMLWriter();
					writer.writeSBML(doc, modeler.OutputDirectory + "testing_SBML.xml");
				} catch (PtoolsErrorException e) {
					e.printStackTrace();
				}
			} break;
			case 230: {
//				readInPalssonIDMaps("/home/Jesse/Desktop/compare_palsson_ecocyc/iAF1260-ecocyc-rxn-mappings.txt");
			} break;
			case 240: {
//				verifyCompoundMappings();
			} break;
			case 250: {
//				verifyReactionMappings();
//				coreReactionTest();
			} break;
			case 300: {
				ArrayList<Reaction> l = new ArrayList<Reaction>();
				try {
//					l.add((Reaction)Reaction.load(conn, "3.1.26.5-RXN"));
					l.add((Reaction)Reaction.load(conn, "3-nucleotid-RXN"));
				} catch (PtoolsErrorException e) {
					e.printStackTrace();
				}
				modeler.generateSpecificReactionsFromGenericReactions(l);
			} break;
		}
	}
}
