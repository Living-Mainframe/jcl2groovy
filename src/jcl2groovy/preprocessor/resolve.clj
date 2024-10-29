(ns jcl2groovy.preprocessor.resolve
  (:gen-class))

(require '[clojure.java.io :as io])
(require '[clojure.string :as string])
(require '[jcl2groovy.utils :as jcl-utils])

(defn resolve-symbols
  "Resolves JCL symbols in the ``input``."
  [input]
  (loop [input input symbols (re-seq #"&[A-Z0-9$@#]+\.?" input)]
    (if (> (count symbols) 0)
      (let [replacement (format "${symbol.%s}" (jcl-utils/jcl-name-to-groovy-name (string/replace (first symbols) #"(&|\.)" "")))]
        (recur
         (string/replace input (first symbols) replacement)
         (rest symbols)))
      input)))

(defn resolve-include
  "Recursively resolve INCLUDE statements, looking for members in each directory in `paths`"
  [jcl-lines paths]

  (letfn [(load-member [member]
    (let [p
          (loop [i 0 p nil]
            (if (< i (count paths))
              (cond

                ;; check path/MEMBER.jcl
                (.exists (io/file (str (nth paths i) "/" member ".jcl")))
                (recur (count paths) (str (nth paths i) "/" member ".jcl"))

                ;; check path/member.jcl
                (.exists (io/file (str (nth paths i) "/" (string/lower-case member) ".jcl")))
                (recur (count paths) (str (nth paths i) "/" (string/lower-case member) ".jcl"))

                ;; check path/MEMBER
                (.exists (io/file (str (nth paths i) "/" member)))
                (recur (count paths) (str (nth paths i) "/" member))

                ;; check path/member
                (.exists (io/file (str (nth paths i) "/" (string/lower-case member))))
                (recur (count paths) (str (nth paths i) "/" (string/lower-case member)))

                :else
                (recur (+ 1 i) p))

              (if (nil? p) (throw (Exception. (str "Could not resolve INCLUDE statement for member " member))) p)))

          member-reader
          (io/reader p)

          member-lines
          (line-seq member-reader)]

      (resolve-include member-lines paths)
    ))]

    (let [include-re #"^//[A-Z0-9$#@]* +INCLUDE.+$" member-re #"MEMBER=[A-Z0-9$#@]+"]

      (loop [i 0 lines []]
        (if (< i (count jcl-lines))

          (let [line (nth jcl-lines i)]
            (cond
              ;; line is an INCLUDE statement
              (re-matches include-re line)
              (recur (+ 1 i) (into lines (load-member (string/replace (re-find member-re line) "MEMBER=" ""))))

              ;; line is a normal statement
              :else
              (recur (+ 1 i) (conj lines line))))

          lines
          )))))
