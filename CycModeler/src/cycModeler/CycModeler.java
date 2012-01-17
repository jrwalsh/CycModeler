package cycModeler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

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
	static protected JavacycConnection conn = null;
	static protected String OutputDirectory = "";
	static protected String DefaultCompartment = "CCO-CYTOSOL";
	static protected int DefaultSBMLLevel = 2;
	static protected int DefaultSBMLVersion = 1;
	static protected HashMap<String, String> CompartmentAbrevs = new HashMap<String, String>();
	static protected String SpeciesPrefix = "M";
	static protected String ReactionPrefix = "R";
	static protected String BoundaryCompartmentName = "Boundary";
	static protected String ExchangeReactionSuffix = "exchange";
	static protected String ModelName = "DefaultName";
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * @param connectionString URL of the server running Pathway Tools
	 * @param port Port that JavaCycO is listening on
	 * @param organism Organism to connect to (i.e., selects which database to connect to)
	 */
	public CycModeler (String connectionString, int port, String organism, String configFile) {
		String CurrentConnectionString = connectionString;
		int CurrentPort = port;
		String CurrentOrganism = organism;

		conn = new JavacycConnection(CurrentConnectionString,CurrentPort);
		conn.selectOrganism(CurrentOrganism);
		
//		setDefaultSettings();
		getConfigFile(configFile);
	}
	
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * Does not set an organism for the JavacycConnection object.  Expects that the connection object has already selected an organism.
	 * @param connection Initialized connection object
	 */
	public CycModeler (JavacycConnection connection, String configFile) {
		conn = connection;
//		setDefaultSettings();
		getConfigFile(configFile);
	}
	

	// Methods
	public void getConfigFile(String fileName) {
		String outputDirectory = null;
		String defaultCompartment = null;
		int defaultSBMLLevel = 0;
		int defaultSBMLVersion = 0;
		String modelName = null;
		String boundaryCompartmentName = null;
		String exchangeReactionSuffix = null;
		String speciesPrefix = null;
		String reactionPrefix = null;
		HashMap<String, String> compartmentAbrevs = new HashMap<String, String>();
		
		
		File configFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(configFile));
			String text = null;
			
			while ((text = reader.readLine()) != null) {
				String command = text.substring(0, text.indexOf(" "));
				String value = text.substring(text.indexOf(" ")+1);
				
				switch (Setting.value(command)) {
					case OUTPUTDIRECTORY: outputDirectory = value; break;
					case DEFAULTCOMPARTMENT: defaultCompartment = value; break;
					case DEFAULTSBMLLEVEL: defaultSBMLLevel = Integer.parseInt(value); break;
					case DEFAULTSBMLVERSION: defaultSBMLVersion = Integer.parseInt(value); break;
					case MODELNAME: modelName = value; break;
					case BOUNDARYCOMPARTMENTNAME: boundaryCompartmentName = value; break;
					case EXCHANGEREACTIONSUFFIX: exchangeReactionSuffix = value; break;
					case SPECIESPREFIX: speciesPrefix = value; break;
					case REACTIONPREFIX: reactionPrefix = value; break;
					case COMPARTMENTABREVS: {
						String[] values = value.split(";");
						for (String compartmentAbrevPair : values) {
							String[] pair = compartmentAbrevPair.split(",");
							compartmentAbrevs.put(pair[0], pair[1]);
						}
					} break;
					default: {
						
					} break;
				}
			}
			
			assert outputDirectory != null;
			assert defaultCompartment != null;
			assert defaultSBMLLevel != 0;
			assert defaultSBMLVersion != 0;
			assert modelName != null;
			assert boundaryCompartmentName != null;
			assert exchangeReactionSuffix != null;
			assert speciesPrefix != null;
			assert reactionPrefix != null;
			assert compartmentAbrevs.size() != 0;
			
			setSettings(outputDirectory, defaultCompartment, defaultSBMLLevel, defaultSBMLVersion, modelName, boundaryCompartmentName, exchangeReactionSuffix, speciesPrefix, reactionPrefix, compartmentAbrevs);
			
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
	
	/**
	 * Initializes default settings for generating SBML models and for translating information from EcoCyc for use
	 * in the SBML models.
	 */
	public void setDefaultSettings() {
		OutputDirectory = "/home/jesse/Desktop/output/";
		DefaultCompartment = "CCO-CYTOSOL";
		DefaultSBMLLevel = 2;
		DefaultSBMLVersion = 1;
		ModelName = "CBiRC";
		
		BoundaryCompartmentName = "Boundary";
		ExchangeReactionSuffix = "Exchange";
		
		SpeciesPrefix = "M";
		ReactionPrefix = "R";
		
		CompartmentAbrevs.put("CCO-CYTOSOL", "c");
		CompartmentAbrevs.put("CCO-PERI-BAC", "periBac");
		CompartmentAbrevs.put("CCO-PERIPLASM", "p");
		CompartmentAbrevs.put("CCO-EXTRACELLULAR", "e");
		CompartmentAbrevs.put("CCO-CYTOPLASM", "cp");
		CompartmentAbrevs.put("CCO-UNKNOWN-SPACE", "unk");
		CompartmentAbrevs.put("CCO-IN", "i");
		CompartmentAbrevs.put("CCO-OUT", "o");
		CompartmentAbrevs.put("CCO-MIDDLE", "m");
		CompartmentAbrevs.put("Boundary", "b");
	}
	
	/**
	 * Initializes default settings for generating SBML models and for translating information from EcoCyc for use
	 * in the SBML models.
	 */
	public void setSettings(String outputDirectory, String defaultCompartment, int defaultSBMLLevel, int defaultSBMLVersion, String modelName, String boundaryCompartmentName, String exchangeReactionSuffix, String speciesPrefix, String reactionPrefix, HashMap<String, String> compartmentAbrevs) {
		OutputDirectory = outputDirectory;
		DefaultCompartment = defaultCompartment;
		DefaultSBMLLevel = defaultSBMLLevel;
		DefaultSBMLVersion = defaultSBMLVersion;
		ModelName = modelName;
		
		BoundaryCompartmentName = boundaryCompartmentName;
		ExchangeReactionSuffix = exchangeReactionSuffix;
		
		SpeciesPrefix = speciesPrefix;
		ReactionPrefix = reactionPrefix;
		
		CompartmentAbrevs = compartmentAbrevs;
	}
	
	/**
	 * This method will create a new genome-scale model from an EcoCyc database. All reactions are included other than 
	 * |Polynucleotide-Reactions| and |Protein-Reactions| reactions, which are filtered out. Generic reactions are
	 * instantiated, and boundary reactions are created for external metabolites. The model is then written to an
	 * SBML file.
	 */
	public void createGenomeScaleModelFromEcoCyc() {
		try {
			// 1) Create blank model
			System.out.println("Generating blank model ...");
			SBMLDocument doc = createBlankSBMLDocument(ModelName, DefaultSBMLLevel, DefaultSBMLVersion);
			
			// 2) Load all reactions
			System.out.println("Loading all reactions ...");
			ArrayList<Reaction> allReactions = Reaction.all(conn);
			ReactionNetwork reactionNetwork = new ReactionNetwork(conn, reactionListToReactionInstances(allReactions));
			
			// 3) Filter unwanted reactions
			System.out.println("Filtering unwanted reactions ...");
			ArrayList<String> classToFilter = new ArrayList<String>();
			classToFilter.add("|Polynucleotide-Reactions|");
			classToFilter.add("|Protein-Reactions|");
			reactionNetwork.filterReactions(classToFilter, null);
			
			// 4) Find and instantiate generics
			System.out.println("Instantiating generic reactions ...");
			reactionNetwork.generateSpecificReactionsFromGenericReactions();
			
			// 5) Add boundaries
			System.out.println("Adding boundary reactions ...");
			reactionNetwork.addBoundaryReactionsByCompartment("CCO-OUT");
			
			// 6) Generate SBML model
			System.out.println("Generating SBML model ...");
			generateSBMLModel(doc, reactionNetwork);
			
			// 7) Write revised model.
			System.out.println("Writing output ...");
			SBMLWriter writer = new SBMLWriter();
			writer.writeSBML(doc, OutputDirectory + "written_SBML.xml");
			
			// *Mapping*
//			System.out.println("Writing mapping output ...");
//			printBoundaryReactionMetaboliteList(boundaryResults, "boundaryMetabolites");
			
			// Print statistics
			reactionNetwork.printNetworkStatistics();
			
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
		ArrayList<ReactionInstance> reactionInstances = reactionNetwork.reactions;
		
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
					if (!metabolites.contains(species.generateSpeciesID())) {
						Species newSpecies = model.createSpecies();
						String sid = species.generateSpeciesID();
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
				if (reaction.thisReactionFrame != null) newReaction.setId(reaction.generateReactionID());
				else if (reaction.parentReaction != null) newReaction.setId(reaction.generateReactionID());
				else newReaction.setId(reaction.generateReactionID());
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
					String sid = reactant.generateSpeciesID();
					SpeciesReference ref = newReaction.createReactant();
					ref.setSpecies(sid);
					ref.setStoichiometry(reactant.stoichiometry);
//					if (ref.setSpecies(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (ref.setStoichiometry(reactant.stoichiometry) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (newReaction.addReactant(ref) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				}
				for (MetaboliteInstance product : reaction.products) {
					String sid = product.generateSpeciesID();
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
				if (reaction.thisReactionFrame != null) newReaction.appendNotes("Gene Rule : " + reaction.reactionGeneRule(conn, false));//reactionGeneRule(reaction.thisReactionFrame.getLocalID(), false));
				else if (reaction.parentReaction != null) newReaction.appendNotes("Gene Rule : " + reaction.reactionGeneRule(conn, false));//reaction.reactionGeneRule(reaction.parentReaction.getLocalID(), false));
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
	 * Convert an ArrayList of JavaCycO Reaction objects into an ArrayList of ReactionInstances
	 * 
	 * @param reactions ArrayList of Reaction objects
	 * @return ArrayList of ReactionInstance objects
	 */
	private ArrayList<ReactionInstance> reactionListToReactionInstances(ArrayList<Reaction> reactions) {
		ArrayList<ReactionInstance> reactionInstances = new ArrayList<ReactionInstance>();
		try {
			for (Reaction reaction : reactions) {
				ReactionInstance reactionInstance = new ReactionInstance(null, reaction, reaction.getCommonName(), reaction.isReversible());
				reactionInstances.add(reactionInstance);
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		return reactionInstances;
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
	protected boolean isGeneralizedReaction(String reactionName) {
		boolean result = false;
		try {
//			Reaction reaction = loadReaction(reactionName);
			Reaction reaction = (Reaction)Reaction.load(conn, reactionName);
			ReactionInstance reactionInstance = new ReactionInstance(reaction);
			result = reactionInstance.isGeneralizedReaction(conn);
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
	 * Replace characters that are commonly used in EcoCyc with characters safe to use in SBML names and IDs.
	 * 
	 * @param input
	 * @return
	 */
	protected static String convertToSBMLSafe(String input) {
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
	
//	/**
//	 * TODO Remove and switch to Frame.load() 
//	 * 
//	 * @param id
//	 * @return
//	 */
//	private Compound loadCompound(String id) throws PtoolsErrorException {
//		Compound f = new Compound(conn, id);
//		if (f.inKB()) return f;
//		else return null;
//	}
	
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
	public enum Setting	{
		OUTPUTDIRECTORY,
		DEFAULTCOMPARTMENT,
		DEFAULTSBMLLEVEL,
		DEFAULTSBMLVERSION,
		MODELNAME,
		BOUNDARYCOMPARTMENTNAME,
		EXCHANGEREACTIONSUFFIX,
		SPECIESPREFIX,
		REACTIONPREFIX,
		COMPARTMENTABREVS,
	    NOVALUE;

	    public static Setting value(String setting) {
	        try {
	            return valueOf(setting.toUpperCase());
	        } catch (Exception e) {
	            return NOVALUE;
	        }
	    }   
	}
}
