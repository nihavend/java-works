package com.st.hackerrank.practice.arrays;
import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.management.MXBean;

public class Solution03 {

	
	
    public static void main(String[] args) {
    	
    	// int q[] = {2, 1, 5, 3, 4};
    	// int q[] = {2, 5, 1, 3, 4};
    	int q[] = {5, 1, 2, 3, 7, 8, 6, 4};
    	// int q[] = {1, 2, 5, 3, 7, 8, 6, 4};
    	
    	// q = getRandomArray(10000);
    	
    	minimumBribes(q);
    }
    
 static void minimumBribes(int[] q) {
    	
    	int moves[] = new int[q.length];
    	
    	boolean isChaotic = false;
    	boolean moved = false;
    	
    	while(true) {
	    	for(int i = 0; i < q.length; i ++) {
	    		if(i + 1 < q.length) {
	    			if(q[i] > q[i+1]) {
	    				moved = true;
	    				if((moves[q[i]-1] += 1) > 2) {
	    					isChaotic = true; 
	    					break;
	    				}
	    				int tmp = q[i];
	    				q[i] = q[i+1];
	    				q[i+1] = tmp;
	    			}
	    		}
	    	}
	    	if(isChaotic || !moved) break;
	    	moved = false;
    	}
    	
    	if(isChaotic) {
    		System.out.println("Too chaotic");
    	} else {
	    	int sum = 0;
	    	for(int i : moves) {
				sum += i;
			}
	    	System.out.println(sum);
    	}
    	
    }
	
    static void minimumBribes02(int[] q) {
    	int moves = 0;
    	for(int i = 0; i < q.length; i ++) {
    		if(q[i] != (i+1)) {
    			int move = Math.abs(q[i] - (i+1)); 
    			if(move > 2) {
    				System.out.println("Too chaotic");
    				return;
    			} 
    			moves += move;
    		}
    	}
    	
    	System.out.println(moves);
    }
    
    
	static boolean isCahotic(int moves[]) {
		for(int move : moves) {
			if(move > 2) return true;
		}
		return false;
	}
    // Complete the minimumBribes function below.
    static void minimumBribes01(int[] q) {
    	
    	// int orig[] = {1, 2, 3, 4, 5};
    	int moves[] = new int[q.length];
    	
    	boolean isChaotic = false;
    	boolean moved = false;
    	
    	while(true) {
	    	for(int i = 0; i < q.length; i ++) {
	    		if(i + 1 < q.length) {
	    			if(q[i] > q[i+1]) {
	    				moved = true;
	    				moves[q[i]-1] += 1;
	    				// moves[q[i+1]-1] += 1;
	    				int tmp = q[i];
	    				q[i] = q[i+1];
	    				q[i+1] = tmp;
	    				isChaotic = isCahotic(moves);
	    				if(isChaotic) break;
	    			}
	    		}
	    	}
	    	if(isChaotic || !moved) break;
	    	moved = false;
    	}
    	
    	if(isChaotic) {
    		System.out.println("Too chaotic");
    	} else {
	    	int sum = 0;
	    	for(int i : moves) {
				sum += i;
			}
	    	System.out.println(sum);
    	}
    	
    	
//    	for(int i = 0; i < q.length; i ++) {
//    		if(q[i] - i > 3) {
//    			System.out.println("Too chaotic");
//    			break;
//    		}
//    		
//    	}
    	
    }
    
    private static final Scanner scanner = new Scanner(System.in);

    public static void main01(String[] args) {
        int t = scanner.nextInt();
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        for (int tItr = 0; tItr < t; tItr++) {
            int n = scanner.nextInt();
            scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

            int[] q = new int[n];

            String[] qItems = scanner.nextLine().split(" ");
            scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

            for (int i = 0; i < n; i++) {
                int qItem = Integer.parseInt(qItems[i]);
                q[i] = qItem;
            }

            minimumBribes(q);
        }

        scanner.close();
    }
    
    
	public static int[] getRandomArray(int size) {
		
		ArrayList<Integer> list = new ArrayList<>(11);
		for (int i = 1; i <= size; i++) {
			list.add(i);
		}
		
		int[] a = new int[size];
		for (int count = 0; count < size; count++) {
			a[count] = list.remove((int) (Math.random() * list.size()));
		}
		
		int arr[] = new int[size];
		
		int index = 0;
		for(int i : a) {
			arr[index ++] = i;
		}
		
		return arr;
	}
}
