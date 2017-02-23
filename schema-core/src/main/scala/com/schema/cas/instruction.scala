package com.schema.cas

/**
 * An instruction. Instructions are the set of permissible operations used to record accesses and
 * modifications to objects. Object accesses are recorded as [[Read]], updates as [[Upsert]] and
 * removals as [[Delete]]. A [[Mutation]] is an instruction that, when applied to a snapshot, causes
 * a change in state.
 */
sealed trait Instruction
case object Read extends Instruction
sealed trait Mutation extends Instruction
case object Delete extends Mutation
case class Upsert[T](value: T) extends Mutation