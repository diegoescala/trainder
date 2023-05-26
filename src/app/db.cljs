(ns app.db
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]))

(def app-db-spec
  (ds/spec {:spec {:settings {:theme (s/spec #{:light :dark})}
                   :version  string?}
            :name ::app-db}))

(def default-app-db
  {:settings {:theme :dark}
   :version  "version-not-set"})

(defn init-events
  []
  (rf/reg-event-db
     :init
     (fn [db [_ _]]
       (assoc db :logged-in false
                 :test-mode {:active false}      ; Should always be false in production
                 :login-errors []
                 :user-token nil
                 :user-type 0
                 :selected-client nil
                 :selected-provider nil
                 :preview-data)))

  (let [fetchers
        [:authenticate
         :logout
         :fetch-file-list
         :fetch-client-list
         :fetch-client-data
         :fetch-provider-list
         :fetch-provider-data
         :upload-file]]
    (doseq [fetcher fetchers]
      (rf/reg-event-fx
        fetcher
        [(rf/inject-cofx fetcher)]
        (fn [cofx event]
          ;(println (str "Fetch: " (prn-str [cofx event])))
          {:db (:db cofx)}))))

  (let [states
        [:user
         :logged-in
         :view
         :login-errors
         :selected-client
         :selected-provider
         :provider-list
         :client-list
         :client-data
         :active-file-list
         :preview-data
         :queued-files]]
    (doseq [e states]
      (rf/reg-event-db
        (keyword (str "set-" (name e)))
        (fn [db [_ param]]
          ;(println (str "Setting state: " (prn-str [e param])))
          (assoc db e param)))
      (rf/reg-sub e (fn [db _] (get db e)))))

  (rf/dispatch [:init]))
