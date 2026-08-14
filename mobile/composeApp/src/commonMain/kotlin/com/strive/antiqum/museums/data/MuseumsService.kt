package com.strive.antiqum.museums.data

import com.strive.antiqum.network.Response
import com.strive.antiqum.network.safeResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

interface MuseumsService {
    suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Response<WikidataSparqlResponse>
}

class MuseumsServiceImpl(private val httpClient: HttpClient) : MuseumsService {
    override suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Response<WikidataSparqlResponse> = safeResponse {
        httpClient.get("https://query.wikidata.org/sparql") {
            parameter("query", nearbyMuseumsQuery(latitude, longitude, radiusKm))
            parameter("format", "json")
            accept(ContentType.parse("application/sparql-results+json"))
            header(HttpHeaders.UserAgent, "Antiqum/1.0 (museum discovery app)")
        }
    }
}

internal fun nearbyMuseumsQuery(
    latitude: Double,
    longitude: Double,
    radiusKm: Int
): String = """
    SELECT DISTINCT ?museum ?museumLabel ?museumDescription ?location ?distance
                    ?image ?website ?inception ?address ?cityLabel ?countryLabel ?typeLabel
    WHERE {
      SERVICE wikibase:around {
        ?museum wdt:P625 ?location .
        bd:serviceParam wikibase:center "Point($longitude $latitude)"^^geo:wktLiteral .
        bd:serviceParam wikibase:radius "$radiusKm" .
        bd:serviceParam wikibase:distance ?distance .
      }
      ?museum wdt:P31 ?type .
      ?type wdt:P279* wd:Q33506 .
      OPTIONAL { ?museum wdt:P18 ?image . }
      OPTIONAL { ?museum wdt:P856 ?website . }
      OPTIONAL { ?museum wdt:P571 ?inception . }
      OPTIONAL { ?museum wdt:P6375 ?address . }
      OPTIONAL { ?museum wdt:P131 ?city . }
      OPTIONAL { ?museum wdt:P17 ?country . }
      SERVICE wikibase:label {
        bd:serviceParam wikibase:language "en,mul" .
        ?museum rdfs:label ?museumLabel .
        ?museum schema:description ?museumDescription .
        ?city rdfs:label ?cityLabel .
        ?country rdfs:label ?countryLabel .
        ?type rdfs:label ?typeLabel .
      }
    }
    ORDER BY ?distance
    LIMIT 80
""".trimIndent()
