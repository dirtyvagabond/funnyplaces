(ns funnyplaces.demo
  (:require [funnyplaces.api :as fun])
  (:import [funnyplaces.api funnyplaces-error])
  (:use [clojure.data.json :only (json-str read-json)])
  (:use [slingshot.slingshot]
        [clojure.pprint]))

(defn connect
  "Connects this demo namespace to Factual's API. You must put
   your key and secret in resources/oauth.json.
   See resources/oauth.sample.json for the expected format."
  []
  (let [auth (read-json (slurp "resources/oauth.json"))]
    (fun/factual! (:key auth) (:secret auth))))

(defn find-coloft []
  (first
   (fun/fetch :places :q "coloft")))

(defn nearby-cafes [lat lon miles]
  "Returns up to 50 cafes within specified miles of specified location."
  []
  (fun/fetch :places
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

(defn resolve-ino-cafe
  "Uses Factual's resolve feature to find entities that are potentially
   the one true Ino Cafe, based on a lat lon."
  []
  (fun/resolve {"name" "ino", "latitude" 40.73, "longitude" -74.01}))

(defn resolved-ino-cafe
  "Uses Factual's resolve feature to find the one true Ino Cafe,
   based on a lat lon."
  []
  (fun/resolved {"name" "ino", "latitude" 40.73, "longitude" -74.01}))

(defn crosswalk-stand
  "Runs basic crosswalk query for The Stand, Century City."
  []
  (fun/crosswalk :factual_id "97598010-433f-4946-8fd5-4a6dd1639d77"))

(defn deliver-dinner [lat lon]
  (fun/fetch :restaurants-us
             :filters {:meal_dinner {:$eq true}
                       :meal_deliver {:$eq true}}
             :geo {:$circle {:$center [lat lon]
                             :$meters 4500}}
             :sort :$distance))

(defn chiang-mai-open []
  (get-in (meta
           (fun/fetch :global
                      :include_count true
                      :filters {:country {:$eq "TH"}
                                :region {:$eq "Chiang Mai"}
                                :status {:$eq 1}
                                :tel {:$blank false}}))
          [:response :total_row_count]))

(defn demo-error
  "Illustrates how to catch an error thrown by a bad response, and inspect it.
   The funnyplaces-error record that results contains useful information, such as the
   status code, and the options you sent in to the query as a hash-map."
  []
  (try+
   (fun/fetch :places :filters {:factual_id "97598010-433f-4946-8fd5-4a6dd1639d77" :BAD :PARAM!})
   (catch funnyplaces-error {code :code message :message opts :opts}
     (println "Got bad resp code:" code)
     (println "Message:" message)
     (println "Opts:" opts))))
