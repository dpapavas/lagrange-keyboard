;; -*- coding: utf-8 -*-

;; Copyright 2020 Dimitris Papavasiliou

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <https://www.gnu.org/licenses/>.

(ns lagrange-keyboard.core
  (:gen-class)
  (:refer-clojure :exclude [use import])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :refer [starts-with?]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]))

(def π Math/PI)
(defn degrees [& θ] (let [f (partial * π 1/180)]
                      (if (> (count θ) 1)
                        (mapv f θ)
                        (f (first θ)))))

;; General parameters.

(def place-keycaps? false)
(def keys-depressed? false)
(def place-keyswitches? false)
(def place-pcb? false)
(def draft? true)
(def mock-threads? true)
(def case-test-build? false)
(def case-test-locations [0])
(def case-test-volume [50 50 150, 0 0 0])
(def case-color [0.70588 0.69804 0.67059])
(def interference-test-build? false)
(def thumb-test-build? false)
(def key-test-build? false)
(def key-test-range [1 2, 4 4])

;; Main section parameters.

(def row-count 5)
(def column-count 6)

;; The radius, in mm, for each column, or for each row.

(def row-radius 235)
(def column-radius #(case %
                      (0 1) 65
                      2 69
                      3 66
                      55))

;; The spacing between rows, in mm, for each column and between
;; successive columns.

(def row-spacing #(case %
                    (0 1) 7/2
                    2 13/4
                    3 13/4
                    9/2))

(def column-spacing #(case %
                       0 2
                       1 0
                       2 9/2
                       3 5
                       3))

;; Column and row rotations in units of keys.

(def row-phase (constantly -8))
(def column-phase (fn [i] (+ -57/25 (cond
                                      (= i 2) -1/4
                                      (= i 3) -1/8
                                      (> i 3) -1/4
                                      :else 0))))

;; Column offsets, in mm, in the Y and Z axes.

(def column-offset #(case %
                      (0 1) 0
                      2 8
                      3 0
                      -14))

(def column-height #(case %
                      (0 1) 0
                      2 -5
                      3 0
                      7))

;; The key scale of the outer columns.

(def last-column-scale 3/2)

;; Palm key location tuning offsets, in mm.

(def palm-key-offset [0 -1 -3])

;; Key plate (i.e. switch mount) parameters.

(def keycap-length (* 0.725 25.4))  ; This is essentially 1u in mm.
(def plate-size keycap-length)
(def plate-thickness 3)             ; Units of mm.
(def plate-hole-size 14)

;; Place nubs at the switch mount hole edges, meant to engage the tabs
;; on the switches.

(def place-nub? not)   ; Which side to place a nub on.  Try even?, odd?, or identity.
(def nub-width 1/3)    ; How much the nub sticks out into the hole.
(def nub-height 1.5)   ; The thickness of the edge of the nub that engages the switch.

;; Thumb section parameters.

(def thumb-offset [5 -12 11])  ; Tuning offsets for the whole thumb section, in mm.

(def thumb-radius 68)
(def thumb-slant 0.85)         ; The degree of downward slant.
(def thumb-key-scale 5/4)      ; The scale of the top-inner key of the thumb cluster.

;; Per-key phase along the baseline arc, in units of keys, as a pair
;; of numbers: initial phase and per-key phase increment.

(defn thumb-key-phase [column row]
  (case row
    (1 2) (degrees -37/2 111/2)
    (degrees (if (zero? column) 12 10) 55/2)))

;; Per-key offset from the baseline arc, in mm.

(defn thumb-key-offset [column row]
  (case row
    1 [0 -20 -8]
    2 [0 -42 -16]
    (case column
      0 [0 (* keycap-length 1/2 (- 3/2 thumb-key-scale)) 0]
      3 [0 -4 0]
      [0 0 0])))

;; Per-key vertical slope.

(defn thumb-key-slope [column row]
  (case row
    1 (degrees 33)
    2 (degrees 6)
    0))

;; Height offset for the whole keyboard.

(def global-z-offset 0)

;; Case-related parameters.

(def ^:dynamic wall-thickness 3.2) ; Should probably be a multiple of
                                   ; nozzle/line size, if printed with
                                   ; walls only.


;; Screw bosses

;; Each boss is a sequence of two key coordinates and, optionally, a
;; pair of parameters.  The key coordinates are ultimately passed to
;; key-place; see comments there for their meaning. The boss is placed
;; at a point on the sidewall, between the two key locations.  By
;; default it's placed halfway between the given points and inset just
;; enough to clear the outer wall, but this can be tuned via the
;; optional parameters.

(def screw-bosses [[[:key, 5 3, 1 -1]      ; Right side
                    [:key, 5 3, 1 1]
                    [5/8 1]]

                   [[:key, 5 0, 1 1]
                    [:key, 5 0, 1 -1]]

                   [[:key, 4 0, 0 1]       ; Top side
                    [:key, 4 0, 1/2 1]]

                   [[:key, 2 0, 0 1]
                    [:key, 2 0, 1/2 1]]

                   [[:key, 0 0, 0 1]
                    [:key, 0 0, -1/2 1]]

                   [[:key, 0 1, -1 -1]     ; Left side
                    [:key, 0 2, -1 1]]

                   [[:key, 0 3, -1 -1]
                    [:thumb, 1 0, -1 1]]

                   [[:thumb, 2 1, -1 1]    ; Front side
                    [:thumb, 2 1, -1 -1]]

                   [[:thumb, 0 1, 1 1]
                    [:thumb, 0 1, 1 -1]]

                   [[:key, 4 3, 0 -1]
                    [:key, 4 3, -1 -1]]])

(def ^:dynamic screw-boss-radius 11/2)
(def screw-boss-height 8)

;; Bottom cover parameters.

(def cover-thickness 4)
(def cover-countersink-diameter 9/2)       ; Inner diameter of countersunk hole.
(def cover-countersink-height 2.4)         ; Head height.
(def cover-countersink-angle (degrees 90)) ; Head angle.
(def cover-fastener-thread [6.5 0.75 8])   ; Cover mount thread diameter, pitch
                                           ; and length in mm.

;; Stand parameters.

;; The stand is essentially formed by a rotational extrusion of the
;; bottom cover polygon.  To save on material, the inner portion is
;; removed, leaving a strip along the outside, parts of which (the
;; inner, taller tented sections) are also removed in turn, leaving
;; arm-like protrusions which are enough to stabilize the keyboard.
;; These arms can optionally be shortened to save on desktop
;; real estate, although this should be avoided, unless a bridge is
;; installed.

(def stand-tenting-angle (degrees 35))   ; The angle of extrusion.
(def stand-shape-factor 0)               ; 0 is radial extrusion, 1 is projection.
(def stand-width 25/2)                   ; The width of the strip forming the stand cross-section.
(def stand-minimum-thickness [6 4 4/10]) ; Thickness at inner bottom, inner top and outer bottom.
(def stand-boss-indexes [0 3, 1 8])      ; The screw bosses shared by the stand.
(def stand-split-points [15 51])         ; The locations where the arms begin.
(def stand-arm-lengths [3 11, 2 6])      ; Hom much to shorten the arms.
(def stand-arm-slope (degrees 13))

;; Stand boot parameters.

;; The wall thickness is given as a pair of offsets from the stand
;; walls.  The total wall thickness is thus the difference of the two
;; and the second number can be used to inset the inner boot wall, to
;; account for material removed from the stand during sanding,
;; printing tolerances, etc.

(def boot-wall-thickness [11/20 -5/20])
(def boot-wall-height 2)            ; The height of the sidewalls.
(def boot-bottom-thickness 1)       ; The thickness of the boot floor.

;; Bridge parameters.

(def bridge-arch-radius 6)
(def bridge-boss-indexes [4 5 6])   ; The mating boss indexes for the mount link.
(def bridge-bore-expansion 1/5)     ; How much to expand the diameter of bearing and link bores.
(def bridge-bearing-separation -10) ; Shortens the separation of the bridge bearings.

;; The thread diameter of the bridge links also determines the
;; geometry of the rod ends.  The first value below determines the
;; nominal diameter, on which the rod end geometry is based, while the
;; second value allows increasing the actual thread diameter above the
;; nominal, to adjust the thread fit (loose or tight), while keeping
;; the rest of the geometry constant.  The third value is the thread
;; pitch.

(def bridge-link-thread [6 1/2 1])
(def bridge-link-diameter 11)           ; Outer diameter of the linkage bars.

;; Link rod end specification as (type, thread length, range).  The
;; type can be :fixed or :rotating.  The latter type allows adjustment
;; in place, but backlash in the rotational joint makes the linkage
;; less rigid, allowing some motion between the two halves, which can
;; be annoying.  The range adjusts the spacing of the pin holes and
;; with it the approximate range of rotation (past the 90° point)
;; before interference with the mating part prohibits further motion.

(def bridge-separation-fork [:fixed 55 (degrees 10)])
(def bridge-toe-fork [:fixed 17 (degrees 10)])
(def bridge-toe-eye [:fixed 7 (degrees 20)])

(def bridge-bracket-size [18 23])       ; Cable bracket aperture size.

;; Controller PCB parameters.

(def pcb-position [-108 32.5 11])       ; PCB mount location.
(def pcb-fastener-thread [5 0.5 6])     ; PCB mount thread diameter,
                                        ; pitch and length in mm.

;; Printable keycap parameters.

(def keycap-family-common [:fn-value (if draft? nil 500)
                           :thickness [9/5 9/5]        ; Cap shell width [base top]
                           :mount-cross-length 4.1     ; Mount cross arm length
                           :mount-cross-width 1.19     ; Mount cross arm width
                           :mount-radius 2.75])        ; Mount stem radius

;; SA family row 3 keycap. See:
;; https://pimpmykeyboard.com/template/images/SAFamily.pdf

(def keycap-family-sa (into keycap-family-common [:height (* 0.462 25.4) :radius 33.35]))
(def keycap-family-sa-concave (into keycap-family-sa [:top :spherical-concave
                                                      :corner-rounding 0.179]))
(def keycap-family-sa-saddle (into keycap-family-sa
                                   [:top :saddle
                                    :corner-rounding 0.145 :extra-height 3/2
                                    :saddle-aspect 2 :saddle-skew [-6/10 -8/10]]))

;; DSA family keycap. See:
;; https://pimpmykeyboard.com/template/images/DSAFamily.pdf

(def keycap-family-dsa (into keycap-family-common [:height (* 0.291 25.4) :extra-height 1/2
                                                   :base-height 1 :radius 36.5
                                                   :mount-recess 6/5]))
(def keycap-family-dsa-concave (into keycap-family-dsa [:top :spherical-concave
                                                        :corner-rounding 0.446]))
(def keycap-family-dsa-convex (into keycap-family-dsa [:top :spherical-convex
                                                       :corner-rounding 0.536]))
(def keycap-family-dsa-fanged (into keycap-family-dsa-convex
                                    [:mount-offset [-0.025 0]
                                     :fang-corner-rounding 0.61 :fang-skew 3/2
                                     :fang-angle (degrees 54)]))

;; An OEM-like keycap, at least functionally (meaning
;; cylindrical top of typical OEM radius and height
;; somewhere between OEM R3 and R4)

(def keycap-family-oem (into keycap-family-common [:top :cylindrical
                                                   :height 15/2 :radius 26
                                                   :thickness [6/5 9/5]]))
(def keycap-family-oem-flush (into keycap-family-oem [:corner-rounding 0.343]))
(def keycap-family-oem-recessed (into keycap-family-oem [:corner-rounding 0.2
                                                         :mount-recess 5/2
                                                         :extra-height 5/2]))
;; Route out a keycap legend on the top of the keycap.  Must be set to
;; a [character font-family font-size] triplet.  The official Lagrange
;; logo keycap was created with ["\\U01D4DB" "Latin Modern Math" 11].

(def keycap-legend nil)

;; Define some utility functions.

(defn one-over-norm [v]
  (Math/pow (reduce + (map * v v)) -1/2))

(defn line-normal [a b]
  (let [[x y] (map - a b)]
    [(- y) x]))

;; Derive some utility variables and set global settings.

(def columns (lazy-seq
              (apply range (cond
                             (or thumb-test-build?
                                 key-test-build?)  [(max 0 (key-test-range 0))
                                                    (min column-count (inc (key-test-range 2)))]
                             :else [0 column-count]))))

(def rows (lazy-seq
           (apply range (cond
                          (or thumb-test-build?
                              key-test-build?) [(max 0 (key-test-range 1))
                                                (min row-count (inc (key-test-range 3)))]
                          :else [0 row-count]))))


(defn row [i] (if (neg? i) (+ row-count i) i))
(defn column [i] (if (neg? i) (+ column-count i) i))

(defn place-key-at? [[i j]]
  (and ((set columns) i)
       ((set rows) j)
       (or (#{2 3 (column -1)} i)
           (< j (row -1)))))

(defn key-scale-at [where, i j]
  (if (= where :thumb)
    [1 (case [i j]
         [0 0] thumb-key-scale
         ([1 0] [2 0]) 3/2
         1)]

    [(if (= i (column -1)) last-column-scale 1) 1]))

(defn key-options-at [where, i j]
  (let [WAN [0.90980 0.90588 0.89020]
        VAT [0.57647 0.76078 0.27843]
        GQC [0.63137 0.61569 0.56863]
        GAH [0.50588 0.50588 0.49412]
        BFV [0.36471 0.80392 0.89020]
        BBJ [0.00000 0.56078 0.69020]
        BBQ [0.00000 0.65098 0.70588]
        BO  [0.00000 0.29804 0.49804]
        RBD [0.82745 0.09804 0.16078]
        RAR [0.79608 0.18431 0.16471]]

    [(cond                              ; keycap-family
       (= [where, i j] [:thumb 0 1]) keycap-family-dsa-fanged
       (= [where, j] [:thumb, 0]) keycap-family-dsa-convex
       (= where :thumb) keycap-family-dsa-concave

       (= [where, i j] [:main, (column -1) (row -1)]) keycap-family-sa-saddle

       :else keycap-family-oem-flush)

     (if (= where :thumb)               ; color
       (case j
         0 WAN
         WAN)

       (cond
         (= [i j] [(column -1) (row -1)]) RAR
         (= i 5) GQC
         :else WAN))]))

(defn scale-to-key [where, i j, x y z]
  ;; Scale normalized plate coordinates (i.e. in [-1 1]) to world
  ;; space.  We stretch recessed (w.r.t their neighbors) columns in
  ;; the main section, as well as certain keys in the thumb section,
  ;; to allow more room for keycaps.

  (let [stretch (if (not= where :thumb)
                  (cond
                    (or (and (= i 2) (not= j (row -1)))
                        (and (= i 3) (pos? x))) [6/5 1 1]
                    :else [1 1 1])

                  (cond
                    (and (= [i j] [0 0]) (neg? x) (pos? y)) [1 1.1 1]
                    (and (= [i j] [1 0]) (pos? y)) [1 1.02 1]
                    (and (= [i j] [3 0]) (neg? y)) [1 17/16 1]
                    :else [1 1 1]))]
    (map *
         [x y z]
         stretch
         (conj (mapv (partial * 1/2) (key-scale-at where, i j)) 1/2)
         [plate-size plate-size plate-thickness])))

(defn thread [D_maj P L & [L_extra]]
  (if (or draft? mock-threads?)
    (let [r (/ D_maj 2)]
      (union
       (cylinder r L :center false)
       (translate [0 0 L] (cylinder [r 0] (or L_extra r) :center false))))

    ;; Match the *fs* setting, which is, by definition:
    ;;
    ;; *fs* = δθ * r = δθ * D_Maj / 2 = π * D_Maj / (2 * a)
    ;;
    ;; Therefore: a = π * D_Maj / (2 * *fs*)

    (let [a (int (/ (* π D_maj) 2 scad-clj.model/*fs*))
          δθ (/ π a)
          N (int (/ (* 2 a L) P))
          H (* 1/2 (Math/sqrt 3) P)
          D_min (- D_maj (* 10/8 H))]

      (polyhedron
       (concat
        [[0 0 0]]

        ;; Vertices

        (apply concat (for [i (range N)
                            :let [r (+ D_maj (/ H 4))
                                  [x_1 x_2] (map (partial * 1/2 (Math/cos (* i δθ))) [D_min r])
                                  [y_1 y_2] (map (partial * 1/2 (Math/sin (* i δθ))) [D_min r])
                                  z #(* (+ % (/ (* i δθ) 2 π)) P)]]

                        [[x_1 y_1 (z -1/2)]
                         [x_1 y_1 (z -3/8)]
                         [x_2 y_2 (z 0)]
                         [x_1 y_1 (z 3/8)]
                         [x_1 y_1 (z 1/2)]]))

        ;; Make the top of the thread conical, to ensure no support is
        ;; needed above it (for female threads).

        [[0 0 (+ L (or L_extra (/ D_maj 2)))]])

       ;; Indices

       (concat
        [(conj (range 5 0 -1) 5 0)]

        (for [i (range (* 2 a))]
          [0 (inc (* i 5)) (inc (* (inc i) 5))])

        (for [i (range (dec N)) j (range 4)]
          (map (partial + 1 (* i 5)) [j (inc j) (+ 6 j) (+ 5 j)]))

        (for [i (range (* 2 a))]
          (map (partial - (* 5 N)) [-1 (* i 5) (* (inc i) 5)]))

        [(map (partial - (* 5 N)) (conj (range 4 -1 -1) -1 0))])))))

;;;;;;;;;;;;;;;;;;;;;;
;; Controller board ;;
;;;;;;;;;;;;;;;;;;;;;;

(def pcb-size [30 65])
(def pcb-thickness 1.6)

;; The radius and position of the board screw hole, measured from the
;; corner of the board.

(def pcb-mount-hole [3/2 4 4])

;; The size and position of the connectors, measured from the
;; upper-left corner of the board to the upper-left corner of the
;; connector.

(def pcb-button-position [4.6 52.7])
(def pcb-button-diameter 2.5)

(def pcb-6p6c-size [16.64 13.59 16.51])
(def pcb-6p6c-position [2.9 17.4])

(def pcb-usb-size [11.46 12.03 15.62])
(def pcb-usb-position [7.8 2.2])

(defn pcb-place [flip? shape]
  (cond->> shape
    flip? (translate (map * [0 -1] pcb-size))

    (not flip?) (mirror [0 1 0])

    true (rotate [0 π 0])
    true (translate pcb-position)))

(def pcb-button-hole-cutout
  (delay
   (->> (cylinder (/ pcb-button-diameter 2) 50)
        (translate pcb-button-position))))

(def pcb-connector-cutout
  (apply union
         (for [[size position] [[pcb-usb-size pcb-usb-position]
                                [pcb-6p6c-size pcb-6p6c-position]]
               :let [δ 0.8
                     [a b c] (map + [δ δ 0] size)
                     [x y] (map (partial + (/ δ -2)) position)]]
           (union
            ;; Main cutout

            (->> (cube a b c :center false)
                 (translate [x y pcb-thickness]))

            ;; Chamfer cutout

            (->> (square a b)
                 (extrude-linear {:height 1/2
                                  :scale [(+ 1 (/ 1 a))
                                          (+ 1 (/ 1 b))]
                                  :center false})
                 (union (translate [0 0 (+ 50/2 1/2)]
                                   (cube (+ a 1) (+ b 1) 50)))
                 (translate [(+ x (/ a 2))
                             (+ y (/ b 2))
                             (+ (last pcb-position) cover-thickness -3/2)]))))))

(def pcb-bosses
  (for [s [-1 1] t [-1 1]
        :let [[w h] pcb-size
              [_ δx δy] pcb-mount-hole
              [D P L] pcb-fastener-thread
              d 6.8

              ;; Make the boss a little higher than the thread (here
              ;; 0.8mm) to allow for a couple of solid layers at the
              ;; bottom of the boss and a better attachment to the
              ;; base.

              h_b (+ L (/ D 2) 4/5)
              z (partial + (last pcb-position))]]

    (->> (difference
          (->> (cube d d h_b)
               (intersection (cylinder 4 h_b))
               (translate [0 0 (z (/ h_b -2))]))
          (->> (apply thread (update pcb-fastener-thread 2 + P))
               (translate [0 0 (z (- (+ h_b P)))])))

         (translate [(- (* 1/2 (inc s) w) (* s δx))
                     (- (* 1/2 (inc t) h) (* t δy))
                     0]))))

(def pcb
  (let [corner-radius 4]

    (with-fs 1/2
      ;; The PCB.

      (color [0 0.55 0.29]
             (difference
              (hull
               ;; The basic PCB...

               (for [s [-1 1] t [-1 1]
                     :let [[w h] pcb-size]]
                 (->> (cylinder corner-radius pcb-thickness)
                      (translate [(- (* 1/2 (inc s) w) (* s corner-radius))
                                  (- (* 1/2 (inc t) h) (* t corner-radius))
                                  (/ pcb-thickness 2)]))))

              ;; minus the mount holes...

              (for [s [-1 1] t [-1 1]
                    :let [[w h] pcb-size
                          [r δx δy] pcb-mount-hole]]
                (->> (cylinder r (* 3 pcb-thickness))
                     (translate [(- (* 1/2 (inc s) w) (* s δx))
                                 (- (* 1/2 (inc t) h) (* t δy))
                                 0])))

              ;; minus the center cutout.

              (->> (cylinder 6 (* 3 pcb-thickness))
                   (union (translate [6 0 0] (cube 12 12 (* 3 pcb-thickness))))
                   (translate [29 32.5 0]))))

      ;; The USB/6P6C connectors.

      (color (repeat 3 0.75)
             (->> (apply cube (conj pcb-usb-size :center false))
                  (translate (conj pcb-usb-position pcb-thickness)))
             (->> (apply cube (conj pcb-6p6c-size :center false))
                  (translate (conj pcb-6p6c-position pcb-thickness))))

      ;; The USB cable

      (translate (conj (mapv #(+ %1 (/ %2 2))
                             pcb-usb-position
                             pcb-usb-size) (+ pcb-thickness 7))
                 (->> (cube 7 8 12)     ; The plug
                      (color (repeat 3 0.85))
                      (translate [0 0 6]))
                 (->> (cube 22 13 16)   ; The housing
                      (color (repeat 3 0.2))
                      (translate [9/2 0 20]))
                 (->> (cylinder 4 10)   ; The strain relief
                      (color (repeat 3 0.2))
                      (rotate [0 (/ π 2) 0])
                      (translate [41/2 0 21]))
                 (->> (cylinder 2 30)   ; The cable
                      (color (repeat 3 0.2))
                      (rotate [0 (/ π 2) 0])
                      (translate [71/2 0 21])))


      ;; The board-to-wire connectors.

      (color [1 1 1]
             (for [y [8 39]]
               (translate [25 y -8]
                          (cube 4.5 18 8 :center false)))

             (translate [9 60 -8]
                        (cube 14 4.5 8 :center false))))))

(def pcb-harness-bracket
  (let [δ -5/2
        r (first pcb-mount-hole)
        h (second pcb-size)]
    (difference
     (union
      (hull
       (translate [δ 0 7/2] (cube 2 h 6))
       (translate [δ 0 7] (cube 2 (- h 2) 1)))

      (for [s [-1 1]
            :let [y (* s (- (* 1/2 h) 4))]]
        (translate [0 y -1]
                   (map hull
                        (partition 2 1 [(translate [δ (* 3/2 s) 1] (cube 2 5 2))
                                        (translate [(+ δ 1) (* 3/2 s) 0] (cube 2 5 2))
                                        (translate [1/2 (* 3/2 s) 0] (cube 2 5 2))]))

                   (hull (translate [1/2 (* 3/2 s) 0] (cube 2 5 2))
                         (translate [4 0 0] (cylinder 4 2))))))
     (hull
      (translate [δ 0 -1/2] (cube 3 (- h 10) 6))
      (translate [δ 0 3] (cube 3 (- h 12) 1)))

     (for [s [-1 1]
           :let [y (* s (- (* 1/2 h) 4))]]
       (translate [4 y 0] (cylinder (+ r 1/3) 10))))))

;;;;;;;;;;
;; Keys ;;
;;;;;;;;;;

(defn key-plate [where, i j]
  (let [key-scale (key-scale-at where, i j)]
    (difference
     (union
      (translate [0 0 (/ plate-thickness -2)]
                 (difference
                  (apply hull
                         (for [s [-1 1] t [-1 1] u [1 -1]
                               :let [[x y z] (scale-to-key where, i j, s t u)]]
                           (->> (sphere (/ plate-thickness 4 (Math/cos (/ π 8))))
                                (with-fn 8)
                                (rotate [0 0 (/ π 8)])
                                (translate [x y 0])
                                (translate (map (partial * (/ plate-thickness -4)) [s t]))
                                (translate [0 0 (/ z 2)]))))
                  (cube plate-hole-size plate-hole-size (* 2 plate-thickness))))
      (for [i (range 4)
            :let [nub-length 5
                  [a b] ((if (even? i) identity reverse)
                         (mapv * (repeat plate-size) key-scale))]]

        (rotate [0 0 (* i π 1/2)]
                (union
                 (when (place-nub? i)
                   (->> (cube (* nub-width 11/10) nub-length nub-height :center false)
                        (mirror [0 0 1])
                        (translate [(- (+ (/ plate-hole-size 2) (* nub-width 1/10)))
                                    (/ nub-length -2)
                                    0])))))))

     ;; Enlarge the hole below the nubs, to provide some space for
     ;; the tabs on the switch to extend into.

     (let [d 5      ; The depth of the keyswitch below the top of the plate.
           l 3/4    ; Extend the hole by this much on each side.

           m (+ nub-height l)
           c (+ plate-hole-size (* 2 l))
           h (- plate-thickness m)]
       (translate [0 0 (- m)]
                  (->> (square c c)
                       (extrude-linear {:height (+ l nub-width)
                                        :scale (repeat 2 (/ (- plate-hole-size (* 2 nub-width)) c))
                                        :center false}))
                  (translate [0 0 (/ (- m d) 2)]
                             (cube c c (- d m))))))))

(def keyswitch-socket
  (translate [(+ -2.275 -2.54)       ; Kaihua PG1511 keyswitch socket.
              (+ -1.675 -5.08)
              (+ -5 -3.05)]
             (with-fn 30
               (color (repeat 3 0.2)
                      (difference
                       (cube 10.9 5.89 1.80 :center false) ; Main body

                       (translate [10.9 0 0]  ; Bottom-right fillet cutout
                                  (difference
                                   (cube 4 4 10)
                                   (translate [-2 2 0]
                                              (cylinder 2 10))))

                       (translate [0 5.89 0]  ; Top cutout
                                  (union
                                   (cube 10.8 4 10)
                                   (translate [5.4 0 0]
                                              (cylinder 2 10)))))

                      (apply union            ; Switch pin contacts
                             (for [r [[2.275 1.675 0]
                                      [8.625 4.215 0]]]
                               (translate r (cylinder (/ 2.9 2) 3.05 :center false)))))

               (color (repeat 3 0.75)         ; Board contacts
                      (apply union
                             (for [r [[-1.8 (- 1.675 (/ 1.68 2)) 0]
                                      [10.9 (- 4.215 (/ 1.68 2)) 0]]]
                               (translate r (cube 1.8 1.68 1.85 :center false))))))))

(defn orient-keyswitch [where, i j, shape]
  (cond->> shape
    (or (and (not= where :thumb) (not= i 2) (not= i 5) (not= j 0))
        (and (= where :thumb) (not= j 0))) (rotate [0 0 π])))

(defn keyswitch [where, i j]
  (orient-keyswitch
   where, i j
   (union
    (color (repeat 3 0.2)
           (hull                           ; Casing, below the plate.
            (translate [0 0 -5/4]
                       (cube 13.95 13.95 5/2))
            (translate [0 0 -15/4]
                       (cube 12.5 13.95 5/2)))

           (translate [0 0 -5.4]           ; Center locating pin.
                      (cylinder (/ 3.85 2) 2))

           (->> (square 13.95 15.6)        ; Casing, above the plate.
                (translate [0 2 0])
                (extrude-linear {:height 6.2 :scale [2/3 2/3] :center false})
                (translate [0 -2 0])))

    (color [0.1 0.5 1]
           (translate [0 0 8]           ; Stem
                      (cube 6 6 4)))

    (color [0.8 0.5 0.2]                ; Electrical terminals.
           (translate [-2.54 -5.08 -6.30]
                      (cube 1.5 0.2 3))

           (translate [3.81 -2.54 -5.85]
                      (cube 1.5 0.2 4))))))

;; A basic keycap shape.  Cylindrical sides, spherical top, rounded
;; corners.  Can be configured to yield SA and DSA style keycaps.
;; Additionally supports a couple more exotic shapes.
;;
;; Where:
;; h is the height of keycap (at top center),
;; h_0 is the height of vertical (i.e. not curved) section at bottom,
;; ρ is the shared radius of sides, top and corner rounding,
;; φ determines how deeply to round the corners.

(defn base-keycap-shape [size h h_0 ρ & {:keys [fn-value shape top corner-rounding
                                                saddle-aspect saddle-skew fang-angle
                                                fang-skew fang-corner-rounding]}]
  (let [[minus-or-plus intersection-or-difference]
        (case top
          (:spherical-convex :saddle) [- intersection]
          [+ difference])
        
        d_0 (/ keycap-length 2) ; Half-length at base
        d_1 (* 1/4 25.4)        ; Half-length at top

        ;; h_1 is height at center of each crest

        h_1 (case top
              :saddle h
              :cylindrical (minus-or-plus h (- ρ (Math/sqrt (- (* ρ ρ) (* d_1 d_1)))))
              (minus-or-plus h (* ρ (- 1 (Math/cos (Math/asin (/ d_1 ρ)))))))

        ;; Consider two points on the center of the base and top of a
        ;; side of the key.  O is the center of a circle of the given
        ;; radius passing through them.  This will be used to form the
        ;; sides.

        p [d_0 h_0]
        q [d_1 h_1]

        v (map (partial * 1/2) (map - q p))
        a (one-over-norm v)
        b (Math/pow (- (* ρ ρ) (/ 1 a a)) 1/2)
        O (map + p v [(* -1 b a (second v)) (* b a (first v))])]

    ;; We need to set *fn* explicitly, since extrude-rotate below
    ;; doesn't seem to respect the *fa*/*fs* settings.

    (union
     (with-fn (or fn-value (and draft? 80) 160)
       (cond-> (intersection-or-difference
                (intersection
                 ;; Initial block.

                 (or shape
                     (union
                      (translate [0 0 h] (apply cube (conj size (* 2 h))))
                      (when fang-angle  ; Extend the side circularly.
                        (->> (square (size 1) (* 2 h))
                             (translate [0 h])
                             (intersection (translate O (circle ρ)))
                             (translate [(/ (size 1) 2) 0])
                             (intersection ; Errors from the previous intersection can extend
                                           ; the shape to span the Y axis, so we clip it.
                              (translate [50 0] (square 100 100)))
                             (extrude-rotate {:angle (/ fang-angle π 1/180)})
                             (rotate (/ π 2) [0 0 1])
                             (translate (map * (repeat -1/2) size))))))

                 ;; Cylindrical sides.

                 (for [s [-1 1] t [0 1]
                       :let [fang (and fang-angle
                                       (= [s t] [-1 0]))]]
                   (cond->> (cylinder ρ (+ (apply max size) 100))
                     true (rotate (/ π 2) [1 0 0])
                     true (translate [(* s (+ (first O) (/ (- (size t) keycap-length) 2)))
                                      0
                                      (second O)])
                     true (rotate (* t π 1/2) [0 0 1])
                     fang (translate (map * (repeat 1/2) size))
                     fang (rotate fang-angle [0 0 1])
                     fang (translate (map * (repeat -1/2) size)))))

                ;; Plus or minus the top.

                (case top
                  ;; Torroidal top.

                  :saddle (->> (circle ρ)
                               (translate [(* saddle-aspect ρ) 0])
                               (extrude-rotate {:angle 360})
                               (rotate [(/ π 2) (/ π 2) 0])
                               (translate [0 0 (* (- saddle-aspect 1) ρ)])

                               (translate (map * (concat saddle-skew [1]) (repeat h))))

                  :cylindrical (->> (cylinder ρ 100)
                                    (scale [(/ (first size) keycap-length) 1 1])
                                    (rotate [(/ π 2) 0 0])
                                    (translate [0 0 (+ ρ h)]))

                  ;; Spherical top (rotated to avoid poles).

                  (apply hull (for [s [-1/2 1/2] t [-1/2 1/2]
                                    :let [δ (map -
                                                 (if (and fang-angle (neg? s))
                                                   (map * [fang-skew 1] size) size)
                                                 (repeat keycap-length))]]
                                (->> (sphere ρ)
                                     (rotate [(/ π 2) 0 0])
                                     (translate (conj (mapv * [s t] δ) (minus-or-plus h ρ))))))))

         ;; Rounded corner.

         corner-rounding
         (difference
          (for [s [0 1] t [0 1]
                :when (or (not fang-angle)
                          (not= [s t] [0 0]))
                :let [fang (and fang-angle (zero? s))

                      ρ_0 2
                      ρ_1 ρ]]
            (cond->> (difference (circle 10)
                                 (circle ρ_0)
                                 (union
                                  (translate [-50 0] (square 100 100 :center false))
                                  (translate [0 -50] (square 100 100 :center false))))
              true (rotate [0 0 (/ π -4)])
              true (translate [(- ρ_1) 0])
              true (extrude-rotate {:angle 90})
              true (translate [(+ ρ_0 ρ_1) 0])
              true (rotate [(/ π -2) (if fang fang-corner-rounding corner-rounding) (/ π 4)])
              true (translate (conj (mapv (partial * -1/2) size) h_0))
              true (mirror [s 0])
              true (mirror [0 t])

              fang (translate (map * (repeat 1/2) size))
              fang (rotate fang-angle [0 0 1])
              fang (translate (map * (repeat -1/2) size))))))))))

(defn keycap-shape [size & rest]
  (let [{h :height           ; Keycap height (measured at top-center).
         h_add :extra-height ; Additional (wrt profile) height.
         h_0 :base-height    ; Height of vertical part of base.
         ρ :radius           ; Radius of sides and top.
         :or {h_add 0
              h_0 0}} rest]
    (apply base-keycap-shape size (+ h h_add) h_0 ρ rest)))

(defn keycap [where, i j]
  ;; For distance from key plate to released keycap (for Cherry MX
  ;; switches, and assuming stem flush with keycap bottom), see:
  ;;
  ;; https://www.cherrymx.de/en/dev.html

  (let [[family-options color-triplet] (key-options-at where, i j)]
    (->> (apply keycap-shape
                (mapv (partial * keycap-length) (key-scale-at where, i j))
                family-options)

         (translate [0 0 (if keys-depressed? 3 6.6)])
         (color color-triplet))))

(defn printable-keycap [scale & rest]
  (let [size (mapv (partial * keycap-length) scale)
        {w :thickness
         a :mount-cross-length
         b :mount-cross-width
         r :mount-radius
         δ :mount-offset
         h :height
         h_add :extra-height
         h_0 :mount-recess
         h_1 :homing-bar-height
         :or {δ [0 0]
              h_add 0
              h_0 0
              h_1 0}
         } rest

        δ_1 3/2
        δ_2 0.4
        δ_3 (mapv (partial * keycap-length) δ)]

    (difference
     (union
      ;; The shell

      (difference
       (union
        (apply keycap-shape size rest)
        (when (pos? h_1)
          (->> (apply keycap-shape size rest)
               (intersection (hull (for [x [-2 2]]
                                     (translate [x 0 0] (with-fn 50 (cylinder 1/2 100))))))
               (translate [0 0 h_1]))))

       (union
        (apply keycap-shape (mapv (partial + (* -2 (first w))) size)
               (apply concat
                      (-> (apply hash-map rest)
                          (dissoc :corner-rounding)
                          (update :extra-height #(- (or % 0) (second w)))
                          seq)))
        (translate [0 0 -4.99] (apply cube (conj (mapv (partial + (* -2 (first w))) size) 10)))))

      ;; The stem

      (apply keycap-shape size
             :shape (translate (conj δ_3 h_0) (cylinder r 100 :center false))
             rest))

     (translate (conj δ_3 (+ 2 h_0))
                (for [θ [0 (/ π 2)]]
                  (rotate [0 0 θ]
                          (translate [0 0 -1/2] (cube a b 5))
                          (->> (square a b)
                               (extrude-linear {:height (/ b 2)
                                                :scale [1 0]
                                                :center false})
                               (translate [0 0 2]))))

                (->> (cube 15/8 15/8 4)
                     (rotate [0 0 (/ π 4)])))

     ;; The legend

     (when-let [[c font size] keycap-legend]
       (->> (text c
                  :font font
                  :size size
                  :halign "center"
                  :valign "center")
            (extrude-linear {:height 1
                             :center false})
            (translate [0 0 (+ h h_add -1/2)]))))))

;; Set up bindings that either generate SCAD code to place a part, or
;; calculate its position.

(declare ^:dynamic rotate-x
         ^:dynamic rotate-z
         ^:dynamic translate-xyz)

(defn transform-or-calculate [transform?]
  (if transform?
    {#'rotate-x #(rotate %1 [1 0 0] %2)
     #'rotate-z #(rotate %1 [0 0 1] %2)
     #'translate-xyz translate}

    {#'rotate-x #(identity [(first %2)
                            (reduce + (map * %2 [0 (Math/cos %1) (- (Math/sin %1))]))
                            (reduce + (map * %2 [0 (Math/sin %1) (Math/cos %1)]))])
     #'rotate-z #(identity [(reduce + (map * %2 [(Math/cos %1) (- (Math/sin %1)) 0]))
                            (reduce + (map * %2 [(Math/sin %1) (Math/cos %1) 0]))
                            (nth %2 2)])
     #'translate-xyz (partial mapv +)}))

;; Either place a shape at [x y z] in the local frame of key [i j] in
;; section where (either :thumb or :key), or calculate and return the
;; coordinates of a point relative to that location.  For convenience,
;; the scale of the local frame depends on the key size in question,
;; so that [x y z] = [1 1 0] is at the top right corner of the upper
;; face of the plate (z starts at zero at the top of the face, as
;; opposed to the center as is the case for x and y, to ensure that
;; the key geometry doesn't change with plate thickness).

(defn key-place [where, i j, x y z & [shape-or-point]]
  (if (not= where :thumb)
    ;; Place on/at a main section key.

    (let [offset (scale-to-key where, i j, x y z)]

      ;; The key plates are spread out along the surface of a torus
      ;; and are always tangent to it.  The angle subtended by a 1u
      ;; key plate (of dimensions plate-size) is 2 * atan(l / 2r),
      ;; where l and r the respective edge length and radius.

      (let [central-angle (fn [l r] (* 2 (Math/atan (/ l 2 r))))
            location (fn [s t phase scale spacing radius]
                       (reduce +
                               (* (+ (phase s) (* scale 1/2))
                                  (central-angle plate-size radius))

                               (for [k (range t)]
                                 (central-angle (+ plate-size (spacing k)) radius))))

            θ (location i j column-phase 1 (constantly (row-spacing i)) (column-radius i))
            φ (location j i row-phase (first (key-scale-at where, i j)) column-spacing row-radius)

            maybe-flip #(if (= [i j] [(column -1) (row -1)])
                          (->> %
                               (translate-xyz palm-key-offset)
                               (translate-xyz [0 (* -1/2 plate-size) 0])
                               (rotate-x (/ π 2))
                               (translate-xyz [0 (* 1/2 plate-size) 0]))
                          %)]

        (with-bindings (transform-or-calculate (fn? shape-or-point))
          (->> (if (fn? shape-or-point)
                 (shape-or-point where, i j)
                 (or shape-or-point [0 0 0]))

               (translate-xyz offset)

               maybe-flip

               (translate-xyz [0 0 (- (column-radius i))])
               (rotate-x (- θ))
               (translate-xyz [0 0 (column-radius i)])

               (translate-xyz [0 0 (- row-radius)])
               (rotate-x (/ π 2))
               (rotate-z (- φ))
               (rotate-x (/ π -2))
               (translate-xyz [0 0 row-radius])

               (translate-xyz [0 (column-offset i) 0])
               (translate-xyz [0 0 (column-height i)])
               (translate-xyz [0 0 global-z-offset])))))

    ;; Same as above, but for the thumb section.

    (with-bindings (transform-or-calculate (fn? shape-or-point))
      (->> (if (fn? shape-or-point) (shape-or-point where, i j) (or shape-or-point [0 0 0]))
           (translate-xyz (scale-to-key where, i j, x y z))
           (rotate-x (thumb-key-slope i j))

           (translate-xyz (thumb-key-offset i j))

           (translate-xyz [0 thumb-radius 0])
           (rotate-x (- thumb-slant))
           (rotate-z (reduce + (map * (thumb-key-phase i j) [1 i])))
           (rotate-x thumb-slant)
           (translate-xyz [0 (- thumb-radius) 0])

           (translate-xyz
            (map +
                 (key-place :main, 1 (row -2), 1 -1 0)
                 thumb-offset))))))

(defn key-placed-shapes [shape]
  (for [i columns j rows
        :when (place-key-at? [i j])]
    (key-place :main, i j, 0 0 0, shape)))

(defn thumb-placed-shapes [shape]
  (for [j (range 3)
        i (case j
            0 (range 4)
            1 (range 3)
            [1])]
    (key-place :thumb, i j, 0 0 0, shape)))

;;;;;;;;;;;;;;;;;;;;;;;
;; Connecting tissue ;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn web-kernel [place, i j, x y & [z]]
  ;; These are small shapes, placed at the edges of the key plates.
  ;; Hulling kernels placed on neighboring key plates yields
  ;; connectors between the plates, which preserve the plate's chamfer
  ;; and are of consistent width.  (We make the
  ;; kernels "infinitesimally" larger than the key plates, to avoid
  ;; non-manifold results).

  (key-place place, i j, x y (or z 0)
             (fn [& _]
               (let [[δx δy] (map #(* (compare %1 0) -1/4 plate-thickness) [x y])
                     ε (+ (* x 0.003) (* y 0.002))]
                 (->> (sphere (/ (+ plate-thickness ε) 4 (Math/cos (/ π 8))))
                      (with-fn 8)
                      (rotate [0 0 (/ π 8)])
                      (translate [δx δy (/ plate-thickness s 4)])
                      (for [s [-1 1]])
                      (apply hull)
                      (translate [0 0 (/ plate-thickness -2)]))))))

(def key-web (partial web-kernel :main))
(def thumb-web (partial web-kernel :thumb))

(defn triangle-hulls [& shapes]
  (->> shapes
       (partition 3 1)
       (map (partial apply hull))
       (apply union)))

(def connectors
  (delay
   (list*
    ;; Odds and ends, which aren't regular enough to handle in a loop.

    (when (every? place-key-at? [[1 (row -2)]
                                 [2 (row -2)]])
      (triangle-hulls
       (key-web 1 (row -2) 1 -1)
       (key-web 1 (row -2) 1 1)
       (key-web 2 (row -2) -1 -1)
       (key-web 1 (row -3) 1 -1)))

    (when (every? place-key-at? [[2 0] [1 0] [2 0]])
      (triangle-hulls
       (key-web 2 1 -1 1)
       (key-web 1 0 1 1)
       (key-web 2 0 -1 -1)
       (key-web 2 0 -1 1)))

    (when (every? place-key-at? [[3 (row -1)]
                                 [3 (row -2)]
                                 [4 (row -2)]])
      (triangle-hulls
       (key-web 3 (row -2) 1 -1)
       (key-web 4 (row -2) -1 -1)
       (key-web 3 (row -1) 1 1)
       (key-web 3 (row -1) 1 -1)))

    ;; Palm key connectors.

    (when (every? place-key-at? [[(column -1) (row -2)]
                                 [(column -2) (row -2)]])
      (triangle-hulls
       (key-web (column -1) (row -2) -1 -1)
       (key-web (column -1) (row -1) -1 1)
       (key-web (column -2) (row -2) 1 -1)))

    ;; Regular connectors.

    (concat
     (for [i (butlast columns)           ; Row connections
           j rows
           :let [maybe-inc (if (= i 1) inc identity)]
           :when (and (not= [i j] [1 (row -2)])
                      (every? place-key-at? [[i j]
                                             [(inc i) j]]))]
       (apply triangle-hulls
              (cond-> [(key-web i j 1 1)
                       (key-web i j 1 -1)
                       (key-web (inc i) (maybe-inc j) -1 1)]

                ;; This bit is irregular for the (row -1) of the first
                ;; column.  It's taken care of by the thumb connectors.

                (not= [i j] [1 (row -2)]) (into [(key-web (inc i) (maybe-inc j) -1 -1)]))))

     (for [i columns                     ; Column connections
           j (butlast rows)
           :when (every? place-key-at? [[i j]
                                        [i (inc j)]])]
       (triangle-hulls
        (key-web i j -1 -1)
        (key-web i j 1 -1)
        (key-web i (inc j) -1 1)
        (key-web i (inc j) 1 1)))

     (for [i (butlast columns)           ; Diagonal connections
           j (butlast rows)
           :let [maybe-inc (if (= i 1) inc identity)]
           :when (and (not= [i j] [1 (row -3)])
                      (every? place-key-at?
                              (for [s [0 1] t [0 1]]
                                [(+ i s) (+ j t)])))]
       (triangle-hulls
        (key-web i j 1 -1)
        (key-web i (inc j) 1 1)
        (key-web (inc i) (maybe-inc j) -1 -1)
        (key-web (inc i) (maybe-inc (inc j)) -1 1)))))))

(def thumb-connectors
  (delay
   (let [z (/ ((thumb-key-offset 0 1) 2) plate-thickness)
         y -5/16]
     (list
      (triangle-hulls
       (thumb-web 0 0 1 -1)
       (thumb-web 1 0 1 -1)
       (thumb-web 0 0 -1 -1)
       (thumb-web 1 0 1 1)
       (thumb-web 0 0 -1 1)
       (thumb-web 0 0 -1 1)
       (thumb-web 0 0 1 1))

      (triangle-hulls
       (thumb-web 2 0 1 1)
       (thumb-web 1 0 -1 1)
       (thumb-web 2 0 1 -1)
       (thumb-web 1 0 -1 -1)
       (thumb-web 1 1 -1 1)
       (thumb-web 1 0 1 -1)
       (thumb-web 1 1 1 1))

      (triangle-hulls
       (thumb-web 0 0 1 -1 z)
       (thumb-web 1 1 1 1)
       (thumb-web 0 1 -1 1)
       (thumb-web 1 1 1 -1)
       (thumb-web 0 1 -1 -1)
       (thumb-web 1 2 1 1)
       (thumb-web 0 1 1 -1)
       (thumb-web 1 2 1 -1))

      (triangle-hulls
       (thumb-web 2 1 -1 1)
       (thumb-web 3 0 -1 -1)
       (thumb-web 2 1 1 1)
       (thumb-web 3 0 1 -1)
       (thumb-web 2 0 -1 -1)
       (thumb-web 3 0 1 1)
       (thumb-web 2 0 -1 1))

      (triangle-hulls
       (thumb-web 2 0 1 -1)
       (thumb-web 2 0 -1 -1)
       (thumb-web 1 1 -1 1)
       (thumb-web 2 1 1 1)
       (thumb-web 1 1 -1 -1)
       (thumb-web 2 1 1 -1)
       (thumb-web 1 2 -1 1)
       (thumb-web 2 1 -1 -1)
       (thumb-web 1 2 -1 -1))

      (triangle-hulls
       (thumb-web 1 1 -1 -1)
       (thumb-web 1 2 -1 1)
       (thumb-web 1 1 1 -1)
       (thumb-web 1 2 1 1))

      (when (place-key-at? [0 (row -2)])
        (triangle-hulls
         (key-web 1 (row -2) -1 -1)
         (key-web 0 (row -2) 1 -1)
         (thumb-web 0 0 -1 1)
         (key-web 0 (row -2) -1 -1)
         (thumb-web 1 0 1 1)
         (thumb-web 1 0 -1 1)))

      (triangle-hulls
       (thumb-web 0 1 -1 1)
       (thumb-web 0 1 1 1)
       (thumb-web 0 0 1 -1 z)
       (key-web 3 (row -1) -1 -1)
       (key-web 2 (row -1) -1 -1)
       (key-web 2 (row -1) 1 -1))

      (triangle-hulls
       (thumb-web 1 1 1 1)
       (thumb-web 1 0 1 -1)
       (thumb-web 0 0 1 -1 z)
       (thumb-web 0 0 1 -1)
       (key-web 2 (row -1) -1 -1)
       (thumb-web 0 0 1 1)
       (key-web 2 (row -1) -1 y)
       (thumb-web 0 0 -1 1)
       (key-web 1 (row -2) 1 -1)
       (key-web 1 (row -2) -1 -1))

      (triangle-hulls
       (key-web 2 (row -1) -1 y)
       (key-web 2 (row -1) -1 1)
       (key-web 1 (row -2) 1 -1)
       (key-web 2 (row -2) -1 -1))))))

;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(defn case-placed-shapes [brace]
  (let [place #(apply brace %&)
        strip (fn [where & rest]
                (for [ab (partition 2 1 rest)]
                  (apply place (map (partial cons where )
                                    (if (:reverse (meta (first ab)))
                                      (reverse ab) ab)))))]

    (concat
     ;; Back wall

     (list*
      (place [:left, 0 0, -1 1]
             [:back, 0 0, -1 1])

      (apply strip
             :back
             (for [i columns
                   x (conj (vec (range -1 1 (/ 1/2 (first (key-scale-at :main, i 0))))) 1)]
               [i 0, x 1])))

     (list
      (place [:back, (column -1) 0, 1 1]
             [:right, (column -1) 0, 1 1]))

     ;; Right wall

     (apply strip
            :right
            (for [j rows
                  y [1 -1]]
              [(column -1) j, 1 y]))

     ;; Front wall

     (list*
      (place [:right, (column -1) (row -1), 1 -1]
             [:front, (column -1) (row -1), 1 -1, -1/4 1/4])

      (strip :front
             [(column -1) (row -1), 1 -1, -1/4 1/4]
             [(column -1) (row -1), -1 -1, 1/4 1/4]
             [(column -1) (row -1), -1 1, -5 -8 0]
             [(column -2) (row -2), 1 -1, -9 -4]
             [(column -2) (row -2), 0 -1, 0 -4]
             [(column -2) (row -2), -1 -1, 3 -4]
             [3 (row -1), 1 -1, 0 -3 -9/2]
             [3 (row -1), 0 -1, 4 -3 -9/2]
             [3 (row -1), -1 -1]))

     ;; Thumb walls

     (list*
      (place [:front, 3 (row -1), -1 -1]
             [:thumb, 0 1, 1 1])

      (strip :thumb
             [0 1, 1 1]
             [0 1, 1 -1, 1/2 0 -2]
             [1 2, 1 -1, 1 -1]
             [1 2, -1 -1, -1 -1]
             [2 1, -1 -1, -1/2 0 -2]
             [2 1, -1 1, -1/2 -1 -2]
             [3 0, -1 -1, -1/2 17/8 -5]
             [3 0, -1 1, 1/2 -7/4 -3]
             [3 0, -1 1, 7/4 -1/2 -3]
             [3 0, 1 1, -3 1/2 -5]
             [2 0, -1 1, 0 1]
             [2 0, 1 1, 0 1]
             [1 0, -1 1]))

     (list
      (place [:thumb, 1 0, -1 1]
             [:left, 0 (row -2), -1 -1]))

     ;; Left wall.  Stripping in reverse order is necessary for
     ;; consistent winding, which boss placement relies on.

     (apply strip
            :left
            (for [j (rest (reverse rows))
                  y [-1 1]]
              [0 j, -1 y])))))

(defn lagrange [i j, x y z, dy]
  ;; They curve of the back side is specified via a set of points
  ;; measured from the keys of the first row.  It passes through those
  ;; points and is smoothly interpolated in-between, using a Lagrange
  ;; polynomial.  We introduce a discontinuity between the second and
  ;; third column, purely for aesthetic reasons.

  (let [discontinuous? true
        [xx yy zz] (if (and discontinuous? (< i 2))
                     [-23/4 0 -65/4]
                     [0 23/4 -13])

        u (first (key-place :main, i j, x y 0))

        uu [(first (key-place :main, 0 0, -1 y 0))
            (first (key-place :main, 3 0, 1 y 0))
            (first (key-place :main, (column -1) 0, 1 y 0))]

        vv [(key-place :main, 0 0, -1 y z, [(if (neg? z) 10 0)
                                            (+ (* 1/2 (- 1 y) plate-size) dy)
                                            -15])
            (key-place :main, 3 0, 1 y (* 5/13 z), [xx (+ (* 5/13 dy) yy) zz])
            (key-place :main, (column -1) 0, 1 y 0, [-1 -5/4 -13/4])]

        l (fn [k]
            (reduce * (for [m (range (count uu))
                            :when (not= m k)]
                        (/ (- u (uu m)) (- (uu k) (uu m))))))]

    (apply (partial map +)
           (for [k (range (count vv))]
             (map (partial * (l k)) (vv k))))))

(defn wall-place [where, i j, x y z, dx dy dz & [shape]]
  (let [offsets (map (fn [a b] (or a b))
                     [dx dy dz]
                     (case where
                       :back [0 0 -15]
                       :right (case [j y]
                                [0 1] [-1/4 -5/2 -5/2]
                                [3 -1] [-3/8 3/2 -19/8]
                                [4 1] [-3/8 -3/2 -19/8]
                                [4 -1] [-1/4 1/4 -5/2]
                                [-1/4 0 -5/2])
                       :left [0 (case [j y]
                                  [3 -1] 2
                                  0) -15]
                       :front (if (= [i j, x y] [3 (row -1), -1 -1])
                                [7 -5 -6]
                                [0 1/2 (if (= i 4) -5 -5/2)])
                       :thumb (cond
                                (= [i j, x] [1 0, -1]) [1 0 -3/2]
                                (= [i j, x y] [0 1, 1 1]) [0 -3/8 -2]
                                :else  [0 0 -6])))]
    (if (= where :back)
      (cond-> (lagrange i j, x y z, (second offsets))
        shape (translate shape))

      (key-place where, i j, x y z (cond-> offsets
                                     shape (-> (translate shape)
                                               constantly))))))

(defn wall-place-a [where i j x y dx dy dz & [shape]]
  (wall-place where i j x y 0 dx dy dz shape))

(defn wall-place-b [where i j x y dx dy dz & [shape]]
  (cond
    (= where :left) (wall-place where i j x y -4 8 (case [j y]
                                                     [0 1] -10
                                                     [3 -1] 6
                                                     0) dz shape)

    (= where :back) (wall-place where i j x y -4 dx -8 dz shape)

    (and (= where :thumb)
         (not= [i j] [3 0])
         (not= [i j] [0 1])
         (not= [i j] [2 1])
         (not= [i j x] [1 0, -1])
         (not= [i j x] [2 0, 1]))

    (wall-place where i j x y -5 (- dx) (- dy) dz shape)

    (and (= [i j] [(column -1) (row -1)])
         (not= y 1))

    (wall-place where i j x y -3 dx dy dz shape)

    :else (wall-place-a where i j x y dx dy dz shape)))

(defn wall-sections [endpoints]
  (apply map vector
         (for [[where i j x y dx dy dz] endpoints
               :let [r (/ wall-thickness 2)
                     shape-a (wall-place-a where i j x y dx dy dz (sphere r))
                     shape-b (wall-place-b where i j x y dx dy dz (sphere r))]]
           [(web-kernel where, i j, x y)
            shape-a
            shape-b
            (->> shape-b
                 project
                 (extrude-linear {:height 1/10 :center false}))])))

(defn wall-brace [sections & endpoints]
  ;; Hull consecutive sections to form the walls.  Filter out
  ;; degenerate segments (where the parts are colinear, instead of
  ;; forming a triangle, for instance because shape-a and shape-b
  ;; above coincide), to avoid wasting cycles to generate non-manifold
  ;; results.

  (->> (sections (reverse endpoints))
       (apply concat)
       (partition 3 1)
       (filter #(= (count (set %)) 3))
       (map (partial apply hull))
       (apply union)))

;; Decide when to place a screw boss in a segment and what parameters
;; to use.  Note that we also use this to create a cutout for case
;; test builds.

(defn place-boss?
  ([endpoints]
   (place-boss? (if case-test-build?
                  (set case-test-locations)
                  (constantly true)) endpoints))

  ([boss-filter endpoints]
   (let [boss-map (apply hash-map
                         (apply concat
                                (keep-indexed
                                 #(when (boss-filter %1) [(set (take 2 %2)) (nth %2 2 [1/2 1])])
                                 screw-bosses)))]

     (boss-map (set (map (comp
                          (partial take 5)
                          #(cons (if (= (first %) :thumb) :thumb :key)
                                 (rest %))) endpoints))))))

(defn boss-place [x d endpoints & shape]
  (let [ab (for [[where i j x y dx dy dz] endpoints]
             (wall-place-b where i j x y dx dy dz))
        n (apply line-normal ab)]

    ;; Place the boss at some point x (in [0, 1]) along the wall
    ;; segment , displaced d radii inwards along the normal
    ;; direction.

    (cond-> (->> ab
                 (take 2)
                 (apply map #(+ %1 (* (- %2 %1) x)))
                 (mapv + (map (partial * d screw-boss-radius (one-over-norm n)) n)))
      shape (translate shape))))

(defn screw-boss [& endpoints]
  (when-let [[x d] (place-boss? endpoints)]
    ;; Hull the boss itself with a part of the final, straight wall
    ;; segment, to create a gusset of sorts, for added strength.  (We
    ;; don't use the exact wall thickness, to avoid non-manifold
    ;; results.)

    (hull
     (intersection
      (binding [wall-thickness (- wall-thickness 0.005)]
        (apply wall-brace
               (comp (partial take-last 2) wall-sections)
               endpoints))

      ;; The height is calculated to yield a 45 deg gusset.

      (boss-place x 0 endpoints (cylinder screw-boss-radius
                                          (+ screw-boss-height
                                             (- (* 2 screw-boss-radius)
                                                (/ wall-thickness 2)))
                                          :center false)))

     (boss-place x d endpoints (cylinder screw-boss-radius
                                         screw-boss-height
                                         :center false)))))

(defn countersink [r h t b]
  (let [r_1 (+ r (* h (Math/tan (/ cover-countersink-angle 2))))]
    (union
     (cylinder [r_1 r]
               h
               :center false)
     (when (pos? t)
       (translate [0 0 -1] (cylinder r (+ t 1) :center false)))
     (when (pos? b)
       (translate [0 0 (- b)] (cylinder r_1 b :center false))))))

(defn screw-countersink [& endpoints]
  (when-let [[x d] (place-boss? endpoints)]
    (let [r (/ cover-countersink-diameter 2)
          h cover-countersink-height]
      (boss-place x d endpoints (translate [0 0 (- h)]
                                           (countersink r h 50 50))))))

(defn screw-thread [& endpoints]
  (when-let [[x d] (place-boss? endpoints)]
    (let [[D P L] cover-fastener-thread]
      ;; Add another turn to the bottom of the thread, to ensure
      ;; proper CSG results at the bottom face of the boss.

      (boss-place x d endpoints
                  (translate [0 0 (- P)]
                             (apply thread (update cover-fastener-thread 2 + P)))))))

(defn case-test-cutout [& endpoints]
  (when-let [[x d] (place-boss? endpoints)]
    (let [[c t] (partition 3 3 case-test-volume)]
      (boss-place x d endpoints (translate (or t [0 0 0]) (apply cube c))))))

;; Form a pie-shaped shard of the bottom cover, by hulling a section
;; of the lower part of the wall with a shape at some point towards
;; the center of the cover (affectionately called the "navel").

(def ^:dynamic cover-navel (-> (key-place :main, 3 1 0 0 0)
                               vec
                               (assoc 2 0)
                               (translate (cube 1 1 cover-thickness :center false))))

(defn cover-shard [& endpoints]
  (->> (wall-sections endpoints)
       last
       (map (partial scale [1 1 (* cover-thickness 10)]))
       (cons cover-navel)
       hull))

;;;;;;;;;;;;;;;;;;;
;; Tenting stand ;;
;;;;;;;;;;;;;;;;;;;

;; We form the stand as a rotational extrusion of a strip running
;; along the periphery of the bottom cover, but we scale each section
;; appropriately, so as to end up with a straight projection, or some
;; in-between shape, selectable via stand-shape-factor.  The center of
;; rotation is chosen so that a specified minimum thickness is
;; maintained at the edge of the resulting wedge.

(defn flattened-endpoints [& endpoints]
  (for [[where i j x y dx dy dz] endpoints]
    (assoc (vec (wall-place-b where i j x y dx dy dz)) 2 0)))

(def stand-baseline-points
  (delay
   (let [points (case-placed-shapes flattened-endpoints)
         n (count points)]
     (->> points
          (cycle)
          (drop n)
          (take 60)))))

(def stand-baseline-origin
  (delay
   (+ (->> @stand-baseline-points
           (apply concat)
           (map first)
           (apply max))
      (/ wall-thickness 2))))

(defn stand-xform [θ shape]
  (let [t (+ @stand-baseline-origin
             (/ (last stand-minimum-thickness) (Math/sin stand-tenting-angle)))]
    (->> shape
         (translate [(- t) 0 0])
         (rotate [0 θ 0])
         (translate [t 0 0]))))

(defn stand-section
  ([s kernel] (stand-section s [[0 0 0] [stand-width 0 0]] kernel))
  ([s offsets kernel]
   (let [θ (* -1 s stand-tenting-angle)
         x_max @stand-baseline-origin]

     ;; Form the strip by displacing each point of the periphery along
     ;; the mean of the normals of the two edges that share it.  This
     ;; doesn't even result in a simple polygon for the inner periphery
     ;; of the strip, but it works well enough, as long as we're
     ;; careful when taking hulls.

     (for [[[a b] [_b c]] (partition 2 1 @stand-baseline-points)
           :let [_ (assert (= b _b))

                 u (map + (line-normal a b) (line-normal b c))
                 n (map (partial * (one-over-norm u)) u)
                 t [(- (second n)) (first n)]

                 ;; Scale with 1 / cos θ, to get a straight projection.

                 p (update b 0 #(+ (* (- 1 stand-shape-factor) %)
                                   (* stand-shape-factor
                                      (+ (* (- % x_max) (/ 1 (Math/cos θ))) x_max))))]]
       (stand-xform θ
                    (for [u offsets
                          :let [[c_n c_t c_b] u
                                q (conj
                                   (mapv + p (map (partial * c_n) n) (map (partial * c_t) t))
                                   c_b)]]
                      (translate q kernel)))))))

(def countersink-boss
  (delay
   (difference
    (countersink (- (/ cover-countersink-diameter 2) 1/4)
                 cover-countersink-height
                 0 0)
    (translate [0 0 (+ 5 cover-countersink-height -1/2)] (cube 10 10 10)))))

(defn stand-boss [& endpoints]
  (when-let [[x d] (place-boss? (set stand-boss-indexes) endpoints)]
    (boss-place x d endpoints @countersink-boss)))

(defn stand-boss-cutout [& endpoints]
  (when-let [[x d] (place-boss? (set stand-boss-indexes) endpoints)]
    (boss-place x d endpoints (translate [0 0 -3/2]
                                         (countersink (/ cover-countersink-diameter 2)
                                                      cover-countersink-height
                                                      50 20)))))

;;;;;;;;;;;;
;; Bridge ;;
;;;;;;;;;;;;

;; Rebind these for brevity.

(let [D_ext bridge-link-diameter
      [D D_add P] bridge-link-thread]

  (defn bridge-bearing [indexes]
    (let [[a b] (map #(conj % 0) (remove nil? (case-placed-shapes
                                               (fn [& endpoints]
                                                 (when-let [[x d] (place-boss? (set indexes) endpoints)]
                                                   (boss-place x d endpoints))))))
          t cover-thickness               ; Mount plate thickness
          ε 3/5                           ; Chamfer length
          R 6                             ; Fillet radius
          N (if draft? 10 20)             ; The resolution of the arch extrusion.

          ;; Flip a vector to point the right way, regardless of the
          ;; relative poisition of a and b.

          y (fn [v] (update v 1 (if (> (second b) (second a)) - +)))

          R' (* (Math/sqrt 2) R)
          R'' (- R' R)

          ;; Points a and b are the mount hole locations.  We go at
          ;; 45° for a bit to c (to bring the bearings closer
          ;; together), then straight up to c', then to b.

          δ (* -1/2 bridge-bearing-separation)
          c (mapv - a (y [δ δ 0]))
          c' (assoc c 0 (first b))

          ;; The offset of the arch (with respect to a), needed to
          ;; get proper fillets.

          O (y [(- (first b) (first a) R') (- R'' ε δ D) 0])

          ;; Points where additional needs to be placed and routed
          ;; out to create nice fillets.

          d (map + c' [0 (second O) 0] (y [(- R') (- δ D R'' ε) 0]))
          e (map + a (y [(+ (- (first b) (first a) R'') R) (+ (- (first b) (first a) R'') R) 0]))

          ;; A chamfered rectangular slice; the hulling kernel for
          ;; the arch.

          section (for [x [0 (+ ε ε)]]
                    (cube 1/10 (+ D D ε ε x) (- t x)))

          ;; A pre-chamfered disk; the hulling kernel for the mount.

          chamfered-disk #(union
                           (cylinder % t)
                           (cylinder (+ % ε) (- t ε ε)))

          ;; The reverse of the above in a way; to cut out fillets.

          fillet-cutout
          (fn [r & parts]
            (->> (list*
                  (translate p (cylinder (- r ε) h))
                  (->> (cylinder [(- r ε) (+ r ε)] (+ ε ε) :center false)
                       (rotate [(* 1/2 π (dec s)) 0 0])
                       (translate (map + p [0 0 (* 1/2 s h)]))
                       (for [s [-1 1]])))

                 ;; The above parts are not convex and need to be
                 ;; hulled separately.

                 (for [p parts :let [h (- t ε ε)]])
                 (apply map vector)
                 (map hull)))

          ;; Transform into place along the arch.

          xform #(->> %2
                      (translate [0 0 bridge-arch-radius])
                      (rotate [0 (* -1 %1 stand-tenting-angle) 0])
                      (translate (map +
                                      [0 0 (- bridge-arch-radius)]
                                      O))
                      (translate a))]
      (translate
       [0 0 (- cover-thickness)]

       (difference
        (union
         (for [p [a b]] (translate p @countersink-boss))

         (translate
          [0 0 (- 1 (/ t 2))]

          (let [h (- D t)
                D' (+ D ε ε)

                ;; A bent plate mounted on the bottom cover on one
                ;; end and providing a bearing for the clevis on
                ;; the other.

                plate
                (union
                 ;; Most of the plate.

                 (->> (apply union
                             (list*
                              (when (zero? i) (translate [R'' 0 0] section))
                              (when (= i N) (->> (chamfered-disk (+ D ε))
                                                 (translate [(- D') 0 0])))
                              section))

                      (xform (/ i N))      ; Transform into place.
                      (for [i (range (inc N))])
                      (partition 2 1)      ; Take pairs and hull.
                      (mapv hull)
                      union)

                 ;; Bottom cover mount parts.

                 (difference
                  (->> (chamfered-disk R)
                       (translate p)
                       (for [p [a c c' b]])
                       (partition 2 1)
                       (mapv hull)

                       ;; Some additional material, to be formed into
                       ;; fillets below.

                       (cons (->> (cube R' R' t)
                                  (translate c')
                                  (translate (y [(/ R' -2) (/ R' 2) 0]))))
                       (cons (->> (cube R'' R'' t)
                                  (translate d)
                                  (translate (y [(/ R'' 2) (/ R'' 2) 0]))))
                       (cons (->> (cube R' (* 3 R') t)
                                  (translate a)
                                  (translate (y [0 (* -2 R') 0]))))

                       (apply union))

                  (apply fillet-cutout R (for [Δ [[-50 0 0]
                                                  [0 0 0]
                                                  [50 50 0]]] (map + c (y [(- R') R' 0]) (y Δ))))
                  (apply fillet-cutout R (for [Δ [[50 50 0]
                                                  [0 0 0]
                                                  [0 -50 0]]] (map + e (y [R' (- R') 0]) (y Δ))))
                  (fillet-cutout R'' d)))

                ;; The parts that make up the bearing, to be hulled separately.

                parts (let [ε_2 (/ ε 2)]
                        [(translate [0 0 (- h ε_2)] (cylinder [D (+ D ε_2)] ε_2 :center false))
                         (translate [0 0 ε_2] (cylinder D (- h ε) :center false))
                         (cylinder [(- D ε_2) D] ε_2 :center false)])]

            (difference
             (->> (cond->> p                        ; Take each bearing part.

                    intersect? (#(->> %             ; Maybe extend along X.
                                      (translate [u 0 0])
                                      (for [u [0 50]])
                                      (apply hull)))

                    true (#(->> %                   ; Transform into place.
                                (translate [(- D') 0 (- (/ t -2) h)])
                                (xform 1)))

                    intersect? (intersection plate)) ; Maybe intersect with the plate.

                  (for [intersect? [true false]])   ; Bearing + intersection with plate
                  (apply hull)                      ; Hull together to form ribs.
                  (for [p parts])                   ; For all parts.
                  (apply union plate))

             ;; Pin bore cutouts

             (->> (countersink (/ (+ D bridge-bore-expansion) 2) (* 1/8 D) 50 50)
                  (rotate [(* π (dec u)) 0 0])
                  (translate [(- D') 0 (- (/ t 2) (* u D))])
                  (xform 1)
                  (for [u [0 1]]))))))

        ;; Mount screw countersinks

        (for [p [a b]]
          (->> (countersink (/ cover-countersink-diameter 2)
                            cover-countersink-height
                            50 50)
               (translate p)
               (translate [0 0 (- 1 t)])))))))


  ;; A clevis fork/eye head, in the style of DIN 71751.  Can have more
  ;; than one holes.  The angle φ determines the distance from
  ;; base (or previous hole) to hole, in such a way that the maximum
  ;; range of the joint is 180° + 2φ.  In other words you can move it,
  ;; up to φ degrees past the 90° mark.

  ;;          +--+          +--+---------
  ;;          |//|          |//|       .
  ;;          +--+          +--+---    .
  ;;          |  |          |  | .     .
  ;; ---------|  |          |  | D     .
  ;;  .   .   |  |          |  | .     .
  ;;  .   .   +--+          +--+---    .
  ;;  .   .   |//|          |//|       .
  ;;  .  l_1  |//|          |//|       .
  ;;  .   .   |//|          |//|      l_3
  ;;  .   .   |//|          |//|       .
  ;; l_2 -----+--+----------+--+       .
  ;;  .       |////////////////|       .
  ;;  .       +--+//////////+--+-----  .
  ;;  .          |//////////|      .   .
  ;;  .          |//////////|     l_4  .
  ;;  .          |//////////|      .   .
  ;; ------------+----------+------------
  ;;             |...D_ext..|

  (defn clevis [style φ & [n]]
    (let [ε 0.8                ; Chamfer legnth / fillet radius
          n' (or n 1)

          δ (* D (+ 1 (Math/tan φ)))
          δ' (* (dec n') (+ δ D))
          l_1 (+ ε δ)
          l_4 (* 4/3 D)
          l_2 (+ l_4 (/ D 2) l_1)
          l_3 (+ l_2 D 1)
          l_3' (+ l_3 δ')
          l_34' (/ (- l_3' l_4) 2)

          ;; Mirrored countersinks to form the bore.

          bore #(->> (countersink (+ (/ (+ D bridge-bore-expansion) 2)) (* 1/8 D) 50 50)
                     (rotate [(* π 1/2 (dec s)) 0 0])
                     (translate [0 0 (* % s D)])
                     (for [s [-1 1]])
                     (apply rotate [(/ π 2) 0 0])
                     (translate [0 0 l_2]))

          ;; Corner rounding geometry

          rounding #(->> (sphere (/ l_3 2))
                         (scale [1.05 1.05 1])
                         (translate [0 0 (* s %)])
                         (for [s [-1/2 1/2]]))]

      (if (= style :eye)
        ;; A clevis eye rod end.

        (-> (cylinder (/ D_ext 2) l_3 :center false) ; The body

            (difference                              ; Minus the edges and bore.

             (->> (sphere ε)
                  (translate [(* u 50)
                              (* v (+ 1 t) (+ (/ D 2) ε))
                              (- l_2 (* s (- l_1 ε)) (* t (+ (/ D 2) ε)))])
                  (for [s [-1 1] t [0 1] u [-1 1]])
                  (apply hull)
                  (for [v [-1 1]]))

             (bore -1/2))

            (intersection (apply hull (rounding l_3)))) ; Corner rounding

        ;; A clevis fork rod end.

        (apply
         difference

         (->> (intersection
               (apply cube (map (partial * 2) [D D l_34']))   ; The body

               (->> (apply cube                               ; Edge chamfering
                           (map #(- (* 2 % (Math/sqrt 2)) ε) [D D 100]))
                    (rotate [0 0 (/ π 4)]))

               (if (= n' 1)                             ; Corner rounding
                 (apply intersection (rounding (- l_4 δ')))
                 (apply hull (rounding (- l_4 δ')))))

              (translate [0 0 l_34'])
              (hull (cylinder (/ D_ext 2) 1 :center false))
              (translate [0 0 l_4])
              (union                         ; The neck
               (cylinder (/ D_ext 2) l_4 :center false)))

         ;; Slot

         (->> (sphere ε)
              (translate [(* u 50) (* t (- (/ D 2) ε)) (+ l_2 (- l_1) (* s 100) ε)])
              (for [s [0 1] t [-1 1] u [-1 1]])
              (apply hull))

         ;; Bore(s)

         (for [i (range n')]
           (translate [0 0 (* i (+ δ D))] (bore -1)))))))

  (defn rod-end [style L & rest]
    (let [φ (degrees 45)        ; Essentially the "overhang" angle.
          ε -0.4                ; Chamfer legnth
          j 3                   ; Journal length
          n 1                   ; Neck length

          D_eff (+ D D_add)

          ;; h(d): height of a cone of base diameter d and opening
          ;; angle 2φ.

          h (fn [d] (/ d 2 (Math/tan φ)))

          ;; x(w): extended base diameter of a cone with a (lateral)
          ;; gap of w relative to a cone of base diameter d.

          x (fn [d w] (+ d (/ w 1/2 (Math/cos φ))))

          g 3/16                         ; Gap
          D_1 (+ D 0)                    ; Journal diameter
          D_2 (- D 1)                    ; Neck diameter
          L_1 (+ L (h (- D_eff D_2)))    ; Journal bottom
          L_1' (+ L_1 (- j n))           ; Journal top
          L_2 (+ L_1' (+ (h (- (+ D_1 D_ext) (* 2 D_2))) n))

          ;; Create two versions of the joint geometry.  One, suitably
          ;; extended by g, will serve as a cut-out.

          [M F]
          (for [δ [0 g]
                :let [D' (x D_1 δ)
                      D_ext' (x D_ext δ)
                      r (/ D' 2)]]
            (union
             (->> (cylinder [0 r] (h D') :center false) ; Lower journal taper
                  (translate [0 0 (- L_1 (h D'))]))
             (->> (cylinder r (- L_1' L_1) :center false) ; Journal
                  (translate [0 0 L_1]))
             (->> (cylinder [r 0] (h D') :center false) ; Upper journal taper
                  (translate [0 0 L_1']))
             (->> (cylinder (/ (x D_2 δ) 2) (- L_2 L_1') :center false) ; Neck
                  (translate [0 0 L_1']))
             (->> (cylinder [0 (/ D_ext' 2)] (h D_ext') :center false)
                  (translate [0 0 (- L_2 (h D_ext'))]))))]

      (difference
       (if (= style :fixed)
         (->> (apply clevis rest)
              (translate [0 0 (- L 7)])
              (union (cylinder (/ D_ext 2) L :center false))
              (intersection (cylinder (mapv (partial + (/ D_ext 2) ε) [0 500]) 500 :center false)))
         (union
          (difference
           (union
            (let [H (- L_2 (/ g (Math/sin φ)) (h 2.4))
                  H_c (+ (/ D_ext 2) H ε)]
              ;; The rod, up to the joint, with some chamfering.

              (when (or true)
                (intersection
                 (cylinder (/ D_ext 2) H :center false)
                 (for [s [0 1]]
                   (->> (cylinder [H_c 0] H_c :center false)
                        (rotate [(* π s) 0 0])
                        (translate [0 0 (* H s)]))))))

            (->> (apply clevis rest)
                 (translate [0 0 (- 4 (* 3/2 D))])
                 (intersection (->> (apply cube (repeat 3 (* 10 D)))
                                    (translate [0 0 (* 5 D)])))
                 (translate [0 0 L_2])))
           F)

          ;; The joint geometry.

          (difference
           M
           ;; Hollow out the center with a suitably expanded cone, to
           ;; ensure that the joint axis is fully supported by the
           ;; conical end of the threaded section below it.

           (->> (cylinder [(/ (x D_eff g) 2) 0] (h (x D_eff g)) :center false)
                (translate [0 0 L])))))

       ;; The thread.  We make sure the tip is perfectly conical past
       ;; the specified height, so that it doesn't intefere with the
       ;; bearing geometry.

       (let [H (h D_eff)
             H' (+ L H)]
         (->> (thread D_eff P (+ L H P))
              (translate [0 0 (- P)])
              (intersection (cylinder [(* D_eff 1/2 H' (/ 1 H))  0] H' :center false))))

       ;; Lead-in chamfer

       (cylinder [(+ (/ D_eff 2) 3/2) (- (/ D_eff 2) 3/2)] 3))))


  (def bridge-cable-bracket
    (delay
     (let [ε 2/5
           ε' (+ ε ε)
           D' (+ D D)

           w 3
           φ (degrees 10)
           δ (* D (+ 2 (Math/tan φ)))

           y_0 (/ (+ (second bridge-bracket-size) w) 2)

           kernel (hull (cube w (+ 1/10 ε') (- D' ε'))
                        (cube (- w ε') 1/10 D'))

           loop (->> kernel
                     (translate (apply
                                 #(vector (/ (+ %1 w) 2) (/ (- %2 1) 2 s) 0)
                                 (if (even? t) bridge-bracket-size (rseq bridge-bracket-size))))
                     (rotate [0 0 (* 1/2 t π)])
                     (for [t (range 4) s [-1 1]])
                     cycle)]
       (-> (->> loop
                (partition 2 1)
                (take 8)
                (map (partial apply hull))
                (apply union))

           (union
            (->> (union (cylinder (- D ε) w) (cylinder D (- w ε ε)))
                 (rotate [(/ π 2) 0 0])
                 (translate [0 y_0 (* s δ)])
                 (for [s [0 2]])
                 hull))

           (union
            (->> kernel
                 (rotate [0 0 (/ π 2)])
                 (translate [(* s (- D t -2 ε ε)) y_0 t])
                 (for [s [-1 1] t [0 2]])
                 hull))

           (difference
            (->> (countersink (/ (+ D bridge-bore-expansion) 2) (/ D 16) 50 50)
                 (rotate [(/ π s 2) 0 0])
                 (translate [0 (+ y_0 (* 1/2 s w)) (* t δ)])
                 (for [s [-1 1] t [1 2]]))))))))

;;;;;;;;;;;;;;;;;;;;
;; Final assembly ;;
;;;;;;;;;;;;;;;;;;;;

(defn assembly [side & parts]
  ;; Take either the union or the difference of the keycaps with the
  ;; rest of the model.

  (let [left? (= side :left)
        place-part? (set parts)
        build-thumb-section? (or thumb-test-build? (not key-test-build?))

        difference-or-union (if interference-test-build? difference union)
        maybe-cut-out (if case-test-build?
                        (partial intersection (apply hull (case-placed-shapes case-test-cutout)))
                        identity)
        pcb-place-properly (partial pcb-place left?)]
    (mirror
     [(if left? 1 0) 0 0]
     (maybe-cut-out
      (difference-or-union
       (apply union (concat
                     (when place-keyswitches?
                       (concat
                        (key-placed-shapes keyswitch)
                        (when build-thumb-section?
                          (thumb-placed-shapes keyswitch))))

                     (when place-keycaps?
                       (concat
                        (key-placed-shapes keycap)
                        (when build-thumb-section?
                          (thumb-placed-shapes keycap))))))

       (when (place-part? :top)
         (apply
          (if case-color (partial color case-color) union)
          (concat
           ;; Main section

           @connectors
           (key-placed-shapes key-plate)

           ;; Thumb section

           (when build-thumb-section?
             (concat @thumb-connectors
                     (thumb-placed-shapes key-plate)))

           ;; Case walls

           (when-not (or thumb-test-build? key-test-build?)
             (list*
              (apply difference (apply union (case-placed-shapes screw-boss))
                     (case-placed-shapes screw-thread))
              (case-placed-shapes (partial wall-brace wall-sections)))))))

       (when (place-part? :bottom)
         (intersection
          (when case-test-build? (hull (case-placed-shapes case-test-cutout)))

          (let [δ 1/2]
            ;; Start with the bottom plate, formed by pie-like pieces
            ;; projected from a central point and remove slightly inflated
            ;; versions of the walls and screw bosses.

            (difference
             (union
              (translate [0 0 (- 1 cover-thickness)]
                         (case-placed-shapes cover-shard))
              (pcb-place-properly pcb-bosses))

             (pcb-place-properly pcb-connector-cutout)
             (pcb-place-properly @pcb-button-hole-cutout)

             (binding [wall-thickness (+ wall-thickness δ)
                       screw-boss-radius (+ screw-boss-radius (/ δ 2))
                       cover-navel nil]
               (doall
                (concat (case-placed-shapes cover-shard)
                        (case-placed-shapes screw-boss))))

             (translate [0 0 (- cover-countersink-height cover-thickness -1)]
                        (case-placed-shapes screw-countersink))))))

       (when place-pcb?
         (union
          (pcb-place-properly pcb)
          (pcb-place-properly
           (translate [0 (/ (second pcb-size) 2) 0]
                      pcb-harness-bracket))))

       (when (or (place-part? :stand)
                 (place-part? :boot))
         (let [n (Math/ceil (/ stand-tenting-angle π (if draft? 2/180 1/180)))
               ;; Extrude the stand section polygon rotationally
               ;; through stand-tenting-angle, forming a "column"
               ;; of each section edge.

               columns (apply map vector
                              (for [i (range (inc n))]
                                (partition 2 1 (stand-section (/ i n)
                                                              (translate
                                                               [0 0 -1/20]
                                                               (cylinder (/ wall-thickness 2) 1/10))))))

               ;; Split it into the parts that will be cut out and the
               ;; part that will be left whole.

               [part-a rest] (split-at (first stand-split-points) columns)
               [part-b part-c] (split-at (- (second stand-split-points)
                                            (first stand-split-points)) rest)

               ;; Optionally shorten the upper and/or lower portion of
               ;; the to be cut out parts and hull.

               shorten (fn [part & limits]
                         [(map-indexed #(if (>= %1 (first limits)) (drop (quot n 2) %2) []) part)
                          (map-indexed #(if (>= %1 (second limits)) (take (quot n 2) %2) []) part)])

               hulled-parts
               (for [part (concat
                           [part-b]
                           (apply shorten part-a (take 2 stand-arm-lengths))
                           (apply shorten (reverse part-c) (drop 2 stand-arm-lengths)))]
                 (for [column part
                       [[a b] [c d]] (partition 2 1 column)]
                   (union (hull a b c) (hull b c d))))]

           (translate
            [0 0 (- 1 cover-thickness)]

            (when (place-part? :stand)
              ;; Assemble the parts of the stand, cut out the arms,
              ;; then subtract the countersinks from the upper parts
              ;; only.

              (let [parts (list*
                           (case-placed-shapes stand-boss)
                           (first hulled-parts)

                           (for [i [1 3 0 2]
                                 :let [q (quot i 2)
                                       r (rem i 2)
                                       qq (- 1 q q)
                                       rr (- 1 r r)

                                       [take-this take-that] (cond->> [take take-last]
                                                               (pos? q) reverse)]]
                             (difference
                              (apply union (nth hulled-parts (inc i)))

                              (->> (sphere 1/2)
                                   (translate [0 0 (* rr (+ 1/2 (stand-minimum-thickness r)))])
                                   (stand-section (- 1 r)
                                                  (for [n [-100 100]
                                                        [t z] [[(/ wall-thickness -2) 0]
                                                               [100 (* (Math/tan stand-arm-slope) 100)]]
                                                        z_add [0 100]] [n (* qq t) (* rr (+ z z_add))]))
                                   (take-this (inc (stand-arm-lengths i)))
                                   (take-that 1)
                                   (apply hull)))))

                    [upper-parts lower-parts] (split-at 4 parts)]

                (apply union
                       (difference
                        (apply union upper-parts)
                        (case-placed-shapes stand-boss-cutout))
                       lower-parts)))

            (when (place-part? :boot)
              (difference
               ;; Take the difference of lower 10° or so of the
               ;; extruded arm, with the hulling kernel inflated or
               ;; deflated, so as to arrive at a shell of width (apply
               ;; min boot-wall-thickness).  Also extend the height of
               ;; the kernel by boot-bottom-thickness at the bottom
               ;; section, to form the boot floor.

               (apply
                difference
                (for [δ boot-wall-thickness
                      :let [n (Math/ceil (/ stand-tenting-angle π 1/180))
                            columns (->> (cylinder (+ (/ wall-thickness 2) δ) h)
                                         (translate [0 0 (/ h -2)])
                                         (stand-section (/ (- n i) n))
                                         (drop (stand-arm-lengths 0))
                                         (drop-last (stand-arm-lengths 2))
                                         (partition 2 1)
                                         (for [i ((if draft?
                                                    identity (partial apply range))
                                                  [0 (if (pos? δ) (quot n 2) n)])
                                               :let [h (if (and (zero? i) (pos? δ))
                                                         boot-bottom-thickness 1/10)]])
                                         (apply map vector))]]
                  (union
                   (for [column columns
                         [[a b] [c d]] (partition 2 1 column)]
                     (union (hull a b c) (hull b c d))))))

               (->> (cube 1000 1000 1000)
                    (translate [0 0 (+ 500 1/10 boot-wall-height)])
                    (stand-xform (- stand-tenting-angle)))

               (case-placed-shapes stand-boss-cutout))))))

       (when (place-part? :bridge)
             ;; Bridge mount

             (->> bridge-boss-indexes
                  (partition 2 1)
                  (map bridge-bearing)
                  (apply union))))))))

(def prototype
  (delay))

(defn -main [& args]
  ;; Process switch arguments and interpret the rest as parts to
  ;; build.

  (let [parts (doall
               (filter
                (fn [arg]
                  (let [groups (re-find #"--(no-)?(.*?)(=.*)?$" arg)]
                    (if-let [[_ no k v] groups]
                      (if-let [p (find-var
                                  (symbol
                                   "lagrange-keyboard.core"
                                   (cond-> k
                                     (not v) (str \?))))]

                        (and (alter-var-root p (constantly
                                                (cond
                                                  v (eval (read-string (subs v 1)))
                                                  no false
                                                  :else true)))
                             false)

                        (println (format "No parameter `%s'; ignoring `%s'." k arg)))
                      true)))
                args))

        xform #(->> %1
                    (translate [0 0 (- cover-thickness)])
                    (rotate [0 (%2 stand-tenting-angle) 0])
                    (translate [(- (%2 @stand-baseline-origin)) 0 0]))]

    (alter-var-root #'scad-clj.model/*fa* (constantly (if draft? 12 3)))
    (alter-var-root #'scad-clj.model/*fs* (constantly (if draft? 2 2/10)))

    (doseq [part parts]
      (case part
        "right" (spit "things/right.scad"
                      (write-scad (assembly :right :top)))
        "right-cover" (spit "things/right-cover.scad"
                            (write-scad (assembly :right :bottom)))
        "right-stand" (spit "things/right-stand.scad"
                            (write-scad (xform (assembly :right :stand) +)))
        "right-boot" (spit "things/right-boot.scad"
                           (write-scad (xform (assembly :right :boot) +)))
        "right-subassembly" (spit "things/right-subassembly.scad"
                            (write-scad (assembly :right :top :bottom)))
        "right-assembly" (spit "things/right-assembly.scad"
                               (write-scad (xform (assembly :right :top :bottom :stand) +)))
        "left" (spit "things/left.scad"
                     (write-scad (assembly :left :top)))
        "left-cover" (spit "things/left-cover.scad"
                           (write-scad (assembly :left :bottom)))
        "left-stand" (spit "things/left-stand.scad"
                           (write-scad (xform (assembly :left :stand) -)))
        "left-boot" (spit "things/left-boot.scad"
                          (write-scad (xform (assembly :left :boot) -)))
        "left-subassembly" (spit "things/left-subassembly.scad"
                            (write-scad (assembly :left :top :bottom)))
        "left-assembly" (spit "things/left-assembly.scad"
                              (write-scad (xform (assembly :left :top :bottom :stand) -)))

        ;; For other mixes of parts that may be useful during development, but not
        ;; provided by the above.

        "custom-assembly" (spit "things/custom-assembly.scad"
                                (write-scad (assembly :right :bottom :stand)))

        ;; For arbitrary experimentation.

        "prototype" (spit "things/prototype.scad" (write-scad @prototype))

        (cond
          ;; Bridge

          (clojure.string/starts-with? part "bridge/")
          (let [subpart (subs part 7)
                scad (case subpart
                       "bracket" @bridge-cable-bracket
                       "left-mount" (assembly :left :bridge)
                       "right-mount" (assembly :right :bridge)

                       "toe-fork" (let [[x l φ] bridge-toe-fork]
                                    (rod-end x l, :fork φ))

                       "toe-eye" (let [[x l φ] bridge-toe-eye]
                                   (rod-end :fixed l, :eye φ))

                       "separation-fork" (let [[x l φ] bridge-separation-fork]
                                           (rod-end x l, :fork φ 2))

                       (println (format "No part `%s'." subpart)))]
            (when scad
              (spit "things/bridge.scad" (write-scad scad))))

          ;; Miscellaneous parts (mostly keycaps).

          (clojure.string/starts-with? part "misc/")
          (let [


                subpart (subs part 5)
                scad (case subpart
                       ;; This is used as a convenient location to tie
                       ;; the harness, in order to provide strain relief
                       ;; for the wiring exiting the PCB.

                       "bracket" pcb-harness-bracket

                       ;; Printable keycaps.

                       "oem-1u-recessed" (apply printable-keycap [1 1]
                                                keycap-family-oem-recessed)

                       "oem-1u" (apply printable-keycap [1 1]
                                       keycap-family-oem-flush)

                       "oem-1.5u" (apply printable-keycap [3/2 1]
                                         keycap-family-oem-flush)

                       "oem-1.5u-recessed" (apply printable-keycap [3/2 1]
                                                  keycap-family-oem-recessed)

                       "dsa-1u-convex" (apply printable-keycap [1 1]
                                              keycap-family-dsa-convex)
                       "dsa-1u-concave" (apply printable-keycap [1 1]
                                               keycap-family-dsa-concave)
                       "dsa-1.25u-convex" (apply printable-keycap [1 5/4]
                                                 keycap-family-dsa-convex)
                       "dsa-1.5u-convex" (apply printable-keycap [1 3/2]
                                                keycap-family-dsa-convex)
                       "dsa-1.5u-convex-homing" (apply printable-keycap [1 3/2]
                                                       :homing-bar-height 1/4
                                                       keycap-family-dsa-convex)
                       "sa-1.5u-concave" (apply printable-keycap [3/2 1]
                                                keycap-family-sa-concave)
                       "sa-1.5u-saddle" (apply printable-keycap [3/2 1]
                                               keycap-family-sa-saddle)
                       "dsa-1u-fanged" (apply printable-keycap [0.95 1]
                                              keycap-family-dsa-fanged)
                       (println (format "No part `%s'." subpart)))]
            (when scad
              (spit "things/misc.scad" (write-scad scad))))
          :else (println (format "No part `%s'." part)))))))
