package stryker4s.http

trait WebIO {
  def postRequest(url: String, content: String): WebResponse
}

case class WebResponse(
    httpCode: Int,
    responseBody: String
) {
  @transient lazy val isSuccess: Boolean = httpCode == 200

  /**
    * Http code 4xx are user errors
    */
  @transient lazy val isUserError: Boolean = Math.ceil(httpCode / 100.0) == 4

  /**
    * Http code 5xx are server errors
    */
  @transient lazy val isServerError: Boolean = Math.ceil(httpCode / 100.0) == 5
}

object RealHttp extends WebIO {
  import scalaj.http._

  private def toWebResponse(input: HttpResponse[String]): WebResponse = {
    WebResponse(
      input.code,
      input.body
    )
  }

  override def postRequest(
      url: String,
      content: String
  ): WebResponse = {
    val response = Http(url)
      .method("POST")
      .postData(content)
      .header("Content-Type", "application/json")
      .asString

    response.location match {
      case Some(redirect) => postRequest(redirect, content)
      case None           => toWebResponse(response)
    }
  }
}
