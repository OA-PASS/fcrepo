/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import static java.util.UUID.randomUUID;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCK_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.Lock;
import org.fcrepo.kernel.services.LockService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.graph.Triple;

/**
 * @author Mike Durbin
 */
public class FedoraLocksTest {

    private FedoraLocks testObj;

    @Mock
    private LockService mockLockService;

    @Mock
    private Lock mockLock;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private Workspace mockWorkspace;

    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new FedoraLocks();
        setField(testObj, "lockService", mockLockService);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        when(mockLock.getLockToken()).thenReturn("token");

        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");
    }

    @Test
    public void testGetLock() throws RepositoryException {
        final String pid = randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);
        when(mockLockService.getLock(mockSession, path)).thenReturn(mockLock);

        testObj.getLock(pid);

        verify(mockLockService).getLock(mockSession, path);
    }

    @Test
    public void testCreateLock() throws RepositoryException, URISyntaxException {
        final String pid = randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);
        when(mockLockService.acquireLock(mockSession, path, false)).thenReturn(mockLock);

        final Response response = testObj.createLock(pid, false);

        verify(mockLockService).acquireLock(mockSession, path, false);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteLock() throws RepositoryException {
        final String pid = randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);

        final Response response = testObj.deleteLock(pid);

        verify(mockLockService).releaseLock(mockSession, path);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRDFGenerationForLockToken() throws RepositoryException {
        final String pid = randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);
        when(mockLockService.getLock(mockSession, path)).thenReturn(mockLock);

        final RdfStream stream = testObj.getLock(pid);
        while (stream.hasNext()) {
            final Triple t = stream.next();
            if (t.getPredicate().getURI().equals(HAS_LOCK_TOKEN.getURI())
                    && t.getObject().getLiteralValue().equals(mockLock.getLockToken())) {
                return;
            }
        }
        fail("Unable to find the lock token in the returned RDF!");
    }

    private void initializeMockNode(final String path) throws RepositoryException {
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getNode(path)).thenReturn(mockNode);
    }

}