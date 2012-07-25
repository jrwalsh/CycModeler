package edu.iastate.cycmodeler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.SBMLWriter;

import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.model.MetaboliteInstance;
import edu.iastate.cycmodeler.model.ReactionInstance;
import edu.iastate.cycmodeler.model.ReactionNetwork;
import edu.iastate.cycmodeler.util.Globals;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

public class Test {
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
		conn = new JavacycConnection(Globals.connectionStringTHTServer,Globals.defaultPort);
		conn.selectOrganism(Globals.organismStringK12);
		test();
		Long stop = System.currentTimeMillis();
		Long runtime = (stop - start) / 1000;
		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	public static void test() {
		try {
			HashSet<MetaboliteInstance> m1 = new HashSet<MetaboliteInstance>();
			HashSet<MetaboliteInstance> m2 = new HashSet<MetaboliteInstance>();
			HashSet<MetaboliteInstance> m3 = new HashSet<MetaboliteInstance>();
			HashSet<MetaboliteInstance> m4 = new HashSet<MetaboliteInstance>();
			m1.add(new MetaboliteInstance(Frame.load(conn, "FRUCTOSE-6P"), "Here", 1));
			m2.add(new MetaboliteInstance(Frame.load(conn, "FRUCTOSE-16-DIPHOSPHATE"), "Here", 1));
			m3.add(new MetaboliteInstance(Frame.load(conn, "FRUCTOSE-6P"), "Here", 1));
			m4.add(new MetaboliteInstance(Frame.load(conn, "ADP"), "Here", 1));
			
			System.out.println(m1.equals(m3));
			System.out.println(m1.equals(m2));
			
			ReactionInstance ri = new ReactionInstance((Reaction)Frame.load(conn, "RXN0-6541"), (Reaction)Frame.load(conn, "RXN0-6541"), "Reaction 1", true, "Here", m1, m2, false);
			ReactionInstance ri2 = new ReactionInstance((Reaction)Frame.load(conn, "6PFRUCTPHOS-RXN"), (Reaction)Frame.load(conn, "RXN0-6541"), "Reaction 1", true, "Here", m3, m4, false);
			HashSet<ReactionInstance> set = new HashSet<ReactionInstance>();
			set.add(ri);
			set.add(ri2);
			System.out.println(ri.equals(ri2));
			System.out.println(set.size());
			
			
//			for (Object s : conn.callFuncArray("all-transported-chemicals :to-compartment 'CCO-EXTRACELLULAR")) {
//				System.out.println(s.toString());
//			}
			
			
//			int count = 0;
//			for (Reaction reaction : Reaction.all(conn)) {
//				for (Object s : conn.callFuncArray("compartments-of-reaction '" + reaction.getLocalID())) {
//					System.out.println(reaction.getLocalID() + " :: " + s.toString());
//				}
//				
////				if (conn.callFuncBool("rxn-in-compartment-p '" + reaction.getLocalID() + " '" + "CCO-EXTRACELLULAR")) {
////					count++;
////				}
//			}
//			System.out.println(count);
			
			
//			ReactionInstance rxnInst = new ReactionInstance((Reaction)Frame.load(conn, "PGLUCISOM-RXN"));
//			rxnInst.getCompartmentOfMetabolite();
			
			
			
			
//			Frame rxn1 = Frame.load(conn, "TRANS-RXN-94");
//			rxn1.print();
//			for (String s : conn.getAllAnnotLabels(rxn1.getLocalID(), "RXN-LOCATIONS", "CCO-PM-BAC-NEG")) {
//				System.out.println("annot: " + s + " : " + conn.getValueAnnot(rxn1.getLocalID(), "RXN-LOCATIONS", "CCO-PM-BAC-NEG", s));
//			}
			
//			Frame rxn2 = Frame.load(conn, "PGLUCISOM-RXN");
//			rxn2.print();
//			
//			Frame rxn3 = Frame.load(conn, "RXN0-1483");
//			rxn3.print();
//			
//			Frame rxn4 = Frame.load(conn, "TRANS-RXN0-455");
//			rxn4.print();
			
//			Frame rxn5 = Frame.load(conn, "RXN-2043");
//			rxn5.print();
			
		} catch (PtoolsErrorException e1) {
			e1.printStackTrace();
		}
		
		
		
		
		
//		try {
//			ArrayList<String> to = (ArrayList<String>)conn.callFuncArray("all-transported-chemicals :to-compartment '"+"CCO-PERI-BAC");
//			ArrayList<String> from = (ArrayList<String>)conn.callFuncArray("all-transported-chemicals :from-compartment '"+"CCO-PERI-BAC");
//			TreeSet<String> unique = new TreeSet<String>();
//			for (String s : to) {
//				unique.add(s);
//			}
//			for (String s : from) {
//				unique.add(s);
//			}
//			try {
//				int goodcount = 0;
//				int nullcount = 0;
//				for (String s : unique) {
//					String slot = Frame.load(conn, s).getSlotValue("MOLECULAR-WEIGHT");
//					if (slot != null && slot.contains("d")) {
//						slot = slot.substring(0, slot.indexOf("d")-1);
//					}
//					Float parse;
//					try {
//						parse = Float.parseFloat(slot);
//					} catch (Exception e) {
//						parse = (float) 800.0;
//					}
//					if (slot == null) {
//						nullcount++;
////						System.err.println(nullcount + " :: " + Frame.load(conn, s).getCommonName() + " : " + Frame.load(conn, s).getSlotValue("MOLECULAR-WEIGHT"));
//					} else if (parse < 600) {
//						goodcount++;
//						System.out.println(goodcount + " :: " + Frame.load(conn, s).getCommonName() + " : " + Frame.load(conn, s).getSlotValue("MOLECULAR-WEIGHT"));
//					}
//				}
//				System.out.println(nullcount);
//			} catch (Exception e) {
//				
//			}
//			
////			for (String s : unique) {
////				System.out.println(Frame.load(conn, s).getCommonName() + " : " + Frame.load(conn, s).getSlotValue("MOLECULAR-WEIGHT"));
////			}
////			System.out.println(to.size());
////			System.out.println(from.size());
////			System.out.println(unique.size());
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
			
//		try {
//			Frame f = Frame.load(conn, "TRANS-RXN0-234");
//			f.print();
//			Frame.load(conn, "F16ALDOLASE-RXN").print();
//			
//			System.out.println(conn.callFuncArray("compartments-of-reaction '"+"TRANS-RXN0-234"));
//			System.out.println(conn.callFuncArray("compartments-of-reaction '"+"F16ALDOLASE-RXN"));
//			
//			
//			Frame.load(conn, "RXN0-5207").print();
//			System.out.println(conn.callFuncArray("compartments-of-reaction '"+"RXN0-5207"));
//			
//			
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
		
		
//		sbmlInteralFunctionTests(200);
//		sbmlInteralFunctionTests(250);
//		sbmlInteralFunctionTests(300);
	}
	
	// Testing
	public static void sbmlInteralFunctionTests(int mode) {
		CycModeler modeler = new CycModeler(conn, "");
		switch (mode) {
			case 40: {
				// Check behavior of the isReactionBalanced function
				ArrayList<String> reacs = new ArrayList<String>();
				ArrayList<String> prods =  new ArrayList<String>();
				reacs.add("GLC");
				reacs.add("GAP");
				prods.add("GAP");
				prods.add("GLC");
//				Balance balance = new Balance(conn);
//				System.out.println(balance.isReactionBalanced(reacs, prods));
			} break;
			case 60: {
				// Check if a pathway contains a general term
//				modeler.isGeneralizedReaction("MALATE-DEHYDROGENASE-ACCEPTOR-RXN");
//				modeler.isGeneralizedReaction("SUCCINATE-DEHYDROGENASE-UBIQUINONE-RXN");
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
					Frame.load(conn, "G6P");
//					System.out.println(modeler.prepareGenericReaction(modeler.loadReaction("ABC-56-RXN")).size());
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
//			case 220: {
//				try {
//					ArrayList<ReactionInstance> rxns = new ArrayList<ReactionInstance>();
////					ReactionInstance rxn = new ReactionInstance(null, modeler.loadReaction("PGLUCISOM-RXN"), "NamedReaction", false, new ArrayList<MetaboliteInstance>(), new ArrayList<MetaboliteInstance>());
////					rxn.reactants_.add(new MetaboliteInstance(modeler.loadFrame("GLC-6-P"), modeler.DefaultCompartment, 1));
////					rxn.products_.add(new MetaboliteInstance(modeler.loadFrame("FRUCTOSE-6P"), modeler.DefaultCompartment, 1));
////					rxns.add(rxn);
//					SBMLDocument doc = modeler.createBlankSBMLDocument("Testing", 2, 1);
////					doc = modeler.generateSBMLModel(doc, rxns);
//					SBMLWriter writer = new SBMLWriter();
//					writer.writeSBML(doc, modeler.OutputDirectory + "testing_SBML.xml");
//				} catch (PtoolsErrorException e) {
//					e.printStackTrace();
//				}
//			} break;
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
//				modeler.generateSpecificReactionsFromGenericReactions(l);
			} break;
		}
	}
}