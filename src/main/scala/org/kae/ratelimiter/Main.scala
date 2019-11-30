package org.kae.ratelimiter

import cats.effect.concurrent.MVar
import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import scala.concurrent.duration._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- IO(println("starts"))
    _ <- printEveryMilliSecondRl.take(20000).compile.drain
  } yield ExitCode.Success

  private def ticks(cadence: FiniteDuration): Stream[IO, Unit] =
    Stream.fixedRate(cadence)


  private lazy val rl = new RateLimiter(2.seconds, MVar.empty[IO, Unit].unsafeRunSync() )
  private val printEveryMilliSecondRl: Stream[IO, Unit] = ticks(1.milli)
    .evalMap(_ => rl.apply(IO(println("hi (every millisecond)"))))
}
