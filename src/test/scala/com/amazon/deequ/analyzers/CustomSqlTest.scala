package com.amazon.deequ.analyzers

import com.amazon.deequ.SparkContextSpec
import com.amazon.deequ.metrics.DoubleMetric
import com.amazon.deequ.utils.FixtureSupport
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Failure
import scala.util.Success

class CustomSqlTest extends AnyWordSpec with Matchers with SparkContextSpec with FixtureSupport {

  "CustomSql" should {
    "return a metric when the statement returns exactly one value" in withSparkSession { session =>
      val data = getDfWithStringColumns(session)
      data.createOrReplaceTempView("primary")

      val sql = CustomSql("SELECT COUNT(*) FROM primary WHERE `Address Line 2` IS NOT NULL")
      val state = sql.computeStateFrom(data)
      val metric: DoubleMetric = sql.computeMetricFrom(state)

      metric.value.isSuccess shouldBe true
      metric.value.get shouldBe 6.0
    }

    "returns a failed metric when the statement returns more than one row" in withSparkSession { session =>
      val data = getDfWithStringColumns(session)
      data.createOrReplaceTempView("primary")

      val sql = CustomSql("Select `Address Line 2` FROM primary WHERE `Address Line 2` is NOT NULL")
      val state = sql.computeStateFrom(data)
      val metric = sql.computeMetricFrom(state)

      metric.value.isFailure shouldBe true
      metric.value match {
        case Success(_) => fail("Should have failed")
        case Failure(exception) => exception.getMessage shouldBe "Custom SQL did not return exactly 1 row"
      }
    }

    "returns a failed metric when the statement returns more than one column" in withSparkSession { session =>
      val data = getDfWithStringColumns(session)
      data.createOrReplaceTempView("primary")

      val sql = CustomSql(
        "Select `Address Line 1`, `Address Line 2` FROM primary WHERE `Address Line 3` like 'Bandra%'")
      val state = sql.computeStateFrom(data)
      val metric = sql.computeMetricFrom(state)

      metric.value.isFailure shouldBe true
      metric.value match {
        case Success(_) => fail("Should have failed")
        case Failure(exception) => exception.getMessage shouldBe "Custom SQL did not return exactly 1 column"
      }

    }
  }
}
