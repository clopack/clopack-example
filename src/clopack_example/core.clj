(ns clopack-example.core
  (:gen-class)
  (:require [clopack.core :refer [create-handler run-handler]]
            [clopack-native.core :as cn]))

(def my-interface "em0")

(def my-form [:sender     32  :string
              :recipient  32  :string
              :message    512 :string])

(defn sender-oliver? [packet]
  (= (:sender packet) "oliver"))

(defn recipient-tanel? [packet]
  (= (:recipient packet) "tanel"))

(defn my-condition [packet]
  (and (sender-oliver? packet)
       (recipient-tanel? packet)))

(defn short-message? [packet]
  (< (count (:message packet)) 32))

(defn long-message? [packet]
  (not (short-message? packet)))

(defn short-message-action [packet]
  (let [sender  (:sender packet)
        message (:message packet)]
  (printf "got a short message from %s: %s" sender message)))

(defn long-message-action [packet]
  (let [sender  (:sender packet)
        message (:message packet)]
    (printf "LOOOONG message from %s: %s" sender message)))

(def my-actions [short-message? short-message-action
                 long-message?  long-message-action])

(defn in-handler [handler]
  (run-handler handler)
  (in-handler handler))

(defn out-handler [ctx sender recipient]
  (let [sender    (format "%-32s" sender)
        recipient (format "%-32s" recipient)
        message   (read-line)
        data (byte-array (map byte (concat sender recipient message)))]
    (if-let [num-written (cn/write-frame ctx data)]
      (out-handler ctx sender recipient))))

(defn -main
  [& args]
  (let [handler (create-handler my-interface
                                my-form
                                my-condition
                                my-actions)
        ctx (cn/create-context my-interface) ]
    (if-not (nil? handler)
      (future (in-handler handler))
      (printf "failed to open packet handler on %s\n" my-interface))
    (if-not (nil? ctx)
      (out-handler ctx "tanel" "oliver")
      (printf "failed to open packet writer on %s\n", my-interface))))
