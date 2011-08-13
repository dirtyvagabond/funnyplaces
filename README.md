# funnyplaces

funnyplaces is an experimental Clojure based cient for Factual's API. It currently supports a basic fetch function, which allows you to run rich queries against Factual's datasets, including their Places data.

## Basic Usage

First, establish your credentials. (You can get your key and secret from TODO)
	> (factual! "YOUR_FACTUAL_KEY" "YOUR_FACTUAL_SECRET")

Now you can use the <tt>fetch</tt> function to run queries against Factual's tables. Here's a simple example that fetches 10 arbitrary locations from Factual's Places table:
	>  (fetch :places :limit 3)

The first argument to <tt>fetch</tt> indicates the Factual table. After that, <tt>fetch</tt> takes pairs of query options.

The returned value is a hashmap built from Factual's json response. It follows the structure of Factual's specification here:
https://factual.onconfluence.com/display/docs/Core+API+-+Read
https://factual.onconfluence.com/display/docs/Core+API+-+Response+Handling

So, for example, let's get the results from the previous query and drill down to just a list of place names:
	> (def res (fetch :places :limit 3))
	> (def plcs (get-in res [:response :data]))
	> (map :name plcs)

The result of the last statement will look like:
	("Lorillard Tobacco Co." "Imediahouse" "El Monte Wholesale Meats")

Note that we use <tt>get-in</tt> to get to our row data, because Factual returns a nested structure containing more than just data rows.

Here are some more example fetches:

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