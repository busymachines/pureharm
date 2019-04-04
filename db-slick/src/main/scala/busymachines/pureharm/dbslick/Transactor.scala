package busymachines.pureharm.dbslick

import scala.concurrent.ExecutionContext

/**
  *
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 02 Apr 2019
  *
  */
trait Transactor[F[_]] {
  def run[T](cio: ConnectionIO[T]): F[T]

  def shutdown: F[Unit]

  /**
    * The execution context used to run all blocking database input/output
    */
  def ioExecutionContext: ExecutionContext

  /**
    * Please use only to compensate for the lacks of this evergrowing
    * API. Prefer to make this wrapper support what you want to do,
    * rather than using this thing.
    *
    * @return
    *   The underlying slick representation of a Database, used to
    *   run your DBIOActions.
    */
  val slickDB: DatabaseBackend

  /**
    * Please use only to compensate for the lacks of this evergrowing
    * API. Prefer to make this wrapper support what you want to do,
    * rather than using this thing.
    *
    * @return
    *   The underlying JDBC profile you used to instantiate this
    *   [[Transactor]]. Most likely that one global object in your
    *   project that you instantiated once, and then forgot about.
    *   Now available to import through here for more localized
    *   reasoning in case you need it.
    */
  val slickAPI: JDBCProfileAPI

}

object Transactor {

  import cats.effect._

  def pgSQLHikari[F[_]: Async](
    dbProfile: JDBCProfileAPI,
  )(
    url:      JDBCUrl,
    username: DBUsername,
    password: DBPassword,
    config:   DBBlockingIOExecutionConfig,
  ): Resource[F, Transactor[F]] =
    impl.HikariTransactorImpl.resource[F](
      dbProfile = dbProfile,
    )(
      url      = url,
      username = username,
      password = password,
      config   = config,
    )

  /**
    * Prefer using [[pgSQLHikari]] instead.
    *
    * You really need to know what you are doing and
    * ensure proper cleanup if using this.
    */
  def pgSQLHikariUnsafe[F[_]: Async](
    dbProfile: JDBCProfileAPI,
  )(
    url:      JDBCUrl,
    username: DBUsername,
    password: DBPassword,
    config:   DBBlockingIOExecutionConfig,
  ): F[Transactor[F]] =
    impl.HikariTransactorImpl.unsafeCreate[F](
      slickProfile = dbProfile,
    )(
      url      = url,
      username = username,
      password = password,
      config   = config,
    )
}