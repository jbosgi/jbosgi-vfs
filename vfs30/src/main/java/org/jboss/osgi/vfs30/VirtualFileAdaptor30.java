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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualJarInputStream;

/**
 * An adaptor to the jboss-vfs-3.0.x VirtualFile.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
class VirtualFileAdaptor30 implements VirtualFile
{
   private final org.jboss.vfs.VirtualFile vfsFile;
   private Closeable mount;
   private TempDir streamDir;
   private File streamFile;

   VirtualFileAdaptor30(org.jboss.vfs.VirtualFile vfsFile, Closeable mount)
   {
      this(vfsFile);
      this.mount = mount;
   }

   VirtualFileAdaptor30(org.jboss.vfs.VirtualFile vfsFile)
   {
      if (vfsFile == null)
         throw new IllegalStateException("Null vfsFile");
      this.vfsFile = vfsFile;
   }

   public org.jboss.vfs.VirtualFile getVirtualFile()
   {
      return vfsFile;
   }

   public String getName()
   {
      return vfsFile.getName();
   }

   public String getPathName()
   {
      return vfsFile.getPathName();
   }

   public boolean isFile() throws IOException
   {
      return vfsFile.isFile();
   }

   public boolean isDirectory() throws IOException
   {
      return vfsFile.isDirectory();
   }

   @Override
   public URL toURL() throws IOException
   {
      URL url = vfsFile.toURL();
      return url;
   }

   @Override
   public URL getStreamURL() throws IOException
   {
      if (vfsFile.isFile() == true)
         return vfsFile.toURL();

      if (streamFile == null)
      {
         streamDir = VFSAdaptor30.getTempFileProvider().createTempDir("urlstream");
         streamFile = streamDir.getFile(getName());
         
         JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(streamFile));
         VirtualJarInputStream jarIn = (VirtualJarInputStream)vfsFile.openStream();
         ZipEntry nextEntry = jarIn.getNextEntry();
         while (nextEntry != null)
         {
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
   public VirtualFile getParent()
   {
      org.jboss.vfs.VirtualFile parent = vfsFile.getParent();
      return parent != null ? new VirtualFileAdaptor30(parent) : null;
   }

   @Override
   public VirtualFile getChild(String path) throws IOException
   {
      org.jboss.vfs.VirtualFile child = vfsFile.getChild(path);
      if (child.exists() == false)
         return null;

      return new VirtualFileAdaptor30(child);
   }

   @Override
   public List<VirtualFile> getChildrenRecursively() throws IOException
   {
      List<VirtualFile> files = new ArrayList<VirtualFile>();
      for (org.jboss.vfs.VirtualFile child : vfsFile.getChildrenRecursively())
         files.add(new VirtualFileAdaptor30(child));
      return Collections.unmodifiableList(files);
   }

   @Override
   public List<VirtualFile> getChildren() throws IOException
   {
      List<VirtualFile> files = new ArrayList<VirtualFile>();
      for (org.jboss.vfs.VirtualFile child : vfsFile.getChildren())
         files.add(new VirtualFileAdaptor30(child));
      return Collections.unmodifiableList(files);
   }

   @Override
   public Enumeration<URL> findEntries(String path, String pattern, boolean recurse) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if (pattern == null)
         pattern = "*";

      if (path.startsWith("/"))
         path = path.substring(1);

      org.jboss.vfs.VirtualFile child = vfsFile.getChild(path);
      if (child.exists() == false)
         return null;

      return new VFSFindEntriesEnumeration(vfsFile, child, pattern, recurse);
   }

   @Override
   public Enumeration<String> getEntryPaths(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if (path.startsWith("/"))
         path = path.substring(1);

      org.jboss.vfs.VirtualFile child;
      if (path.length() > 0)
         child = vfsFile.getChild(path);
      else
         child = vfsFile;

      if (child.exists() == false)
         return null;

      return new VFSEntryPathsEnumeration(vfsFile, child);
   }

   @Override
   public InputStream openStream() throws IOException
   {
      if (mount != null)
         return getStreamURL().openStream();

      return vfsFile.openStream();
   }

   @Override
   public void recursiveCopy(File target) throws IOException
   {
      VFSUtils.recursiveCopy(vfsFile, target);
   }
   
   @Override
   public void close()
   {
      VFSUtils.safeClose(mount);
      VFSAdaptor30.unregister(this);
      if (streamFile != null)
      {
         File streamParent = streamFile.getParentFile();
         streamFile.delete();
         streamParent.delete();
         streamFile = null;
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      return vfsFile.equals(obj);
   }

   @Override
   public int hashCode()
   {
      return vfsFile.hashCode();
   }

   @Override
   public String toString()
   {
      return vfsFile.toString();
   }
}
