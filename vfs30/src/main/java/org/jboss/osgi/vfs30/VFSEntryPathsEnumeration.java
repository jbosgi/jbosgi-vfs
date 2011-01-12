/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.jboss.vfs.VirtualFile;

/**
 * An enumeration of VFS entry paths.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
class VFSEntryPathsEnumeration implements Enumeration<String> {

    /** The paths */
    private Iterator<String> paths;

    /**
     * Create a new VFSEntryPathsEnumeration.
     * 
     * @param root the root file
     * @param file the file to enumerate
     * @throws IOException for any error
     */
    public VFSEntryPathsEnumeration(VirtualFile root, VirtualFile file) throws IOException {
        if (root == null)
            throw new IllegalArgumentException("Null root");
        if (file == null)
            throw new IllegalArgumentException("Null file");

        String rootPath = root.getPathName();
        ArrayList<String> paths = new ArrayList<String>();

        String fixedPath = fixPath(rootPath, file);
        if (fixedPath != null)
            paths.add(fixedPath);

        List<VirtualFile> children = file.getChildrenRecursively();
        for (VirtualFile child : children) {
            fixedPath = fixPath(rootPath, child);
            if (fixedPath != null)
                paths.add(fixedPath);
        }

        this.paths = paths.iterator();
    }

    public boolean hasMoreElements() {
        return paths.hasNext();
    }

    public String nextElement() {
        return paths.next();
    }

    private String fixPath(String rootPath, VirtualFile file) {
        String result = file.getPathName();

        int length = rootPath.length();
        if (length != 0)
            result = result.substring(length);

        // Returned paths indicating subdirectory paths end with a "/"
        if (file.isDirectory() && result.endsWith("/") == false)
            result += "/";

        // The returned paths are all relative to the root of this bundle and must not begin with "/".
        if (result.startsWith("/"))
            result = result.substring(1);

        if (result.isEmpty())
            return null;

        return result;
    }
}
