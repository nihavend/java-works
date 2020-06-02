package com.st.hackerrank.practice.arrays;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Solution02 {

    // Complete the rotLeft function below.
    static int[] rotLeft(int[] a, int d) {

    	int tmp[] = new int[a.length];
    	
    	int hedef = 0;
    	for(int index = 0; index < a.length; index ++) {
    		
    		hedef = index + d;
    		if(hedef >= a.length) hedef = hedef - a.length; 
    		tmp[index] = a[hedef];
    		
    	}
    	
    	return tmp;
    	
    }
    
    public static void main(String[] args) throws IOException {
    	int a[] = {1, 2, 3, 4, 5};
    	
    	int[] result = rotLeft(a, 4);

        for (int i = 0; i < result.length; i++) {
            System.out.print(String.valueOf(result[i]));

            if (i != result.length - 1) {
                System.out.print(" ");
            }
        }
    }

    private static final Scanner scanner = new Scanner(System.in);

    public static void main01(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        String[] nd = scanner.nextLine().split(" ");

        int n = Integer.parseInt(nd[0]);

        int d = Integer.parseInt(nd[1]);

        int[] a = new int[n];

        String[] aItems = scanner.nextLine().split(" ");
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        for (int i = 0; i < n; i++) {
            int aItem = Integer.parseInt(aItems[i]);
            a[i] = aItem;
        }

        int[] result = rotLeft(a, d);

        for (int i = 0; i < result.length; i++) {
            bufferedWriter.write(String.valueOf(result[i]));

            if (i != result.length - 1) {
                bufferedWriter.write(" ");
            }
        }

        bufferedWriter.newLine();

        bufferedWriter.close();

        scanner.close();
    }
}
