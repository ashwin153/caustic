package com.schema.core.transactions

/**
 * A successfully applied transaction.
 *
 * @param mutations Applied mutations.
 */
case class Change(mutations: Seq[(String, Mutation)])