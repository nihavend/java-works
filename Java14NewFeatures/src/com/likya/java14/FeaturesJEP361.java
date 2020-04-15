package com.likya.java14;

public class FeaturesJEP361 {

	enum days {
		MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
	};

	public static void main(String[] args) {
		/*
		brandNewSwitchStatement();
		
		traditionalSwitchStatement();
		
		howMany(1);

		brandNewSwitchStatement();

		switchAsExpression();

		switchAsExpressionWithYield();

		yieldWithTraditionalSwitch();
		*/
		
		// exhaustiveness1();
		
		exhaustiveness2();
		
	}

	static void howMany(int k) {
		
		System.out.println("Running sample : howMany ");
	    switch (k) {
	        case 1  -> System.out.println("one");
	        case 2  -> System.out.println("two");
	        default -> System.out.println("many");
	    }
	}

	public static void yieldWithTraditionalSwitch() {
		
		System.out.println("Running sample : yieldWithTraditionalSwitch ");
		String s = "Fuuu";
		
		int result = switch (s) {
			
		    case "Foo": 
		        yield 1;
		    case "Bar":
		        yield 2;
		    default:
		        System.out.println("Neither Foo nor Bar, hmmm...");
		        yield 0;
		};
		
		System.out.println("value of result : " + result);
	}

	public static int f(int k) {
		return k + 1;
	}

	public static void switchAsExpressionWithYield() {
	
		System.out.println("Running sample : switchAsExpressionWithYield ");
		var day = days.TUESDAY;
		
		int j = switch (day) {
		    case MONDAY  -> 0;
		    case TUESDAY -> 1;
	    	default      -> {
	    		int k = day.toString().length();
	    		int result = f(k);
	    		yield result;
	    	}
		};
	
		System.out.println("j : " + j);
	}

	public static void switchAsExpression() {
	
		System.out.println("Running sample : switchAsExpression ");
		int k = 0;
		
		System.out.println(
		        switch (k) {
		            case  1 -> "one";
		            case  2 -> "two";
		            default -> "many";
		        }
		    );
	}

	public static void brandNewSwitchStatement() {
		
		System.out.println("Running sample : brandNewSwitchStatement ");
		var day = days.TUESDAY;
		
		switch (day) {
		    case MONDAY, FRIDAY, SUNDAY -> System.out.println(6);
		    case TUESDAY                -> System.out.println(7);
		    case THURSDAY, SATURDAY     -> System.out.println(8);
		    case WEDNESDAY              -> System.out.println(9);
		}
		
	}

	public static void traditionalSwitchStatement() {

		System.out.println("Running sample : traditionalSwitchStatement ");
		var day = days.WEDNESDAY;

		switch (day) {
		case MONDAY:
		case FRIDAY:
		case SUNDAY:
			System.out.println(6);
			break;
		case TUESDAY:
			System.out.println(7);
			break;
		case THURSDAY:
		case SATURDAY:
			System.out.println(8);
			break;
		case WEDNESDAY:
			System.out.println(9);
			break;
		}

	}
	
	public static void exhaustiveness1() {

		System.out.println("Running sample : exhaustiveness1 ");
		var day = days.FRIDAY;
		
		int i = switch (day) {
	    	case MONDAY -> {
	    		System.out.println("Monday"); 
	    		// ERROR! Block doesn't contain a yield statement
	    	}
	    	default -> 1;
		};
	}

	public static void exhaustiveness2() {

		System.out.println("Running sample : exhaustiveness2 ");
		var day = days.FRIDAY;
		
		int i = switch (day) {
			case MONDAY, TUESDAY, WEDNESDAY: 
				yield 0;
			default: 
				System.out.println("Second half of the week");
				// ERROR! Group doesn't contain a yield statement
		};
	}

	public static void exhaustiveness3() {

		System.out.println("Running sample : exhaustiveness1 ");
		var day = days.FRIDAY;
		
		int i = switch (day) {
	    	case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY -> {
	    		yield 1;
	    	}
		};
	}

}
