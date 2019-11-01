package stryker4s.testutil

import better.files.Resource
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject

object MutationTestingElementsJsonSchema {
  /** Load json schema from resources so we can validate our report case classes output to the json schema.
    */
  def mutationTestingElementsJsonSchema: Schema = {
    val schema = Resource.getAsString("mutation-testing-report-schema/mutation-testing-report-schema.json")
    SchemaLoader
      .builder()
      .schemaJson(new JSONObject(schema))
      .build()
      .load()
      .build()
  }
}
