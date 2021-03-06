/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.profiler.spi.java;

import java.util.Collections;
import java.util.Set;
import org.graalvm.visualvm.lib.profiler.api.java.SourceClassInfo;
import org.graalvm.visualvm.lib.profiler.api.java.SourceMethodInfo;
import org.openide.filesystems.FileObject;

/**
 * An SPI for {@linkplain org.graalvm.visualvm.lib.profiler.api.java.JavaProfilerSource} functionality providers
 * @author Jaroslav Bachorik
 */
public interface AbstractJavaProfilerSource {
    final public static AbstractJavaProfilerSource NULL = new AbstractJavaProfilerSource() {

        @Override
        public boolean isTest(FileObject fo) {
            return false;
        }

        @Override
        public boolean isApplet(FileObject fo) {
            return false;
        }

        @Override
        public SourceClassInfo getTopLevelClass(FileObject fo) {
            return null;
        }

        @Override
        public Set<SourceClassInfo> getClasses(FileObject fo) {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceClassInfo> getMainClasses(FileObject fo) {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceMethodInfo> getConstructors(FileObject fo) {
            return Collections.EMPTY_SET;
        }

        @Override
        public SourceClassInfo getEnclosingClass(FileObject fo, int position) {
            return null;
        }

        @Override
        public SourceMethodInfo getEnclosingMethod(FileObject fo, int position) {
            return null;
        }

        @Override
        public boolean isInstanceOf(FileObject fo, String[] classNames, boolean allRequired) {
            return false;
        }

        @Override
        public boolean isInstanceOf(FileObject fo, String className) {
            return false;
        }

        @Override
        public boolean hasAnnotation(FileObject fo, String[] annotationNames, boolean allRequired) {
            return false;
        }

        @Override
        public boolean hasAnnotation(FileObject fo, String annotation) {
            return false;
        }

        @Override
        public boolean isOffsetValid(FileObject fo, int offset) {
            return false;
        }

        @Override
        public SourceMethodInfo resolveMethodAtPosition(FileObject fo, int position) {
            return null;
        }

        @Override
        public SourceClassInfo resolveClassAtPosition(FileObject fo, int position, boolean resolveField) {
            return null;
        }
    };
    
    /**
     * @param fo The source file. Must not be NULL
     * @return Returns true if the source represents a junit tet
     */
    boolean isTest(FileObject fo);

    /**
     * @param fo The source file. Must not be NULL
     * @return Returns true if the source is a java applet
     */
    boolean isApplet(FileObject fo);

    /**
     * @param fo The source file. Must not be NULL
     * @return Returns {@linkplain SourceClassInfo} of a top level class
     */
    SourceClassInfo getTopLevelClass(FileObject fo);
    
    /**
     * Lists all top level classes contained in the source
     * @param fo The source file. Must not be NULL
     * @return Returns a set of {@linkplain SourceClassInfo} instances from a source
     */
    Set<SourceClassInfo> getClasses(FileObject fo);

    /**
     * Lists all main classes contained in the source
     * @param fo The source file. Must not be NULL
     * @return Returns a set of {@linkplain SourceClassInfo} instances from a source
     */
    Set<SourceClassInfo> getMainClasses(FileObject fo);
    
    /**
     * Lists all constructors contained in the source
     * @param fo The source file. Must not be NULL
     * @return Returns a set of {@linkplain SourceMethodInfo} instances from the source
     */
    Set<SourceMethodInfo> getConstructors(FileObject fo);

    /**
     * Finds a class present on the given position in the source
     * @param fo The source file. Must not be NULL
     * @param position The position in the source
     * @return Returns a {@linkplain SourceClassInfo} for the class present on the given position
     */
    SourceClassInfo getEnclosingClass(FileObject fo, final int position);

    /**
     * Finds a method present on the given position in the source
     * @param fo The source file. Must not be NULL
     * @param position The position in the source
     * @return Returns a {@linkplain SourceMethodInfo} for the method present on the given position
     */
    SourceMethodInfo getEnclosingMethod(FileObject fo, final int position);

    /**
     * Checks whether the source represents any or all of the provided superclasses/interfaces
     * @param fo The source file. Must not be NULL
     * @param classNames A list of required superclasses/interfaces
     * @param allRequired Require all(TRUE)/any(FALSE) provided superclasses/interfaces to match
     * @return Returns TRUE if the source represents any or all of the provided classes/interfaces
     */
    boolean isInstanceOf(FileObject fo, String[] classNames, boolean allRequired);

    /**
     * Checks whether the source represents the provided superclass/interface
     * @param fo The source file. Must not be NULL
     * @param className The required superclass/interface
     * @return Returns TRUE if the source represents the provided superclass/interface
     */
    boolean isInstanceOf(FileObject fo, String className);

    /**
     * Checks whether the source contains any/all provided annotations
     * @param fo The source file. Must not be NULL
     * @param annotationNames A list of required annotations
     * @param allRequired Require all(TRUE)/any(FALSE) provided annotations to match
     * @return Returns TRUE if the source contains any or all of the provided annotations
     */
    boolean hasAnnotation(FileObject fo, String[] annotationNames, boolean allRequired);

    /**
     * Checks whether the source contains the provided annotation
     * @param fo The source file. Must not be NULL
     * @param annotation The required annotation
     * @return Returns TRUE if the source contains the provided annotation
     */
    boolean hasAnnotation(FileObject fo, String annotation);

    /**
     * Is the given offset valid within a particular source
     * @param fo The source file. Must not be NULL
     * @param offset The offset to check
     * @return Returns TRUE if the offset is valid for the source
     */
    boolean isOffsetValid(FileObject fo, int offset);
    
    /**
     * Resolves a method at the given position<br>
     * In order to resolve the method there must be the method definition or invocation
     * at the given position.
     * @param fo The source file. Must not be NULL
     * @param position The position to check for method definition or invocation
     * @return Returns the {@linkplain SourceMethodInfo} for the method definition or invocation at the given position or NULL if there is none
     */
    SourceMethodInfo resolveMethodAtPosition(FileObject fo, int position);
    
    /**
     * Resolves a class at the given position<br>
     * In order to resolve the class there must be the class definition or reference
     * at the given position.
     * @param fo The source file. Must not be NULL
     * @param position The position to check for class definition or reference
     * @param resolveField Should the class be resolved from a variable type too?
     * @return Returns the {@linkplain SourceClassInfo} for the class definition or reference at the given position or NULL if there is none
     */
    SourceClassInfo resolveClassAtPosition(FileObject fo, int position, boolean resolveField);
}
