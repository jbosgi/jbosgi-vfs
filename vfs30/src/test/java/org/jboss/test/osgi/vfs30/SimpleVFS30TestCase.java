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
package org.jboss.test.osgi.vfs30;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.vfs30.bundle.SimpleActivator;
import org.jboss.vfs.VFSUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Constants;

/**
 * A test that verifies the VFS30 abstraction.
 * 
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 11-Mar-2010
 */
public class SimpleVFS30TestCase {
    private static File file, file2;

    @BeforeClass
    public static void beforeClass() throws IOException {
        createExampleSimple();
        createExample2();
    }

    private static void createExampleSimple() throws IOException {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-simple.jar");
        archive.addClass(SimpleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                String path = "/simple/" + JarFile.MANIFEST_NAME;
                try {
                    URL manifest = getClass().getResource(path);
                    return manifest.openStream();
                } catch (IOException ex) {
                    throw new IllegalStateException("Cannot open stream for: " + path, ex);
                }
            }
        });
        file = toFile(archive);
    }

    private static void createExample2() throws IOException {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example2.jar");
        archive.addResource(getAsset("file1"), "file1.txt");
        archive.addResource(getAsset("file2"), "file2.txt");
        archive.addResource(getAsset("file3"), "sub/file3.txt");
        archive.addResource(getAsset("file4"), "sub/file4.txt");
        archive.addResource(getAsset("file5"), "sub/sub/file5.txt");
        archive.addResource(getAsset("file6"), "sub/sub1/file6.txt");
        file2 = toFile(archive);
    }

    @Test
    public void testFromURL() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI().toURL());
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals(file.getAbsolutePath() + "/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFromURI() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals(file.getAbsolutePath() + "/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFromStream() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile("foo", new FileInputStream(file));
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals("/foo/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFromStream2() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile("/foo", new FileInputStream(file));
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals("/foo/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFromStream3() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile("/foo/", new FileInputStream(file));
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals("/foo/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFromStream4() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile("/foo/bar", new FileInputStream(file));
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals("/foo/bar/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFromStream5() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile("file://foo/bar", new FileInputStream(file));
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertEquals("/file:/foo/bar/" + JarFile.MANIFEST_NAME, child.getPathName());
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testManifestAccess() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertNotNull("Manifest not null", child);

            Manifest manifest = new Manifest();
            manifest.read(child.openStream());
            Attributes attributes = manifest.getMainAttributes();
            String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
            assertEquals("example-simple", symbolicName);
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testManifestURLAccess() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            VirtualFile child = virtualFile.getChild(JarFile.MANIFEST_NAME);
            assertNotNull("Manifest not null", child);

            URL childURL = child.toURL();
            InputStream is = childURL.openStream();

            Manifest manifest = new Manifest();
            manifest.read(is);
            Attributes attributes = manifest.getMainAttributes();
            String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
            assertEquals("example-simple", symbolicName);
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testGetEntryPaths() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            Set<String> actual = new HashSet<String>();
            Enumeration<String> en = virtualFile.getEntryPaths("/");
            while (en.hasMoreElements())
                actual.add(en.nextElement());

            Set<String> expected = new HashSet<String>();
            expected.add("org/");
            expected.add("META-INF/");
            assertEquals(expected, actual);
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFindEntries() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            Set<String> actual = new HashSet<String>();
            Enumeration<URL> en = virtualFile.findEntries("/", null, true);
            while (en.hasMoreElements())
                actual.add(en.nextElement().toExternalForm());

            Set<String> expected = new HashSet<String>();
            expected.add(virtualFile.toURL() + "org/");
            expected.add(virtualFile.toURL() + "org/jboss/");
            expected.add(virtualFile.toURL() + "org/jboss/test/");
            expected.add(virtualFile.toURL() + "org/jboss/test/osgi/");
            expected.add(virtualFile.toURL() + "org/jboss/test/osgi/vfs30/");
            expected.add(virtualFile.toURL() + "org/jboss/test/osgi/vfs30/bundle/");
            expected.add(virtualFile.toURL() + "org/jboss/test/osgi/vfs30/bundle/SimpleActivator.class");
            expected.add(virtualFile.toURL() + "META-INF/");
            expected.add(virtualFile.toURL() + "META-INF/MANIFEST.MF");
            assertEquals(expected, actual);
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testFindEntries2() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file2.toURI());
        try {
            Set<String> actual = new HashSet<String>();
            Enumeration<URL> en = virtualFile.findEntries("sub", "*", false);
            while (en.hasMoreElements())
                actual.add(en.nextElement().toExternalForm());

            Set<String> expected = new HashSet<String>();
            expected.add(virtualFile.toURL() + "sub/file3.txt");
            expected.add(virtualFile.toURL() + "sub/file4.txt");
            expected.add(virtualFile.toURL() + "sub/sub/");
            expected.add(virtualFile.toURL() + "sub/sub1/");
            assertEquals(expected, actual);
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testStreamURLAccess() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            URL streamURL = virtualFile.getStreamURL();
            JarInputStream jarIn = new JarInputStream(streamURL.openStream());
            Manifest manifest = jarIn.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
            assertEquals("example-simple", symbolicName);
        } finally {
            virtualFile.close();
        }
    }

    @Test
    public void testStreamAccess() throws Exception {
        VirtualFile virtualFile = AbstractVFS.toVirtualFile(file.toURI());
        try {
            InputStream instream = virtualFile.openStream();
            JarInputStream jarIn = new JarInputStream(instream);
            Manifest manifest = jarIn.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
            assertEquals("example-simple", symbolicName);
        } finally {
            virtualFile.close();
        }
    }

    private static File toFile(JavaArchive archive) throws IOException {
        ZipExporter exporter = archive.as(ZipExporter.class);
        InputStream inputStream = exporter.exportZip();
        File file = new File("target/" + archive.getName());
        VFSUtils.copyStream(inputStream, new FileOutputStream(file));
        return file;
    }

    private static Asset getAsset(final String content) {
        return new Asset() {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(content.getBytes());
            }
        };
    }
}