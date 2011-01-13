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
package org.jboss.osgi.vfs30;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.osgi.vfs.VFSAdaptor;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.vfs.VFS;

/**
 * An adaptor to the jboss-vfs-3.0.x VFS.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
public final class VFSAdaptor30 implements VFSAdaptor {

    private static Map<org.jboss.vfs.VirtualFile, VirtualFile> registry = new WeakHashMap<org.jboss.vfs.VirtualFile, VirtualFile>();

    @Override
    public VirtualFile toVirtualFile(URL url) throws IOException {
        try {
            return toVirtualFile(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public VirtualFile toVirtualFile(URI uri) throws IOException {
        org.jboss.vfs.VirtualFile vfsFile = VFS.getChild(uri);
        return (VirtualFileAdaptor30) adapt(vfsFile);
    }

    @Override
    public VirtualFile toVirtualFile(String name, InputStream inputStream) throws IOException {
        return new VirtualFileAdaptor30(name, inputStream);
    }

    @Override
    public VirtualFile toVirtualFile(InputStream inputStream) throws IOException {
        String name = "stream" + System.currentTimeMillis();
        return new VirtualFileAdaptor30(name, inputStream);
    }

    @Override
    public VirtualFile adapt(Object other) throws IOException {
        if (other == null)
            return null;

        if (other instanceof org.jboss.vfs.VirtualFile == false)
            throw new IllegalArgumentException("Not a org.jboss.vfs.VirtualFile: " + other);

        org.jboss.vfs.VirtualFile vfsFile = (org.jboss.vfs.VirtualFile) other;
        VirtualFile absFile = registry.get(other);
        if (absFile != null)
            return absFile;

        // Register the VirtualFile abstraction
        absFile = new VirtualFileAdaptor30(vfsFile);
        registry.put(vfsFile, absFile);
        return absFile;
    }

    @Override
    public Object adapt(VirtualFile absFile) {
        if (absFile == null)
            return null;

        VirtualFileAdaptor30 adaptor = (VirtualFileAdaptor30) absFile;
        return adaptor.getVirtualFile();
    }

    static void unregister(VirtualFileAdaptor30 absFile) {
        registry.remove(absFile.getVirtualFile());
    }
}