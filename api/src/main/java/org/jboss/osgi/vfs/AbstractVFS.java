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
 * The AbstractVFS is the entry point for VFS abstraction used by the OSGi layer.
 * 
 * This abstraction should be removed once we settle on a single jboss-vfs version.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
public abstract class AbstractVFS {

    private static VFSAdaptor adaptor;

    public static VirtualFile toVirtualFile(URI uri) throws IOException {
        return getVFSAdaptor().toVirtualFile(uri);
    }

    public static VirtualFile toVirtualFile(URL url) throws IOException {
        return getVFSAdaptor().toVirtualFile(url);
    }

    public static VirtualFile toVirtualFile(String name, InputStream inputStream) throws IOException {
        return getVFSAdaptor().toVirtualFile(name, inputStream);
    }

    public static VirtualFile adapt(Object virtualFile) throws IOException {
        return getVFSAdaptor().adapt(virtualFile);
    }

    public static Object adapt(VirtualFile virtualFile) throws IOException {
        return getVFSAdaptor().adapt(virtualFile);
    }

    @SuppressWarnings("unchecked")
    private static VFSAdaptor getVFSAdaptor() {
        if (adaptor == null) {
            Class<VFSAdaptor> adaptorClass = null;

            // Try to load the jboss-vfs-3.0.x adaptor
            try {
                String adaptorName = "org.jboss.osgi.vfs30.VFSAdaptor30";
                adaptorClass = (Class<VFSAdaptor>) AbstractVFS.class.getClassLoader().loadClass(adaptorName);
            } catch (ClassNotFoundException e) {
                // ignore
            }

            if (adaptorClass == null)
                throw new IllegalStateException("Cannot load VFS adaptor");

            try {
                adaptor = adaptorClass.newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot instanciate VFS adaptor");
            }
        }
        return adaptor;
    }
}
