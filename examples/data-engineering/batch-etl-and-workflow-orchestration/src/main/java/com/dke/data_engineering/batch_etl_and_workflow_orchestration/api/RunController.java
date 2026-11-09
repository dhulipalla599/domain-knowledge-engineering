package com.dke.data_engineering.batch_etl_and_workflow_orchestration.api;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun.Trigger;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.service.PipelineService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final PipelineService pipeline;

    public RunController(PipelineService pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse start(@Valid @RequestBody RunRequest request) {
        return RunResponse.from(pipeline.run(request.businessDate(), Trigger.API));
    }

    @GetMapping
    public List<RunResponse> list() {
        return pipeline.list().stream().map(RunResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RunResponse get(@PathVariable Long id) {
        return RunResponse.from(pipeline.get(id));
    }
}
