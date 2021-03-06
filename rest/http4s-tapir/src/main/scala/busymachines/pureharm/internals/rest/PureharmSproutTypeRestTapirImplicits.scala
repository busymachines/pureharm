/** Copyright (c) 2017-2019 BusyMachines
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
package busymachines.pureharm.internals.rest

import busymachines.pureharm.phantom._
import sttp.tapir

import java.util.UUID

/** @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 10 Jul 2020
  */

trait PureharmSproutTypeRestTapirImplicits extends sttp.tapir.json.circe.TapirJsonCirce {

  implicit def pureharmNewTypeTapirStringPathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[String],
    p:  NewType[String, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[String, New]

  implicit def pureharmNewTypeTapirUUIDPathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[UUID],
    p:  NewType[UUID, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[UUID, New]

  implicit def pureharmNewTypeTapirLongPathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[Long],
    p:  NewType[Long, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[Long, New]

  implicit def pureharmNewTypeTapirIntPathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[Int],
    p:  NewType[Int, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[Int, New]

  implicit def pureharmNewTypeTapirBytePathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[Byte],
    p:  NewType[Byte, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[Byte, New]

  implicit def pureharmNewTypeTapirShortPathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[Short],
    p:  NewType[Short, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[Short, New]

  implicit def pureharmNewTypeTapirBooleanPathCodec[New](implicit
    tc: tapir.Codec.PlainCodec[Boolean],
    p:  NewType[Boolean, New],
  ): tapir.Codec.PlainCodec[New] = genericSproutTypePathMatcher[Boolean, New]

  @inline private def genericSproutTypePathMatcher[Old, New](implicit
    tc: tapir.Codec.PlainCodec[Old],
    p:  NewType[Old, New],
  ): tapir.Codec.PlainCodec[New] = tc.map(p.newType _)(p.oldType)

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  implicit def pureharmSproutTypeGenericSchema[Underlying, New: OldType[Underlying, *]](implicit
    sc: tapir.Schema[Underlying]
  ): tapir.Schema[New] =
    sc.copy(description = sc.description match {
      case None           => Option(OldType[Underlying, New].symbolicName)
      case Some(original) => Option(s"$original — type name: ${OldType[Underlying, New].symbolicName}")
    }).asInstanceOf[tapir.Schema[New]]

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  implicit def phantomTypeGenericValidator[Underlying, PT: Spook[Underlying, *]](implicit
    sc: tapir.Validator[Underlying]
  ): tapir.Validator[PT] = sc.contramap(Spook[Underlying, PT].despook)

  implicit def safePhantomTypeGenericValidator[E, Underlying, PT: SafeSpook[E, Underlying, *]](implicit
    sc: tapir.Validator[Underlying]
  ): tapir.Validator[PT] = sc.contramap(SafeSpook[E, Underlying, PT].despook)

  /** Basically, it's union of the schema of AnomalyBase and AnomaliesBase,
    * + any non-anomaly throwable is being wrapped in an UnhandledAnomaly
    */
  implicit val tapirSchemaThrowableAnomaly: tapir.Schema[Throwable] = PureharmTapirSchemas.tapirSchemaAnomalies

  implicit def pureharmTapirAuthOps(o: tapir.TapirAuth.type): TapirOps.AuthOps = new TapirOps.AuthOps(o)

  implicit def pureharmTapirCodecOps[Old](c: sttp.tapir.Codec.PlainCodec[Old]): TapirOps.CodecOps[Old] =
    new TapirOps.CodecOps[Old](c)
}
