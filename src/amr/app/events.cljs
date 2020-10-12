(ns amr.app.events
  (:require [amr.app.db :as db]
            [amr.utils :as utils]
            [ajax.core :as ajax]
            [clojure.spec.alpha :as s]
            [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx inject-cofx path after debug]]
            [reitit.frontend.controllers :as reitit.contollers]
            [reitit.frontend.easy :as reitit.easy]))

;;; HELPERS ;;;

(defn set-title! [route]
  (let [route-title (get-in route [:data :title])]
    (set! (.-title js/document)
          (cond->> "Parliament of Species"
            route-title (str route-title " | " )))))

;;; INTERCEPTORS ;;;

(defn- check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`.
  SOURCE: re-frame docs."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :multiverse.db/db)))
(def spec-interceptor [check-spec-interceptor])

(def ->local-storage (after db/collections->local-storage))
(def local-storage-interceptor [->local-storage])

;; (reg-event-fx
;;  :initialize-db
;;  [(inject-cofx :local-store-collections) spec-interceptor]
;;  (fn [{:keys [local-store-collections]} [_ default-db]]
;;    {:db (utils/?assoc default-db :stories local-store-collections)}))

;;; EFFECTS ;;;

(reg-fx
 ::navigate!
 (fn [route]
   (set-title! route)
   (apply reitit.easy/push-state route)))

;;; EVENTS ;;;

(reg-event-fx
 ::initialize-db
 (fn [_ [_ default-db]]
   {:db default-db}))

;; (reg-event-db
;;  ::active-page
;;  (fn [db [_ page]]
;;    (assoc-in db [:app :active-page] page)))

(reg-event-fx
 ::navigate
 (fn [_ [_ & route]]
   {::navigate! route}))

(reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (let [old-match (-> db :app :current-route)
         controllers (reitit.contollers/apply-controllers (:contollers old-match) new-match)]
     (set-title! new-match) ;; WARN This now does two things, which I dislike
     (assoc-in db [:app :route] (assoc new-match :contollers controllers)))))