package org.kae.ratelimiter

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import scala.concurrent.duration._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO(println("starts"))
      rl <- CancellableRateLimiter.create[IO](2.seconds)
      _ <- printEveryMilliSecondRl(rl).take(20000).compile.drain
      _ <- rl.cancel()
    } yield ExitCode.Success

  private def ticks(cadence: FiniteDuration): Stream[IO, Unit] =
    Stream.fixedRate(cadence)

  private def printEveryMilliSecondRl(
    rl: CancellableRateLimiter[IO]
  ): Stream[IO, Unit] =
    ticks(1.milli)
      .evalMap(_ => rl(IO(println("hi (every millisecond)"))))
}
