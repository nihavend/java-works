package com.st.hackerrank.practice;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

// Repeated String

public class Solution05 {
	
	public static void main(String[] args) throws IOException {
		
		String str = "aba"; //"a"; //"aba"; //"abcac";
		long n = 10;
		
		// long n = 1000000000000L;
		// long n = 100000;
		
		System.out.println(repeatedString(str, n));
	}

    // Complete the repeatedString function below.
    static long repeatedString(String s, long n) {

    	if(s.length() < 1 || s.length() > 100) return 0 ;
    	
    	if(n < 1 || n > Math.pow(10, 12)) return 0 ;
    	
    	long loopCount = n / s.length();
    	
    	if(loopCount * s.length() == n) {
    		return (s.length() - s.replace("a", "").length()) * loopCount;
    	} else {
    		return ((s.length() - s.replace("a", "").length()) * loopCount) + (s.substring(0, (int)(n - loopCount * s.length())).length() - s.substring(0, (int)(n - loopCount * s.length())).replace("a", "").length());
    	}
    	
    }

    private static final Scanner scanner = new Scanner(System.in);

    public static void main01(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        String s = scanner.nextLine();

        long n = scanner.nextLong();
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        long result = repeatedString(s, n);

        bufferedWriter.write(String.valueOf(result));
        bufferedWriter.newLine();

        bufferedWriter.close();

        scanner.close();
    }
}
