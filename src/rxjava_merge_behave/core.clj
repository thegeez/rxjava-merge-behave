(ns rxjava-merge-behave.core
  (:import rx.Observable
           rx.subjects.Subject
           java.util.concurrent.atomic.AtomicLong))

;; merge is not well behaved as defined in "Reactive Extensions
;; Design Guidelines" section 6.7
;; http://go.microsoft.com/fwlink/?LinkID=205219

;; This may hold for other operators that combine multiple Observables as well

;; Call to onNext by observables should be synchronized, the following
;; example shows the desired behavior in Rx.Net:

;; (Example can be run with linqpad as C# statements)

;; Console.WriteLine("Starting example");
;; var o1 = Observable.Interval(TimeSpan.FromSeconds(1)).Select(i => "stream ONE" + i);
;; var o2 = Observable.Interval(TimeSpan.FromSeconds(2)).Select(i => "stream TWO" + i);

;; Observable.Merge(o1,o2).Subscribe(x => 
;;   { 
;;     if (x == "stream ONE0") { 
;;       Console.WriteLine("slow onNext processing start");
;;       Thread.Sleep(TimeSpan.FromSeconds(3));
;;       Console.WriteLine("slow onNext processing done");
;;     }
;;     Console.WriteLine("OnNext" + x);
;;   },
;;   e => Console.WriteLine("Error" + e),
;;   () => Console.WriteLine("Completed"));

;; // // Output with a long wait between processing start and done
;; // // No OnNextStream is interleaved
;; // Starting example
;; // slow onNext processing start
;; // slow onNext processing done
;; // OnNextstream ONE0
;; // OnNextstream TWO0
;; // OnNextstream ONE1
;; // OnNextstream ONE2
;; // OnNextstream ONE3
;; // OnNextstream TWO1
;; // OnNextstream ONE4
;; // OnNextstream ONE5
;; // OnNextstream TWO2
;; // // Etc...



(defn interval [secs]
  ;; same as Observable.Interval(TimeSpan.FromSeconds(1))
  (let [s (Subject/create)
        index (AtomicLong. 0)
        t (Thread. (fn []
                     (while true
                       (Thread/sleep (* secs 1000))
                       (.onNext s (.getAndIncrement index)))))]
    (.start t)
    s))

(defn -main [& args]
  (println "Starting example")
  (let [o1 (-> (interval 1)
               (.map (fn [i] (str "stream ONE" i))))
        o2 (-> (interval 2)
               (.map (fn [i] (str "stream TWO" i))))]
    (-> (Observable/merge [o1 o2])
        (.subscribe 
         (fn [x]
           (when (= x "stream ONE0")
             (println "slow onNext processing start")
             (Thread/sleep (* 3 1000))
             (println "slow onNext processing done"))
           (println "onNext" x))
         (fn [e] (println "onError" e))
         (fn [] (println "onCompleted"))))))


;; ;; clojure output:
;; ;; the "onNext stream TWO0" shows that the onNext calls by a merge
;; ;; observable are not synchronized
;; Starting example
;; slow onNext processing start
;; onNext stream TWO0
;; slow onNext processing done
;; onNext stream ONE0
;; onNext stream TWO1
;; onNext stream ONE1
;; onNext stream ONE2
;; onNext stream TWO2
;; onNext stream ONE3
;; onNext stream ONE4
