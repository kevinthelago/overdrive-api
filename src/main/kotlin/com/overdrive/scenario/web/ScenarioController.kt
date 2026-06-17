package com.overdrive.scenario.web

import com.overdrive.scenario.app.ScenarioDiffService
import com.overdrive.scenario.app.ScenarioNameConflictException
import com.overdrive.scenario.app.ScenarioNotFoundException
import com.overdrive.scenario.app.ScenarioService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/scenarios")
class ScenarioController(
    private val scenarioService: ScenarioService,
    private val scenarioDiffService: ScenarioDiffService,
) {

    @GetMapping
    fun list(): List<ScenarioResponse> =
        scenarioService.listAll().map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ScenarioResponse =
        scenarioService.getById(id).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid request: ScenarioCreateRequest): ScenarioResponse =
        scenarioService.create(request).toResponse()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: ScenarioUpdateRequest,
    ): ScenarioResponse = scenarioService.update(id, request).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = scenarioService.delete(id)

    @PostMapping("/{id}/compare")
    fun compare(
        @PathVariable id: UUID,
        @RequestBody @Valid request: ScenarioCompareRequest,
    ): ScenarioDiffResponse {
        val scenario = scenarioService.getById(id)
        return scenarioDiffService.compare(
            scenario = scenario,
            scenarioId = id,
            productIds = request.productIds,
            destinationZip = request.destinationZip,
            serviceLevel = request.serviceLevel,
        )
    }

    @ExceptionHandler(ScenarioNotFoundException::class)
    fun handleNotFound(ex: ScenarioNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("detail" to (ex.message ?: "Not found")))

    @ExceptionHandler(ScenarioNameConflictException::class)
    fun handleConflict(ex: ScenarioNameConflictException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("detail" to (ex.message ?: "Conflict")))
}
