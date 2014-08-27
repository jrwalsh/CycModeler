package edu.iastate.cycmodeler;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.cycmodeler.view.DefaultController;
import edu.iastate.cycmodeler.view.LoginPanel;
import edu.iastate.cycmodeler.view.MainCardPanel;
import edu.iastate.cycmodeler.view.OptionPanel;
import edu.iastate.javacyco.JavacycConnection;

/**
 * Main class for the CycModeler class.
 * 
 * @author Jesse R. Walsh
 *
 */
public class Main {
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
			System.out.println("Usage: Main CONFIGFILE REACTIONCONFIGFILE");
			System.exit(0);
		}
		String configFile = args[0];
		String reactionConfigFile = args[1];
		
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
		        createAndShowGUI();
		    }
		});
		
		
//		Long start = System.currentTimeMillis();
//		run(configFile, reactionConfigFile);
//		Long stop = System.currentTimeMillis();
//		Long runtime = (stop - start) / 1000;
//		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	private static void createAndShowGUI() {
//		run(configFile, reactionConfigFile);
		DefaultController controller = new DefaultController();
		
		MainCardPanel cardPanel = new MainCardPanel();
		cardPanel.add(new LoginPanel(controller), MainCardPanel.loginCard);
		cardPanel.add(new OptionPanel(controller), MainCardPanel.optionCard);
		
		controller.setMainCardPanel(cardPanel);
		
		JFrame displayFrame = new JFrame("CycBrowser");
		controller.setMainJFrame(displayFrame);
		
		displayFrame.setResizable(false);
		displayFrame.setPreferredSize(new Dimension(835, 435));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{800, 0};
		gridBagLayout.rowHeights = new int[]{35, 400, 30, 0};
		gridBagLayout.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		displayFrame.getContentPane().setLayout(gridBagLayout);
		
		GridBagConstraints gbc_toolPanel = new GridBagConstraints();
		gbc_toolPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_toolPanel.anchor = GridBagConstraints.NORTH;
		gbc_toolPanel.insets = new Insets(0, 0, 5, 0);
		gbc_toolPanel.gridx = 0;
		gbc_toolPanel.gridy = 0;
		
		GridBagConstraints gbc_cardPanel = new GridBagConstraints();
		gbc_cardPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_cardPanel.anchor = GridBagConstraints.NORTH;
		gbc_cardPanel.insets = new Insets(0, 0, 5, 0);
		gbc_cardPanel.gridx = 0;
		gbc_cardPanel.gridy = 1;
		displayFrame.getContentPane().add(cardPanel, gbc_cardPanel);
		
		GridBagConstraints gbc_statusPanel = new GridBagConstraints();
		gbc_statusPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_statusPanel.anchor = GridBagConstraints.NORTH;
		gbc_statusPanel.gridx = 0;
		gbc_statusPanel.gridy = 2;
		
        displayFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        displayFrame.pack();
        
        displayFrame.setLocationRelativeTo(null);
        displayFrame.setVisible(true);
	}
	
	/**
	 * This method initializes a CycModeler object and calls its methods.
	 */
	public static void run(String configFile, String reactionConfigFile) {
		MyParameters parameters = new MyParameters();
		parameters.initializeFromConfigFile(configFile);
		
		JavacycConnection conn = new JavacycConnection(parameters.Host, parameters.Port);
//		JavacycConnection conn = new JavacycConnection(parameters.Host, parameters.Port, parameters.User, parameters.Password);
		conn.selectOrganism(parameters.Organism);
		
		CycModeler modeler = new CycModeler(conn, parameters);
		modeler.createModel(reactionConfigFile);
	}
}
