/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import java.time.Instant;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.GONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class TombstoneExceptionMapperTest {

    @Mock
    private FedoraResource mockResource;

    private final FedoraId fedoraId = FedoraId.create("/some:uri");

    private final Instant deleteTime = Instant.now();

    private ExceptionMapper<TombstoneException> testObj;

    private static final String SERVER_URI = "http://localhost:8080/rest(.*)";

    private final HttpIdentifierConverter idConverter = new HttpIdentifierConverter(UriBuilder.fromUri(SERVER_URI));

    @Before
    public void setUp() {
        initMocks(this);
        when(mockResource.getFedoraId()).thenReturn(fedoraId);
        when(mockResource.getLastModifiedDate()).thenReturn(deleteTime);
        testObj = new TombstoneExceptionMapper();
    }

    @Test
    public void testUrilessException() {
        final Response response = testObj.toResponse(new TombstoneException(mockResource));
        assertEquals(GONE.getStatusCode(), response.getStatus());
        assertTombstone(response, fedoraId.getFullIdPath(), null);
    }

    @Test
    public void testExceptionWithUri() {
        final String tombstone = idConverter.toExternalId(fedoraId.asTombstone().getFullId());
        final Response response = testObj.toResponse(new TombstoneException(mockResource, tombstone));
        assertEquals(GONE.getStatusCode(), response.getStatus());
        assertTombstone(response, fedoraId.getFullIdPath(), tombstone);
    }

    private void assertTombstone(final Response response, final String tombstoneAt, final String tombstoneUri) {
        if (tombstoneUri == null) {
            assertNull(response.getHeaderString(LINK));
        } else {
            final Link link = Link.valueOf(response.getHeaderString(LINK));
            assertEquals(tombstoneUri, link.getUri().toString());
            assertEquals("hasTombstone", link.getRel());
        }
        final String expectedString = "Discovered tombstone resource at " + tombstoneAt + ", departed at: " +
                ISO_INSTANT.withZone(UTC).format(deleteTime);
        assertEquals(expectedString, response.getEntity().toString());
    }
}
