package com.st.hackerrank.practice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DrawCost {
	
	static int counter = 0;
	
	static final int A = 15;
	
	public static void main(String[] args) {

		ArrayList<String> inputs = new ArrayList<String>(Arrays.asList("1", "2", "3", "4", "6", "7", "8", "9"));
		// ArrayList<String> inputs = new ArrayList<String>(Arrays.asList("1", "2", "3"));

		ArrayList<String> outputs = new ArrayList<String>();
		
		printPermutn(inputs, outputs);
		
		// String s = "ab"; 
		// printPermutnStr(s, ""); 
	}
	
	// Function to print all the permutations of str 
    static void printPermutnStr(String str, String ans) 
    { 
  
        // If string is empty 
        if (str.length() == 0) { 
            System.out.print(ans + " "); 
            return; 
        } 
  
        for (int i = 0; i < str.length(); i++) { 
  
            // ith character of str 
            char ch = str.charAt(i); 
  
            // Rest of the string after excluding  
            // the ith character 
            String ros = str.substring(0, i) +  
                         str.substring(i + 1); 
  
            // Recurvise call 
            printPermutnStr(ros, ans + ch); 
        } 
    } 
	
	static void printPermutn(ArrayList<String> input, ArrayList<String> output) 
    { 
  
        // If string is empty 
        if (input.size() == 0) { 
        	saveMatrice(output);
            return; 
        } 
  
        for (int i = 0; i < input.size(); i++) { 

            String ch = input.get(i); 

            ArrayList<String> newInput = new ArrayList<String>(input);
            newInput.remove(ch);
            
            ArrayList<String> newOutput = new ArrayList<String>(output);
            newOutput.add(ch);
  
            // Recurvise call 
            printPermutn(newInput, newOutput); 
        } 
    } 
	
	public static void main1(String[] args) {
		
		int list [] = {1,2,3,4,5,6,7,8,9};
		
		int max = 9;
		int min = 1;
		
		ArrayList<Integer> mx = new ArrayList<Integer>();

		
		Random random = new Random();
		
		
		
		while(true) {
			int rnd = random.nextInt(max - min + 1) + min;
			System.out.println(rnd);
			
			if(rnd == 5) continue;

			if(mx.indexOf(rnd) < 0) mx.add(rnd);

			if(mx.size() == 8) break;
		}
		
		mx.add(4,5);
		// System.out.println(mx.toString());
	
		// printMatrice(mx);
		
		for(int i = 0; i < 40000; i ++) System.out.println(i);;
		
		System.out.println("Finished !");
		
	}
	
	public static void saveMatrice(ArrayList<String> mx) {
		ArrayList<String> newList = new ArrayList<String>(mx);
		newList.add(4, "5");
		mx = newList;
		// System.out.println(mx.toString());
		
		boolean b = isMagic(mx);
		
		if(!b) return;
		
		System.out.println(++counter + ". matris");
		
		printMatrice(mx);
	}
	
	public static void printMatrice(ArrayList<String> mx) {
		
		System.out.println(mx.subList(0, 3).toString());
		System.out.println(mx.subList(3, 6).toString());
		System.out.println(mx.subList(6, 9).toString());
		System.out.println();
	}
	
	public static boolean isMagic(ArrayList<String> mx) {
		
		int total = 0;
		/*
		mx.get(0)
		mx.get(3)
		mx.get(6)
		
		mx.get(1)
		mx.get(4)
		mx.get(7)
		
		mx.get(2)
		mx.get(5)
		mx.get(8)
		*/
		
		// System.out.println();
		for(int i = 0; i < 3; i ++) {
			for(int k = 0; k <= i + 6; k += 3) {
				total = total + Integer.parseInt(mx.get(k));
				// System.out.print(mx.get(k) + " ");
			}
			// System.out.println();
			if (total != A) {
				return false;
			} else {
//				if(i == 1) {
//					System.out.println(total);
//					printMatrice(mx);
//					System.out.println();
//				}
			}
			// printMatrice(mx);
			total = 0;
		}
		
		
		for(int i = 0; i < 3; i +=3 ) {
			for(String str : mx.subList(i, i + 3)) {
				total = total + Integer.parseInt(str);
			}
			if (total != A) {
				return false;
			} 
			printMatrice(mx);
			total = 0;
		}
		
		// printMatrice(mx);
		
		return true;
	}

	public static void mainx(String[] args) {
	
		int mtrxG [][] = {
				{5, 3, 4},
				{1, 5, 8},
				{6, 4, 2}
		};
		
		int mtrxS [][] = {
				{8, 3, 4},
				{1, 5, 9},
				{6, 7, 2}
		};
		
		int mtrx0 [][] = {
				{4, 9, 2},
				{3, 5, 7},
				{8, 1, 5}
		};
		
		int mtrx1 [][] = {
				{4, 8, 2},
				{4, 5, 7},
				{6, 1, 6}
		};
		
		int cost = 0;
		
		int A = 3;
		
		
		int Mt = 0;
		
		int q1 = 0;
		int q2 = 0;
		int q3 = 0;

		int Tg = sumMtrx(mtrxG);
		
		int Ts = sumMtrx(mtrxS);
		
		Mt = magicSum(mtrxG);
		
		// System.out.println("Sample magic solved : " + magicSum(mtrx1));
		
		// System.out.println("Real Total Given : " + Tg);
		// System.out.println("Real Total Solved : " + Ts);
		
		// System.out.println("Cost : " + Math.abs(Ts - Tg));
		
		// System.out.println("Magic Sum : " + Mt);
		
		Tg = sumMtrx(mtrx0);
		
		System.out.println("Real Total Given : " + Tg);

		cost = Math.abs(Tg - 45);
		
		System.out.println("d: " +  5 + " A:" + 15 + " Cost: " + cost);
		
//		for(int cnt = 1; cnt <= 9 ; cnt ++) {
//			
//			int a = (cnt * 3);
//			double m = ((3 * a)  - cnt) / 4;
//			
//			//System.out.println("M : " + m);
//			// Mm = 9 * b
//			cost = Tg - (a * 3);
//			System.out.println("d: " +  cnt + " A:" + a + " Cost: " + cost);
//		}
		
	}
	
	public static int magicSum(int [][] m) {
		
		int toplam = 0;
		
		toplam = 3 * (m[0][0] + m[0][2] + m[2][0] + m[2][2]) + 2 * (m[1][0] + m[1][2] + m[0][1] + m[2][1]) + 4 * m[1][1];
		
		return toplam;
	}
	
	public static int sumMtrx(int [][] m) {
		
		int toplam = 0;
		
		for(int i[] : m) {
			for(int j: i) {
				toplam = toplam + j;
			}
		}
		
		return toplam;
	}

}


