package stryker4s.http

import scalaj.http.HttpResponse

trait WebIO {
  def postRequest(url: String, content: String): HttpResponse[String]
}

object HttpClient extends WebIO {
  import scalaj.http._

  override def postRequest(url: String, content: String): HttpResponse[String] = {
    Http(url)
      .postData(content)
      .header("Content-Type", "application/json")
      .options(HttpOptions.followRedirects(true))
      .asString
  }
}
