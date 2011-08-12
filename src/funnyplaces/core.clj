(ns funnyplaces.core
  (:import (java.io BufferedReader IOException InputStreamReader))
  (:import (com.google.api.client.auth.oauth OAuthHmacSigner OAuthParameters))
  (:import (com.google.api.client.http GenericUrl HttpRequest HttpRequestFactory HttpResponse HttpTransport))
  (:import (com.google.api.client.http.javanet NetHttpTransport)))
  
(def *consumer-key* "YOUR_KEY")
(def *consumer-secret* "YOUR_SECRET")
(def *api-request* "http://api.v3.factual.com/t/places?limit=10")

(def *url* (GenericUrl. *api-request*))
(def *method* "GET")

(def *signer* (OAuthHmacSigner.))
(set! (. *signer* clientSharedSecret) *consumer-secret*)

(def *params* (OAuthParameters.))
(set! (. *params* consumerKey) *consumer-key*)
(doto *params*
  (.computeNonce)
  (.computeTimestamp))
(set! (. *params* signer) *signer*)
(.computeSignature *params* *method* *url*)

(def *transport* (NetHttpTransport.))
(def *f* (.createRequestFactory *transport* *params*))

(def *request* (.buildGetRequest *f* *url*))

(def *response* (.execute *request*))

(with-open [stream (.getContent *response*)]
     (let  [buf (BufferedReader. 
                 (InputStreamReader. stream))]
       (doseq [line (line-seq buf)]
         (println line))))