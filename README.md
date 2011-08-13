# funnyplaces

funnyplaces is an experimental Clojure based cient for Factual's API. It currently supports a basic fetch function, which allows you to run rich queries against Factual's datasets, including their Places data.

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

You can get at this metadata by using Clojure's meta function on the result you get back from a fetch. For example:

	> (def starbucks (fetch :places :filters {:name {:$bw "Starbucks"}} :include_count true))
	> (meta starbucks)
	{:total_row_count 8751, :included_rows 20, :version 3, :status "ok"}

## Places Usage

	;; Return rows with a name equal to "Stand" within 5000 meters of the specified lat/lng
	(fetch :places
	       :filters {:name "Stand"}
	       :geo {"$circle" {"$center" [34.06018, -118.41835] "$meters" 5000}})

## License

TODO