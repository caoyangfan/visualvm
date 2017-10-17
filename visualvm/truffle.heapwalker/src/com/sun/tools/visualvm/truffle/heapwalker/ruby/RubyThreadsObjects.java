/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.truffle.heapwalker.ruby;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalker.v2.java.LocalObjectNode;
import org.netbeans.modules.profiler.heapwalker.v2.java.StackFrameNode;
import org.netbeans.modules.profiler.heapwalker.v2.java.ThreadNode;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleStackTraces;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.RootNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.TextNode;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyThreadsObjects {
    
    private static final String RUBY_THREAD_NAME_PREFIX = "Ruby Thread";
    
    
    static HeapWalkerNode[] getThreads(RootNode rootNode, Heap heap) {
        List<HeapWalkerNode> threadNodes = new ArrayList();
        
//        JavaClass javaClassClass = heap.getJavaClassByName(Class.class.getName());
        
        TruffleStackTraces tst = new TruffleStackTraces(heap);
        Collection<TruffleStackTraces.StackTrace> threads = tst.getStackTraces();
        
        if (threads != null) {
            for (TruffleStackTraces.StackTrace st : threads) {
                Instance threadInstance = st.getThread();
                String threadName = DetailsSupport.getDetailsString(threadInstance, heap);
                if (threadName == null || !threadName.startsWith(RUBY_THREAD_NAME_PREFIX)) continue;
                
                final List<HeapWalkerNode> stackFrameNodes = new ArrayList();
                ThreadNode threadNode = new ThreadNode(threadName, threadInstance) {
                    protected HeapWalkerNode[] computeChildren(RootNode root) {
                        return stackFrameNodes.toArray(HeapWalkerNode.NO_NODES);
                    }
                };
                threadNodes.add(threadNode);

                for (TruffleStackTraces.Frame f : st.getFrames()) {
                    Set<HeapWalkerNode> localObjects = new HashSet();
                    for (FieldValue fv :  f.getFieldValues()) {
    //                    String val;
    //                    if (fv instanceof ObjectFieldValue) {
    //                        Instance i = ((ObjectFieldValue)fv).getInstance();
    //                        val = i.getJavaClass().getName()+"#"+i.getInstanceNumber();
    //                    } else {
    //                        val = fv.getValue();
    //                    }
                        if (!(fv instanceof ObjectFieldValue)) continue;

                        Instance instance = ((ObjectFieldValue)fv).getInstance();
                        if (instance == null) continue;
                        if (!DynamicObject.isDynamicObject(instance)) {
                            localObjects.add(new LocalObjectNode(instance));
                        } else {
                            DynamicObject dobject = new DynamicObject(instance);
                            localObjects.add(new RubyNodes.RubyLocalDynamicObjectNode(dobject, dobject.getType(heap)));
                        }

                    }
    //                List<FieldValue> fields = f.getFieldValues();
                    stackFrameNodes.add(new StackFrameNode(f.getName(), localObjects.toArray(HeapWalkerNode.NO_NODES)));
    //                sb.append("    at "+f.getName()+"()");
    //                sb.append("<br>");  // NOI18N
    //
    ////                if (!fields.isEmpty()) sb.append("        Locals:");
    ////                sb.append("<br>");  // NOI18N
    //

                }
            }
        } else {
            threadNodes.add(new TextNode("Thread not available"));
        }
        
        return threadNodes.toArray(HeapWalkerNode.NO_NODES);
    }
    
}