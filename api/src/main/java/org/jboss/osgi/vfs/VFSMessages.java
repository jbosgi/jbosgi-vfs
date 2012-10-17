/*
 * #%L
 * JBossOSGi VFS API
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.osgi.vfs;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Logging Id ranges: 10100-10199
 *
 * https://docs.jboss.org/author/display/JBOSGI/JBossOSGi+Logging
 *
 * @author Thomas.Diesler@jboss.com
 */
@MessageBundle(projectCode = "JBOSGI")
public interface VFSMessages {

    VFSMessages MESSAGES = Messages.getBundle(VFSMessages.class);

    @Message(id = 10100, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 10101, value = "Cannot load VFS adaptor")
    IllegalStateException illegalStateCannotLoadAdaptor();

    @Message(id = 10102, value = "Cannot create VFS adaptor")
    IllegalStateException illegalStateCannotCreateAdaptor(@Cause Throwable cause);

    @Message(id = 10103, value = "Not a VirtualFile: %s")
    IllegalArgumentException illegalArgumentNoVirtualFile(Object other);

    @Message(id = 10104, value = "Error visiting VirtualFile: %s")
    RuntimeException runtimeErrorVistingFile(@Cause Throwable cause, Object file);

    @Message(id = 10105, value = "Cannot create VFS temp file provider")
    IllegalStateException illegalStateCannotCreateTempFileProvider(@Cause Throwable cause);

    @Message(id = 10106, value = "Cannot close VFS temp file provider")
    IllegalStateException illegalStateCannotCloseTempFileProvider(@Cause Throwable cause);
}
