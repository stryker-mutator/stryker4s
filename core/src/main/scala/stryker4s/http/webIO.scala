package stryker4s.http

trait WebIO {
  def postRequest(url: String, content: String): WebResponse
}

case class WebResponse(httpCode: Int, responseBody: String)

object HttpClient extends WebIO {
  import scalaj.http._

  override def postRequest(
      url: String,
      content: String
  ): WebResponse = {
    val response = Http(url)
      .postData(content)
      .header("Content-Type", "application/json")
      .options(HttpOptions.followRedirects(true))
      .asString

    WebResponse(response.code, response.body)
  }
}
