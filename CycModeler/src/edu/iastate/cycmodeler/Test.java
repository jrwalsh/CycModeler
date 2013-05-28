package edu.iastate.cycmodeler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.SBMLWriter;

import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.model.InstantiatedReactionInstance;
import edu.iastate.cycmodeler.model.MetaboliteInstance;
import edu.iastate.cycmodeler.model.ReactionInstance;
import edu.iastate.cycmodeler.model.ReactionNetwork;
import edu.iastate.cycmodeler.util.MyParameters;
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
		conn = new JavacycConnection("jrwalsh.student.iastate.edu",4444);
		conn.selectOrganism("ECOLI");
		test();
		Long stop = System.currentTimeMillis();
		Long runtime = (stop - start) / 1000;
		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	public static void test() {
		try {
			MyParameters parameters = new MyParameters();
			parameters.initializeFromConfigFile("/home/jesse/workspace/CycModeler/defaultSettings");
			
//			/home/jesse/workspace/CycModeler/defaultSettings /home/jesse/workspace/CycModeler/defaultReactionSettings
			
			CycModeler modeler = new CycModeler(conn, parameters);
			ReactionNetwork reactionNetwork = new ReactionNetwork(new ArrayList<Reaction>());
//			modeler.test();
			
			Reaction r = (Reaction)Reaction.load(conn, "MALONYL-COA-ACP-TRANSACYL-RXN");
			ReactionInstance rInstance = new ReactionInstance(r, "myName", true, "here");
			for (InstantiatedReactionInstance ir : rInstance.generateInstantiatedReactions()) {
				System.out.println(ir.printReaction());
			}
			
//			HashSet<MetaboliteInstance> m1 = new HashSet<MetaboliteInstance>();
//			HashSet<MetaboliteInstance> m2 = new HashSet<MetaboliteInstance>();
//			HashSet<MetaboliteInstance> m3 = new HashSet<MetaboliteInstance>();
//			HashSet<MetaboliteInstance> m4 = new HashSet<MetaboliteInstance>();
//			m1.add(new MetaboliteInstance(Frame.load(conn, "FRUCTOSE-6P"), "Here", 1));
//			m2.add(new MetaboliteInstance(Frame.load(conn, "FRUCTOSE-16-DIPHOSPHATE"), "Here", 1));
//			m3.add(new MetaboliteInstance(Frame.load(conn, "FRUCTOSE-6P"), "Here", 1));
//			m4.add(new MetaboliteInstance(Frame.load(conn, "ADP"), "Here", 1));
//			
//			System.out.println(m1.equals(m3));
//			System.out.println(m1.equals(m2));
//			
//			ReactionInstance ri = new ReactionInstance((Reaction)Frame.load(conn, "RXN0-6541"), "Reaction 1", true, "Here", m1, m2);
//			ReactionInstance ri2 = new ReactionInstance((Reaction)Frame.load(conn, "RXN0-6541"), "Reaction 1", true, "Here", m3, m4);
//			HashSet<ReactionInstance> set = new HashSet<ReactionInstance>();
//			set.add(ri);
//			set.add(ri2);
//			System.out.println(ri.equals(ri2));
//			System.out.println(set.size());
//			
		} catch (PtoolsErrorException e1) {
			e1.printStackTrace();
		}
	}
}
