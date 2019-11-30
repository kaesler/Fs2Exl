package org.kae.ratelimiter

import cats.effect.concurrent.MVar
import cats.effect.{ContextShift, Fiber, IO, LiftIO, Sync, Timer}
import cats.syntax.applicative._
import cats.syntax.apply._
import fs2.Stream
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.syntax.flatMap._
import cats.syntax.functor._

class RateLimiterImpl(
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
    mvar.tryTake.to[F]
      .flatMap {
        case None =>
          ().pure[IO].to[F]
        case Some(_) => action
      }
}
