package edu.iastate.cycmodeler.logic;

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

import edu.iastate.cycmodeler.model.ReactionInstance;
import edu.iastate.javacyco.Compound;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.Pathway;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;
import edu.iastate.javacyco.TransportReaction;

public class CycCompare {
	// Global Variables
	private JavacycConnection conn = null;
	
	public static void main(String[] args) {
		
	}
	
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
	
	
	
	
	
	
	// **********   COPIES   *****************
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
}
