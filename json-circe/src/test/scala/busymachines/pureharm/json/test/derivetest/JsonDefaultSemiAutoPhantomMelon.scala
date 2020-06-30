/**
  * Copyright (c) 2017-2019 BusyMachines
  *
  * See company homepage at: https://www.busymachines.com/
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package busymachines.pureharm.json.test.derivetest

import org.scalatest.flatspec.AnyFlatSpec
import busymachines.pureharm.json.implicits._
import busymachines.pureharm.json.test._

/**
  * Here we test [[busymachines.pureharm.json.Decoder]] derivation
  *
  * See the [[Melon]] hierarchy
  *
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 11 Jun 2019
  */
final class JsonDefaultSemiAutoPhantomMelon extends AnyFlatSpec {
  import melonsDefaultSemiAutoCodecs._

  //-----------------------------------------------------------------------------------------------

  it should "... encode + decode phantomMelon" in {
    val originalPhantomMelon: Melon = PhantomMelon(
      weight     = Weight(42),
      weights    = Weights(List(1, 2)),
      weightsSet = WeigthsSet(Set(3, 4)),
      duo        = MelonDuo((5, "duo")),
      trio       = MelonTrio((6, "trio", List(1, 2, 3))),
    )

    val encoded = originalPhantomMelon.asJson
    val decoded = encoded.unsafeDecodeAs[Melon]

    assertResult(originalPhantomMelon)(decoded)
  }
}
