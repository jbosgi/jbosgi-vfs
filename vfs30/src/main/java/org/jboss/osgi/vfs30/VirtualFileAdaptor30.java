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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualJarInputStream;

/**
 * An adaptor to the jboss-vfs-3.0.x VirtualFile.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
class VirtualFileAdaptor30 implements VirtualFile {

    private final org.jboss.vfs.VirtualFile vfsFile;
    private IOException leakDebuggingStack;
    private Closeable mount;
    private TempDir streamDir;
    private File streamFile;

    private static Set<String> suffixes = new HashSet<String>();
    static {
        suffixes.add(".jar");
        suffixes.add(".war");
    }

    private static boolean LEAK_DEBUGGING;
    static {
        if (System.getSecurityManager() != null) {
            LEAK_DEBUGGING = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return "true".equals(System.getProperty(PROPERTY_VFS_LEAK_DEBUGGING));
                }
            });
        } else {
            LEAK_DEBUGGING = "true".equals(System.getProperty(PROPERTY_VFS_LEAK_DEBUGGING));
        }
    }

    private static final TempFileProvider tmpProvider;
    static {
        try {
            tmpProvider = TempFileProvider.create("osgitmp-", null);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create VFS temp file provider", ex);
        }

        Thread shutdownThread = new Thread("vfs-shutdown") {
            public void run() {
                try {
                    tmpProvider.close();
                } catch (IOException ex) {
                    throw new IllegalStateException("Cannot close VFS temp file provider", ex);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    VirtualFileAdaptor30(String name, InputStream input) throws IOException {
        this(VFS.getChild(name));
        if (input == null)
            throw new IllegalStateException("Null input");
        mount = mountVirtualFile(input);
    }

    VirtualFileAdaptor30(org.jboss.vfs.VirtualFile vfsFile) {
        if (vfsFile == null)
            throw new IllegalStateException("Null vfsFile");
        this.vfsFile = vfsFile;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mount != null && leakDebuggingStack != null) {
            leakDebuggingStack.printStackTrace(System.err);
        }
    }

    public org.jboss.vfs.VirtualFile getVirtualFile() {
        return vfsFile;
    }

    public String getName() {
        return vfsFile.getName();
    }

    public String getPathName() {
        return vfsFile.getPathName();
    }

    public boolean isFile() throws IOException {
        return vfsFile.isFile();
    }

    public boolean isDirectory() throws IOException {
        return vfsFile.isDirectory();
    }

    @Override
    public URL toURL() throws IOException {
        URL url = vfsFile.toURL();
        return url;
    }

    @Override
    public URL getStreamURL() throws IOException {
        if (vfsFile.isFile() == true)
            return vfsFile.toURL();

        if (streamFile == null) {
            streamDir = tmpProvider.createTempDir("urlstream");
            streamFile = streamDir.getFile(getName());

            JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(streamFile));
            VirtualJarInputStream jarIn = (VirtualJarInputStream) vfsFile.openStream();
            ZipEntry nextEntry = jarIn.getNextEntry();
            while (nextEntry != null) {
                jarOut.putNextEntry(nextEntry);
                VFSUtils.copyStream(jarIn, jarOut);
                nextEntry = jarIn.getNextEntry();
            }
            jarOut.close();
            jarIn.close();
        }
        return streamFile.toURI().toURL();
    }

    @Override
    public VirtualFile getParent() {
        org.jboss.vfs.VirtualFile parent = vfsFile.getParent();
        return parent != null ? new VirtualFileAdaptor30(parent) : null;
    }

    @Override
    public VirtualFile getChild(String path) throws IOException {
        org.jboss.vfs.VirtualFile child = getMountedChild(path);
        if (child.exists() == false)
            return null;

        return new VirtualFileAdaptor30(child);
    }

    @Override
    public List<VirtualFile> getChildrenRecursively() throws IOException {
        List<VirtualFile> files = new ArrayList<VirtualFile>();
        for (org.jboss.vfs.VirtualFile child : getMountedChildrenRecursively())
            files.add(new VirtualFileAdaptor30(child));
        return Collections.unmodifiableList(files);
    }

    @Override
    public List<VirtualFile> getChildren() throws IOException {
        List<VirtualFile> files = new ArrayList<VirtualFile>();
        for (org.jboss.vfs.VirtualFile child : getMountedChildren())
            files.add(new VirtualFileAdaptor30(child));
        return Collections.unmodifiableList(files);
    }

    @Override
    public Enumeration<URL> findEntries(String path, String pattern, boolean recurse) throws IOException {
        if (path == null)
            throw new IllegalArgumentException("Null path");

        if (pattern == null)
            pattern = "*";

        if (path.startsWith("/"))
            path = path.substring(1);

        org.jboss.vfs.VirtualFile child = getMountedChild(path);
        if (child.exists() == false)
            return null;

        return new VFSFindEntriesEnumeration(vfsFile, child, pattern, recurse);
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) throws IOException {
        if (path == null)
            throw new IllegalArgumentException("Null path");

        if (path.startsWith("/"))
            path = path.substring(1);

        org.jboss.vfs.VirtualFile child;
        if (path.length() > 0) {
            child = getMountedChild(path);
        } else {
            ensureMounted();
            child = vfsFile;
        }

        if (child.exists() == false)
            return null;

        return new VFSEntryPathsEnumeration(vfsFile, child);
    }

    @Override
    public InputStream openStream() throws IOException {
        if (mount != null)
            return getStreamURL().openStream();

        return vfsFile.openStream();
    }

    @Override
    public Certificate[] getCertificates() {
        return vfsFile.getCertificates();
    }

    @Override
    public CodeSigner[] getCodeSigners() {
        return vfsFile.getCodeSigners();
    }

    @Override
    public void close() {
        VFSUtils.safeClose(mount);
        leakDebuggingStack = null;
        mount = null;
        VFSAdaptor30.unregister(this);
        if (streamFile != null) {
            File streamParent = streamFile.getParentFile();
            streamFile.delete();
            streamParent.delete();
            streamFile = null;
        }
    }

    private boolean acceptForMount() {
        if (vfsFile.isDirectory())
            return false;

        boolean accept = false;
        String rootName = vfsFile.getName();
        for (String suffix : suffixes) {
            if (rootName.endsWith(suffix)) {
                accept = true;
                break;
            }
        }
        return accept;
    }

    private void ensureMounted() throws IOException {
        if (mount == null && acceptForMount()) {
            mount = mountVirtualFile(null);
        }
    }

    public Closeable mountVirtualFile(InputStream input) throws IOException {
        Closeable mount;
        if (input != null) {
            mount = VFS.mountZip(input, vfsFile.getName(), vfsFile, tmpProvider);
        } else {
            mount = VFS.mountZip(vfsFile, vfsFile, tmpProvider);
        }
        if (LEAK_DEBUGGING == true)
            this.leakDebuggingStack = new IOException("VirtualFile created in this stack frame not closed");
        return mount;
    }

    private org.jboss.vfs.VirtualFile getMountedChild(String path) throws IOException {
        ensureMounted();
        return vfsFile.getChild(path);
    }

    private List<org.jboss.vfs.VirtualFile> getMountedChildren() throws IOException {
        ensureMounted();
        return vfsFile.getChildren();
    }

    private List<org.jboss.vfs.VirtualFile> getMountedChildrenRecursively() throws IOException {
        ensureMounted();
        return vfsFile.getChildrenRecursively();
    }

    @Override
    public boolean equals(Object obj) {
        return vfsFile.equals(obj);
    }

    @Override
    public int hashCode() {
        return vfsFile.hashCode();
    }

    @Override
    public String toString() {
        return vfsFile.toString();
    }
}
