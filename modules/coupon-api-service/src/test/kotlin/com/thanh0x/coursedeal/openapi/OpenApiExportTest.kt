package com.thanh0x.coursedeal.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiExportTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `export deterministic OpenAPI spec and verify contract`() {
        val rawJson =
            mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val mapper =
            JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build()

        val parsed = mapper.readValue(rawJson, Any::class.java)
        val sorted = sortJsonRecursively(parsed)
        val formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sorted) + "\n"

        val rootNode = mapper.readTree(formattedJson)
        verifySchemas(rootNode.path("components").path("schemas"))
        verifyOperations(rootNode.path("paths"))

        val outputPath =
            System.getProperty("openapi.output.file")
                ?: File("../../openapi/openapi.json").canonicalPath
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(formattedJson)
        assertThat(outputFile).exists()
    }

    private fun verifySchemas(schemasNode: JsonNode) {
        assertThat(schemasNode.has("UserEntity")).isFalse()
        assertThat(schemasNode.has("PublicKeyCredentialCreationOptions")).isFalse()
        assertThat(schemasNode.has("AssertionRequest")).isFalse()

        assertThat(schemasNode.path("CouponSummaryDTO").path("properties").has("isNew")).isTrue()
        assertThat(schemasNode.path("CouponSummaryDTO").path("properties").has("new")).isFalse()
        assertThat(schemasNode.path("CouponDetailDTO").path("properties").has("isNew")).isTrue()
        assertThat(schemasNode.path("CouponDetailDTO").path("properties").has("new")).isFalse()
    }

    private fun verifyOperations(pathsNode: JsonNode) {
        val operationIds = mutableSetOf<String>()
        pathsNode.properties().forEach { (_, pathItem) ->
            pathItem.properties().forEach { (_, operation) ->
                val opId = operation.path("operationId").asText(null)
                if (opId != null) {
                    operationIds.add(opId)
                }
                operation.path("parameters").forEach { param ->
                    assertThat(param.path("name").asText("")).doesNotContain("user", "request", "httpServletRequest")
                }
            }
        }
        assertThat(operationIds).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATION_IDS)
    }

    private fun sortJsonRecursively(node: Any?): Any? =
        when (node) {
            is Map<*, *> ->
                node.entries
                    .associate { (k, v) ->
                        val key = k.toString()
                        val sortedVal =
                            when {
                                (key == "tags" || key == "parameters") && v is List<*> ->
                                    v.map { sortJsonRecursively(it) }
                                        .sortedBy { (it as? Map<*, *>)?.get("name")?.toString() ?: it.toString() }
                                key == "required" && v is List<*> ->
                                    v.map { it.toString() }.sorted()
                                else -> sortJsonRecursively(v)
                            }
                        key to sortedVal
                    }
                    .toSortedMap()
            is List<*> -> node.map { sortJsonRecursively(it) }
            else -> node
        }

    companion object {
        private val EXPECTED_OPERATION_IDS =
            setOf(
                "listCoupons",
                "createCoupon",
                "deleteCoupon",
                "refreshCoupon",
                "updateCoupon",
                "getCouponDetail",
                "getCourseDetails",
                "getCourseReviews",
                "getCourseCurriculum",
                "getRelatedCourses",
                "reportCourse",
                "register",
                "sendOtp",
                "verifyOtp",
                "login",
                "refresh",
                "logout",
                "logoutAll",
                "socialLogin",
                "mockLogin",
                "getPasskeyRegistrationOptions",
                "finishPasskeyRegistration",
                "getPasskeyAuthenticationOptions",
                "finishPasskeyAuthentication",
                "getPreferences",
                "updatePreferences",
                "acceptTerms",
                "deleteMyAccount",
                "getProfile",
                "updateProfile",
            )
    }
}
