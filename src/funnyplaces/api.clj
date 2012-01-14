(ns funnyplaces.api
  (:refer-clojure :exclude [resolve])
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http.javanet NetHttpTransport))
  (:use [clojure.data.json :only (json-str read-json)])
  (:use [clojure.java.io :only (reader)])
  (:use [slingshot.slingshot :only [throw+]])
  (:import (com.google.api.client.http GenericUrl HttpResponseException HttpHeaders)))
  

(def DRIVER_VERSION_TAG "factual-clojure-driver-v1.2")

(declare ^:dynamic *factual-config*)

(defrecord funnyplaces-error [code message opts])

(def ^:dynamic *base-url* "http://api.v3.factual.com/")

(defn factual!
  [key secret]
  (def ^:dynamic *factual-config* {:key key :secret secret}))

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
  (let [params (make-params gurl "GET")
        factory (.createRequestFactory (NetHttpTransport.) params)
        req (.buildGetRequest factory gurl)
        heads (HttpHeaders.)]
    (.set heads "X-Factual-Lib" DRIVER_VERSION_TAG)
    (set! (. req headers) heads)
    req))

(defn get-resp
  "gurl must be a GenericUrl."
  [gurl]
  (slurp (reader (.getContent (.execute (make-req gurl))))))

(defn make-gurl-map
  "Builds a GenericUrl pointing to the given path on Factual's API,
   and including opts as key value parameters in the query string.

   opts should be a hashmap with all desired query parameters for
   the resulting url. Values in opts should be primitives or hash-maps;
   they will be coerced to the proper json string representation for
   inclusion in the url query string.

   Returns a hash-map that holds the GenericUrl (as :gurl), as well as
   the original opts (as :opts). This is useful later for error
   handling, in order to include opts in the thrown error."
  [path opts]
  (let [gurl (GenericUrl. (str *base-url* path))]
    (doseq [[k v] opts]
      (.put gurl
        ;; query param name    
        (name k)
        ;; query param value
        (if (or (keyword? v) (string? v))
          (name v)
          (json-str v))))
    {:gurl gurl :opts opts}))

(defn do-meta [res]
  (let [data (or
               ;; standard result set
               (get-in res [:response :data])
               ;; schema result
               (get-in res [:response :view :fields]))]
    (with-meta data (merge
                     (dissoc res :response)
                     {:response (dissoc (:response res) :data)}))))

(defn new-error
  "Given an HttpResponseException, returns a funnyplaces-error record representing
   the error response, which includes things like status code, status message, as
   well as the original opts used to create the request."
  [hre gurl-map]
  (let [res (. hre response)
        code (. res statusCode)
        msg (. res statusMessage)
        opts (:opts gurl-map)]
    (funnyplaces-error. code msg opts)))

(defn get-results
  "Executes the specified query and returns the results.
   The returned results will have metadata associated with it,
   built from the results metadata returned by Factual.

   In the case of a bad response code, throws a funnyplaces-error record
   as a slingshot stone. The record will include any opts that were
   passed in by user code."
  ([gurl-map]
     (try
       (do-meta (read-json (get-resp (:gurl gurl-map))))
       (catch HttpResponseException hre
         (throw+ (new-error hre gurl-map)))))
  ([path opts]
     (get-results (make-gurl-map path opts))))

(defn fetch
  "Runs a fetch request against Factual and returns the results.
   table should be a valid table name, such as :global. The query
   is specified with the rest of the args. Examples:

   (fetch :restaurants-us)

   (fetch :places :q \"cafe\")

   (fetch :restaurants-us
     :q \"cafe\"
     :offset 20
     :limit 10
     :filters {:name {:$bw \"starbucks\" :locality {:$eq \"los angeles\"}}))"
  [table & {:as opts}]
  (get-results (str "t/" (name table)) opts))

(defn fetch-q
  "Uses table and query to build the proper collection of
   arguments to fun/fetch, then applies, thereby running the
   specified query against Factual."
  [table query]
  (let [args (reduce (fn [col [k v]]
                       (conj col k v))
                     [table]
                     query)]
    (apply fetch args)))

(defn schema [table]
  (get-results (str "t/" (name table) "/schema") []))

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
