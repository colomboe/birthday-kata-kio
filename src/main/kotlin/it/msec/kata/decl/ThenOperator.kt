package it.msec.kata.decl

import it.msec.kio.KIO
import it.msec.kio.common.tuple.T
import it.msec.kio.common.tuple.T2
import it.msec.kio.flatMap
import it.msec.kio.map

@JvmName("a") inline infix fun <A, B, C> ((A) -> B).then(crossinline f: (B) -> C): (A) -> C = { a -> f(this(a)) }
@JvmName("b") infix fun <R, E, A, B, C> ((A) -> KIO<R, E, B>).then(f: (B) -> C): (A) -> KIO<R, E, C> = { a -> this(a).map(f) }
@JvmName("c") infix fun <R, E, A, B, C> ((A) -> KIO<R, E, B>).then(f: (B) -> KIO<R, E, C>): (A) -> KIO<R, E, C> = { a -> this(a).flatMap(f) }
@JvmName("d") inline infix fun <B, C> (() -> B).then(crossinline f: (B) -> C): () -> C = { f(this()) }
@JvmName("e") infix fun <R, E, B, C> (() -> KIO<R, E, B>).then(f: (B) -> C): () -> KIO<R, E, C> = { this().map(f) }
@JvmName("f") infix fun <R, E, B, C> (() -> KIO<R, E, B>).then(f: (B) -> KIO<R, E, C>): () -> KIO<R, E, C> = { this().flatMap(f) }

@JvmName("wa")
infix fun <R, E, A, B, C> ((A) -> B).with(f: () -> KIO<R, E, C>): (A) -> KIO<R, E, T2<B, C>> =
        { a -> f().map { T(this(a), it) } }

@JvmName("wb")
infix fun <R, E, A, B, C> ((A) -> KIO<R, E, B>).with(f: () -> KIO<R, E, C>): (A) -> KIO<R, E, T2<B, C>> =
        { a -> this(a).flatMap { b -> f().map { c -> T(b, c) } } }

@JvmName("wc")
infix fun <R, E, A, B, C> ((A) -> KIO<R, E, B>).with(f: (A) -> KIO<R, E, C>): (A) -> KIO<R, E, T2<B, C>> =
        { a -> this(a).flatMap { b -> f().map { c -> T(b, c) } } }
