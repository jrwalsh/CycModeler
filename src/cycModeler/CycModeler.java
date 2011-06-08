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
	private JavacycConnection conn = null;
	private String OutputDirectory = "";
	private String defaultCompartment = "CCO-CYTOSOL";
	private int DefaultSBMLLevel = 2;
	private int DefaultSBMLVersion = 1;
	private HashMap<String, String> compartmentAbrevs = new HashMap<String, String>();
	private String speciesPrefix = "M";
	private String reactionPrefix = "R";
	private String BoundaryCompartmentName = "Boundary";
	private String ExchangeReactionSuffix = "exchange";
	
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
	
	
	// Testing
	/**
	 * Internal testing method. Used for manual testing of methods in this class
	 * @param mode Select test to perform.
	 */
	public void sbmlInteralFunctionTests(int mode) {
		switch (mode) {
			case 40: {
				// Check behavior of the isReactionBalanced function
				ArrayList<String> reacs = new ArrayList<String>();
				ArrayList<String> prods =  new ArrayList<String>();
				reacs.add("GLC");
				reacs.add("GAP");
				prods.add("GAP");
				prods.add("GLC");
				System.out.println(isReactionBalanced(reacs, prods));
			} break;
			case 60: {
				// Check if a pathway contains a general term
				isGeneralizedReaction("MALATE-DEHYDROGENASE-ACCEPTOR-RXN");
				isGeneralizedReaction("SUCCINATE-DEHYDROGENASE-UBIQUINONE-RXN");
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
				createGenomeScaleModelFromEcoCyc();
			} break;
			case 210: {
				try {
					System.out.println(prepareGenericReaction(loadReaction("ABC-56-RXN")).size());
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
					ReactionInstance rxn = new ReactionInstance(null, loadReaction("PGLUCISOM-RXN"), "NamedReaction", false, new ArrayList<Metabolite>(), new ArrayList<Metabolite>());
					rxn.reactants.add(new Metabolite(loadFrame("GLC-6-P"), defaultCompartment, 1, getChemicalFormula(loadFrame("GLC-6-P"))));
					rxn.products.add(new Metabolite(loadFrame("FRUCTOSE-6P"), defaultCompartment, 1, getChemicalFormula(loadFrame("FRUCTOSE-6P"))));
					rxns.add(rxn);
					SBMLDocument doc = createBlankSBMLDocument("Testing", 2, 1);
					doc = generateSBMLModel(doc, rxns);
					SBMLWriter writer = new SBMLWriter();
					writer.writeSBML(doc, OutputDirectory + "testing_SBML.xml");
				} catch (PtoolsErrorException e) {
					e.printStackTrace();
				}
			} break;
			case 230: {
				readInPalssonIDMaps("/home/Jesse/Desktop/compare_palsson_ecocyc/iAF1260-ecocyc-rxn-mappings.txt");
			} break;
			case 240: {
				verifyCompoundMappings();
			} break;
			case 250: {
//				verifyReactionMappings();
				coreReactionTest();
			} break;
		}
	}
	
	
	// Methods
	/**
	 * Initializes default settings for generating SBML models and for translating information from EcoCyc for use
	 * in the SBML models.
	 */
	public void setDefaults() {
		//TODO read from a config file?
		OutputDirectory = "/home/Jesse/Desktop/output/";
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
			
			// 7) Write revised model.
			System.out.println("Writing output ...");
			SBMLWriter writer = new SBMLWriter();
			writer.writeSBML(doc, OutputDirectory + "written_SBML.xml");
			
			// *Mapping*
			System.out.println("Writing mapping output ...");
			printBoundaryReactionMetaboliteList(boundaryResults, "boundaryMetabolites");
			
			
			// Print statistics
//			System.out.println("Writing statistics ...");
//			System.out.println("All reactions : " + allReactions.size());
//			System.out.println("Filtered reactions keeplist : " + filterResults.keepList.size());
//			System.out.println("Filtered reactions tosslist : : " + filterResults.removedList.size());
//			System.out.println("Generic reactions found : " + instantiationResults.genericReactionsFound.size());
//			System.out.println("Generic reactions failed to instantiate : " + instantiationResults.genericReactionsFailedToInstantiate.size());
//			System.out.println("New reactions from generic reaction instantiations : " + instantiationResults.instantiatedReactions.size());
//			System.out.println("Generic keeplist : " + genericReactionFilterResults.keepList.size());
//			System.out.println("Generic tosslist : " + genericReactionFilterResults.removedList.size());
//			System.out.println("Boundary reactions added : " + boundaryResults.size());
//			int grandTotal = instantiationResults.instantiatedReactions.size() + genericReactionFilterResults.keepList.size() + boundaryResults.size();
//			System.out.println("Grand total : " + grandTotal);
			
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
	private ArrayList<ReactionInstance> prepareGenericReaction(Reaction reaction) {
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
			
			// Generate the Metabolite objects for reactants and products, which will be static accross all new reactions
			ArrayList<Metabolite> reactantMetabolites = new ArrayList<Metabolite>();
			ArrayList<Metabolite> productMetabolites = new ArrayList<Metabolite>();
			for (Frame reactant : reactants) reactantMetabolites.add(generateMetabolite(origReaction, reactantSlot, reactant, reactant));
			for (Frame product : products) productMetabolites.add(generateMetabolite(origReaction, productSlot, product, product));
			
			// For each combination, create a new reaction for it if the reaction is elementally balanced
			for (ArrayList<String> combinationSet : termCombinations.listOfTuples) {
				ReactionInstance newReaction = new ReactionInstance(origReaction, null, "", origReaction.isReversible(), new ArrayList<Metabolite>(), new ArrayList<Metabolite>());
				ArrayList<String> reactantBalance = new ArrayList<String>();
				ArrayList<String> productBalance = new ArrayList<String>();
				
				// Non-generic metabolites
				for (Metabolite reactant : reactantMetabolites) {
					newReaction.reactants.add(reactant);
					int count = reactant.stoichiometry;
					while (count > 0) {
						reactantBalance.add(reactant.metabolite.getLocalID());
						count--;
					}
				}
				for (Metabolite product : productMetabolites) {
					newReaction.products.add(product);
					int count = product.stoichiometry;
					while (count > 0) {
						productBalance.add(product.metabolite.getLocalID());
						count--;
					}
				}

				// Generic metabolites
				for (Frame term : genericReactants) {
					Metabolite newMetabolite = generateMetabolite(origReaction, reactantSlot, term, loadFrame(combinationSet.get(termCombinations.nameList.indexOf(term.getLocalID()))));
					
					int count = newMetabolite.stoichiometry;
					while (count > 0) {
						reactantBalance.add(newMetabolite.metabolite.getLocalID());
						count--;
					}
					newReaction.reactants.add(newMetabolite);
				}
				for (Frame term : genericProducts) {
					Metabolite newMetabolite = generateMetabolite(origReaction, productSlot, term, loadFrame(combinationSet.get(termCombinations.nameList.indexOf(term.getLocalID()))));
					
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
					tuple.add(item);
					newListOfTuples.add((ArrayList<String>)tuple.clone());
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
	private Metabolite generateMetabolite(Reaction origReaction, String slot, Frame origMetabolite, Frame newMetabolite) throws PtoolsErrorException {
		String compartment = conn.getValueAnnot(origReaction.getLocalID(), slot, origMetabolite.getLocalID(), "COMPARTMENT");
		if (compartment.equalsIgnoreCase("NIL")) compartment = defaultCompartment;
		
		int coeficient = 1;
		try {
			coeficient = Integer.parseInt(conn.getValueAnnot(origReaction.getLocalID(), slot, origMetabolite.getLocalID(), "COEFFICIENT"));
		} catch (Exception e) {
			coeficient = 1;
		}
		
		String chemicalFormula = getChemicalFormula(newMetabolite);
		
		return new Metabolite(newMetabolite, compartment, coeficient, chemicalFormula);
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
			ArrayList<Metabolite> allReactants = reaction.reactants;
			ArrayList<Metabolite> allProducts = reaction.products;
			
			for (Metabolite reactant : allReactants) {
				if (reactant.compartment.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(reactant.metabolite.getLocalID())) {
					exchangeMetabolites.add(reactant.metabolite);
					exchangeMetaboliteIDs.add(reactant.metabolite.getLocalID());
				}
			}
			for (Metabolite product : allProducts) {
				if (product.compartment.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(product.metabolite.getLocalID())) {
					exchangeMetabolites.add(product.metabolite);
					exchangeMetaboliteIDs.add(product.metabolite.getLocalID());
				}
			}
		}
		
		// Generate exchange reactions
		ArrayList<ReactionInstance> exchangeReactions = new ArrayList<ReactionInstance>();
		for (Frame metabolite : exchangeMetabolites) {
			ArrayList<Metabolite> reactants = new ArrayList<Metabolite>();
			reactants.add(new Metabolite(metabolite, compartment, 1, getChemicalFormula(metabolite)));
			ArrayList<Metabolite> products = new ArrayList<Metabolite>();
			products.add(new Metabolite(metabolite, BoundaryCompartmentName, 1, getChemicalFormula(metabolite)));
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
	private SBMLDocument generateSBMLModel(SBMLDocument doc, ArrayList<ReactionInstance> reactionInstances) {
		Model model = doc.getModel();
		ArrayList<String> metabolites = new ArrayList<String>();
		ArrayList<String> compartments = new ArrayList<String>();
		
		// Get mappings to iAF1260
//		HashMap<String, ArrayList<String>> map = readMap("/home/Jesse/output/e2p");
		
		try {
			// Create compartment list
			for (ReactionInstance reaction : reactionInstances) {
				ArrayList<Metabolite> reactantsProducts = new ArrayList<Metabolite>();
				reactantsProducts.addAll(reaction.reactants);
				reactantsProducts.addAll(reaction.products);
				for (Metabolite species : reactantsProducts) {
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
				ArrayList<Metabolite> reactantsProducts = new ArrayList<Metabolite>();
				reactantsProducts.addAll(reaction.reactants);
				reactantsProducts.addAll(reaction.products);
				for (Metabolite species : reactantsProducts) {
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
				
				for (Metabolite reactant : reaction.reactants) {
					String sid = generateSpeciesID(reactant.metabolite.getLocalID(), reactant.compartment);
					SpeciesReference ref = newReaction.createReactant();
					ref.setSpecies(sid);
					ref.setStoichiometry(reactant.stoichiometry);
//					if (ref.setSpecies(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (ref.setStoichiometry(reactant.stoichiometry) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (newReaction.addReactant(ref) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				}
				for (Metabolite product : reaction.products) {
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
				ReactionInstance reactionInstance = new ReactionInstance(null, reaction, reaction.getCommonName(), reaction.isReversible(), new ArrayList<Metabolite>(), new ArrayList<Metabolite>());
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
	private String getChemicalFormula(Frame compound) {
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
	private boolean isGeneralizedReaction(String reactionName) {
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
	private boolean isGeneralizedReaction(Reaction reaction) {
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
	private boolean isReactionBalanced(ArrayList<String> reactantIDs, ArrayList<String> productIDs) {
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
	private Frame loadFrame(String id) {
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
	private Reaction loadReaction(String id) throws PtoolsErrorException {
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
	// Try to generate a mapping file between palsson and ecocyc given prior mappings and new mappings
	private void verifyCompoundMappings() {
		String output = "";
		String fileName = "/home/Jesse/Desktop/ecocyc_model/mapping/iAF1260-ecocyc-cpd-mappings.txt";
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			String header = reader.readLine();
			output += header;
			output += "\tecocyc_CommonName\tecocyc_formula\tecocyc_KeggID\tecocyc_Synonyms\tformulaMatch\tkeggMatch\n";
			
			while ((text = reader.readLine()) != null) {
				String[] line = text.split("\t", 11);
				
				// palsson info
				String abbreviation = line[0];
				String officialName = line[1];
				String formula = line[2];
				String charge = line[3];
				String casNumber = line[4];
				String compoundNames = line[5];
				String keggID = line[6];
				String ecocycid = line[7];
				String analysis = line[8];
				String metacycid = line[9];
				String notes = line[10];
				
				output += text;
				
				// EcoCyc ID Special Cases
				ecocycid = ecocycid.replace("\"", "");
				if (ecocycid.equalsIgnoreCase("|PROPANE-1,2-DIOL|")) ecocycid = "PROPANE-1-2-DIOL";
				if (ecocycid.equalsIgnoreCase("|l-delta(1)-pyrroline_5-carboxylate|")) ecocycid = "L-DELTA1-PYRROLINE_5-CARBOXYLATE";
				if (ecocycid.equalsIgnoreCase("|2,3-dihydrodipicolinate|")) ecocycid = "2-3-DIHYDRODIPICOLINATE";
				if (ecocycid.equalsIgnoreCase("5K-GLUCONATE")) ecocycid = "5-DEHYDROGLUCONATE";
				if (ecocycid.equalsIgnoreCase("D-CARNITINYL-COA")) ecocycid = "L-CARNITINYL-COA";
				if (ecocycid.equalsIgnoreCase("|7,8-dihydropteroate|")) ecocycid = "7-8-DIHYDROPTEROATE";
				if (ecocycid.equalsIgnoreCase("GAMMA-GLUTAMYL-GAMMA-AMINOBUTYRATE")) ecocycid = "CPD-9000";
				if (ecocycid.equalsIgnoreCase("|threo-d(s)-iso-citrate|")) ecocycid = "THREO-DS-ISO-CITRATE";
				if (ecocycid.equalsIgnoreCase("|delta(3)-isopentenyl-pp|")) ecocycid = "DELTA3-ISOPENTENYL-PP";
				if (ecocycid.equalsIgnoreCase("|5,10-methenyl-thf|")) ecocycid = "5-10-METHENYL-THF";
				if (ecocycid.equalsIgnoreCase("L-METHIONINE_SULFOXIDE")) ecocycid = "L-Methionine-sulfoxides";
				if (ecocycid.equalsIgnoreCase("VITAMIN_K_{2}")) ecocycid = "CPD-9728";
				if (ecocycid.equalsIgnoreCase("PAP")) ecocycid = "3-5-ADP";
				if (ecocycid.equalsIgnoreCase("|N-(5-PHOSPHORIBOSYL)-ANTHRANILATE|")) ecocycid = "N-5-PHOSPHORIBOSYL-ANTHRANILATE";
				if (ecocycid.equalsIgnoreCase("UBIQUINOL-8")) ecocycid = "CPD-9956";
				if (ecocycid.equalsIgnoreCase("CPD-249")) ecocycid = "Elemental-Sulfur";
				if (ecocycid.equalsIgnoreCase("O-SUCCINYLBENZOYL-COA")) ecocycid = "CPD-6972";
				if (ecocycid.equalsIgnoreCase("|n-succinylll-2,6-diaminopimelate|")) ecocycid = "N-SUCCINYLLL-2-6-DIAMINOPIMELATE";
				if (ecocycid.equalsIgnoreCase("SUCCINATE-SEMIALDEHYDE-THIAMINE-PPI")) ecocycid = "CPD0-2102";
				if (ecocycid.equalsIgnoreCase("DELTA{1}-PIPERIDEINE-2-6-DICARBOXYLATE")) ecocycid = "DELTA1-PIPERIDEINE-2-6-DICARBOXYLATE";
				if (ecocycid.equalsIgnoreCase("UDP-ACETYLMURAMOYL-ALA")) ecocycid = "CPD0-1456";
				if (ecocycid.equalsIgnoreCase("GLCNAC-PP-LIPID")) ecocycid = "ACETYL-D-GLUCOSAMINYLDIPHOSPHO-UNDECAPRE";
				
				//TODO
				if (ecocycid.equalsIgnoreCase("|2,3-diketo-l-gulonate|")) ecocycid = "|2,3-diketo-l-gulonate|";
				if (ecocycid.equalsIgnoreCase("3-OHMYRISTOYL-ACP")) ecocycid = "3-OHMYRISTOYL-ACP";
				
				// Eco info
				Frame compound = null;
				String ecoCommonName = "";
				String ecoChemicalFormula = "";
				String ecoKeggID = "";
				String ecoSynonyms = "";
				boolean formulaMatch = false;
				boolean keggIDMatch = false;
				if (ecocycid.length() > 0) compound = loadFrame(ecocycid);
				if (compound != null) {
					ecoCommonName = compound.getCommonName();
					ecoChemicalFormula = getChemicalFormula(compound);
					ecoKeggID = getKeggID(compound);
//					ecoSynonyms = compound.getSynonyms();
					ArrayList<String> palssonFormula = new ArrayList<String>();
					ArrayList<String> ecoFormula = new ArrayList<String>();
					palssonFormula.add(formula);
					ecoFormula.add(ecoChemicalFormula);
					if (isElementallyBalancedFormulas(palssonFormula, ecoFormula)) formulaMatch = true;
					if (keggID.equalsIgnoreCase(ecoKeggID)) keggIDMatch = true;
				} else if (ecocycid.length() > 0) System.out.println("Can't get compound : " + ecocycid);
				output += "\t";
				output += ecoCommonName+"\t";
				output += ecoChemicalFormula+"\t";
				output += ecoKeggID+"\t";
				output += ecoSynonyms+"\t";
				output += formulaMatch+"\t";
				output += keggIDMatch;
				output += "\n";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		printString("/home/Jesse/Desktop/ecocyc_model/mapping/cmpMappings.txt", output);
	}
	
	private void verifyReactionMappings() {
		String output = "";
		String fileName = "/home/Jesse/Desktop/ecocyc_model/mapping/iAF1260-ecocyc-rxn-mappings.txt";
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			String header = reader.readLine();
			output += header.trim();
			output += "\tecocycID\tecoCommonName\tecoEC\tecoGene\tecoSynonyms\tECMatch\tbNumMatch\tisClass\n";
			
			while ((text = reader.readLine()) != null) {
				String[] line = text.split("\t", 11);
				
				// palsson info
				String abbreviation = line[0];
				String officialName = line[1];
				String equation = line[2];
				String geneAssociation = line[3];
				String proteinClass = line[4];
				String ecocycrxnids = line[5];
				String analysis = line[6];
				String notes = line[7];
				
				// Special Cases
				if (analysis.contains(":DIFFUSION") || analysis.contains(":EXCHANGE")) continue;
				
				output += abbreviation+"\t";
				output += officialName+"\t";
				output += equation+"\t";
				output += geneAssociation+"\t";
				output += proteinClass+"\t";
				output += ecocycrxnids+"\t";
				output += analysis+"\t";
				output += notes;
				
				// Split ids
				ArrayList<String> ecocycIDs = new ArrayList<String>();
				if (ecocycrxnids.length() > 0) {
					ecocycrxnids = ecocycrxnids.substring(1, ecocycrxnids.length()-1);
					for (String id : ecocycrxnids.split(" ")) ecocycIDs.add(id);
				} 
				
				// Eco info
				String ecoOutput = "";
				int count = ecocycIDs.size();
				for (String ecocycid : ecocycIDs) {
					Reaction reaction = null;
					String ecoCommonName = "";
					String ecoEC = "";
					String ecoGene = "";
					String ecoSynonyms = "";
					boolean ECMatch = false;
					boolean bNumMatch = false;
					boolean classReaction = false;
					if (ecocycid.length() > 0) reaction = loadReaction(ecocycid);
					if (reaction != null) {
						ecoCommonName = reaction.getCommonName();
						ecoEC = reaction.getEC();
						if (ecoEC != null) ecoEC = ecoEC.replace("\"", "");
						else ecoEC = "";
						ecoGene = reactionGeneRule(reaction.getLocalID(), true);
	//					ecoSynonyms = compound.getSynonyms();
						if (proteinClass.equalsIgnoreCase(ecoEC)) ECMatch = true;
						if (geneAssociation != null && geneAssociation.length() > 0 && ecoGene != null && ecoGene.length() > 0) {
							bNumMatch = true;
							TreeSet<String> palssonBNumberSet = new TreeSet<String>();
							TreeSet<String> ecoBNumberSet = new TreeSet<String>();
							for (String bNum : geneAssociation.replace("(", "").replace(")", "").replace("and", "").replace("or", "").split(" ")) palssonBNumberSet.add(bNum);
							for (String bNum : ecoGene.replace("(", "").replace(")", "").replace("and", "").replace("or", "").split(" ")) ecoBNumberSet.add(bNum);
							for (String bNum : ecoBNumberSet) {
								if (!palssonBNumberSet.contains(bNum)) bNumMatch = false;
							}
							if (palssonBNumberSet.size() != ecoBNumberSet.size()) bNumMatch = false;
						}
						classReaction = isGeneralizedReaction(reaction);
						
						// Option 2
						ecoOutput += "\t";
						ecoOutput += ecocycid+"\t";
						ecoOutput += ecoCommonName+"\t";
						ecoOutput += ecoEC+"\t";
						ecoOutput += ecoGene+"\t";
						ecoOutput += ecoSynonyms+"\t";
						ecoOutput += ECMatch+"\t";
						ecoOutput += bNumMatch+"\t";
						ecoOutput += classReaction+"\t";
						ecoOutput += "\n";
						break;
					} else if (ecocycid.length() > 0) System.out.println("Can't get reaction : " + ecocycid);
					
					// Option 1
//					output += "\t";
//					output += ecocycid+"\t";
//					output += ecoCommonName+"\t";
//					output += ecoEC+"\t";
//					output += ecoGene+"\t";
//					output += ecoSynonyms+"\t";
//					output += ECMatch+"\t";
					
//					count--;
//					if (count != 0) output += "\n\t\t\t\t\t\t\t";
				}
				// Option 1
//				output += "\n";
				
				// Option 2
				if (ecoOutput.length() > 0) output += ecoOutput;
				else output += ecoOutput + "\n";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		printString("/home/Jesse/Desktop/ecocyc_model/mapping/rxnMappings.txt", output);
	}

	private void coreReactionTest() {
		// Attempts to match a small subset of EcoCyc reactions in and around central carbon metabolism to reactions in
		// Palsson's iAF1260 model. Used as a tool to generate files for manual review.
		String output = "";
		String fileName = "/home/Jesse/Desktop/ecocyc_model/mapping/iAF1260-ecocyc-rxn-mappings.txt";
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		HashMap<String, String> map = new HashMap<String, String>();
		HashMap<String, String> chemMap = new HashMap<String, String>();
		HashMap<String, String> ecMap = new HashMap<String, String>();
		HashMap<String, String> bMap = new HashMap<String, String>();
		HashMap<String, String> eqMap = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> reactionReactantMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> reactionProductMap = new HashMap<String, ArrayList<String>>();
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			String header = reader.readLine();
			output += "ecoID\tPID\n";
			
			while ((text = reader.readLine()) != null) {
				String[] line = text.split("\t", 11);
				
				String abbreviation = line[0];
				String equation = line[2];
				String geneAssociation = line[3];
				String proteinClass = line[4];
				String ecocycrxnids = line[5];
				
				// Split ids
				ArrayList<String> ecocycIDs = new ArrayList<String>();
				if (ecocycrxnids.length() > 0) {
					ecocycrxnids = ecocycrxnids.substring(1, ecocycrxnids.length()-1);
					for (String id : ecocycrxnids.split(" ")) ecocycIDs.add(id);
				} 
				
				// Map
				ecMap.put(abbreviation, proteinClass);
				bMap.put(abbreviation, geneAssociation);
				eqMap.put(abbreviation, equation);
				for (String id : ecocycIDs) map.put(id, abbreviation);
				chemMap = getChemicalMapEcoCycToPalsson();
				
				// Handle reaction equation
				String reactants = "";
				String products = "";
				if (equation.contains("<==>")) {
					String[] equationParts = equation.split("<==>");
					reactants = equationParts[0];
					if (equationParts.length > 1) products = equationParts[1];
					else products = "";
				}
				else if (equation.contains("-->")) {
					String[] equationParts = equation.split("-->");
					reactants = equationParts[0];
					if (equationParts.length > 1) products = equationParts[1];
					else products = "";
				}
				else System.err.println("Bad equation arrow in equation : " + equation);
				
				reactants = reactants.replace("[c]", "").replace("[p]", "").replace("[e]", "").replace("[b]", "").replace(" : ", "").replace(" + ", " ");
				products = products.replace("[c]", "").replace("[p]", "").replace("[e]", "").replace("[b]", "").replace(" : ", "").replace(" + ", " ");
				
				for (String reactant : reactants.split(" ")) {
					if (reactant != null && reactant.length() > 0 && !reactant.startsWith("(")) {
						if (reactionReactantMap.keySet().contains(abbreviation)) reactionReactantMap.get(abbreviation).add(reactant);
						else {
							ArrayList<String> newArray = new ArrayList<String>();
							newArray.add(reactant);
							reactionReactantMap.put(abbreviation, newArray);
						}
					}
				}
				for (String product : products.split(" ")) {
					if (product != null && product.length() > 0 && !product.startsWith("(")) {
						if (reactionProductMap.keySet().contains(abbreviation)) reactionProductMap.get(abbreviation).add(product);
						else {
							ArrayList<String> newArray = new ArrayList<String>();
							newArray.add(product);
							reactionProductMap.put(abbreviation, newArray);
						}
					}
				}
			}
			
//			// Get ecocyc reactions (by pathway)
//			ArrayList<String> biosynPwys = (ArrayList<String>)conn.getClassAllInstances("|Amino-Acid-Biosynthesis|");
//			
//			Pathway glyoxalateCycle = loadPathway("GLYOXYLATE-BYPASS");
//			Pathway TCACycle = loadPathway("TCA");
//			Pathway glycolysisI = loadPathway("GLYCOLYSIS");
//			Pathway ppp = loadPathway("PENTOSE-P-PWY");
//			Pathway superPwy = loadPathway("GLYCOLYSIS-TCA-GLYOX-BYPASS");
//			Pathway ed = loadPathway("ENTNER-DOUDOROFF-PWY");
//			
			TreeSet<String> reactionIDs = new TreeSet<String>();
//			reactionIDs.addAll(glyoxalateCycle.getReactionIDs());
//			reactionIDs.addAll(TCACycle.getReactionIDs());
//			reactionIDs.addAll(glycolysisI.getReactionIDs());
//			reactionIDs.addAll(ppp.getReactionIDs());
//			reactionIDs.addAll(superPwy.getReactionIDs());
//			reactionIDs.addAll(ed.getReactionIDs());
//			
//			for (String biosynPwy : biosynPwys) {
//				reactionIDs.addAll(loadPathway(biosynPwy).getReactionIDs());
//			}
//			
//			// Recursively get all reactions, as some "reactionIDs" may be pathways themselves
//			boolean done = false;
//			ArrayList<String> remove = new ArrayList<String>();
//			while (!done) {
//				done = true;
//				ArrayList<String> add = new ArrayList<String>();
//				for (String id : reactionIDs) {
//					Frame pwy = Frame.load(conn, id);
//					if (pwy.getGFPtype().equals(Pathway.GFPtype)) {
//						done = false;
//						add.addAll(((Pathway)pwy).getReactionIDs());
//						remove.add(id);
//					}
//				}
//				reactionIDs.addAll(add);
//				reactionIDs.removeAll(remove);
//			}
//			
//			// Create metabolite set
//			TreeSet<String> metaboliteIDs = new TreeSet<String>();
//			for (String reactionID : reactionIDs) {
//				Frame reaction = Reaction.load(conn, reactionID);
//				metaboliteIDs.addAll((ArrayList<String>)reaction.getSlotValues("LEFT"));
//				metaboliteIDs.addAll((ArrayList<String>)reaction.getSlotValues("RIGHT"));
//			}
//			
//			// Fill in neighbor reactions connected to important metabolites
//			ArrayList<String> commonMetExcludeList = new ArrayList<String>();
//			commonMetExcludeList.add("ADP");
//			commonMetExcludeList.add("AMP");
//			commonMetExcludeList.add("ATP");
//			commonMetExcludeList.add("NAD");
//			commonMetExcludeList.add("NADH");
//			commonMetExcludeList.add("NADP");
//			commonMetExcludeList.add("NADPH");
//			commonMetExcludeList.add("OXYGEN-MOLECULE");
//			commonMetExcludeList.add("PROTON");
//			commonMetExcludeList.add("|Pi|");
//			commonMetExcludeList.add("NAD-P-OR-NOP");
//			commonMetExcludeList.add("NADH-P-OR-NOP");
//			commonMetExcludeList.add("PROT-CYS");
//			commonMetExcludeList.add("|Charged-SEC-tRNAs|");
//			commonMetExcludeList.add("|Demethylated-methyl-acceptors|");
//			commonMetExcludeList.add("|L-seryl-SEC-tRNAs|");
//			commonMetExcludeList.add("|Methylated-methyl-acceptors|");
//			commonMetExcludeList.add("|Quinones|");
//			commonMetExcludeList.add("|Reduced-Quinones|");
//			commonMetExcludeList.add("|SEC-tRNAs|");
//			commonMetExcludeList.add("|Ubiquinols|");
//			commonMetExcludeList.add("|Ubiquinones|");
//			commonMetExcludeList.add("ENZYME-S-SULFANYLCYSTEINE");
//			commonMetExcludeList.add("WATER");
//			commonMetExcludeList.add("CARBON-DIOXIDE");
//			commonMetExcludeList.add("PPI");
//			
//			for (String m : metaboliteIDs) {
//				if (!commonMetExcludeList.contains(m)) {
//					Compound met = (Compound)Compound.load(conn, m);
//					for (Reaction r : met.reactantIn()) reactionIDs.add(r.getLocalID());
//					for (Reaction r : met.productOf()) reactionIDs.add(r.getLocalID());
//				}
//			}
//			
//			// Filter: Non transport, non generic, unmapped
//			ArrayList<String> filterList = new ArrayList<String>();
//			for (String reactionID : reactionIDs) {
//				if (((ArrayList<String>)conn.getInstanceAllTypes(reactionID)).contains(TransportReaction.GFPtype)) {
//					filterList.add(reactionID);
//				}
//				if (isGeneralizedReaction(reactionID)) {
//					filterList.add(reactionID);
//				}
////				if (map.get(reactionID) != null) {
////					filterList.add(reactionID);
////				}
//			}
//			reactionIDs.removeAll(filterList);
//			
//			reactionIDs.clear();
			
//			reactionIDs = reactionListA();
			reactionIDs = reactionListB(map);
//			reactionIDs = reactionListC();
			
			// Output
			for (String reactionID : reactionIDs) {
				Reaction reaction = (Reaction)Reaction.load(conn, reactionID);
				String ec = reaction.getEC();
				if (ec != null && ec.length() > 0) ec = ec.replace("\"", "");
				String bNumbers = reactionGeneRule(reactionID, true);

				// Reaction Equation
				String reactionEquation = "";
				int unmappedReactants = 0;
				int unmappedProducts = 0;
				ArrayList<String> reactants = new ArrayList<String>();
				ArrayList<String> products = new ArrayList<String>();
				for (Frame reactant : reaction.getReactants()) {
					String reactantPalssonID = chemMap.get(reactant.getLocalID());
					if (reactantPalssonID != null) {
						reactionEquation += reactantPalssonID + " ";
						reactants.add(reactantPalssonID);
					}
					else {
						reactionEquation += reactant.getLocalID() + " ";
						unmappedReactants++;
					}
				}
				reactionEquation += " --> ";
				for (Frame product : reaction.getProducts()) {
					String productPalssonID = chemMap.get(product.getLocalID());
					if (productPalssonID != null) {
						reactionEquation += productPalssonID + " ";
						products.add(productPalssonID);
					}
					else {
						reactionEquation += product.getLocalID() + " ";
						unmappedProducts++;
					}
				}
				
				// Potential Matches
				ArrayList<String> potentialMatches = equationChecker(reactionReactantMap, reactionProductMap, reactants, products, unmappedReactants, unmappedProducts);
				
				String matches = "";
				for (String potentialMatch : potentialMatches) {
					matches += "\t" + potentialMatch + "\t" + ecMap.get(potentialMatch) + "\t" + bMap.get(potentialMatch) + "\t" + eqMap.get(potentialMatch) + "\n";
				}
				
				output += reactionID + "\t" + map.get(reactionID) + "\t" + ec + "\t" + bNumbers + "\t" + reactionEquation + "\n";
				output += matches;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
//		printString("/home/Jesse/Desktop/ecocyc_model/mapping/coreReactions.txt", output);
	}
	
	private TreeSet<String> reactionListA(HashMap<String, String> iAF1260_to_EcoCyc_Map) throws PtoolsErrorException {
		// All reactions of central carbon metabolism
		TreeSet<String> reactionIDs = new TreeSet<String>();

		ArrayList<String> biosynPwys = (ArrayList<String>)conn.getClassAllInstances("|Amino-Acid-Biosynthesis|");
		Pathway glyoxalateCycle = loadPathway("GLYOXYLATE-BYPASS");
		Pathway TCACycle = loadPathway("TCA");
		Pathway glycolysisI = loadPathway("GLYCOLYSIS");
		Pathway ppp = loadPathway("PENTOSE-P-PWY");
		Pathway superPwy = loadPathway("GLYCOLYSIS-TCA-GLYOX-BYPASS");
		Pathway ed = loadPathway("ENTNER-DOUDOROFF-PWY");
		
		reactionIDs.addAll(glyoxalateCycle.getReactionIDs());
		reactionIDs.addAll(TCACycle.getReactionIDs());
		reactionIDs.addAll(glycolysisI.getReactionIDs());
		reactionIDs.addAll(ppp.getReactionIDs());
		reactionIDs.addAll(superPwy.getReactionIDs());
		reactionIDs.addAll(ed.getReactionIDs());
		
		for (String biosynPwy : biosynPwys) {
			reactionIDs.addAll(loadPathway(biosynPwy).getReactionIDs());
		}
		
		// Recursively get all reactions, as some "reactionIDs" may be pathways themselves
		boolean done = false;
		ArrayList<String> remove = new ArrayList<String>();
		while (!done) {
			done = true;
			ArrayList<String> add = new ArrayList<String>();
			for (String id : reactionIDs) {
				Frame pwy = Frame.load(conn, id);
				if (pwy.getGFPtype().equals(Pathway.GFPtype)) {
					done = false;
					add.addAll(((Pathway)pwy).getReactionIDs());
					remove.add(id);
				}
			}
			reactionIDs.addAll(add);
			reactionIDs.removeAll(remove);
		}
		
		return reactionIDs;
	}
	
	private TreeSet<String> reactionListB(HashMap<String, String> iAF1260_to_EcoCyc_Map) throws PtoolsErrorException {
		// All reactions that are within the p=1 neighborhood of central carbon metabolism,
		// excluding generic and transport reactions.
		TreeSet<String> reactionIDs = new TreeSet<String>();

		ArrayList<String> biosynPwys = (ArrayList<String>)conn.getClassAllInstances("|Amino-Acid-Biosynthesis|");
		Pathway glyoxalateCycle = loadPathway("GLYOXYLATE-BYPASS");
		Pathway TCACycle = loadPathway("TCA");
		Pathway glycolysisI = loadPathway("GLYCOLYSIS");
		Pathway ppp = loadPathway("PENTOSE-P-PWY");
		Pathway superPwy = loadPathway("GLYCOLYSIS-TCA-GLYOX-BYPASS");
		Pathway ed = loadPathway("ENTNER-DOUDOROFF-PWY");
		
		reactionIDs.addAll(glyoxalateCycle.getReactionIDs());
		reactionIDs.addAll(TCACycle.getReactionIDs());
		reactionIDs.addAll(glycolysisI.getReactionIDs());
		reactionIDs.addAll(ppp.getReactionIDs());
		reactionIDs.addAll(superPwy.getReactionIDs());
		reactionIDs.addAll(ed.getReactionIDs());
		
		for (String biosynPwy : biosynPwys) {
			reactionIDs.addAll(loadPathway(biosynPwy).getReactionIDs());
		}
		
		// Recursively get all reactions, as some "reactionIDs" may be pathways themselves
		boolean done = false;
		ArrayList<String> remove = new ArrayList<String>();
		while (!done) {
			done = true;
			ArrayList<String> add = new ArrayList<String>();
			for (String id : reactionIDs) {
				Frame pwy = Frame.load(conn, id);
				if (pwy.getGFPtype().equals(Pathway.GFPtype)) {
					done = false;
					add.addAll(((Pathway)pwy).getReactionIDs());
					remove.add(id);
				}
			}
			reactionIDs.addAll(add);
			reactionIDs.removeAll(remove);
		}
		
		// Create metabolite set
		TreeSet<String> metaboliteIDs = new TreeSet<String>();
		for (String reactionID : reactionIDs) {
			Frame reaction = Reaction.load(conn, reactionID);
			metaboliteIDs.addAll((ArrayList<String>)reaction.getSlotValues("LEFT"));
			metaboliteIDs.addAll((ArrayList<String>)reaction.getSlotValues("RIGHT"));
		}
		
		// Fill in neighbor reactions connected to important metabolites
		ArrayList<String> commonMetExcludeList = new ArrayList<String>();
		commonMetExcludeList.add("ADP");
		commonMetExcludeList.add("AMP");
		commonMetExcludeList.add("ATP");
		commonMetExcludeList.add("NAD");
		commonMetExcludeList.add("NADH");
		commonMetExcludeList.add("NADP");
		commonMetExcludeList.add("NADPH");
		commonMetExcludeList.add("OXYGEN-MOLECULE");
		commonMetExcludeList.add("PROTON");
		commonMetExcludeList.add("|Pi|");
		commonMetExcludeList.add("NAD-P-OR-NOP");
		commonMetExcludeList.add("NADH-P-OR-NOP");
		commonMetExcludeList.add("PROT-CYS");
		commonMetExcludeList.add("|Charged-SEC-tRNAs|");
		commonMetExcludeList.add("|Demethylated-methyl-acceptors|");
		commonMetExcludeList.add("|L-seryl-SEC-tRNAs|");
		commonMetExcludeList.add("|Methylated-methyl-acceptors|");
		commonMetExcludeList.add("|Quinones|");
		commonMetExcludeList.add("|Reduced-Quinones|");
		commonMetExcludeList.add("|SEC-tRNAs|");
		commonMetExcludeList.add("|Ubiquinols|");
		commonMetExcludeList.add("|Ubiquinones|");
		commonMetExcludeList.add("ENZYME-S-SULFANYLCYSTEINE");
		commonMetExcludeList.add("WATER");
		commonMetExcludeList.add("CARBON-DIOXIDE");
		commonMetExcludeList.add("PPI");
		
		for (String m : metaboliteIDs) {
			if (!commonMetExcludeList.contains(m)) {
				Compound met = (Compound)Compound.load(conn, m);
				for (Reaction r : met.reactantIn()) reactionIDs.add(r.getLocalID());
				for (Reaction r : met.productOf()) reactionIDs.add(r.getLocalID());
			}
		}
		
		// Filter: Non transport, non generic, unmapped
		ArrayList<String> filterList = new ArrayList<String>();
		for (String reactionID : reactionIDs) {
			if (((ArrayList<String>)conn.getInstanceAllTypes(reactionID)).contains(TransportReaction.GFPtype)) {
				filterList.add(reactionID);
			}
			if (isGeneralizedReaction(reactionID)) {
				filterList.add(reactionID);
			}
			if (iAF1260_to_EcoCyc_Map.get(reactionID) != null) {
				filterList.add(reactionID);
			}
		}
		reactionIDs.removeAll(filterList);
		
		return reactionIDs;
	}
		
	private TreeSet<String> reactionListC() {
		// All unmapped (iAF1260 to EcoCyc) reactions that are within the p=1 neighborhood of central carbon metabolism,
		// excluding generic and transport reactions.  This list includes only those reactions manually found to not 
		// exist in the iAF1260 model
		TreeSet<String> reactionIDs = new TreeSet<String>();
		reactionIDs.add("ALLANTOATE-DEIMINASE-RXN");
		reactionIDs.add("CHERTAPM-RXN");
		reactionIDs.add("CHERTARM-RXN");
		reactionIDs.add("CHERTRGM-RXN");
		reactionIDs.add("CHERTSRM-RXN");
		reactionIDs.add("PROPIONYL-COA-CARBOXY-RXN");
		reactionIDs.add("PROPIONATE--COA-LIGASE-RXN");
		reactionIDs.add("PROLINE-MULTI");
		reactionIDs.add("2.5.1.64-RXN");
		reactionIDs.add("2.6.1.7-RXN");
		reactionIDs.add("3.5.1.88-RXN");
		reactionIDs.add("4-COUMARATE--COA-LIGASE-RXN");
		reactionIDs.add("4OH2OXOGLUTARALDOL-RXN");
		reactionIDs.add("6-PHOSPHO-BETA-GLUCOSIDASE-RXN");
		reactionIDs.add("AMINOPROPDEHYDROG-RXN");
		reactionIDs.add("AMP-DEAMINASE-RXN");
		reactionIDs.add("DARABALDOL-RXN");
		reactionIDs.add("ENTMULTI-RXN");
		reactionIDs.add("FORMATETHFLIG-RXN");
		reactionIDs.add("GLUTATHIONE-PEROXIDASE-RXN");
		reactionIDs.add("GMP-SYN-NH3-RXN");
		reactionIDs.add("GUANOSINE-DEAMINASE-RXN");
		reactionIDs.add("LCARNCOALIG-RXN");
		reactionIDs.add("MALOX-RXN");
		reactionIDs.add("BETA-PHOSPHOGLUCOMUTASE-RXN");
		reactionIDs.add("DIHYDLIPACETRANS-RXN");
		reactionIDs.add("GLUCOSE-6-PHOSPHATE-1-EPIMERASE-RXN");
		reactionIDs.add("HOMOCYSMET-RXN");
		reactionIDs.add("METBALT-RXN");
		reactionIDs.add("NAD-SYNTH-GLN-RXN");
		reactionIDs.add("NICOTINATEPRIBOSYLTRANS-RXN");
		reactionIDs.add("PYRDAMPTRANS-RXN");
		reactionIDs.add("PYRIMSYN1-RXN");
		reactionIDs.add("PYROXALTRANSAM-RXN");
		reactionIDs.add("RXN0-5222");
		reactionIDs.add("PHENYLSERINE-ALDOLASE-RXN");
		reactionIDs.add("PYRUVATEDECARB-RXN");
		reactionIDs.add("R524-RXN");
		reactionIDs.add("RXN-11302");
		reactionIDs.add("RXN-11475");
		reactionIDs.add("RXN-8073");
		reactionIDs.add("RXN-8636");
		reactionIDs.add("RXN-8675");
		reactionIDs.add("RXN-9311");
		reactionIDs.add("RXN0-1241");
		reactionIDs.add("RXN0-2023");
		reactionIDs.add("RXN0-2061");
		reactionIDs.add("RXN0-310");
		reactionIDs.add("RXN0-5040");
		reactionIDs.add("RXN0-5185");
		reactionIDs.add("RXN0-5192");
		reactionIDs.add("RXN0-5213");
		reactionIDs.add("RXN0-5219");
		reactionIDs.add("RXN0-5245");
		reactionIDs.add("RXN0-5253");
		reactionIDs.add("RXN0-5257");
		reactionIDs.add("RXN0-5261");
		reactionIDs.add("RXN0-5269");
		reactionIDs.add("RXN0-5297");
		reactionIDs.add("RXN0-5364");
		reactionIDs.add("RXN0-5375");
		reactionIDs.add("RXN0-5398");
		reactionIDs.add("RXN0-5433");
		reactionIDs.add("RXN0-5507");
		reactionIDs.add("RXN0-6375");
		reactionIDs.add("RXN0-6541");
		reactionIDs.add("RXN0-6562");
		reactionIDs.add("RXN0-6563");
		reactionIDs.add("RXN0-6576");
		reactionIDs.add("RXN0-984");
		reactionIDs.add("RXN0-985");
		reactionIDs.add("RXN0-986");
		reactionIDs.add("SEDOBISALDOL-RXN");
		reactionIDs.add("UREIDOGLYCOLATE-LYASE-RXN");
		reactionIDs.add("URUR-RXN");
		return reactionIDs;
	}
	
	private ArrayList<String> equationChecker(HashMap<String, ArrayList<String>> reactionReactantMap, HashMap<String, ArrayList<String>> reactionProductMap, ArrayList<String> reactants, ArrayList<String> products, int unmappedReactants, int unmappedProducts) {
		// Given a hashmap of reaction to reactant and reaction to product for the iAF1260 model, and an arraylist of the reactions and products
		// in the ecocyc reaction of question (already converted to their iAF1260 counterparts), which palsson reactions could potentially map
		// to this ecocyc reaction
		
		ArrayList<String> potentialMatches = new ArrayList<String>();
		for (String reaction : reactionReactantMap.keySet()) {
			ArrayList<String> reactantList = reactionReactantMap.get(reaction);
			ArrayList<String> productList = reactionProductMap.get(reaction);
			
			if (reactantList == null) reactantList = new ArrayList<String>();
			if (productList == null) productList = new ArrayList<String>();
			
			if (reactantList.size() != reactants.size() + unmappedReactants || productList.size() != products.size() + unmappedProducts) continue;
			
			boolean match = true;
			for (String reactant : reactants) {
				if (!reactantList.contains(reactant)) match = false;
			}
			for (String product : products) {
				if (!productList.contains(product)) match = false;
			}
			if (match) potentialMatches.add(reaction);
		}
		return potentialMatches;
	}
	
	private void coreReactionTest_orig() {
		Long start = System.currentTimeMillis();
		String output = "";
		String fileName = "/home/Jesse/Desktop/ecocyc_model/mapping/iAF1260-ecocyc-rxn-mappings.txt";
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		HashMap<String, String> map = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> ecMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> bMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> chemMap = new HashMap<String, String>();
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			String header = reader.readLine();
			output += "ecoID\tPID\n";
			
			while ((text = reader.readLine()) != null) {
				String[] line = text.split("\t", 11);
				
				String abbreviation = line[0];
				String officialName = line[1];
				String equation = line[2];
				String geneAssociation = line[3];
				String proteinClass = line[4];
				String ecocycrxnids = line[5];
				String analysis = line[6];
				String notes = line[7];
				
				// Split ids
				ArrayList<String> ecocycIDs = new ArrayList<String>();
				if (ecocycrxnids.length() > 0) {
					ecocycrxnids = ecocycrxnids.substring(1, ecocycrxnids.length()-1);
					for (String id : ecocycrxnids.split(" ")) ecocycIDs.add(id);
				} 
				
				// Map
				for (String id : ecocycIDs) map.put(id, abbreviation);
				
				if (proteinClass.length() > 0) {
					if (ecMap.keySet().contains(proteinClass)) ecMap.get(proteinClass).add(abbreviation);
					else {
						ArrayList<String> newArray = new ArrayList<String>();
						newArray.add(abbreviation);
						ecMap.put(proteinClass, newArray);
					}
				}
				
				if (geneAssociation.length() > 0) {
					for (String b : geneAssociation.replace("(", "").replace(")", "").replace("and ", "").replace("or ", "").split(" ")) {
						if (bMap.keySet().contains(b)) bMap.get(b).add(abbreviation);
						else {
							ArrayList<String> newArray = new ArrayList<String>();
							newArray.add(abbreviation);
							bMap.put(b, newArray);
						}
					}
				}
				
				chemMap = getChemicalMapEcoCycToPalsson();
			}
			
			// Get ecocyc reactions (by pathway)
			ArrayList<String> biosynPwys = (ArrayList<String>)conn.getClassAllInstances("|Amino-Acid-Biosynthesis|");
			
			Pathway glyoxalateCycle = loadPathway("GLYOXYLATE-BYPASS");
			Pathway TCACycle = loadPathway("TCA");
			Pathway glycolysisI = loadPathway("GLYCOLYSIS");
			Pathway ppp = loadPathway("PENTOSE-P-PWY");
			Pathway superPwy = loadPathway("GLYCOLYSIS-TCA-GLYOX-BYPASS");
			Pathway ed = loadPathway("ENTNER-DOUDOROFF-PWY");
			
			TreeSet<String> reactionIDs = new TreeSet<String>();
			reactionIDs.addAll(glyoxalateCycle.getReactionIDs());
			reactionIDs.addAll(TCACycle.getReactionIDs());
			reactionIDs.addAll(glycolysisI.getReactionIDs());
			reactionIDs.addAll(ppp.getReactionIDs());
			reactionIDs.addAll(superPwy.getReactionIDs());
			reactionIDs.addAll(ed.getReactionIDs());
			
			for (String biosynPwy : biosynPwys) {
				reactionIDs.addAll(loadPathway(biosynPwy).getReactionIDs());
			}
			
			// Recursively get all reactions, as some "reactionIDs" may be pathways themselves
			boolean done = false;
			ArrayList<String> remove = new ArrayList<String>();
			while (!done) {
				done = true;
				ArrayList<String> add = new ArrayList<String>();
				for (String id : reactionIDs) {
					Frame pwy = Frame.load(conn, id);
					if (pwy.getGFPtype().equals(Pathway.GFPtype)) {
						done = false;
						add.addAll(((Pathway)pwy).getReactionIDs());
						remove.add(id);
					}
				}
				reactionIDs.addAll(add);
				reactionIDs.removeAll(remove);
			}
			
			// Create metabolite set
			TreeSet<String> metaboliteIDs = new TreeSet<String>();
			for (String reactionID : reactionIDs) {
				Frame reaction = Reaction.load(conn, reactionID);
				metaboliteIDs.addAll((ArrayList<String>)reaction.getSlotValues("LEFT"));
				metaboliteIDs.addAll((ArrayList<String>)reaction.getSlotValues("RIGHT"));
			}
			
			// Fill in neighbor reactions connected to important metabolites
			ArrayList<String> commonMetExcludeList = new ArrayList<String>();
			commonMetExcludeList.add("ADP");
			commonMetExcludeList.add("AMP");
			commonMetExcludeList.add("ATP");
			commonMetExcludeList.add("NAD");
			commonMetExcludeList.add("NADH");
			commonMetExcludeList.add("NADP");
			commonMetExcludeList.add("NADPH");
			commonMetExcludeList.add("OXYGEN-MOLECULE");
			commonMetExcludeList.add("PROTON");
			commonMetExcludeList.add("|Pi|");
			commonMetExcludeList.add("NAD-P-OR-NOP");
			commonMetExcludeList.add("NADH-P-OR-NOP");
			commonMetExcludeList.add("PROT-CYS");
			commonMetExcludeList.add("|Charged-SEC-tRNAs|");
			commonMetExcludeList.add("|Demethylated-methyl-acceptors|");
			commonMetExcludeList.add("|L-seryl-SEC-tRNAs|");
			commonMetExcludeList.add("|Methylated-methyl-acceptors|");
			commonMetExcludeList.add("|Quinones|");
			commonMetExcludeList.add("|Reduced-Quinones|");
			commonMetExcludeList.add("|SEC-tRNAs|");
			commonMetExcludeList.add("|Ubiquinols|");
			commonMetExcludeList.add("|Ubiquinones|");
			commonMetExcludeList.add("ENZYME-S-SULFANYLCYSTEINE");
			commonMetExcludeList.add("WATER");
			commonMetExcludeList.add("CARBON-DIOXIDE");
			commonMetExcludeList.add("PPI");
			
			for (String m : metaboliteIDs) {
				if (!commonMetExcludeList.contains(m)) {
//					System.out.println(m);
					Compound met = (Compound)Compound.load(conn, m);
//					System.out.println(met.reactantIn().size());
//					System.out.println(met.productOf().size());
					for (Reaction r : met.reactantIn()) reactionIDs.add(r.getLocalID());
					for (Reaction r : met.productOf()) reactionIDs.add(r.getLocalID());
				}
			}
			
			// Filter: Non transport, non generic
			ArrayList<String> filterList = new ArrayList<String>();
			for (String reactionID : reactionIDs) {
				if (((ArrayList<String>)conn.getInstanceAllTypes(reactionID)).contains(TransportReaction.GFPtype)) {
					filterList.add(reactionID);
				}
				if (isGeneralizedReaction(reactionID)) {
					filterList.add(reactionID);
				}
			}
			reactionIDs.removeAll(filterList);
			
			// Output
			for (String reactionID : reactionIDs) {
				// only output unmapped reactions
				if (map.get(reactionID) == null) {
					Reaction reaction = (Reaction)Reaction.load(conn, reactionID);
					String ec = reaction.getEC();
					if (ec != null && ec.length() > 0) ec = ec.replace("\"", "");
					String bNumbers = reactionGeneRule(reactionID, true);

					String reactionEquation = "";
					for (Frame reactant : reaction.getReactants()) {
						if (chemMap.get(reactant.getLocalID()) != null) reactionEquation += chemMap.get(reactant.getLocalID()) + " ";
						else reactionEquation += reactant.getLocalID() + " ";
					}
					reactionEquation += " --> ";
					for (Frame product : reaction.getProducts()) {
						if (chemMap.get(product.getLocalID()) != null) reactionEquation += chemMap.get(product.getLocalID()) + " ";
						else reactionEquation += product.getLocalID() + " ";
					}
					
					// Potential Matches
//					if (reactionID.equals("R137-RXN")) {
//						System.out.println("1");
//					}
					
					TreeSet<String> potentialMatches = new TreeSet<String>();
					if (ec != null && ec.length() > 0) {
						try {
							for (String s : ecMap.get(ec)) {
								potentialMatches.add(s);
							}
						} catch (NullPointerException e) {
							// no suggestions to be made, ignore
						}
					}
					
					if (bNumbers != null && bNumbers.length() > 0) {
						for (String bNumber : bNumbers.replace("(", "").replace(")", "").replace("and ", "").replace("or ", "").split(" ")) {
							try {
								for (String s : bMap.get(bNumber)) {
									potentialMatches.add(s);
								}
							} catch (NullPointerException e) {
								// no suggestions to be made, ignore
							}
						}
					}
					
					String matches = "";
					for (String potentialMatch : potentialMatches) {
						matches += potentialMatch + ":";
					}
					if (matches.length() > 0) matches = matches.substring(0, matches.length() - 1);
					
					output += reactionID + "\t" + map.get(reactionID) + "\t" + ec + "\t" + bNumbers + "\t" + matches + "\t" + reactionEquation + "\n";
				}
			}
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		printString("/home/Jesse/Desktop/ecocyc_model/mapping/coreReactions.txt", output);
		
		Long stop = System.currentTimeMillis();
		Long runtime = (stop - start) / 1000;
		System.out.println("Runtime is " + runtime + " seconds.");
	}
	
	private HashMap<String, String> getChemicalMapEcoCycToPalsson() {
		String fileName = "/home/Jesse/Desktop/ecocyc_model/mapping/iAF1260-ecocyc-cpd-mappings.txt";
		HashMap<String, String> map = new HashMap<String, String>();
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			reader.readLine();
			while ((text = reader.readLine()) != null) {
				String[] line = text.split("\t", 11);
				
				String abbreviation = line[0];
				String ecocycid = line[7];
				map.put(ecocycid, abbreviation);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return map;
	}
	
	
	private void printReactionList(ArrayList<ReactionInstance> reactions, String fileName) {
		String outString = "";
		for (ReactionInstance reaction : reactions) {
			Frame r = loadFrame("");
		}
		printString("/home/Jesse/Desktop/ecocyc_model/mapping/" + fileName, outString);
	}
	
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
					outString += keggID;
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		printString("/home/Jesse/Desktop/ecocyc_model/mapping/" + fileName, outString);
	}
	
	private HashMap<String, ArrayList<String>> readMap(String fileName) {
		HashMap<String, ArrayList<String>> ecoCycToPalsson = new HashMap<String, ArrayList<String>>();
		
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			reader.readLine();
			
			while ((text = reader.readLine()) != null) {
				String[] line = text.split("\t");
				
				if (ecoCycToPalsson.keySet().contains(line[0])) ecoCycToPalsson.get(line[0]).add(line[1]);
				else {
					ArrayList<String> newArray = new ArrayList<String>();
					newArray.add(line[1]);
					ecoCycToPalsson.put(line[0], newArray);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return ecoCycToPalsson;
	}	
	
	private void readInPalssonIDMaps(String fileName) {
		String e2pMapped = "";
		String e2pUnmapped = "";
		String e2pUnmappedDiffusion = "";
		String e2pUnmappedExchange = "";
		String header = "";
		
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			header = reader.readLine();
			
			while ((text = reader.readLine()) != null) {
				// Set up mappings
				String[] line = text.split("\t");
				if (line.length > 5 && line[5].length() > 0) {
					for (String id : line[5].replace("(", "").replace(")", "").replace("\"", "").split(" ")) {
						e2pMapped += id + "\t" + line[0] + "\n";
					}
				}
				else if (text.contains(":DIFFUSION")) e2pUnmappedDiffusion += text + "\n";
				else if (text.contains(":EXCHANGE")) e2pUnmappedExchange += text + "\n";
				else e2pUnmapped += text + "\n";
			}
			
			// Output
			printString("/home/Jesse/Desktop/output/e2pMapped", e2pMapped);
			printString("/home/Jesse/Desktop/output/e2pUnmapped", header + "\n" + e2pUnmapped);
			printString("/home/Jesse/Desktop/output/e2pUnmappedDiffusion", e2pUnmappedDiffusion);
			printString("/home/Jesse/Desktop/output/e2pUnmappedExchange", e2pUnmappedExchange);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		readInPalssonIDMaps2("/home/Jesse/Desktop/output/e2pUnmapped");
	}
	
	private void readInPalssonIDMaps2(String fileName) {
		File reactionMapFile = new File(fileName);
		BufferedReader reader = null;
		HashMap<String, ArrayList<String>> ecMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> bMap = new HashMap<String, ArrayList<String>>();
		
		try {
			// B# map & EC# map
			for (Reaction rxn : Reaction.all(conn)) {
				String ec = rxn.getEC();
				if (ec != null && !ec.equals("null")) {
					ec = ec.replace("\"", "");
					
					if (ecMap.keySet().contains(ec)) ecMap.get(ec).add(rxn.getLocalID());
					else {
						ArrayList<String> newArray = new ArrayList<String>();
						newArray.add(rxn.getLocalID());
						ecMap.put(ec, newArray);
					}
				}
				bMap.put(rxn.getLocalID(), (ArrayList<String>)conn.genesOfReaction(rxn.getLocalID()));
			}
			
			reader = new BufferedReader(new FileReader(reactionMapFile));
			String text = null;
			
			// Headers
			reader.readLine();
			
			while ((text = reader.readLine()) != null) {
				// Set up mappings
				String[] line = text.split("\t");
				if (line.length > 4 && line[4].length() > 0) {
					String ec = line[4];
					System.out.println(ec);
					System.out.println(ecMap.containsKey(ec));
					
					String b = line[3];
					System.out.println(b);
					boolean found = false;
				}
//				if (line.length > 3 && line[3].length() > 0) {
//					for (String bnum : line[3].replace("(", "").replace(")", "").replace("and", "").replace("or", "").split(" ")) {
//						
//					}
//				}
				
				// Useful output and sorting
				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void palssonToCytoscape() {
//		String fileName = "/home/Jesse/Desktop/ecocyc_model/mapping/iAF1260-ecocyc-rxn-mappings.txt";
//		File reactionMapFile = new File(fileName);
//		BufferedReader reader = null;
//		
//		try {
//			reader = new BufferedReader(new FileReader(reactionMapFile));
//			String text = null;
//			
//			// Headers
//			String header = reader.readLine();
//			output += header.trim();
//			output += "\tecocycID\tecoCommonName\tecoEC\tecoGene\tecoSynonyms\tECMatch\tbNumMatch\tisClass\n";
//			
//			while ((text = reader.readLine()) != null) {
//				String[] line = text.split("\t", 11);
//				
//				// palsson info
//				String abbreviation = line[0];
//				String officialName = line[1];
//				String equation = line[2];
//				String geneAssociation = line[3];
//				String proteinClass = line[4];
//				String ecocycrxnids = line[5];
//				String analysis = line[6];
//				String notes = line[7];
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		finally {
//			try {
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			try {
//				if (reader != null) {
//					reader.close();
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		
//		Network rst = new Network("palsson_reaction_network");
//		ArrayList<Reaction> rxns = this.getReactions();
//		for(Reaction i : rxns)
//		{
//			ArrayList<Frame> productsi = i.getProducts();
//			//System.out.println(i.getLocalID()+" has "+productsi.size()+" products");
//			for(Reaction j : rxns)
//			{
//				ArrayList<Frame> reactantsj = j.getReactants();
//				//System.out.println("\t"+j.getLocalID()+" has "+reactantsj.size()+" reactants");
//				boolean hit = false;
//				for(Frame p : productsi)
//				{
//					for(Frame r : reactantsj)
//					{
//						if(p.getLocalID().equals(r.getLocalID()))
//						{
//							rst.addEdge(i,j,"");
//							hit = true;
//							break;
//						}
//					}
//					if(hit)
//						break;
//				}
//			}
//		}
//		//return rst;
	}
	
	
//	private void readInPalssonIDMaps(String fileName) {
//		String e2p = "";
//		String p2e = "";
//		TreeSet<String> mapsToAtLeastOneEcoCycFrame = new TreeSet<String>();
//		TreeSet<String> mapsToAtLeastOnePalssonID = new TreeSet<String>();
//		TreeSet<String> unmappedPalssonIDs = new TreeSet<String>();
//		HashMap<String, ArrayList<String>> ecoCycToPalsson = new HashMap<String, ArrayList<String>>();
//		HashMap<String, ArrayList<String>> palssonToEcoCyc = new HashMap<String, ArrayList<String>>();
//		
//		File reactionMapFile = new File(fileName);
//		BufferedReader reader = null;
//		
//		try {
//			reader = new BufferedReader(new FileReader(reactionMapFile));
//			String text = null;
//			
//			// Headers
//			reader.readLine();
//			
//			while ((text = reader.readLine()) != null) {
//				// Set up mappings
//				String[] line = text.split("\t");
//				if (line.length > 5 && line[5].length() > 0) {
//					for (String id : line[5].replace("(", "").replace(")", "").replace("\"", "").split(" ")) {
//						if (ecoCycToPalsson.keySet().contains(id)) ecoCycToPalsson.get(id).add(line[0]);
//						else {
//							ArrayList<String> newArray = new ArrayList<String>();
//							newArray.add(line[0]);
//							ecoCycToPalsson.put(id, newArray);
//						}
//						
//						if (palssonToEcoCyc.keySet().contains(line[0])) palssonToEcoCyc.get(line[0]).add(id);
//						else {
//							ArrayList<String> newArray = new ArrayList<String>();
//							newArray.add(id);
//							palssonToEcoCyc.put(line[0], newArray);
//						}
//						mapsToAtLeastOneEcoCycFrame.add(line[0]);
//						mapsToAtLeastOnePalssonID.add(id);
//					}
//				}
//				else unmappedPalssonIDs.add(line[0]);
//				
//				
//				// Useful output and sorting
//				
//				
//				
//				
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		finally {
//			try {
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			try {
//				if (reader != null) {
//					reader.close();
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//
////		// iAF1260 reactions that are mapped to EcoCyc
////		System.out.println("iAF1260 reactions that are mapped to EcoCyc");
////		for (String id : mapsToAtLeastOneEcoCycFrame) {
////			try {
////				Frame reaction = loadFrame(id);
////				if (reaction != null) {
////					System.out.println(id);
////				} else System.out.println("Null reaction frame: " + id);
////			} catch (PtoolsErrorException e) {
////				e.printStackTrace();
////			}
////		}
////		
////		// iAF1260 reactions that are not mapped to EcoCyc
////		System.out.println("iAF1260 reactions that are not mapped to EcoCyc");
////		for (String s : unmappedPalssonIDs) {
////			System.out.println(s);
////		}
////		
////		// EcoCyc reactions that are mapped to iAF1260
////		System.out.println("EcoCyc reactions that are mapped to iAF1260");
////		for (String s : mapsToAtLeastOnePalssonID) {
////			System.out.println(s);
////		}
////		
////		// EcoCyc reactions that are not mapped to iAF1260
////		System.out.println("EcoCyc reactions that are not mapped to iAF1260");
//////		for (String s : ?) {
//////			System.out.println(s);
//////		}
//		
//		
//		
//		
//		
//		for (String s : mapsToAtLeastOneEcoCycFrame) {
//			for (String id : palssonToEcoCyc.get(s))
//			p2e += s + "\t" + id + "\n";
//		}
//		
//		printString("/home/Jesse/Desktop/output/p2e", p2e);
////		printString("e2p", e2p);
//		
//		
//		
//		
////		int nullCount = 0;
////		int count = 0;
////		for (String s : mapsToAtLeastOnePalssonID) {
////			System.out.println(s);
////			try {
////				if (loadFrame(s) == null) nullCount++;
////				else count++;
////			} catch (PtoolsErrorException e) {
////				e.printStackTrace();
////			}
////		}
////		System.out.println(nullCount);
////		System.out.println(count);
//		
//		
////		System.out.println(mapsToAtLeastOneEcoCycFrame.size());
////		System.out.println(mapsToAtLeastOnePalssonID.size());
////		System.out.println(unmappedPalssonIDs.size());
//	}
	
	
	
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
 	 * Internal class which holds all reaction information needed to generate reactions in the SBMLDocument model.
 	 * 
 	 * @author Jesse
 	 */
 	private class ReactionInstance {
 		public Reaction parentReaction;
 		public Reaction thisReactionFrame;
 		public String name;
 		public boolean reversible;
		public ArrayList<Metabolite> reactants;
		public ArrayList<Metabolite> products;
		
		public ReactionInstance(Reaction parentReactionFrame, Reaction thisReactionFrame, String name, boolean reversible, ArrayList<Metabolite> reactants, ArrayList<Metabolite> products) {
			this.parentReaction = parentReactionFrame;
			this.thisReactionFrame = thisReactionFrame;
			this.name = name;
			this.reversible = reversible;
			this.reactants = reactants;
			this.products = products;
		}
 	}
 	
 	/**
 	 * Internal class which holds all metabolite information needed to generate species in the SBMLDocument model.
 	 * 
 	 * @author Jesse
 	 */
 	private class Metabolite {
 		public Frame metabolite;
		public String compartment;
		public int stoichiometry;
		public String chemicalFormula;
		
		public Metabolite(Frame metabolite, String compartment, int stoichiometry, String chemicalFormula) {
			this.metabolite = metabolite;
			this.compartment = compartment;
			this.stoichiometry = stoichiometry;
			this.chemicalFormula = chemicalFormula;
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



// Reactions with non-existing metabolites 
//	RXN0-1321
//	CYTC-OX-RXN

// Reactions with two of the same generic metabolite on the same side
//	3.1.26.12-RXN