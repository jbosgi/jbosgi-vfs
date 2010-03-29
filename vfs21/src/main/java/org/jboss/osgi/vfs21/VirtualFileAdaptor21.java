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
package org.jboss.osgi.vfs21;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.virtual.VFSUtils;

/**
 * An adaptor to the jboss-vfs-2.1.x VirtualFile. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
class VirtualFileAdaptor21 implements VirtualFile
{
   private org.jboss.virtual.VirtualFile root;
   private org.jboss.virtual.VirtualFile delegate;

   VirtualFileAdaptor21(org.jboss.virtual.VirtualFile root, org.jboss.virtual.VirtualFile delegate)
   {
      if (root == null)
         throw new IllegalStateException("Null root");
      if (delegate == null)
         throw new IllegalStateException("Null delegate");

      this.delegate = delegate;
      this.root = root;
   }

   org.jboss.virtual.VirtualFile getDelegate()
   {
      return delegate;
   }

   @Override
   public String getName()
   {
      return delegate.getName();
   }

   @Override
   public String getPathName()
   {
      String pathName = "";
      try
      {
         pathName = VFSUtils.getRealURL(root).getPath();
      }
      catch (Exception ex)
      {
         // ignore
      }
      pathName += delegate.getPathName();
      return pathName;
   }

   @Override
   public boolean isFile() throws IOException
   {
      return delegate.isLeaf();
   }

   @Override
   public boolean isDirectory() throws IOException
   {
      return delegate.isLeaf() == false;
   }

   @Override
   public URL toURL() throws IOException
   {
      try
      {
         return delegate.toURL();
      }
      catch (URISyntaxException ex)
      {
         throw new IOException(ex);
      }
   }

   @Override
   public URL getStreamURL() throws IOException
   {
      if (root != delegate)
         return null;

      try
      {
         URI uri = VFSUtils.getCompatibleURI(root);
         String path = VFSUtils.stripProtocol(uri);
         return new File(path).toURI().toURL();
      }
      catch (Exception ex)
      {
         throw new IOException(ex);
      }
   }

   @Override
   public void close()
   {
      delegate.close();
   }

   @Override
   public VirtualFile getParent() throws IOException
   {
      org.jboss.virtual.VirtualFile parent = delegate.getParent();
      return parent != null ? new VirtualFileAdaptor21(root, parent) : null;
   }

   @Override
   public VirtualFile getChild(String path) throws IOException
   {
      org.jboss.virtual.VirtualFile child = delegate.getChild(path);
      if (child == null)
         return null;

      return new VirtualFileAdaptor21(root, child);
   }

   @Override
   public List<VirtualFile> getChildrenRecursively() throws IOException
   {
      List<VirtualFile> files = new ArrayList<VirtualFile>();
      for (org.jboss.virtual.VirtualFile child : delegate.getChildrenRecursively())
         files.add(new VirtualFileAdaptor21(root, child));

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

      org.jboss.virtual.VirtualFile child = delegate.getChild(path);
      if (child == null)
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

      org.jboss.virtual.VirtualFile child = delegate.getChild(path);
      if (child == null)
         return null;

      return new VFSEntryPathsEnumeration(delegate, child);
   }

   @Override
   public InputStream openStream() throws IOException
   {
      return delegate.openStream();
   }

   @Override
   public int hashCode()
   {
      return delegate.hashCode();
   }

   @Override
   public boolean equals(Object obj)
   {
      return delegate.equals(obj);
   }

   @Override
   public String toString()
   {
      return delegate.toString();
   }
}
