package com.overdrive.catalog.app

/** Thrown when a catalog entity delete is blocked by referencing entities. */
class CatalogReferentialIntegrityException(message: String) : RuntimeException(message)
