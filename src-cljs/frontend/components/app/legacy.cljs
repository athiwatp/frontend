(ns frontend.components.app.legacy
  (:require [frontend.components.admin :as admin]
            [frontend.components.aside :as aside]
            [frontend.components.dashboard :as dashboard]
            [frontend.components.enterprise-landing :as enterprise-landing]
            [frontend.components.errors :as errors]
            [frontend.components.insights :as insights]
            [frontend.components.landing :as landing]
            [frontend.components.org-settings :as org-settings]
            [frontend.components.pages.add-projects :as add-projects]
            [frontend.components.pages.build :as build]
            [frontend.components.pages.project-insights :as project-insights]
            [frontend.components.pages.project-settings :as project-settings]
            [frontend.components.pages.team :as team]
            [frontend.components.templates.main :as main-template]
            [frontend.config :as config]
            [frontend.state :as state]
            [frontend.utils.legacy :refer [build-legacy]]
            [om.core :as om :include-macros true]
            [om.next :as om-next :refer-macros [defui]]))

(defn- templated
  "Compatibility shim during transition to Component-Oriented pages. Takes an
  old-world \"dominant component\" function and returns a new-world page
  component function.

  As pages are rewritten to use Component-Oriented pages, they no longer need to
  be wrapped in this function."
  [old-world-dominant-component-f]
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (main-template/template
         {:app app
          :main-content (om/build old-world-dominant-component-f app)
          :sidebar (case (:navigation-point app)
                     :org-settings (om/build aside/org-settings-menu app)
                     :admin-settings (om/build aside/admin-settings-menu app)
                     ;; The dashboard doesn't show a sidebar when you're not
                     ;; logged in (OSS), when the projects or builds haven't
                     ;; loaded yet, or when you're not yet following any
                     ;; projects.
                     :dashboard (when (and (get-in app state/user-path)
                                           (get-in app state/recent-builds-path)
                                           (seq (get-in app state/projects-path)))
                                  (om/build aside/branch-activity-list app))
                     nil)})))))

(def nav-point->page
  (merge
   ;; Page component functions, which are good as they are.
   {:add-projects add-projects/page
    :build build/page
    :project-insights project-insights/page
    :project-settings project-settings/page
    :team team/page}
   ;; Old-World dominant component functions which need to be wrapped in the `main` template.
   ;; As we migrate these, we'll move them into the map above.
   (into {}
         (map #(vector (key %) (templated (val %))))
         {:dashboard dashboard/dashboard
          :build-insights insights/build-insights
          :org-settings org-settings/org-settings

          :admin-settings admin/admin-settings
          :build-state admin/build-state
          :switch admin/switch

          :landing (fn [app owner]
                     (reify
                       om/IRender
                       (render [_]
                         (om/build
                          (if (config/enterprise?) enterprise-landing/home landing/home)
                          app))))

          :error errors/error-page})))

(defui ^:once LegacyPage
  static om-next/IQuery
  (query [this]
    '[{:legacy/state [*]}])
  Object
  (render [this]
    (let [app (:legacy/state (om-next/props this))
          page (get nav-point->page (:navigation-point app))]
      (when page
        (build-legacy page app)))))
