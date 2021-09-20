package trading.processor

import trading.commands.TradeCommand
import trading.core.EventSource
import trading.core.snapshots.SnapshotReader
import trading.events.TradeEvent
import trading.lib.{ Producer, Time }
import trading.state.TradeState

import cats.Monad
import cats.effect.std.Console
import cats.syntax.all._
import fs2.{ Pipe, Stream }

trait Engine[F[_]] {
  def run: Pipe[F, TradeCommand, Unit]
}

object Engine {
  def make[F[_]: Console: Monad: Time](
      producer: Producer[F, TradeEvent],
      snapshots: SnapshotReader[F]
  ): Engine[F] =
    new Engine[F] {
      def run: Pipe[F, TradeCommand, Unit] =
        commands =>
          // TODO: This only allows one instance of `trading`. To allow more instances,
          // we need to shard per symbol, for example.
          Stream
            .eval(snapshots.latest.map(_.getOrElse(TradeState.empty)))
            .flatMap { latest =>
              Stream.eval(Console[F].println(s">>> SNAPSHOTS: $latest")) >>
                commands
                  .evalMapAccumulate(latest) { case (st, cmd) =>
                    val (newSt, events) = EventSource.run(st)(cmd)
                    events
                      .traverse_ { f =>
                        Time[F].timestamp.map(f) >>= producer.send
                      }
                      .tupleLeft(newSt)
                  }
                  .void
            }
    }
}