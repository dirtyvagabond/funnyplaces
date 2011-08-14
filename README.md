# funnyplaces

funnyplaces is an experimental Clojure client for Factual's API. It supports rich queries against Factual's datasets, including their Places data.

## Basic Usage

Establish credentials:

	(factual! "YOUR_FACTUAL_KEY" "YOUR_FACTUAL_SECRET")

Now you can use the <tt>fetch</tt> function to run queries against Factual's tables:

	;; Fetch 3 Places from Factual
	(fetch :places :limit 3)

<tt>fetch</tt> takes the table name as the first argument, followed by a list of option pairs. It returns a sequence of records, where each record is a hashmap representing a row of results. So our results from above will look like:

	[{:status 1, :country US, :longitude -94.819339, :name Lorillard Tobacco Co., :postcode 66218, ... }
	 {:status 1, :country US, :longitude -118.300024, :name Imediahouse, :postcode 90005, ... }
	 {:status 1, :country US, :longitude -118.03132, :name El Monte Wholesale Meats, :postcode 91733, ... }]

This means it's easy to compose concise queries. For example:

	;; Get the names of 3 Places from Factual
	> (map :name (fetch :places :limit 3))
	("Lorillard Tobacco Co." "Imediahouse" "El Monte Wholesale Meats")

Some more examples of <tt>fetch</tt> usage:

	;; Return rows where region equals "CA"
	(fetch :places :filters {"region" "CA"})

	;; Return rows where name begins with "Starbucks" and return both the data and a total count of the matched rows:
	(fetch :places :filters {:name {:$bw "Starbucks"}} :include_count true)

	;; Do a full text search for rows that contain "Starbucks" or "Santa Monica"
	(fetch :places :q "Starbucks,Santa Monica")

	;; Do a full text search for rows that contain "Starbucks" or "Santa Monica" and return rows 20-40
	(fetch :places :q "Starbucks,Santa Monica" :offset 20 :limit 20)

## Results Metadata

Factual's API returns more than just results rows from a fetch. It also returns various metadata about the results. For example, each response comes with a count of how many rows are being returned, a count of how many total rows match in the underlying Factual dataset (if requested by you), and so on.

You can get at this metadata by using Clojure's <tt>meta</tt> function on the result you get back from a fetch. For example:

	> (def starbucks (fetch :places :filters {:name {:$bw "Starbucks"}} :include_count true))
	> (meta starbucks)
	{:total_row_count 8751, :included_rows 20, :version 3, :status "ok"}

## Places Usage

	;; Return rows with a name equal to "Stand" within 5000 meters of the specified lat/lng
	(fetch :places
	       :filters {:name "Stand"}
	       :geo {:$circle {:$center [34.06018, -118.41835] :$meters 5000}})

## Crosswalk Usage

	;; Return all Crosswalk data for the place identified by the specified Factual ID
	(crosswalk :factual_id "97598010-433f-4946-8fd5-4a6dd1639d77")

	;; Return Loopt.com Crosswalk data for the place identified by the specified Factual ID
	(crosswalk :factual_id "97598010-433f-4946-8fd5-4a6dd1639d77" :only "loopt")

	;; Return all Crosswalk data for the place identified by the specified Foursquare ID
	(crosswalk :namespace "foursquare" :namespace_id 215159)

	;; Return the Yelp.com Crosswalk data for the place identified by the specified Foursquare ID: 
	(crosswalk :namespace "foursquare" :namespace_id 215159 :only "yelp")

## Crossref Usage

The <tt>get-factid</tt> function takes a URL and returns the Factual ID for the place mentioned on the specified URL. For example: 

	> (get-factid "https://foursquare.com/venue/215159")
	[{:url "https://foursquare.com/venue/215159", :is_canonical true, :factual_id "97598010-433f-4946-8fd5-4a6dd1639d77"}]

The <tt>get-urls</tt> function takes a Factual ID and returns URLs that mention that place. For example:

	;; Return all URLs where the place identified by the specified Factual ID is mentioned
	(get-urls "97598010-433f-4946-8fd5-4a6dd1639d77")

Just as with <tt>fetch</tt>, these Crossref functions support the options that the Factual API supports, and they also provide metadata. For example:

	> (meta (get-urls "97598010-433f-4946-8fd5-4a6dd1639d77" :limit 12))
	{:total_row_count 66, :included_rows 12, :version 3, :status "ok"}

## Tying Things Together

Let's create a simple function that finds cafes close to a geolocation:

	(defn nearby-cafes
	  "Returns up to 12 cafes within 5000 meters of the specified location."
	  [lat lon]
	  (fetch :places
	         :q "cafe"
	         :filters {:category {:$eq "Food & Beverage"}}
	         :geo {:$circle {:$center [lat lon]
	                         :$meters 5000}}
	         :include_count true
	         :limit 12))

An example usage:

	> (def cafes (nearby-cafes 34.06018 -118.41835))

Let's peek at the metadata:

	> (meta cafes)
	{:total_row_count 26, :included_rows 12, :version 3, :status "ok"}

Ok, we got back a full 12 results, and note that there's actually a total of 26 cafes near us. Let's take a look at a few of the cafes we got back:

	> (map :name (take 3 cafes))
	("Aroma Cafe" "Cafe Connection" "Panini Cafe")

That first one, "Aroma Cafe", sounds interesting. Let's see the details:

	> (first cafes)
	{:status "1", :country "US", :longitude -118.423421, :factual_id "eb67e10b-b103-41be-8bb5-e077855b7ae7", :name "Aroma Cafe", :postcode "90064", :locality "Los Angeles", :latitude 34.039792, :region "CA", :address "2530 Overland Ave", :website "http://aromacafe-la.com/", :tel "(310) 836-2919", :category "Food & Beverage"}

So I wonder what Yelp has to say about this place. Let's use Crosswalk to find out. Note that we use Aroma Cafe's :factual_id from the above results...

	> (crosswalk :factual_id "eb67e10b-b103-41be-8bb5-e077855b7ae7" :only "yelp")
	[{:url "http://www.yelp.com/biz/aroma-cafe-los-angeles", :factual_id "eb67e10b-b103-41be-8bb5-e077855b7ae7", :namespace_id "AmtMwS2wCbr3l-_S0d9AoQ", :namespace "yelp"}]

That gives me the yelp URL for the Aroma Cafe, should I wish to read up on it. 

Now, Factual supports other Crosswalked sources besides Yelp. These are the :namespace key value pairs in the results from the crosswalk function. So let's find out what namespaces are available for the Aroma Cafe:

	> (map :namespace (crosswalk :factual_id "eb67e10b-b103-41be-8bb5-e077855b7ae7"))
	("menupages" "manta" "loopt" "urbanspoon" "yp" "gowalla" "yellowbook" "insiderpages" "chow" "merchantcircle" "citysearch" "yahoolocal" "yelp" "urbanspoon" "yp" "allmenus" "menupages" "simplegeo" "foursquare")

You know what might be cool is to create a function that takes a :factual_id and returns a hashamp of each valid namespaces to the associated Crosswalked URL:

	(defn namespaces->urls [factid]
	  (let [crosswalks (crosswalk :factual_id factid)]
	    (reduce
	     #(assoc %1 (:namespace %2) (:url %2))
	     {}
	     crosswalks)))

So now we can do this:

	> (namespaces->urls "eb67e10b-b103-41be-8bb5-e077855b7ae7")
	{"foursquare" "https://foursquare.com/venue/38146", 
	 "menupages" "http://losangeles.menupages.com/restaurants/the-westside-city/", 
	 "manta" "http://www.manta.com/c/mmcw5s5/aroma-cafe", 
	 "yahoolocal" "http://local.yahoo.com/info-20400708-aroma-cafe-los-angeles",
	 ... }

## License

TODO