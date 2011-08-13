(ns funnyplaces.api
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http.javanet NetHttpTransport))
  (:use [clojure.contrib.duck-streams :only [slurp*]])
  (:use [clojure.contrib.json])
  (:use [clojure.contrib.string :only [as-str]])
  (:import (com.google.api.client.http GenericUrl)))
  

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
   - keys are coerced to strings
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

(defn get-hashmap
  "Executes a get request to gurl, parses the json response, and returns
   the result as a hashmap."
  [gurl]
  (read-json (get-resp gurl)))

(defn fetch [table & {:as opts}]
  (let [gurl (make-gurl (str "t/" (as-str table)) opts)]
    (get-hashmap gurl)))

