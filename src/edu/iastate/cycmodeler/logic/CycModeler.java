package edu.iastate.cycmodeler.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import edu.iastate.javacyco.Reaction;
import edu.iastate.javacyco.*;

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
	static protected JavacycConnection conn = null;
	static protected String OutputDirectory = "/home/jesse/Desktop/output/";
	public static String DefaultCompartment = "CCO-CYTOSOL";
	static protected int DefaultSBMLLevel = 2;
	static protected int DefaultSBMLVersion = 1;
	public static HashMap<String, String> CompartmentAbrevs = new HashMap<String, String>();
	public static String SpeciesPrefix = "M";
	public static String ReactionPrefix = "R";
	public static String BoundaryCompartmentName = "Boundary";
	public static String ExchangeReactionSuffix = "Exchange";
	static protected String ModelName = "DefaultName";
	static protected String ExternalCompartmentName = "CCO-EXTRACELLULAR";
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * 
	 * @param connectionString URL of the server running Pathway Tools
	 * @param port Port that JavaCycO is listening on
	 * @param organism Organism to connect to (i.e., selects which database to connect to)
	 * @param configFile Path to configuration file
	 */
	public CycModeler (String connectionString, int port, String organism, String configFile) {
		String CurrentConnectionString = connectionString;
		int CurrentPort = port;
		String CurrentOrganism = organism;

		conn = new JavacycConnection(CurrentConnectionString,CurrentPort);
		conn.selectOrganism(CurrentOrganism);
		
		initializeFromConfigFile(configFile);
	}
	
	
	/**
	 * Constructor: sets internal JavacycConnection object and initializes several default settings for generating models.
	 * Does not set an organism for the JavacycConnection object.  Expects that the connection object has already selected an organism.
	 * 
	 * @param connection Initialized connection object
	 * @param configFile Path to configuration file
	 */
	public CycModeler (JavacycConnection connection, String configFile) {
		conn = connection;
		initializeFromConfigFile(configFile);
	}
	

	// Methods
	public void initializeFromConfigFile(String fileName) {
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
			
			// Parse settings from file
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
						System.err.println("Unknown config command : " + command);
					} break;
				}
			}
			
			// Verify settings
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
			
			// Set variables
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
			ReactionNetwork reactionNetwork = ReactionNetwork.getReactionNetwork(conn, allReactions);
			
			// 3) Filter unwanted reactions
			System.out.println("Filtering unwanted reactions ...");
			ArrayList<String> classToFilter = new ArrayList<String>();
			classToFilter.add("|Polynucleotide-Reactions|");
			classToFilter.add("|Protein-Reactions|");
			reactionNetwork.filterReactions(classToFilter, null);
			
			// 4) Find and instantiate generics
			System.out.println("Instantiating generic reactions ...");
			reactionNetwork.generateSpecificReactionsFromGenericReactions();
			
			// 5) Add diffusion reactions
			System.out.println("Adding diffusion reactions ...");
			reactionNetwork.addPassiveDiffusionReactions("CCO-PERI-BAC" , "CCO-EXTRACELLULAR", (float) 610.00);
			
			// 6) Add boundaries
			System.out.println("Adding boundary reactions ...");
			reactionNetwork.addBoundaryReactionsByCompartment(ExternalCompartmentName);
			
			// 7) Generate SBML model
			System.out.println("Generating SBML model ...");
			generateSBMLModel(doc, reactionNetwork);
			
			// 8) Write revised model.
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
	}
	
	
	/**
	 * TODO
	 */
	public void createCustomModel() {
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
		HashSet<AbstractReactionInstance> reactionInstances = reactionNetwork.Reactions;
		
		// Get mappings to iAF1260
//		HashMap<String, ArrayList<String>> map = readMap("/home/Jesse/output/e2p");
		
		try {
			// Create compartment list
			for (AbstractReactionInstance reaction : reactionInstances) {
				ArrayList<MetaboliteInstance> reactantsProducts = new ArrayList<MetaboliteInstance>();
				reactantsProducts.addAll(reaction.Reactants);
				reactantsProducts.addAll(reaction.Products);
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
				reactantsProducts.addAll(reaction.Reactants);
				reactantsProducts.addAll(reaction.Products);
				for (MetaboliteInstance species : reactantsProducts) {
					if (!metabolites.contains(species.generateSpeciesID())) {
						Species newSpecies = model.createSpecies();
						String sid = species.generateSpeciesID();
						newSpecies.setId(sid);
						newSpecies.setName(species.MetaboliteFrame.getCommonName());
						newSpecies.setCompartment(model.getCompartment(convertToSBMLSafe(species.compartment_)).getId());
						newSpecies.setBoundaryCondition(false);
//						if (newSpecies.setId(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setName(species.metabolite.getCommonName()) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setCompartment(model.getCompartment(convertToSBMLSafe(species.compartment)).getId()) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//						if (newSpecies.setBoundaryCondition(false) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
						metabolites.add(sid);
						
						// Append Notes
						newSpecies.appendNotes("Palsson SID : \n");
						newSpecies.appendNotes("EcoCyc Frame ID : " + species.MetaboliteFrame.getLocalID() + "\n");
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
				newReaction.setName(reaction.Name);
				newReaction.setReversible(reaction.Reversible);
//				if (reaction.thisReactionFrame != null) {
//					if (newReaction.setId(convertToSBMLSafe(reaction.thisReactionFrame.getLocalID())) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				} else if (reaction.parentReaction != null) {
//					if (newReaction.setId(convertToSBMLSafe(reaction.parentReaction.getLocalID())) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				} else {
//					if (newReaction.setId(convertToSBMLSafe(reaction.name)) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//				}
//				if (newReaction.setName(reaction.name) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				
				for (MetaboliteInstance reactant : reaction.Reactants) {
					String sid = reactant.generateSpeciesID();
					SpeciesReference ref = newReaction.createReactant();
					ref.setSpecies(sid);
					ref.setStoichiometry(reactant.coefficient_);
//					if (ref.setSpecies(sid) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (ref.setStoichiometry(reactant.stoichiometry) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
//					if (newReaction.addReactant(ref) != libsbml.LIBSBML_OPERATION_SUCCESS) throw new Exception();
				}
				for (MetaboliteInstance product : reaction.Products) {
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
	
	
	
	
	
	// *** SANDBOX *** \\
//	private void printBoundaryReactionMetaboliteList(ArrayList<ReactionInstance> reactions, String fileName) {
//		String outString = "";
//		try {
//			for (ReactionInstance reaction : reactions) {
//				if (reaction.Reactants.size() > 0) {
//					Frame m = reaction.Reactants.get(0).MetaboliteFrame;
//					outString += m.getLocalID() + "\t" + m.getCommonName() + "\t" + reaction.Reactants.get(0).chemicalFormula_ + "\t";
//					
//					String keggID = "";
//					ArrayList dblinks = null;
//					if (m.hasSlot("DBLINKS")) dblinks = m.getSlotValues("DBLINKS");
//					for (Object dblink : dblinks) {
//						ArrayList<String> dbLinkArray = ((ArrayList<String>)dblink); 
//						if (dbLinkArray.get(0).contains("LIGAND-CPD")) {
//							keggID += dbLinkArray.get(1).replace("\"", "") + "\t";
//						}
//					}
//					keggID = keggID.split("\t")[0]; // Many kegg id entries are duplicated in EcoCyc v15.0, but we only need one
//					outString += keggID + "\n";
//				}
//			}
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
//		
//		printString(OutputDirectory + fileName, outString);
//	}
	
	
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
