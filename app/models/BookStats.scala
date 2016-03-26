package models

import reactivemongo.bson.{BSONArray, BSONDocument, BSONInteger, BSONLong, BSONString}


object BookStats {

  def stats(bookVec: BookVector): BSONDocument = {


    BSONDocument(
      "authorCount" -> BSONInteger(authorCount(bookVec)),
      "bookCount" -> BSONInteger(bookCount(bookVec)),
      "pageCount" -> BSONInteger(imputedPageCount(bookVec)),
      "averagePageCount" -> BSONInteger(averagePageCount(bookVec)),
      "booksPerAuthor" -> BSONArray(booksPerAuthor(bookVec).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2)))),
      "pagesPerAuthor" -> BSONArray(pagesPerAuthor(bookVec).toVector.sortBy(p => -p._2).map(p => BSONArray(BSONString(p._1), BSONInteger(p._2)))),
      "cumulativePages" -> BSONArray(cumulativePages(bookVec).map(p => BSONArray(BSONLong(p._1), BSONInteger(p._2)))),
      "booksPerPubYear" -> BSONArray(booksPerPubYear(bookVec).map(p => BSONArray(BSONInteger(p._1), BSONInteger(p._2))))
    )
  }

  def authorCount(bookVec: BookVector): Int = {

    bookVec.books.flatMap(_.authors).toSet.size
  }


  def bookCount(bookVec: BookVector): Int = {

    bookVec.books.length
  }


  def imputedPageCount(bookVec: BookVector): Int = {

    val hasPageCount = bookVec.books.filter(_.pages > 0).map(_.pages)

    val avg = hasPageCount.sum / hasPageCount.length

    avg * bookVec.books.length
  }


  def averagePageCount(bookVec: BookVector): Int = {

    val hasPageCount = bookVec.books.filter(_.pages > 0).map(_.pages)

    val avg = hasPageCount.sum / hasPageCount.length

    avg
  }


  def booksPerAuthor(bookVec: BookVector): Map[String, Int] = {
    bookVec.books.flatMap(_.authors).groupBy(a => a).mapValues(_.length)
  }


  // Only looking at first author. Could change that.
  def pagesPerAuthor(bookVec: BookVector): Map[String, Int] = {
    bookVec.books.filter(_.authors.nonEmpty).groupBy(_.authors.head).mapValues(books => books.map(_.pages).sum)
  }

  def cumulativePages(bookVec: BookVector): Vector[(Long, Int)] = {

    val avg = averagePageCount(bookVec)

    // First, impute missing page counts
    bookVec.books.sortBy(_.finished).map(book => (book.finished, if (book.pages > 0) book.pages else avg)).map({

      var s = 0

      p => {
        s += p._2
        (p._1, s)
      }
    })

  }

  def booksPerPubYear(bookVec: BookVector): Vector[(Int, Int)] = {
    bookVec.books.filter(_.pubYear != 0).groupBy(_.pubYear).mapValues(_.length).toVector.sortBy(_._1)
  }
}
