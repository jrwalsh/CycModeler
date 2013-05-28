package edu.iastate.cycmodeler.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.Pathway;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

public class ReactionChooser {
	ArrayList<Reaction> reactions_;
	
	public ReactionChooser (String reactionConfigFile) {
		this.reactions_ = new ArrayList<Reaction>();
		loadConfigFile(reactionConfigFile);
	}
	
	private void loadConfigFile(String fileName) {
		ArrayList<String> classToFilter = new ArrayList<String>();
		ArrayList<String> metaboliteClassToFilter = new ArrayList<String>();
		ArrayList<String> reactionsToFilter = new ArrayList<String>();
		
		File configFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(configFile));
			String text = null;
			
			// Parse settings from file
			while ((text = reader.readLine()) != null) {
				try {
					text = text.substring(0, text.indexOf("%"));
				} catch (Exception e) {
					// ignore, there are no comments
				}
				
				String command = "";
				String value = "";
				try {
					command = text.substring(0, text.indexOf(" "));
					value = text.substring(text.indexOf(" ")+1);
				} catch (Exception e) {
					// this is not an attribute/value pair
					command = text;
				}
				
				if (command.length() == 0) continue;
				command = command.trim();
				value = value.trim();
				
				switch (ReactionSetting.value(command)) {
					case INCLUDE: {
						System.err.println("Not implemented"); break;//TODO
					}
					case INCLUDE_ALL_REACTIONS: getAllReactions(); break;
					case INCLUDE_ALL_PATHWAYS: getAllPathwayReactions(); break;
					case EXCLUDE_REACTION_CLASS: classToFilter.add(value); break;
					case EXCLUDE_METABOLITE_CLASS: metaboliteClassToFilter.add(value); break;
					case EXCLUDE_REACTION: reactionsToFilter.add(value); break;
					default: {
						System.err.println("Unknown config command : " + command);
					} break;
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
		
		if (reactions_ == null || reactions_.size() == 0) {
			System.err.println("No reactions have been selected!");
			System.exit(-1);
		}
		
		try {
			removeReactionsByClass(classToFilter);
			removeReactionsByMetaboliteClass(metaboliteClassToFilter);
			removeSpecificReactions(reactionsToFilter);
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
	}

	private void getAllReactions() throws PtoolsErrorException {
		this.reactions_ = Reaction.all(CycModeler.conn);
		
		System.out.println("ReactionList : " + this.reactions_.size());
	}
	
	private void getAllPathwayReactions() throws PtoolsErrorException {
		ArrayList<String> pathwayLabels = CycModeler.conn.allPathways();
		for (String pathwayLabel : pathwayLabels) {
			Pathway pwy = (Pathway) Pathway.load(CycModeler.conn, pathwayLabel);
			this.reactions_.addAll(pwy.getReactions());
		}
	}
	
	private void getAllGenericReactions() throws PtoolsErrorException {
		ArrayList<Reaction> reactions = new ArrayList<Reaction>();
		for (Reaction reaction : Reaction.all(CycModeler.conn)) {
			boolean isGeneric = false;
			for (Frame reactant : reaction.getReactants()) {
				if (reactant.isClassFrame()) isGeneric = true;
			}
			for (Frame product : reaction.getProducts()) {
				if (product.isClassFrame()) isGeneric = true;
			}
			
			if (isGeneric) {
				reactions.add(reaction);
			}
		}
	}
	
	private ArrayList<Reaction> removeSpecificReactions(ArrayList<String> reactionIDs) {
		ArrayList<Reaction> newReactionList = new ArrayList<Reaction>();
		ArrayList<Reaction> removedReactions = new ArrayList<Reaction>();
		for (Reaction reaction : reactions_) {
			if (!reactionIDs.contains(reaction.getLocalID())) {
				newReactionList.add(reaction);
			} else {
				removedReactions.add(reaction);
			}
		}
		
		this.reactions_ = newReactionList;
		
		System.out.println("Removed by specific reaction : " + removedReactions.size());
		System.out.println("ReactionList : " + this.reactions_.size());
		
		print(arrayToString(removedReactions), "specific");
		
		return removedReactions;
	}
	
	private ArrayList<Reaction> removeReactionsByClass(ArrayList<String> classIDs) throws PtoolsErrorException {
		ArrayList<Reaction> newReactionList = new ArrayList<Reaction>();
		ArrayList<Reaction> removedReactions = new ArrayList<Reaction>();
		
		for (Reaction reaction : reactions_) {
			boolean remove = false;
			for (String classID : classIDs) {
				if (reaction.isGFPClass(classID)) {
					remove = true;
				}
			}
			
			if (!remove) {
				newReactionList.add(reaction);
			} else {
				removedReactions.add(reaction);
			}
		}
		
		this.reactions_ = newReactionList;
		
		System.out.println("Removed by class : " + removedReactions.size());
		System.out.println("ReactionList : " + this.reactions_.size());
		
		print(arrayToString(removedReactions), "by_class");
		
		return removedReactions;
	}
	
	private ArrayList<Reaction> removeReactionsByMetaboliteClass(ArrayList<String> classIDs) throws PtoolsErrorException {
		ArrayList<Reaction> newReactionList = new ArrayList<Reaction>();
		ArrayList<Reaction> removedReactions = new ArrayList<Reaction>();
		
		for (Reaction reaction : reactions_) {
			
			ArrayList<Frame> metabolites = new ArrayList<Frame>();
			metabolites.addAll(reaction.getReactants());
			metabolites.addAll(reaction.getProducts());
			
			boolean remove = false;
			for (String classID : classIDs) {
				for (Frame metabolite : metabolites) {
					if (metabolite.isGFPClass(classID)) {
						remove = true;
					}
				}
			}
			
			if (!remove) {
				newReactionList.add(reaction);
			} else {
				removedReactions.add(reaction);
			}
		}
		
		this.reactions_ = newReactionList;
		
		System.out.println("Removed by metabolite class : " + removedReactions.size());
		System.out.println("ReactionList : " + this.reactions_.size());
		
		print(arrayToString(removedReactions), "metabolite_class");
		
		return removedReactions;
	}
	
	public ArrayList<Reaction> getReactionList() {
		return reactions_;
	}
	
	private static String arrayToString(ArrayList<Reaction> reactions) {
		String printString = "";
		for (Reaction rxn : reactions) {
			try {
				printString += rxn.getLocalID() + "\t" + rxn.getCommonName()  + "\t";
				for (Frame met : rxn.getReactants()) {
					printString += met.getLocalID() + "\t";
				}
				for (Frame met : rxn.getProducts()) {
					printString += met.getLocalID() + "\t";
				}
				printString += "\n";
			} catch (PtoolsErrorException e) {
				e.printStackTrace();
			}
		}
		return printString;
	}
	
	private static void print(String printString, String fileName) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(CycModeler.parameters.OutputDirectory + fileName));
			out.write(printString);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Internal Classes
	private enum ReactionSetting	{
		INCLUDE, INCLUDE_ALL_REACTIONS, INCLUDE_ALL_PATHWAYS,
		EXCLUDE_REACTION_CLASS, EXCLUDE_METABOLITE_CLASS, EXCLUDE_REACTION,
		NOVALUE;

	    public static ReactionSetting value(String setting) {
	        try {
	            return valueOf(setting.toUpperCase());
	        } catch (Exception e) {
	            return NOVALUE;
	        }
	    }  
	}
}
