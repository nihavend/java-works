package com.likya.java14;

public class FeaturesJEP305 {

	/**
	 *  JDK Enhancement Proposal JEP-305
	 *  Pattern Matching for instanceof (Preview)
	 * @param args
	 */
		
	
	public static void main(String[] args) {

		Object obj = "Thisi the test of JEP-305";
		
		// Before 
		if (obj instanceof String) {
		    String s = (String) obj;
		    System.out.println("Length of string : "  + s.length());
		}
		
		// After
		if (obj instanceof String s) {
		    System.out.println("Length of string : "  + s.length());
		}
		    

	}

}
