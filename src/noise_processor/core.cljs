(ns noise-processor.core)

(enable-console-print!)

(def   TWO_PI (* 2.0 Math/PI))
(def   buffer-size (* 2 8192))

(def  lut-cos
  (js/Float32Array.
   (into-array
    (take buffer-size (map #(.cos js/Math %)
                           (range 0 TWO_PI (/ TWO_PI buffer-size)))))))

(derive js/Number ::number)
(derive cljs.core/PersistentVector ::vector)
(derive js/ScriptProcessorNode ::audio-node)
(derive js/AudioBufferSourceNode ::audio-node)


(def ac
  (let [context
        (or js/window.AudioContext
            js/window.webkitAudioContext)]
    (context.)))

(defprotocol AudioNodeExtrasFactory
  (make-dc [ac x])
  (make-sin-osc [ac])
  ;;(make-noise)
  ;;(make-mult [ac x])
  )

(extend-type js/AudioContext
  AudioNodeExtrasFactory
  (make-dc [ac x]
    (let [buffer-size 64;(* 2 8192)
          buffer (.createBuffer ac 1 buffer-size (.-sampleRate ac))
          channel-data (.getChannelData buffer 0)
          dc-node (.createBufferSource ac)]
      (do
        (dotimes [i (.-length channel-data)]
          (aset channel-data i x))
        (set! (.-buffer dc-node) buffer)
        (.start dc-node)
        (set! (.-loop dc-node) true)
        dc-node)))

  (make-sin-osc [ac]
    (let [n (atom 0)
          lut lut-cos
          step (/ (.-length lut) (.-sampleRate ac))
          sin-processor (.createScriptProcessor ac buffer-size 2 1)]
      (do
        (set! (.-onaudioprocess sin-processor)
              (fn [e]
                (let [freq (-> e .-inputBuffer (.getChannelData 0))
                      phase (-> e .-inputBuffer (.getChannelData 1))
                      output (-> e .-outputBuffer (.getChannelData 0))]
                  (dotimes [i (.-length output)]
                    (let [freq' (aget freq i)
                          phase' (* (aget phase i) buffer-size)
                          index (-> @n (* freq')
                                    (+ phase') (mod buffer-size)
                                    (int) (Math/abs))]
                      (aset output i (aget lut index)))
                    (swap! n #(mod (+ % step) buffer-size))))))
        sin-processor)))

  ;;(make-mult [ac x])
  )


(defmulti out (fn [channels in] (type channels)))

(defmethod out ::number [_ in]
  (.connect in (.-destination ac))
  in)

(defmethod out ::vector [channels signal]
  (print "to do"))

(defn dc [x]
  (make-dc ac x))

 (defmulti sin-osc
   (fn [freq & [phase & _]] [(type freq) (type phase)]))

(defn- merge! [merger osc freq phase]
  (do
    (.connect freq merger 0 0)
    (.connect phase merger 0 1)
    (.connect merger osc)
    osc))


(defmethod sin-osc [::audio-node ::audio-node]
  [freq & [phase & _]]
  (let [merger (.createChannelMerger ac 2)
        osc (make-sin-osc ac)]
    (merge! merger osc freq phase)))

(defmethod sin-osc [::number ::audio-node]
  [freq & [phase & _]]
  (let [merger (.createChannelMerger ac 2)
        osc (make-sin-osc ac)
        freq (dc freq)]
    (merge! merger osc freq phase)))

(defmethod sin-osc [::number nil]
  [freq & [phase & _]]
  (let [merger (.createChannelMerger ac 2)
        osc (make-sin-osc ac)
        phase (dc 0)
        freq (dc freq)]
    (merge! merger osc freq phase)))

(defmethod sin-osc [::number ::number]
  [freq & [phase & _]]
  (let [merger (.createChannelMerger ac 2)
        osc (make-sin-osc ac)
        freq (dc freq)
        phase (dc phase)]
    (merge! merger osc freq phase)))


(defn hold [in time]
  (js/setTimeout #(.disconnect in) (* 1000 time))
  in)

(defn demo [time in]
  (out 0 (hold in time)))

;;(demo 15 (sin-osc 110 (sin-osc 220)))
