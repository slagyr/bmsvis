(defproject bmsvis "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main bmsvis.core
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520" :exclusions [com.google.errorprone/error_prone_annotations com.google.code.findbugs/jsr305]]
                 [org.clojure/core.async "0.4.500"]
                 [reagent "0.8.1"]
                 [cljsjs/react-chartjs-2 "2.7.4-0"]
                 [cljsjs/react-with-addons "0.12.2-7"]]

  :profiles {:dev {:dependencies   [[speclj "3.3.2"]]
                   :resource-paths ["resources" "dev"]}}

  :plugins [[speclj "3.3.2"]
            [lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]]

  :source-paths ["src"]
  :test-paths ["spec"]
  :clean-targets ^{:protect false} [:target-path "public/js"]


  :cljsbuild {:builds        {:dev  {:main           bmsvis.core
                                     :source-paths   ["spec" "src"]
                                     :compiler       {:output-to     "public/js/bmsvis.js"
                                                      :optimizations :whitespace
                                                      :pretty-print  true}
                                     :notify-command ["bin/speclj.js" "public/js/bmsvis.js"]}

                              :prod {:source-paths ["src"]
                                     :compiler     {:output-to     "public/js/bmsvis.js"
                                                    :optimizations :advanced}}}
              :test-commands {"test" ["bin/speclj.js" "public/js/bmsvis.js"]}}

  :aliases {"cljs" ["do" "clean," "cljsbuild" "once" "prod"]
            "specljs" ["do" "clean," "cljsbuild" "auto" "dev"]}
  )
