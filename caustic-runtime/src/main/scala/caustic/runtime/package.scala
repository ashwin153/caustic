package caustic

package object runtime {

  // All keys are strings, which the library uses to store values in the database.
  type Key = String
  val Key = String

  // All version numbers are 64 bit integers, which the library uses for optimistic concurrency.
  type Version = Long
  val Version = Long

}

