(defproject jcl2groovy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.0-alpha3"] [org.clojure/tools.cli "1.0.206"]]
  :main ^:skip-aot jcl2groovy.core
  :target-path "target/%s"
  :plugins [[lein-codox "0.10.8"] [lein-kibit "0.1.8"] [jonase/eastwood "1.2.3"] [com.github.clj-kondo/lein-clj-kondo "0.1.3"] [lein-count "1.0.9"]]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
