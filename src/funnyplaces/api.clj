(ns funnyplaces.api
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http.javanet NetHttpTransport))
  (:use [clojure.contrib.duck-streams :only [slurp*]])
  (:use [clojure.contrib.def :only [defnk]])
  (:use [clojure.contrib.json])
  (:import (com.google.api.client.http GenericUrl)))
  

(def *factual-config* {})

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

(defn make-req [url method]
  (.buildGetRequest 
    (.createRequestFactory
      (NetHttpTransport.)
      (make-params url method))
    url))  

(defn resp-body
  "Returns the response body of the specified request."
  [url method]
  (slurp* (.getContent (.execute (make-req url method)))))

(defnk places [:limit 10]
  (let [resp-map (read-json 
                   (resp-body (GenericUrl. (str "http://api.v3.factual.com/t/places?limit=" limit)) "GET"))]
    (get-in resp-map [:response :data])))

(defn crosswalk [] ;;{:keys [factual-id only namespace namespace-id]}
  (read-json (resp-body (GenericUrl. (str "http://api.v3.factual.com/t/places/crosswalk?factual_id=97598010-433f-4946-8fd5-4a6dd1639d77")) "GET")))

;;(factual! KEY SECRET)

;;; Make the request and print out each line of the response
;;(doseq [place (places)]
;; (println place))