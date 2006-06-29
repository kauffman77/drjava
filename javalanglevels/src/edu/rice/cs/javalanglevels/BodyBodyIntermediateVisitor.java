/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project:
 * http://sourceforge.net/projects/drjava/ or http://www.drjava.org/
 *
 * DrJava Open Source License
 * 
 * Copyright (C) 2001-2005 JavaPLT group at Rice University (javaplt@rice.edu)
 * All rights reserved.
 *
 * Developed by:   Java Programming Languages Team
 *                 Rice University
 *                 http://www.cs.rice.edu/~javaplt/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"),
 * to deal with the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 *     - Redistributions of source code must retain the above copyright 
 *       notice, this list of conditions and the following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright 
 *       notice, this list of conditions and the following disclaimers in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor
 *       use the term "DrJava" as part of their names without prior written
 *       permission from the JavaPLT group.  For permission, write to
 *       javaplt@rice.edu.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR 
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS WITH THE SOFTWARE.
 * 
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.javalanglevels;

import edu.rice.cs.javalanglevels.tree.*;
import edu.rice.cs.javalanglevels.parser.*;
import java.util.*;
import java.io.*;

import junit.framework.TestCase;


/*
 * Language Level Visitor that represents the Intermediate Language Level.  Enforces constraints during the
 * first walk of the AST (checking for langauge specific errors and building the symbol table).
 * This class enforces things that are common to all contexts reachable within a method body or other body 
 * (not class or interface body) at the Intermediate Language Level). 
 */
public class BodyBodyIntermediateVisitor extends IntermediateVisitor {

  /**The MethodData of this method.*/
  private BodyData _bodyData;
  
  /*
   * Constructor for BodyBodyElementaryVisitor.
   * @param bodyData  The BodyData that encloses the context we are visiting.
   * @param file  The source file this came from.
   * @param packageName  The package the source file is in
   * @importedFiles  A list of classes that were specifically imported
   * @param importedPackages  A list of package names that were specifically imported
   * @param classDefsInThisFile  A list of the classes that are defined in the source file
   * @param continuations  A hashtable corresponding to the continuations (unresolved Symbol Datas) that will need to be resolved
   */
  public BodyBodyIntermediateVisitor(BodyData bodyData, File file, String packageName, LinkedList<String> importedFiles, 
                             LinkedList<String> importedPackages, LinkedList<String> classDefsInThisFile, Hashtable<String, Pair<SourceInfo, LanguageLevelVisitor>> continuations) {
    super(file, packageName, importedFiles, importedPackages, classDefsInThisFile, continuations);
    _bodyData = bodyData;
  }
  
  /*Give an appropriate error*/
  public void forMethodDefDoFirst(MethodDef that) {
    _addError("Methods definitions cannot appear within the body of another method or block.", that);
  }
  
  /* There is currently no way to differentiate between a block statement and
   * an instance initializer in a braced body given the general nature of a 
   * braced body.  Whenever an instance initialization is visited in a method
   * body, we must assume that it is a block statement.
   */
  public void forInstanceInitializer(InstanceInitializer that) {
    forBlock(that.getCode());
  }

 /* Visit this BlockData with a new BodyBodyIntermediate visitor after making sure no errors need to be thrown.*/
  public void forBlock(Block that) {
    forBlockDoFirst(that);
    if (prune(that)) {  return; }
    BlockData bd = new BlockData(_bodyData);
    _bodyData.addBlock(bd);
    that.getStatements().visit(new BodyBodyIntermediateVisitor(bd, _file, _package, _importedFiles, _importedPackages, _classNamesInThisFile, continuations));
    forBlockOnly(that);
  }
  
  /** 
   * Visit the block as in forBlock(), but first add the exception parameter as a variable in 
   * that block.
   */
  public void forCatchBlock(CatchBlock that) {
    forCatchBlockDoFirst(that);
    if (prune(that)) { return; }
    
    Block b = that.getBlock();
    forBlockDoFirst(b);
    if (prune(b)) { return; }
    BlockData bd = new BlockData(_bodyData);
    _bodyData.addBlock(bd);
    
    VariableData exceptionVar = formalParameters2VariableData(new FormalParameter[]{ that.getException() }, bd)[0];
    if (prune(that.getException())) { return; }
    bd.addVar(exceptionVar);
    
    b.getStatements().visit(new BodyBodyIntermediateVisitor(bd, _file, _package, _importedFiles, _importedPackages, _classNamesInThisFile, continuations));
    forBlockOnly(b);
    forCatchBlockOnly(that);
  }
  
  /*Add the variables that were declared to the body data and make sure that no two
   * variables have the same name.*/
  public void forVariableDeclarationOnly(VariableDeclaration that) {
    if (!_bodyData.addFinalVars(_variableDeclaration2VariableData(that, _bodyData))) {
      _addAndIgnoreError("You cannot have two variables with the same name.", that);
    }
  }
  
  /**Override method in IntermediateVisitor that throws an error here.*/
  public void forTryCatchStatementDoFirst(TryCatchStatement that) {
    //do nothing!  No errors to throw here.
  }
    
  
  /*
   * Make sure that no modifiers appear before the InnerClassDef, and then delegate.
   */
//  public void forInnerClassDef(InnerClassDef that) {
//    if (_bodyData.hasModifier("static")) {
//      _addError("Static classes can not be declared inside of methods", that);
//    }
//    else {
//      handleInnerClassDef(that, _bodyData, getQualifiedClassName(_bodyData.getSymbolData().getName()) + "$" + _bodyData.getSymbolData().preincrementLocalClassNum() + that.getName().getText());
//    }
//  }

  /**
   * Delegate to method in LLV
   */
  public void forComplexAnonymousClassInstantiation(ComplexAnonymousClassInstantiation that) {
    complexAnonymousClassInstantiationHelper(that, _bodyData);
  }

  /**
   * Delegate to method in LLV
   */
  public void forSimpleAnonymousClassInstantiation(SimpleAnonymousClassInstantiation that) {
    simpleAnonymousClassInstantiationHelper(that, _bodyData);
  }
  
  /**
   * If this is the body of a constructor, it is not legal to reference the 'this' literal.
   * So, check to see if this is a constructor, and if so, throw an error.
   * This should catch both the ComplexThisReference and the SimpleThisReference case.
   */
  //TODO: Long term, it might be nice to create a ConstructorBodyIntermediateVisitor, so this check is not necessary here.
  public void forThisReferenceDoFirst(ThisReference that) {
    if (isConstructor(_bodyData)) {
      _addAndIgnoreError("You cannot reference the field 'this' inside a constructor at the Intermediate Level", that);
    }
  }

  /**
   * Call the super method to convert these to a VariableData array, then make sure that
   * each VariableData is set to be final, as required at the Intermediate level.
   * @param enclosingData  The Data which contains the variables
   */
  protected VariableData[] _variableDeclaration2VariableData(VariableDeclaration vd, Data enclosingData) {
    VariableData[] vds = llVariableDeclaration2VariableData(vd, enclosingData);
    for (int i = 0; i < vds.length; i++) {
      if (vds[i].getMav().getModifiers().length > 0) {
        StringBuffer s = new StringBuffer("the keyword(s) ");
        String[] modifiers = vds[i].getMav().getModifiers();
        for (int j = 0; j<modifiers.length; j++) {s.append("\"" + modifiers[j] + "\" ");}
        _addAndIgnoreError("You cannot use " + s + "to declare a local variable at the Intermediate level", vd);
      }
      vds[i].setFinal();

    }
    return vds;
  }
  
  /**
   * Test most of the methods declared above right here:
   */
  public static class BodyBodyIntermediateVisitorTest extends TestCase {
    
    private BodyBodyIntermediateVisitor _bbv;
    
    private SymbolData _sd1;
    private MethodData _md1;
    private ModifiersAndVisibility _publicMav = new ModifiersAndVisibility(JExprParser.NO_SOURCE_INFO, new String[] {"public"});
    private ModifiersAndVisibility _protectedMav = new ModifiersAndVisibility(JExprParser.NO_SOURCE_INFO, new String[] {"protected"});
    private ModifiersAndVisibility _privateMav = new ModifiersAndVisibility(JExprParser.NO_SOURCE_INFO, new String[] {"private"});
    private ModifiersAndVisibility _packageMav = new ModifiersAndVisibility(JExprParser.NO_SOURCE_INFO, new String[0]);
    private ModifiersAndVisibility _abstractMav = new ModifiersAndVisibility(JExprParser.NO_SOURCE_INFO, new String[] {"abstract"});
    private ModifiersAndVisibility _finalMav = new ModifiersAndVisibility(JExprParser.NO_SOURCE_INFO, new String[] {"final"});
    
    
    public BodyBodyIntermediateVisitorTest() {
      this("");
    }
    
    public BodyBodyIntermediateVisitorTest(String name) {
      super(name);
    }
    
    public void setUp() {
      _sd1 = new SymbolData("i.like.monkey");
      _md1 = new MethodData("methodName", _publicMav, new TypeParameter[0], SymbolData.INT_TYPE, 
                                   new VariableData[0], 
                                   new String[0],
                                   _sd1,
                                   null);

      errors = new LinkedList<Pair<String, JExpressionIF>>();
      symbolTable = new Symboltable();
      visitedFiles = new LinkedList<Pair<LanguageLevelVisitor, edu.rice.cs.javalanglevels.tree.SourceFile>>();      
      _hierarchy = new Hashtable<String, TypeDefBase>();
      _classesToBeParsed = new Hashtable<String, Pair<TypeDefBase, LanguageLevelVisitor>>();
      _bbv = new BodyBodyIntermediateVisitor(_md1, new File(""), "", new LinkedList<String>(), new LinkedList<String>(), new LinkedList<String>(), new Hashtable<String, Pair<SourceInfo, LanguageLevelVisitor>>());
      _bbv.continuations = new Hashtable<String, Pair<SourceInfo, LanguageLevelVisitor>>();
      _bbv._resetNonStaticFields();
      _bbv._importedPackages.addFirst("java.lang");
      _errorAdded = false;
    }
    
    public void testForMethodDefDoFirst() {
      ConcreteMethodDef cmd = new ConcreteMethodDef(JExprParser.NO_SOURCE_INFO, 
                                                    _packageMav, 
                                                    new TypeParameter[0], 
                                                    new PrimitiveType(JExprParser.NO_SOURCE_INFO, "int"), 
                                                    new Word(JExprParser.NO_SOURCE_INFO, "methodName"),
                                                    new FormalParameter[0],
                                                    new ReferenceType[0], 
                                                    new BracedBody(JExprParser.NO_SOURCE_INFO, new BodyItemI[0]));
      cmd.visit(_bbv);
      assertEquals("There should be one error.", 1, errors.size());
      assertEquals("The error message should be correct.", 
                   "Methods definitions cannot appear within the body of another method or block.",
                   errors.get(0).getFirst());
    }
    
    /* These last two tests are shared with ClassBodyIntermediateVisitor,
     * perhaps we could factor them out. */
    
    public void testForVariableDeclarationOnly() {
      // Check one that works
      VariableDeclaration vdecl = new VariableDeclaration(JExprParser.NO_SOURCE_INFO,
                                                       _packageMav,
                                                       new VariableDeclarator[] {
        new UninitializedVariableDeclarator(JExprParser.NO_SOURCE_INFO, 
                               new PrimitiveType(JExprParser.NO_SOURCE_INFO, "double"), 
                               new Word (JExprParser.NO_SOURCE_INFO, "field1")),
        new UninitializedVariableDeclarator(JExprParser.NO_SOURCE_INFO, 
                               new PrimitiveType(JExprParser.NO_SOURCE_INFO, "boolean"), 
                               new Word (JExprParser.NO_SOURCE_INFO, "field2"))});
      VariableData vd1 = new VariableData("field1", _finalMav, SymbolData.DOUBLE_TYPE, false, _bbv._bodyData);
      VariableData vd2 = new VariableData("field2", _finalMav, SymbolData.BOOLEAN_TYPE, false, _bbv._bodyData);
      vdecl.visit(_bbv);
      assertEquals("There should not be any errors.", 0, errors.size());
      assertTrue("field1 was added.", _md1.getVars().contains(vd1));
      assertTrue("field2 was added.", _md1.getVars().contains(vd2));
      
      // Check one that doesn't work
      VariableDeclaration vdecl2 = new VariableDeclaration(JExprParser.NO_SOURCE_INFO,
                                                        _packageMav,
                                                        new VariableDeclarator[] {
        new UninitializedVariableDeclarator(JExprParser.NO_SOURCE_INFO, 
                                            new PrimitiveType(JExprParser.NO_SOURCE_INFO, "double"), 
                                            new Word (JExprParser.NO_SOURCE_INFO, "field3")),
        new UninitializedVariableDeclarator(JExprParser.NO_SOURCE_INFO, 
                                            new PrimitiveType(JExprParser.NO_SOURCE_INFO, "int"), 
                                            new Word (JExprParser.NO_SOURCE_INFO, "field3"))});
      VariableData vd3 = new VariableData("field3", _finalMav, SymbolData.DOUBLE_TYPE, false, _bbv._bodyData);
      vdecl2.visit(_bbv);
      assertEquals("There should be one error.", 1, errors.size());
      assertEquals("The error message should be correct", "You cannot have two variables with the same name.", errors.get(0).getFirst());
      assertTrue("field3 was added.", _md1.getVars().contains(vd3));
    }
    
//    public void testForOtherExpressionOnly() {
//      // Test that if the OtherExpressino contains a Word, that the Word is resolved.
//      assertFalse("java.lang.System should not be in the symbolTable.", symbolTable.containsKey("java.lang.System"));
//      Expression ex = new Expression( JExprParser.NO_SOURCE_INFO,
//                                     new ExpressionPiece[] { new OtherExpression(JExprParser.NO_SOURCE_INFO, 
//                                                                                 new Word(JExprParser.NO_SOURCE_INFO,
//                                                                                                              "System"))});
//      ex.visit(_bbv);
//////      System.out.println(errors.get(0).getFirst());
////      for (int i = 0; i < errors.size(); i++)
////        System.out.println(errors.get(i).getFirst());
//      assertEquals("There should not be any errors.", 0, errors.size());
//      assertTrue("java.lang.System should be in the symbolTable.", symbolTable.containsKey("java.lang.System"));
//    }
    
    public void testForTryCatchStatement() {
      //Make sure that no error is thrown
      BracedBody emptyBody = new BracedBody(JExprParser.NO_SOURCE_INFO, new BodyItemI[0]);
      Block b = new Block(JExprParser.NO_SOURCE_INFO, emptyBody);

      NormalTryCatchStatement ntcs = new NormalTryCatchStatement(JExprParser.NO_SOURCE_INFO, b, new CatchBlock[0]);
      TryCatchFinallyStatement tcfs = new TryCatchFinallyStatement(JExprParser.NO_SOURCE_INFO, b, new CatchBlock[0], b);
      ntcs.visit(_bbv);
      tcfs.visit(_bbv);
      assertEquals("After visiting both NormalTryCatchStatement and TryCatchFinallyStatement, there should be no errors", 0, errors.size());
      
      //make sure that if there is an error in one of the bodies, it is caught:
      BracedBody errorBody = new BracedBody(JExprParser.NO_SOURCE_INFO, new BodyItemI[] {
        new ExpressionStatement(JExprParser.NO_SOURCE_INFO, 
                                new BitwiseOrExpression(JExprParser.NO_SOURCE_INFO, new SimpleNameReference(JExprParser.NO_SOURCE_INFO, new Word(JExprParser.NO_SOURCE_INFO, "i")), new IntegerLiteral(JExprParser.NO_SOURCE_INFO, 10)))});
      Block errorBlock = new Block(JExprParser.NO_SOURCE_INFO, errorBody);
      
      ntcs = new NormalTryCatchStatement(JExprParser.NO_SOURCE_INFO, errorBlock, new CatchBlock[0]);
      ntcs.visit(_bbv);
      assertEquals("Should be one error", 1, errors.size());
      assertEquals("Error message should be correct", "Bitwise or expressions cannot be used at any language level.  Perhaps you meant to compare two values using regular or (||)", errors.getLast().getFirst());
      
      //make sure that if there is an error in one of the catch statements, it is caught:
      UninitializedVariableDeclarator uvd = new UninitializedVariableDeclarator(JExprParser.NO_SOURCE_INFO, new PrimitiveType(JExprParser.NO_SOURCE_INFO, "int"), new Word(JExprParser.NO_SOURCE_INFO, "i"));
      FormalParameter fp = new FormalParameter(JExprParser.NO_SOURCE_INFO, uvd, false);

      tcfs = new TryCatchFinallyStatement(JExprParser.NO_SOURCE_INFO, b, new CatchBlock[] {
        new CatchBlock(JExprParser.NO_SOURCE_INFO, fp, errorBlock)}, b);
        
     tcfs.visit(_bbv);
     assertEquals("Should be two errors", 2, errors.size());
     assertEquals("Error message should be correct", "Bitwise or expressions cannot be used at any language level.  Perhaps you meant to compare two values using regular or (||)", errors.getLast().getFirst());
    }
    
    public void testForThisReferenceDoFirst() {
      SimpleThisReference str = new SimpleThisReference(JExprParser.NO_SOURCE_INFO);
      ComplexThisReference ctr = new ComplexThisReference(JExprParser.NO_SOURCE_INFO, new SimpleNameReference(JExprParser.NO_SOURCE_INFO, new Word(JExprParser.NO_SOURCE_INFO, "field")));

      //if a this reference occurs outside of a constructor, no error
      _bbv._bodyData = _md1;
      str.visit(_bbv);
      ctr.visit(_bbv);
      assertEquals("Should be no errors", 0, errors.size());
           
      
      //if a this reference occurs in a constructor, give an error
      MethodData constr = new MethodData("monkey", _publicMav, new TypeParameter[0], _sd1, 
                                   new VariableData[0], 
                                   new String[0],
                                   _sd1,
                                   null);
      _bbv._bodyData = constr;
      str.visit(_bbv);
      assertEquals("Should be 1 error", 1, errors.size());
      assertEquals("Error message should be correct", "You cannot reference the field 'this' inside a constructor at the Intermediate Level", errors.getLast().getFirst());
      
      ctr.visit(_bbv);
      assertEquals("Should be 2 errors", 2, errors.size());
      assertEquals("Error message should be correct", "You cannot reference the field 'this' inside a constructor at the Intermediate Level", errors.getLast().getFirst());
      
    }
    
  }
}