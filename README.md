# funnyplaces

funnyplaces is an experimental Clojure based cient for Factual's API. It currently supports a basic fetch function, which allows you to run rich queries against Factual's datasets, including their Places data.

## Basic Usage

Establish credentials:

	> (factual! "YOUR_FACTUAL_KEY" "YOUR_FACTUAL_SECRET")

Now you can use the <tt>fetch</tt> function to run queries against Factual's tables:

	;; Fetch 3 Places from Factual
	>  (fetch :places :limit 3)

<tt>fetch</tt> takes the table name as the first argument, then a list of option pairs. It returns a hashmap following Factual's API spec.

Let's rerun the previous query and pull from the results the list of the place names:

	> (def res (fetch :places :limit 3))
	> (def places (get-in res [:response :data]))
	> (map :name places)
	("Lorillard Tobacco Co." "Imediahouse" "El Monte Wholesale Meats")

Note that we use <tt>get-in</tt> to get to our row data, because Factual returns a nested structure containing more than just data rows.

A few more examples:

	;; Return rows where region equals "CA"
	(fetch :places :filters {"region" "CA"})

	;; Return rows where name begins with "Starbucks" and return both the data and a total count of the matched rows:
	(fetch :places :filters {"name" {"$bw" "Starbucks"}} :include_count true)

Let's use the previous example and build a function that tells us how many Starbucks exist in Factual's Places data:

	(defn count-starbucks []
	  (let [res
	        (fetch :places
	               :filters {"name" {"$bw" "Starbucks"}}
	               :include_count true)]
	    (get-in res [:response :total_row_count])))

Note that we used <tt>get-in</tt> again, but this time we drill down to the <tt>:total_row_count</tt> metadata we want, rather than the data rows.

Running this function looks like:

	> (count-starbucks)
	8751

## License

TODO