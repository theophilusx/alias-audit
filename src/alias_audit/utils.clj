(ns alias-audit.utils)

(defn remove-keys [m ks]
  (reduce dissoc m ks))
