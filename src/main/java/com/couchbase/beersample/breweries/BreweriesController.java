/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.beersample.breweries;

import java.util.List;

import com.couchbase.beersample.CouchbaseService;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.AsyncViewRow;
import com.couchbase.client.java.view.ViewQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * REST CRUD Controller for breweries
 *
 * @author Simon Baslé
 * @since 1.0
 */
@RestController
@RequestMapping(value = "/brewery", produces = MediaType.APPLICATION_JSON_VALUE)
public class BreweriesController {

    private static final Logger LOGGER =  LoggerFactory.getLogger(BreweriesController.class);

    private final CouchbaseService couchbaseService;

    @Autowired
    public BreweriesController(final CouchbaseService couchbaseService) {
        this.couchbaseService = couchbaseService;
    }

    @RequestMapping("/{id}")
    public ResponseEntity<String> getBrewery(@PathVariable String id) {

        ViewQuery forBrewery = CouchbaseService.createQueryBeersForBrewery(id);

        Observable<JsonDocument> brewery = couchbaseService.asyncRead(id);
        Observable<List<JsonDocument>> beers =
                couchbaseService.findBeersForBreweryAsync(id)
                //extract rows from the result
                .flatMap(new Func1<AsyncViewResult, Observable<AsyncViewRow>>() {
                    @Override
                    public Observable<AsyncViewRow> call(AsyncViewResult asyncViewResult) {
                        return asyncViewResult.rows();
                    }
                })
                //extract the actual document (pair of brewery id and beer id)
                .flatMap(new Func1<AsyncViewRow, Observable<JsonDocument>>() {
                    @Override
                    public Observable<JsonDocument> call(AsyncViewRow asyncViewRow) {
                        return asyncViewRow.document();
                    }
                })
                .toList();

        //in the next observable we'll transform list of brewery-beer pairs into an array of beers
        //then we'll inject it into the associated brewery jsonObject
        Observable<JsonDocument> fullBeers = couchbaseService.concatBeerInfoToBrewery(brewery, beers)
                //take care of the case where no corresponding brewery info was found
                .singleOrDefault(JsonDocument.create("empty",
                        JsonObject.create().put("error", "brewery " + id + " not found")))
                //log errors and return a json describing the error if one arises
                .onErrorReturn(new Func1<Throwable, JsonDocument>() {
                    @Override
                    public JsonDocument call(Throwable throwable) {
                        LOGGER.warn("Couldn't get beers", throwable);
                        return JsonDocument.create("error",
                                JsonObject.create().put("error", throwable.getMessage()));
                    }
                });

        try {
            return new ResponseEntity<String>(fullBeers.toBlocking().single().content().toString(), HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("Unable to get brewery " + id, e);
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


}
