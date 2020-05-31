package com.st.hackerrank.practice;

import java.util.ArrayList;
import java.util.Arrays;

public class MagicMatrix {

	public static void main(String[] args) {

		
		ArrayList<String> matrix = new ArrayList<String>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9"));
		
		// matrix = new ArrayList<String>(Arrays.asList("5", "3", "4", "1", "5", "8", "6", "4", "2"));
		
		// matrix = new ArrayList<String>(Arrays.asList("8", "3", "4", "1", "5", "9", "6", "7", "2"));
		
		// matrix = new ArrayList<String>(Arrays.asList("4", "9", "2", "3", "5", "7", "8", "1", "5"));
		
		// matrix = new ArrayList<String>(Arrays.asList("4", "9", "2", "3", "5", "7", "8", "1", "6"));
		
		// matrix = new ArrayList<String>(Arrays.asList("4", "8", "2", "4", "5", "7", "6", "1", "6"));
		
		matrix = new ArrayList<String>(Arrays.asList("4", "9", "2", "3", "5", "7", "8", "1", "6"));
		
		int A = 15;
		
		System.out.println(satirlar(matrix, A));
		
		System.out.println(sutunlar(matrix, A));

		System.out.println(diagonalright(matrix, A));
		
		System.out.println(diagonalleft(matrix, A));
	}
	
	private static boolean satirlar(ArrayList<String> matrix, int A) {
		
		int total = 0;
		
		for(int i = 0; i < 9; i += 3) {
			for(int k = i; k < 3 + i ; k++ ) {
				// System.out.println(i + ":" + k);
				total = total + Integer.parseInt(matrix.get(k));
			}
			
			if (total != A) return false;
			total = 0;
		}
		
		return true;
	}

	private static boolean sutunlar(ArrayList<String> matrix, int A) {
		
		int total = 0;
		
		for(int i = 0; i < 3; i ++) {
			for(int k = i; k < 9 ; k +=3 ) {
				// System.out.println(i + ":" + k);
				total = total + Integer.parseInt(matrix.get(k));
			}
			
			if (total != A) return false;
			total = 0;
		}
		
		return true;
	}
	
	private static boolean diagonalright(ArrayList<String> matrix, int A) {
		
		int total = 0;
		
		for(int i = 0; i < 3; i ++) {
			//System.out.println(i + ":" + i * 4);
			total = total + Integer.parseInt(matrix.get(i * 4));
		}
		
		if (total != A) return false;
		
		return true;
	}

	private static boolean diagonalleft(ArrayList<String> matrix, int A) {
		
		int total = 0;
		
		for(int i = 2; i < 7; i +=2 ) {
			// System.out.println(i + ":" + i);
			total = total + Integer.parseInt(matrix.get(i));
		}
		
		if (total != A) return false;
		
		return true;
	}

}
