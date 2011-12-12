package cycModeler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sbml.libsbml.*;

import edu.iastate.javacyco.Reaction;
import edu.iastate.javacyco.*;

/**
 * CycModeler is a class that is designed to generate a stoichiometric model in SBML output from a BioCyc database.
 * This class is built around the JavaCycO class created by John Van Hemert.
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
	// Global variables
	protected JavacycConnection conn = null;
	protected String OutputDirectory = "";
	protected String defaultCompartment = "CCO-CYTOSOL";
	protected int DefaultSBMLLevel = 2;
	protected int DefaultSBMLVersion = 1;
	protected HashMap<String, String> compartmentAbrevs = new HashMap<String, String>();
	protected String speciesPrefix = "M";
	protected String reactionPrefix = "R";
	protected String BoundaryCompartmentName = "Boundary";
	protected String ExchangeReactionSuffix = "exchange";
	
	// Constructor
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * @param connectionString URL of the server running Pathway Tools
	 * @param port Port that JavaCycO is listening on
	 * @param organism Organism to connect to (i.e., selects which database to connect to)
	 */
	public CycModeler (String connectionString, int port, String organism) {
		String CurrentConnectionString = connectionString;
		int CurrentPort = port;
		String CurrentOrganism = organism;

		conn = new JavacycConnection(CurrentConnectionString,CurrentPort);
		conn.selectOrganism(CurrentOrganism);
		
		setDefaults();
	}
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * Does not set an organism for the JavacycConnection object
	 * @param connection Initialized connection object
	 */
	public CycModeler (JavacycConnection connection) {
		conn = connection;
		setDefaults();
	}
	

	// Methods
	/**
	 * Initializes default settings for generating SBML models and for translating information from EcoCyc for use
	 * in the SBML models.
	 */
	public void setDefaults() {
		//TODO read from a config file?
		OutputDirectory = "/home/jesse/Desktop/output/";
		defaultCompartment = "CCO-CYTOSOL";
		DefaultSBMLLevel = 2;
		DefaultSBMLVersion = 1;
		
		
		BoundaryCompartmentName = "Boundary";
		ExchangeReactionSuffix = "exchange";
		
		speciesPrefix = "M";
		reactionPrefix = "R";
		
		compartmentAbrevs.put("CCO-CYTOSOL", "c");
		compartmentAbrevs.put("CCO-PERI-BAC", "periBac");
		compartmentAbrevs.put("CCO-PERIPLASM", "p");
		compartmentAbrevs.put("CCO-EXTRACELLULAR", "e");
		compartmentAbrevs.put("CCO-CYTOPLASM", "cp");
		compartmentAbrevs.put("CCO-UNKNOWN-SPACE", "unk");
		compartmentAbrevs.put("CCO-IN", "i");
		compartmentAbrevs.put("CCO-OUT", "o");
		compartmentAbrevs.put("CCO-MIDDLE", "m");
		compartmentAbrevs.put("Boundary", "b");
	}
	
	/**
	 * This method will create a new genome-scale model from an EcoCyc database. All reactions are included other than 
	 * |Polynucleotide-Reactions| and |Protein-Reactions| reactions, which are filtered out. Generic reactions are
	 * instantiated, and boundary reactions are created for external metabolites. The model is then written to an
	 * SBML file.
	 */
	public void createGenomeScaleModelFromEcoCyc() {
		//TODO Setup a config file
		try {
			// 1) Create blank model
			System.out.println("Generating blank model ...");
			SBMLDocument doc = createBlankSBMLDocument("CBiRC", DefaultSBMLLevel, DefaultSBMLVersion);
			
			// 2) Load all reactions
			System.out.println("Loading all reactions ...");
			ArrayList<Reaction> allReactions = Reaction.all(conn);
			
			// 3) Filter unwanted reactions
			System.out.println("Filtering unwanted reactions ...");
			ArrayList<String> classToFilter = new ArrayList<String>();
			classToFilter.add("|Polynucleotide-Reactions|");
			classToFilter.add("|Protein-Reactions|");
			FilterResults filterResults = filterReactions(allReactions, classToFilter, null);
			
			// 4) Find and instantiate generics
			System.out.println("Instantiating generic reactions ...");
			InstantiationResults instantiationResults = generateSpecificReactionsFromGenericReactions(filterResults.keepList);
			ArrayList<String> reactionsToFilter = new ArrayList<String>();
			for (Frame reaction : instantiationResults.genericReactionsFound) reactionsToFilter.add(reaction.getLocalID());
			FilterResults genericReactionFilterResults = filterReactions(filterResults.keepList, null, reactionsToFilter);
			
			// 5) Add boundaries
			System.out.println("Adding boundary reactions ...");
			ArrayList<ReactionInstance> reactions = reactionListToReactionInstances(genericReactionFilterResults.keepList);
			reactions.addAll(instantiationResults.instantiatedReactions);
			ArrayList<ReactionInstance> boundaryResults = addBoundaryReactionsByCompartment("CCO-OUT", reactions);
			
			// 6) Generate SBML model
			System.out.println("Generating SBML model ...");
			reactions.addAll(boundaryResults);
			generateSBMLModel(doc, reactions);
			
			ArrayList<String> list = (ArrayList<String>)conn.getClassAllInstances("|Transport-Reactions|");
			for (ReactionInstance reaction : reactions) {
				if (reaction.thisReactionFrame != null) {
					String s = "\tnon-transport";
					if (list.contains(reaction.thisReactionFrame.getLocalID())) s = "\tTransport-Reaction";
					System.out.println("thisReactionFrame = " + reaction.thisReactionFrame.getLocalID() + s);
				}
				else if (reaction.parentReaction != null) {
					String s = "\tnon-transport";
					if (list.contains(reaction.parentReaction.getLocalID())) s = "\tTransport-Reaction";
					System.out.println("parentReaction = " + reaction.parentReaction.getLocalID() + s);
				}
				else {
					System.out.println("name = " + reaction.name);
				}
			}
			
			// 7) Write revised model.
			System.out.println("Writing output ...");
			SBMLWriter writer = new SBMLWriter();
			writer.writeSBML(doc, OutputDirectory + "written_SBML.xml");
			
			// *Mapping*
			System.out.println("Writing mapping output ...");
			printBoundaryReactionMetaboliteList(boundaryResults, "boundaryMetabolites");
			
			
			// Print statistics
			System.out.println("Writing statistics ...");
			System.out.println("All reactions : " + allReactions.size());
			System.out.println("Filtered reactions keeplist : " + filterResults.keepList.size());
			System.out.println("Filtered reactions tosslist : : " + filterResults.removedList.size());
			System.out.println("Generic reactions found : " + instantiationResults.genericReactionsFound.size());
			System.out.println("Generic reactions failed to instantiate : " + instantiationResults.genericReactionsFailedToInstantiate.size());
			System.out.println("New reactions from generic reaction instantiations : " + instantiationResults.instantiatedReactions.size());
			System.out.println("Generic keeplist : " + genericReactionFilterResults.keepList.size());
			System.out.println("Generic tosslist : " + genericReactionFilterResults.removedList.size());
			System.out.println("Boundary reactions added : " + boundaryResults.size());
			int grandTotal = instantiationResults.instantiatedReactions.size() + genericReactionFilterResults.keepList.size() + boundaryResults.size();
			System.out.println("Grand total : " + grandTotal);
			
			System.out.println("Done!");
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * TODO
	 */
	public void createReactionScaleModelFromEcoCyc() {
//		String outputFileName = "reaction_sbml.xml";
//		
//		ArrayList<String> reactionIDs = new ArrayList<String>();
//		reactionIDs.add("");
//		
//		
//		ArrayList<String> boundaryMetaboliteIDs = new ArrayList<String>();
//		boundaryMetaboliteIDs.add("");
//		
//		try {
//			// 1) Create blank model
//			System.out.println("Generating blank model ...");
//			SBMLDocument doc = createBlankSBMLDocument("CBiRC", DefaultSBMLLevel, DefaultSBMLVersion);
//			
//			// 2) Load all reactions
//			System.out.println("Loading reactions ...");
//			ArrayList<Reaction> allReactions = new ArrayList<Reaction>();
//			for (String reactionID : reactionIDs) allReactions.add((Reaction)Reaction.load(conn, reactionID));
//			
//			// 4) Find and instantiate generics
//			
//			// 5) Add boundaries
//			System.out.println("Adding boundary reactions ...");
//			ArrayList<ReactionInstance> reactions = reactionListToReactionInstances(allReactions);
//			ArrayList<ReactionInstance> boundaryResults = addBoundaryReactionsByID("CCO-IN", boundaryMetaboliteIDs);
//			
//			// 6) Generate SBML model
//			System.out.println("Generating SBML model ...");
//			reactions.addAll(boundaryResults);
//			generateSBMLModel(doc, reactions);
//			
//			// 7) Write revised model.
//			System.out.println("Writing output ...");
//			SBMLWriter writer = new SBMLWriter();
//			writer.writeSBML(doc, OutputDirectory + outputFileName);
//			
//			System.out.println("Done!");
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
////		Long stop = System.currentTimeMillis();
////		Long runtime = (stop - start) / 1000;
////		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	/**
	 * TODO
	 */
	public void createCustomModel() {
//		ArrayList<ReactionInstance> reactions = new ArrayList<ReactionInstance>();
//		
//		ArrayList<Metabolite> reactants = new ArrayList<Metabolite>();
//		Metabolite reactant = new Metabolite(null, "Cytoplasm", 1, "CO2");
//		ArrayList<Metabolite> products = new ArrayList<Metabolite>();
//		ReactionInstance reaction = new ReactionInstance(null, null, "name", false, reactants, products);
//		
//		
//		
//		String outputFileName = "reaction_sbml.xml";
//		
//		try {
//			// 1) Create blank model
//			System.out.println("Generating blank model ...");
//			SBMLDocument doc = createBlankSBMLDocument("CBiRC", DefaultSBMLLevel, DefaultSBMLVersion);
//			
//			// 6) Generate SBML model
//			System.out.println("Generating SBML model ...");
//			generateSBMLModel(doc, reactions);
//			
//			// 7) Write revised model.
//			System.out.println("Writing output ...");
//			SBMLWriter writer = new SBMLWriter();
//			writer.writeSBML(doc, OutputDirectory + outputFileName);
//			
//			System.out.println("Done!");
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
////		Long stop = System.currentTimeMillis();
////		Long runtime = (stop - start) / 1000;
////		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
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
	 * Remove all Reactions from the ArrayList reactions which are either an instance of any of the EcoCyc classes in classToFilter, or
	 * are explicitly named with their EcoCyc Frame ID in the reactionsToFilter list. 
	 * @param reactions List of Reactions to which the filter will be applied
	 * @param classToFilter EcoCyc Frame ID of a class frame, instances of which should be removed from reactions
	 * @param reactionsToFilter EcoCyc Frame ID of a reaction frame which should be removed from reactions
	 * @return FilterResults containing the filtered reaction list and a list of reactions actually removed
	 */
	public FilterResults filterReactions(ArrayList<Reaction> reactions, ArrayList<String> classToFilter, ArrayList<String> reactionsToFilter) {
		ArrayList<String> filter = new ArrayList<String>();
		ArrayList<Reaction> removedList = new ArrayList<Reaction>();
		ArrayList<Reaction> keepList = new ArrayList<Reaction>();
		
		try {
			if (classToFilter != null) {
				for (String reactionClass : classToFilter) {
					for (Object reaction : conn.getClassAllInstances(reactionClass)) filter.add(reaction.toString());
				}
			}
			
			if (reactionsToFilter != null) {
				for (String reaction : reactionsToFilter) filter.add(reaction);
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		for (Reaction reaction : reactions) {
			if (filter.contains(reaction.getLocalID())) removedList.add(reaction);
			else keepList.add(reaction);
		}
		
		return new FilterResults(keepList, removedList);
	}
	
	/**
	 * Will take in a list of reactions, find any generic reactions (according to EcoCyc), and will attempt to return instances of the
	 * generic reactions from data in EcoCyc.
	 * 
	 * Note: Metabolite class instance information is not complete, resulting in reactions that should exist but aren't included here.
	 * Also, reactions have been found that should occur but do not pass the elemental balancing step due to missing proton/water
	 * molecules.
	 * 
	 * @param reactions List of reactions to instantiate
	 * @return Results of the attempt to instantiate generic reactions
	 */
	public InstantiationResults generateSpecificReactionsFromGenericReactions(ArrayList<Reaction> reactions) {
		InstantiationResults instantiationResults = new InstantiationResults(new ArrayList<ReactionInstance>(), new ArrayList<Frame>(), new ArrayList<Frame>());
		
		for (Reaction reaction : reactions) {
			if (isGeneralizedReaction(reaction)) {
				instantiationResults.genericReactionsFound.add(reaction);
				ArrayList<ReactionInstance> instantiatedReactions = prepareGenericReaction(reaction);
				if (instantiatedReactions != null && instantiatedReactions.size() > 0) {
					instantiationResults.instantiatedReactions.addAll(instantiatedReactions);
				} else {
					instantiationResults.genericReactionsFailedToInstantiate.add(reaction);
				}
			}
		}
		
		return instantiationResults;
	}
	
	/**
	 * Checks a reaction to see if it is possible to instantiate. If checks pass, makes a call to generateInstantiatedReactions
	 * which returns any instantiated reactions that could be made.
	 * 
	 * Note: Returns null if specific forms of reaction already exist in the EcoCyc database, as it is assumed that these will
	 * be retrieved directly in the initial list of reactions being considered for model generation.
	 *  
	 * @param reaction Generic reaction to instantiate
	 * @return Instantiated forms of the generic reaction if possible. Returns null if reaction cannot be instantiated, and empty
	 * list of no instances could be created.
	 */
	@SuppressWarnings("unchecked")
	protected ArrayList<ReactionInstance> prepareGenericReaction(Reaction reaction) {
		ArrayList<String> allReactantIDs = new ArrayList<String>();
		ArrayList<String> allProductIDs = new ArrayList<String>();
		ArrayList<Frame> generalizedReactants = new ArrayList<Frame>();
		ArrayList<Frame> generalizedProducts = new ArrayList<Frame>();
		ArrayList<Frame> reactants = new ArrayList<Frame>();
		ArrayList<Frame> products = new ArrayList<Frame>();
		
		try {
			// If reaction has specific forms, then assume those forms are already in the model
			if (conn.specificFormsOfReaction(reaction.getLocalID()).size() > 0) return null;
			
			// If reaction cannot be balanced then it cannot be instantiated
			if (reaction.hasSlot("CANNOT-BALANCE?") && reaction.getSlotValue("CANNOT-BALANCE?") != null) return null;
			
			// Get reactants and products.  Must account for direction of reaction.
			String reactantSlot = "";
			String productSlot = "";
			if (reaction.getSlotValue("REACTION-DIRECTION") == null || !reaction.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
				reactantSlot = "LEFT";
				productSlot = "RIGHT";
			} else {
				reactantSlot = "RIGHT";
				productSlot = "LEFT";
			}
			
			allReactantIDs = reaction.getSlotValues(reactantSlot);
			allProductIDs = reaction.getSlotValues(productSlot);
			
			for (String reactantID : allReactantIDs) {
				Frame reactant = loadFrame(reactantID);
				if (conn.getFrameType(reactantID).toUpperCase().equals(":CLASS")) generalizedReactants.add(reactant);
				else reactants.add(reactant);
			}
			for (String productID : allProductIDs) {
				Frame product = loadFrame(productID);
				if (conn.getFrameType(productID).toUpperCase().equals(":CLASS")) generalizedProducts.add(product);
				else products.add(product);
			}
			
			// Make sure this reaction is a generalized reaction
			if (generalizedReactants.size() == 0 && generalizedProducts.size() == 0) return null;
			
			return generateInstantiatedReactions(reaction, reactants, products, generalizedReactants, generalizedProducts, reactantSlot, productSlot);
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Generates new instantiated reactions for a generic reaction by finding all instances of the metabolite classes in the reaction and
	 * generating every combination of these instances possible. Only metabolite combinations that result in reactions that are
	 * elementally balanced are included in the results.
	 * 
	 * Note: Generation of instances of metabolite classes depends on the existance of this information in EcoCyc. It is known that
	 * this information is currently incomplete, resulting in reactions that should exist but aren't included here. Also, reactions
	 * have been found that should occur but do not pass the elemental balancing step due to missing proton/water molecules. This 
	 * issue is currently left up to manual review.
	 * 
	 * @param origReaction Generic reaction being instantiated
	 * @param reactants Non-generic reactant metabolites
	 * @param products Non-generic product metabolites
	 * @param genericReactants Generic reactant metabolites
	 * @param genericProducts Generic product metabolites
	 * @param reactantSlot Slot name which holds reactants (Usually "RIGHT" or "LEFT" depending on reaction direction)
	 * @param productSlot Slot name which holds products (Usually "RIGHT" or "LEFT" depending on reaction direction)
	 * @return List of newly created, elementally balanced reaction instances
	 */
	private ArrayList<ReactionInstance> generateInstantiatedReactions(Reaction origReaction, ArrayList<Frame> reactants, ArrayList<Frame> products, ArrayList<Frame> genericReactants, ArrayList<Frame> genericProducts, String reactantSlot, String productSlot) {
		ArrayList<ReactionInstance> newReactions = new ArrayList<ReactionInstance>();
		
		try {
			// Generate all possible combinations of instances for the general terms
			ArrayList<NamedList> listSet = new ArrayList<NamedList>();
			for (Frame genericTerm : genericReactants) {
				ArrayList<String> instancesOfGeneralTerm = new ArrayList<String>();
				for (Object instance : conn.getClassAllInstances(genericTerm.getLocalID())) instancesOfGeneralTerm.add(instance.toString());
				if (instancesOfGeneralTerm.size() == 0) instancesOfGeneralTerm.add(genericTerm.getLocalID());
				NamedList namedList = new NamedList(genericTerm.getLocalID(), instancesOfGeneralTerm);
				if (!listSet.contains(namedList)) listSet.add(namedList);
			}
			
			for (Frame genericTerm : genericProducts) {
				ArrayList<String> instancesOfGeneralTerm = new ArrayList<String>();
				for (Object instance : conn.getClassAllInstances(genericTerm.getLocalID())) instancesOfGeneralTerm.add(instance.toString());
				if (instancesOfGeneralTerm.size() == 0) instancesOfGeneralTerm.add(genericTerm.getLocalID());
				NamedList namedList = new NamedList(genericTerm.getLocalID(), instancesOfGeneralTerm);
				if (!listSet.contains(namedList)) listSet.add(namedList);
			}
			
			ListCombinationResults termCombinations = listCombinations(listSet);
			
			// Generate the Metabolite objects for reactants and products, which will be static across all new reactions
			ArrayList<MetaboliteInstance> reactantMetabolites = new ArrayList<MetaboliteInstance>();
			ArrayList<MetaboliteInstance> productMetabolites = new ArrayList<MetaboliteInstance>();
			for (Frame reactant : reactants) reactantMetabolites.add(generateMetabolite(origReaction, reactantSlot, reactant, reactant));
			for (Frame product : products) productMetabolites.add(generateMetabolite(origReaction, productSlot, product, product));
			
			// For each combination, create a new reaction for it if the reaction is elementally balanced
			for (ArrayList<String> combinationSet : termCombinations.listOfTuples) {
				ReactionInstance newReaction = new ReactionInstance(origReaction, null, "", origReaction.isReversible(), new ArrayList<MetaboliteInstance>(), new ArrayList<MetaboliteInstance>());
				ArrayList<String> reactantBalance = new ArrayList<String>();
				ArrayList<String> productBalance = new ArrayList<String>();
				
				// Non-generic metabolites
				for (MetaboliteInstance reactant : reactantMetabolites) {
					newReaction.reactants.add(reactant);
					int count = reactant.stoichiometry;
					while (count > 0) {
						reactantBalance.add(reactant.metabolite.getLocalID());
						count--;
					}
				}
				for (MetaboliteInstance product : productMetabolites) {
					newReaction.products.add(product);
					int count = product.stoichiometry;
					while (count > 0) {
						productBalance.add(product.metabolite.getLocalID());
						count--;
					}
				}

				// Generic metabolites
				for (Frame term : genericReactants) {
					MetaboliteInstance newMetabolite = generateMetabolite(origReaction, reactantSlot, term, loadFrame(combinationSet.get(termCombinations.nameList.indexOf(term.getLocalID()))));
					
					int count = newMetabolite.stoichiometry;
					while (count > 0) {
						reactantBalance.add(newMetabolite.metabolite.getLocalID());
						count--;
					}
					newReaction.reactants.add(newMetabolite);
				}
				for (Frame term : genericProducts) {
					MetaboliteInstance newMetabolite = generateMetabolite(origReaction, productSlot, term, loadFrame(combinationSet.get(termCombinations.nameList.indexOf(term.getLocalID()))));
					
					int count = newMetabolite.stoichiometry;
					while (count > 0) {
						productBalance.add(newMetabolite.metabolite.getLocalID());
						count--;
					}
					newReaction.products.add(newMetabolite);
				}
				
				String nameModifier = "";
				for (String term : combinationSet) nameModifier += term + "_";
				if (nameModifier.endsWith("_")) nameModifier = nameModifier.substring(0, nameModifier.length()-1);
				
				if (isReactionBalanced(reactantBalance, productBalance)) {
					newReaction.name = newReaction.parentReaction.getCommonName() + nameModifier;
					newReactions.add(newReaction);
				}
			}
			
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return newReactions;
	}
	
	/**
	 * "All possible combinations from a list of sublists problem"
	 * 
	 * This function takes in a list of lists and returns every possible combination of 1 item from each sublist.
	 * Thus, if the lists [1,2,3], [4,5,6], and [7,8,9] were input, then the output would be
	 * [1,4,7], [1,4,8], [1,4,8], [1,5,7], [1,5,8], [1,5,9] ...
	 * This method was written as a way to instantiate general terms in a reaction. Each general term in a reaction has 
	 * a list of possible values, and every possible combination of terms is needed.
	 * 
	 * @param listOfNamedLists List of NamedList objects. Name of list should be the class metabolite, while the list is
	 * each instance of the class metabolite.
	 * @return ListCombinationResults where the name list is a list of all the names of the NamedList input, and a list of tuples
	 * which represent each possible combination of the items in the named list. Order of names in the NameList matches the order
	 * of the items in the tuples.
	 */
	@SuppressWarnings("unchecked")
	private ListCombinationResults listCombinations(ArrayList<NamedList> listOfNamedLists) {
		if (listOfNamedLists == null || listOfNamedLists.size() < 1) return new ListCombinationResults(new ArrayList<String>(), new ArrayList<ArrayList<String>>());
		
		NamedList namedList = listOfNamedLists.remove(0);
		ListCombinationResults results = listCombinations(listOfNamedLists);
		results.nameList.add(namedList.name);
		ArrayList<ArrayList<String>> newListOfTuples = new ArrayList<ArrayList<String>>();
		
		if (results.listOfTuples.size() > 0) {
			for (String item : namedList.list) {
				for (ArrayList<String> tuple : results.listOfTuples) {
					ArrayList<String> newTuple = new ArrayList<String>();
					newTuple = (ArrayList<String>)tuple.clone();
					newTuple.add(item);
					newListOfTuples.add(newTuple);
				}
			}
		} else {
			for (String item : namedList.list) {
				ArrayList<String> tuple = new ArrayList<String>();
				tuple.add(item);
				newListOfTuples.add(tuple);
			}
		}
		
		results.listOfTuples = newListOfTuples;
		
		return results;
	}
	
	/**
	 * Intended for instantiating generic reactions. Creates a Metabolite object within the context of a reaction and another metabolite.
	 * Typical usage would be to provide a generic reaction, a class metabolite for that generic reaction, and an instance of the class
	 * metabolite. The result would be a Metabolite object borrowing the compartment and coefficient information from the original
	 * reaction-metabolite pair.
	 * 
	 * @param origReaction Original reaction, which combined with origMetabolite provides compartment and coefficient information for
	 * the resulting Metabolite object
	 * @param slot Slot of origReaction containing the origMetabolite (Usually either "RIGHT" or "LEFT")
	 * @param origMetabolite Original metabolite, which combined with origReaction provides compartment and coefficient information for
	 * the resulting Metabolite object
	 * @param newMetabolite Metabolite which will be entity on which the resulting Metabolite object will be based
	 * @return Metabolite object based on newMetabolite, but borrowing compartment and coefficient information from the origReaction-
	 * origMetabolite pair.
	 * @throws PtoolsErrorException
	 */
	private MetaboliteInstance generateMetabolite(Reaction origReaction, String slot, Frame origMetabolite, Frame newMetabolite) throws PtoolsErrorException {
		String compartment = conn.getValueAnnot(origReaction.getLocalID(), slot, origMetabolite.getLocalID(), "COMPARTMENT");
		if (compartment.equalsIgnoreCase("NIL")) compartment = defaultCompartment;
		
		int coeficient = 1;
		try {
			coeficient = Integer.parseInt(conn.getValueAnnot(origReaction.getLocalID(), slot, origMetabolite.getLocalID(), "COEFFICIENT"));
		} catch (Exception e) {
			coeficient = 1;
		}
		
		String chemicalFormula = getChemicalFormula(newMetabolite);
		
		return new MetaboliteInstance(newMetabolite, compartment, coeficient, chemicalFormula);
	}
	
	/**
	 * Creates exchange reactions for each metabolite that, for any reaction in reactions list, is also in compartment at least once.
	 * 
	 * @param compartment Compartment in which exchange reactions to metabolites will be created
	 * @param reactions List of all reaction in model, which provides metabolite and compartment information
	 * @return Exchange reactions
	 */
	private ArrayList<ReactionInstance> addBoundaryReactionsByCompartment(String compartment, ArrayList<ReactionInstance> reactions) {
		ArrayList<Frame> exchangeMetabolites = new ArrayList<Frame>();
		ArrayList<String> exchangeMetaboliteIDs = new ArrayList<String>();
		if (reactions == null) {// && generatedReactions == null) {
			//?
		}
		
		for (ReactionInstance reaction : reactions) {
			ArrayList<MetaboliteInstance> allReactants = reaction.reactants;
			ArrayList<MetaboliteInstance> allProducts = reaction.products;
			
			for (MetaboliteInstance reactant : allReactants) {
				if (reactant.compartment.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(reactant.metabolite.getLocalID())) {
					exchangeMetabolites.add(reactant.metabolite);
					exchangeMetaboliteIDs.add(reactant.metabolite.getLocalID());
				}
			}
			for (MetaboliteInstance product : allProducts) {
				if (product.compartment.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(product.metabolite.getLocalID())) {
					exchangeMetabolites.add(product.metabolite);
					exchangeMetaboliteIDs.add(product.metabolite.getLocalID());
				}
			}
		}
		
		// Generate exchange reactions
		ArrayList<ReactionInstance> exchangeReactions = new ArrayList<ReactionInstance>();
		for (Frame metabolite : exchangeMetabolites) {
			ArrayList<MetaboliteInstance> reactants = new ArrayList<MetaboliteInstance>();
			reactants.add(new MetaboliteInstance(metabolite, compartment, 1, getChemicalFormula(metabolite)));
			ArrayList<MetaboliteInstance> products = new ArrayList<MetaboliteInstance>();
			products.add(new MetaboliteInstance(metabolite, BoundaryCompartmentName, 1, getChemicalFormula(metabolite)));
			exchangeReactions.add(new ReactionInstance(null, null, metabolite.getLocalID() + "_" + ExchangeReactionSuffix, true, reactants, products));
		}
		
		return exchangeReactions;
	}
	
	/**
	 * TODO
	 */
	private ArrayList<ReactionInstance> addBoundaryReactionsByID(String boundaryCompartment, ArrayList<String> exchangeMetaboliteIDs) throws PtoolsErrorException {
		ArrayList<ReactionInstance> exchangeReactions = new ArrayList<ReactionInstance>();
//		for (String metaboliteID : exchangeMetaboliteIDs) {
//			Frame metabolite = Frame.load(conn, metaboliteID);
//			ArrayList<Metabolite> reactants = new ArrayList<Metabolite>();
//			reactants.add(new Metabolite(metabolite, boundaryCompartment, 1, getChemicalFormula(metabolite)));
//			ArrayList<Metabolite> products = new ArrayList<Metabolite>();
//			products.add(new Metabolite(metabolite, BoundaryCompartmentName, 1, getChemicalFormula(metabolite)));
//			exchangeReactions.add(new ReactionInstance(null, null, metabolite.getLocalID() + "_" + ExchangeReactionSuffix, true, reactants, products));
//		}
//		
		return exchangeReactions;
	}
	
	/**
	 * Populate an SBMLDocument object with reaction, metabolite, and compartments.
	 * 
	 * @param doc Empty SBMLDocument with initialized Model object containing only a model name and unit definitions.
	 * 
	 * @param reactionInstances Reactions which represent the complete reaction set for the model.
	 * @return SBMLDocument object with poplulated model.
	 */
	protected SBMLDocument generateSBMLModel(SBMLDocument doc, ArrayList<ReactionInstance> reactionInstances) {
		Model model = doc.getModel();
		ArrayList<String> metabolites = new ArrayList<String>();
		ArrayList<String> compartments = new ArrayList<String>();
		
		// Get mappings to iAF1260
//		HashMap<String, ArrayList<String>> map = readMap("/home/Jesse/output/e2p");
		
		try {
			// Create compartment list
			for (ReactionInstance reaction : reactionInstances) {
				ArrayList<MetaboliteInstance> reactantsProducts = new ArrayList<MetaboliteInstance>();
				reactantsProducts.addAll(reaction.reactants);
				reactantsProducts.addAll(reaction.products);
				for (MetaboliteInstance species : reactantsProducts) {
					if (!compartments.contains(species.compartment)) {
						Compartment compartment = model.createCompartment();
						compartment.setId(convertToSBMLSafe(species.compartment));
						compartment.setName(species.compartment);
//						if (compartment.setId(convertToSBMLSafe(species.compartment)) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (compartment.setName(species.compartment) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
						compartments.add(species.compartment);
					}
				}
			}
			
			// Create species list
			for (ReactionInstance reaction : reactionInstances) {
				ArrayList<MetaboliteInstance> reactantsProducts = new ArrayList<MetaboliteInstance>();
				reactantsProducts.addAll(reaction.reactants);
				reactantsProducts.addAll(reaction.products);
				for (MetaboliteInstance species : reactantsProducts) {
					if (!metabolites.contains(generateSpeciesID(species.metabolite.getLocalID(), species.compartment))) {
						Species newSpecies = model.createSpecies();
						String sid = generateSpeciesID(species.metabolite.getLocalID(), species.compartment);
						newSpecies.setId(sid);
						newSpecies.setName(species.metabolite.getCommonName());
						newSpecies.setCompartment(model.getCompartment(convertToSBMLSafe(species.compartment)).getId());
						newSpecies.setBoundaryCondition(false);
//						if (newSpecies.setId(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setName(species.metabolite.getCommonName()) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setCompartment(model.getCompartment(convertToSBMLSafe(species.compartment)).getId()) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setBoundaryCondition(false) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
						metabolites.add(sid);
						
						// Append Notes
						newSpecies.appendNotes("Palsson SID : \n");
						newSpecies.appendNotes("EcoCyc Frame ID : " + species.metabolite.getLocalID() + "\n");
						newSpecies.appendNotes("Chemical Formula : " + "\n");
					}
				}
			}
			
			// Create reaction list
			for (ReactionInstance reaction : reactionInstances) {
				org.sbml.libsbml.Reaction newReaction = model.createReaction();
				if (reaction.thisReactionFrame != null) newReaction.setId(generateReactionID(reaction.thisReactionFrame.getLocalID()));
				else if (reaction.parentReaction != null) newReaction.setId(generateReactionID(reaction.parentReaction.getLocalID()));
				else newReaction.setId(generateReactionID(reaction.name));
				newReaction.setName(reaction.name);
				newReaction.setReversible(reaction.reversible);
//				if (reaction.thisReactionFrame != null) {
//					if (newReaction.setId(convertToSBMLSafe(reaction.thisReactionFrame.getLocalID())) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				} else if (reaction.parentReaction != null) {
//					if (newReaction.setId(convertToSBMLSafe(reaction.parentReaction.getLocalID())) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				} else {
//					if (newReaction.setId(convertToSBMLSafe(reaction.name)) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				}
//				if (newReaction.setName(reaction.name) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				
				for (MetaboliteInstance reactant : reaction.reactants) {
					String sid = generateSpeciesID(reactant.metabolite.getLocalID(), reactant.compartment);
					SpeciesReference ref = newReaction.createReactant();
					ref.setSpecies(sid);
					ref.setStoichiometry(reactant.stoichiometry);
//					if (ref.setSpecies(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (ref.setStoichiometry(reactant.stoichiometry) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (newReaction.addReactant(ref) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				}
				for (MetaboliteInstance product : reaction.products) {
					String sid = generateSpeciesID(product.metabolite.getLocalID(), product.compartment);
					SpeciesReference ref = newReaction.createProduct();
					ref.setSpecies(sid);
					ref.setStoichiometry(product.stoichiometry);
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
				if (newReaction.getReversible()) lb.setValue(-1000);
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
				newReaction.appendNotes("Palsson Reaction ID : \n");
				newReaction.appendNotes("EcoCyc Frame ID : \n");
				newReaction.appendNotes("Abbreviation : \n");
				newReaction.appendNotes("Synonyms : \n");
				newReaction.appendNotes("EC Number : \n");
				newReaction.appendNotes("SUBSYSTEM : \n");
				newReaction.appendNotes("Equation : \n");
				newReaction.appendNotes("Confidence Level : \n");
				if (reaction.thisReactionFrame != null) newReaction.appendNotes("Gene Rule : " + reactionGeneRule(reaction.thisReactionFrame.getLocalID(), false));
				else if (reaction.parentReaction != null) newReaction.appendNotes("Gene Rule : " + reactionGeneRule(reaction.parentReaction.getLocalID(), false));
				else newReaction.appendNotes("Gene Rule : ");
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return doc;
	}
	
	/**
	 * Convert an ArrayList of Reaction objects into an ArrayList of ReactionInstances
	 * 
	 * @param reactions ArrayList of Reaction objects
	 * @return ArrayList of ReactionInstance objects
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<ReactionInstance> reactionListToReactionInstances(ArrayList<Reaction> reactions) {
		ArrayList<ReactionInstance> reactionInstances = new ArrayList<ReactionInstance>();
		try {
			for (Reaction reaction : reactions) {
				ReactionInstance reactionInstance = new ReactionInstance(null, reaction, reaction.getCommonName(), reaction.isReversible(), new ArrayList<MetaboliteInstance>(), new ArrayList<MetaboliteInstance>());
				String reactionReactantSlot = reactionReactantSlot(reaction);
				for (String reactant : (ArrayList<String>)reaction.getSlotValues(reactionReactantSlot)) {
					Frame reactantFrame = loadFrame(reactant);
					reactionInstance.reactants.add(generateMetabolite(reaction, reactionReactantSlot, reactantFrame, reactantFrame));
				}
				String reactionProductSlot = reactionProductSlot(reaction);
				for (String product : (ArrayList<String>)reaction.getSlotValues(reactionProductSlot)) {
					Frame productFrame = loadFrame(product);
					reactionInstance.products.add(generateMetabolite(reaction, reactionProductSlot, productFrame, productFrame));
				}
				
				reactionInstances.add(reactionInstance);
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		return reactionInstances;
	}
	
	/**
	 * Get slot name of Reaction reactants depending on reaction direction. Reversible reactions report reactant slot as "LEFT".
	 * 
	 * @param reaction
	 * @return
	 * @throws PtoolsErrorException
	 */
	private String reactionReactantSlot(Reaction reaction) throws PtoolsErrorException {
		if (reaction.getSlotValue("REACTION-DIRECTION") == null || !reaction.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
			return "LEFT";
		} else {
			return "RIGHT";
		}
	}
	
	/**
	 * Get slot name of Reaction products depending on reaction direction. Reversible reactions report product slot as "RIGHT".
	 * 
	 * @param reaction
	 * @return
	 * @throws PtoolsErrorException
	 */
	private String reactionProductSlot(Reaction reaction) throws PtoolsErrorException {
		if (reaction.getSlotValue("REACTION-DIRECTION") == null || !reaction.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
			return "RIGHT";
		} else {
			return "LEFT";
		}
	}
	
	/**
	 * Appends compartment appreviations to an SBML metabolite ID.
	 * 
	 * @param baseID
	 * @param compartment
	 * @return
	 */
	private String generateSpeciesID(String baseID, String compartment) {
		if (baseID.startsWith("_")) return convertToSBMLSafe(speciesPrefix + "" + baseID + "_" + compartmentAbrevs.get(compartment));
		else return convertToSBMLSafe(speciesPrefix + "_" + baseID + "_" + compartmentAbrevs.get(compartment));
	}
	
	/**
	 * A way to generate SBML reaction IDs for instantiated reactions.
	 * 
	 * @param baseID
	 * @return
	 */
	private String generateReactionID(String baseID) {
		if (baseID.startsWith("_")) return convertToSBMLSafe(reactionPrefix + "" + baseID);
		else return convertToSBMLSafe(reactionPrefix + "_" + baseID);
	}
	
	/**
	 * Gets the chemical formula from EcoCyc of given compound. Intended for use as a display string, not for elemental balancing.
	 * 
	 * Note: When comparing chemical formulae for elemental balancing, naming conventions in EcoCyc can differ from standard practice.
	 * This function will translate elements into standard one or two character symbols as found on a periodic table of elements. For
	 * example, EcoCyc lists Cobalt as "COBALT", which is otherwise normally shortened to the symbol "Co". This is also caps sensitive,
	 * as "CO" would stand for carbon and oxygen, rather than Co which stands for cobalt. Finally, elements with a a stoichiometry of 1
	 * do not add the 1 explicitly to the formula.
	 * 
	 * @param compound
	 * @return Chemical formula of the compound. Returns empty string if no formula information is in EcoCyc.
	 */
	protected String getChemicalFormula(Frame compound) {
		String chemicalFormula = "";
		try {
			if (!compound.hasSlot("CHEMICAL-FORMULA")) return "";
			for (Object o : compound.getSlotValues("CHEMICAL-FORMULA")) {
				String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
				String element = chemicalFormulaElement.split(",")[0];
				Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
				
				// Special Cases
				//TODO what is formula for ACP?
				if (element.equalsIgnoreCase("ACP")) element = "ACP";
				if (element.equalsIgnoreCase("COBALT")) element = "Co";
				if (element.equalsIgnoreCase("FE")) element = "Fe";
				if (element.equalsIgnoreCase("ZN")) element = "Zn";
				if (element.equalsIgnoreCase("SE")) element = "Se";
				if (element.equalsIgnoreCase("NI")) element = "Ni";
				if (element.equalsIgnoreCase("NA")) element = "Na";
				if (element.equalsIgnoreCase("MN")) element = "Mn";
				if (element.equalsIgnoreCase("MG")) element = "Mg";
				if (element.equalsIgnoreCase("HG")) element = "Hg";
				if (element.equalsIgnoreCase("CU")) element = "Cu";
				if (element.equalsIgnoreCase("CD")) element = "Cd";
				if (element.equalsIgnoreCase("CA")) element = "Ca";
				if (element.equalsIgnoreCase("AS")) element = "As";
				if (element.equalsIgnoreCase("CL")) element = "Cl";
				if (element.equalsIgnoreCase("AG")) element = "Ag";
				
				
				if (quantity != 1) chemicalFormula += element + quantity;
				else chemicalFormula += element;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return chemicalFormula;
	}
	
	/**
	 * Gets the Kegg ID of the compound.
	 * 
	 * @param compound
	 * @return Kegg ID of compound, empty string if no Kegg ID is found in EcoCyc for this compound.
	 */
	@SuppressWarnings("unchecked")
	private String getKeggID(Frame compound) {
		String keggID = "";
		try {
			ArrayList dblinks = null;
			if (compound.hasSlot("DBLINKS")) dblinks = compound.getSlotValues("DBLINKS");
			for (Object dblink : dblinks) {
				ArrayList<String> dbLinkArray = ((ArrayList<String>)dblink); 
				if (dbLinkArray.get(0).contains("LIGAND-CPD")) {
					keggID += dbLinkArray.get(1).replace("\"", "") + "\t";
				}
			}
			keggID = keggID.split("\t")[0]; // Many kegg id entries are duplicated in EcoCyc v15.0, but we only need one
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return keggID;
	}
	
	/**
	 * Test if a reaction is balanced by adding up each element on the reactant side and each element on the product side and
	 * comparing the quantity of each. If a reactant or product has a stoichiometry greater than |1|, then it should appear
	 * in the list as many times as its stoichiometric value. (i.e., if there are two waters on the reactant side, then H2O
	 * should be in reactantFormulas twice).
	 * 
	 * Note: This method does not interpret chemical shorthand (eg R-groups, etc). This method also assumes strict matching only,
	 * so a missing proton or water will result in a failed test for balance, even though these compounds can be assumed to be
	 * present to a trained biochemist. (Reactions missing a water or proton have been found in EcoCyc on occasion, although these
	 * instances are probably typos in EcoCyc).
	 * 
	 * Depricated by isReactionBalanced
	 * 
	 * @param reactantFormulas Chemical formula of each compound in a reactions reactant side.
	 * @param productFormulas Chemical formula of each compound in a reactions product side.
	 * @return Returns true if formulas are balanced, false if not.  Any errors or unreadable/missing formulas return false.
	 */
	private boolean isElementallyBalancedFormulas(ArrayList<String> reactantFormulas, ArrayList<String> productFormulas) {
		Pattern matchElement = Pattern.compile("\\A[A-Z][a-z]?");
		Pattern matchQuantity = Pattern.compile("\\A\\d+");
		HashMap<String, Integer> reactantElements = new HashMap<String, Integer>();
		HashMap<String, Integer> productElements = new HashMap<String, Integer>();
		try {
			for (String reactantFormula : reactantFormulas) {
				if (reactantFormula == null || reactantFormula.length() == 0) return false;
				
				while (reactantFormula.length() > 0) {
					Matcher m = matchElement.matcher(reactantFormula);
					String element = "";
					Integer quantity = 1;
					
					//Get element
					if (m.find()) {
						element = reactantFormula.substring(0, m.end());
						reactantFormula = reactantFormula.substring(m.end());
					} else return false;
					
					//Get quantity
					m = matchQuantity.matcher(reactantFormula);
					if (m.find()) {
						quantity = Integer.parseInt(reactantFormula.substring(0, m.end()));
						reactantFormula = reactantFormula.substring(m.end());
					} else quantity = 1;
					
					//Add to map
					if (reactantElements.containsKey(element)) {
						reactantElements.put(element, reactantElements.get(element) + quantity);
					} else {
						reactantElements.put(element, quantity);
					}
				}
			}
			for (String productFormula : productFormulas) {
				if (productFormula == null || productFormula.length() == 0) return false;
				
				while (productFormula.length() > 0) {
					Matcher m = matchElement.matcher(productFormula);
					String element = "";
					Integer quantity = 1;
					
					//Get element
					if (m.find()) {
						element = productFormula.substring(0, m.end());
						productFormula = productFormula.substring(m.end());
					} else return false;
					
					//Get quantity
					m = matchQuantity.matcher(productFormula);
					if (m.find()) {
						quantity = Integer.parseInt(productFormula.substring(0, m.end()));
						productFormula = productFormula.substring(m.end());
					} else quantity = 1;
					
					//Add to map
					if (productElements.containsKey(element)) {
						productElements.put(element, productElements.get(element) + quantity);
					} else {
						productElements.put(element, quantity);
					}
				}
			}
		} catch (Exception e) {
			return false;
		}
		
		if (!reactantElements.keySet().containsAll(productElements.keySet()) || !productElements.keySet().containsAll(reactantElements.keySet())) return false;
		for (String key : reactantElements.keySet()) {
//			if (key.equalsIgnoreCase("H")) {
//				if (reactantElements.get(key) - productElements.get(key) == 1 || reactantElements.get(key) - productElements.get(key) == -1) {
//					System.out.println("Save reaction with a proton.");
//				}
//			}
			if (reactantElements.get(key) != productElements.get(key)) return false;
		}
		
		return true;
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
	 * Test if a reaction is a generic reaction (i.e., it must contain at least one class frame in its reactions or products).
	 * 
	 * @param reactionName
	 * @return True if reaction is generic.
	 */
	@SuppressWarnings("unchecked")
	protected boolean isGeneralizedReaction(String reactionName) {
		boolean result = false;
		try {
			Reaction reaction = loadReaction(reactionName);
			ArrayList<String> leftMetabolites = reaction.getSlotValues("LEFT");
			ArrayList<String> rightMetabolites = reaction.getSlotValues("RIGHT");
			
			for (String left : leftMetabolites) {
				if (conn.getFrameType(left).toUpperCase().equals(":CLASS")) return true;
			}
			
			for (String right : rightMetabolites) {
				if (conn.getFrameType(right).toUpperCase().equals(":CLASS")) return true;
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Test if a reaction is a generic reaction (i.e., it must contain at least one class frame in its reactions or products).
	 * 
	 * @param reaction
	 * @return True if reaction is generic.
	 */
	@SuppressWarnings("unchecked")
	protected boolean isGeneralizedReaction(Reaction reaction) {
		boolean result = false;
		try {
			ArrayList<String> leftMetabolites = reaction.getSlotValues("LEFT");
			ArrayList<String> rightMetabolites = reaction.getSlotValues("RIGHT");
			
			for (String left : leftMetabolites) {
				if (conn.getFrameType(left).toUpperCase().equals(":CLASS")) return true;
			}
			
			for (String right : rightMetabolites) {
				if (conn.getFrameType(right).toUpperCase().equals(":CLASS")) return true;
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Test if a metabolite is a generic metabolite (i.e., it must be a class frame).
	 * 
	 * @param metaboliteName
	 * @return True if metabolite is generic.
	 */
	private boolean isGeneralizedMetabolite(String metaboliteName) {
		boolean result = false;
		try {
			if (conn.getFrameType(metaboliteName).toUpperCase().equals(":CLASS")) result = true;
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * TODO Transfer logic into isElementallyBalancedFormulas? (consolidate these two methods), use this one as an overload option?
	 * 
	 * Test if a reaction is balanced by adding up each element on the reactant side and each element on the product side and
	 * comparing the quantity of each. If a reactant or product has a stoichiometry greater than |1|, then it should appear
	 * in the list as many times as its stoichiometric value. (i.e., if there are two waters on the reactant side, then H2O
	 * should be in reactantFormulas twice).
	 * 
	 * Note: This method does not interpret chemical shorthand (eg R-groups, etc). This method also assumes strict matching only,
	 * so a missing proton or water will result in a failed test for balance, even though these compounds can be assumed to be
	 * present to a trained biochemist. (Reactions missing a water or proton have been found in EcoCyc on occasion, although these
	 * instances are probably typos in EcoCyc).
	 * 
	 * @param reactantIDs Frame IDs of reactants.
	 * @param productIDs Frame IDs of products.
	 * @return Returns true if reactants and products are elementally balanced, false if not.
	 * Any errors or unreadable/missing formulas return false.
	 */
	protected boolean isReactionBalanced(ArrayList<String> reactantIDs, ArrayList<String> productIDs) {
		// If a reactant or product has a stoichiometry greater than |1|, then it should appear in the list as many times as its stoich value
		// This method does not interpret chemical shorthand (eg R-groups, etc).
		// Returns true if successful, false if not.  Any errors or unreadable/missing formulas return false.
		
		HashMap<String, Integer> reactantElements = new HashMap<String, Integer>();
		HashMap<String, Integer> productElements = new HashMap<String, Integer>();
		try {
			for (String reactant : reactantIDs) {
				// Special Case
				int specialCases = 0;
				if (reactant.equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (reactant.equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
				switch (specialCases) {
					case 1: {
						if (reactantElements.containsKey("A")) {
							reactantElements.put("A", reactantElements.get("A") + 1);
						} else {
							reactantElements.put("A", 1);
						}
					} break;
					case 2: {
						if (reactantElements.containsKey("A")) {
							reactantElements.put("A", reactantElements.get("A") + 1);
						} else {
							reactantElements.put("A", 1);
						}
						if (reactantElements.containsKey("H")) {
							reactantElements.put("H", reactantElements.get("H") + 2);
						} else {
							reactantElements.put("H", 2);
						}
					} break;
				}
				if (specialCases != 0) {
//					System.out.println("Special Case handled");
					continue;
				}
				
				// Regular Case
				Compound reactantFrame = loadCompound(reactant);
				
				for (Object o : reactantFrame.getSlotValues("CHEMICAL-FORMULA")) {
					String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
					String element = chemicalFormulaElement.split(",")[0];
					Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
					
					//Add to map
					if (reactantElements.containsKey(element)) {
						reactantElements.put(element, reactantElements.get(element) + quantity);
					} else {
						reactantElements.put(element, quantity);
					}
				}
			}
			for (String product : productIDs) {
				// Special Case
				int specialCases = 0;
				if (product.equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (product.equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
				switch (specialCases) {
					case 1: {
						if (productElements.containsKey("A")) {
							productElements.put("A", productElements.get("A") + 1);
						} else {
							productElements.put("A", 1);
						}
					} break;
					case 2: {
						if (productElements.containsKey("A")) {
							productElements.put("A", productElements.get("A") + 1);
						} else {
							productElements.put("A", 1);
						}
						if (productElements.containsKey("H")) {
							productElements.put("H", productElements.get("H") + 2);
						} else {
							productElements.put("H", 2);
						}
					} break;
				}
				if (specialCases != 0) {
//					System.out.println("Special Case handled");
					continue;
				}
				
				// Regular Case
				Compound productFrame = loadCompound(product);
				
				for (Object o : productFrame.getSlotValues("CHEMICAL-FORMULA")) {
					String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
					String element = chemicalFormulaElement.split(",")[0];
					Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
					
					//Add to map
					if (productElements.containsKey(element)) {
						productElements.put(element, productElements.get(element) + quantity);
					} else {
						productElements.put(element, quantity);
					}
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			return false;
		}
		
		if (!reactantElements.keySet().containsAll(productElements.keySet()) || !productElements.keySet().containsAll(reactantElements.keySet())) return false;
		for (String key : reactantElements.keySet()) {
//			if (key.equalsIgnoreCase("H")) {
//				if (reactantElements.get(key) - productElements.get(key) == 1 || reactantElements.get(key) - productElements.get(key) == -1) {
//					System.out.println("Save reaction with a proton.");
//				}
//			}
			if (reactantElements.get(key) != productElements.get(key)) return false;
		}
		
		return true;
	}
	
	/**
	 * Replace characters that are commonly used in EcoCyc with characters safe to use in SBML names and IDs.
	 * 
	 * @param input
	 * @return
	 */
	private String convertToSBMLSafe(String input) {
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
	 * TODO Remove and switch to Frame.load() 
	 * 
	 * @param id
	 * @return
	 */
	protected Frame loadFrame(String id) {
		Frame frame = new Frame(conn, id);
		try {
			if (frame.inKB()) return frame;
			else if (!id.startsWith("|") && !id.endsWith("|")) {
				Frame classFrame = new Frame(conn, "|"+id+"|");
				if (classFrame.inKB()) return classFrame;
			} else if (id.startsWith("|") && id.endsWith("|")) {
				Frame instanceFrame = new Frame(conn, id.substring(1, id.length()-1));
				if (instanceFrame.inKB()) return instanceFrame;
			}
		} catch (PtoolsErrorException e) {
			System.err.println("Error: Unable to load frame " + id);
		}
		return null;
	}
	
	/**
	 * TODO Remove and switch to Frame.load() 
	 * 
	 * @param id
	 * @return
	 */
	private Compound loadCompound(String id) throws PtoolsErrorException {
		Compound f = new Compound(conn, id);
		if (f.inKB()) return f;
		else return null;
	}
	
	/**
	 * TODO Remove and switch to Frame.load() 
	 * 
	 * @param id
	 * @return
	 */
	protected Reaction loadReaction(String id) throws PtoolsErrorException {
		Reaction f = new Reaction(conn, id);
		if (f.inKB()) return f;
		else return null;
	}
	
	/**
	 * TODO Remove and switch to Frame.load() 
	 * 
	 * @param id
	 * @return
	 */
	private Pathway loadPathway(String id) throws PtoolsErrorException {
		Pathway f = new Pathway(conn, id);
		if (f.inKB()) return f;
		else return null;
	}
	
	/**
	 * Return gene-reaction associations as a string of EcoCyc gene frame IDs in a boolean logic format. This format
	 * is intended for inclusion in SBML models.
	 * 
	 * In this format, the presence of each gene could be represented by TRUE and its absence by FALSE,
	 * and if the statement resolves to TRUE using the rules of boolean logic, than the reaction can be
	 * considered to be functional.
	 * 
	 * Example: Reaction F16BDEPHOS-RXN (FRUCTOSE-1,6-BISPHOSPHATASE) is governed by 4 genes, each of which will produce
	 * an enzyme capable of catalyzing this reaction, thus any one gene is sufficient.  The rule is then something like
	 * (yggF or ybhA or glpX or fbp).
	 * 
	 * Example: Reaction SUCCCOASYN-RXN (SUCCINYL-COA SYNTHETASE) is governed by 2 genes, both of which are required
	 * to produce the enzyme capable of catalyzing this reaction, thus both are necessary.  The rule is then something like
	 * (sucC and sucD).
	 * 
	 * @param reactionID EcoCyc reaction frame ID
	 * @param asBNumber If true, return string with gene b#'s instead of gene frame IDs
	 * @return String of gene-reaction associations
	 * @throws PtoolsErrorException
	 */
	private String reactionGeneRule(String reactionID, boolean asBNumber) throws PtoolsErrorException {
		String orRule = "";
		for (Object enzyme : conn.enzymesOfReaction(reactionID)) {
			String andRule = "";
			for (Object gene : conn.genesOfProtein(enzyme.toString())) {
				String geneID = gene.toString();
				if (asBNumber) {
					try {
						geneID = loadFrame(geneID).getSlotValue("ACCESSION-1").replace("\"", "");
					} catch (Exception e) {
						geneID = gene.toString();
					}
				}
				andRule += geneID + " and ";
			}
			if (andRule.length() > 0) {
				andRule = "(" + andRule.substring(0, andRule.length()-5) + ")";
				orRule += andRule + " or ";
			}
		}
		if (orRule.length() > 0) orRule = orRule.substring(0, orRule.length()-4);
		return orRule;
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
	
	
	
	
	
	// *** SANDBOX *** \\
	private void printBoundaryReactionMetaboliteList(ArrayList<ReactionInstance> reactions, String fileName) {
		String outString = "";
		try {
			for (ReactionInstance reaction : reactions) {
				if (reaction.reactants.size() > 0) {
					Frame m = reaction.reactants.get(0).metabolite;
					outString += m.getLocalID() + "\t" + m.getCommonName() + "\t" + reaction.reactants.get(0).chemicalFormula + "\t";
					
					String keggID = "";
					ArrayList dblinks = null;
					if (m.hasSlot("DBLINKS")) dblinks = m.getSlotValues("DBLINKS");
					for (Object dblink : dblinks) {
						ArrayList<String> dbLinkArray = ((ArrayList<String>)dblink); 
						if (dbLinkArray.get(0).contains("LIGAND-CPD")) {
							keggID += dbLinkArray.get(1).replace("\"", "") + "\t";
						}
					}
					keggID = keggID.split("\t")[0]; // Many kegg id entries are duplicated in EcoCyc v15.0, but we only need one
					outString += keggID + "\n";
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		printString(OutputDirectory + fileName, outString);
	}

	
	
	
	
 	// Internal Classes
	/**
	 * Internal class to hold the results of filtering reactions to be excluded from the reaction set.
	 * 
	 * @author Jesse
	 */
 	private class FilterResults {
		public ArrayList<Reaction> keepList;
		public ArrayList<Reaction> removedList;
		
		public FilterResults(ArrayList<Reaction> keepList, ArrayList<Reaction> removedList) {
			this.keepList = keepList;
			this.removedList = removedList;
		}
	}
 	
 	/**
 	 * Internal class to facilitate generic reaction instantiation by holding a metabolite class as "name" and all
 	 * metabolite instances of the class in "list".
 	 * 
 	 * @author Jesse
 	 */
 	private class NamedList {
		public String name;
		public ArrayList<String> list;
		
		public NamedList(String name, ArrayList<String> list) {
			this.name = name;
			this.list = list;
		}
		
		/**
		A shallow test of equality. Test the names of two NamedLists for equality. Does not compare the list itself.
		@return true if both NamedLists have the name. 
		*/
		@Override public boolean equals(Object aThat) {
			//Based on example at http://www.javapractices.com/topic/TopicAction.do?Id=17
			
		    //Check for self-comparison
		    if (this == aThat) return true;

		    //Check for similar class
		    if (!(aThat instanceof NamedList)) return false;
		    
		    //Cast to native type
		    NamedList that = (NamedList)aThat;

		    //Compare frame IDs
		    return this.name.equals(that.name);
		  }

		@Override public int hashCode() {
			return this.name.hashCode();
		  }
	}
 	
 	/**
 	 * Internal class to facilitate generic reaction instantiation by holding the results of the listCombinations method.
 	 * 
 	 * @author Jesse
 	 */
 	private class ListCombinationResults {
 		public ArrayList<String> nameList;
		public ArrayList<ArrayList<String>> listOfTuples;
		
		public ListCombinationResults(ArrayList<String> nameList, ArrayList<ArrayList<String>> listOfTuples) {
			this.nameList = nameList;
			this.listOfTuples = listOfTuples;
		}
 	}
 		
 	/**
 	 * Internal class which holds results of an attempt to instantiate generic reactions in a list of reactions.
 	 * 
 	 * @author Jesse
 	 */
 	private class InstantiationResults {
 		public ArrayList<ReactionInstance> instantiatedReactions;
 		public ArrayList<Frame> genericReactionsFound;
 		public ArrayList<Frame> genericReactionsFailedToInstantiate;
		
		public InstantiationResults(ArrayList<ReactionInstance> instantiatedReactions, ArrayList<Frame> genericReactionsFound, ArrayList<Frame> genericReactionsFailedToInstantiate) {
			this.instantiatedReactions = instantiatedReactions;
			this.genericReactionsFound = genericReactionsFound;
			this.genericReactionsFailedToInstantiate = genericReactionsFailedToInstantiate;
		}
 	}
}
