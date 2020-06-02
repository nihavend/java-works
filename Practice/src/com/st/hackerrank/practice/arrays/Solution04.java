package com.st.hackerrank.practice.arrays;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Solution04 {
	
	public static void main(String[] args) {
    	
    	// int q[] = {7, 1, 3, 2, 4, 5, 6};
    	// int q[] = {2, 3, 4, 1, 5};
		int q[] = {1, 3, 5, 2, 4, 6, 7};
    	
    	System.out.println(minimumSwaps(q));
    }

	// Complete the minimumSwaps function below.
	static int minimumSwaps(int[] arr) {

		int l = arr.length;

		int swaps[] = new int[l];

		boolean swapped = false;

		while (true) {
			for (int i = 0; i < arr.length; i++) {
				if (i + 1 < l) {
					if (arr[i] != i + 1) {
						swapped = true;
						swaps[arr[i]-1] += 1;
						int tmp = arr[i];
						arr[i] = arr[arr[i] - 1];
						arr[tmp - 1] = tmp;
					}
				}
			}
			if (!swapped)
				break;
			swapped = false;
		}

		int sum = 0;
		for (int i : swaps) {
			sum += i;
		}
		
		return sum;

	}

	private static final Scanner scanner = new Scanner(System.in);

	public static void main01(String[] args) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

		int n = scanner.nextInt();
		scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		int[] arr = new int[n];

		String[] arrItems = scanner.nextLine().split(" ");
		scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		for (int i = 0; i < n; i++) {
			int arrItem = Integer.parseInt(arrItems[i]);
			arr[i] = arrItem;
		}

		int res = minimumSwaps(arr);

		bufferedWriter.write(String.valueOf(res));
		bufferedWriter.newLine();

		bufferedWriter.close();

		scanner.close();
	}
}
