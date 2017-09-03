-- :name get-identity-status :? :1
-- :doc Return the current status for an identity
SELECT status, id_category
FROM urs.identities
WHERE identity_name = :id

-- :name get-alias :? :1
-- :doc Return the current definition for an alias
SELECT alias_name, alias_type, status
FROM urs.email_aliases
WHERE alias_name = :alias

