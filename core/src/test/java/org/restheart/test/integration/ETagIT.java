/*-
 * ========================LICENSE_START=================================
 * restheart-core
 * %%
 * Copyright (C) 2014 - 2022 SoftInstigate
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * =========================LICENSE_END==================================
 */
package org.restheart.test.integration;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class ETagIT extends AbstactIT {

    private final String DB = TEST_DB_PREFIX + "-etag-db";
    private final String DB_REQUIRED = TEST_DB_PREFIX + "-etag-required-db";
    private final String COLL_REQUIRED = "coll-required";
    private final String COLL = "coll";

    @SuppressWarnings("rawtypes")
    HttpResponse resp;

    /**
     *
     * @throws URISyntaxException
     */
    public ETagIT() throws URISyntaxException {
    }

    /**
     *
     * @throws Exception
     */
    @Before
    public void createTestData() throws Exception {
        // create test db
        resp = Unirest.put(url(DB))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .asString();

        Assert.assertEquals("create db " + DB, HttpStatus.SC_CREATED, resp.getStatus());

        // create test db with required policy
        resp = Unirest.put(url(DB_REQUIRED))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .body("{'etagDocPolicy': 'required'}")
                .asString();

        Assert.assertEquals("create db " + DB_REQUIRED, HttpStatus.SC_CREATED, resp.getStatus());

        // create collection
        resp = Unirest.put(url(DB, COLL))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .asString();

        Assert.assertEquals("create collection " + DB.concat("/").concat(COLL), HttpStatus.SC_CREATED, resp.getStatus());

        // create collection
        resp = Unirest.put(url(DB_REQUIRED, COLL))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .asString();

        Assert.assertEquals("create collection " + DB.concat("/").concat(COLL), HttpStatus.SC_CREATED, resp.getStatus());

        // create documents
        resp = Unirest.put(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .asString();

        // create documents
        resp = Unirest.put(url(DB_REQUIRED, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .queryString("wm", "upsert")
                .asString();

        Assert.assertEquals("create document " + DB_REQUIRED.concat("/").concat(COLL).concat("/docid"), HttpStatus.SC_CREATED, resp.getStatus());

        // create collection with required etag
        resp = Unirest.put(url(DB, COLL_REQUIRED))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .body("{'etagDocPolicy': 'required'}")
                .asString();

        Assert.assertEquals("create collection " + DB.concat("/").concat(COLL), HttpStatus.SC_CREATED, resp.getStatus());

        // create document
        resp = Unirest.put(url(DB, COLL_REQUIRED, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .queryString("wm", "upsert")
                .asString();

        Assert.assertEquals("create document " + DB.concat("/").concat(COLL).concat("/docid"), HttpStatus.SC_CREATED, resp.getStatus());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testUpdateNotRequired() throws Exception {
        // makes ure docid does not exists
        resp = Unirest.delete(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .asString();

        resp = Unirest.put(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("wm", "upsert")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of create test data", HttpStatus.SC_CREATED, resp.getStatus());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testUpdateRequired() throws Exception {
        resp = Unirest.put(url(DB, COLL_REQUIRED, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("wm", "upsert")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of data with missing etag", HttpStatus.SC_CONFLICT, resp.getStatus());

        resp = Unirest.put(url(DB, COLL_REQUIRED, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .header("If-Match", "wrong etag")
                .queryString("wm", "upsert")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of update data with wrong etag", HttpStatus.SC_PRECONDITION_FAILED, resp.getStatus());

        String etag = resp.getHeaders().get("ETag").get(0);

        resp = Unirest.put(url(DB, COLL_REQUIRED, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("checkEtag", "")
                .queryString("wm", "upsert")
                .header("If-Match", etag)
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of update data with correct etag", HttpStatus.SC_OK, resp.getStatus());

        // now use the DB_REQUIRED
        resp = Unirest.put(url(DB_REQUIRED, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("wm", "upsert")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of create test data", HttpStatus.SC_CONFLICT, resp.getStatus());

        resp = Unirest.put(url(DB_REQUIRED, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .header("If-Match", "wrong etag")
                .queryString("wm", "upsert")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of update data without etag", HttpStatus.SC_PRECONDITION_FAILED, resp.getStatus());

        etag = resp.getHeaders().get("ETag").get(0);

        resp = Unirest.put(url(DB_REQUIRED, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("checkEtag", "")
                .queryString("wm", "upsert")
                .header("If-Match", etag)
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of update data with correct etag", HttpStatus.SC_OK, resp.getStatus());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testUpdateEtagQParam() throws Exception {
        // this makes sure that the document docid exists
        resp = Unirest.put(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("checkEtag", "")
                .queryString("wm", "insert")
                .body("{'a':1 }")
                .asString();

        resp = Unirest.put(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("checkEtag", "")
                .queryString("wm", "update")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of create test data", HttpStatus.SC_CONFLICT, resp.getStatus());

        resp = Unirest.put(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .header("content-type", "application/json")
                .queryString("checkEtag", "")
                .queryString("wm", "update")
                .header("If-Match", "wrong etag")
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of create test data", HttpStatus.SC_PRECONDITION_FAILED, resp.getStatus());

        String etag = resp.getHeaders().get("ETag").get(0);

        resp = Unirest.put(url(DB, COLL, "docid"))
                .basicAuth(ADMIN_ID, ADMIN_PWD)
                .queryString("checkEtag", "")
                .queryString("wm", "update")
                .header("content-type", "application/json")
                .header("If-Match", etag)
                .body("{'a':1 }")
                .asString();

        Assert.assertEquals("check response status of create test data", HttpStatus.SC_OK, resp.getStatus());
    }
}
