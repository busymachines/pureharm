/** Copyright (c) 2019 BusyMachines
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
package busymachines.pureharm.internals.effects

import busymachines.pureharm.phantom._
import cats.Show

/** @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 16 Jun 2019
  */
object PureharmShowInstances {

  trait Implicits extends PhantomTypeInstances {
    implicit val pureharmThrowableShow: Show[Throwable] = Show.fromToString[Throwable]
  }

  trait PhantomTypeInstances {

    implicit final def phantomShow[Underlying, Phantom](implicit
      spook: Spook[Underlying, Phantom],
      show:  Show[Underlying],
    ): Show[Phantom] = Show.show[Phantom](ph => show.show(spook.despook(ph)))

    implicit final def safePhantomShow[Err, Underlying, Phantom](implicit
      spook: SafeSpook[Err, Underlying, Phantom],
      show:  Show[Underlying],
    ): Show[Phantom] = Show.show[Phantom](ph => show.show(spook.despook(ph)))
  }

}
