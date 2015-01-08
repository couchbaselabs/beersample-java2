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
package com.couchbase.beersample.beers;

import java.util.Iterator;
import java.util.Map;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.AsyncViewRow;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * REST CRUD Controller for beers
 *
 * @author Simon Baslé
 * @since 1.0
 */
@RestController
@RequestMapping("/beer")
public class BeersController {

    private final Bucket bucket;

    @Autowired
    public BeersController(final Bucket bucket) {
        this.bucket = bucket;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBeer(@PathVariable String id) {
        JsonDocument doc = bucket.get(id);
        if (doc != null) {
            return new ResponseEntity<String>(doc.content().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createBeer(@RequestBody Map<String, Object> beerData) {
        String id = "";
        try {
            JsonObject beer = parseBeer(beerData);
            id = "beer-" + beer.getString("name");
            bucket.insert(JsonDocument.create(id, beer));
            return new ResponseEntity<String>(id, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        } catch (DocumentAlreadyExistsException e) {
            return new ResponseEntity<String>("Id " + id + " already exist", HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{beerId}")
    public ResponseEntity<String> deleteBeer(@PathVariable String beerId) {
        JsonDocument deleted = bucket.remove(beerId);
        return new ResponseEntity<String>(""+deleted.cas(), HttpStatus.OK);
    }

    @RequestMapping(value = "/{beerId}", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
    public ResponseEntity<String> updateBeer(@PathVariable String beerId, @RequestBody Map<String, Object> beerData) {
        try {
            JsonObject beer = parseBeer(beerData);
            bucket.replace(JsonDocument.create(beerId, beer));
            return new ResponseEntity<String>(beerId, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        } catch (DocumentDoesNotExistException e) {
            return new ResponseEntity<String>("Id " + beerId + " does not exist", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonObject parseBeer(Map<String, Object> beerData) {
        String type = (String) beerData.get("type");
        String name = (String) beerData.get("name");
        if (type == null || name == null || type.isEmpty() || name.isEmpty()) {
           throw new IllegalArgumentException();
        } else {
            JsonObject beer = JsonObject.create();
            for (Map.Entry<String, Object> entry : beerData.entrySet()) {
                beer.put(entry.getKey(), entry.getValue());
            }
            return beer;
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listBeers(@RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit) {
        ViewQuery query = ViewQuery.from("beer", "by_name");
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        if (offset != null && offset > 0) {
            query.skip(offset);
        }
        ViewResult result = bucket.query(query);
        if (!result.success()) {
            //TODO maybe detect type of error and change error code accordingly
            return new ResponseEntity<String>(result.error().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            JsonArray keys = JsonArray.create();
            Iterator<ViewRow> iter = result.rows();
            while(iter.hasNext()) {
                ViewRow row = iter.next();
                JsonObject beer = JsonObject.create();
                beer.put("name", row.key());
                beer.put("id", row.id());
                keys.add(beer);
            }
            return new ResponseEntity<String>(keys.toString(), HttpStatus.OK);
        }
    }

    //===== Here is a more advanced example, using Async API to search in Beer names =====

    @RequestMapping(method = RequestMethod.GET, value = "/search/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchBeer(@PathVariable final String token) {
        //we'll get all beers asynchronously and compose on the stream to extract those that match
        ViewQuery allBeers = ViewQuery.from("beer", "by_name");
        AsyncViewResult result = bucket.async().query(allBeers).toBlocking().single();
        if (result.success()) {
            return result.rows()
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
                    })
                    //transform the array into a ResponseEntity with correct status
                    .map(new Func1<JsonArray, ResponseEntity<String>>() {
                        @Override
                        public ResponseEntity<String> call(JsonArray objects) {
                            return new ResponseEntity<String>(objects.toString(), HttpStatus.OK);
                        }
                    })
                    //in case of errors during this processing, return a ERROR 500 response with detail
                    .onErrorReturn(new Func1<Throwable, ResponseEntity<String>>() {
                        @Override
                        public ResponseEntity<String> call(Throwable throwable) {
                            return new ResponseEntity<String>("Error while parsing results - " + throwable,
                                    HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }).toBlocking().single(); //block and send back the response

        } else {
            return new ResponseEntity<String>("Error while searching - " + result.error(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
