package com.st.hackerrank.practice;
import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Solution02 {

    // Complete the countingValleys function below.
    static int countingValleys(int n, String s) {

    	int countOfValleys = 0;
    	
    	int level = 0;
    	
    	final char up = 'U';
    	final char down = 'D';
    	
    	for(char step : s.toCharArray()) {
    		if(step == up) {
    			
    			level ++;
    			if(level == 0) countOfValleys ++;
    			
    		} else if (step == down) {
    			level --;
    		} else {
    			// do nothing
    		}
    	}
    	
    	return countOfValleys;

    }

    private static final Scanner scanner = new Scanner(System.in);

    public static void main1(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        int n = scanner.nextInt();
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        String s = scanner.nextLine();

        int result = countingValleys(n, s);

        bufferedWriter.write(String.valueOf(result));
        bufferedWriter.newLine();

        bufferedWriter.close();

        scanner.close();
    }
    
    public static void main(String[] args) throws IOException {
    	
    	int result = countingValleys(8, "DDUUUUDD");
    	
    	System.out.println(result);
    }
}
