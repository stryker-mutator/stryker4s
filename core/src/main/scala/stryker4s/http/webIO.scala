package stryker4s.http

import scalaj.http.HttpResponse

trait WebIO {
  def postRequest(url: String, content: String): HttpResponse[String]
  def putRequest(url: String, content: String, headers: Map[String, String] = Map.empty): HttpResponse[String]
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
  def putRequest(url: String, content: String, headers: Map[String, String] = Map.empty): HttpResponse[String] = {
    Http(url)
      .put(content)
      .header("Content-Type", "application/json")
      .headers(headers)
      .options(HttpOptions.followRedirects(true))
      .asString
  }
}
