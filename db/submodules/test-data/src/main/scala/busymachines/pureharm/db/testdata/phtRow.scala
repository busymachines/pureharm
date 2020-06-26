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
package busymachines.pureharm.db.testdata

import busymachines.pureharm.identifiable.Identifiable

/**
  *
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 12 Jun 2019
  *
  */
final private[pureharm] case class PHTRow(
  id:          PhantomPK,
  byte:        PhantomByte,
  int:         PhantomInt,
  long:        PhantomLong,
  bigDecimal:  PhantomBigDecimal,
  string:      PhantomString,
  jsonbCol:    PHTJSONCol,
  optionalCol: Option[PhantomString],
)

object PHTRow {

  implicit val identifiable: Identifiable[PHTRow, PhantomPK] = Identifiable.mkIdentifiable
}

final private[pureharm] case class PHTJSONCol(
  jsonInt:    Int,
  jsonString: String,
)