/*
 * RESTHeart - the Web API for MongoDB
 * Copyright (C) SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.handlers.txns;

import org.restheart.db.sessions.XClientSessionFactory;
import org.restheart.db.sessions.XClientSession;
import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import io.undertow.server.HttpServerExchange;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.restheart.db.Database;
import org.restheart.db.DatabaseImpl;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.representation.Resource;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a test hanldr that creates two documents in the transaction of th given session
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class TestTxnHandler extends PipedHttpHandler {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestTxnHandler.class);
    
    MongoClient MCLIENT = MongoDBClientSingleton.getInstance().getClient();

    /**
     * Creates a new instance of DeleteTxnHandler
     */
    public TestTxnHandler() {
        super();
    }

    public TestTxnHandler(PipedHttpHandler next) {
        super(next, new DatabaseImpl());
    }

    public TestTxnHandler(PipedHttpHandler next, Database dbsDAO) {
        super(next, dbsDAO);
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(
            HttpServerExchange exchange,
            RequestContext context)
            throws Exception {
        if (context.isInError()) {
            next(exchange, context);
            return;
        }

        boolean commit = exchange.getQueryParameters().containsKey("commit");

        LOGGER.debug("Going to create two docs in /db/coll and {}",
                (commit ? "commit" : "abort"));

        String sid = context.getCollectionName();

        LOGGER.debug("server session id {}", sid);

        XClientSession cs = XClientSessionFactory.getClientSession(sid);

        if (!cs.hasActiveTransaction()) {
            // this avoids sending the startTransaction msg
            cs.setMessageSentInCurrentTransaction(true);
            cs.startTransaction();
        }
        
        MongoCollection<BsonDocument> coll = MCLIENT
                .getDatabase("db")
                .getCollection("coll", BsonDocument.class);
        
        var doc1 = new BsonDocument("_id", new BsonString("txn25"));
        var doc2 = new BsonDocument("_id", new BsonString("txn35"));
        
        if (cs == null) {
            LOGGER.debug("session not found {}", sid);

            context.setResponseStatusCode(HttpStatus.SC_NOT_FOUND);
        } else {

            try {
                coll.insertOne(cs, doc1);
                
                LOGGER.debug("inserted doc {}", doc1.toJson());
                
                coll.insertOne(cs, doc2);
                
                LOGGER.debug("inserted doc {}", doc2.toJson());
                
                context.setResponseContentType(Resource.HAL_JSON_MEDIA_TYPE);
                context.setResponseStatusCode(HttpStatus.SC_OK);
            } catch (MongoCommandException mce) {
                LOGGER.error("Error {} {}, {}",
                        mce.getErrorCode(),
                        mce.getErrorCodeName(),
                        mce.getErrorMessage());

                if (mce.getErrorCode() == 20) {
                    ResponseHelper.endExchangeWithMessage(exchange,
                            context,
                            HttpStatus.SC_BAD_GATEWAY,
                            mce.getErrorCodeName() + ", " + mce.getErrorMessage());
                } else {
                    throw mce;
                }
            }
        }

        next(exchange, context);
    }
}
