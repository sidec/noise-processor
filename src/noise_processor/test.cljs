(ns noise-processor.test
  (:require [noise-processor.core :as np]))

(np/demo 15 (np/sin-osc 55 (np/sin-osc 110)))
