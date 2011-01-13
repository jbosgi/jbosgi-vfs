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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;

/**
 * A basic abstraction of a VirtualFile used by the OSGi layer.
 * 
 * This abstraction should be removed once we settle on a single jboss-vfs version.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
public interface VirtualFile extends Closeable {

    /**
     * System property to enable detection of unclosed virtual files 
     */
    final String PROPERTY_VFS_LEAK_DEBUGGING = "jboss.osgi.vfs.leakDebugging";

    /**
     * Get the simple VF name (X.java)
     * 
     * @return the simple file name
     * @throws IllegalStateException if the file is closed
     */
    String getName();

    /**
     * Get the VFS relative path name (org/jboss/X.java)
     * 
     * @return the VFS relative path name
     * @throws IllegalStateException if the file is closed
     */
    String getPathName();

    /**
     * Whether it is a file in the VFS.
     * 
     * @return true if a simple file.
     * @throws IOException for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed
     */
    boolean isFile() throws IOException;

    /**
     * Whether it is a directory in the VFS.
     * 
     * @return true if a directory.
     * @throws IOException for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed
     */
    boolean isDirectory() throws IOException;

    /**
     * Get the VF URL (vfs:/root/org/jboss/X.java)
     * 
     * @return the full URL to the VF in the VFS.
     * @throws MalformedURLException if a url cannot be parsed
     * @throws IOException for any problem accessing the virtual file system
     */
    URL toURL() throws IOException;

    /**
     * Get the URL used for streaming.
     * 
     * @return The stream URL or null if this is not the mounted root file.
     */
    URL getStreamURL() throws IOException;

    /**
     * Get the parent
     * 
     * @return the parent or null if there is no parent
     * @throws IOException for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed
     */
    VirtualFile getParent() throws IOException;

    /**
     * Get a child
     * 
     * @param path the path
     * @return the child or <code>null</code> if not found
     * @throws IOException for any problem accessing the VFS
     * @throws IllegalArgumentException if the path is null
     * @throws IllegalStateException if the file is closed or it is a leaf node
     */
    VirtualFile getChild(String path) throws IOException;

    /**
     * Get all children recursively
     * <p>
     * 
     * This always uses {@link VisitorAttributes#RECURSE}
     * 
     * @return the children
     * @throws IOException for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed
     */
    List<VirtualFile> getChildrenRecursively() throws IOException;

    /**
     * Get the children
     * <p>
     * 
     * This always uses {@link VisitorAttributes#RECURSE}
     * 
     * @return the children
     * @throws IOException for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed
     */
    List<VirtualFile> getChildren() throws IOException;

    /**
     * Returns entries in this bundle and its attached fragments. This bundle's class loader is not used to search for entries.
     * Only the contents of this bundle and its attached fragments are searched for the specified entries.
     * 
     * @see Bundle.findEntries(String path, String pattern, boolean recurse)
     */
    Enumeration<URL> findEntries(String path, String pattern, boolean recurse) throws IOException;

    /**
     * Returns an Enumeration of all the paths (<code>String</code> objects) to entries within this bundle whose longest
     * sub-path matches the specified path. This bundle's class loader is not used to search for entries. Only the contents of
     * this bundle are searched.
     * 
     * @see Bundle.getEntryPaths(String path)
     */
    Enumeration<String> getEntryPaths(String path) throws IOException;

    /**
     * Access the file contents.
     * 
     * @return an InputStream for the file contents.
     * @throws IOException for any error accessing the file system
     * @throws IllegalStateException if the file is closed
     */
    InputStream openStream() throws IOException;

    /**
     * Get the {@link Certificate}s for the virtual file. Simply extracts the certificate entries from the code signers array.
     * 
     * @return the certificates for the virtual file, or {@code null} if not signed
     */
    Certificate[] getCertificates();

    /**
     * Get the {@link CodeSigner}s for a the virtual file.
     * 
     * @return the {@link CodeSigner}s for the virtual file, or {@code null} if not signed
     */
    CodeSigner[] getCodeSigners();
}
