package edu.iastate.cycmodeler.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.model.AbstractReactionInstance;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

public class ReactionChooser {
	ArrayList<Reaction> reactions_;
	JavacycConnection conn;
	
	public ReactionChooser (JavacycConnection conn) {
		this.reactions_ = new ArrayList<Reaction>();
		this.conn = conn;
	}
	
	public void getAllReactions() throws PtoolsErrorException {
		this.reactions_ = Reaction.all(conn);
		
		System.out.println("ReactionList : " + this.reactions_.size());
	}
	
	public ArrayList<Reaction> removeSpecificReactions(ArrayList<String> reactionIDs) {
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
	
	public ArrayList<Reaction> removeReactionsByClass(ArrayList<String> classIDs) throws PtoolsErrorException {
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
	
	public ArrayList<Reaction> removeReactionsByMetaboliteClass(ArrayList<String> classIDs) throws PtoolsErrorException {
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
}
