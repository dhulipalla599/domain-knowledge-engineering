package com.dke.data_engineering.real_time_streaming_etl_with_kafka.api;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.integration.RevenueByCountry;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository.PipelineRunRepository;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository.RejectedRecordRepository;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.service.StreamingEtlService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EtlController {

    private final StreamingEtlService etl;
    private final PipelineRunRepository runs;
    private final RejectedRecordRepository rejects;
    private final RevenueByCountry revenue;

    public EtlController(StreamingEtlService etl, PipelineRunRepository runs,
                         RejectedRecordRepository rejects, RevenueByCountry revenue) {
        this.etl = etl;
        this.runs = runs;
        this.rejects = rejects;
        this.revenue = revenue;
    }

    @PostMapping("/pipeline-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse start(@Valid @RequestBody RunRequest request) {
        return RunResponse.from(etl.run(request.source()));
    }

    @GetMapping("/pipeline-runs")
    public List<RunResponse> list() {
        return runs.findAllByOrderByIdDesc().stream().map(RunResponse::from).toList();
    }

    @GetMapping("/pipeline-runs/{id}/rejects")
    public List<RejectedView> rejects(@PathVariable Long id) {
        return rejects.findByRunId(id).stream()
                .map(r -> new RejectedView(r.getRawLine(), r.getReason())).toList();
    }

    @GetMapping("/revenue")
    public Map<String, Long> revenueByCountry() {
        return revenue.snapshot();
    }

    public record RejectedView(String rawLine, String reason) {
    }
}
