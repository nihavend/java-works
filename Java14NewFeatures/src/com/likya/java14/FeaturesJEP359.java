package com.likya.java14;

public class FeaturesJEP359 {

	record Cube(int x, int y, int z) {};

	record Range(int lo, int hi) {
		  public Range {
		    if (lo > hi)  /* referring here to the implicit constructor parameters */
		      throw new IllegalArgumentException(String.format("(%d,%d)", lo, hi));
		  }
	}

	public static void main(String[] args) {

		record mytestclass = new Cube(1, 2, 3);

		System.out.println(mytestclass.x());
		System.out.println(mytestclass.y());
		System.out.println(mytestclass.z());

		System.out.println(mytestclass.toString());
		
		record rangeTest = new Range(5, 4); 
	}

}
