package falstar.falsification

import falstar.hybrid.Region
import falstar.hybrid.System
import falstar.mtl.Formula
import falstar.hybrid.Signal
import falstar.util.Timer
import falstar.hybrid.Time
import falstar.mtl.Robustness
import falstar.hybrid.Rho
import falstar.util.Probability
import falstar.hybrid.Config
import falstar.hybrid.Input

trait Falsification {
  def repeat(sys: System, cfg: Config, phi: Formula, _seed: Option[Long], n: Int): Table = {
    import falstar.util.IntOps

    _seed match {
      case None => Probability.setUniqueSeed()
      case Some(seed) => Probability.seed = seed
    }
    
    val seed = Probability.seed

    val data = (1 to n) map {
      i =>
        println("trial " + i + "/" + n)
        apply(sys, cfg, phi)
    }

    val (best, _) = data.minBy(_._1.score)
    val (good, bad) = data.partition(_._1.isFalsified)

    val (_, stats) = good.unzip
    val table = Table(sys, phi, this, seed, good.size, n, Statistics.min(stats), Statistics.max(stats), Statistics.avg(stats), best)

    table
  }

  def apply(sys: System, cfg: Config, phi: Formula): (Result, Statistics) = {
    println("property " + phi)
    println("algorithm " + identification)
    for ((name, value) <- this.params) {
      println("  " + name + ": " + value)
    }

    val (res, stats) = search(sys,cfg, phi)
    println()

    println("inputs")
    val us = res.tr.us
    val t__ = us map { case (t, u) => Array(t) }
    val u__ = us map { case (t, u) => u.data }
    val U = u__.last
    val T = res.T
    println("  t__ = [" + t__.map(_.mkString(" ")).mkString("; ") + "; " + T + "]")
    println("  u__ = [" + u__.map(_.mkString(" ")).mkString("; ") + "; " + U.mkString(" ") + "]")

    if (res.isFalsified) {
      print("falsified")
    } else {
      print("not falsified")
    }
    println(" with robustness " + res.score)
    println()

    println("statistics")
    println("  simulations " + stats.simulations)
    println("  total time  " + stats.time + "s")
    println("  peak memory " + util.peakMemBytes / 1000 + " kb")
    println()

    (res, stats)
  }

  def identification: String
  def params: Seq[(String, Any)]
  def search(sys: System, cfg: Config, phi: Formula): (Result, Statistics)
}

trait WithStatistics {
  this: Falsification =>

  def search(sys: System, cfg: Config, phi: Formula): (Result, Statistics) = {
    var simulations = 0
    object simulation extends Timer
    object formula extends Timer
    object total extends Timer

    val T = phi.T

    def sim(ps: Input, us: Signal, T: Time): Result = {
      simulations += 1
      val tr = simulation.during {
        sys.sim(ps, us, T)
      }
      val rs = formula.during {
        Robustness(phi, tr.us, tr.ys)
      }
      Result(tr, rs)
    }

    val res = total.during {
      search(sys, cfg, phi, T, sim)
    }

    val stats = Statistics(simulations, total.seconds, 0)

    (res, stats)
  }

  def search(sys: System, cfg: Config, phi: Formula, T: Time, sim: (Input, Signal, Time) => Result): Result
}

object Falsification {
  trait Observer {
    def reset(phi: Formula)
    def update(tr: Result) { update(Seq(tr)) }
    def update(trs: Results)
  }

  object Observer {
    object default extends Observer {
      def reset(phi: Formula) {}
      def update(trs: Results) {}
    }
  }

  var observer: Observer = Observer.default
}
