(ns funnyplaces.api
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http.javanet NetHttpTransport))
  (:use [clojure.contrib.duck-streams :only [slurp*]])
  (:use [clojure.contrib.def :only [defnk]])
  (:use [clojure.contrib.json])
  (:use [clojure.contrib.string :only [join as-str]])
  (:import (com.google.api.client.http GenericUrl)))
  

(def *factual-config* {})

(def *base-url* "http://api.v3.factual.com/")

(defn factual!
  [client-key client-secret]
  (def *factual-config* {:key client-key :secret client-secret}))

(defn make-params
  "Returns configured OAuth params for the specified request"
  [url method]
  (let [signer (OAuthHmacSigner.)
        params (OAuthParameters.)]
    (set! (. params consumerKey) (:key *factual-config*))
    (doto params
      (.computeNonce)
      (.computeTimestamp))
    (set! (. signer clientSharedSecret) (:secret *factual-config*))
    (set! (. params signer) signer)
    (.computeSignature params method url)
    params))

(defn make-req [url]
  (.buildGetRequest 
    (.createRequestFactory
      (NetHttpTransport.)
      (make-params url "GET"))
    url))

(defn resp-body
  "Returns the response body of the specified request."
  [url-str]
  (slurp* (.getContent (.execute (make-req (GenericUrl. url-str))))))

(defn make-url-str [path & opts]
  (str *base-url* path "?"
       (join "&" (map #(as-str (key %) "=" (val %)) (first opts)))))

(defn get-hashmap [url]
  (read-json (resp-body url)))

(defn fetch [table & {:keys [limit]
                     :or {limit 10}
                     :as opts}]
  (let [url (make-url-str (str "t/" (as-str table)) opts)
        resp (get-hashmap url)]
    (get-in resp [:response :data])))

(defn crosswalk [] ;;{:keys [factual-id only namespace namespace-id]}
  (get-hashmap (make-url-str "places/crosswalk" {:factual_id "97598010-433f-4946-8fd5-4a6dd1639d77"})))
