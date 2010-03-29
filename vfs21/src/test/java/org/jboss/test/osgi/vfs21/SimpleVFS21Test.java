/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.osgi.vfs21;

//$Id: SimpleTestCase.java 102173 2010-03-09 21:16:59Z thomas.diesler@jboss.com $

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VFSUtils;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.vfs21.bundle.SimpleActivator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Constants;

/**
 * A test that verifies the VFS21 abstraction.
 * 
 * @author thomas.diesler@jboss.com
 * @since 11-Mar-2010
 */
public class SimpleVFS21Test
{
   private static File archiveFile;
   private static VirtualFile virtualFile;
   
   @BeforeClass
   public static void beforeClass() throws IOException
   {
      JavaArchive archive = Archives.create("example-simple.jar", JavaArchive.class);
      archive.addClass(SimpleActivator.class);
      Asset asset = new Asset()
      {
         public InputStream openStream()
         {
            String path = "/simple/" + JarFile.MANIFEST_NAME;
            try
            {
               URL manifest = getClass().getResource(path);
               return manifest.openStream();
            }
            catch (IOException ex)
            {
               throw new IllegalStateException("Cannot open stream for: " + path, ex);
            }
         }
      };
      archive.add(asset, JarFile.MANIFEST_NAME);

      // Convert archive to file URL 
      ZipExporter exporter = archive.as(ZipExporter.class);
      archiveFile = File.createTempFile("archive_", ".jar");
      exporter.exportZip(archiveFile, true);
      archiveFile.deleteOnExit();
      
      FileInputStream fis = new FileInputStream(archiveFile);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      VFSUtils.copyStream(fis, baos);
      
      virtualFile = AbstractVFS.getRoot(archiveFile.toURI().toURL());
   }
   
   @AfterClass
   public static void afterClass() throws IOException
   {
      if (virtualFile != null)
         virtualFile.close();
   }
   
   @Test
   public void testManifestAccess() throws Exception
   {
      VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
      assertNotNull("Manifest not null", child);
      
      Manifest manifest = new Manifest();
      manifest.read(child.openStream());
      Attributes attributes = manifest.getMainAttributes();
      String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
      assertEquals("example-simple", symbolicName);
   }
   
   @Test
   public void testManifestURLAccess() throws Exception
   {
      VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
      assertNotNull("Manifest not null", child);
      
      URL childURL = child.toURL();
      InputStream is = childURL.openStream();
      
      Manifest manifest = new Manifest();
      manifest.read(is);
      Attributes attributes = manifest.getMainAttributes();
      String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
      assertEquals("example-simple", symbolicName);
   }
   
   @Test
   public void testGetEntryPaths() throws Exception
   {
      Set<String> actual = new HashSet<String>();
      Enumeration<String> en = virtualFile.getEntryPaths("/");
      while(en.hasMoreElements())
         actual.add(en.nextElement());
      
      Set<String> expected = new HashSet<String>();
      expected.add("org/");
      expected.add("org/jboss/");
      expected.add("org/jboss/test/");
      expected.add("org/jboss/test/osgi/");
      expected.add("org/jboss/test/osgi/vfs21/");
      expected.add("org/jboss/test/osgi/vfs21/bundle/");
      expected.add("org/jboss/test/osgi/vfs21/bundle/SimpleActivator.class");
      expected.add("META-INF/");
      expected.add("META-INF/MANIFEST.MF");
      assertEquals(expected, actual);
   }
   
   @Test
   public void testFindEntries() throws Exception
   {
      Set<String> actual = new HashSet<String>();
      Enumeration<URL> en = virtualFile.findEntries("/", null, true);
      while(en.hasMoreElements())
         actual.add(en.nextElement().toExternalForm());
      
      Set<String> expected = new HashSet<String>();
      expected.add(virtualFile.toURL() + "org/jboss/test/osgi/vfs21/bundle/SimpleActivator.class");
      expected.add(virtualFile.toURL() + "META-INF/MANIFEST.MF");
      assertEquals(expected, actual);
   }
   
   @Test
   public void testStreamURLAccess() throws Exception
   {
      URL streamURL = virtualFile.getStreamURL();
      JarInputStream jarIn = new JarInputStream(streamURL.openStream());
      Manifest manifest = jarIn.getManifest();
      Attributes attributes = manifest.getMainAttributes();
      String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
      assertEquals("example-simple", symbolicName);
   }
   
   @Test
   public void testStreamAccess() throws Exception
   {
      InputStream instream = virtualFile.openStream();
      JarInputStream jarIn = new JarInputStream(instream);
      Manifest manifest = jarIn.getManifest();
      Attributes attributes = manifest.getMainAttributes();
      String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
      assertEquals("example-simple", symbolicName);
   }
}