(ns funnyplaces.api
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http.javanet NetHttpTransport))
  (:use [clojure.contrib.duck-streams :only [slurp*]])
  (:use [clojure.contrib.def :only [defnk]])
  (:use [clojure.contrib.json])
  (:use [clojure.contrib.string :only [join as-str]])
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

(defn make-gurl [path opts]
  (println "make-gurl opts:" opts)
  (doto
    (GenericUrl. (str *base-url* path))
    (.putAll opts)))

(defn get-hashmap [gurl]
  (read-json (get-resp gurl)))

(defn coerce-opts
  "Grooms the specified hashmap of query parameters for url inclusion.
   - keys are coerced to strings
   - values are turned into their json string representation"
  [opts]
  (reduce #(assoc %1 (as-str (key %2)) (json-str (val %2))) {} opts))

(defn fetch [table & {:keys [limit]
                      :or {limit 10}
                      :as opts}]
  (let [gurl (make-gurl (str "t/" (as-str table)) (coerce-opts opts))
        _ (println "fetch gurl:" (str gurl))]
    (get-hashmap gurl)))