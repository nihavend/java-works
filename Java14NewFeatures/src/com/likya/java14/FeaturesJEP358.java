package com.likya.java14;

import java.util.Random;

public class FeaturesJEP358 {

	
	public static void main(String[] args) {
		
		Integer a = test();
		
		// �rnek 7
		// a.toString().length();

		// �rnek 8
		// a.toString().getBytes().clone().toString();
		
		// �rnek 9 
		// String [][][] b = new String[1][1][1];
		
		// b[0] = null;
		
		// b[0][0][0].length();
		
		// �rnek 10
		
		// a = test1();
		// Integer b = new FeaturesJEP358().test();
		
		// boolean c = a.intValue() > b.intValue();
		
		// �rnek 11
		
		String testResult = test1().toString() + ":" + test().toString();
		
		System.out.println(testResult);
	}

	
	public static Integer test() {
		return null; 
	}
	
	public static Integer test1() {
		return new Random().nextInt(); 
	}
}
