package org.kae.ratelimiter

import cats.effect.concurrent.MVar
import cats.effect.{ContextShift, IO, LiftIO, Sync, Timer}
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait CancellableRateLimiter[F[_]] extends (F[Unit] => F[Unit]) {
  def cancel(): F[Unit]
}

object CancellableRateLimiter {
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def create[F[_]: LiftIO: Sync](period: FiniteDuration): F[CancellableRateLimiter[F]] = (
    for {
      mvar <- MVar.empty[IO, Unit]
      fiber <- Stream.fixedRate[IO](period)
        .evalMap(_ => IO(println("tryPut")) *> mvar.tryPut(()).void)
        .compile
        .drain
        .start

    } yield {
      val res = new CancellableRateLimiter[F] {
        override def cancel(): F[Unit] = fiber.cancel.to[F]

        override def apply(action: F[Unit]): F[Unit] =
          mvar.tryTake.to[F]
            .flatMap {
              case None =>
                ().pure[IO].to[F]
              case Some(_) => action
            }
      }
      res
    }).to[F]
}
