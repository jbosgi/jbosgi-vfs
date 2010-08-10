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
import org.jboss.vfs.TempFileProvider;
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
   private org.jboss.vfs.VirtualFile delegate;
   private Closeable mount;
   private TempDir streamDir;
   private File streamFile;

   VirtualFileAdaptor30(org.jboss.vfs.VirtualFile root, Closeable mount)
   {
      this(root);
      this.mount = mount;
   }

   VirtualFileAdaptor30(org.jboss.vfs.VirtualFile delegate)
   {
      if (delegate == null)
         throw new IllegalStateException("Null delegate");
      this.delegate = delegate;
   }

   org.jboss.vfs.VirtualFile getDelegate()
   {
      return delegate;
   }

   public String getName()
   {
      return delegate.getName();
   }

   public String getPathName()
   {
      return delegate.getPathName();
   }

   public boolean isFile() throws IOException
   {
      return delegate.isFile();
   }

   public boolean isDirectory() throws IOException
   {
      return delegate.isDirectory();
   }

   @Override
   public URL toURL() throws IOException
   {
      URL url = delegate.toURL();
      return url;
   }

   @Override
   public URL getStreamURL() throws IOException
   {
      if (delegate.isFile() == true)
         return delegate.toURL();

      if (streamFile == null)
      {
         TempFileProvider tmpProvider = TempFileProvider.create("osgiurl-", null);
         streamDir = tmpProvider.createTempDir(getName());
         streamFile = streamDir.getFile(getName());
         JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(streamFile));

         VirtualJarInputStream jarIn = (VirtualJarInputStream)delegate.openStream();
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
      org.jboss.vfs.VirtualFile parent = delegate.getParent();
      return parent != null ? new VirtualFileAdaptor30(parent) : null;
   }

   @Override
   public VirtualFile getChild(String path) throws IOException
   {
      org.jboss.vfs.VirtualFile child = delegate.getChild(path);
      if (child.exists() == false)
         return null;

      return new VirtualFileAdaptor30(child);
   }

   @Override
   public List<VirtualFile> getChildrenRecursively() throws IOException
   {
      List<VirtualFile> files = new ArrayList<VirtualFile>();
      for (org.jboss.vfs.VirtualFile child : delegate.getChildrenRecursively())
         files.add(new VirtualFileAdaptor30(child));
      return Collections.unmodifiableList(files);
   }

   @Override
   public List<VirtualFile> getChildren() throws IOException
   {
      List<VirtualFile> files = new ArrayList<VirtualFile>();
      for (org.jboss.vfs.VirtualFile child : delegate.getChildren())
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

      org.jboss.vfs.VirtualFile child = delegate.getChild(path);
      if (child.exists() == false)
         return null;

      return new VFSFindEntriesEnumeration(delegate, child, pattern, recurse);
   }

   @Override
   public Enumeration<String> getEntryPaths(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if (path.startsWith("/"))
         path = path.substring(1);

      org.jboss.vfs.VirtualFile child = delegate.getChild(path);
      if (child.exists() == false)
         return null;

      return new VFSEntryPathsEnumeration(delegate, child);
   }

   @Override
   public InputStream openStream() throws IOException
   {
      if (mount != null)
         return getStreamURL().openStream();

      return delegate.openStream();
   }

   @Override
   public void close()
   {
      VFSUtils.safeClose(mount);
      VFSAdaptor30.unregister(this);
      if (streamFile != null)
      {
         streamFile.delete();
         streamFile = null;
      }
      try
      {
         if (streamDir != null)
         {
            streamDir.close();
            streamDir = null;
         }
      }
      catch (IOException ex)
      {
         // ignore
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      return delegate.equals(obj);
   }

   @Override
   public int hashCode()
   {
      return delegate.hashCode();
   }

   @Override
   public String toString()
   {
      return delegate.toString();
   }
}
