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

import java.io.IOException;
import java.net.URL;

import org.jboss.osgi.vfs.VFSAdaptor;
import org.jboss.osgi.vfs.VirtualFile;

/**
 * An adaptor to the jboss-vfs-2.1.x VFS. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2010
 */
public class VFSAdaptor21 implements VFSAdaptor
{
   public VirtualFile getRoot(URL url) throws IOException
   {
      org.jboss.virtual.VirtualFile root = org.jboss.virtual.VFS.getRoot(url);
      return new VirtualFileAdaptor21(root, root);
   }

   public VirtualFile adapt(Object virtualFile)
   {
      if (virtualFile == null)
         return null;

      if (virtualFile instanceof org.jboss.virtual.VirtualFile == false)
         throw new IllegalArgumentException("Not a org.jboss.virtual.VirtualFile: " + virtualFile);

      org.jboss.virtual.VirtualFile file = (org.jboss.virtual.VirtualFile)virtualFile;
      org.jboss.virtual.VirtualFile root = file;
      try
      {
         org.jboss.virtual.VirtualFile parent = file.getParent();
         while (parent != null)
         {
            root = parent;
            parent = parent.getParent();
         }
      }
      catch (IOException ex)
      {
         throw new IllegalStateException("Cannot obtain root", ex);
      }
      
      return new VirtualFileAdaptor21(root, file);
   }

   public Object adapt(VirtualFile virtualFile)
   {
      if (virtualFile == null)
         return null;

      VirtualFileAdaptor21 adaptor = (VirtualFileAdaptor21)virtualFile;
      return adaptor.getDelegate();
   }
}
