(defproject funnyplaces "1.2.0"
  :url "http://github.com/dirtyvagabond/Funnyplaces"
  :description "An experimental Clojure client library for Factual's Places API"
  :dependencies [
    [com.google.api.client/google-api-client "1.4.1-beta"]
    [com.google.guava/guava "r09"]
    [org.clojure/clojure "1.4.0-alpha2"]
    [org.clojure/data.json "0.1.1"]
    [slingshot "0.9.0"]
  ]

  :dev-dependencies [
    [swank-clojure "1.4.0-SNAPSHOT"]
    [lein-clojars "0.6.0"]
  ]
)
