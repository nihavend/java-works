package com.st.hackerrank.practice;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class Solution01 {

    // Complete the sockMerchant function below.
    static int sockMerchant(int n, int[] ar) {

        HashMap <String, Integer> h = new HashMap<String, Integer>();

        for(int i = 0; i < ar.length; i++) {
            
            String color = "" + ar[i];
            
            if(h.containsKey(color)) {
                h.put(color, new Integer(h.get(color).intValue() + 1));
            } else {
                h.put(color, new Integer(1));
            }
        }

        int renkler = 0;

        for (Integer i : h.values()) {
            renkler = renkler + (i / 2);
        }

        return renkler;
    }
    
    private static final Scanner scanner = new Scanner(System.in);
    
    public static void main01(String[] args) throws IOException {
    	
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        int n = scanner.nextInt();
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        int[] ar = new int[n];

        String[] arItems = scanner.nextLine().split(" ");
        scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

        for (int i = 0; i < n; i++) {
            int arItem = Integer.parseInt(arItems[i]);
            ar[i] = arItem;
        }

        int result = sockMerchant(n, ar);

        bufferedWriter.write(String.valueOf(result));
        bufferedWriter.newLine();

        bufferedWriter.close();

        scanner.close();
    }

    public static void main(String[] args) throws IOException {
 
        

        int[] ar = {1, 1, 3, 1, 2, 1, 3, 3, 3, 3};

        int n = ar.length;

        int result = sockMerchant(n, ar);


        System.out.println(String.valueOf(result));
        

    }
}
