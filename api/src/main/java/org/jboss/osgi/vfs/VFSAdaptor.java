/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * The basic adaptor for the VFS that needs to be implemented for a specific jboss-vfs version.
 * 
 * This abstraction should be removed once we settle on a single jboss-vfs version.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
public interface VFSAdaptor {

    /**
     * Get the root virtual file
     * 
     * @param url the root url
     * @return the virtual file
     * @throws IOException if there is a problem accessing the VFS
     */
    VirtualFile toVirtualFile(URL url) throws IOException;

    /**
     * Get the root virtual file
     * 
     * @param uri the root uri
     * @return the virtual file
     * @throws IOException if there is a problem accessing the VFS
     */
    VirtualFile toVirtualFile(URI uri) throws IOException;

    /**
     * Adapt an named InputStream to a virtual file.
     * 
     * @param name The name of the virtual file
     * @param input The input stream
     * @return The VirtualFile abstraction
     * @throws IOException if there is a problem accessing the VFS
     */
    VirtualFile toVirtualFile(String name, InputStream input) throws IOException;

    /**
     * Adapt an InputStream to a virtual file.
     * 
     * @param input The input stream
     * @return The VirtualFile abstraction
     * @throws IOException if there is a problem accessing the VFS
     */
    VirtualFile toVirtualFile(InputStream input) throws IOException;
    
    /**
     * Adapt a concrete instance of a jboss-vfs VirtualFile.
     * 
     * @param virtualFile The VirtualFile instance
     * @return The VirtualFile abstraction
     * @throws IllegalArgumentException If the given virtualFile is not a VirtualFile supported by the VFSAdaptor implementation
     */
    VirtualFile adapt(Object virtualFile) throws IOException;

    /**
     * Adapt a VirtualFile to a concrete instance of a jboss-vfs VirtualFile.
     * 
     * @param virtualFile The VirtualFile instance
     * @return The jboss-vfs VirtualFile
     */
    Object adapt(VirtualFile virtualFile) throws IOException;
}
