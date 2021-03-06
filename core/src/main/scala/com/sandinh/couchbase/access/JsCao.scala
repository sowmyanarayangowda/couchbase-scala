package com.sandinh.couchbase.access

import com.couchbase.client.java.document.json.JsonArray
import com.couchbase.client.java.query.{N1qlParams, N1qlQuery}
import com.sandinh.couchbase.ScalaBucket
import com.sandinh.couchbase.document.JsDocument
import play.api.libs.json.{Json, JsValue, Format}
import com.sandinh.rx.Implicits._
import com.sandinh.couchbase.Implicits._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** Base class for Couchbase Access Object to access json documents that can be decode/encode to/from the `T` type */
abstract class JsCao[T: Format](bucket: ScalaBucket) extends CaoBase[T, JsValue, JsDocument](bucket) {
  protected def reads(u: JsValue): T = u.as[T]
  protected def writes(t: T): JsValue = Json.toJson(t)

  protected def createDoc(id: String, expiry: Int, content: JsValue): JsDocument = new JsDocument(id, content, expiry)

  final def query1(n1ql: String, qparam: N1qlParams, params: AnyRef*): Future[Option[T]] = {
    val q =
      if (params.isEmpty) N1qlQuery.simple(n1ql, qparam)
      else {
        val p = JsonArray.from(params: _*)
        N1qlQuery.parameterized(n1ql, p, qparam)
      }
    bucket.query(q)
      .flatMap(_.rows().toFuture
        .map { row =>
          Json.fromJson[T](row.value().toPlayJs)
            .asOpt
        }.recover {
          case _: NoSuchElementException => None
        })
  }

  final def query1(n1ql: String, params: AnyRef*): Future[Option[T]] = query1(n1ql, null, params: _*)
}

/** Base class for Couchbase Access Object to access json documents that can be decode/encode to/from the `T` type - which is
  * store in couchbase at key generated from the T.key(A) method
  */
abstract class JsCao1[T: Format, A](bucket: ScalaBucket) extends JsCao[T](bucket) with WithCaoKey1[T, A, JsValue, JsDocument]

/** Base class for Couchbase Access Object to access json documents that can be decode/encode to/from the `T` type - which is
  * store in couchbase at key generated from the T.key(A, B) method
  */
abstract class JsCao2[T: Format, A, B](bucket: ScalaBucket) extends JsCao[T](bucket) with WithCaoKey2[T, A, B, JsValue, JsDocument]
