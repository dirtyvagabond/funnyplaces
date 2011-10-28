(ns funnyplaces.api
  (:refer-clojure :exclude [resolve])
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http.javanet NetHttpTransport))
  (:use [clojure.contrib.duck-streams :only [slurp*]]
        [clojure.contrib.json]
        [clojure.contrib.string :only [as-str]]
        [slingshot.slingshot :only [throw+]])
  (:import (com.google.api.client.http GenericUrl HttpResponseException)))
  

(declare *factual-config*)

(defrecord bad-resp [code message opts])

(def *base-url* "http://api.v3.factual.com/")

(defn factual!
  [client-key client-secret]
  (def *factual-config* {:key client-key :secret client-secret}))

(defn make-params
  "Returns configured OAuth params for the specified request.
   gurl must be a GenericUrl."
  [gurl method]
  (let [signer (OAuthHmacSigner.)
        params (OAuthParameters.)]
    (set! (. params consumerKey) (:key *factual-config*))
    (doto params
      (.computeNonce)
      (.computeTimestamp))
    (set! (. signer clientSharedSecret) (:secret *factual-config*))
    (set! (. params signer) signer)
    (.computeSignature params method gurl)
    params))

(defn make-req
  "gurl must be a GenericUrl."
  [gurl]
  (.buildGetRequest 
    (.createRequestFactory
      (NetHttpTransport.)
      (make-params gurl "GET"))
    gurl))

(defn get-resp
  "gurl must be a GenericUrl."
  [gurl]
  (slurp* (.getContent (.execute (make-req gurl)))))

(defn coerce
  "Grooms the specified hashmap of query parameters for url inclusion.
   - keyword keys are coerced to strings
   - values are coerced to json string representations"
  [opts]
  (reduce
   #(assoc %1
      (as-str (key %2))
      (if (string? (val %2))
          (val %2)
          (json-str (val %2))))
   {} opts))

(defn make-gurl-map
  "Builds a GenericUrl pointing to the given path on Factual's API.
   opts should be a hashmap with all desired query parameters for
   the resulting url. Values in opts should be primitives or hashmaps;
   they will be coerced to the proper json string representation for
   inclusion in the url query string.

   Returns a hash-map that holds the GenericUrl (as :gurl), as well as
   the opts (as :opts). This is useful later for error handling, in order
   to include opts in the thrown error."
  [path opts]
  (let [gurl (GenericUrl. (str *base-url* path))]
    (.putAll gurl (coerce opts))
    {:gurl gurl :opts opts}))

(defn do-meta [res]
  (let [data (get-in res [:response :data])]
    (with-meta data (merge
                     (dissoc res :response)
                     {:response (dissoc (:response res) :data)}))))

(defn new-bad-resp
  "Given an HttpResponseException, returns a bad-resp record representing
   the error response, which can be thrown by slingshot."
  [hre gurl-map]
  (let [res (. hre response)
        code (. res statusCode)
        msg (. res statusMessage)
        opts (:opts gurl-map)]
    (throw+ (bad-resp. code msg opts))))

(defn get-results
  "Executes the specified query and returns the results.
   The returned results will have metadata associated with it,
   built from the results metadata returned by Factual.

   In the case of a bad response code, throws a bad-response record
   as a slingshot stone. The record will include any opts that were
   passed in by user code."
  ([gurl-map]
     (try
       (do-meta (read-json (get-resp (:gurl gurl-map))))
       (catch HttpResponseException hre
         (throw+ (new-bad-resp hre gurl-map)))))
  ([path opts]
     (get-results (make-gurl-map path opts))))

(defn fetch [table & {:as opts}]
  (get-results (str "t/" (as-str table)) opts))

(defn get-factid [url & {:as opts}]
  (let [opts (assoc opts :url url)]
    (get-results "places/crossref" opts)))

(defn get-urls [factid & {:as opts}]
  (let [opts (assoc opts :factual_id factid)]
    (get-results "places/crossref" opts)))

(defn crosswalk [& {:as opts}]
  (map #(update-in % [:namespace] keyword)
       (get-results "places/crosswalk" opts)))

(defn resolve [values]
  (get-results "places/resolve" {:values values}))

(defn resolved [values]
  (first (filter :resolved
                 (get-results "places/resolve" {:values values}))))