(ns boxplot-clj.core
  (:require [clojure.string :as string]
            [com.stuartsierra.frequencies :as freqs])
  (:import (java.math RoundingMode)))


(defn- place!
  "Center `substr` `percent` of the way through `buf`, moving it forward (to the
  right) until it is at least one space away from any previous strings to the
  left. Buffers should therefore start out with spaces and have `place!` called
  from left to right."
  [^StringBuilder buf substr percent]
  (letfn [(occupied? [idx]
            (and (< 0 idx (.length buf))
                 (not= (.charAt buf idx) \space)))]
    (let [start (int (* (- (.length buf) (count substr)) percent))
          ;; Shift the starting point over if we'd be overwriting something,
          ;; and give a margin of one space.
          start' (as->
                   (->> (iterate inc start) (drop-while occupied?) first) start'
                   (if (occupied? (dec start'))
                     (inc start') start'))
          end (+ start' (count substr))]
      (when (> end (.length buf))
        (.setLength buf (inc end)))
      (.replace buf start' end substr))))


(defn- fill!
  "Fill `buf` with `fill-char` from `from-percent` of the way through `buf` to
  `to-percent`."
  [^StringBuilder buf fill-char from-percent to-percent]
  (letfn [(occupied? [idx]
            (and (< idx (.length buf))
                 (not= (.charAt buf idx) \space)))]
    (let [[start end] (map #(int (* (dec (.length buf)) %)) [from-percent to-percent])]
      (when (> end (.length buf))
        (.setLength buf (inc end)))
      (doseq [idx (range start end)]
        (when-not (occupied? idx)
          (.setCharAt buf idx fill-char))))))


(defn- box-plot-points
  "Given a set of numbers, return a vector of [min q1 median q3 max] values,
  with the given number of sig figs."
  [numbers sig-figs]
  (letfn [(conform [n]
            (with-precision sig-figs :rounding RoundingMode/HALF_EVEN
                                     (* 1M (bigdec n))))]
    (let [{:keys [percentiles] :as stats}
          (-> numbers frequencies freqs/stats)
          [min' q1 median q3 max'] (map conform [(:min stats)
                                                 (get percentiles 25)
                                                 (get percentiles 50)
                                                 (get percentiles 75)
                                                 (:max stats)])]
      [min' q1 median q3 max'])))


(defn box-plot
  "Generate a string box plot of the given set of `numbers` with the given
  line `width`.

  Options
    key       A function to extract the number to plot from each datum in
              `numbers`.
    sig-figs  The number of significant figures to show in the labels."
  [numbers width & {:keys [key sig-figs] :or {key identity
                                              sig-figs 2}}]
  (let [points (box-plot-points (map key numbers) sig-figs)

        ;; Figure out the percent intervals on the line to place the points
        domain (- (last points) (first points))
        [p0 p1 p2 p3 p4 :as percents]
        (map (fn [n]
               (with-precision sig-figs :rounding RoundingMode/HALF_EVEN
                                        (/ (- n (first points)) domain))) points)

        ;; The minimum line width is all the labels together w/ spaces
        min-width (transduce (comp (interpose " ") (map (comp count str)))
                             + points)

        ;; The buffers we'll use to build the text
        line (-> (max min-width width) (repeat " ") string/join)
        [l1 l2 l3 l4 :as lines] (repeatedly 4 #(StringBuilder. line))]

    ;; Put the '|' characters at proportional percentages of the line width
    ;; and put a label under each one with the value
    (doseq [[quartile percent] (map vector points percents)]
      (place! l2 "|" percent)
      (place! l4 (str quartile) percent))

    ;; Fill in the box
    (fill! l1 \_ p1 p3)
    (fill! l2 \- p0 p1)
    (fill! l2 \- p3 p4)
    (fill! l3 \¯ p1 p3)

    ;; Put together all the buffers
    (string/join "\n" (map #(.toString %) lines))))


(comment
  "Some examples"

  (-> (box-plot (repeatedly 100 rand) 50) println)
  ; =>
  ;            _________________________
  ;|-----------|         |              |-----------|
  ;            ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
  ;0.0076     0.26     0.45          0.76        0.99

  (-> (box-plot (repeatedly 100 rand) 50 :sig-figs 3) println)
  ;         _________________________
  ;|--------|              |         |--------------|
  ;         ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
  ;0.0144  0.206         0.497    0.699         0.997
  )
