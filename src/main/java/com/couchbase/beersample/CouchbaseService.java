/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.beersample;

import java.util.List;

import com.couchbase.beersample.config.Database;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.AsyncViewRow;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Skeleton class for the tutorial, where most couchbase-related operations are stubbed
 * for the user to fill-in.
 *
 * @author Simon Baslé
 */
@Service
public class CouchbaseService {

    private final Database config;

    private final Bucket bucket;

    @Autowired
    public CouchbaseService(final Database config) {
        this.config = config;

        //connect to the cluster and open the configured bucket
        Cluster cluster = CouchbaseCluster.create(config.getNodes());
        this.bucket = cluster.openBucket(config.getBucket(), config.getPassword());
    }

    /**
     * Prepare a new JsonDocument with some JSON content
     */
    public static JsonDocument createDocument(String id, JsonObject content) {
        return JsonDocument.create(id, content);
    }

    /**
     * CREATE the document in database
     * @return the created document, with up to date metadata
     */
    public JsonDocument create(JsonDocument doc) {
        return bucket.insert(doc);
    }

    /**
     * READ the document from database
     */
    public JsonDocument read(String id) {
        return bucket.get(id);
    }

    /**
     * UPDATE the document in database
     * @return the updated document, with up to date metadata
     */
    public JsonDocument update(JsonDocument doc) {
        return bucket.replace(doc);
    }

    /**
     * DELETE the document from database
     * @return the deleted document, with only metadata (since content has been deleted)
     */
    public JsonDocument delete(String id) {
        return bucket.remove(id);
    }

    /**
     * Uses a view query to find all beers. Possibly use an offset and a limit of the
     * number of beers to retrieve.
     *
     * @param offset the number of beers to skip, null or < 1 to ignore
     * @param limit the limit of beers to retrieve, null or < 1 to ignore
     */
    public ViewResult findAllBeers(Integer offset, Integer limit) {
        ViewQuery query = ViewQuery.from("beer", "by_name");
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        if (offset != null && offset > 0) {
            query.skip(offset);
        }
        ViewResult result = bucket.query(query);
        return result;
    }

    /**
     * Retrieves all the beers using a view query, returning the result asynchronously.
     */
    public Observable<AsyncViewResult> findAllBeersAsync() {
        ViewQuery allBeers = ViewQuery.from("beer", "by_name");
        return bucket.async().query(allBeers);
    }

    /**
     * READ the document asynchronously from database.
     */
    public Observable<JsonDocument> asyncRead(String id) {
        return bucket.async().get(id);
    }

    /**
     * Create a ViewQuery to retrieve all the beers for one single brewery.
     * The "\uefff" character (the largest UTF8 char) can be used to put an
     * upper limit to the brewery key retrieved by the view (which otherwise
     * would return all beers for all breweries).
     *
     * @param breweryId the brewery key for which to retrieve associated beers.
     */
    public static ViewQuery createQueryBeersForBrewery(String breweryId) {
        ViewQuery forBrewery = ViewQuery.from("beer", "brewery_beers");
        forBrewery.startKey(JsonArray.from(breweryId));
        //the trick here is that sorting is UTF8 based, uefff is the largest UTF8 char
        forBrewery.endKey(JsonArray.from(breweryId, "\uefff"));
        return forBrewery;
    }

    /**
     * Asynchronously query the database for all beers associated to a brewery.
     *
     * @param breweryId the brewery key for which to retrieve associated beers.
     * @see #createQueryBeersForBrewery(String)
     */
    public Observable<AsyncViewResult> findBeersForBreweryAsync(String breweryId) {
        return bucket.async().query(createQueryBeersForBrewery(breweryId));
    }

    /**
     * From a brewery document and a list of documents for its associated beers,
     * both asynchronously represented, prepare a stream of JSON documents concatenating
     * the data.
     *
     * Each returned document is similar to the brewery document, but with a JSON array
     * of beer info under the "beers" attribute. Each beer info is a JSON object with an "id"
     * attribute (the key for the beer) and "beer" attribute (the original whole beer data).
     */
    public static Observable<JsonDocument> concatBeerInfoToBrewery(Observable<JsonDocument> brewery,
            Observable<List<JsonDocument>> beers) {
        return Observable.zip(brewery, beers,
                new Func2<JsonDocument, List<JsonDocument>, JsonDocument>() {
                    @Override
                    public JsonDocument call(JsonDocument breweryDoc, List<JsonDocument> beersDoc) {
                        JsonArray beers = JsonArray.create();
                        for (JsonDocument beerDoc : beersDoc) {
                            JsonObject beer = JsonObject.create()
                                                        .put("id", beerDoc.id())
                                                        .put("beer", beerDoc.content());
                            beers.add(beer);
                        }
                        breweryDoc.content().put("beers", beers);
                        return breweryDoc;
                    }
                });
    }

    //===== Here is a more advanced example, using Async API to search in Beer names =====

    /**
     * From an async stream of all the beers and a search token, returns a stream
     * emitting a single JSON array. The array contains data for all matching beers,
     * each represented by three attributes: "id" (the beer's key), "name" (the beer's name)
     * and "detail" (the beers whole document content).
     */
    public Observable<JsonArray> searchBeer(Observable<AsyncViewRow> allBeers, final String token) {
        return allBeers
                //extract the document from the row and carve a result object using its content and id
                .flatMap(new Func1<AsyncViewRow, Observable<JsonObject>>() {
                    @Override
                    public Observable<JsonObject> call(AsyncViewRow row) {
                        return row.document().map(new Func1<JsonDocument, JsonObject>() {
                            @Override
                            public JsonObject call(JsonDocument jsonDocument) {
                                return JsonObject.create()
                                                 .put("id", jsonDocument.id())
                                                 .put("name", jsonDocument.content().getString("name"))
                                                 .put("detail", jsonDocument.content());
                            }
                        });
                    }
                })
                        //reject beers that don't match the partial name
                .filter(new Func1<JsonObject, Boolean>() {
                    @Override
                    public Boolean call(JsonObject jsonObject) {
                        String name = jsonObject.getString("name");
                        return name != null && name.toLowerCase().contains(token.toLowerCase());
                    }
                })
                        //collect results into a JSON array (one could also just use toList() since a List would be
                        // transcoded into a JSON array)
                .collect(new Func0<JsonArray>() { //this creates the array (once)
                    @Override
                    public JsonArray call() {
                        return JsonArray.empty();
                    }
                }, new Action2<JsonArray, JsonObject>() { //this populates the array (each item)
                    @Override
                    public void call(JsonArray objects, JsonObject jsonObject) {
                        objects.add(jsonObject);
                    }
                });
    }

}
