package edu.rice.cs.dynamicjava;

import java.io.*;
import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;
import edu.rice.cs.plt.text.TextUtil;
import edu.rice.cs.plt.text.ArgumentParser;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.reflect.PathClassLoader;
import edu.rice.cs.dynamicjava.interpreter.Interpreter;
import edu.rice.cs.dynamicjava.interpreter.InterpreterException;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

public final class DynamicJava {
  
  private DynamicJava() {}
  
  public static void main(String... args) throws IOException {
    debug.log();

    ArgumentParser argParser = new ArgumentParser();
    argParser.supportOption("classpath", IOUtil.WORKING_DIRECTORY.toString());
    argParser.supportAlias("cp", "classpath");
    argParser.supportOption("batch",0);
    argParser.supportAlias("b", "batch");
    ArgumentParser.Result parsedArgs = argParser.parse(args);
    Iterable<File> cp = IOUtil.parsePath(parsedArgs.getUnaryOption("classpath"));
    boolean batchMode = parsedArgs.hasOption("batch");

    Interpreter i = new Interpreter(Options.DEFAULT, new PathClassLoader(cp));
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String input;
    String prompt = "> ";

    System.out.println("Welcome to Dynamic Java. Workding directory is: "+IOUtil.WORKING_DIRECTORY.toString());
    do {
      if(!batchMode){
        System.out.print(prompt);
        System.out.flush();
        input = in.readLine();
      }
      else{
        input = in.readLine();
        if(input != null){
          System.out.print(prompt);
          System.out.println(input);
        }
      }        

      if (input != null) {
        try {
          Option<Object> result = i.interpret(input);
          result.apply(new OptionVisitor<Object, Void>() {
            public Void forSome(Object o) { System.out.println(TextUtil.toString(o)); return null; }
            public Void forNone() { return null; }
          });
        }
        catch (InterpreterException e) { e.printUserMessage(); debug.log(e); }
        catch (RuntimeException e) {
          System.out.println("INTERNAL ERROR: Uncaught exception");
          e.printStackTrace(System.out);
        }
      }
    } while (input != null);
  }
  
}
