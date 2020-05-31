package com.st.hackerrank.practice;
import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Solution04 {

    // Complete the catAndMouse function below.
    static String catAndMouse(int x, int y, int z) {

    	String retStr = "";
    	
    	int a = Math.abs(z - x);
    	int b = Math.abs(y - z);
    	
    	if(a == b) {
    		retStr = "Mouse C";
    	} else if (a < b) {
    		retStr = "Cat A";
    	} else if (a > b) {
    		retStr = "Cat B";
    	}
    	
    	return retStr;

    }

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        int q = scanner.nextInt();
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        for (int qItr = 0; qItr < q; qItr++) {
            String[] xyz = scanner.nextLine().split(" ");

            int x = Integer.parseInt(xyz[0]);

            int y = Integer.parseInt(xyz[1]);

            int z = Integer.parseInt(xyz[2]);

            String result = catAndMouse(x, y, z);

            bufferedWriter.write(result);
            bufferedWriter.newLine();
        }

        bufferedWriter.close();

        scanner.close();
    }
}
