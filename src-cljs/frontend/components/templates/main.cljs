(ns frontend.components.templates.main
  "Provides a template for the root of a normal page."
  (:require [frontend.components.aside :as aside]
            [frontend.components.header :as header]
            [frontend.state :as state]
            [frontend.utils.seq :refer [dissoc-in]]
            [om.core :as om :include-macros true])
  (:require-macros [frontend.utils :refer [html]]))

(defn template
  "The template for building a page in the app.

  :app            - The entire app state.
  :main-content   - A component which forms the main content of the page, which
                    is everything below the header.
  :crumbs         - Breadcrumbs to display in the header. Defaults to
                    (get-in app state/crumbs-path), but this is deprecated.
  :header-actions - A component which will be placed on the right in the
                    header. This is used for page-wide actions.
  :sidebar        - (optional) Content for the sidebar area. If missing, there
                    will be no sidebar."
  [{:keys [app main-content crumbs header-actions sidebar]}]
  (html
   (let [outer? (contains? #{:landing :error} (:navigation-point app))
         ;; simple optimzation for real-time updates when the build is running
         app-without-container-data (dissoc-in app state/container-data-path)]
     ;; Outer gets just a plain div here.
     [(if outer? :div :main.app-main)
      (om/build header/header {:app app-without-container-data
                               :crumbs (or crumbs (get-in app state/crumbs-path))
                               :actions header-actions})

      [:div.app-dominant
       (when sidebar
         [:aside.app-aside
          [:nav.aside-left-menu
           sidebar]])
       [:div.main-body
        main-content]]])))
