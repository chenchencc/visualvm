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

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceReferenceNode;
import org.netbeans.modules.profiler.heapwalker.v2.java.PrimitiveNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.SortedNodesBuffer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapWalkerNode.Provider.class, position = 210)
public class RubyFieldsProvider extends HeapWalkerNode.Provider {
    
    // TODO: will be configurable, ideally by instance
    private boolean includeStaticFields = true;
    private boolean includeInstanceFields = true;
    
    
    public String getName() {
        return "variables";
    }

    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("ruby_");
    }

    public boolean supportsNode(HeapWalkerNode parent, Heap heap, String viewID) {
        return parent instanceof DynamicObjectNode && !(parent instanceof DynamicObjectReferenceNode);
    }
    
    public HeapWalkerNode[] getNodes(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        return getNodes(getFields(parent, heap), parent, heap, viewID, dataTypes, sortOrders);
    }
    
    static HeapWalkerNode[] getNodes(List<FieldValue> fields, HeapWalkerNode parent, Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        if (fields == null) return null;
        
        DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
        SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
        SortedNodesBuffer nodes = new SortedNodesBuffer(1000, dataType, sortOrder, heap, parent) {
            protected String getMoreItemsString(String formattedNodesLeft) {
                return "<another " + formattedNodesLeft + " properties left>";
            }
        };
        
        for (FieldValue field : fields) {
            if (field instanceof ObjectFieldValue) {
                Instance instance = ((ObjectFieldValue)field).getInstance();
                if (DynamicObject.isDynamicObject(instance)) {
                    nodes.add(new RubyNodes.RubyDynamicObjectFieldNode(new DynamicObject(instance), field, heap));
                } else {
                    // TODO: include the actual values (strings, arrays etc.)
                    if (instance == null) {
                        nodes.add(new InstanceReferenceNode.Field((ObjectFieldValue)field, false));
                    } else if (instance instanceof PrimitiveArrayInstance) {
                        nodes.add(new InstanceReferenceNode.Field((ObjectFieldValue)field, false));
                    } else {
                        String name = instance.getJavaClass().getName();
                        if (name.startsWith("java.lang.")) {
                            nodes.add(new InstanceReferenceNode.Field((ObjectFieldValue)field, false));
                        }
                    }
                }
            } else {
                nodes.add(new PrimitiveNode.Field(field));
            }
        }
        return nodes.getNodes();
    }
    
    
    protected List<FieldValue> getFields(HeapWalkerNode parent, Heap heap) {
        DynamicObject dobject = parent == null ? null : HeapWalkerNode.getValue(parent, DynamicObject.DATA_TYPE, heap);
        if (dobject == null) return null;

        if (includeStaticFields == includeInstanceFields) {
            List<FieldValue> fields = new ArrayList(dobject.getFieldValues());
            fields.addAll(dobject.getStaticFieldValues());
            return fields;
        } else if (includeInstanceFields) {
            return dobject.getFieldValues();
        } else {
            return dobject.getStaticFieldValues();
        }
    }
    
}