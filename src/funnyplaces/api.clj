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

(def *base-url* "http://api.v3.factual.com/")

(defn factual!
  [client-key client-secret]
  (def *factual-config* {:key client-key :secret client-secret}))

(defn make-params
  "Returns configured OAuth params for the specified request"
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

(defn make-req [gurl]
  (.buildGetRequest 
    (.createRequestFactory
      (NetHttpTransport.)
      (make-params gurl "GET"))
    gurl))

(defn get-resp [gurl]
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

(defn make-gurl
  "Builds a GenericUrl pointing to the given path on Factual's API.
   opts should be a hashmap with all desired query parameters for
   the resulting url. Values in opts should be primitives or hashmaps;
   they will be coerced to the proper json string representation for
   inclusion in the url query string."
  [path opts]
  (doto
    (GenericUrl. (str *base-url* path))
    (.putAll (coerce opts))))

(defn do-meta [res]
  (let [data (get-in res [:response :data])]
    (with-meta data (merge
                     (dissoc res :response)
                     {:response (dissoc (:response res) :data)}))))

(defn stone
  "Given an HttpResponseException, returns a hashmap representing
   the error response, which can be thrown by slingshot."
  [hre]
  (let [res (. hre response)
        status (. res statusCode)
        msg (. res statusMessage)]
    (throw+ {:status status :message msg})))

(defn get-results
  "Executes the specified query and returns the results.
   The returned results will have metadata associated with it,
   built from the results metadata returned by Factual."
  ([gurl]
     (try
       (do-meta (read-json (get-resp gurl)))
       (catch HttpResponseException hre
         (throw+ (stone hre)))))
  ([path opts]
     (get-results (make-gurl path opts))))

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