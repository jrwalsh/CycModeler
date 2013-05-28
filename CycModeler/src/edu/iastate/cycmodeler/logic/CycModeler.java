package edu.iastate.cycmodeler.logic;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
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
	public static JavacycConnection conn;
	public static MyParameters parameters;
	
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
	
	// Test
	public void test() throws PtoolsErrorException {
		ArrayList<Reaction> reactionList = new ArrayList<Reaction>();
		reactionList.add((Reaction) Reaction.load(conn, "MALATE-DEHYDROGENASE-ACCEPTOR-RXN"));
		ReactionNetwork reactionNetwork = new ReactionNetwork(reactionList);
		
		// 2) Find and instantiate generics
		System.out.println("Instantiating generic reactions ...");
		reactionNetwork.generateSpecificReactionsFromGenericReactions();
		
		// 5) Create blank model
		System.out.println("Initiating blank model ...");
		SBMLDocument doc = createBlankSBMLDocument("Test", 2, 1);
				
		// 6) Generate SBML model
		System.out.println("Generating SBML model ...");
		generateSBMLModel(doc, reactionNetwork);
		
		// 7) Write model.
		System.out.println("Writing output ...");
		SBMLWriter writer = new SBMLWriter();
		writer.writeSBML(doc, "/home/jesse/Desktop/test_output_model.xml");
		
		// Print statistics
		reactionNetwork.printNetworkStatistics();
		
		System.out.println("Done!");
	}

	// Methods
	/**
	 * This method will create a new model from a Cyc database. Generic reactions are instantiated, and boundary reactions
	 * are created for external metabolites. The model is then written to an SBML file.
	 */
	public void createModel(String reactionConfigFile) {
		// 1) Load reaction config file
		/*
		 * Note that in this step all reactions requested are considered and placed in the reaction list. However, duplicate reactions are not
		 * allowed. Duplicate reactions are considered reactions for which the exact same reactants and products are used and the reactions 
		 * occur in the same location (ie having the same name is not sufficient for a reaction to be considered duplicate at this stage)
		 */
		System.out.println("Loading reaction config file ...");
		ReactionChooser reactionChooser = new ReactionChooser(reactionConfigFile);
		ReactionNetwork reactionNetwork = new ReactionNetwork(reactionChooser.getReactionList());
		
		// 1.1) Remove reactions with the CANNOT-BALANCE flag set
		reactionNetwork.removeCannotBalanceReactions();
		
		// 2.1) Remove any unbalance reactions
		reactionNetwork.removeUnbalancedReactions();
		
		// 2) Find and instantiate generics
		/*
		 * Note that in this step all reactions are filtered between generic and non-generic reactions. Those that are generic are attempted to be
		 * instantiated. Those that aren't are left alone. Generic reactions, whether instantiated or not, are removed from the reaction list.
		 */
		System.out.println("Instantiating generic reactions ...");
		reactionNetwork.generateSpecificReactionsFromGenericReactions();
		
		// 2.1) Final filter for all non-balanced reactions
		
		// 2.2) Output network heatmap
		String heatMap = reactionNetwork.generateHeatMap();
		System.out.println(heatMap);
		
		// 3) Add diffusion reactions
		System.out.println("Adding diffusion reactions ...");
		reactionNetwork.addPassiveDiffusionReactions("CCO-PERI-BAC" , "CCO-EXTRACELLULAR", parameters.DiffusionSize);
		
		// 4) Add boundaries
		System.out.println("Adding boundary reactions ...");
		reactionNetwork.addBoundaryReactionsByCompartment(parameters.ExternalCompartmentName);
		
		// 5) Create blank model
		System.out.println("Initiating blank model ...");
		SBMLDocument doc = createBlankSBMLDocument(parameters.ModelName, parameters.DefaultSBMLLevel, parameters.DefaultSBMLVersion);
				
		// 6) Generate SBML model
		System.out.println("Generating SBML model ...");
		generateSBMLModel(doc, reactionNetwork);
		
		// 7) Write model.
		System.out.println("Writing output ...");
		SBMLWriter writer = new SBMLWriter();
		writer.writeSBML(doc, parameters.OutputDirectory + parameters.OutputFileName);
		
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
			HashMap<String, Integer> reactionMap = new HashMap<String, Integer>();
			for (AbstractReactionInstance reaction : reactionInstances) {
				org.sbml.libsbml.Reaction newReaction = model.createReaction();
				
//				ListOfReactions listOfReactions = model.getListOfReactions();
//				for (int i = 0; i < listOfReactions.size(); i++) {
//					org.sbml.libsbml.Reaction rxn = listOfReactions.get(i);
//				}
				
				// Handle duplicate IDs
				String reactionID = reaction.generateReactionID();
				if (reactionMap.containsKey(reactionID)) {
					int value = reactionMap.get(reactionID) + 1;
					reactionMap.put(reactionID, value);
					reactionID += "_" + value;
				} else reactionMap.put(reactionID, 0);
				
				newReaction.setId(reactionID);
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
				if (newReaction.getReversible()) lb.setValue(parameters.DefaultLowerBound);
				else lb.setValue(0);
				lb.setUnits("mmol_per_gDW_per_hr");
				
				Parameter ub = kl.createParameter();
				ub.setId("UPPER_BOUND");
				ub.setValue(parameters.DefaultUpperBound);
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
}
