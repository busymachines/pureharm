package busymachines.pureharm

/**
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 10 Jul 2020
  */
package object rest extends PureharmRestTypeDefinitions {
  object implicits extends PureharmRestTapirImplicits with PureharmHttp4sCirceInstances
}