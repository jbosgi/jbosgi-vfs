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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.osgi.vfs.VFSAdaptor;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;

/**
 * An adaptor to the jboss-vfs-3.0.x VFS. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
public class VFSAdaptor30 implements VFSAdaptor
{
   private static Map<Object, VirtualFile> registry = new ConcurrentHashMap<Object, VirtualFile>();
   private static Set<String> suffixes = new HashSet<String>();
   static
   {
      suffixes.add(".jar");
      suffixes.add(".war");
   }

   public VirtualFile getRoot(URL url) throws IOException
   {
      try
      {
         org.jboss.vfs.VirtualFile vfsFile = org.jboss.vfs.VFS.getChild(url);
         VirtualFileAdaptor30 absFile = (VirtualFileAdaptor30)adapt(vfsFile);
         return absFile;
      }
      catch (URISyntaxException ex)
      {
         throw new IOException(ex);
      }
   }

   public VirtualFile adapt(Object other)
   {
      if (other == null)
         return null;

      if (other instanceof org.jboss.vfs.VirtualFile == false)
         throw new IllegalArgumentException("Not a org.jboss.vfs.VirtualFile: " + other);

      org.jboss.vfs.VirtualFile vfsFile = (org.jboss.vfs.VirtualFile)other;
      VirtualFile absFile = registry.get(other);
      if (absFile != null)
         return absFile;

      // Accept the file for mounting
      Closeable mount = null;
      if (acceptForMount(vfsFile) == true)
      {
         try
         {
            TempFileProvider tmp = TempFileProvider.create("osgimount-", null);
            mount = VFS.mountZip(vfsFile, vfsFile, tmp);
         }
         catch (IOException ex)
         {
            throw new IllegalStateException("Cannot mount native file: " + other, ex);
         }
      }

      // Register the VirtualFile abstraction
      absFile = new VirtualFileAdaptor30(vfsFile, mount);
      registry.put(vfsFile, absFile);
      return absFile;
   }

   private boolean acceptForMount(org.jboss.vfs.VirtualFile vfsFile)
   {
      boolean accept = false;
      if (vfsFile.isFile() == true)
      {
         String rootName = vfsFile.getName();
         for (String suffix : suffixes)
         {
            if (rootName.endsWith(suffix))
            {
               accept = true;
               break;
            }
         }
      }
      return accept;
   }

   public Object adapt(VirtualFile absFile)
   {
      if (absFile == null)
         return null;

      VirtualFileAdaptor30 adaptor = (VirtualFileAdaptor30)absFile;
      return adaptor.getDelegate();
   }

   static void unregister(VirtualFileAdaptor30 absFile)
   {
      registry.remove(absFile.getDelegate());
   }
}