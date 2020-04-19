package com.likya.java14;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class FeaturesJEP368 {

	public static void main(String[] args) {
		
//		oldFashionedHtml();
//		brandNewHtml();
//		
//		oldFashionedSQL();
//		brandNewSQL();
//
//		oldFashionedJS();
//		brandNewJS();
//	
// 		newEscapeChars();
//		
// 		concatTextBlocks();
		
 		newMethods();
		
	}

	
	public static void oldFashionedHtml() {
		
		String html = "<html>\n" +
	              "    <body>\n" +
	              "        <p>Hello, world</p>\n" +
	              "    </body>\n" +
	              "</html>\n";
		
		System.out.println(html);
	}
	
	public static void brandNewHtml() {
		String html = """
	              <html>
	                  <body>
	                      <p>Hello, world</p>
	                  </body>
	              </html>
	              """;
	
	    System.out.println(html);
	}
	
	public static void oldFashionedSQL() {
		String query = "SELECT `EMP_ID`, `LAST_NAME` FROM `EMPLOYEE_TB`\n" +
	               "WHERE `CITY` = 'INDIANAPOLIS'\n" +
	               "ORDER BY `EMP_ID`, `LAST_NAME`;\n";
		
		System.out.println(query);
	}

	public static void brandNewSQL() {
		String query = """
	               SELECT `EMP_ID`, `LAST_NAME` FROM `EMPLOYEE_TB`
	               WHERE `CITY` = 'INDIANAPOLIS'
	               ORDER BY `EMP_ID`, `LAST_NAME`;
	               """;
		System.out.println(query);
	}

	public static void oldFashionedJS() {
		
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
		Object obj;
		try {
			obj = engine.eval("function hello() {\n" +
			                         "    print('\"Hello, world\"');\n" +
			                         "}\n" +
			                         "\n" +
			                         "hello();\n");
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void brandNewJS() {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
		Object obj;
		try {
			obj = engine.eval("""
		                         function hello() {
		                             print('"Hello, world"');
		                         }
		                         
		                         hello();
		                         """);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		                         		
		
	}
	
	public static void newEscapeChars() {
		String literal = "Lorem ipsum dolor sit amet, consectetur adipiscing " +
                "elit, sed do eiusmod tempor incididunt ut labore " +
                "et dolore magna aliqua.";
		
		String text = """
                Lorem ipsum dolor sit amet, consectetur adipiscing \
                elit, sed do eiusmod tempor incididunt ut labore \
                et dolore magna aliqua.\
                """;
        
        System.out.println("literal => " + literal);
        System.out.println("text => " + text);     
        
	}
	
	public static void concatTextBlocks() {
		
		String type = "Array";
		
		String code1 = """
	              public void print($type o) {
	                  System.out.println(Objects.toString(o));
	              }
	              """.replace("$type", type);

	    System.out.println(code1);
	    
	    String code2 = String.format("""
	              public void print(%s o) {
	                  System.out.println(Objects.toString(o));
	              }
	              """, type);
	    System.out.println(code2);
	    
	    String source = """
                public void print(%s object) {
                    System.out.println(Objects.toString(object));
                }
                """.formatted(type);
	    System.out.println(source);
	}

	public static void newMethods() {

		String data = """
				""";
		
		data = readFileAsString();
	    System.out.println(data);
		System.out.println(data.stripIndent());
		System.out.println(data.stripIndent().translateEscapes());

	}
	
	public static String readFileAsString() { 
		
		String fileName = "test.txt"; 
	    String data = """
	    		""";
	    
	    try {
			data = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		} 	    
	    
	    return data;
	    
	  } 
}
