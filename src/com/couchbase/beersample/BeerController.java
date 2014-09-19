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

package com.couchbase.beersample;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.AsyncViewRow;
import com.couchbase.client.java.view.ViewRow;


@Controller
public class BeerController {
	public ConnectionManager connectionManager = ConnectionManager.getInstance();
	@RequestMapping("/beers")
    String beers(Model model, @RequestParam(value = "beer", required = false) String beer) {
		
		if (beer != null && !beer.isEmpty()) {
			JsonDocument response = ConnectionManager.getItem(beer);
			if (response != null){
				model.addAttribute("beer", response.content().toMap());
			}
			
			return "beer";
			}
		else{
			ArrayList<AsyncViewRow> result = ConnectionManager.getView("beer", "by_name");
			model.addAttribute("beers", result);
		    return "beers";
		}
    }
    
    @RequestMapping("/beer/delete")
    String delete(Model model, @RequestParam(value = "beer", required = true) String beer){
    	ConnectionManager.deleteItem(beer);
    	model.addAttribute("deleted","Beer Deleted");
    	ArrayList<AsyncViewRow> result = ConnectionManager.getView("beer", "by_name");
		model.addAttribute("beers", result);
    	return "beers";
    }

	@RequestMapping(value = "/beer/edit", method=RequestMethod.GET)
    String editGet(Model model, @RequestParam(value = "beer", required = true) String beer){
		JsonDocument response = ConnectionManager.getItem(beer);
		if (response != null){
			Map<String, Object> map = response.content().toMap();
			model.addAttribute("beer", map);
			BeerModel beerModel = new BeerModel();

			beerModel.setId(response.id());
			beerModel.setName(map.getOrDefault("name", "").toString());
			beerModel.setStyle(map.getOrDefault("style", "").toString());
			beerModel.setDescription(map.getOrDefault("description", "").toString());
			beerModel.setCategory(map.getOrDefault("category", "").toString());
			beerModel.setAbv(map.getOrDefault("abv", "").toString());
			beerModel.setSrm(map.getOrDefault("srm", "").toString());
			beerModel.setIbu(map.getOrDefault("ibu", "").toString());
			beerModel.setUpc(map.getOrDefault("upc", "").toString());
			beerModel.setBrewery(map.getOrDefault("brewery_id", "").toString());
			
			model.addAttribute("beerModel", beerModel);
		}
    	return "beerEdit";
    }
	
	@RequestMapping(value = "/beer/edit/submit", method=RequestMethod.POST)
    String editPost(Model model, @ModelAttribute(value = "beerModel") BeerModel beerModel){
		JsonObject beer = JsonObject.empty()
				.put("name", beerModel.getName())
				.put("style", beerModel.getStyle())
				.put("description", beerModel.getDescription())
				.put("abv", beerModel.getAbv())
				.put("ibu", beerModel.getIbu())
				.put("srm", beerModel.getSrm())
				.put("upc", beerModel.getUpc())
				.put("brewery_id", beerModel.getBrewery())
				.put("type", "beer");
		JsonDocument doc = JsonDocument.create(beerModel.getId(),beer);
		ConnectionManager.updateItem(doc);
		
    	return "redirect:/beers/?beer=" + beerModel.getId();
    }
    
    @RequestMapping("/beers/search")
    String search(@RequestParam(value = "beer", required = false) String beer){
    	return "search";
    }
}
	