# boxplot-clj

String box plots for numeric data.


## Usage

```clojure
(defproject ;; ...
  :dependencies [[boxplot-clj "0.1.0"]])
```

```clojure
(require '[boxplot-clj.core :as box])

(box/box-plot (repeatedly 10000 rand) 50)
;; =>
;; "             _______________________              
;;  |-----------|           |           |------------|
;;               ¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯              
;;  0.000068   0.25        0.51       0.75         1.0"
```

## License

Distributed under the Eclipse Public License either version 1.0.
