(ns app.index
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   ["@react-navigation/native" :as nav]
   ["@react-navigation/stack" :as rn-stack]
   ["expo" :as ex]
   ["expo-constants" :as expo-constants]
   ["expo-file-system" :as fs]
   ["react" :as react]
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["tailwind-rn" :default tailwind-rn]

   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]
   [shadow.expo :as expo]

   [app.helpers :refer [<sub >evt]]
   [app.state :as state]

   [cljsjs.react]
   [cljs-http.client :as http]
   [cognitect.transit :as transit]
   [cljs.core.async :refer [<!]]
   [alandipert.storage-atom :refer [local-storage]]
   [re-frame.core :as rf]
   [cljs.reader :as reader]
   [clojure.string :as s]))


(defn tw [style-str]
  ;; https://github.com/vadimdemedes/tailwind-rn#supported-utilities
  (-> style-str
      tailwind-rn
      (js->clj :keywordize-keys true)))

(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def scenes2 (r/atom []))

(defn read-json [data]
  (let [r (transit/reader :json)]
    (transit/read r data)))

(defn write-json [data]
  (let [w (transit/writer :json)]
    (transit/write w data)))

(defn jsonify-map [data-map]
  (->> data-map
       (clj->js)
       (.stringify js/JSON)))

(defn query-api-endpoint
  [method url params handler]
  (go
    (let [full-params (merge {:with-credentials? false :oauth-token "mykey"} params)]

      (println (str "Making call: " (prn-str [method url full-params])))
      (let [response (<! (method url full-params))]
        ; (println (str "Network response: " (prn-str response)))
        (when (= 401 (:status response))
          (rf/dispatch [:set-logged-in false]))

        (handler response)))))

(defn trainer-card [trainer]
  [:> paper/Card {:style {:margin 10 :margin-bottom 20 :height 150 :flex 1 :flex-direction :row :backgroundColor "#242424"}}
    [:> (.-Image paper/Avatar) {:source {:uri (:image trainer)}}]
    [:> (.-Content paper/Card) 
      [:> paper/Title (:name trainer)]
      [:> paper/Paragraph (:description trainer)]]
    [:> (.-Actions paper/Card)
      [:> paper/Button {:onPress #(>evt [:set-selected-trainer trainer])}
        "BOOK"]]])

(defn trainer-list [trainers]
 [:> rn/View
  (for [trainer (map-indexed vector trainers)]
    ^{:key (first trainer)}[trainer-card (second trainer)])])

(defn trainer-profile [trainer]
 [:> rn/View
    [trainer-card trainer]
    [:> paper/Button 
      {:onPress #(>evt [:set-selected-trainer nil])} 
      "CLOSE"]])

(defn trainers-main 
  []
  (let [sel-trainer (<sub [:selected-trainer])]
      (println "sel trainer" sel-trainer)
      (if (some? sel-trainer)
        [trainer-profile sel-trainer]
        [trainer-list [
                       {:image "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_D4hDQmQHV8BEXjwkniRyh97qZH79zEqyVg&usqp=CAU"
                        :name "John Doe"
                        :description "Personal trainer with 5 years of experience specializing in weight loss and muscle gain."}
                       {:image "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSzTY-OaR6SJn6bvqmqXJV65868zbVRJ_w9qQ&usqp=CAU"
                        :name "John Day"
                        :description "Personal trainer with 5 years of experience specializing in weight loss and muscle gain."}
                       {:image "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTpSN_UD9wV4-JC1jZm537oRXb1N2QIAsm14A&usqp=CAU"
                        :name "John Doe"
                        :description "Personal trainer with 5 years of experience specializing in weight loss and muscle gain."}]])))


(defn screen-main
  [props]
  (r/as-element
    [:> rn/SafeAreaView {:style {:flex 1}}
     [:> rn/StatusBar] ;{:visibility :hidden}]
     [:> paper/Surface {:style {:flex 1}}
      [:> rn/ScrollView
       [:> rn/View {:style {:flex 1}}
         [trainers-main]]]]]))

(def stack (rn-stack/createStackNavigator))

(defn navigator [] (-> stack (j/get :Navigator)))

(defn screen [props] [:> (-> stack (j/get :Screen)) props])

(defn root []
  (let [theme           (<sub [:theme])
        !route-name-ref (clojure.core/atom {})
        !navigation-ref (clojure.core/atom {})]

    (println "theme is" theme)
    [:> paper/Provider
     {:theme (case theme
               :light paper/DefaultTheme
               :dark  paper/DarkTheme
               paper/DarkTheme)}

     [:> nav/NavigationContainer
      {:ref             (fn [el] (reset! !navigation-ref el))
       :on-ready        (fn []
                          (println "Hello")
                          (swap! !route-name-ref merge {:current (-> @!navigation-ref
                                                                     (j/call :getCurrentRoute)
                                                                     (j/get :name))}))
       :on-state-change (fn []
                          (let [prev-route-name    (-> @!route-name-ref :current)
                                current-route-name (-> @!navigation-ref
                                                       (j/call :getCurrentRoute)
                                                       (j/get :name))]
                            (when (not= prev-route-name current-route-name)
                              ;; This is where you can do side effecty things like analytics
                              (>evt [:some-fx-example (str "New screen encountered " current-route-name)]))
                            (swap! !route-name-ref merge {:current current-route-name})))}

      [:> (navigator) {:header-mode "none"}
       (screen {:name      "Screen1"
                :component (paper/withTheme screen-main)})]]])) 

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(def version (-> expo-constants
                 (j/get :default)
                 (j/get :manifest)
                 (j/get :version)))

(defn init []
 (do
  (state/init-events)
  (start)))
