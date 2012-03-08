/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project from http://www.drjava.org/
 * or http://sourceforge.net/projects/drjava/
 *
 * DrJava Open Source License
 * 
 * Copyright (C) 2001-2010 JavaPLT group at Rice University (javaplt@rice.edu).  All rights reserved.
 *
 * Developed by:   Java Programming Languages Team, Rice University, http://www.cs.rice.edu/~javaplt/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal with the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 *     - Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *       following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 *       following disclaimers in the documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the names of its contributors may be used to 
 *       endorse or promote products derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor use the term "DrJava" as part of their 
 *       names without prior written permission from the JavaPLT group.  For permission, write to javaplt@rice.edu.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * WITH THE SOFTWARE.
 * 
 *END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model.compiler;

import java.util.LinkedList;

import scala.Function1;
import scala.Function1$class;
import scala.Unit;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

import scala.tools.nsc.Settings;
import scala.tools.nsc.reporters.ConsoleReporter;
import scala.tools.nsc.reporters.Reporter;
import scala.tools.nsc.util.FakePos;
import scala.tools.nsc.util.Position;
import scala.tools.nsc.util.SourceFile;

import edu.rice.cs.drjava.model.DJError;
//import scala.tools.nsc.util.*;  /* { Position, NoPosition, SourceFile } */

/** DrJava Reporter class that extends scala.tools.nsc.ConsoleReporter.  The extension includes:
  * (i) a djErrors table of type LinkedList<DJError> for logging all errors generated by the scalc compiler and 
  * (ii) an overridden print method that adds each scalac error message to the table. 
  */
class DrJavaReporter extends ConsoleReporter {
  /** Error table passed in from client. */
  LinkedList<DJError> djErrors;
  
//  final Function1<String, BoxedUnit> scalacError = new AbstractFunction1<String, BoxedUnit>() {
//    public BoxedUnit apply(String msg) { 
//      error(new FakePos("scalac"), msg + "\n  scalac -help  gives more information");
//      return BoxedUnit.UNIT;
//    }
//  };

  DrJavaReporter(LinkedList<DJError> errors) { 
    super(new Settings(new AbstractFunction1<String, BoxedUnit>() {
      public BoxedUnit apply(String msg) { 
//        error(new FakePos("scalac"), msg + "\n  scalac -help  gives more information");
        return BoxedUnit.UNIT;
      }
    }));
    djErrors = errors; 
  }
  
  /* WHY IS THE RETURN TYPE void?  CONFLICTS WITH SCALA DOCUMENTATION */
  @Override public void print(Position pos, String msg, Severity severity) {
    if (pos != null && pos.isDefined()) {
      /* msg has a corresponding source file */
      SourceFile source = pos.source();
      /* TODO: drop reporting of messages with severity = NOTE. */
      djErrors.add(new DJError(source.file().file(), pos.line(), pos.column(), msg, ! severity.equals(ERROR())));
      /* pos.file() is a scala AbstractFile; pos.file().file() is the Java File backing it. */
    }
    else {
      /* pos is either null, NoPosition (a Scala object) or a FakePos (an instance of a Scala case class). 
       * No sourcefile is available in any of these cases. */
      djErrors.add(new DJError(null, -1, -1, msg, ! severity.equals(ERROR())));
    }
    super.print(pos, msg, severity);
  }
}
  