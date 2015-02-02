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

        //FIXME connect to the cluster and open the configured bucket
        this.bucket = null;
    }

    /**
     * Prepare a new JsonDocument with some JSON content
     */
    public static JsonDocument createDocument(String id, JsonObject content) {
        //FIXME return the prepared JsonDocument
        return null;
    }

    /**
     * CREATE the document in database
     * @return the created document, with up to date metadata
     */
    public JsonDocument create(JsonDocument doc) {
        //FIXME use the bucket to create a doc in database
        return null;
    }

    /**
     * READ the document from database
     */
    public JsonDocument read(String id) {
        //FIXME use the bucket to read and return a doc from database
        return null;
    }

    /**
     * UPDATE the document in database
     * @return the updated document, with up to date metadata
     */
    public JsonDocument update(JsonDocument doc) {
        //FIXME use the bucket to update a doc in database
        return null;
    }

    /**
     * DELETE the document from database
     * @return the deleted document, with only metadata (since content has been deleted)
     */
    public JsonDocument delete(String id) {
        //FIXME use the bucket to delete a doc in database
        return null;
    }

    /**
     * Uses a view query to find all beers. Possibly use an offset and a limit of the
     * number of beers to retrieve.
     *
     * @param offset the number of beers to skip, null or < 1 to ignore
     * @param limit the limit of beers to retrieve, null or < 1 to ignore
     */
    public ViewResult findAllBeers(Integer offset, Integer limit) {
        //FIXME prepare the query, choosing the right design document and view
        ViewQuery query = null;

        //FIXME augment the query with adequate parameters if offset and/or limit are set

        //FIXME execute the query and return the result
        ViewResult result = null;
        return result;
    }

    /**
     * Retrieves all the beers using a view query, returning the result asynchronously.
     */
    public Observable<AsyncViewResult> findAllBeersAsync() {
        //FIXME prepare a query for all beers using the right design document and view
        ViewQuery allBeers = null;

        //FIXME execute the query asynchronously
        return null;
    }

    /**
     * READ the document asynchronously from database.
     */
    public Observable<JsonDocument> asyncRead(String id) {
        //FIXME read the document asynchronously
        return null;
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
        String veryLargeUtf8Char = "\uefff";
        //FIXME prepare a query to list beers associated with a brewery
        ViewQuery forBrewery = null;
        //FIXME limit the results to beers for the specific breweryId (hint: use startKey and endKey)

        return forBrewery;
    }

    /**
     * Asynchronously query the database for all beers associated to a brewery.
     *
     * @param breweryId the brewery key for which to retrieve associated beers.
     * @see #createQueryBeersForBrewery(String)
     */
    public Observable<AsyncViewResult> findBeersForBreweryAsync(String breweryId) {
        ViewQuery beersForBrewery = createQueryBeersForBrewery(breweryId);
        //FIXME execute the query asynchronously
        return null;
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
        //FIXME produce the correct JsonDocument stream from the stream of brewery info and the one of beers list.
        /* Hint: Use a factory method from Observable.
        The following code can be used in a ZIP FUNCTION to produce such a correct hybrid document:

        JsonArray beers = JsonArray.create();
        for (JsonDocument beerDoc : beersDoc) {
            JsonObject beer = JsonObject.create()
                            .put("id", beerDoc.id())
                            .put("beer", beerDoc.content());
            beers.add(beer);
        }
        breweryDoc.content().put("beers", beers);
        return breweryDoc;

        */
        return null;
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
                //FIXME extract the data for each row into an id+name+detail beer document
                // hint: transform an AsyncViewRow into an Observable<JsonObject>
                //FIXME reject beers that don't match the partial name
                //collect results into a JSON array (one could also just use toList() since a List would be
                // transcoded into a JSON array), using collect operator.
                .collect(
                        null, //FIXME create the array (invoked once)
                        null  //FIXME populate the array (invoked for each item)
                );
    }

}
