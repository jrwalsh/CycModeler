package edu.iastate.cycmodeler.util;

import java.util.ArrayList;


/**
	 * Internal class to facilitate generic reaction instantiation by holding a metabolite class as "name" and all
	 * metabolite instances of the class in "list".
	 * 
	 * @author Jesse Walsh
	 */
public class NamedList {
	public String name;
	public ArrayList<String> list;
	
	public NamedList(String name, ArrayList<String> list) {
		this.name = name;
		this.list = list;
	}
	
	/**
	Test the names of two NamedLists for equality. Does not compare the list itself.
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
