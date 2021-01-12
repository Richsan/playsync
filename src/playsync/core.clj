(ns playsync.core
  (:gen-class)
  (:require [clojure.core.async :as async
             :refer [>! <! >!! <!! onto-chan! go go-loop chan buffer close! thread
                     timeout put! take! pub sub dropping-buffer
                     sliding-buffer poll! mult tap alt!!]]))


;go block—runs concurrently on a separate thread.
; Go blocks run your processes on a thread pool that contains a number of threads
; equal to 2 + the number of cores on your machine

;<!! = take function
;>!! = put function

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn thread-id []
  "A helper function to get the current thread -id"
  (-> (Thread/currentThread)
    (.getId)))

(println "Main Thread id = " (thread-id))

(defn print-val [val]
  "A helper function to print value showing the thread id"
  (println "Thread - " (thread-id) " - " val))

(defn put-in-a-non-buffered-channel-blocks []
  "this code blocks your repl in a infinite loop.
  You should restart your REPL after call this function"
  (let [c (chan)]
    (>!! c "will block indefinitelly")
    (println "Never will be executed")))

(defn take-value-blocks-until-production []
  "this code blocks your repl in a infinite loop.
   will block because nobody will never produce to this channel
  You should restart your REPL after call this function"
  (let [c (chan)]
    (<!! c)
    (println "Never will be executed")))

(defn put-in-a-buffered-channel-blocks []
  "this code blocks your repl in a infinite loop.
  You should restart your REPL after call this function"
  (let [c (chan 1)]
    (>!! c 1)
    (println "will not block because channel has a buffer of lenght 1")
    (>!! c 2)
    (println "Never will be executed because the buffer has more than 1 item")))

(defn put-in-a-dropping-buffer-chanel []
  "dropping buffer discard subsequent puts when buffer is full"
  (let [c (chan (dropping-buffer 1))]
    (>!! c 1)
    (>!! c 2)
    (println "Will not block but just the first one put succeed")
    (println "Val inside channel -> " (poll! c))
    (println "Has more value? " (boolean (poll! c)))))

(defn put-in-a-sliding-buffer-chanel []
  "sliding buffer discard previous puts when buffer is full"
  (let [c (chan (sliding-buffer 1))]
    (>!! c 1)
    (>!! c 2)
    (println "Will not block but just the last one put succeed")
    (println "Val inside channel -> " (poll! c))
    (println "Has more value? " (boolean (poll! c)))))

(defn put!-not-blocks-but-buffers-the-value []
  "Put! function tries to put value in channel and if the channel's buffer
  is full, they will buffer the value in its own buffer queue to put the value later
  when the channel will be available.
  Put! function never blocks."
  (let [c (chan)]
    (put! c 1)
    (put! c 2)
    (println "Consuming first value - " (<!! c))
    (println "Consuming second value - " (<!! c))))

(defn put!-exceeding-buffer-size []
  "The put! buffer size is 1024, if we exceed this size, an exception will be throwed"
  (let [c (chan)]
    (dotimes [i 1025]
      (put! c i))))

(defn put!-droping-buffer []
  "If the channel has a dropping buffer all put will succeed
  but all subsequent puts will be dropped"
  (let [c (chan (dropping-buffer 1))]
    (dotimes [i 1025]
      (put! c i))
    (println "All 1 to 1024 put was dropped.Val inside ->" (<!! c))))

(defn put!-sliding-buffer []
  "If the channel has a sliding buffer all put will succeed
  but just the last ones wil remain in the buffer"
  (let [c (chan (sliding-buffer 1))]
    (dotimes [i 1025]
      (put! c i))
    (println "All 0 to 1023 values was dropped.Val inside-> " (<!! c))))

(defn take!-also-has-a-buffer []
  "The take! function also has its own buffer with same size than put! buffer"
  (let [c (chan)]
    (thread (dotimes [i 5000]
              (put! c i)))

    (dotimes [_ 5000]
      (take! c #(print-val %)))))

(defn put-in-a-non-buffered-channel-separated-thread []
  "Using a separated thread avoid block the REPL's thread"
  (let [c (chan)]

    (thread (>!! c "will block an other thread")
      (println "Unblocked the thread"))
    (println "Will be executed")
    (println "Taking the value to unblock the thread")
    (println (<!! c))))

(defn consuming-from-separated-thread []
 "If we have a dedicated thread to consume channel value, we can be unblocked"
  (let [c (chan)]
    (thread (println "Consuming from thread -id " (thread-id))
      (print-val (<!! c)))
    (>!! c "Hello World!")
    (print-val "Will be executed after the channel be consummed")))

(defn consuming-from-go-block []
  "We can also use go blocks insted dedicated threads
  to be executed asynchronously by an event loop"
  (let [c (chan)]
    (go (println "Consuming from go block in thread -id " (thread-id))
        (print-val (<! c)))
    (>!! c "Hello World!")
    (print-val "Will be executed after the channel be consummed")))

(defn go-block-event-loop-thread-dynamic []
  "This experiment shows that a go block could be executed
   by more than one thread"
  (let [c (chan)]
    (go (while true (let [val          (<! c)
                          can-proceed? (even? val)]
                      (println "Start executing in thread " (thread-id))
                      (if can-proceed?
                        (print-val val)
                        (do (<! (timeout 1000))
                            (print-val val))))))
    (>!! c 1)))

(defn go-block-threads-usage []
  "Showing how go block just makes usage of a limited set of threads"
  (let [c (chan)]
    (go (dotimes [i 30]
          (when (even? i) (<! (timeout 1000)))
          (print-val (<! c))))
    (dotimes [i 30]
      (>!! c i))))

(defn mult-chan []
  "Multiplies content of a channel into two different channels"
  (let [c (chan)
        tap1 (chan)
        tap2 (chan)
        c-mult (mult c)]
    (tap c-mult tap1)
    (tap c-mult tap2)
    (>!! c "Hello")
    (print-val (str "consumed from tap1 -> " (<!! tap1)))
    (print-val (str "consumed from tap2 -> " (<!! tap2)))))

(defn pub-sub []
  "Demonstrating pub sub mechanism with topic by cats and dogs"
  (let [input-chan (chan)
        p (pub input-chan :topic)
        dog-chan (chan)
        cat-chan (chan)]
    (sub p :cats cat-chan)
    (sub p :dogs dog-chan)
    (go-loop [] (print-val (:content (<! cat-chan))) (recur))
    (go-loop [] (print-val (:content (<! dog-chan))) (recur))
    (>!! input-chan {:topic :cats :content "meow meow"})
    (>!! input-chan {:topic :dogs :content "woof woof"})))

(defn alt-wacky-race []
  "An wacky race with alt to describe how we can select from multiple channels"
  (let [mutley           (timeout (* 1000 (rand-int 3)))
        penelope-pitstop (timeout (* 1000 (rand-int 3)))
        rock-slag        (timeout (* 1000 (rand-int 3)))]
    (alt!!
      mutley :mutley
      penelope-pitstop :penelope-pitstop
      rock-slag :rock-slag)))


