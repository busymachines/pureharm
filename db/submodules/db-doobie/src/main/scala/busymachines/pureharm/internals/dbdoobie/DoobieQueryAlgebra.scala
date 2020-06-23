/**
  * Copyright (c) 2019 BusyMachines
  *
  * See company homepage at: https://www.busymachines.com/
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package busymachines.pureharm.internals.dbdoobie

import busymachines.pureharm.effects._
import busymachines.pureharm.effects.implicits._
import busymachines.pureharm.db._
import busymachines.pureharm.dbdoobie._
import busymachines.pureharm.dbdoobie.implicits._

/**
  *
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 24 Sep 2019
  *
  */
abstract class DoobieQueryAlgebra[E, PK, Table <: TableWithPK[E, PK]] extends DAOAlgebra[ConnectionIO, E, PK] {
  def table: Table

  implicit def getPK: Get[PK]
  implicit def putPK: Put[PK]

  /**
    * Should be overriden as non implicit since doobie doesn't
    * provide semiauto-derivation so you want to write in your subclasses:
    * {{{
    *   override def getE: Read[MyCaseClass] = Read[MyCaseClass]
    * }}}
    *
    * Otherwise the implicit picks itself up.
    * But in this definition here it is implicit.
    *
    * Alternatively you can create a superclass that takes
    * these as implicit parameters and they are passed to the class from
    * outside
    */
  implicit def getE: Read[E]
  implicit def putE: Write[E]

  implicit def showPK: Show[PK]

  protected def tableName: TableName  = table.tableName
  protected def pkColumn:  ColumnName = table.pkColumn

  //FIXME: move somewhere reusable
  implicit private val stdIterableTraverse: Traverse[Iterable] = new Traverse[Iterable] {

    override def foldLeft[A, B](fa: Iterable[A], b: B)(f: (B, A) => B): B =
      fa.foldLeft(b)(f)

    override def foldRight[A, B](fa: Iterable[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa.foldRight(lb)(f)

    override def traverse[G[_], A, B](fa: Iterable[A])(f: A => G[B])(implicit G: Applicative[G]): G[Iterable[B]] = {
      import scala.collection.mutable
      foldLeft[A, G[mutable.Builder[B, mutable.Iterable[B]]]](fa, G.pure(mutable.Iterable.newBuilder[B]))((lglb, a) =>
        G.map2(f(a), lglb)((b: B, lb: mutable.Builder[B, mutable.Iterable[B]]) => lb.+=(b))
      ).map(_.result().toIterable)
    }
  }

  override def find(pk: PK): ConnectionIO[Option[E]] =
    Query[PK, E](findSQL).option(pk)

  override def retrieve(pk: PK)(implicit show: Show[PK]): ConnectionIO[E] =
    this.find(pk).flattenOption(DoobieDBEntryNotFoundAnomaly(show.show(pk), Option.empty))

  override def insert(e: E): ConnectionIO[PK] =
    Update[E](insertSQL).withUniqueGeneratedKeys[PK](pkColumn)(e)

  override def insertMany(es: Iterable[E]): ConnectionIO[Unit] = {
    val expectedSize = es.size
    for {
      inserted <- Update[E](insertSQL).updateMany(es).adaptError {
        case bux: java.sql.BatchUpdateException =>
          DoobieDBBatchInsertFailedAnomaly(
            expectedSize = expectedSize,
            actualSize   = 0,
            causedBy     = Option(bux),
          )
      }
      _        <- (inserted != expectedSize).ifTrueRaise[ConnectionIO](
        DoobieDBBatchInsertFailedAnomaly(
          expectedSize = expectedSize,
          actualSize   = inserted,
          causedBy     = Option.empty,
        )
      )
    } yield ()
  }

  override def update(e: E): ConnectionIO[E] =
    Update[(E, PK)](updateSQL).withUniqueGeneratedKeys[E](table.rawColumns: _*)((e, table.pkOf(e)))

  override def updateMany[M[_]: Traverse](es: M[E]): ConnectionIO[Unit] =
    es.traverse(this.update).void

  override def delete(pk: PK): ConnectionIO[Unit] =
    for {
      deleted <- Update[PK](deleteSQL).run(pk)
      _       <- (deleted == 1).ifFalseRaise[ConnectionIO](DoobieDBDeleteByPKFailedAnomaly(pk.show))
    } yield ()

  override def deleteMany(pks: Iterable[PK]): ConnectionIO[Unit] = {
    val q = deleteManySQLFragment(pks.toList).update
    for {
      deleted <- q.run
      _       <- (deleted == pks.size).ifFalseRaise[ConnectionIO](DoobieDBDeleteByPKFailedAnomaly(pks.mkString(", ")))
    } yield ()
  }

  override def exists(pk: PK): ConnectionIO[Boolean] =
    Query[PK, Boolean](existsSQL).unique(pk)

  override def existsAtLeastOne(pks: Iterable[PK]): ConnectionIO[Boolean] =
    existsAtLeastOneSQLFragment(pks.toList).query[Boolean].unique

  override def existAll(pks: Iterable[PK]): ConnectionIO[Boolean] =
    existsAllSQLFragment(pks.toList).query[Boolean].unique

  //----- plain string queries -----
  private val findSQL: String =
    s"SELECT ${table.tupleString} FROM $tableName WHERE $pkColumn = ?"

  private val insertSQL: String =
    s"INSERT INTO $tableName ${table.tupleStringEnclosed} VALUES ${table.questionMarkTupleEnclosed}"

  /**
    * Generate something like:
    * {{{
    *
    * }}}
    */
  private val updateSQL: String =
    s"""
       |UPDATE $tableName SET ${table.rawColumns.map(s => s"$s = ?").intercalate(", ")}
       |WHERE $pkColumn = ?
       |""".stripMargin

  private val deleteSQL: String =
    s"DELETE FROM $tableName WHERE $pkColumn = ?"

  private val existsSQL =
    s"SELECT EXISTS(SELECT 1 FROM $tableName WHERE $pkColumn = ?)"

  //----- fragments -----
  private def inArraySQLFragment[T: Put](fs: List[T]): Fragment =
    fr"IN (" ++ fs.map(n => fr"$n").intercalate(fr",") ++ fr")"

  private def deleteManySQLFragment(pks: List[PK]): Fragment =
    Fragment.const(s"DELETE FROM $tableName WHERE $pkColumn") ++ inArraySQLFragment(pks)

  private def existsAtLeastOneSQLFragment(pks: List[PK]): Fragment =
    Fragment.const(s"SELECT EXISTS(SELECT 1 FROM $tableName WHERE $pkColumn") ++ inArraySQLFragment(pks) ++ fr")"

  private def existsAllSQLFragment(pks: List[PK]): Fragment =
    Fragment.const(s"SELECT (SELECT COUNT(*) FROM $tableName WHERE $pkColumn") ++
      inArraySQLFragment(pks) ++ fr")" ++ Fragment.const(s" = ${pks.size}")

}