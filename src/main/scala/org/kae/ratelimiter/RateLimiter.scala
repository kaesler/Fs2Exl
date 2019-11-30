package org.kae.ratelimiter

import cats.effect.concurrent.MVar
import cats.effect.{ContextShift, Fiber, IO, LiftIO, Sync, Timer}
import cats.syntax.applicative._
import cats.syntax.apply._
import fs2.Stream
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.syntax.flatMap._

class RateLimiter(
    period: FiniteDuration,
    mvar: MVar[IO, Unit],
) {
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val ticks: Fiber[IO, Unit] = Stream
    .fixedRate[IO](period)
    .evalMap { _ =>
      IO(println("tryPut")) *>
        mvar.tryPut(())
    }
    .compile
    .drain
    .start
    .unsafeRunSync()

  def cancel(): Unit = ticks.cancel.unsafeRunSync()

  def apply[F[_]: LiftIO: Sync](action: F[Unit]): F[Unit] =
    LiftIO[F]
      .liftIO(mvar.tryTake)
      .flatMap {
        case None =>
          //IO(println("tryTake gave None")) *>
          LiftIO[F].liftIO(().pure[IO])
        case Some(_) => action
      }
}

