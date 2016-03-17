package models

import reactivemongo.bson.Macros


case class IntroMessage(total: Double,
                        todaysTotal: Double,
                        dailyAverage: Double,
                        currentStreak: Int,
                        longestStreak: Int,
                        daysSinceStart: Int,
                        todaysSessions: Vector[Session])

object IntroMessage {

  implicit val IntroMessageReader = Macros.reader[IntroMessage]

  implicit val IntroMessageWriter = Macros.writer[IntroMessage]

}