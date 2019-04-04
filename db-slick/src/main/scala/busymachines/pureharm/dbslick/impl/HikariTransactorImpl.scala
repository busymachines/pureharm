package busymachines.pureharm.dbslick.impl

import cats.implicits._
import cats.effect._
import busymachines.pureharm.dbslick._

import scala.concurrent.ExecutionContext

/**
  *
  * @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 02 Apr 2019
  *
  */
private[dbslick] class HikariTransactorImpl[F[_]] private (
  override val slickAPI: JDBCProfileAPI,
  override val slickDB:  DatabaseBackend,
)(
  implicit private val F: Async[F]
) extends Transactor[F] {

  override def run[T](cio: ConnectionIO[T]): F[T] =
    IO.fromFuture(IO(slickDB.run(cio))).to[F]

  override def shutdown: F[Unit] = F.delay(slickDB.close())

  /**
    * The execution context used to run all blocking database input/output
    */
  override def ioExecutionContext: ExecutionContext = slickDB.ioExecutionContext
}

private[dbslick] object HikariTransactorImpl {

  import slick.util.AsyncExecutor

  import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

  def resource[F[_]: Async](
    dbProfile: JDBCProfileAPI,
  )(
    url:      JDBCUrl,
    username: DBUsername,
    password: DBPassword,
    config:   DBBlockingIOExecutionConfig,
  ): Resource[F, Transactor[F]] = {
    Resource.make(unsafeCreate[F](dbProfile)(url, username, password, config))(_.shutdown)
  }

  /**
    * Prefer using [[resource]] unless you know what you are doing.
    * @tparam F
    */
  def unsafeCreate[F[_]: Async](
    slickProfile: JDBCProfileAPI,
  )(
    url:      JDBCUrl,
    username: DBUsername,
    password: DBPassword,
    config:   DBBlockingIOExecutionConfig,
  ): F[Transactor[F]] = {
    val F = Async[F]

    for {
      hikari <- F.delay {
                 val hikariConfig = new HikariConfig()
                 hikariConfig.setJdbcUrl(url)
                 hikariConfig.setUsername(username)
                 hikariConfig.setPassword(password)

                 new HikariDataSource(hikariConfig)
               }

      exec <- F.delay(
               AsyncExecutor(
                 name           = config.prefixName:     String,
                 minThreads     = config.maxConnections: Int,
                 maxThreads     = config.maxConnections: Int,
                 queueSize      = config.queueSize:      Int,
                 maxConnections = config.maxConnections: Int,
               )
             )
      slickDB <- F.delay(
                  DatabaseBackend(
                    slickProfile.Database.forDataSource(
                      ds             = hikari,
                      maxConnections = Option(config.maxConnections),
                      executor       = exec
                    )
                  )
                )
      _ <- F.delay(slickDB.createSession())
    } yield new HikariTransactorImpl(slickProfile, slickDB)
  }
}