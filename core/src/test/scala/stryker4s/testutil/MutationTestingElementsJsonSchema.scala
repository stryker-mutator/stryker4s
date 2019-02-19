package stryker4s.testutil


import grizzled.slf4j.Logging
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import stryker4s.scalatest.FileUtil

object MutationTestingElementsJsonSchema extends Logging {

  /**
    * Retrieve json schema from unpkg so we can validate our html reporter output to the json schema.
    */
  def mutationTestingElementsJsonSchema: Schema = {
    val schema = FileUtil.getResource("mutation-testing-report-schema.json").contentAsString
      SchemaLoader
        .builder()
        .schemaJson(new JSONObject(schema))
        .build()
        .load()
        .build()
  }

}
