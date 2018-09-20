package stryker4s

import com.softwaremill.sttp._
import grizzled.slf4j.Logging
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject

object MutationTestingElementsJsonSchema extends Logging {

  private[this] implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  private[this] val schemaUri: Uri =
    uri"https://unpkg.com/mutation-testing-elements@latest/mutation-testing-report-schema.json"

  /**
    * Retrieve json schema from unpkg so we can validate our html reporter output to the json schema.
    */
  def mutationTestingElementsJsonSchema: Option[Schema] = {
    val request = sttp.get(schemaUri).response(asString)
    val response = request.send()

    response.body match {
      case Right(responseBody) => toJsonSchema(responseBody)
      case Left(failure) =>
        error(s"Could not retrieve json schema reason: $failure")
        None
    }
  }

  private[this] def toJsonSchema(responseBody: String): Option[Schema] = {
    Option(
      SchemaLoader
        .builder()
        .schemaJson(new JSONObject(responseBody))
        .build()
        .load()
        .build())
  }
}
