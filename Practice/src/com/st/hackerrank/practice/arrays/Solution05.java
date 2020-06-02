package com.st.hackerrank.practice.arrays;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Solution05 {

	public static void main(String[] args) {
		/*
		int n = 5;
		int q[][] = {{1, 2, 100}, {2, 5, 100}, {3, 4, 100}};
		*/
		
		int n = 10;
		//int q[][] = {{1, 5, 3}, {4, 8, 7}, {6, 9, 1}};
		int q[][] = {
				{2, 6, 8}, {3, 5, 7}, {1, 8, 1}, {5, 9, 15}
			};
		
    	System.out.println(arrayManipulation(n, q));
    }
	
    // Complete the arrayManipulation function below.
    static long arrayManipulation(int n, int[][] queries) {
    	
    	int m = queries.length;
    	
    	long arr[] = new long[n];
    	
    	for(int i = 0; i < m; i ++) {
        	int a = queries[i][0];
        	int b = queries[i][1];
    		int k = queries[i][2];

    		arr[a-1] += k;
    		if(b < n) arr[b] -= k;   		
    		// 
    	}
    	
    	long max = arr[0];
    	for(int i = 1; i < n; i++) {
    		arr[i] = arr[i - 1] + arr[i];
    		if(max < arr[i]) max = arr[i];
    	}
    	return max;
    }

    // Complete the arrayManipulation function below.
    static long arrayManipulation0(int n, int[][] queries) {
    	
    	int m = queries.length;
    	
    	int arr1[] = new int[n];
    	
    	for(int i = 0; i < m; i ++) {
        	int a = queries[i][0];
        	int b = queries[i][1];
    		int k = queries[i][2];

    		arr1[a-1] += k;
    		if(b < n) arr1[b] -= k;   		
    		// 
    	}
    	
    	int arr[] = new int[n];
    	for (int i = 0; i < n; i++) {
    		arr[i] = arr1[i];
    		System.out.print(arr1[i] + ", ");
    	}
    	System.out.println();    	
    	
    	long max = arr1[0];
    	System.out.print(arr1[0] + ", ");
    	for(int i = 1; i < n; i++) {
    		arr1[i] = arr1[i - 1] + arr1[i];
    		System.out.print(arr1[i] + ", ");
    		if(max < arr1[i]) max = arr1[i];
    	}
    	System.out.println();
    	System.out.println("Max : " + max);
    	
    	
		max = 0;
		long temp = 0;
		for (int i = 0; i < n; i++) {
			temp += arr[i];
			System.out.print(temp + ", ");
			if (temp > max)
				max = temp;
		}
		System.out.println();
    	System.out.println("Max : " + max);
    	return max;
    }
    
    private static final Scanner scanner = new Scanner(System.in);

    public static void main01(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        String[] nm = scanner.nextLine().split(" ");

        int n = Integer.parseInt(nm[0]);

        int m = Integer.parseInt(nm[1]);

        int[][] queries = new int[m][3];

        for (int i = 0; i < m; i++) {
            String[] queriesRowItems = scanner.nextLine().split(" ");
            scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

            for (int j = 0; j < 3; j++) {
                int queriesItem = Integer.parseInt(queriesRowItems[j]);
                queries[i][j] = queriesItem;
            }
        }

        long result = arrayManipulation(n, queries);

        bufferedWriter.write(String.valueOf(result));
        bufferedWriter.newLine();

        bufferedWriter.close();

        scanner.close();
    }
}
