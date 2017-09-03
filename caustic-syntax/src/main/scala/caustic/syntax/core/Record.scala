//package caustic.syntax.core
//
///**
// *
// * @param key
// */
//case class Record(key: Transaction)
//    extends Evaluable
//    with Assignable
//    with Deletable
//    with Chainable[Record] {
//
//  /**
//   *
//   * @return
//   */
//  def kind: Transaction = add(this.key, Record.Field, "__kind__")
//
////  /**
////   *
////   * @param to
////   * @return
////   */
////  def copy(to: Transaction): Transaction =
////    cons(
////      // Construct a stack to recursively process fields of a structure.
////      store("__stack__", add(this.key, Record.Array)),
////      store("__prefix__", to),
////
////      repeat(ne(load("__stack__"), Empty), cons(
////        // Pop the head of the stack and its kind.
////        store("__i__", indexOf(load("__stack__"), Record.Array)),
////        store("__head__", slice(load("__stack__"), Zero, load("__i__"))),
////        store("__kind__", add(load("__head__"), Record.Field, "__kind__")),
////        store("__stack__", slice(load("__stack__"), add(load("__i__"), length(Record.Array)))),
////
////        // Recurse on stuctures.
////        branch(equal(read(load("__kind__")), "structure"), cons(
////          // Prefix all fields with the head key.
////          store("__fields__", read(load("__head__"))),
////          store("__i__", Zero),
////
////          repeat(less(load("__i__"), length(load("__fields__"))), cons(
////            store("__next__", indexOf(slice(load("__fields__"), load("__i__")), Record.Array)),
////            store("__prefix__", slice(load("__fields__"), Zero, load("__i__"))),
////            store("__suffix__", slice(load("__fields__"), load("__next__"))),
////
////            store("__name__", slice(load("__fields__"), load("__i__"), load("__next__"))),
////            store("__key__", add(load("__head__"), Record.Field, load("__name__"))),
////            store("__fields__", add(load("__prefix__"), load("__key__"), load("__suffix__"))),
////            store("__i__", add(add(load("__next"), length("__key__")), length(Record.Array)))
////          )),
////
////          // Prefetch keys and append them to the stack.
////          prefetch(load("__fields__")),
////          store("__stack__", add(load("__stack__"), load("__fields__"), Record.Array))
////        )),
////
////        // Prepend the new field with the new key.
////        store("__key__", add(to, slice("__head__", length(this.key)))),
////        write(load("__key__"), read(load("__head__"))),
////        write(add(load("__key__"), Record.Field, "__kind__"), read(load("__kind__")))))
////    )
//
//  /**
//   *
//   * @return
//   */
//  override def get: Transaction = read(this.key)
//
//  override def dot(name: Transaction): Record =
//    Record(cons(
//      // If the field is a primitive, then convert it into a structure.
//      branch(equal(read(this.kind), Record.Attribute), cons(
//        write(this.kind, Record.Structure),
//        write(this.key, add(name, Record.Array))
//      )),
//
//      // If the field is a reference, then dereference it. Otherwise, return the field.
//      branch(equal(read(this.kind), "reference"),
//        add(this.key, Record.Field, name),
//        add(this.key, Record.Field, name)
//      )
//    ))
//
//  override def set(value: Evaluable): Transaction =
//    cons(
//      // Validate that all parent records are properly constructed.
//      store("$key", this.key),
//      repeat(contains(read(load("$key")), Record.Field), cons(
//        // Extract the parent record of the key.
//        store("$i", indexOf(load("$key"), Record.Array)),
//        store("$parent", slice(load("$key"), Zero, load("$i"))),
//
//        // Verify parent is a structure.
//        store("$kind", add(read(load("$parent")), Record.Field, "__kind__")),
//        branch(not(equal(read(load("$kind")), "structure")), cons(
//          write(load("$kind"), "structure"),
//          write(load("$parent"), Empty)
//        )),
//
//        // Verify parent contains field name.
//        store("$field", slice(load("$key"), add(load("$i"), length(Record.Array)))),
//        branch(not(contains(read(load("$parent")), load("$field"))),
//          write(load("$parent"), add(read(load("$parent")), Record.Array, load("$field")))
//        ),
//
//        // Recurse on parent record.
//        store("$key", load("$parent"))
//      )),
//
//      // Update the kind of the record.
//      store("$kind", Record.kind(value)),
//      branch(not(equal(read(this.kind), load("$kind"))), cons(
//        this.delete(false),
//        write(this.kind, load("$kind"))
//      )),
//
//      // Update the value of the record.
//      value match {
//        case Record(k) => branch(not(equal(this.get, k)), write(this.key, k))
//        case Value(x) => branch(not(equal(this.get, x)), write(this.key, x))
//        case Pointer(p) => branch(not(equal(this.get, p)), write(this.key, p))
//        case Variable(x) => branch(not(equal(this.get, load(x))), write(this.key, load(x)))
//      }
//    )
//
//    /**
//     *
//     * @param recursive
//     * @return
//     */
//    def delete(recursive: Boolean): Transaction =
//      cons(
//        // Construct a stack to recursively process fields of a structure.
//        store("__stack__", add(this.key, ",")),
//
//        repeat(ne(load("__stack__"), Empty), cons(
//          // Pop the head of the stack and its kind.
//          store("__i__", indexOf(load("__stack__"), Record.Array)),
//          store("__head__", slice(load("__stack__"), Zero, load("__i__"))),
//          store("__kind__", add(load("__head__"), Record.Field, "__kind__")),
//          store("__stack__", slice(load("__stack__"), add(load("__i__"), length(Record.Array)))),
//
//          // Recurse on stuctures.
//          branch(equal(read(load("__kind__")), "structure"), cons(
//            // Prefix all fields with the head key.
//            store("__fields__", read(load("__head__"))),
//            store("__i__", Zero),
//
//            repeat(less(load("__i__"), length(load("__fields__"))), cons(
//              store("__next__", indexOf(slice(load("__fields__"), load("__i__")), Record.Array)),
//              store("__prefix__", slice(load("__fields__"), Zero, load("__i__"))),
//              store("__suffix__", slice(load("__fields__"), load("__next__"))),
//
//              store("__name__", slice(load("__fields__"), load("__i__"), load("__next__"))),
//              store("__key__", add(load("__head__"), Record.Field, load("__name__"))),
//              store("__fields__", add(load("__prefix__"), load("__key__"), load("__suffix__"))),
//              store("__i__", add(add(load("__next"), length("__key__")), length(Record.Array)))
//            )),
//
//            // Prefetch keys and append them to the stack.
//            prefetch(load("__fields__")),
//            store("__stack__", add(load("__stack__"), load("__fields__"), Record.Array))
//          )),
//
//          // Optionally recurse on references.
//          branch(and(recursive, equal(read(load("__kind__")), "reference")),
//            store("__stack__", add(load("__stack__"), read(load("__head__")), Record.Array))
//          ),
//
//          // Delete the head and its kind.
//          write(load("__head__"), Empty),
//          write(load("__kind__"), Empty)
//        ))
//      )
//
//}
//
///**
// *
// */
//object Record {
//
//  //
//  val Field: String = "@"
//  val Array: String = ","
//
//  //
//  val Structure: String = "structure"
//  val Reference: String = "reference"
//  val Attribute: String = "attribute"
//
//  /**
//   *
//   * @param evaluable
//   * @return
//   */
//  def kind(evaluable: Evaluable): String = evaluable match {
//    case _: Record => Structure
//    case _: Pointer => Reference
//    case _: Variable => Attribute
//    case _: Variable => Attribute
//  }
//
//}
//
//
//
/////**
//// *
//// */
////object Record {
////
////  val Field: Transaction = "@"
////  val Array: Transaction = ","
////}
////
/////**
//// * A transactional database object. Records are associated with a key, which uniquely identifies the
//// * record in the database. Records store one of the three kinds of values (structure, primtiive, or
//// * reference). Exactly which kind it contains is stored in the __kind__ field of every record.
//// *
//// * @param key Unique database identifier.
//// */
////
/////**
//// *
//// * @param key
//// */
////case class Record(key: Transaction) {
////
////  /**
////   *
////   * @return
////   */
////  def kind: Transaction = add(this.key, Record.Field, "__kind__")
////
////  /**
////   *
////   * @return
////   */
////  def value: Transaction = read(this.key)
////
////  /**
////   *
////   * @param name
////   * @return
////   */
////  def get(name: Transaction): Record =
////    Record(cons(
////      // If the field is a primitive, then convert it into a structure.
////      branch(equal(read(this.kind), "primitive"), cons(
////        write(this.kind, "structure"),
////        write(this.key, add(name, Record.Array))
////      )),
////
////      // If the field is a reference, then dereference it. Otherwise, return the field.
////      branch(equal(read(this.kind), "reference"),
////        add(this.key, Record.Field, name),
////        add(this.key, Record.Field, name)
////      )
////    ))
////
////  /**
////   *
////   * @param value
////   * @return
////   */
////  def set(value: Symbol): Transaction =
////    cons(
////      // Validate that all parent records are properly constructed.
////      store("$key", this.key),
////      repeat(contains(read(load("$key")), Record.Field), cons(
////        // Extract the parent record of the key.
////        store("$i", indexOf(load("$key"), Record.Array)),
////        store("$parent", slice(load("$key"), Zero, load("$i"))),
////
////        // Verify parent is a structure.
////        store("$kind", add(read(load("$parent")), Record.Field, "__kind__")),
////        branch(not(equal(read(load("$kind")), "structure")), cons(
////          write(load("$kind"), "structure"),
////          write(load("$parent"), Empty)
////        )),
////
////        // Verify parent contains field name.
////        store("$field", slice(load("$key"), add(load("$i"), length(Record.Array)))),
////        branch(not(contains(read(load("$parent")), load("$field"))),
////          write(load("$parent"), add(read(load("$parent")), Record.Array, load("$field")))
////        ),
////
////        // Recurse on parent record.
////        store("$key", load("$parent"))
////      )),
////
////      // Update the kind of the record.
////      store("$kind", value match {
////        case Structure(_) => "Structure"
////        case Primitive(_) => "primitive"
////        case Reference(_) => "reference"
////      }),
////
////      branch(not(equal(read(this.kind), load("$kind"))), cons(
////        this.delete(false),
////        write(this.kind, load("$kind"))
////      )),
////
////      // Update the value of the record.
////      value match {
////        case Structure(r) => r.copy(this.key)
////        case Primitive(x) => branch(not(equal(this.value, x)), write(this.key, x))
////        case Reference(p) => branch(not(equal(this.value, p)), write(this.key, p))
////      }
////    )
////
////  /**
////   *
////   * @param to
////   * @return
////   */
////  def copy(to: Transaction): Transaction =
////    cons(
////      // Construct a stack to recursively process fields of a structure.
////      store("__stack__", add(this.key, Record.Array)),
////      store("__prefix__", to),
////
////      repeat(ne(load("__stack__"), Empty), cons(
////        // Pop the head of the stack and its kind.
////        store("__i__", indexOf(load("__stack__"), Record.Array)),
////        store("__head__", slice(load("__stack__"), Zero, load("__i__"))),
////        store("__kind__", add(load("__head__"), Record.Field, "__kind__")),
////        store("__stack__", slice(load("__stack__"), add(load("__i__"), length(Record.Array)))),
////
////        // Recurse on stuctures.
////        branch(equal(read(load("__kind__")), "structure"), cons(
////          // Prefix all fields with the head key.
////          store("__fields__", read(load("__head__"))),
////          store("__i__", Zero),
////
////          repeat(less(load("__i__"), length(load("__fields__"))), cons(
////            store("__next__", indexOf(slice(load("__fields__"), load("__i__")), Record.Array)),
////            store("__prefix__", slice(load("__fields__"), Zero, load("__i__"))),
////            store("__suffix__", slice(load("__fields__"), load("__next__"))),
////
////            store("__name__", slice(load("__fields__"), load("__i__"), load("__next__"))),
////            store("__key__", add(load("__head__"), Record.Field, load("__name__"))),
////            store("__fields__", add(load("__prefix__"), load("__key__"), load("__suffix__"))),
////            store("__i__", add(add(load("__next"), length("__key__")), length(Record.Array)))
////          )),
////
////          // Prefetch keys and append them to the stack.
////          prefetch(load("__fields__")),
////          store("__stack__", add(load("__stack__"), load("__fields__"), Record.Array))
////        )),
////
////        // Prepend the new field with the new key.
////        store("__key__", add(to, slice("__head__", length(this.key)))),
////        write(load("__key__"), read(load("__head__"))),
////        write(add(load("__key__"), Record.Field, "__kind__"), read(load("__kind__")))
////      ))
////    )
////
////  /**
////   *
////   * @param recursive
////   * @return
////   */
////  def delete(recursive: Boolean): Transaction =
////    cons(
////      // Construct a stack to recursively process fields of a structure.
////      store("__stack__", add(this.key, ",")),
////
////      repeat(ne(load("__stack__"), Empty), cons(
////        // Pop the head of the stack and its kind.
////        store("__i__", indexOf(load("__stack__"), Record.Array)),
////        store("__head__", slice(load("__stack__"), Zero, load("__i__"))),
////        store("__kind__", add(load("__head__"), Record.Field, "__kind__")),
////        store("__stack__", slice(load("__stack__"), add(load("__i__"), length(Record.Array)))),
////
////        // Recurse on stuctures.
////        branch(equal(read(load("__kind__")), "structure"), cons(
////          // Prefix all fields with the head key.
////          store("__fields__", read(load("__head__"))),
////          store("__i__", Zero),
////
////          repeat(less(load("__i__"), length(load("__fields__"))), cons(
////            store("__next__", indexOf(slice(load("__fields__"), load("__i__")), Record.Array)),
////            store("__prefix__", slice(load("__fields__"), Zero, load("__i__"))),
////            store("__suffix__", slice(load("__fields__"), load("__next__"))),
////
////            store("__name__", slice(load("__fields__"), load("__i__"), load("__next__"))),
////            store("__key__", add(load("__head__"), Record.Field, load("__name__"))),
////            store("__fields__", add(load("__prefix__"), load("__key__"), load("__suffix__"))),
////            store("__i__", add(add(load("__next"), length("__key__")), length(Record.Array)))
////          )),
////
////          // Prefetch keys and append them to the stack.
////          prefetch(load("__fields__")),
////          store("__stack__", add(load("__stack__"), load("__fields__"), Record.Array))
////        )),
////
////        // Optionally recurse on references.
////        branch(and(recursive, equal(read(load("__kind__")), "reference")),
////          store("__stack__", add(load("__stack__"), read(load("__head__")), Record.Array))
////        ),
////
////        // Delete the head and its kind.
////        write(load("__head__"), Empty),
////        write(load("__kind__"), Empty)
////      ))
////    )
////
////}
////
////
///////**
////// * A transactional database object. Records are associated with a key and an optional parent. The
////// * key uniquely identifies the record in the database and the parent describes its owning record.
////// * There are three kinds of records: structures, attributes, and references. A structure is a
////// * container of sub-records (called fields), an attribute is a container of a value, and a reference
////// * is a container of a pointer to a record. The kind of the record is stored on a special metafield
////// * called __kind__.
////// *
////// * @param parent Optional owning record.
////// * @param key Unique database identifier.
////// */
//////case class Record(
//////  key: Transaction
//////) extends Assignable with Removable with Evaluable {
//////
//////  /**
//////   * Returns the field of the record with the specified name. If the record has a parent, then it is
//////   * considered to be a reference and is automatically folllowed. Field keys are formed by
//////   * concatenating the parent record key, a reserved field delimiter ('@'), and the field name.
//////   * For example, the 'bar' field of the record 'foo' would have key 'foo@bar'.
//////   *
//////   * @param name Field name.
//////   * @return Field record.
//////   */
//////  def get(name: Transaction): Record =
//////    Record(add(
//////      // If the field is a reference, then dereference it. Return concatenation of key and name.
//////      branch(equal(read(add(this.key, "@__kind__")), "reference"), read(this.key), this.key),
//////      // Every field is separated by a reserved field delimiter "@".
//////      "@",
//////      // Every key also contains the unique field name.
//////      name
//////    ))
//////
//////  override def get: Transaction = read(this.key)
//////
//////  // YOU CANNOT SET A RECORD VALUE OF KIND STRUCTURE. BECAUSE IT DOESN'T DO DEEP COPIES. WE NEED TO
//////  // ADD THIS FEATURE. To do this we need some sort of mechanism for traversing the fields of a key.
//////
//////  override def set(expression: Expression): Transaction =
//////    cons(
//////      // Validate the integrity of the schema.
//////      store("__key__", this.key),
//////      repeat(contains(read(load("__key__")), "@"), cons(
//////        // Extract the parent record of the key.
//////        store("__delimiter__", indexOf(load("__key__"), ",")),
//////        store("__parent__", slice(load("__key__"), 0, load("__delimiter__"))),
//////
//////        // Verify parent is a structure.
//////        store("__kind__", add(read(load("__parent__")), "@__kind__")),
//////        branch(not(equal(read(load("__kind__")), "structure")), cons(
//////          write(load("__kind__"), "__structure__"),
//////          write(load("__parent__"), Empty)
//////        )),
//////
//////        // Verify parent contains field name.
//////        store("__field__", slice(load("__key__"), add(load("__delimiter__"), One))),
//////        branch(not(contains(read(load("__parent__")), load("__field__"))),
//////          write(load("__parent__"), add(read(load("__parent__")), ",", load("__field__")))
//////        )
//////      )),
//////
//////      // Update the kind of the record to the value of the expression..
//////      store("__kind__", add(this.key, "@__kind__")),
//////      branch(not(equal(read(load("__kind__")), expression.kind)), cons(
//////        this.delete(false),
//////        write(load("__kind__"), expression.kind)
//////      )),
//////
//////      // Update the value of the key.
//////      branch(not(equal(read(key), expression.value)), write(key, expression.value))
//////    )
//////
//////  override def delete(recursive: Boolean): Transaction =
//////    cons(
//////      // Remove the record from the field list of its parent.
//////      store("__delimiter__", indexOf(read(this.key), ",")),
//////      store("__parent__", slice(read(this.key), 0, load("__delimiter__"))),
//////      store("__field__", slice(load("__key__"), add(load("__delimiter__"), One))),
//////      store("__i__", indexOf(read(load("__parent__")), load("__field__"))),
//////      store("__j__", add(indexOf(slice(read(load("__parent__")), load("__i__")), ","), One)),
//////
//////      store("__prefix__", slice(read(load("__parent__")), 0, load("__i__"))),
//////      store("__suffix__", slice(read(load("__parent__")), load("__j__"))),
//////      write(load("__parent__"), add(load("__prefix__"), load("__suffix__"))),
//////
//////      // Remove all fields of the record.
//////      store("__keys__", add(this.key, ",")),
//////      repeat(not(equal(load("__keys__"), Empty)), cons(
//////        // Pop the key at the head of the stack.
//////        store("__until__", indexOf(load("__keys__"), ",")),
//////        store("__head__", slice(load("__keys__"), 0, load("__until__"))),
//////        store("__kind__", add(load("__head__"), "@__kind__")),
//////        store("__keys__", slice(load("__keys__"), add(load("__until__"), One))),
//////
//////        // Recurse on stuctures.
//////        branch(equal(read(load("__kind__")), "structure"), cons(
//////          // Prefix all fields with the head key.
//////          store("__fields__", read(load("__head__"))),
//////          store("__i__", 0),
//////
//////          repeat(less(load("__i__"), length(load("__fields__"))), cons(
//////            store("__next__", indexOf(slice(load("__fields__"), load("__i__")), ",")),
//////            store("__prefix__", slice(load("__fields__"), 0, load("__i__"))),
//////            store("__suffix__", slice(load("__fields__"), load("__next__"))),
//////            store("__name__", slice(load("__fields__"), load("__i__"), load("__next__"))),
//////            store("__key__", add(load("__head__"), "@@", load("__name__"))),
//////
//////            store("__fields__", add(load("__prefix__"), load("__key__"), load("__suffix__"))),
//////            store("__i__", add(add(load("__i__"), length("__key__")), One))
//////          )),
//////
//////          // Prefetch keys and append them to the stack.
//////          prefetch(load("__fields__")),
//////          store("__keys__", add(load("__keys__"), load("__fields__"), ","))
//////        )),
//////
//////        // Recurse on references.
//////        branch(and(recursive, equal(read(load("__kind__")), "reference")),
//////          store("__keys__", add(load("__keys__"), read(load("__head__")), ","))
//////        ),
//////
//////        // Delete the head and its kind.
//////        write(load("__head__"), Empty),
//////        write(load("__kind__"), Empty)
//////      ))
//////    )
//////
//////}
