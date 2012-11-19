package edu.iastate.cycmodeler.logic;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

import org.sbml.libsbml.*;

import edu.iastate.cycmodeler.model.AbstractReactionInstance;
import edu.iastate.cycmodeler.model.InstantiatedReactionInstance;
import edu.iastate.cycmodeler.model.MetaboliteInstance;
import edu.iastate.cycmodeler.model.ReactionInstance;
import edu.iastate.cycmodeler.model.ReactionNetwork;
import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.cycmodeler.util.ReactionChooser;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * CycModeler is a class that is designed to generate a stoichiometric model in SBML output from a BioCyc database.
 * 
 * Generates genome-scale models in which the entire EcoCyc reaction network is exported to SBML, at which point 
 * it is read in by this class and manipulated into a feasible stoichiometric model. Includes methods to add boundary
 * reactions to a model, instantiate generic reactions from ecocyc, and control which reactions are included in
 * the model.
 * 
 * @author Jesse Walsh
 *
 */
public class CycModeler {
	static protected JavacycConnection conn;
	public static MyParameters parameters;
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * 
	 * @param connectionString URL of the server running Pathway Tools
	 * @param port Port that JavaCycO is listening on
	 * @param organism Organism to connect to (i.e., selects which database to connect to)
	 * @param configFile Path to configuration file
	 */
	public CycModeler (String connectionString, int port, String organism, MyParameters parameters) {
		String CurrentConnectionString = connectionString;
		int CurrentPort = port;
		String CurrentOrganism = organism;

		conn = new JavacycConnection(CurrentConnectionString,CurrentPort);
		conn.selectOrganism(CurrentOrganism);
		
		this.parameters = parameters;
	}
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * Does not set an organism for the JavacycConnection object.  Expects that the connection object has already selected an organism.
	 * 
	 * @param connection Initialized connection object
	 * @param configFile Path to configuration file
	 */
	public CycModeler (JavacycConnection connection, MyParameters parameters) {
		conn = connection;
		this.parameters = parameters;
	}
	

	// Methods
	/**
	 * This method will create a new genome-scale model from an EcoCyc database. All reactions are included other than 
	 * |Polynucleotide-Reactions| and |Protein-Reactions| reactions, which are filtered out. Generic reactions are
	 * instantiated, and boundary reactions are created for external metabolites. The model is then written to an
	 * SBML file.
	 */
	public void createGenomeScaleModelFromEcoCyc() {
		// 1) Create blank model
		System.out.println("Generating blank model ...");
		SBMLDocument doc = createBlankSBMLDocument(parameters.ModelName, parameters.DefaultSBMLLevel, parameters.DefaultSBMLVersion);
		
		// 2) Load all reactions
//		System.out.println("Loading all reactions ...");
//		ArrayList<Reaction> allReactions = Reaction.all(conn);
//		ReactionNetwork reactionNetwork = ReactionNetwork.getReactionNetwork(conn, allReactions);
		
		// 3) Filter unwanted reactions
//		System.out.println("Filtering unwanted reactions ...");
		System.out.println("Initializing reaction network ...");
		ArrayList<String> classToFilter = new ArrayList<String>();
		classToFilter.add("|Polynucleotide-Reactions|");
		classToFilter.add("|Protein-Reactions|"); //TODO filter more stuff
		classToFilter.add("|RNA-Reactions|");
		ArrayList<String> metaboliteClassToFilter = new ArrayList<String>();
		metaboliteClassToFilter.add("|Proteins|");
		metaboliteClassToFilter.add("|Nucleotides|"); //TODO filter more stuff
		metaboliteClassToFilter.add("|POLYPEPTIDE|");
		ArrayList<String> reactionsToFilter = new ArrayList<String>();
		reactionsToFilter.add("RXN0-5266");//Duplicate reactants/products to RXN0-5268              //TODO Verify these duplicate removals (and consider merging instead)
		reactionsToFilter.add("RXN0-5330");//Duplicate reactants/products to NADH-DEHYDROG-A-RXN
		reactionsToFilter.add("ACID-PHOSPHATASE-RXN");//Merge with ALKAPHOSPHA-RXN
		reactionsToFilter.add("RXN0-6370");//Should not conflict with RXN0-5242
		reactionsToFilter.add("RXN0-5255");//Should not conflict with RXN0-3283
		reactionsToFilter.add("RXN0-5254");//Should not conflict with TRANS-RXN0-234
		reactionsToFilter.add("RXN0-5244");//Should not conflict with TRANS-RXN0-237
		ReactionNetwork reactionNetwork = new ReactionNetwork(conn);
//		reactionNetwork.initializeGenomeScaleReactionNetwork(classToFilter, reactionsToFilter); //TODO filtering should be part of the config file
//		reactionNetwork.initializeReactionNetwork();
		ReactionChooser reactionChooser = new ReactionChooser(conn);
		try {
			reactionChooser.getAllReactions();
			reactionChooser.removeReactionsByClass(classToFilter);
			reactionChooser.removeReactionsByMetaboliteClass(metaboliteClassToFilter);
			reactionChooser.removeSpecificReactions(reactionsToFilter);
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		reactionNetwork.importJavacycReactions(reactionChooser.getReactionList());
		
		// 4) Find and instantiate generics
		System.out.println("Instantiating generic reactions ...");
		reactionNetwork.generateSpecificReactionsFromGenericReactions();
		
		// 5) Add diffusion reactions
		System.out.println("Adding diffusion reactions ...");
		reactionNetwork.addPassiveDiffusionReactions("CCO-PERI-BAC" , "CCO-EXTRACELLULAR", (float) 610.00); //TODO again, from a config file
		
		// 6) Add boundaries
		System.out.println("Adding boundary reactions ...");
		reactionNetwork.addBoundaryReactionsByCompartment(parameters.ExternalCompartmentName);
		
		// 7) Generate SBML model
		System.out.println("Generating SBML model ...");
		generateSBMLModel(doc, reactionNetwork);
		
		// 8) Write revised model.
		System.out.println("Writing output ...");
		SBMLWriter writer = new SBMLWriter();
		writer.writeSBML(doc, parameters.OutputDirectory + "written_SBML.xml"); //TODO again, config file for file name
		
		// Print statistics
		reactionNetwork.printNetworkStatistics();
		
		System.out.println("Done!");
	}
	
	
	// SBML Document Methods
	/**
	 * Initialize a blank SBMLDocument object with default values set.  Creates the Model object and sets
	 * the mmol_per_gDW_per_hr UnitDefinition.
	 * @param modelID Name of model
	 * @param SBMLLevel Create SBMLDocument to conform to this level
	 * @param SBMLVersion Create SBMLDocument to conform to this version
	 * @return Initialized SBMLDocument object with a blank Model
	 */
	public SBMLDocument createBlankSBMLDocument(String modelID, int SBMLLevel, int SBMLVersion) {
		SBMLDocument doc = new SBMLDocument(SBMLLevel, SBMLVersion);
		Model model = doc.createModel(modelID);
		model.setName("Generated from BioCyc Pathway/Genome Database");
		
		UnitDefinition UD = model.createUnitDefinition();
		UD.setId("mmol_per_gDW_per_hr");
		Unit mole = UD.createUnit();
		mole.setKind(libsbmlConstants.UNIT_KIND_MOLE);
		mole.setScale(-3);
		mole.setMultiplier(1);
		mole.setOffset(0);
		
		Unit gram = UD.createUnit();
		gram.setKind(libsbmlConstants.UNIT_KIND_GRAM);
		gram.setExponent(-1);
		gram.setMultiplier(1);
		gram.setOffset(0);
		
		Unit second = UD.createUnit();
		second.setKind(libsbmlConstants.UNIT_KIND_SECOND);
		second.setExponent(-1);
		second.setMultiplier(0.00027777);
		second.setOffset(0);
		
		return doc;
	}
	
	/**
	 * Populate an SBMLDocument object with reaction, metabolite, and compartments.
	 * 
	 * @param doc Empty SBMLDocument with initialized Model object containing only a model name and unit definitions.
	 * 
	 * @param reactionInstances Reactions which represent the complete reaction set for the model.
	 * @return SBMLDocument object with poplulated model.
	 */
	protected SBMLDocument generateSBMLModel(SBMLDocument doc, ReactionNetwork reactionNetwork) {
		Model model = doc.getModel();
		ArrayList<String> metabolites = new ArrayList<String>();
		ArrayList<String> compartments = new ArrayList<String>();
		HashSet<AbstractReactionInstance> reactionInstances = reactionNetwork.Reactions;
		
		try {
			// Create compartment list
			for (AbstractReactionInstance reaction : reactionInstances) {
				ArrayList<MetaboliteInstance> reactantsProducts = new ArrayList<MetaboliteInstance>();
				reactantsProducts.addAll(reaction.reactants_);
				reactantsProducts.addAll(reaction.products_);
				for (MetaboliteInstance species : reactantsProducts) {
					if (!compartments.contains(species.compartment_)) {
						Compartment compartment = model.createCompartment();
						compartment.setId(convertToSBMLSafe(species.compartment_));
						compartment.setName(species.compartment_);
//						if (compartment.setId(convertToSBMLSafe(species.compartment)) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (compartment.setName(species.compartment) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
						compartments.add(species.compartment_);
					}
				}
			}
			
			// Create species list
			for (AbstractReactionInstance reaction : reactionInstances) {
				ArrayList<MetaboliteInstance> reactantsProducts = new ArrayList<MetaboliteInstance>();
				reactantsProducts.addAll(reaction.reactants_);
				reactantsProducts.addAll(reaction.products_);
				for (MetaboliteInstance species : reactantsProducts) {
					if (!metabolites.contains(species.generateSpeciesID())) {
						Species newSpecies = model.createSpecies();
						String sid = species.generateSpeciesID();
						newSpecies.setId(sid);
						newSpecies.setName(species.getMetaboliteFrame().getCommonName());
						newSpecies.setCompartment(model.getCompartment(convertToSBMLSafe(species.compartment_)).getId());
						newSpecies.setBoundaryCondition(false);
//						if (newSpecies.setId(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setName(species.metabolite.getCommonName()) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setCompartment(model.getCompartment(convertToSBMLSafe(species.compartment)).getId()) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setBoundaryCondition(false) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
						metabolites.add(sid);
						
						// Append Notes
						newSpecies.appendNotes("Palsson SID : \n"); //TODO does not exist except for ecoli
						newSpecies.appendNotes("EcoCyc Frame ID : " + species.getMetaboliteID() + "\n");
						newSpecies.appendNotes("Chemical Formula : " + "\n");
					}
				}
			}
			
			// Create reaction list
			for (AbstractReactionInstance reaction : reactionInstances) {
				org.sbml.libsbml.Reaction newReaction = model.createReaction();
				newReaction.setId(reaction.generateReactionID());
//				if (reaction.ReactionFrame != null) newReaction.setId(reaction.generateReactionID());
//				else if (reaction.parentReaction_ != null) newReaction.setId(reaction.generateReactionID());
//				else newReaction.setId(reaction.generateReactionID());
				newReaction.setName(reaction.name_);
				newReaction.setReversible(reaction.reversible_);
//				if (reaction.thisReactionFrame != null) {
//					if (newReaction.setId(convertToSBMLSafe(reaction.thisReactionFrame.getLocalID())) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				} else if (reaction.parentReaction != null) {
//					if (newReaction.setId(convertToSBMLSafe(reaction.parentReaction.getLocalID())) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				} else {
//					if (newReaction.setId(convertToSBMLSafe(reaction.name)) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				}
//				if (newReaction.setName(reaction.name) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				
				for (MetaboliteInstance reactant : reaction.reactants_) {
					String sid = reactant.generateSpeciesID();
					SpeciesReference ref = newReaction.createReactant();
					ref.setSpecies(sid);
					ref.setStoichiometry(reactant.coefficient_);
//					if (ref.setSpecies(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (ref.setStoichiometry(reactant.stoichiometry) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (newReaction.addReactant(ref) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				}
				for (MetaboliteInstance product : reaction.products_) {
					String sid = product.generateSpeciesID();
					SpeciesReference ref = newReaction.createProduct();
					ref.setSpecies(sid);
					ref.setStoichiometry(product.coefficient_);
//					if (ref.setSpecies(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (ref.setStoichiometry(product.stoichiometry) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (newReaction.addProduct(ref) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				}
				
				// Kinetic Law
				ASTNode math = new ASTNode();
				math.setName("FLUX_VALUE");
				
				KineticLaw kl = newReaction.createKineticLaw();
				kl.setFormula("");
				kl.setMath(math);
				
				Parameter lb = kl.createParameter();
				lb.setId("LOWER_BOUND");
				if (newReaction.getReversible()) lb.setValue(-1000); //TODO set default scale value in config file, for both upper and lower
				else lb.setValue(0);
				lb.setUnits("mmol_per_gDW_per_hr");
				
				Parameter ub = kl.createParameter();
				ub.setId("UPPER_BOUND");
				ub.setValue(1000);
				ub.setUnits("mmol_per_gDW_per_hr");
				
				Parameter obj = kl.createParameter();
				obj.setId("OBJECTIVE_COEFFICIENT");
				obj.setValue(0);
				
				Parameter flux = kl.createParameter();
				flux.setId("FLUX_VALUE");
				flux.setValue(0);
				flux.setUnits("mmol_per_gDW_per_hr");
				
				// Append Notes
//				if (map.containsKey(reaction.name)) {
//					newReaction.appendNotes("Palsson Reaction ID : ");
//					for (String s : map.get(reaction.name)) newReaction.appendNotes(s + ",");
//					newReaction.appendNotes("\n");
//				}
				newReaction.appendNotes("Palsson Reaction ID : \n");//TODO does not exist except for ecoli
				newReaction.appendNotes("EcoCyc Frame ID : \n");
				newReaction.appendNotes("Abbreviation : \n");
				newReaction.appendNotes("Synonyms : \n");
				newReaction.appendNotes("EC Number : \n");
				newReaction.appendNotes("SUBSYSTEM : \n");
				newReaction.appendNotes("Equation : \n");
				newReaction.appendNotes("Confidence Level : \n");
//				if (reaction.ReactionFrame != null) newReaction.appendNotes("Gene Rule : " + reaction.reactionGeneRule(false));//reactionGeneRule(reaction.thisReactionFrame.getLocalID(), false));
//				else if (reaction.parentReaction_ != null) newReaction.appendNotes("Gene Rule : " + reaction.reactionGeneRule(false));//reaction.reactionGeneRule(reaction.parentReaction.getLocalID(), false));
				if (reaction instanceof ReactionInstance) newReaction.appendNotes("Gene Rule : " + ((ReactionInstance)reaction).reactionGeneRule(false));
				else if (reaction instanceof InstantiatedReactionInstance) newReaction.appendNotes("Gene Rule : " + ((InstantiatedReactionInstance)reaction).reactionGeneRule(false));
				else newReaction.appendNotes("Gene Rule : ");
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return doc;
	}
	
	
	// Helper functions
	/**
	 * Read an SBML file, catch any SBML and/or read errors. Return the SBMLDocument generated.
	 * 
	 * @param fileName
	 * @return
	 */
	public SBMLDocument readSBML(String fileName) {
		SBMLReader reader = new SBMLReader();
		SBMLDocument doc  = reader.readSBML(fileName);

		if (doc.getNumErrors() > 0) {
		    if (doc.getError(0).getErrorId() == libsbmlConstants.XMLFileUnreadable) System.out.println("XMLFileUnreadable error occured."); 
		    else if (doc.getError(0).getErrorId() == libsbmlConstants.XMLFileOperationError) System.out.println("XMLFileOperationError error occured.");  
		    else System.out.println("Error occured in document read or document contains errors.");
		}
		
		return doc;
	}
	
	/**
	 * Replace characters that are commonly used in EcoCyc with characters safe to use in SBML names and IDs.
	 * 
	 * @param input
	 * @return
	 */
	public static String convertToSBMLSafe(String input) {
		String output = input;
		output = output.replace("-", "__45__");
		output = output.replace("+", "__43__");
		output = output.replace(" ", "__32__");
		output = output.replace("(", "__40__");
		output = output.replace(")", "__41__");
		output = output.replace(".", "__46__");
		
		output = output.replace("|", "");
		try {
			Integer.parseInt(output.substring(0,1));
			output = "_" + output;
		} catch(NumberFormatException nfe) {
			// Do nothing
		}
		
//		output = output + "_CCO__45__CYTOSOL";
		return output;
	}
	
	/**
	 * Reverse of convertToSBMLSafe.
	 * 
	 * @param input
	 * @return
	 */
	private String convertFromSBMLSafe(String input) {
		String output = input;
		output = output.replace("__45__", "-");
		output = output.replace("__43__", "+");
		output = output.replace("__32__", " ");
		output = output.replace("__40__", "(");
		output = output.replace("__41__", ")");
		output = output.replace("__46__", ".");
		if (output.substring(0,1).equals("_")) output = output.substring(1, output.length());
		
		output = output.replace("_CCO-UNKNOWN-SPACE", "");
		output = output.replace("_CCO-CYTOPLASM", "");
		output = output.replace("_CCO-EXTRACELLULAR", "");
		output = output.replace("_CCO-PERIPLASM", "");
		output = output.replace("_CCO-PERI-BAC", "");
		output = output.replace("_CCO-PM-BAC-NEG", "");
		output = output.replace("_CCO-CYTOSOL", "");
		
		return output;
	}
	
	/**
	 * Simple function to print a string to the specified file location.
	 * 
	 * @param fileName
	 * @param printString
	 */
	private void printString(String fileName, String printString) {
		PrintStream o = null;
		try {
			o = new PrintStream(new File(fileName));
			o.println(printString);
			o.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
 	}
	
}
