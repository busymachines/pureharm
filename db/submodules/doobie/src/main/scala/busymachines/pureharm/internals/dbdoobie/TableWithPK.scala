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

import busymachines.pureharm.db._
import busymachines.pureharm.effects._
import busymachines.pureharm.dbdoobie._
import busymachines.pureharm.identifiable.Identifiable

/**
  *
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 24 Sep 2019
  *
  */
abstract class TableWithPK[E, PK](implicit val iden: Identifiable[E, PK]) {
  def tableName:   TableName
  def tailColumns: List[ColumnName]

  /**
    * Should be overriden as non implicit since doobie doesn't
    * provide semiauto-derivation so you want to write in your subclasses:
    * {{{
    *   override def readE: Read[MyCaseClass] = Read[MyCaseClass]
    * }}}
    *
    * These are then aliased as implicits in the [[DoobieQueryAlgebra]]
    * for seamless use 99% of the cases
    */
  def showPK: Show[PK]
  def metaPK: Meta[PK]
  def readE:  Read[E]
  def writeE: Write[E]

  final def pkColumn: ColumnName = ColumnName(iden.fieldName)
  final def columns:  Row        = Row(NEList(pkColumn, tailColumns))

  final def rawColumns: List[String] = columns.toList.map(ColumnName.despook)

  final def pkOf(e: E): PK = iden.id(e)

  final def tupleString: String = Row.asString(columns)

  final def tupleStringEnclosed: String = s"($tupleString)"

  final def questionMarkTuple: String = Row.asQuestionMarks(columns)

  final def questionMarkTupleEnclosed: String = s"(${Row.asQuestionMarks(columns)})"
}