package com.st.hackerrank.practice.arrays;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Solution01 {

	
	public static void main(String[] args) throws IOException {

		int test01[][] = {
				{-9, -9, -9,  1, 1, 1}, 
				{ 0, -9,  0,  4, 3, 2},
				{-9, -9, -9,  1, 2, 3},
				{ 0,  0,  8,  6, 6, 0},
				{ 0,  0,  0, -2, 0, 0},
				{ 0,  0,  1,  2, 4, 0}
				};
						
		int test02[][] = {
				{-9, -9, -9,  1, 1, 1}, 
				{ 0, -9,  0,  4, 3, 2},
				{-9, -9, -9,  1, 2, 3},
				{ 0,  0,  8,  6, 6, 0},
				{ 0,  0,  0, -2, 0, 0},
				};

		int test03[][] = {
				{-9, -9, -9,  1, 1}, 
				{ 0, -9,  0,  4, 3},
				{-9, -9, -9,  1, 2},
				{ 0,  0,  8,  6, 6},
				{ 0,  0,  0, -2, 0},
				{ 0,  0,  1,  2, 4}
				};

		int test04[][] = {
				{ 1, 1, 1, 0, 0, 0}, 
				{ 0, 1, 0, 0, 0, 0},
				{ 1, 1, 1, 0, 0, 0},
				{ 0, 0, 2, 4, 4, 0},
				{ 0, 0, 0, 2, 0, 0},
				{ 0, 0, 1, 2, 4, 0}
				};
		
		int result = hourglassSum(test03);
		
	}
	
    // Complete the hourglassSum function below.
    static int hourglassSum(int[][] arr) {
    	
    	
    	// ilk yapýlan ve submit edilen çözüm
    	int max = -1000;
    	int satirlar =  arr.length;
    	int sutunlar = arr[0].length;
    	
    	for(int sa = 0; sa < satirlar; sa ++) {
    		if(sa + 2 >= satirlar) break;
    		for(int su = 0; su < sutunlar; su ++) {
    			if(su + 2 >= sutunlar) break;
    			int sum = hourGlass.sum(sa, su, arr); 
    			if(sum > max) {
    				max = sum; 
    			}
    		}
    	}
    	
    	// ikinci çözüm submit edilmedi
    	System.out.println("Max1 : " + max);
    	
    	int max2 = -1000;
		int sum2 = 0;
		for(int i = 0; i < arr.length; i ++) {
			if(i + 2 >= arr.length) break;
			for(int j = 0; j < arr[0].length; j ++) {
				if(j + 2 >= arr[0].length) break;
				sum2 = arr[i][j] + arr[i][j + 1] + arr[i][j + 2] + arr[i + 1][j + 1] + arr[i + 2][j] + arr[i + 2][j + 1] + arr[i + 2][j + 2];
				//System.out.println("Sum2 :" + sum2);
				if(sum2 > max2) {
    				max2 = sum2; 
    			}
			}
		}
		
		System.out.println("Max2 : " + max2);
    	
    	return max;
    }
    
    
    private static final Scanner scanner = new Scanner(System.in);

    public static void main01(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));

        int[][] arr = new int[6][6];

        for (int i = 0; i < 6; i++) {
            String[] arrRowItems = scanner.nextLine().split(" ");
            scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

            for (int j = 0; j < 6; j++) {
                int arrItem = Integer.parseInt(arrRowItems[j]);
                arr[i][j] = arrItem;
            }
        }

        int result = hourglassSum(arr);

        bufferedWriter.write(String.valueOf(result));
        bufferedWriter.newLine();

        bufferedWriter.close();

        scanner.close();
    }
    
    static class hourGlass {

    	public static int sum(int sa, int su, int[][] arr) {

			int satir = sa;
			int sutun = su;

    		int hourGlassArray[] = {satir, sutun, satir, sutun + 1, satir, sutun + 2, 
   				 satir + 1, sutun + 1,
   				 satir + 2, sutun, satir + 2, sutun + 1, satir + 2, sutun + 2
   				};
    		
			int sum = 0;
			for(int i =  0; i < hourGlassArray.length; i++) {
				sum += arr[hourGlassArray[i++]][hourGlassArray[i]];
			}
			
			// System.out.println("Sum :" + sum);
			
			return sum;
    	}

    }
    
    static class hourGlass02 {

    	public static int sum(int sa, int su, int[][] arr) {

			int satir = sa;
			int sutun = su;

    		int hourGlassArray[] = {satir, sutun, satir, sutun + 1, satir, sutun + 2, 
   				 satir + 1, sutun + 1,
   				 satir + 2, sutun, satir + 2, sutun + 1, satir + 2, sutun + 2
   				};
    		
			int sum = 0;
			for(int i =  0; i < hourGlassArray.length; i++) {
				sum += arr[hourGlassArray[i++]][hourGlassArray[i]];
			}
			
			return sum;
    	}

    }
    
    static class hourGlass01 {

    	public static int sum(int sa, int su, int[][] arr) {

			int satir = sa;
			int sutun = su;

    		int hourGlassArray[] = {satir, sutun, satir, sutun + 1, satir, sutun + 2, 
   				 satir + 1, sutun + 1,
   				 satir + 2, sutun, satir + 2, sutun + 1, satir + 2, sutun + 2
   				};
    		
    		
			// int sum2 = 0;
    		// int index = 0;
    		
    		/*
			sum2 = arr[hourGlassArray[index ++]][hourGlassArray[index ++]] + arr[hourGlassArray[index ++]][hourGlassArray[index ++]] + arr[hourGlassArray[index ++]][hourGlassArray[index ++]] +
			      arr[hourGlassArray[index ++]][hourGlassArray[index ++]] + 
			      arr[hourGlassArray[index ++]][hourGlassArray[index ++]] + arr[hourGlassArray[index ++]][hourGlassArray[index ++]] + arr[hourGlassArray[index ++]][hourGlassArray[index]];
			*/
			int sum = 0;
			for(int i =  0; i < hourGlassArray.length; i++) {
				sum += arr[hourGlassArray[i++]][hourGlassArray[i]];
			}
			
			// System.out.println("açýk : " + sum + " for loop : " + sum2);
			
			//for(int i =  0; i < hourGlassArray.length; i++) {
			// System.out.println(arr[hourGlassArray[i++]][hourGlassArray[i]]);
			//}
			// System.out.println("=================================================");
			return sum;
    	}

    }
}
