package edu.iastate.cycmodeler.util;

import java.util.ArrayList;

import edu.iastate.cycmodeler.model.MetaboliteInstance;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;

/**
 * A class to facilitate generic reaction instantiation by holding the results of the listCombinations method.
 * 
 * @author Jesse Walsh
 */
public class ListCombinations {
	public ArrayList<String> nameList;
	public ArrayList<ArrayList<String>> listOfTuples;
	
	
	public static ListCombinations listCombinations(JavacycConnection conn, ArrayList<MetaboliteInstance> genericReactants, ArrayList<MetaboliteInstance> genericProducts) throws PtoolsErrorException {
		ArrayList<NamedList> listSet = new ArrayList<NamedList>();
		for (MetaboliteInstance genericTerm : genericReactants) {
			ArrayList<String> instancesOfGenericTerm = new ArrayList<String>();
			for (Object instance : conn.getClassAllInstances(genericTerm.getMetaboliteID())) {
				if (!Frame.load(conn, (String) instance).isClassFrame()) instancesOfGenericTerm.add(instance.toString());
			}
			if (genericTerm.chemicalFormula_ != null && !genericTerm.chemicalFormula_.equalsIgnoreCase("")) {
				System.err.println(genericTerm.getMetaboliteID() + " " + genericTerm.chemicalFormula_);
				instancesOfGenericTerm.add(genericTerm.getMetaboliteID());//Failsafe, if the "CLASS" metabolite has a proper chemical formula, it can be balanced
			}
			if (instancesOfGenericTerm.size() == 0) {
				return null;
//				instancesOfGenericTerm.add(genericTerm.getMetaboliteID()); // DO NOT ADD A GENERIC BACK INTO THE LIST!!
			}
			NamedList namedList = new NamedList(genericTerm.getMetaboliteID(), instancesOfGenericTerm);
			if (!listSet.contains(namedList)) listSet.add(namedList);
		}
		
		for (MetaboliteInstance genericTerm : genericProducts) {
			ArrayList<String> instancesOfGenericTerm = new ArrayList<String>();
			for (Object instance : conn.getClassAllInstances(genericTerm.getMetaboliteID())) {
				if (!Frame.load(conn, (String) instance).isClassFrame()) instancesOfGenericTerm.add(instance.toString());
			}
			if (genericTerm.chemicalFormula_ != null && !genericTerm.chemicalFormula_.equalsIgnoreCase("")) {
				System.err.println(genericTerm.getMetaboliteID() + " " + genericTerm.chemicalFormula_);
				instancesOfGenericTerm.add(genericTerm.getMetaboliteID());//Failsafe, if the "CLASS" metabolite has a proper chemical formula, it can be balanced
			}
			if (instancesOfGenericTerm.size() == 0) {
				return null;
//				instancesOfGenericTerm.add(genericTerm.getMetaboliteID()); // DO NOT ADD A GENERIC BACK INTO THE LIST!!
			}
			NamedList namedList = new NamedList(genericTerm.getMetaboliteID(), instancesOfGenericTerm);
			if (!listSet.contains(namedList)) listSet.add(namedList);
		}
		
		return listCombinations(listSet);
	}
	/**
	 * "All possible combinations from a list of sublists problem"
	 * 
	 * This function takes in a list of lists and returns every possible combination of 1 item from each sublist.
	 * Thus, if the lists [1,2,3], [4,5,6], and [7,8,9] were input, then the output would be
	 * [1,4,7], [1,4,8], [1,4,9], [1,5,7], [1,5,8], [1,5,9] ...
	 * This method was written as a way to instantiate generic terms in a reaction. Each generic term in a reaction has 
	 * a list of possible values, and every possible combination of terms is needed.
	 * 
	 * @param listOfNamedLists List of NamedList objects. Name of list should be the class metabolite, while the list is
	 * each instance of the class metabolite.
	 * @return ListCombinationResults where the name list is a list of all the names of the NamedList input, and a list of tuples
	 * which represent each possible combination of the items in the named list. Order of names in the NameList matches the order
	 * of the items in the tuples.
	 */
	@SuppressWarnings("unchecked")
	public static ListCombinations listCombinations(ArrayList<NamedList> listOfNamedLists) {
		if (listOfNamedLists == null || listOfNamedLists.size() < 1) return new ListCombinations(new ArrayList<String>(), new ArrayList<ArrayList<String>>());
		
		NamedList namedList = listOfNamedLists.remove(0);
		ListCombinations results = listCombinations(listOfNamedLists);
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
	
	public ListCombinations(ArrayList<String> nameList, ArrayList<ArrayList<String>> listOfTuples) {
		this.nameList = nameList;
		this.listOfTuples = listOfTuples;
	}
}
