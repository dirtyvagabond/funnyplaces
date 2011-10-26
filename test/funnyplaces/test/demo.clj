(ns funnyplaces.demo
  (use funnyplaces.api)
  (:use [clojure.contrib.json])
  (:use [clojure.contrib.pprint]))

(defn connect
  "Connects this demo namespace to Factual's API. You must put
   your key and secret in resources/oauth.json.
   See resources/oauth.sample.json for the expected format."
  []
  (let [auth (read-json (slurp "resources/oauth.json"))]
    (factual! (:key auth) (:secret auth))))

;;; (fetch :places :q "coloft")

(defn find-coloft []
  (first
   (fetch :places :q "coloft")))

(defn nearby-cafes [lat lon miles]
  "Returns up to 50 cafes within specified miles of specified location."
  []
  (fetch :places
         :q "cafe"
         :filters {:category {:$eq "Food & Beverage"}}
         :geo {:$circle {:$center [lat lon]
                         :$meters (* miles 1609.344)}}
         :include_count true
         :limit 50))

(defn cafes-near [place miles]
  (let [lat (:latitude place)
        lon (:longitude place)]
    (nearby-cafes lat lon miles)))
;;; Above query is just a simple structure of hash-maps and vectors.
;;; Maps very nicely from API JSON query specs; even a little more concise.
;;; No OOP

;;; (def cross (crosswalk :factual_id (:factual_id coloft)))

;;; (map :name cafes)

;;; (map :namespace cross)