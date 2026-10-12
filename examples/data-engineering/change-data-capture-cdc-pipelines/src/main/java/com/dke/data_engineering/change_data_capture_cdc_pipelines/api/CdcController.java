package com.dke.data_engineering.change_data_capture_cdc_pipelines.api;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.CustomerReplica;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RejectedEvent;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.CustomerReplicaRepository;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.PipelineRunRepository;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.repository.RejectedEventRepository;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.CdcPipelineService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CdcController {

    private final CdcPipelineService pipeline;
    private final PipelineRunRepository runs;
    private final CustomerReplicaRepository replicas;
    private final RejectedEventRepository rejects;

    public CdcController(CdcPipelineService pipeline, PipelineRunRepository runs,
                         CustomerReplicaRepository replicas, RejectedEventRepository rejects) {
        this.pipeline = pipeline;
        this.runs = runs;
        this.replicas = replicas;
        this.rejects = rejects;
    }

    @PostMapping("/pipeline/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse trigger(@Valid @RequestBody RunRequest request) {
        return RunResponse.from(pipeline.run(request.source(), "API"));
    }

    @GetMapping("/pipeline/runs")
    public List<RunResponse> listRuns() {
        return runs.findAllByOrderByIdDesc().stream().map(RunResponse::from).toList();
    }

    @GetMapping("/pipeline/runs/{id}")
    public RunResponse getRun(@PathVariable Long id) {
        return runs.findById(id).map(RunResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Run " + id + " not found"));
    }

    @GetMapping("/pipeline/runs/{id}/rejected")
    public List<RejectedEvent> rejected(@PathVariable Long id) {
        return rejects.findByRunIdOrderByLsnAsc(id);
    }

    @GetMapping("/replica/customers")
    public List<CustomerReplica> customers() {
        return replicas.findAllByOrderByCustomerIdAsc();
    }
}
